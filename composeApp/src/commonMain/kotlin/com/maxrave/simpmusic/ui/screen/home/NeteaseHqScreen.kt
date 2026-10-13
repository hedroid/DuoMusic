package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NeteaseHqViewModel
import com.maxrave.simpmusic.viewModel.NeteaseNewAlbumsViewModel
import com.maxrave.simpmusic.viewModel.NeteaseToplistViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.netease_home_error
import simpmusic.composeapp.generated.resources.retry

/**
 * 网易精品歌单二级页(主页"精品歌单"行更多入口):NeteaseTagScreen 同款网格
 * (Adaptive 160dp 方卡/15dp 页边/4dp 横距/8dp 纵距),游标分页近底自动翻。
 */
@Composable
fun NeteaseHQScreen(
    navController: NavController,
    title: String,
    viewModel: NeteaseHqViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    val gridState = rememberLazyGridState()
    // 近底 12 张(~2 行)预取;落地后仍在区则继续链发,快划不撞墙(同 FM 行配方)
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 12
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadMore()
    }
    LaunchedEffect((state as? NeteaseHqViewModel.State.Ready)?.contents?.size) {
        val ready = state as? NeteaseHqViewModel.State.Ready ?: return@LaunchedEffect
        if (nearEnd && ready.hasMore && !ready.loadingMore) viewModel.loadMore()
    }

    NeteaseSecondaryPageScaffold(
        title = title,
        onBack = navController::navigateUp,
    ) {
        Crossfade(state) { current ->
            when (current) {
                is NeteaseHqViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is NeteaseHqViewModel.State.Error -> NeteaseSecondaryErrorColumn(
                    onRetry = { viewModel.load(force = true) },
                )

                is NeteaseHqViewModel.State.Ready ->
                    NeteaseSecondaryPlaylistGrid(
                        contents = current.contents,
                        gridState = gridState,
                        footerLoading = current.loadingMore,
                        onPlaylistClick = { id ->
                            navController.navigate(PlaylistDestination(playlistId = id))
                        },
                    )
            }
        }
    }
}

/** 网易排行榜二级页(主页"排行榜"行更多入口):完整榜单一次到位,同款网格无分页。 */
@Composable
fun NeteaseToplistScreen(
    navController: NavController,
    title: String,
    viewModel: NeteaseToplistViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    NeteaseSecondaryPageScaffold(
        title = title,
        onBack = navController::navigateUp,
    ) {
        Crossfade(state) { current ->
            when (current) {
                is NeteaseToplistViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is NeteaseToplistViewModel.State.Error -> NeteaseSecondaryErrorColumn(
                    onRetry = { viewModel.load() },
                )

                is NeteaseToplistViewModel.State.Ready ->
                    NeteaseSecondaryPlaylistGrid(
                        contents = current.contents,
                        gridState = rememberLazyGridState(),
                        footerLoading = false,
                        onPlaylistClick = { id ->
                            navController.navigate(PlaylistDestination(playlistId = id))
                        },
                    )
            }
        }
    }
}

/** 二级页骨架:返回钮 + 居中标题(NeteaseTagScreen 同款顶栏);MV/雷达/电台列表二级页共用 */
@Composable
internal fun NeteaseSecondaryPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    imageVector = SimpIcons.ArrowBackIosNew,
                    contentDescription = null,
                )
            }
            Text(
                text = title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        content()
    }
}

@Composable
internal fun NeteaseSecondaryErrorColumn(onRetry: () -> Unit) {
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
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.retry))
        }
    }
}

/** 分类页同款歌单网格:Adaptive 160dp 方卡、15dp 页边、4dp 横距/8dp 纵距 */
@Composable
private fun NeteaseSecondaryPlaylistGrid(
    contents: List<com.maxrave.domain.data.model.mood.moodmoments.Content>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    footerLoading: Boolean,
    onPlaylistClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            contents,
            key = { index, content -> "${content.playlistBrowseId}-$index" },
        ) { _, content ->
            HomeItemContentPlaylist(
                onClick = { onPlaylistClick(content.playlistBrowseId) },
                data = content,
                fillMaxWidth = true,
            )
        }
        if (footerLoading) {
            item(key = "loading-more", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                }
            }
        }
        item(key = "end", span = { GridItemSpan(maxLineSpan) }) { EndOfPage() }
    }
}

/**
 * 网易新碟上架二级页(主页"新碟上架"行的更多入口,2026-10-13 用户定案):顶栏带行标题
 * 标签 + 地区 chips(与主页行同款,切换整页重拉)+ 分类页同款 Adaptive 网格;
 * offset 近底自动翻页。主页一级行限量展示,全量来这里。
 * 顶栏随滚动收起(2026-10-13 用户反馈"下滑时顶部标题收起只留标签,类似页面统一处理"):
 * 播客分类页同款定式——上滑标题行(返回+新碟上架)收起,只留地区 chips 贴顶,回顶展开;
 * 位置驱动+纯覆盖层+maxOf 锁 contentPadding(防 LazyGrid 每帧重锚定振荡)。
 */
@Composable
fun NeteaseNewAlbumsScreen(
    navController: NavController,
    title: String,
    initialArea: String = "ALL",
    viewModel: NeteaseNewAlbumsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val area by viewModel.area.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.startOnce(initialArea) }

    val gridState = rememberLazyGridState()
    // 上滑收起(播客分类页同款):网格滚离顶部时标题行收起;回到顶部重新展开。
    // 位置驱动——纯 derivedStateOf 读网格状态,收起是纯覆盖层(contentPadding 锁定,
    // 内容零位移),无"几何变化→滚动上报"反馈回路
    val atTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }
    // 锁定展开态顶栏高度给网格 contentPadding:收起时覆盖层变矮,padding 不跟随逐帧变化
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // 近底 12 张(~2 行)预取;落地后仍在区则继续链发(与精品歌单页同配方)
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 12
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadMore()
    }
    LaunchedEffect((state as? NeteaseNewAlbumsViewModel.State.Ready)?.albums?.size) {
        val ready = state as? NeteaseNewAlbumsViewModel.State.Ready ?: return@LaunchedEffect
        if (nearEnd && ready.hasMore && !ready.loadingMore) viewModel.loadMore()
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(state) { current ->
            when (current) {
                is NeteaseNewAlbumsViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is NeteaseNewAlbumsViewModel.State.Error ->
                    NeteaseSecondaryErrorColumn(onRetry = { viewModel.load() })

                is NeteaseNewAlbumsViewModel.State.Ready ->
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        // 顶部让位=锁定的展开态顶栏高(标题行+chips);收起时内容从覆盖层下穿过
                        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = topBarHeight + 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            current.albums,
                            key = { index, album -> "${album.browseId}-$index" },
                        ) { _, album ->
                            HomeItemContentPlaylist(
                                onClick = {
                                    navController.navigate(
                                        com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination(album.browseId),
                                    )
                                },
                                data = album,
                                fillMaxWidth = true,
                            )
                        }
                        if (current.loadingMore) {
                            item(key = "loading-more", span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                                }
                            }
                        }
                        item(key = "end", span = { GridItemSpan(maxLineSpan) }) { EndOfPage() }
                    }
            }
        }

        // 顶栏覆盖层:标题行可收起,地区 chips 恒贴顶(独立于内容状态流,切地区回 Loading
        // 不闪回 ALL;播客分类页同款双 AnimatedVisibility+状态栏 Spacer 占位)
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .background(
                        // 滚开后专辑卡从覆盖层下穿过,必须有实底
                        if (atTop) Color.Transparent else MaterialTheme.colorScheme.surface,
                    ).onGloballyPositioned { coordinates ->
                        val measured = with(density) { coordinates.size.height.toDp() }
                        topBarHeight = maxOf(topBarHeight, measured)
                    },
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = atTop,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                // 标题行(二级页骨架同款:返回+居中标题),自带状态栏避让(收起态由下方 Spacer 顶位)
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
                        text = title,
                        style = typo().headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            // 收起态由同尺寸的状态栏 Spacer 占位,chips 不钻到状态栏下面
            androidx.compose.animation.AnimatedVisibility(
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
            NewAlbumAreaChips(
                selected = area,
                modifier = Modifier.padding(horizontal = 15.dp),
            ) { viewModel.load(it) }
        }
    }
}
