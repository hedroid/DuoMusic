package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.domain.data.model.mood.moodmoments.Content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 网易精品歌单二级页 VM:游标分页(before=上页 nextBefore),近底自动翻页。 */
class NeteaseHqViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        /** hasMore=false 时到底不再拉 */
        data class Ready(
            val contents: List<Content>,
            val loadingMore: Boolean = false,
            val hasMore: Boolean = true,
        ) : State

        data class Error(val message: String? = null) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state.asStateFlow()

    /** 下一页游标;0=未开拉。翻页失败可重试,游标不前进 */
    private var nextBefore = 0L
    private var started = false

    fun load(force: Boolean = false) {
        if (started && !force) return
        started = true
        _state.value = State.Loading
        nextBefore = 0L
        viewModelScope.launch {
            neteaseRepository.getHqPlaylistPage().fold(
                onSuccess = { (contents, next) ->
                    nextBefore = next ?: 0L
                    _state.value =
                        if (contents.isEmpty()) {
                            State.Error()
                        } else {
                            State.Ready(contents, hasMore = next != null)
                        }
                },
                onFailure = { _state.value = State.Error(it.message) },
            )
        }
    }

    fun loadMore() {
        val current = _state.value as? State.Ready ?: return
        if (!current.hasMore || current.loadingMore) return
        _state.update { (it as State.Ready).copy(loadingMore = true) }
        viewModelScope.launch {
            neteaseRepository.getHqPlaylistPage(before = nextBefore).fold(
                onSuccess = { (page, next) ->
                    // 游标异常会翻回重复页,按 id 去重(与 tag 页同防线)
                    val existing = current.contents.map { it.playlistBrowseId }.toSet()
                    val merged = current.contents + page.filter { it.playlistBrowseId !in existing }
                    nextBefore = next ?: 0L
                    // 空页/全重复但游标还在走=服务端游标回退,视为没有更多,防死循环翻页
                    val progressed = page.isNotEmpty() && merged.size > current.contents.size
                    _state.value = State.Ready(merged, loadingMore = false, hasMore = next != null && progressed)
                },
                onFailure = {
                    // 翻页失败退回可重试态(不弹错,下次近底再试)
                    _state.update { (it as State.Ready).copy(loadingMore = false) }
                },
            )
        }
    }
}

/** 网易排行榜二级页 VM:完整榜单一次到位(toplist 无分页,与主页行共用 10min 缓存)。 */
class NeteaseToplistViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data class Ready(val contents: List<Content>) : State

        data class Error(val message: String? = null) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state.asStateFlow()

    fun load() {
        if (_state.value is State.Ready) return
        _state.value = State.Loading
        viewModelScope.launch {
            runCatching { neteaseRepository.getToplistContents() }.fold(
                onSuccess = { contents ->
                    _state.value =
                        if (contents.isEmpty()) State.Error() else State.Ready(contents)
                },
                onFailure = { _state.value = State.Error(it.message) },
            )
        }
    }
}

/** 网易新碟上架二级页 VM(2026-10-13,主页"新碟上架"行更多入口):地区切换整页重拉+
 *  offset 分页近底自动翻;hasMore=满页且去重后有新增(服务端不认 offset 翻回重复页时
 *  去重+进度判定收口,与精品歌单页同防线)。 */
class NeteaseNewAlbumsViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data class Ready(
            val albums: List<com.maxrave.domain.data.model.searchResult.albums.AlbumsResult>,
            val loadingMore: Boolean = false,
            val hasMore: Boolean = true,
        ) : State

        data object Error : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state.asStateFlow()

    /** 当前地区独立于页面状态流:切地区回 Loading 时 chips 选中态不闪回 ALL */
    private val _area = MutableStateFlow("ALL")
    val area: StateFlow<String> get() = _area.asStateFlow()

    private var started = false

    /** 代际计数(CR P2:快速切地区旧回包晚到会覆盖新地区):load 换代,翻页沿用当前代;
     *  回包时核对代际,过期直接丢弃——旧地区数据/旧页都不再并入当前状态 */
    private var loadGeneration = 0

    fun load(area: String = _area.value) {
        started = true
        val gen = ++loadGeneration
        _area.value = area
        _state.value = State.Loading
        viewModelScope.launch {
            neteaseRepository.getNewAlbumsPage(area = area).fold(
                onSuccess = { page ->
                    if (gen != loadGeneration) return@fold
                    _state.value =
                        if (page.isEmpty()) {
                            State.Error
                        } else {
                            State.Ready(page, hasMore = page.size >= PAGE_SIZE)
                        }
                },
                onFailure = {
                    if (gen == loadGeneration) _state.value = State.Error
                },
            )
        }
    }

    /** 首次进入触发一次(地区切换/重试直接调 [load]);initialArea=主页行当前选中
     *  地区(2026-10-13 四轮:跳二级页保持选中标签) */
    fun startOnce(initialArea: String = "ALL") {
        if (!started) load(initialArea)
    }

    fun loadMore() {
        val current = _state.value as? State.Ready ?: return
        if (!current.hasMore || current.loadingMore) return
        val gen = loadGeneration
        _state.update { (it as State.Ready).copy(loadingMore = true) }
        viewModelScope.launch {
            neteaseRepository.getNewAlbumsPage(area = _area.value, offset = current.albums.size).fold(
                onSuccess = { page ->
                    if (gen != loadGeneration) return@fold
                    val existing = current.albums.map { it.browseId }.toSet()
                    val merged = current.albums + page.filter { it.browseId !in existing }
                    // 空页/全重复但仍有偏移可走=服务端 offset 失效回吐,视为没有更多
                    val progressed = page.isNotEmpty() && merged.size > current.albums.size
                    _state.value =
                        State.Ready(merged, loadingMore = false, hasMore = page.size >= PAGE_SIZE && progressed)
                },
                onFailure = {
                    if (gen == loadGeneration) {
                        _state.update { (it as State.Ready).copy(loadingMore = false) }
                    }
                },
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
