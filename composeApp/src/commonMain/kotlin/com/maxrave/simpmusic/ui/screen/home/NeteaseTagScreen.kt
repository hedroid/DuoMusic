package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.data.repository.NeteaseTagOrder
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NeteaseTagViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.netease_home_error
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.sort_highquality
import simpmusic.composeapp.generated.resources.sort_hot

/**
 * 网易标签分类页:两列自适应网格(对标网页版歌单广场),上游 MoodScreen 零改动。
 * 顶部排序 chip:热门/精品(/playlist/list 的 order 实测只支持 hot,new 返回空),
 * 下拉刷新绕过会话缓存强制重拉。
 */
@Composable
fun NeteaseTagScreen(
    navController: NavController,
    viewModel: NeteaseTagViewModel = koinViewModel(),
    tag: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // enum 不能直接进 Bundle,存 ordinal
    var orderIndex by rememberSaveable { mutableIntStateOf(0) }
    val order = NeteaseTagOrder.entries[orderIndex]

    LaunchedEffect(tag, order) { viewModel.load(tag, order) }

    // isRefreshing 只跟随"下拉触发的刷新";切 chip 的 Loading 不点亮下拉指示器
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state !is NeteaseTagViewModel.State.Loading) refreshing = false
    }

    // 顶栏随滚动收起(2026-10-13 用户反馈"类似页面统一处理",播客分类页同款定式):
    // 上滑时标题行(返回+标签名)收起,只留排序 chips 贴顶,回顶展开;位置驱动+
    // 纯覆盖层+maxOf 锁 contentPadding(防 LazyGrid 每帧重锚定振荡)
    val gridState = rememberLazyGridState()
    val atTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                viewModel.load(tag, order, force = true)
            },
        ) {
            Crossfade(state) { current ->
                when (current) {
                    is NeteaseTagViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                    is NeteaseTagViewModel.State.Error ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.netease_home_error),
                                style = typo().bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.load(tag, order) }) {
                                Text(stringResource(Res.string.retry))
                            }
                        }

                    is NeteaseTagViewModel.State.Ready ->
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            // 与主页歌单行同口径:页面水平边距 15dp、卡片间距 4dp。
                            // 旧实现无 contentPadding,间距靠每个 item 外包 Box(padding 8dp),
                            // 页边只有 8dp 且随列数浮动,与主页 15dp 不一致。
                            // 顶部让位=锁定的展开态顶栏高(标题行+chips),收起时内容从覆盖层下穿过
                            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = topBarHeight + 4.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // id 参与但 index 兜底:映射形状缺 id(null)或数据源异常重复时
                            // 不至于 duplicate/null key 直接崩
                            itemsIndexed(
                                current.contents,
                                key = { index, content -> "${content.playlistBrowseId}-$index" },
                            ) { _, content ->
                                HomeItemContentPlaylist(
                                    onClick = {
                                        content.playlistBrowseId?.let { id ->
                                            navController.navigate(PlaylistDestination(playlistId = id))
                                        }
                                    },
                                    data = content,
                                    fillMaxWidth = true,
                                )
                            }
                            // copyright 页脚:跨满整行(普通 item 只占一格宽,格式就不对了)
                            item(span = { GridItemSpan(maxLineSpan) }) { EndOfPage() }
                        }
                }
            }
        }

        // 顶栏覆盖层:标题行可收起,排序 chips 恒贴顶(播客分类页同款双 AnimatedVisibility)
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .background(
                        // 滚开后歌单卡从覆盖层下穿过,必须有实底
                        if (atTop) Color.Transparent else MaterialTheme.colorScheme.surface,
                    ).onGloballyPositioned { coordinates ->
                        val measured = with(density) { coordinates.size.height.toDp() }
                        topBarHeight = maxOf(topBarHeight, measured)
                    },
        ) {
            AnimatedVisibility(
                visible = atTop,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                // 顶栏:返回 + 标签名(自带状态栏避让,收起态由下方 Spacer 顶位)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    IconButton(onClick = navController::navigateUp, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(
                            imageVector = SimpIcons.ArrowBackIosNew,
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = tag,
                        style = typo().headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            // 收起态由同尺寸的状态栏 Spacer 占位,chips 不钻到状态栏下面
            AnimatedVisibility(
                visible = !atTop,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars),
                )
            }
            // 排序 chips:热门/精品(order=new 服务端不支持,不加)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val labels =
                    listOf(
                        Res.string.sort_hot,
                        Res.string.sort_highquality,
                    )
                NeteaseTagOrder.entries.forEachIndexed { index, tagOrder ->
                    Chip(
                        isSelected = index == orderIndex,
                        text = stringResource(labels[index]),
                    ) {
                        if (orderIndex != index) orderIndex = index
                    }
                }
            }
        }
    }
}
