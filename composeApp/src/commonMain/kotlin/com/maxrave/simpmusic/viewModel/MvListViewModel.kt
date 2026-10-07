package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 收藏的 MV 列表页状态:items 累积 + hasMore/加载中(整页错误回 Error) */
sealed interface MvListUIState {
    data object Loading : MvListUIState

    data class Success(
        val mvs: List<Track> = emptyList(),
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
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

    init {
        load()
    }

    private fun load() {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            val state = _uiState.value
            val offset = (state as? MvListUIState.Success)?.mvs?.size ?: 0
            neteaseRepository
                .getMvSublistPaged(limit = PAGE_SIZE, offset = offset)
                .fold(
                    onSuccess = { (mvs, hasMore) ->
                        _uiState.update {
                            val old = (it as? MvListUIState.Success)?.mvs ?: emptyList()
                            // 跨页按 id 去重(与歌曲分页同款防护)
                            MvListUIState.Success(
                                mvs = (old + mvs).distinctBy { m -> m.videoId },
                                hasMore = hasMore && mvs.isNotEmpty(),
                                loadingMore = false,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            if (it is MvListUIState.Success) {
                                it.copy(loadingMore = false)
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
        if (state is MvListUIState.Success && state.hasMore && !state.loadingMore) {
            _uiState.value = state.copy(loadingMore = true)
            load()
        }
    }

    fun refresh() {
        _uiState.value = MvListUIState.Loading
        inFlight = false
        load()
    }

    companion object {
        private const val PAGE_SIZE = 30
    }
}
