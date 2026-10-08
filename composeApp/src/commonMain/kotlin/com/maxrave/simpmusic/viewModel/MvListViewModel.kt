package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.SongRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.favorite_mvs
import simpmusic.composeapp.generated.resources.mv_partial_loaded
import simpmusic.composeapp.generated.resources.no_favorite_mvs

/** 收藏的 MV 列表页状态:items 累积 + hasMore/加载中(整页错误回 Error) */
sealed interface MvListUIState {
    data object Loading : MvListUIState

    data class Success(
        val mvs: List<Track> = emptyList(),
        val hasMore: Boolean = false,
        /** 服务端收藏总数(mv/sublist count):头部计数用它,翻完页前 > 已加载条数 */
        val totalCount: Int = 0,
        val loadingMore: Boolean = false,
        /** 翻页失败标记:挡住近底自动重试(失败时列表仍停底部,条件会立刻再成立,
         *  不挡就变成无退避的连续重试风暴),页脚改出"重试"行等用户点 */
        val loadMoreFailed: Boolean = false,
    ) : MvListUIState

    data class Error(val message: String) : MvListUIState
}

/** 收藏的 MV(mv/sublist,offset 分页 30/页);点击行=单 MV 队列起播(SharedViewModel.playMv) */
class MvListViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<MvListUIState>(MvListUIState.Loading)
    val uiState: StateFlow<MvListUIState> get() = _uiState.asStateFlow()

    private var inFlight = false

    /** 取页互斥锁:offset 真源是 consumed,读 offset→请求→appendPage(推进 consumed)
     *  必须整体串行——两路并发取同一 offset 会双推进 consumed,显示去重后无感,
     *  但真实 offset 跳过一页(近底自动翻页与"全部"动作并发的场景)。 */
    private val loadMutex = Mutex()

    /** 服务端累计已返回条数(去重前)——分页 offset 的真源。展示列表按 id 去重会缩水,
     *  拿去重后的 size 当 offset,跨页出现重复 MV 时会重取一段旧数据 */
    private var consumed = 0

    init {
        load()
    }

    private fun load() {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            fetchNextPage()
            inFlight = false
        }
    }

    /** 取一页(锁内);返回本页是否成功落地 */
    private suspend fun fetchNextPage(): Boolean =
        loadMutex.withLock {
            val offset = if (_uiState.value is MvListUIState.Success) consumed else 0
            neteaseRepository
                .getMvSublistPaged(limit = PAGE_SIZE, offset = offset)
                .fold(
                    onSuccess = { (mvs, hasMore, totalCount) ->
                        appendPage(mvs, hasMore, totalCount)
                        true
                    },
                    onFailure = { e ->
                        _uiState.update {
                            if (it is MvListUIState.Success) {
                                it.copy(loadingMore = false, loadMoreFailed = true)
                            } else {
                                MvListUIState.Error(e.message ?: "mv sublist failed")
                            }
                        }
                        false
                    },
                )
        }

    /** 一页落地:跨页按 id 去重(与歌曲分页同款防护);totalCount 以服务端为准,回 0 保旧值 */
    private fun appendPage(
        mvs: List<Track>,
        hasMore: Boolean,
        totalCount: Int,
    ) {
        consumed += mvs.size
        _uiState.update {
            val old = it as? MvListUIState.Success
            MvListUIState.Success(
                mvs = ((old?.mvs ?: emptyList()) + mvs).distinctBy { m -> m.videoId },
                hasMore = hasMore && mvs.isNotEmpty(),
                totalCount = totalCount.takeIf { t -> t > 0 } ?: (old?.totalCount ?: 0),
                loadingMore = false,
                loadMoreFailed = false,
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state is MvListUIState.Success && state.hasMore && !state.loadingMore && !state.loadMoreFailed) {
            _uiState.value = state.copy(loadingMore = true)
            load()
        }
    }

    /** 页脚"重试":清失败标记后走正常翻页 */
    fun retryLoadMore() {
        val state = _uiState.value
        if (state is MvListUIState.Success && state.loadMoreFailed && !state.loadingMore) {
            _uiState.value = state.copy(loadMoreFailed = false)
            loadMore()
        }
    }

    fun refresh() {
        _uiState.value = MvListUIState.Loading
        inFlight = false
        consumed = 0
        load()
    }

    /** loadAllRemaining 的结果:complete=false=失败/触顶截断,动作只覆盖已加载部分(必须提示) */
    data class AllMvs(
        val mvs: List<Track>,
        val complete: Boolean,
    )

    /**
     * 显式全量动作(播放全部/添加到队列/批量下载)前置:把剩余页全部拉完再返回全量。
     * 队列哨兵(NETEASE_MV_SUBLIST)禁止续页,只装已加载条目会让"全部"缩水成"已翻到
     * 的那部分";MV 元数据分页轻量(30 条/页),显式动作值得整备。取页走 loadMutex,
     * 与在途翻页自然串行(锁内读 consumed),不会取到同一 offset。
     * 失败/触顶(防服务端异常翻不完)时返回已加载部分并标 complete=false——动作照做
     * 但调用方必须提示范围缩水,不静默装作"全部"。
     */
    private suspend fun loadAllRemaining(): AllMvs {
        inFlight = true
        _uiState.update { (it as? MvListUIState.Success)?.copy(loadingMore = true) ?: it }
        try {
            var pages = 0
            while (true) {
                val state = _uiState.value as? MvListUIState.Success ?: return AllMvs(emptyList(), true)
                if (!state.hasMore) return AllMvs(state.mvs, complete = true)
                if (++pages > MAX_ALL_LOAD_PAGES) return AllMvs(state.mvs, complete = false)
                if (!fetchNextPage()) {
                    return AllMvs(
                        (_uiState.value as? MvListUIState.Success)?.mvs ?: emptyList(),
                        complete = false,
                    )
                }
            }
        } finally {
            inFlight = false
            _uiState.update { (it as? MvListUIState.Success)?.copy(loadingMore = false) ?: it }
        }
    }

    /** 范围缩水提示(全量动作只拿到部分):VM 内统一发,三个入口同一文案 */
    private suspend fun toastPartialIfAny(all: AllMvs) {
        if (!all.complete && all.mvs.isNotEmpty()) {
            makeToast(
                org.jetbrains.compose.resources.getString(Res.string.mv_partial_loaded, all.mvs.size.toString()),
            )
        }
    }

    /** UI 侧全量入口:拉全(含缩水提示)后在 Main 线程回调(添加到队列等页面编排用) */
    fun withAllMvs(block: (List<Track>) -> Unit) {
        viewModelScope.launch {
            val all = loadAllRemaining()
            toastPartialIfAny(all)
            block(all.mvs)
        }
    }

    /** 整队起播(播放/随机按钮,2026-10-08 歌单详情页同构):playlistId 用 NETEASE_MV_
     *  前缀哨兵——双端 loadMore 见前缀即早退,不触发无尽续播(本页靠页内翻页;
     *  "播放全部"先把剩余页拉完,见 loadAllRemaining)。 */
    fun playAll(shuffle: Boolean = false) {
        viewModelScope.launch {
            val all = loadAllRemaining()
            if (all.mvs.isEmpty()) {
                makeToast(getString(Res.string.no_favorite_mvs))
                return@launch
            }
            toastPartialIfAny(all)
            val list = if (shuffle) all.mvs.shuffled() else all.mvs
            setQueueData(
                QueueData.Data(
                    listTracks = list.toCollection(arrayListOf<Track>()),
                    firstPlayedTrack = list.first(),
                    playlistId = MV_SUBLIST_QUEUE_ID,
                    playlistName = getString(Res.string.favorite_mvs),
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            loadMediaItem(list.first(), Config.PLAYLIST_CLICK, 0)
        }
    }

    /** 已应用过移除的 id:防 likeChangeEvent 与 likeWriteResults 双通道对同一笔取消
     *  各送达一次导致重复减计。不能用"行是否在列表"当幂等键——未加载页的 MV 取消
     *  (播放页取消第 31+ 条)行本来就不在,照样要减 totalCount。重新收藏(insertMv)
     *  时移除,之后的取消是另一笔;refresh 不清(刷新后到达的重复旧事件须继续被吸收,
     *  新鲜服务端计数不该再减)。 */
    private val appliedRemovals = mutableSetOf<String>()

    /**
     * 取消收藏落定(服务端对账确认后)原地移除行并同步计数/分页游标:
     * - totalCount -1:事件到达时服务端计数已减(未加载页的 MV 同样减——头部计数是
     *   服务端口径,不是已加载条数);幂等键=appliedRemovals(见上),双通道重复
     *   送达只减一次。
     * - consumed -1(仅行在已加载窗口内时):sublist 按 offset 分页,移行后服务端
     *   后续条目前移一位,游标不回退会从旧 offset 取下一页=跳过原第 consumed+1 条
     *   (显示去重掩盖不了,那条永远漏掉)。行不在已加载窗口(后面的页)时服务端
     *   移动发生在窗口之外,已消费位置不变,游标不动。
     */
    fun removeMv(videoId: String) {
        val state = _uiState.value as? MvListUIState.Success ?: return
        if (!appliedRemovals.add(videoId)) return
        if (state.mvs.any { mv -> mv.videoId == videoId }) {
            consumed = maxOf(consumed - 1, 0)
        }
        _uiState.update {
            val s = it as? MvListUIState.Success ?: return@update it
            s.copy(
                mvs = s.mvs.filterNot { mv -> mv.videoId == videoId },
                totalCount = (s.totalCount - 1).coerceAtLeast(0),
            )
        }
    }

    /** 别处(迷你条/播放页)收藏 MV:插首行(sublist 服务端口径=最新在前=新收藏是
     *  服务端位置 1,原列表整体后移一位);按 id 去重,只有真插入才 +1 计数——重复
     *  事件不虚增;真插入时 consumed +1 对齐后移(否则下一页取回已展示的旧末条,
     *  去重后白费一页窗口);同时清 appliedRemovals——重新收藏后再取消是新的一笔 */
    fun insertMv(track: Track) {
        appliedRemovals.remove(track.videoId)
        val state = _uiState.value as? MvListUIState.Success ?: return
        // 已在列表(重复事件)不动:计数与游标都别动
        if (state.mvs.any { mv -> mv.videoId == track.videoId }) return
        consumed += 1
        _uiState.update {
            val s = it as? MvListUIState.Success ?: return@update it
            if (s.mvs.any { mv -> mv.videoId == track.videoId }) {
                s
            } else {
                s.copy(mvs = listOf(track) + s.mvs, totalCount = s.totalCount + 1)
            }
        }
    }

    // ------------------------------------------------ 页级批量下载(2026-10-08 对齐歌单三选口径)

    /** 批量下载分类结果:已下载=视频文件在盘(song 行视频列) */
    data class MvBatchRequest(
        val notDownloaded: List<Track> = emptyList(),
        val downloaded: List<Track> = emptyList(),
    )

    private val _mvBatchRequest = MutableStateFlow<MvBatchRequest?>(null)
    val mvBatchRequest: StateFlow<MvBatchRequest?> get() = _mvBatchRequest.asStateFlow()

    fun dismissMvBatchRequest() {
        _mvBatchRequest.value = null
    }

    /**
     * 页级下载入口(按钮/顶栏菜单共用):先拉全再按 song 行视频列分类——文件在盘=已下载,
     * 其余未下载(在途不单独分:downloadMvFile 内置在途守卫,重复入队自动 no-op)。
     * 全部已下载→toast 不发请求;否则发请求由页面决定直下(全未下载)或弹三选(部分)。
     */
    fun requestDownloadAll() {
        viewModelScope.launch {
            val all = loadAllRemaining()
            val mvs = all.mvs
            if (mvs.isEmpty()) {
                makeToast(getString(Res.string.no_favorite_mvs))
                return@launch
            }
            toastPartialIfAny(all)
            val rows =
                songRepository.getSongsByListVideoId(mvs.map { it.videoId }).firstOrNull()
                    ?: emptyList()
            val pathById = rows.associateBy({ it.videoId }, { it.downloadedVideoFilePath })
            val notDownloaded = mutableListOf<Track>()
            val downloaded = mutableListOf<Track>()
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

    companion object {
        private const val PAGE_SIZE = 30

        /** loadAllRemaining 页数封顶(30/页×40=1200 条):防服务端异常(hasMore 恒 true
         *  /重复回页)把显式动作带进死循环 */
        private const val MAX_ALL_LOAD_PAGES = 40

        /** 整队播放队列哨兵:与单 MV 播放(NETEASE_MV_<mvid>)同前缀族,loadMore 早退 */
        const val MV_SUBLIST_QUEUE_ID = "NETEASE_MV_SUBLIST"
    }
}
