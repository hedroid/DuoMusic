package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.netease.model.NeteaseDjRadio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 播客发现页"更多"二级页数据源(路由字符串透传) */
object NeteaseRadioListSource {
    /** 我的订阅(/djradio/get/subed,需登录) */
    const val MY_SUBS = "my_subs"

    /** 猜你喜欢(/djradio/personalize/rcmd,需登录) */
    const val GUESS = "guess"

    /** 精选电台(/djradio/recommend/v1) */
    const val FEATURED = "featured"

    /** 热门电台榜(/djradio/toplist type=1) */
    const val HOT = "hot"

    /** 新晋电台榜(/djradio/toplist type=0) */
    const val NEW = "new"
}

/**
 * 播客电台列表二级页 VM:五个发现页行区各自的完整列表,一次到位
 * (端点要么无分页要么 limit 放大拉全;发现页行只预览前 8 张)。
 */
class NeteaseRadioListViewModel(
    private val neteaseRepository: NeteaseRepositoryImpl,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data class Ready(val radios: List<NeteaseDjRadio>) : State

        data class Error(val message: String? = null) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state.asStateFlow()

    private var started = false

    fun load(
        source: String,
        force: Boolean = false,
    ) {
        if (started && !force) return
        started = true
        _state.value = State.Loading
        viewModelScope.launch {
            val result =
                when (source) {
                    NeteaseRadioListSource.MY_SUBS -> neteaseRepository.getMyDjRadios()
                    NeteaseRadioListSource.GUESS -> neteaseRepository.getPersonalizedDjRadios(limit = 30)
                    NeteaseRadioListSource.FEATURED -> neteaseRepository.getRecommendDjRadios()
                    NeteaseRadioListSource.HOT -> neteaseRepository.getDjRadioToplist(limit = 100, type = 1)
                    else -> neteaseRepository.getDjRadioToplist(limit = 100, type = 0)
                }
            result.fold(
                onSuccess = { radios ->
                    _state.value = radios.takeIf { it.isNotEmpty() }?.let { State.Ready(it) } ?: State.Error()
                },
                onFailure = { _state.value = State.Error(it.message) },
            )
        }
    }
}
