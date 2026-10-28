package com.maxrave.simpmusic.ui.screen.home

import com.maxrave.simpmusic.ui.component.rememberCardHover
import com.maxrave.simpmusic.ui.component.hoverZoom
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselItemDrawInfo
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.home.Content
import com.maxrave.simpmusic.extension.scrollReportingEffect
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.ShelfTitleRow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.utils.toHiResArtworkUrl
import com.maxrave.simpmusic.viewModel.NeteaseMixViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.netease_home_error
import simpmusic.composeapp.generated.resources.personal_fm
import simpmusic.composeapp.generated.resources.personal_fm_pause
import simpmusic.composeapp.generated.resources.personal_fm_resume
import simpmusic.composeapp.generated.resources.personal_fm_start
import simpmusic.composeapp.generated.resources.personal_fm_subtitle
import simpmusic.composeapp.generated.resources.play_all
import simpmusic.composeapp.generated.resources.retry

/**
 * 网易"混合"tab 独立屏(私人FM):挂载点按 selected_source 与上游 MixForYouScreen 二选一
 * (AppNavigationGraph 分流,同 HomeDestination 模式)。
 * 骨架对照 NeteaseHomeScreen(悬浮顶栏+PullToRefresh+Crossfade 状态机);内容区两块:
 * FM hero 大卡(开始收听)+ 扩容批次卡(3 批去重 ~9 首,NeteaseSongCard 复用),以及
 * 每日推荐 30 首(主页只有推荐歌单,歌曲是本页增量)。两块并行拉取,空块隐藏。
 * 下拉刷新=换一批;FM 整批入队播放,队列耗尽由 core 的 loadMore 哨兵分支自动续批;
 * 每日推荐独立起播、播完即止。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeteaseMixScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: NeteaseMixViewModel = koinInject(),
) {
    // 头像=当前音源登录账号(SharedViewModel 共享流,与库页同一套口径)
    val sharedViewModel: SharedViewModel = koinInject()
    val accountThumbnail by sharedViewModel.sourceAccountThumbnail.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val heartLoading by viewModel.heartLoading.collectAsStateWithLifecycle()
    val fmLoadingMore by viewModel.fmLoadingMore.collectAsStateWithLifecycle()
    val expressLoading by viewModel.expressLoading.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    // 位移粒度滚动上报(库页同款 scrollReportingEffect,含 canScrollForward 贴底回吸兜底):
    // 旧 index-only+isScrollingUp 上报在贴底 fling 时把末端钳制回吸读成上滑,
    // 底栏/迷你条来回跳(2026-10-13 用户反馈回归;库页 544a7e99/bae1c760 同病同修)
    scrollState.scrollReportingEffect(onScrolling)
    val pullToRefreshState = rememberPullToRefreshState()
    var topAppBarHeightPx by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PullToRefreshBox(
            state = pullToRefreshState,
            onRefresh = { viewModel.refresh(force = true) },
            // 指示器=手势已受理的短确认(≤600ms),不再绑定整页 Loading 态陪跑全部请求
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
            // targetKey 用 state 的"类别"而非实例:Ready→Ready'(追加/垃圾桶的 copy)
            // 不触发过渡,内容原地重组——否则每次追加整页 Crossfade 闪一下。
            Crossfade(state::class) { _ ->
                when (val current = state) {
                    is NeteaseMixViewModel.State.Loading ->
                        Column {
                            Spacer(
                                Modifier.height(
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                                ),
                            )
                            HomeShimmer()
                        }

                    is NeteaseMixViewModel.State.Error ->
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
                            Button(onClick = { viewModel.refresh(force = true) }) {
                                Text(stringResource(Res.string.retry))
                            }
                        }

                    is NeteaseMixViewModel.State.Ready -> {
                        val fmContents = current.fmContents
                        val dailyContents = current.dailyContents
                        val expressContents = current.expressContents
                        val expressArea = current.expressArea
                        val recentContents = current.recentContents
                        LazyColumn(
                            state = scrollState,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
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
                            if (fmContents.isNotEmpty()) {
                                // FM hero:首曲封面大卡 + 开始收听
                                item(key = "hero") {
                                    val heroContent =
                                        fmContents.firstOrNull { it.thumbnails.any { thumb -> thumb.url.isNotBlank() } }
                                            ?: fmContents.first()
                                    FmHeroCard(
                                        content = heroContent,
                                        isFmActive = playback.isFmQueue,
                                        isPlaying = playback.isPlaying,
                                        onStart = { viewModel.playFrom(fmContents, 0) },
                                        onToggle = viewModel::togglePlayback,
                                    )
                                }
                                // 红心电台入口:红心随机起播,播完接 FM 续批
                                item(key = "heart-radio") {
                                    HeartRadioCard(
                                        loading = heartLoading,
                                        isHeartActive = playback.isHeartQueue,
                                        isPlaying = playback.isPlaying,
                                        onClick = viewModel::playHeartRadio,
                                        onToggle = viewModel::togglePlayback,
                                    )
                                }
                                // FM 当前批次:单行横滑(主页歌单行同款排版),滑到尾自动拉下批
                                item(key = "fm-batch") {
                                    FmSongRow(
                                        contents = fmContents,
                                        loadingMore = fmLoadingMore,
                                        onTrash = viewModel::trashFm,
                                        onLoadMore = viewModel::loadMoreFm,
                                    ) { index ->
                                        viewModel.playFrom(fmContents, index)
                                    }
                                }
                            }
                            if (dailyContents.isNotEmpty()) {
                                // 每日推荐:YTM"每日发现"同款 hero 轮播+播放全部(2026-10-12 用户定案,
                                // 替代原三行紧凑网格;30 首全量入轮播,点中间卡从该曲整队起播)
                                item(key = "daily-section") {
                                    NeteaseDailySection(
                                        title = "每日推荐",
                                        contents = dailyContents,
                                        onPlayAll = { viewModel.playSectionFrom(dailyContents, 0, "每日推荐") },
                                        onPlayIndex = { index ->
                                            viewModel.playSectionFrom(dailyContents, index, "每日推荐")
                                        },
                                    )
                                }
                            }
                            if (expressContents.isNotEmpty() || expressLoading) {
                                // 新歌速递:编辑性新歌(与主页"推荐新歌"个性化不同源),地区 chips。
                                // 标题→chips→网格统一 8dp(2026-10-13 用户反馈与主页"新碟上架"
                                // 距离不一致:此前 header/chips 是独立 LazyColumn 项吃 20dp 项距,
                                // 现三项合并一项,节奏与主页行内 8dp 对齐);切地区的小转圈占
                                // 尾钮槽位(ShelfTitleRow trailing),与其它行的占位同位
                                item(key = "express-section") {
                                    Column {
                                        ShelfTitleRow(
                                            title = "新歌速递",
                                            titleColor = MaterialTheme.colorScheme.onBackground,
                                            // 切地区小转圈占尾钮槽(transient)
                                            trailing =
                                                if (expressLoading) {
                                                    {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            strokeWidth = 2.dp,
                                                        )
                                                    }
                                                } else {
                                                    null
                                                },
                                        )
                                        ExpressAreaChips(
                                            selected = expressArea,
                                            enabled = !expressLoading,
                                            modifier = Modifier.padding(top = 8.dp),
                                        ) { area -> viewModel.loadExpress(area) }
                                        if (expressContents.isNotEmpty()) {
                                            FmSongGrid(contents = expressContents, topPadding = 8.dp) { index ->
                                                viewModel.playSectionFrom(expressContents, index, "新歌速递")
                                            }
                                        }
                                    }
                                }
                            }
                            if (recentContents.isNotEmpty()) {
                                // 最近在听:网易侧听歌周榜(按播放次数降序),FM 同款单行两列排版
                                item(key = "recent-header") {
                                    SectionHeader("最近在听")
                                }
                                item(key = "recent-row") {
                                    FmSongRow(contents = recentContents) { index ->
                                        viewModel.playSectionFrom(recentContents, index, "最近在听")
                                    }
                                }
                            }
                            item(key = "end") { EndOfPage() }
                        }
                    }
                }
            }
        }
        // 悬浮顶栏(对照 NeteaseHomeScreen:置顶透明、滚动后 surfaceContainer)
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
                                Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                            },
                        ).onGloballyPositioned { coordinates ->
                            topAppBarHeightPx = coordinates.size.height
                        },
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars),
                )
                // 标题左侧头像=当前音源登录账号(用户 2026-09-30,与库页同款设计:
                // 26dp 圆形+crossfade,空串隐藏;标题 titleMedium 与库页顶栏同字号;
                // 本页无 TopAppBar,手排 Row)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                ) {
                    AnimatedVisibility(
                        !accountThumbnail.isNullOrEmpty(),
                        modifier = Modifier.padding(end = 12.dp),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(accountThumbnail)
                                    .crossfade(550)
                                    .build(),
                            placeholder = rememberVectorPainter(SimpIcons.PeopleAlt),
                            error = rememberVectorPainter(SimpIcons.PeopleAlt),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.personal_fm),
                        style = typo().titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

/** 每日推荐/新歌速递三行紧凑卡网格(与主页歌曲行同款;横向 4dp=每日推荐歌单行基准,纵向 8dp 原节奏) */
@Composable
private fun FmSongGrid(
    contents: List<Content>,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onPlay: (index: Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    LazyHorizontalGrid(
        rows = GridCells.Fixed(3),
        modifier =
            Modifier
                .padding(horizontal = 15.dp)
                .padding(top = topPadding)
                .height(460.dp),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gridItemsIndexed(contents, key = { _, content -> content.videoId ?: content.title }) { index, content ->
            NeteaseSongCard(
                content = content,
                onClick = { onPlay(index) },
            )
        }
    }
}

/** 单行两列横滑卡(主页"每日推荐歌单"行同款 160dp 卡,2026-10-12 用户定案全仓统一该尺寸)。
 *  FM 用完整形态:滑到尾自动拉下一批(onLoadMore)+卡带垃圾桶角标;最近在听复用排版,
 *  两个参数传 null 即无角标无增量(数据一次到位)。 */
@Composable
private fun FmSongRow(
    contents: List<Content>,
    loadingMore: Boolean = false,
    onTrash: ((Content) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    onPlay: (index: Int) -> Unit,
) {
    val rowState = rememberLazyListState()
    // 无感预取(2026-09-30 五轮修,用户反馈"到尾要回划再划"):**预取区深度必须大于
    // 每批增量**——personalRadio 服务端固定每批 3 首,旧区=最后 3 张与批量等深,批落地
    // 后必须把整批滑完才再触发=零跑道,快划必撞墙。现区=最后 6 张(约 3 屏,含一批余量),
    // 并在每批落地时(contents.size 变化)重评估:仍在区内立即链发下一批,跑道自动维持;
    // VM 在飞守卫(fmLoadingMore)防风暴。垃圾桶删歌后 contents 收缩也会重评估自然补拉。
    if (onLoadMore != null) {
        val nearEnd by remember {
            derivedStateOf {
                val info = rowState.layoutInfo
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 6
            }
        }
        // 滚动驱动:进入近尾区触发(布尔边沿,一次进区只打一发;离开再进才重武装)
        LaunchedEffect(rowState) {
            snapshotFlow { nearEnd }
                .distinctUntilChanged()
                .collect { near ->
                    if (near) onLoadMore?.invoke()
                }
        }
        // 落地链发:新批到达时若仍处近尾区立即续拉——区深 6>批 3 保证批落地瞬间
        // nearEnd 仍为 true(用户在旧末尾,新 total-6 通常仍覆盖),跑道逐批+1 直到脱离
        LaunchedEffect(contents.size, nearEnd) {
            if (nearEnd && !loadingMore) onLoadMore?.invoke()
        }
    }
    LazyRow(
        state = rowState,
        modifier = Modifier.padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(contents, key = { _, content -> content.videoId ?: content.title }) { index, content ->
            NeteaseSongCard(
                content = content,
                onClick = { onPlay(index) },
                onTrash = onTrash?.let { trash -> { trash(content) } },
                // 160dp=主页"每日推荐歌单"行同款卡尺寸(2026-10-12 全仓统一)
                cardWidth = 160.dp,
            )
        }
        if (loadingMore) {
            item(key = "fm-loading-more") {
                Box(
                    modifier =
                        Modifier
                            .width(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
            }
        }
    }
}

/** 红心电台入口卡:红心随机 30 首起播,播完接私人FM(心动模式端点已死,本地替代方案)。
 *  播放/暂停**只**作用在尾部播放按钮上(用户定案),整行不响应点击防误触。 */
@Composable
private fun HeartRadioCard(
    loading: Boolean,
    isHeartActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                // 描边(用户 2026-10-12):低对比填充容器在浅色主题下边界发虚,outlineVariant 细线勾出轮廓
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "红心电台",
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "从你的红心歌单随机出发，播完接私人FM",
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        // 尾部槽位必须恒为 IconButton(48dp 触摸目标):loading 时裸换 22dp 转圈会把本行
        // 高度压掉 4dp,卡以下全部分区随 loading 起止上下跳一下(实测 10px)。转圈画在按钮
        // 内保持行高;loading 中的重复点击由 VM 的 heartLoading 守卫挡掉。
        IconButton(onClick = if (isHeartActive) onToggle else onClick) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                com.maxrave.simpmusic.ui.component.PlayBadgeIcon(
                    if (isHeartActive && isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                )
            }
        }
    }
}

/** FM hero 大卡:首曲封面铺底 + 暗部渐变 + 标题/副标题/收听按钮。
 *  播放/暂停**只**作用在收听按钮上(用户定案):FM 队列活动态按钮=暂停/继续收听,
 *  整卡(含封面)不响应点击,防误触。 */
@Composable
private fun FmHeroCard(
    content: Content,
    isFmActive: Boolean,
    isPlaying: Boolean,
    onStart: () -> Unit,
    onToggle: () -> Unit,
) {
    val cardAction = if (isFmActive) onToggle else onStart
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp)),
    ) {
        AsyncImage(
            // 300dp 全宽大卡,数据侧只有 param=500(toMixContent)——请求侧升 1080
            model = content.thumbnails.lastOrNull()?.url?.toHiResArtworkUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                    ),
                ),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
        ) {
            Text(
                text = stringResource(Res.string.personal_fm),
                style = typo().headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.personal_fm_subtitle),
                style = typo().bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = cardAction) {
                com.maxrave.simpmusic.ui.component.PlayBadgeIcon(
                    if (isFmActive && isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isFmActive && isPlaying -> stringResource(Res.string.personal_fm_pause)
                        isFmActive -> stringResource(Res.string.personal_fm_resume)
                        else -> stringResource(Res.string.personal_fm_start)
                    },
                )
            }
        }
    }
}

/** 分区标题(最近在听等):走 ShelfTitleRow——尾钮位(更多/占位)与 MediaRow 头部同几何 */
@Composable
private fun SectionHeader(title: String) {
    ShelfTitleRow(title = title, titleColor = MaterialTheme.colorScheme.onBackground)
}

/** 宽窗口(平板/桌面)hero 轮播阈值:与 YTM 主页 HomeDiscoverSection 同款 */
// 大卡目标宽(上游 9dbef7a0 同款移植):固定而非行宽份额——宽行放下更多张标准大卡,
// 而不是把同样两张拉宽;手机窄于两倍目标宽时仍是一张大卡+两侧窄邻卡
private val DAILY_HERO_TARGET_WIDTH = 360.dp

/** 每日推荐/私人雷达共用 hero 轮播(YTM"每日发现"HomeDiscoverSection 同款):一张大卡
 *  居中+窄邻卡,卡上叠歌名/艺人;点邻卡=带回中间,点中间卡=从该曲整队起播,头部
 *  "播放全部"=从第 1 首起播。2026-10-13 三轮:私人雷达对齐本形态(用户定稿),title 参数化。 */
@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
internal fun NeteaseDailySection(
    title: String,
    contents: List<Content>,
    onPlayAll: () -> Unit,
    onPlayIndex: (Int) -> Unit,
) {
    val state = rememberCarouselState { contents.size }
    val scope = rememberCoroutineScope()
    // hover 缩放(上游 9dbef7a0 同款,桌面鼠标):封面在自身框内放大,不出框
    val (hoverModifier, hovered) = rememberCardHover(enabled = true)
    Column(Modifier.padding(horizontal = 15.dp)) {
        // 头部走 ShelfTitleRow(分区可见即有数据=播放全部的显示语义,与 YTM playAllEndpoint
        // 数据驱动同构;按钮色维持本页原 onSurface)
        ShelfTitleRow(
            title = title,
            horizontalPadding = 0.dp,
            titleColor = MaterialTheme.colorScheme.onBackground,
            moreColor = MaterialTheme.colorScheme.onSurface,
            onPlayAll = onPlayAll,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // 上游同款:窄于两倍目标宽=手机形态一张大卡;宽行由轮播按目标宽自动排更多张
            val heroWidth = (maxWidth * 0.74f).coerceAtMost(DAILY_HERO_TARGET_WIDTH)
            val wide = maxWidth >= 600.dp
            HorizontalCenteredHeroCarousel(
                state = state,
                maxItemWidth = heroWidth,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp),
            ) { index ->
                val song = contents[index]
                val info = carouselItemDrawInfo
                // 邻卡只带回中间;中间卡才是可播的
                val play: () -> Unit = {
                    if (info.isHero()) onPlayIndex(index) else scope.launch { state.animateScrollToItem(index) }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .maskClip(RoundedCornerShape(28.dp))
                        .then(hoverModifier)
                        .clickable { play() },
                ) {
                    AsyncImage(
                        model = song.thumbnails.lastOrNull()?.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().hoverZoom(hovered),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                smoothScrimBrush(
                                    from = Color.Transparent,
                                    to = Color.Black.copy(alpha = 0.78f),
                                    startFraction = 0.45f,
                                ),
                            ),
                    )
                    // 遮罩恒深色,文字用 force-dark 样式(HomeDiscoverSection 同款)
                    ForceDarkContent {
                        Column(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 64.dp, bottom = 20.dp),
                        ) {
                            Text(
                                text = song.title,
                                style = if (wide) typo().titleLarge else typo().headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists?.joinToString(", ") { it.name }.orEmpty(),
                                style = typo().bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// 0 while the item sits in a narrow neighbour slot, 1 once it is the full-size middle card.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun CarouselItemDrawInfo.heroFraction(): Float =
    if (maxSize <= minSize) 1f else ((size - minSize) / (maxSize - minSize)).coerceIn(0f, 1f)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun CarouselItemDrawInfo.isHero(): Boolean = heroFraction() > 0.9f

/** 新歌速递地区 chips(网易原生 areaId);切换期间禁用防连点 */
@Composable
private fun ExpressAreaChips(
    selected: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (area: Int) -> Unit,
) {
    val areas = listOf(0 to "全部", 7 to "华语", 96 to "欧美", 8 to "日语", 16 to "韩语")
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        areas.forEach { (area, label) ->
            Chip(
                isAnimated = !enabled,
                isSelected = area == selected,
                text = label,
            ) {
                if (enabled) onSelect(area)
            }
        }
    }
}
