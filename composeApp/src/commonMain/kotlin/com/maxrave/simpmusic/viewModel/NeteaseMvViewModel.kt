package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.repository.SongRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.downloaded

/** MV 二级页数据源(路由字符串透传,destination 序列化用) */
object NeteaseMvGridSource {
    /** 最新 MV(mv/first,服务端无 offset 分页,一次 30 条) */
    const val FIRST = "first"

    /** MV 总榜(mv/toplist,offset 分页 50 条到头) */
    const val TOPLIST = "toplist"

    /** 艺人视频(artist/mvs,offset 分页) */
    const val ARTIST = "artist"
}

/**
 * MV 二级页 VM(主页"最新 MV/MV 总榜"行与艺人页"视频"行的更多入口):
 * BrowseScreen 同款 Adaptive 网格 + 近底翻页;FIRST 无分页一次到位。
 * 艺人视频页复用同一分页状态,playAll 走歌单式整队装载(MV 哨兵 id 逐曲路由取流)。
 */
class NeteaseMvViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    sealed interface State {
        data object Loading : State

        data class Ready(
            val mvs: List<com.maxrave.domain.data.model.browse.album.Track> = emptyList(),
            val loadingMore: Boolean = false,
            val hasMore: Boolean = true,
        ) : State

        data class Error(val message: String? = null) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state.asStateFlow()

    private var source: String = NeteaseMvGridSource.FIRST
    private var artistId: Long = 0L
    private var started = false

    fun load(
        source: String,
        artistId: Long,
        force: Boolean = false,
    ) {
        if (started && !force) return
        started = true
        this.source = source
        this.artistId = artistId
        _state.value = State.Loading
        viewModelScope.launch { fetch(offset = 0, first = true) }
    }

    fun loadMore() {
        val current = _state.value as? State.Ready ?: return
        if (!current.hasMore || current.loadingMore) return
        _state.update { (it as State.Ready).copy(loadingMore = true) }
        viewModelScope.launch { fetch(offset = current.mvs.size, first = false) }
    }

    private suspend fun fetch(
        offset: Int,
        first: Boolean,
    ) {
        val result =
            when (source) {
                NeteaseMvGridSource.TOPLIST ->
                    neteaseRepository.getMvToplist(limit = PAGE_SIZE, offset = offset)
                NeteaseMvGridSource.ARTIST ->
                    neteaseRepository.getArtistMvsPaged(artistId, limit = PAGE_SIZE, offset = offset)
                else -> neteaseRepository.getMvFirst().map { it to false }
            }
        result.fold(
            onSuccess = { (page, hasMore) ->
                if (first) {
                    _state.value =
                        if (page.isEmpty()) {
                            State.Error()
                        } else {
                            State.Ready(page, hasMore = hasMore)
                        }
                } else {
                    val current = _state.value as? State.Ready ?: return
                    // 服务端游标回退会翻回重复页,按 videoId 去重;全重复视为没有更多(防死循环)
                    val existing = current.mvs.map { it.videoId }.toSet()
                    val merged = current.mvs + page.filter { it.videoId !in existing }
                    val progressed = page.isNotEmpty() && merged.size > current.mvs.size
                    _state.value = State.Ready(merged, loadingMore = false, hasMore = hasMore && progressed)
                }
            },
            onFailure = {
                if (first) {
                    _state.value = State.Error(it.message)
                } else {
                    // 翻页失败退回可重试态(不弹错,下次近底再试)
                    _state.update { s -> (s as? State.Ready)?.copy(loadingMore = false) ?: s }
                }
            },
        )
    }

    /** 艺人视频页"播放全部/随机播放"(歌单式整队,MvListViewModel.playAll 同语义):
     *  队列哨兵不带无尽前缀=普通歌单队列;PLAYLIST_CLICK+index 全量装载。 */
    fun playAll(
        shuffle: Boolean = false,
        queueName: String,
    ) {
        viewModelScope.launch {
            val current = _state.value as? State.Ready ?: return@launch
            if (current.mvs.isEmpty()) return@launch
            val list = if (shuffle) current.mvs.shuffled() else current.mvs
            setQueueData(
                QueueData.Data(
                    listTracks = ArrayList(list),
                    firstPlayedTrack = list.first(),
                    playlistId = NETEASE_ARTIST_VIDEOS_QUEUE_ID,
                    playlistName = queueName,
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            loadMediaItem(list.first(), Config.PLAYLIST_CLICK, 0)
        }
    }

    /** 艺人视频页搜索/加队列前把剩余页翻完(收藏 MV 页 withAllMvs 同口径:只过滤已
     *  加载条目会漏未加载页的结果);已无更多则零请求直接返回。onDone 收全量。 */
    fun withAllMvs(onDone: (List<com.maxrave.domain.data.model.browse.album.Track>) -> Unit = {}) {
        viewModelScope.launch {
            var pages = 0
            while ((_state.value as? State.Ready)?.hasMore == true && pages < MAX_ALL_LOAD_PAGES) {
                val current = _state.value as? State.Ready ?: break
                fetch(offset = current.mvs.size, first = false)
                pages++
            }
            onDone((_state.value as? State.Ready)?.mvs ?: emptyList())
        }
    }

    /** 批量下载分类结果(与收藏 MV 页 MvBatchRequest 同构):显式全量动作先翻完
     *  剩余页再分类,文件在盘=已下载;全已下载 toast,含已下载交屏弹三选确认。 */
    data class MvBatchRequest(
        val notDownloaded: List<com.maxrave.domain.data.model.browse.album.Track>,
        val downloaded: List<com.maxrave.domain.data.model.browse.album.Track>,
    )

    private val _mvBatchRequest =
        MutableStateFlow<MvBatchRequest?>(null)
    val mvBatchRequest: StateFlow<MvBatchRequest?> get() = _mvBatchRequest.asStateFlow()

    fun requestDownloadAll() {
        viewModelScope.launch {
            // 显式全量动作不缩水:先翻完剩余页(页数封顶防服务端异常死循环)
            var pages = 0
            while ((_state.value as? State.Ready)?.hasMore == true && pages < MAX_ALL_LOAD_PAGES) {
                val current = _state.value as? State.Ready ?: break
                fetch(offset = current.mvs.size, first = false)
                pages++
            }
            val mvs = (_state.value as? State.Ready)?.mvs ?: return@launch
            if (mvs.isEmpty()) return@launch
            val rows =
                songRepository.getSongsByListVideoId(mvs.map { it.videoId }).firstOrNull()
                    ?: emptyList()
            val pathById = rows.associateBy({ it.videoId }, { it.downloadedVideoFilePath })
            val notDownloaded = mutableListOf<com.maxrave.domain.data.model.browse.album.Track>()
            val downloaded = mutableListOf<com.maxrave.domain.data.model.browse.album.Track>()
            mvs.forEach { mv ->
                val onDisk =
                    pathById[mv.videoId]?.let { java.io.File(it).exists() } == true
                if (onDisk) downloaded += mv else notDownloaded += mv
            }
            if (downloaded.size == mvs.size) {
                makeToast(getString(Res.string.downloaded))
                return@launch
            }
            _mvBatchRequest.value = MvBatchRequest(notDownloaded, downloaded)
        }
    }

    fun dismissMvBatchRequest() {
        _mvBatchRequest.value = null
    }

    companion object {
        const val PAGE_SIZE = 30

        private const val MAX_ALL_LOAD_PAGES = 40

        /** 艺人视频整队播放的队列哨兵(普通歌单语义,不进无尽电台判定) */
        const val NETEASE_ARTIST_VIDEOS_QUEUE_ID = "NETEASE_ARTIST_VIDEOS"
    }
}

/**
 * 私人雷达二级页 VM:数据与主页行同源(getRadarSongsRow 的 10min 会话缓存共享,
 * 行已拉过则页零请求);继承 BaseViewModel 供卡片 playRadio 单曲电台起播。
 */
class NeteaseRadarViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
) : BaseViewModel() {
    sealed interface State {
        data object Loading : State

        data class Ready(val contents: List<Content>) : State

        data class Error(val message: String? = null) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state.asStateFlow()

    private var started = false

    fun load(force: Boolean = false) {
        if (started && !force) return
        started = true
        _state.value = State.Loading
        viewModelScope.launch {
            val contents =
                neteaseRepository.getRadarSongsRow(force)
                    ?.contents
                    ?.filterNotNull()
                    .orEmpty()
            _state.value = contents.takeIf { it.isNotEmpty() }?.let { State.Ready(it) } ?: State.Error()
        }
    }
}
