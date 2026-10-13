package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.navigation.destination.list.NeteaseRadioDetailDestination
import com.maxrave.simpmusic.ui.screen.home.NeteaseSecondaryErrorColumn
import com.maxrave.simpmusic.ui.screen.home.NeteaseSecondaryPageScaffold
import com.maxrave.simpmusic.viewModel.NeteaseRadioListSource
import com.maxrave.simpmusic.viewModel.NeteaseRadioListViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 播客电台列表二级页(发现页 我的订阅/猜你喜欢/精选电台/热门电台/新晋电台 五行的
 * 更多入口):精品歌单二级页同款 Adaptive 160dp 网格,电台卡铺满槽宽;榜单源带排名角标。
 */
@Composable
fun NeteaseRadioListScreen(
    navController: NavController,
    source: String,
    title: String,
    viewModel: NeteaseRadioListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(source) { viewModel.load(source) }

    // 榜单源(HOT/NEW)整页带排名角标,与发现页行的角标语义一致(1 起)
    val ranked = source == NeteaseRadioListSource.HOT || source == NeteaseRadioListSource.NEW

    NeteaseSecondaryPageScaffold(
        title = title,
        onBack = navController::navigateUp,
    ) {
        Crossfade(state) { current ->
            when (current) {
                is NeteaseRadioListViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is NeteaseRadioListViewModel.State.Error ->
                    NeteaseSecondaryErrorColumn(onRetry = { viewModel.load(source, force = true) })

                is NeteaseRadioListViewModel.State.Ready ->
                    LazyVerticalGrid(
                        state = rememberLazyGridState(),
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            current.radios,
                            key = { _, radio -> "${radio.id}" },
                        ) { index, radio ->
                            NeteaseDjRadioCard(
                                radio = radio,
                                rank = if (ranked) index + 1 else null,
                                fillMaxWidth = true,
                            ) {
                                navController.navigate(
                                    NeteaseRadioDetailDestination(radioId = radio.id, radioName = radio.name),
                                )
                            }
                        }
                        item(key = "end", span = { GridItemSpan(maxLineSpan) }) { EndOfPage() }
                    }
            }
        }
    }
}
