package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.favorite_mvs
import simpmusic.composeapp.generated.resources.no_favorite_mvs

/** 收藏的 MV 列表页状态:items 累积 + hasMore/加载中(整页错误回 Error) */
sealed interface MvListUIState {
    data object Loading : MvListUIState

    data class Success(
        val mvs: List<Track> = emptyList(),
        val hasMore: Boolean = false,
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
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<MvListUIState>(MvListUIState.Loading)
    val uiState: StateFlow<MvListUIState> get() = _uiState.asStateFlow()

    private var inFlight = false

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
            val offset = if (_uiState.value is MvListUIState.Success) consumed else 0
            neteaseRepository
                .getMvSublistPaged(limit = PAGE_SIZE, offset = offset)
                .fold(
                    onSuccess = { (mvs, hasMore) ->
                        consumed += mvs.size
                        _uiState.update {
                            val old = (it as? MvListUIState.Success)?.mvs ?: emptyList()
                            // 跨页按 id 去重(与歌曲分页同款防护)
                            MvListUIState.Success(
                                mvs = (old + mvs).distinctBy { m -> m.videoId },
                                hasMore = hasMore && mvs.isNotEmpty(),
                                loadingMore = false,
                                loadMoreFailed = false,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            if (it is MvListUIState.Success) {
                                it.copy(loadingMore = false, loadMoreFailed = true)
                            } else {
                                MvListUIState.Error(e.message ?: "mv sublist failed")
                            }
                        }
                    },
                )
            inFlight = false
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

    /** 整队起播(播放/随机按钮,2026-10-08 歌单详情页同构):playlistId 用 NETEASE_MV_
     *  前缀哨兵——双端 loadMore 见前缀即早退,不触发无尽续播(本页靠页内翻页)。 */
    fun playAll(shuffle: Boolean = false) {
        val mvs = (uiState.value as? MvListUIState.Success)?.mvs ?: return
        if (mvs.isEmpty()) {
            makeToast(getString(Res.string.no_favorite_mvs))
            return
        }
        val list = if (shuffle) mvs.shuffled() else mvs
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

    /** 顶栏菜单"添加到队列"目标(已加载 MV 追加进当前队列,不打断播放) */
    fun loadedMvs(): List<Track> = (uiState.value as? MvListUIState.Success)?.mvs ?: emptyList()

    companion object {
        private const val PAGE_SIZE = 30

        /** 整队播放队列哨兵:与单 MV 播放(NETEASE_MV_<mvid>)同前缀族,loadMore 早退 */
        const val MV_SUBLIST_QUEUE_ID = "NETEASE_MV_SUBLIST"
    }
}
