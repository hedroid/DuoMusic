package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.source.MusicSource
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toQueryList
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import com.maxrave.netease.model.NeteaseHotWord
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.featured_playlists
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.videos

// State cho tìm kiếm
data class SearchScreenState(
    val searchType: SearchType = SearchType.ALL,
    val searchAllResult: List<SearchResultType> = emptyList(),
    val searchSongsResult: List<SongsResult> = emptyList(),
    val searchVideosResult: List<VideosResult> = emptyList(),
    val searchAlbumsResult: List<AlbumsResult> = emptyList(),
    val searchArtistsResult: List<ArtistsResult> = emptyList(),
    val searchPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchFeaturedPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchPodcastsResult: List<PlaylistsResult> = emptyList(),
    val suggestQueries: List<String> = emptyList(),
    val suggestYTItems: List<SearchResultType> = emptyList(),
    // SONGS tab 分页:下一页令牌(null=没有更多)+追加中标记;其余 tab 一次拉完无分页
    val songsNextPageToken: String? = null,
    val songsLoadingMore: Boolean = false,
    // ALL 聚合搜索中失败的部分类别:该类别在混排里静默缺席,切到其 tab 且列表为空时
    // 显示错误+重试(而非"无结果");tab 专属搜索成功或新 searchAll 时覆写清空
    val searchAllFailedCategories: Set<SearchType> = emptySet(),
    // ALL 混排所属搜索词(聚合发布时始终记录,不限失败):切回 ALL tab 时与当前搜索词
    // 不一致就重新聚合(UI 侧判断),混排展示永远对得上搜索框;null=尚无聚合(或已作废)
    val searchAllQuery: String? = null,
    // 聚合所属音源(MusicSource.name):同词切源后旧聚合不得复用——chip 判定词+源双比对
    val searchAllSource: String? = null,
    // 分类**单发**搜索的失败标记(tab→(搜索词,派发时源)):失败发生时用户已切走的话全局
    // Error 不写(错误页不能盖到别的 tab 上),靠本标记让用户回到该 tab 时仍见错误+重试;
    // 渲染侧要求词+源都与当前一致——换词后旧词的失败不得显示在新词下。
    // 该 tab 再搜成功/新聚合发布时清除。与聚合失败标记(searchAllFailedCategories)分层
    val tabSearchFailed: Map<SearchType, Pair<String, String>> = emptyMap(),
    // 各分类列表的所属绑定(tab→(搜索词,派发时源)):聚合发布覆盖全部七类,分类单发
    // 成功覆盖本类。chip 切入时与当前词/源核对,不符就地重搜该分类——分类展示永远
    // 对得上搜索框(与 ALL 的词+源双比对对称),旧词/旧源结果不再复用
    val categoryBindings: Map<SearchType, Pair<String, String>> = emptyMap(),
)

// Loại tìm kiếm
enum class SearchType {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FEATURED_PLAYLISTS,
    PODCASTS,
}

fun SearchType.toStringRes(): StringResource =
    when (this) {
        SearchType.ALL -> Res.string.all
        SearchType.SONGS -> Res.string.songs
        SearchType.VIDEOS -> Res.string.videos
        SearchType.ALBUMS -> Res.string.albums
        SearchType.ARTISTS -> Res.string.artists
        SearchType.PLAYLISTS -> Res.string.playlists
        SearchType.FEATURED_PLAYLISTS -> Res.string.featured_playlists
        SearchType.PODCASTS -> Res.string.podcasts
    }

// UI state cho tìm kiếm
sealed class SearchScreenUIState {
    object Empty : SearchScreenUIState()

    object Loading : SearchScreenUIState()

    object Success : SearchScreenUIState()

    object Error : SearchScreenUIState()
}

class SearchViewModel(
    private val dataStoreManager: DataStoreManager,
    private val searchRepository: SearchRepository,
    private val homeRepository: HomeRepository,
    private val neteaseRepository: NeteaseRepositoryImpl,
) : BaseViewModel() {
    private val _searchScreenUIState = MutableStateFlow<SearchScreenUIState>(SearchScreenUIState.Empty)
    val searchScreenUIState: StateFlow<SearchScreenUIState> get() = _searchScreenUIState.asStateFlow()

    private val _searchScreenState = MutableStateFlow(SearchScreenState())
    val searchScreenState: StateFlow<SearchScreenState> get() = _searchScreenState.asStateFlow()

    private val _searchHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val searchHistory: StateFlow<List<String>> get() = _searchHistory.asStateFlow()

    /** Browse categories shown on the empty search screen, the way YouTube Music itself does. */
    private val _moodAndGenres: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val moodAndGenres: StateFlow<Mood?> get() = _moodAndGenres.asStateFlow()

    /** Cover art per category params, filled in as tiles scroll into view. */
    private val _moodArtwork: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    val moodArtwork: StateFlow<Map<String, String>> get() = _moodArtwork.asStateFlow()

    private val requestedArtwork = mutableSetOf<String>()

    /** 网易热搜词(搜索空态页;YT 源恒空) */
    private val _hotSearch: MutableStateFlow<List<NeteaseHotWord>> = MutableStateFlow(emptyList())
    val hotSearch: StateFlow<List<NeteaseHotWord>> get() = _hotSearch.asStateFlow()

    var regionCode: String? = null
    var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        getSearchHistory()
        getMoodAndGenres()
        loadHotSearch()
        // 本 VM 是 Koin single,换源后空态数据必须重取(drop(1) 跳过首发射,
        // 避免与上面的 init 拉取重复):mood 置 null 触发重拉,热搜按新源重载
        viewModelScope.launch {
            dataStoreManager.selectedSource.drop(1).distinctUntilChanged().collect {
                _moodAndGenres.value = null
                requestedArtwork.clear()
                _moodArtwork.value = emptyMap()
                getMoodAndGenres()
                loadHotSearch()
            }
        }
    }

    private fun loadHotSearch() {
        viewModelScope.launch {
            if (dataStoreManager.selectedSource.first() != MusicSource.NETEASE.name) {
                _hotSearch.value = emptyList()
                return@launch
            }
            neteaseRepository.searchHotWords().onSuccess { _hotSearch.value = it }
        }
    }

    /**
     * Fetched once for the app's lifetime — this ViewModel is a Koin `single`, and the category
     * list changes about as often as YouTube ships a new mood, so re-fetching on every visit to
     * the search tab would be pure waste.
     */
    private fun getMoodAndGenres() {
        if (_moodAndGenres.value != null) return
        viewModelScope.launch {
            homeRepository.getMoodAndMomentsData().collect { resource ->
                if (resource is Resource.Success) {
                    _moodAndGenres.value = resource.data
                }
            }
        }
    }

    /**
     * Resolve the cover for one category. Called from the tile itself, so only categories the user
     * actually scrolls to cost a request — [requestedArtwork] keeps a tile scrolling in and out of
     * view from firing it again.
     */
    fun loadMoodArtwork(params: String) {
        if (!requestedArtwork.add(params)) return
        viewModelScope.launch {
            homeRepository.getMoodCategoryArtwork(params).collect { url ->
                if (url != null) {
                    _moodArtwork.update { it + (params to url) }
                }
            }
        }
    }

    private fun getSearchHistory() {
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { values ->
                if (values.isNotEmpty()) {
                    values.toQueryList().reversed().let { list ->
                        _searchHistory.value = list
                        log("Search history updated: $list")
                    }
                } else {
                    _searchHistory.value = emptyList()
                    log("Search history is empty")
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            searchRepository.insertSearchHistory(SearchHistory(query = query)).collectLatest {
                Logger.d(tag, "Inserted search history: $query, $it")
                getSearchHistory()
            }
        }
    }

    fun deleteSearchHistory() {
        viewModelScope.launch {
            searchRepository.deleteSearchHistory()
            delay(1000)
            getSearchHistory()
        }
    }

    /** 最近一次提交的搜索词(SONGS tab 加载更多要带着它续页) */
    private var lastQuery: String = ""

    // 在途搜索协程:新搜索(提交/点建议/点历史/切源重查)必须取消旧的——
    // searchAll 与 searchSongs 都写 searchSongsResult,任一路径晚到的旧结果会覆盖新查询(串台);
    // 旧 loadMore 不取消则会把旧查询的下一页追加进新结果。
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    private fun cancelInFlightSearch() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        // 被取消的 loadMore 走不到任何复位分支,这里统一复位,否则 songsLoadingMore 卡 true 永久堵住翻页
        _searchScreenState.update { it.copy(songsLoadingMore = false) }
    }

    /** ALL 混排列表构建(artists≥3 时只取前 3 个置顶):searchAll 发布与失败分类
     *  补齐回写共用,排序的唯一定义处。 */
    private fun buildMixedResult(
        song: List<SongsResult>,
        artist: List<ArtistsResult>,
        album: List<AlbumsResult>,
        playlist: List<PlaylistsResult>,
        video: List<VideosResult>,
        featured: List<PlaylistsResult>,
        podcast: List<PlaylistsResult>,
    ): ArrayList<SearchResultType> {
        val temp = ArrayList<SearchResultType>()
        if (artist.size >= 3) {
            for (i in 0..2) {
                temp += artist[i]
            }
        } else {
            temp.addAll(artist)
        }
        temp.addAll(song)
        temp.addAll(video)
        temp.addAll(album)
        temp.addAll(playlist)
        temp.addAll(featured)
        temp.addAll(podcast)
        return temp
    }

    /** 分类搜索成功的统一回写:[applyCategory] 落本分类字段。[source]=本次分类搜索派发
     *  时的音源。四种情形:
     *  1) **新词**(失败聚合仍挂着旧词) → 旧失败状态作废:混排清空+失败集合清空+
     *     所属词/源置 null(切回 ALL 由 UI 侧词+源比对重聚合兜底);
     *  2) 同词同源且本分类在失败集合 → 清本分类标记 + 用最新七路重建 ALL 混排
     *     (否则返回"全部"时横幅已消而该分类结果仍缺席);
     *  3) **同词跨源** → 旧聚合(含失败集合)整体不动——失败标记描述的是旧源聚合,
     *     新源的成功不替旧源消账(否则横幅消了而旧源混排仍缺该分类);旧聚合对
     *     其所属源仍有效,切回该源可正确展示,由 chip 词+源比对兜底刷新;
     *  4) 其余(无失败聚合/同词同源但本分类未失败) → 仅清本分类标记(对空集合
     *     no-op),混排不动。 */
    private fun categorySuccessState(
        state: SearchScreenState,
        category: SearchType,
        query: String,
        source: String,
        applyCategory: (SearchScreenState) -> SearchScreenState,
    ): SearchScreenState {
        val aggregateQuery = state.searchAllQuery
        val hasFailedAggregate = state.searchAllFailedCategories.isNotEmpty()
        val invalidateStaleAggregate = hasFailedAggregate && aggregateQuery != query
        // 同词同源:本次成功才允许动旧聚合(清标记+按需重建混排);跨源一律不碰
        val sameAggregateScope =
            hasFailedAggregate && !invalidateStaleAggregate &&
                aggregateQuery == query && state.searchAllSource == source
        val wasFailedAggregate = sameAggregateScope && category in state.searchAllFailedCategories
        val newFailedCategories =
            when {
                invalidateStaleAggregate -> emptySet()
                sameAggregateScope -> state.searchAllFailedCategories - category
                else -> state.searchAllFailedCategories
            }
        val updated =
            applyCategory(state).copy(
                // 本分类成功:单发失败标记无条件清除(与聚合扇区无关);列表绑定落到本次词/源
                tabSearchFailed = state.tabSearchFailed - category,
                categoryBindings = state.categoryBindings + (category to (query to source)),
                searchAllFailedCategories = newFailedCategories,
                searchAllQuery =
                    if (invalidateStaleAggregate) {
                        null
                    } else {
                        aggregateQuery
                    },
                searchAllSource =
                    if (invalidateStaleAggregate) {
                        null
                    } else {
                        state.searchAllSource
                    },
            )
        return when {
            invalidateStaleAggregate -> updated.copy(searchAllResult = emptyList())
            wasFailedAggregate -> {
                // 扇区体检:有分类绑定已属他源(往返切源后被他源搜索顶掉)→ 原聚合数据
                // 不可恢复,仅剔除会**静默丢失原本成功的分类**(剔除者不在失败提示里,
                // 聚合又仍挂着原词源被 chip 判定有效)——整体作废(词/源置 null),切回
                // ALL 由词+源比对自动重聚合,宁重聚不静默缺
                val scope = query to source
                val hasForeignCategory =
                    SearchType.entries
                        .filter { it != SearchType.ALL }
                        .any { updated.categoryBindings[it] != scope }
                if (hasForeignCategory) {
                    updated.copy(
                        searchAllResult = emptyList(),
                        searchAllFailedCategories = emptySet(),
                        searchAllQuery = null,
                        searchAllSource = null,
                    )
                } else {
                    updated.copy(
                        searchAllResult =
                            // 扇区过滤保留为防御(正常路径 hasForeign 已在上方拦截)
                            run {
                                fun <T> List<T>.inAggregateScope(type: SearchType): List<T> =
                                    if (updated.categoryBindings[type] == scope) this else emptyList()
                                buildMixedResult(
                                    song = updated.searchSongsResult.inAggregateScope(SearchType.SONGS),
                                    artist = updated.searchArtistsResult.inAggregateScope(SearchType.ARTISTS),
                                    album = updated.searchAlbumsResult.inAggregateScope(SearchType.ALBUMS),
                                    playlist = updated.searchPlaylistsResult.inAggregateScope(SearchType.PLAYLISTS),
                                    video = updated.searchVideosResult.inAggregateScope(SearchType.VIDEOS),
                                    featured = updated.searchFeaturedPlaylistsResult.inAggregateScope(SearchType.FEATURED_PLAYLISTS),
                                    podcast = updated.searchPodcastsResult.inAggregateScope(SearchType.PODCASTS),
                                )
                            },
                    )
                }
            }
            else -> updated
        }
    }

    fun searchSongs(query: String) {
        Logger.w("SEARCHPROBE", "searchSongs: '$query'")
        lastQuery = query
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataSongPage(query, null).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { (songsList, nextToken) ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.SONGS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchSongsResult = songsList,
                                        songsNextPageToken = nextToken,
                                        songsLoadingMore = false,
                                        tabSearchFailed = it.tabSearchFailed - SearchType.SONGS,
                                        categoryBindings = it.categoryBindings + (SearchType.SONGS to (query to sourceAtDispatch)),
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.SONGS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.SONGS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    /** SONGS tab 滚动近底追加下一页;失败静默保留已加载内容(token 不动,再滚可重试) */
    fun loadMoreSongs() {
        val state = _searchScreenState.value
        val token = state.songsNextPageToken ?: return
        if (state.songsLoadingMore || state.searchType != SearchType.SONGS) return
        _searchScreenState.update { it.copy(songsLoadingMore = true) }
        loadMoreJob = viewModelScope.launch {
            searchRepository.getSearchDataSongPage(lastQuery, token).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { (more, nextToken) ->
                            _searchScreenState.update { s ->
                                s.copy(
                                    searchSongsResult = s.searchSongsResult + more,
                                    songsNextPageToken = nextToken,
                                    songsLoadingMore = false,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        _searchScreenState.update { it.copy(songsLoadingMore = false) }
                    }
                }
            }
        }
    }

    fun searchAll(query: String) {
        Logger.w("SEARCHPROBE", "searchAll: '$query'")
        cancelInFlightSearch()
        // 聚合即 ALL 搜索,开头先落 tab 选中态(发布前的 Loading 期 chip 不滞后)
        _searchScreenState.update { it.copy(searchType = SearchType.ALL) }
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            // 派发时刻的音源随聚合落库:同词切源后旧聚合不复用(chip 词+源双比对)
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            var song = ArrayList<SongsResult>()
            val video = ArrayList<VideosResult>()
            var album = ArrayList<AlbumsResult>()
            var artist = ArrayList<ArtistsResult>()
            var playlist = ArrayList<PlaylistsResult>()
            var featuredPlaylist = ArrayList<PlaylistsResult>()
            var podcast = ArrayList<PlaylistsResult>()
            val temp: ArrayList<SearchResultType> = ArrayList()

            // 分类收集器:collect 完成后返回该分类是否成功(最后一次发射为准)。
            // 失败分类不能静默缺席——空结果与失败要能区分(业务拒绝误报"无结果"家族)
            val jobs =
                listOf(
                    async {
                        var ok = false
                        searchRepository.getSearchDataSong(query).collect { values ->
                            when (values) {
                                is Resource.Success -> {
                                    values.data?.let { song = it }
                                    ok = true
                                }
                                is Resource.Error -> ok = false
                            }
                        }
                        ok
                    },
                    async {
                        var ok = false
                        searchRepository.getSearchDataArtist(query).collect { values ->
                            when (values) {
                                is Resource.Success -> {
                                    values.data?.let { artist = it }
                                    ok = true
                                }
                                is Resource.Error -> ok = false
                            }
                        }
                        ok
                    },
                    async {
                        var ok = false
                        searchRepository
                            .getSearchDataAlbum(query)
                            .collect { values ->
                                when (values) {
                                    is Resource.Success -> {
                                        values.data?.let { album = it }
                                        ok = true
                                    }
                                    is Resource.Error -> ok = false
                                }
                            }
                        ok
                    },
                    async {
                        var ok = false
                        searchRepository.getSearchDataPlaylist(query).collect { values ->
                            when (values) {
                                is Resource.Success -> {
                                    values.data?.let { playlist = it }
                                    ok = true
                                }
                                is Resource.Error -> ok = false
                            }
                        }
                        ok
                    },
                    async {
                        var ok = false
                        searchRepository.getSearchDataVideo(query).collect { values ->
                            when (values) {
                                is Resource.Success -> {
                                    values.data?.let { video.addAll(it) }
                                    ok = true
                                }
                                is Resource.Error -> ok = false
                            }
                        }
                        ok
                    },
                    async {
                        var ok = false
                        searchRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                            when (values) {
                                is Resource.Success -> {
                                    values.data?.let { featuredPlaylist = it }
                                    ok = true
                                }
                                is Resource.Error -> ok = false
                            }
                        }
                        ok
                    },
                    async {
                        var ok = false
                        searchRepository.getSearchDataPodcast(query).collect { values ->
                            when (values) {
                                is Resource.Success -> {
                                    values.data?.let { podcast = it }
                                    ok = true
                                }
                                is Resource.Error -> ok = false
                            }
                        }
                        ok
                    },
                )
            val oks: List<Boolean> = jobs.awaitAll()
            // 与上面 async 顺序一一对应(featured 对网易恒 Success 空集,不计失败)
            val categoryTypes =
                listOf(
                    SearchType.SONGS,
                    SearchType.ARTISTS,
                    SearchType.ALBUMS,
                    SearchType.PLAYLISTS,
                    SearchType.VIDEOS,
                    SearchType.FEATURED_PLAYLISTS,
                    SearchType.PODCASTS,
                )
            val failedCategories = categoryTypes.filterIndexed { index, _ -> !oks[index] }.toSet()
            if (failedCategories.isNotEmpty()) {
                Logger.w("SEARCHPROBE", "searchAll partial failures: $failedCategories")
            }

            try {
                temp.addAll(buildMixedResult(song, artist, album, playlist, video, featuredPlaylist, podcast))

                _searchScreenState.update { state ->
                    state.copy(
                        searchAllResult = temp,  // searchType 不在此写:完成时写回会抢用户点 chip(见 setSearchType 注释)
                        searchSongsResult = song,
                        searchArtistsResult = artist,
                        searchAlbumsResult = album,
                        searchPlaylistsResult = playlist,
                        searchVideosResult = video,
                        searchFeaturedPlaylistsResult = featuredPlaylist,
                        searchPodcastsResult = podcast,
                        searchAllFailedCategories = failedCategories,
                        // 混排所属词/源始终记录(不限失败)——切回 ALL 词+源比对重聚合的判据
                        searchAllQuery = query,
                        searchAllSource = sourceAtDispatch,
                        // 聚合刷新了七个分类,单发失败标记整体作废;
                        // 七类列表的词/源绑定同步覆盖(失败类=空列表也绑——其空态由聚合失败标记渲染)
                        tabSearchFailed = emptyMap(),
                        categoryBindings = categoryTypes.associateWith { query to sourceAtDispatch },
                    )
                }
                // 全军覆没,或"结果为空且有失败"(分不清真空与失败,错误优先——
                // 真空无失败才允许落到"无结果"):错误态带重试,别把失败误报成空。
                // 聚合中途用户已切走的话不写全局 Error(错误页不能盖到别的 tab 上),
                // 空+失败态由 ALL 空态分支(源核对)在回看时兜底渲染
                if (_searchScreenState.value.searchType == SearchType.ALL) {
                    _searchScreenUIState.value =
                        if (failedCategories.size == categoryTypes.size || (temp.isEmpty() && failedCategories.isNotEmpty())) {
                            SearchScreenUIState.Error
                        } else {
                            SearchScreenUIState.Success
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchScreenUIState.value = SearchScreenUIState.Error
            }
        }
    }

    fun suggestQuery(query: String) {
        viewModelScope.launch {
            searchRepository.getSuggestQuery(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { suggestData ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    suggestQueries = suggestData.queries,
                                    suggestYTItems = suggestData.recommendedItems,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        // Không cần xử lý lỗi đặc biệt cho gợi ý
                        log("Error fetching suggest queries: ${values.message}", LogLevel.ERROR)
                    }
                }
            }
        }
    }

    fun searchAlbums(query: String) {
        Logger.w("SEARCHPROBE", "searchAlbums: '$query'")
        // 纳入在途取消:新搜索/切源重派取消旧请求,旧源晚到结果不再覆盖新源
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataAlbum(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { albumsList ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.ALBUMS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchAlbumsResult = albumsList,
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.ALBUMS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.ALBUMS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    fun searchFeaturedPlaylist(query: String) {
        Logger.w("SEARCHPROBE", "searchFeaturedPlaylist: '$query'")
        // 纳入在途取消:新搜索/切源重派取消旧请求,旧源晚到结果不再覆盖新源
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { featuredPlaylistList ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.FEATURED_PLAYLISTS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchFeaturedPlaylistsResult = featuredPlaylistList,
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.FEATURED_PLAYLISTS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.FEATURED_PLAYLISTS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    fun searchPodcast(query: String) {
        Logger.w("SEARCHPROBE", "searchPodcast: '$query'")
        // 纳入在途取消:新搜索/切源重派取消旧请求,旧源晚到结果不再覆盖新源
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataPodcast(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { podcastList ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.PODCASTS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchPodcastsResult = podcastList,
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.PODCASTS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.PODCASTS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    fun searchArtists(query: String) {
        Logger.w("SEARCHPROBE", "searchArtists: '$query'")
        // 纳入在途取消:新搜索/切源重派取消旧请求,旧源晚到结果不再覆盖新源
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataArtist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { artistsList ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.ARTISTS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchArtistsResult = artistsList,
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.ARTISTS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.ARTISTS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    fun searchPlaylists(query: String) {
        Logger.w("SEARCHPROBE", "searchPlaylists: '$query'")
        // 纳入在途取消:新搜索/切源重派取消旧请求,旧源晚到结果不再覆盖新源
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataPlaylist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { playlistsList ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.PLAYLISTS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchPlaylistsResult = playlistsList,
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.PLAYLISTS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.PLAYLISTS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    fun searchVideos(query: String) {
        Logger.w("SEARCHPROBE", "searchVideos: '$query'")
        // 纳入在途取消:新搜索/切源重派取消旧请求,旧源晚到结果不再覆盖新源
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob = viewModelScope.launch {
            val sourceAtDispatch = dataStoreManager.selectedSource.first()
            searchRepository.getSearchDataVideo(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { videosList ->
                            _searchScreenState.update { state ->
                                categorySuccessState(state, SearchType.VIDEOS, query, sourceAtDispatch) {
                                    it.copy(
                                        searchVideosResult = videosList,
                                    )
                                }
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        // 失败归属发起 tab:用户已切走则不写全局 Error(错误页不盖别的 tab),
                        // 记 tabSearchFailed(带派发源)让回看该 tab 时仍见错误+重试
                        _searchScreenState.update { it.copy(tabSearchFailed = it.tabSearchFailed + (SearchType.VIDEOS to (query to sourceAtDispatch))) }
                        if (_searchScreenState.value.searchType == SearchType.VIDEOS) {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
        }
    }

    /** searchType 只在**派发点**写:chip 点击(本函数)与 searchAll 开头的同步写。
     *  各搜索的完成回调一律不回写 searchType——加载中用户点其它分类后,晚到的成功
     *  回调若写回自己派发时的类型,会把用户的 chip 选择抢回去(tab 弹回旧分类)。
     *  切 tab 同时**取消在途搜索**并把 uiState 复位 Success:被弃请求的完成态
     *  (尤其失败的全局 Error)不再落到新 tab 上;失败信息由 tabSearchFailed 保留,
     *  用户回到发起 tab 时仍能看到错误+重试。 */
    fun setSearchType(searchType: SearchType) {
        // 点已选中的 chip = 无操作:不取消在途请求不覆写状态(加载中误触自己
        // 曾把请求掐掉,页面停在旧结果/"无结果")
        if (_searchScreenState.value.searchType == searchType) return
        Logger.w("SEARCHPROBE", "setSearchType -> $searchType")
        cancelInFlightSearch()
        _searchScreenUIState.value = SearchScreenUIState.Success
        _searchScreenState.update { state ->
            state.copy(searchType = searchType)
        }
    }
}