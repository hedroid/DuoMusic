package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import androidx.compose.animation.core.animateFloatAsState
import com.maxrave.simpmusic.ui.component.rememberCardHover
import com.maxrave.simpmusic.ui.component.hoverZoom
import com.maxrave.simpmusic.ui.component.HoverPlayButton
import com.maxrave.simpmusic.ui.component.ExpandableChip
import simpmusic.composeapp.generated.resources.category
import androidx.compose.runtime.saveable.rememberSaveable
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.maxrave.simpmusic.ui.screen.home.AccountAvatar
import simpmusic.composeapp.generated.resources.guest
import androidx.compose.foundation.layout.widthIn
import com.maxrave.simpmusic.ui.component.CreateActions
import simpmusic.composeapp.generated.resources.netease_account
import simpmusic.composeapp.generated.resources.log_in_to_netease
import com.maxrave.simpmusic.ui.navigation.destination.login.NeteaseLoginDestination
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.maxrave.simpmusic.ui.icon.Person
import com.maxrave.simpmusic.ui.icon.PersonAdd
import simpmusic.composeapp.generated.resources.welcome_back
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.CachePolicy
import coil3.compose.LocalPlatformContext
import simpmusic.composeapp.generated.resources.notification
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.settings
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.icon.Settings
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.good_morning
import simpmusic.composeapp.generated.resources.good_afternoon
import simpmusic.composeapp.generated.resources.good_evening
import simpmusic.composeapp.generated.resources.good_night
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.Notifications
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.exclude
import com.maxrave.domain.extension.now
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.ui.component.ItemArtistChart
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.extension.scrollReportingEffect
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeItemSong
import com.maxrave.simpmusic.ui.component.HomeItemVideo
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.ShelfTitleRow
import com.maxrave.simpmusic.ui.icon.PlaylistRemove
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.component.MediaRow
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NeteaseHQDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NeteaseMvGridDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NeteaseNewAlbumsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NeteaseTagDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NeteaseToplistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NeteaseHomeViewModel
import com.maxrave.simpmusic.viewModel.NeteaseMvGridSource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import simpmusic.composeapp.generated.resources.netease_home_error
import simpmusic.composeapp.generated.resources.retry

/**
 * 网易主页(独立页):挂载点按 selected_source 与上游 HomeScreen 二选一。
 * 上游文件零改动;数据来自 NeteaseHomeViewModel(独立,不实例化上游 HomeViewModel)。
 * 布局对照上游骨架(悬浮顶栏+chips+PullToRefresh)+ 网易自己的内容区
 * (账户卡/feed 行/榜单/五组分类),没有的元素根本不存在,无需条件隐藏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeteaseHomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: NeteaseHomeViewModel = koinInject(),
    navController: NavController,
) {
    // 上游启动横幅(远端配置 PromoBanner)已在 fork 整体移除——内容是 maxrave 的公告
    // 渠道,两个主页(YTM/网易)都不带(与博客/X 引流同口径)。
    val sharedViewModel: com.maxrave.simpmusic.viewModel.SharedViewModel = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    // 位移粒度滚动上报(库页同款 scrollReportingEffect,含贴底回吸兜底;index-only
    // 旧上报把末端钳制回吸读成上滑致底栏振荡——2026-10-13 混合页回归案,两页同修)
    scrollState.scrollReportingEffect(onScrolling)
    // 顶栏标题行显隐仍用方向态(上游同款;只管标题不管底栏上报)
    val pullToRefreshState = rememberPullToRefreshState()
    // 头部对齐 YT 主页玻璃形态(2026-10-28):页面内容=玻璃源,悬浮行(chips/药丸)是
    // 兄弟层折射;滚走后顶栏 hazeBlur 模糊条淡入(与 YT 一字不差同款)
    // 分类收起/展开(YT 的 Mood chip 同款形态):选中后折叠显示所选分类
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedTagIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val chipRowState = rememberScrollState()
    val hazeState = rememberHazeState()
    val chipBackdrop = rememberBackdrop(MaterialTheme.colorScheme.background)
    var topAppBarHeightPx by remember { mutableIntStateOf(0) }
    // 视频卡/歌曲卡长按=歌曲菜单(YT 主页同款;MV 行经 NETEASE_MV_ 哨兵自动换 MV 语义行)
    var menuTrack by remember {
        mutableStateOf<com.maxrave.domain.data.model.browse.album.Track?>(null)
    }
    menuTrack?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { menuTrack = null },
            song = track.toSongEntity(),
            navController = navController,
        )
    }

    // VM 是进程级 single,数据驻留整进程;每次本页重组(含 tab 切回)交给 VM 检测跨天,
    // 跨天则静默刷新一轮(行保持内容原位替换),同一天内零重拉
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    // 切源跳主页+刷新的落点(2026-10-13):消费与 YT 主页同一信号,静默刷新(行原地替换)。
    // 滚顶与刷新不再互斥(2026-10-30):切源落地时若只滚顶,恢复的深滚动位置会让新源
    // 主页停留在上次访问的缓存内容上——切源必须换内容,两个动作都要做。
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    LaunchedEffect(reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                scrollState.animateScrollToItem(0)
            }
            viewModel.refresh()
            sharedViewModel.reloadDestinationDone()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PullToRefreshBox(
            state = pullToRefreshState,
            onRefresh = viewModel::refresh,
            // 静默刷新:行内容保持不动、后台重拉原位替换,顶部指示器是唯一的刷新信号
            isRefreshing = refreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = refreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top =
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                            ),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.hazeSource(hazeState),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                // 上游 141d472a 同款:第一栏上方 8dp 呼吸(头部与内容不贴死)
                contentPadding = PaddingValues(top = 8.dp),
            ) {
                item(key = "top-space") {
                    Spacer(
                        Modifier.height(
                            with(LocalDensity.current) {
                                topAppBarHeightPx.toDp()
                            },
                        ),
                    )
                }
                // 行级懒加载:固定行序,每行独立槽位(Loading 占位→滚到才拉→Ready/Failed)
                NeteaseHomeViewModel.Row.entries.forEachIndexed { rowIndex, row ->
                    item(key = row.name) {
                        NeteaseHomeRowSlot(
                            row = row,
                            // 第一行带账号名眉题(YT:个人栏目名小字在标题上方,头像已在头部)
                            eyebrow = if (rowIndex == 0) accountInfo?.first?.takeIf { it.isNotBlank() } else null,
                            ui = state.rows[row] ?: NeteaseHomeViewModel.RowUi.Loading,
                            viewModel = viewModel,
                            navController = navController,
                            newAlbumsArea = state.newAlbumsArea,
                            onPlayMv = { sharedViewModel.playMv(it) },
                            onSongLongClick = { menuTrack = it.toTrack() },
                            onMvLongClick = { menuTrack = it },
                        )
                    }
                }
                item(key = "end") { EndOfPage() }
            }
        }
        // 悬浮顶栏 + chips(对照上游骨架)
        AnimatedContent(
            targetState = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0,
            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
        ) { target ->
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .then(
                            if (target) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier.hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle())
                            },
                        ).onGloballyPositioned { coordinates ->
                            topAppBarHeightPx = coordinates.size.height
                        },
            ) {
                // 头部跟随上游 YT 主页新设计(2026-10-27):头像菜单+chips+Create 药丸常驻一行;
                // 通知入头像菜单、设置在药丸齿轮——网易主页自此有通知与设置入口(旧不对称消除)
                Row(
                    // 头部行几何与 YT 主页逐字段对齐(2026-10-28):行高 56dp,头像盒
                    // padding(start=16,end=8),"全部"钉在头像旁第一格,其余分类收进
                    // ExpandableChip(展开选,选中后折叠显示所选),药丸在右
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.padding(start = 16.dp, end = 8.dp)) {
                        NeteaseHomeAccountButton(
                            accountInfo = accountInfo,
                            navController = navController,
                        )
                    }
                    // 竖屏:Create 药丸骑在 chips 行尾(YT 同款)——分类展开变宽时药丸被
                    // 推出右缘而不是从它底下滑过,视觉=展开让位;横屏/桌面药丸钉死右缘。
                    val createPinned = getPlatform() == Platform.Desktop || getScreenSizeInfo().let { it.wDP >= it.hDP }
                    val createActions: @Composable () -> Unit = {
                        CreateActions(
                            backdrop = chipBackdrop,
                            navController = navController,
                            onSettings = { navController.navigate(SettingsDestination) },
                        )
                    }
                    BoxWithConstraints(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        val chipHeadFade by animateFloatAsState(if (chipRowState.canScrollBackward) 1f else 0f)
                        val chipTailFade by animateFloatAsState(if (chipRowState.canScrollForward) 1f else 0f)
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .chipStripEdgeFades(32.dp, start = { chipHeadFade }, end = { chipTailFade })
                                    .horizontalScroll(chipRowState)
                                    .widthIn(min = maxWidth)
                                    .padding(end = if (createPinned) 0.dp else 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Chip(
                                    isAnimated = false,
                                    isSelected = selectedTagIndex == null && !categoriesExpanded,
                                    text = stringResource(Res.string.all),
                                    backdrop = chipBackdrop,
                                ) {
                                    // 与 YT 的 All 同款语义:收起分类展开+清选中(回"全部"态)
                                    categoriesExpanded = false
                                    selectedTagIndex = null
                                }
                                ExpandableChip(
                                    text = stringResource(Res.string.category),
                                    options = viewModel.chips,
                                    selectedIndex = selectedTagIndex,
                                    expanded = categoriesExpanded,
                                    onExpandedChange = { categoriesExpanded = it },
                                    onSelect = { index ->
                                        // YT 的 Mood chip 同款:选中即收起展开
                                        categoriesExpanded = false
                                        selectedTagIndex = index
                                        navController.navigate(NeteaseTagDestination(tag = viewModel.chips[index]))
                                    },
                                    backdrop = chipBackdrop,
                                )
                            }
                            if (!createPinned) {
                                Box(Modifier.padding(start = 8.dp)) { createActions() }
                            }
                        }
                    }
                    if (createPinned) {
                        Box(Modifier.padding(start = 8.dp, end = 16.dp)) { createActions() }
                    }
                }
            }
        }
    }
}

/** 一行内容:歌单卡跳歌单页,歌曲卡播放。标题行走 ShelfTitleRow——尾钮位(更多/占位)
 *  与 MediaRow 头部同几何,页内所有行标题右缘对齐(2026-10-13 用户定案)。 */
@Composable
private fun NeteaseHomeRow(
    title: String,
    contents: List<com.maxrave.domain.data.model.home.Content?>,
    navController: NavController,
    onSongClick: (com.maxrave.domain.data.model.home.Content) -> Unit,
    // 眉题(YT 货架 subtitle 同款):账号名小字画在行标题上方——头部头像之外的账号存在感
    eyebrow: String? = null,
) {
    Column(Modifier.padding(horizontal = 15.dp)) {
        val songContents = contents.filterNotNull().filter { it.videoId != null }
        eyebrow?.let {
            Text(
                text = it,
                style = typo().bodySmall,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        // 更多按钮只在真能跳二级页时显示(三轮修订,占位已拆)
        ShelfTitleRow(
            title = title,
            horizontalPadding = 0.dp,
            titleColor = MaterialTheme.colorScheme.onBackground,
        )
        if (songContents.isNotEmpty()) {
            // 歌曲行:3 行紧凑卡网格(120dp 方图+两行小字,避免大卡被行高裁切)。
            // 横向 4dp=每日推荐歌单行同款封面间距;纵向 8dp 维持原三行节奏
            val gridState = rememberLazyGridState()
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .height(460.dp),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(songContents, key = { it.videoId ?: it.title }) { content ->
                    NeteaseSongCard(
                        content = content,
                        onClick = { onSongClick(content) },
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                items(contents.filterNotNull(), key = { it.playlistId ?: it.title }) { content ->
                    HomeItemContentPlaylist(
                        onClick = {
                            content.playlistId?.let { id ->
                                navController.navigate(PlaylistDestination(playlistId = id))
                            }
                        },
                        data = content,
                    )
                }
            }
        }
    }
}


/** 分类区块:布局逐项对照上游 MoodMomentAndGenre(副标题+组标题+3 行横向网格),仅导航目标不同 */
@Composable
private fun NeteaseCategorySections(
    sections: List<com.maxrave.domain.data.model.mood.MoodSection>,
    navController: NavController,
) {
    Column(
        Modifier
            .padding(horizontal = 15.dp)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        sections.forEach { section ->
            val gridState = rememberLazyGridState()
            val flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = gridState))
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(210.dp),
                state = gridState,
                flingBehavior = flingBehavior,
            ) {
                items(section.items, key = { it.params }) { tag ->
                    MoodMomentAndGenreHomeItem(
                        title = tag.title,
                        stripeColor = tag.stripeColor,
                    ) {
                        navController.navigate(NeteaseTagDestination(tag = tag.title))
                    }
                }
            }
        }
    }
}



/** MV Track → HomeItemVideo 的 Content(主页 MV 行与 MV 二级页共用的视频卡映射,
 *  与艺人页 MV 货架同一映射形状) */
internal fun com.maxrave.domain.data.model.browse.album.Track.toVideoContent(): com.maxrave.domain.data.model.home.Content =
    com.maxrave.domain.data.model.home.Content(
        album = null,
        artists = artists,
        description = null,
        isExplicit = isExplicit,
        playlistId = null,
        browseId = null,
        thumbnails = thumbnails ?: emptyList(),
        title = title,
        videoId = videoId,
        views = views,
    )

/** 网格/单行通用紧凑歌曲卡:方图 + 标题/艺人两行小字。cardWidth 默认 96dp(3 行网格用),
 *  FM 单行(主页歌单行样式)传 120dp。NeteaseMixScreen(私人FM)同包复用,勿改回 private。
 *  onTrash 非空时图上叠垃圾桶角标(仅 FM 卡用;主页/每日推荐不传,视觉零变化)。
 *  卡片不带外层 padding——行内间距统一由容器 spacedBy 提供(2026-10-12 用户定案:
 *  对齐主页"每日推荐歌单"行的 4dp 封面间距),文本行自带 4dp 起始边距。 */
@Composable
internal fun NeteaseSongCard(
    content: com.maxrave.domain.data.model.home.Content,
    onClick: () -> Unit,
    onTrash: (() -> Unit)? = null,
    cardWidth: androidx.compose.ui.unit.Dp = 88.dp,
) {
    // hover 态(上游 HomeItemSong 同款,桌面鼠标):卡片 tint 浮起+封面框内缩放+悬停播放钮
    val (hoverModifier, hovered) = rememberCardHover(enabled = true)
    Column(
        modifier =
            Modifier
                .width(cardWidth)
                .clip(RoundedCornerShape(8.dp))
                .then(hoverModifier)
                .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            AsyncImage(
                model = content.thumbnails.lastOrNull()?.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .hoverZoom(hovered),
            )
            HoverPlayButton(visible = hovered, onClick = onClick)
            if (onTrash != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(onClick = onTrash),
                    contentAlignment = Alignment.Center,
                ) {
                    // 三横线+右下减号(与歌曲三点 sheet"删除下载?"行同款,2026-10-02 用户定):
                    // trashFm 语义=从 FM 批次移除并补新曲,PlaylistRemove 比垃圾桶贴切
                    Icon(
                        imageVector = SimpIcons.PlaylistRemove,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Text(
            text = content.title,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            // 卡下文本两行全静态省略(2026-10-13 七轮:主标题跑马灯"感觉有点乱"再停——
            // 副标题上一轮已静态,标题这轮也停,跑马灯在卡族整体退场)
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .padding(start = 4.dp, top = 4.dp),
        )
        Text(
            text = content.artists?.joinToString(", ") { it.name }.orEmpty(),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .padding(start = 4.dp),
        )
    }
}

/** 热门歌手行:100dp 圆形头像卡,点击进艺人页(M6 数字 ID 同页路由);库页"您的网易云"关注的歌手分区复用(showRank=false,
 *  horizontalPadding=0:网格已统一 15dp 边距,行自带会双重缩进) */
@Composable
internal fun NeteaseArtistRow(
    title: String,
    artists: List<com.maxrave.domain.data.model.searchResult.artists.ArtistsResult>,
    showRank: Boolean,
    navController: NavController,
    horizontalPadding: Dp = 15.dp,
) {
    // YTM 主页"热门艺人"(排行榜 shelf 艺人榜)同款:3 行 240dp 横滑网格 + ItemArtistChart
    // (排名+60dp 圆头像+名字+副标题)。网易无订阅数,副标题留空隐藏。
    var rowWidthDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val lazyGridState = rememberLazyGridState()
    val snapperFlingBehavior =
        rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyGridState))
    Column(Modifier.padding(horizontal = horizontalPadding)) {
        ShelfTitleRow(
            title = title,
            horizontalPadding = 0.dp,
            titleColor = MaterialTheme.colorScheme.onBackground,
        )
        Column(
            Modifier
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        rowWidthDp = (coordinates.size.width).toDp()
                    }
                },
        ) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(240.dp),
                state = lazyGridState,
                flingBehavior = snapperFlingBehavior,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(artists.size, key = { artists[it].browseId }) { index ->
                    val artist = artists[index]
                    ItemArtistChart(
                        onClick = {
                            navController.navigate(
                                com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination(artist.browseId),
                            )
                        },
                        data =
                            com.maxrave.domain.data.model.home.chart.ItemArtist(
                                browseId = artist.browseId,
                                rank = if (showRank) "${index + 1}" else "",
                                subscribers = "",
                                thumbnails = artist.thumbnails,
                                title = artist.artist,
                                trend = "",
                            ),
                        widthDp = rowWidthDp,
                    )
                }
            }
        }
    }
}

/** 新碟上架行:YT 主页同款 HomeItemContentPlaylist(160dp 方卡,一屏两列),
 *  点击进专辑页(M6 数字 ID 同页路由)。area 非空时显示地区 chips(分区缓存即时切换);
 *  库页"您的网易云"收藏的专辑分区复用(不带地区 chips,长按取消收藏)。
 *  previewCount 非空时限量展示+"更多"进二级页(2026-10-13 用户定案:主页一级页数量要控制,
 *  更多按钮门控=条目多于预览数,未达标也保留占位——ShelfTitleRow 内建)。 */
@Composable
internal fun NeteaseAlbumRow(
    title: String,
    albums: List<com.maxrave.domain.data.model.searchResult.albums.AlbumsResult>,
    navController: NavController,
    area: String? = null,
    onAreaSelect: ((String) -> Unit)? = null,
    onAlbumLongClick: ((com.maxrave.domain.data.model.searchResult.albums.AlbumsResult) -> Unit)? = null,
    horizontalPadding: Dp = 15.dp,
    previewCount: Int? = null,
    onMoreClick: (() -> Unit)? = null,
) {
    Column(Modifier.padding(horizontal = horizontalPadding)) {
        ShelfTitleRow(
            title = title,
            horizontalPadding = 0.dp,
            titleColor = MaterialTheme.colorScheme.onBackground,
            onMoreClick = onMoreClick,
        )
        if (area != null && onAreaSelect != null) {
            NewAlbumAreaChips(selected = area) { onAreaSelect(it) }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            items(albums.let { if (previewCount != null) it.take(previewCount) else it }, key = { it.browseId }) { album ->
                HomeItemContentPlaylist(
                    onClick = {
                        navController.navigate(
                            com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination(album.browseId),
                        )
                    },
                    data = album,
                    onLongClick = onAlbumLongClick?.let { callback -> { callback(album) } },
                )
            }
        }
    }
}

// 主页"有更多入口"的行(精品歌单/排行榜)横滑预览数量(用户 2026-10-12 定案:
// YTM 式"显示几个+更多进二级页";160dp 卡一屏约 2.3 张,8 张≈3 屏)
private const val HOME_ROW_PREVIEW_COUNT = 8

/** 行槽位:按行状态渲染占位或内容。Loading 占位被组合即触发拉取(滚到才加载)。
 *  占位高度对齐真实行高,内容落地不跳滚动位置。 */
@Composable
private fun NeteaseHomeRowSlot(
    row: NeteaseHomeViewModel.Row,
    ui: NeteaseHomeViewModel.RowUi,
    viewModel: NeteaseHomeViewModel,
    navController: NavController,
    newAlbumsArea: String,
    onPlayMv: (com.maxrave.domain.data.model.browse.album.Track) -> Unit,
    onSongLongClick: (com.maxrave.domain.data.model.home.Content) -> Unit,
    onMvLongClick: (com.maxrave.domain.data.model.browse.album.Track) -> Unit,
    eyebrow: String? = null,
) {
    when (ui) {
        is NeteaseHomeViewModel.RowUi.Loading -> {
            // 进入组合即触发本行拉取(下拉刷新重置后重新进入 Loading 也会再触发)
            LaunchedEffect(Unit) { viewModel.ensureRowLoaded(row) }
            RowPlaceholder(
                title = row.title,
                height = row.placeholderHeight(),
                retry = false,
            )
        }

        is NeteaseHomeViewModel.RowUi.Failed -> {
            RowPlaceholder(
                title = row.title,
                height = 120.dp,
                retry = true,
                onRetry = { viewModel.retryRow(row) },
            )
        }

        is NeteaseHomeViewModel.RowUi.Ready -> {
            when (val content = ui.content) {
                is NeteaseHomeViewModel.RowContent.Feed ->
                    if (row == NeteaseHomeViewModel.Row.RADAR_SONGS) {
                        // 私人雷达 = 混合页"每日推荐"同款 hero 轮播(用户 2026-10-13 三轮
                        // 定稿,推翻二轮 Quick picks 紧凑网格):大卡居中+窄邻卡+卡上叠字,
                        // 头部 Play all;无更多按钮(整队内容都在轮播里)
                        val radarSongs =
                            content.item.contents
                                .filterNotNull()
                                .filter { it.videoId != null }
                        NeteaseDailySection(
                            title = content.item.title,
                            contents = radarSongs,
                            onPlayAll = { viewModel.playSectionFrom(radarSongs, row.title) },
                            onPlayIndex = { index -> viewModel.playSectionFrom(radarSongs, row.title, index) },
                        )
                    } else if (row == NeteaseHomeViewModel.Row.HQ) {
                        // 精品歌单:YTM 式限量横滑 + 头部"更多"进二级页(游标分页网格);
                        // 门控=条目多于预览数(对齐 YTM moreEndpoint 服务端语义)
                        MediaRow(
                            title = content.item.title,
                            horizontalPadding = 15.dp,
                            onMoreClick =
                                if (content.item.contents.filterNotNull().size > HOME_ROW_PREVIEW_COUNT) {
                                    { navController.navigate(NeteaseHQDestination(title = row.title)) }
                                } else {
                                    null
                                },
                        ) {
                            items(
                                content.item.contents.filterNotNull().take(HOME_ROW_PREVIEW_COUNT),
                                key = { "${it.playlistId ?: it.title}" },
                            ) { pl ->
                                HomeItemContentPlaylist(
                                    onClick = {
                                        pl.playlistId?.let { id ->
                                            navController.navigate(PlaylistDestination(playlistId = id))
                                        }
                                    },
                                    data = pl,
                                )
                            }
                        }
                    } else {
                        NeteaseHomeRow(
                            title = content.item.title,
                            contents = content.item.contents,
                            navController = navController,
                            onSongClick = viewModel::playSong,
                            eyebrow = eyebrow,
                        )
                    }

                is NeteaseHomeViewModel.RowContent.ChartRow ->
                    // 排行榜:YTM 式限量横滑 + 头部"更多"进二级页(完整榜单网格)
                    MediaRow(
                        title = content.chart.listChartItem.firstOrNull()?.title ?: row.title,
                        horizontalPadding = 15.dp,
                        onMoreClick = {
                            navController.navigate(NeteaseToplistDestination(title = row.title))
                        },
                    ) {
                        items(
                            (content.chart.listChartItem.firstOrNull()?.playlists ?: emptyList()).take(HOME_ROW_PREVIEW_COUNT),
                            key = { it.id },
                        ) { pl ->
                            HomeItemContentPlaylist(
                                onClick = {
                                    navController.navigate(
                                        PlaylistDestination(playlistId = pl.id),
                                    )
                                },
                                data = pl,
                            )
                        }
                    }

                is NeteaseHomeViewModel.RowContent.ArtistsRow ->
                    NeteaseArtistRow(
                        title = row.title,
                        artists = content.list,
                        showRank = row == NeteaseHomeViewModel.Row.TOP_ARTISTS,
                        navController = navController,
                    )

                is NeteaseHomeViewModel.RowContent.AlbumsRow ->
                    if (row == NeteaseHomeViewModel.Row.NEW_ALBUMS) {
                        // 新碟上架:一级页限量展示(2026-10-13 用户定案)+更多进二级页
                        // (顶栏带"新碟上架"标签+地区 chips+分页网格)
                        NeteaseAlbumRow(
                            title = row.title,
                            albums = content.list,
                            navController = navController,
                            area = newAlbumsArea,
                            onAreaSelect = viewModel::loadNewAlbumsArea,
                            previewCount = HOME_ROW_PREVIEW_COUNT,
                            onMoreClick =
                                if (content.list.size > HOME_ROW_PREVIEW_COUNT) {
                                    // 跳二级页保持当前选中地区(2026-10-13 四轮用户反馈)
                                    { navController.navigate(NeteaseNewAlbumsDestination(title = row.title, area = newAlbumsArea)) }
                                } else {
                                    null
                                },
                        )
                    } else {
                        NeteaseAlbumRow(
                            title = row.title,
                            albums = content.list,
                            navController = navController,
                        )
                    }

                is NeteaseHomeViewModel.RowContent.MvRow -> {
                    // MV 行统一 YT 主页视频卡(HomeItemVideo,16:9/160dp 高);最新 MV/MV 总榜
                    // YTM 式限量+头部"更多"进二级页,个人推荐(服务端仅 2-3 条)全量无更多
                    var onMoreClick: (() -> Unit)? = null
                    var limited = false
                    when (row) {
                        NeteaseHomeViewModel.Row.MV -> {
                            limited = true
                            onMoreClick = {
                                navController.navigate(
                                    NeteaseMvGridDestination(source = NeteaseMvGridSource.FIRST, title = row.title),
                                )
                            }
                        }
                        NeteaseHomeViewModel.Row.MV_TOPLIST -> {
                            limited = true
                            onMoreClick = {
                                navController.navigate(
                                    NeteaseMvGridDestination(source = NeteaseMvGridSource.TOPLIST, title = row.title),
                                )
                            }
                        }
                        else -> {}
                    }
                    MediaRow(
                        title = row.title,
                        horizontalPadding = 15.dp,
                        onMoreClick = onMoreClick,
                    ) {
                        items(
                            content.list.let { if (limited) it.take(HOME_ROW_PREVIEW_COUNT) else it },
                            key = { it.videoId },
                        ) { mv ->
                            HomeItemVideo(
                                onClick = { onPlayMv(mv) },
                                onLongClick = { onMvLongClick(mv) },
                                data = mv.toVideoContent(),
                            )
                        }
                    }
                }

                is NeteaseHomeViewModel.RowContent.SectionsRow ->
                    NeteaseCategorySections(
                        sections = content.mood.sections,
                        navController = navController,
                    )
            }
        }
    }
}

/** 行占位:标题 + 居中转圈(失败态换成重试按钮) */
@Composable
private fun RowPlaceholder(
    title: String,
    height: androidx.compose.ui.unit.Dp,
    retry: Boolean,
    onRetry: () -> Unit = {},
) {
    Column(Modifier.padding(horizontal = 15.dp)) {
        Text(
            text = title,
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(height)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            if (retry) {
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.retry))
                }
            } else {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }
}

/** 占位高度对齐真实行高:歌单/榜单/歌手/专辑/雷达(单行 160 卡)行 ~250dp,歌曲三行网格 460dp,分类区块更高 */
private fun NeteaseHomeViewModel.Row.placeholderHeight(): androidx.compose.ui.unit.Dp =
    when (this) {
        NeteaseHomeViewModel.Row.NEW_SONGS -> 460.dp
        NeteaseHomeViewModel.Row.SECTIONS -> 640.dp
        else -> 250.dp
    }

/** 新碟上架地区 chips(ALL/华语/欧美/韩语/日语),命中分区缓存即时切换;
 *  新碟上架二级页复用(internal,同包)——两处选中态语义一致 */
@Composable
internal fun NewAlbumAreaChips(
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val areas = listOf("ALL" to "全部", "ZH" to "华语", "EA" to "欧美", "KR" to "韩语", "JP" to "日语")
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        areas.forEach { (code, label) ->
            Chip(
                isAnimated = false,
                isSelected = code == selected,
                text = label,
            ) { onSelect(code) }
        }
    }
}

// 网易主页头像按钮:与 YT 主页 HomeAccountButton 同构(大菜单=账户行+问候语+通知+登录态),
// 复用共享的 AccountAvatar;问候语时段同款。
@Composable
private fun NeteaseHomeAccountButton(
    accountInfo: Pair<String?, String?>?,
    navController: NavController,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // 登录态用真流(CR P2):头像 URL 判定在无头像账号/头像未回填时误显示登录按钮
    val sharedVm: com.maxrave.simpmusic.viewModel.SharedViewModel = koinInject()
    val neteaseLoggedIn by sharedVm.neteaseLoggedIn.collectAsStateWithLifecycle()
    val signedIn = neteaseLoggedIn
    val displayName = accountInfo?.first?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.guest)
    val hour = remember { now().time.hour }
    val greeting =
        stringResource(
            when (hour) {
                in 6..12 -> Res.string.good_morning
                in 13..17 -> Res.string.good_afternoon
                in 18..23 -> Res.string.good_evening
                else -> Res.string.good_night
            },
        )
    Box {
        AccountAvatar(
            signedIn = signedIn,
            thumbnailUrl = accountInfo?.second,
            size = 44.dp,
            contentDescription = displayName,
            modifier = Modifier.clickable { menuOpen = true },
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountAvatar(
                    signedIn = signedIn,
                    thumbnailUrl = accountInfo?.second,
                    size = 40.dp,
                    contentDescription = null,
                )
                Column {
                    Text(text = displayName, style = typo().titleSmall, maxLines = 1)
                    Text(text = greeting, style = typo().bodySmall)
                }
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.notification), style = typo().bodyMedium) },
                leadingIcon = { Icon(SimpIcons.Notifications, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    navController.navigate(NotificationDestination)
                },
            )
            if (!signedIn) {
                Button(
                    onClick = {
                        menuOpen = false
                        navController.navigate(NeteaseLoginDestination)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(Res.string.log_in_to_netease))
                }
            }
        }
    }
}

@Composable
fun HomeTopAppBar(navController: NavController) {
    val hour =
        remember {
            val date = now().time
            date.hour
        }
    TopAppBar(
        windowInsets =
            TopAppBarDefaults.windowInsets.exclude(
                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
            ),
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text =
                        when (hour) {
                            in 6..12 -> {
                                stringResource(Res.string.good_morning)
                            }

                            in 13..17 -> {
                                stringResource(Res.string.good_afternoon)
                            }

                            in 18..23 -> {
                                stringResource(Res.string.good_evening)
                            }

                            else -> {
                                stringResource(Res.string.good_night)
                            }
                        },
                    style = typo().bodySmall,
                )
            }
        },
        actions = {
            // 顶栏图标钮必须带描述:无描述的 IconButton 在 TalkBack 下是 NAF、被完全跳过
            // (UI-CR-07 走查实证:设置/通知/历史/一起听入口曾整体不可达)
            RippleIconButton(
                imageVector = SimpIcons.Notifications,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(Res.string.notification),
            ) {
                navController.navigate(NotificationDestination)
            }
            // 一起听随上游新头部进 Create sheet;History(Recently Added)页随上游删除。
            // 网易主页头部跟随改造(待办)时此整个组件按新设计重做。
            ListenTogetherIconButton(
                contentDescription = stringResource(Res.string.listen_together),
            ) { navController.navigate(ListenTogetherDestination) }
            RippleIconButton(
                imageVector = SimpIcons.Settings,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(Res.string.settings),
            ) {
                navController.navigate(SettingsDestination)
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}

