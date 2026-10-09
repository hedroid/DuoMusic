package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.common.NETEASE_MV_MEDIA_ID_PREFIX
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.ui.component.ActionButton
import com.maxrave.simpmusic.ui.component.BatchDownloadConfirmDialog
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfModalBottomSheet
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.MvListUIState
import com.maxrave.simpmusic.viewModel.MvListViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_to_queue
import simpmusic.composeapp.generated.resources.download
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.favorite_mvs
import simpmusic.composeapp.generated.resources.mv_count_short
import simpmusic.composeapp.generated.resources.no_favorite_mvs
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.search

/**
 * 收藏的 MV 页(2026-10-08 二稿,用户定案:对齐歌单详情页 PlaylistScreen 同构)——
 * 沉浸式半屏头图(首条 MV 封面+调色板底色)+液态玻璃顶栏(返回+搜索+更多)、
 * [随机][播放][下载]按钮行、16:9 行卡带三点菜单(NowPlayingBottomSheet,MV 语义行已内建)、
 * 顶栏搜索=本地过滤已加载条目。分页/页脚重试沿用一稿口径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvListScreen(
    @Suppress("UNUSED_PARAMETER") innerPadding: PaddingValues,
    navController: NavController,
    viewModel: MvListViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    // 正在播放判定双源(track 在队列 rebuild 窗口恒 null,songEntity 兜底——九轮铁律)
    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.songEntity?.videoId ?: it?.track?.videoId }
    }.collectAsState(null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)
    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingThisList = queueData?.data?.playlistId == MvListViewModel.MV_SUBLIST_QUEUE_ID

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var searchBarHeightPx by remember { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuTarget by remember { mutableStateOf<Track?>(null) }
    var showListMenu by remember { mutableStateOf(false) }
    // 列表内取消收藏(行菜单红心)→服务端对账确认后原地移行:MV 写路径不乐观翻转
    // (对账铁律),onLikeChanged 只登记目标,likeWriteResults(带目标 id+成败)落定再动行;
    // 失败保留行,toast 由写路径自报
    var pendingUnlikeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        sharedViewModel.likeWriteResults.collect { result ->
            val pending = pendingUnlikeId ?: return@collect
            if (result.videoId == pending) {
                if (result.success && !result.liked) {
                    viewModel.removeMv(pending)
                }
                pendingUnlikeId = null
            }
        }
    }

    // 别处(迷你条/播放页)收藏/取消 MV → 开着的本页原地增删行(实时档);
    // 取消幂等(与 pendingUnlikeId 路径重叠时 filterNot 无害)
    LaunchedEffect(Unit) {
        sharedViewModel.likeChangeEvent.collect { ev ->
            if (ev.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                if (ev.liked) {
                    ev.track?.let(viewModel::insertMv)
                } else {
                    viewModel.removeMv(ev.videoId)
                }
            }
        }
    }

    // stringResource 不能在 onClick 协程/回调里取——组合期先解析好(项目惯例)
    val emptyText = stringResource(Res.string.no_favorite_mvs)
    val downloadingText = stringResource(Res.string.downloading)

    val mvs = (uiState as? MvListUIState.Success)?.mvs.orEmpty()
    // 本地过滤(歌单页同款):直接计算,不走 remember+derivedStateOf——mvs 是普通局部
    // val,lambda 捕获的是首帧值,翻页新批次会被旧闭包吞掉
    val filteredMvs =
        if (query.isEmpty() || !showSearchBar) {
            mvs
        } else {
            mvs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artists?.joinToString(", ")?.contains(query, ignoreCase = true) == true
            }
        }

    // 近底触发下一页(剩 8 行内);翻页失败后被 loadMoreFailed 挡住,改由页脚"重试"驱动。
    // uiState 是 collectAsStateWithLifecycle 的 by 委托(State 读取),derivedStateOf 里
    // 每次计算都会重读它——依赖被正确跟踪,与 MoreSongsScreen(线上验证过分页)同款
    // 写法;不是捕获首帧值的普通局部 val(那种才是 lambda 闭包陷阱)
    val shouldLoadMore by remember {
        derivedStateOf {
            val state = uiState as? MvListUIState.Success ?: return@derivedStateOf false
            state.hasMore &&
                !state.loadingMore &&
                !state.loadMoreFailed &&
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?.let { it >= listState.layoutInfo.totalItemsCount - 8 } == true
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val firstItemVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) { shouldHideTopBar = !firstItemVisible }

    // 打开搜索=先拉全再过滤+滚回顶部(歌单页 getFullTracks+scrollToItem 同款):
    // 收藏总数大于已加载页数时只过滤已加载条目会漏结果;列表停在深处时搜索态
    // 顶部换成 Spacer,不回顶视口悬在半空。withAllMvs 与近底翻页在 VM 锁内串行
    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            viewModel.withAllMvs { }
            listState.animateScrollToItem(0)
        }
    }

    val paletteState = rememberPaletteState()
    val headerCover = mvs.firstOrNull()?.thumbnails?.lastOrNull()?.url
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    // 防滚动回收重放:同封面只生成一次调色板(歌单页同款护栏)
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && headerCover != null && paletteGeneratedFor != headerCover) {
            paletteState.generate(bm)
            paletteGeneratedFor = headerCover
        }
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = uiState) { state ->
            when (state) {
                is MvListUIState.Success -> {
                    LazyColumn(
                        modifier =
                            Modifier
                                // fillMaxSize 而非 fillMaxWidth:深色底画在 LazyColumn 自身,
                                // 搜索过滤后内容变短时底色随内容缩水会漏浅色窗口底
                                // (歌单页模式 A 白屏同因,31da652e)
                                .fillMaxSize()
                                .background(mutedPaletteBg)
                                .hazeSource(state = hazeState),
                        state = listState,
                    ) {
                        if (!showSearchBar) {
                            item(contentType = "header") {
                                ImmersiveMvHeader(
                                    coverUrl = headerCover,
                                    paletteBg = mutedPaletteBg,
                                    onBack = navController::navigateUp,
                                    onSearch = { showSearchBar = !showSearchBar },
                                    onMore = { showListMenu = true },
                                    onBitmap = { bitmap = it },
                                )
                            }
                            item(contentType = "actions") {
                                // 歌单详情页同构:按钮行+数量行包在 32dp 缩进 Column 里,
                                // 数量"N 个 MV"与歌单页"N 音轨"同位(按钮行下、左对齐)
                                Column(Modifier.padding(horizontal = 32.dp)) {
                                // [随机][播放/暂停][下载] 与歌单详情页按钮行同构(居中 48dp 档)
                                val isThisPlaying = isPlaying && playingThisList
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .clickable { viewModel.playAll(shuffle = true) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Shuffle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Box(
                                        modifier =
                                            Modifier
                                                .height(48.dp)
                                                .widthIn(min = 110.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .clickable {
                                                    if (isThisPlaying) {
                                                        sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                    } else {
                                                        viewModel.playAll()
                                                    }
                                                }.padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isThisPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(22.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isThisPlaying) "Pause" else "Play",
                                                color = Color.Black,
                                                style = typo().labelLarge,
                                            )
                                        }
                                    }
                                    // 下载全部=对齐歌单三选口径(2026-10-08):分类在 VM——
                                    // 全部已下载 toast/部分已下载三选弹窗/全未下载直接入队;
                                    // 进度弹窗只在 Info 面板上下文可见,反馈=toast+管理页落行
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .clickable { viewModel.requestDownloadAll() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.DownloadForOffline,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                // 数量行(歌单页"N 音轨"同位同款:bodyMedium/白/左对齐/
                                // vertical 8dp);计数=服务端收藏总数(mv/sublist count),
                                // 翻完页前大于已加载数;未知(0)回落已加载数
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.mv_count_short,
                                            (state.totalCount.takeIf { it > 0 } ?: mvs.size).toString(),
                                        ),
                                    color = Color.White,
                                    style = typo().bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                if (mvs.isEmpty()) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = emptyText,
                                            style = typo().bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                                }
                            }
                        } else {
                            item {
                                val density = LocalDensity.current
                                Spacer(
                                    Modifier.height(
                                        with(density) { searchBarHeightPx.toDp() },
                                    ),
                                )
                            }
                        }
                        itemsIndexed(filteredMvs, key = { _, mv -> mv.videoId }) { index, mv ->
                            Column(modifier = Modifier.animateItem()) {
                                MvPlaylistRow(
                                    title = mv.title,
                                    artist = mv.artists?.joinToString { it.name } ?: "",
                                    coverUrl = mv.thumbnails?.lastOrNull()?.url,
                                    isPlaying = mv.videoId == playingTrack && isPlaying,
                                    onClick = { sharedViewModel.playMv(mv) },
                                    onMore = { menuTarget = mv },
                                )
                                if (index < filteredMvs.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 128.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                    )
                                }
                            }
                        }
                        item(contentType = "footer") {
                            when {
                                state.loadingMore -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.5.dp, color = Color.White)
                                }

                                state.loadMoreFailed -> TextButton(
                                    onClick = viewModel::retryLoadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.retry),
                                        style = typo().labelMedium,
                                        color = Color.White,
                                    )
                                }

                                else -> EndOfPage()
                            }
                        }
                    }

                    // 搜索条覆盖层(歌单页同款:不透明 palette 底兜底防 haze 白带)
                    AnimatedVisibility(
                        visible = showSearchBar,
                        enter = SearchBarEnter,
                        exit = SearchBarExit,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { searchBarHeightPx = it.size.height }
                                .background(mutedPaletteBg)
                                .hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(mutedPaletteBg, 0.55f)),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        // statusBars 避让(歌单页同款):不加的话搜索条直接顶进
                                        // 状态栏,返回/关闭按钮被时间电池图标压住
                                        .windowInsetsPadding(WindowInsets.statusBars),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                            RippleIconButton(
                                imageVector = SimpIcons.ArrowBackIosNew,
                            ) {
                                navController.navigateUp()
                            }
                                SearchBar(
                                    modifier =
                                        Modifier
                                            .height(50.dp)
                                            .padding(horizontal = 12.dp)
                                            .weight(1f),
                                    colors =
                                        SearchBarDefaults.colors().copy(
                                            containerColor = Color.Transparent,
                                        ),
                                    inputField = {
                                        CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                                            SearchBarDefaults.InputField(
                                                query = query,
                                                onQueryChange = { query = it },
                                                onSearch = { showSearchBar = false },
                                                expanded = showSearchBar,
                                                onExpandedChange = { showSearchBar = it },
                                                placeholder = {
                                                    Text(
                                                        stringResource(Res.string.search),
                                                        style = typo().bodyMedium,
                                                    )
                                                },
                                            )
                                        }
                                    },
                                    expanded = false,
                                    onExpandedChange = {},
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                ) {
                                }
                                IconButton(
                                    onClick = { showSearchBar = !showSearchBar },
                                ) {
                                    Icon(SimpIcons.Close, null, tint = Color.White)
                                }
                            }
                        }
                    }

                    // 滚动后收起的顶栏(标题 marquee + 返回 + 搜索)
                    AnimatedVisibility(
                        visible = shouldHideTopBar && !showSearchBar,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                    ) {
                        TopAppBar(
                            windowInsets =
                                TopAppBarDefaults.windowInsets.exclude(
                                    TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                                ),
                            title = {
                                Text(
                                    text = stringResource(Res.string.favorite_mvs),
                                    style = typo().titleMedium,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    RippleIconButton(
                                        SimpIcons.ArrowBackIosNew,
                                        Modifier.size(32.dp),
                                        true,
                                    ) {
                                        navController.navigateUp()
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showSearchBar = !showSearchBar },
                                ) {
                                    Icon(SimpIcons.Search, null, tint = Color.White)
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                ),
                            modifier =
                                Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(mutedPaletteBg, 0.55f)),
                        )
                    }
                }

                MvListUIState.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is MvListUIState.Error ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(mutedPaletteBg)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = typo().bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Button(onClick = viewModel::refresh) {
                                Text(stringResource(Res.string.retry))
                            }
                        }
                    }
            }
        }
    }

    // 行三点菜单:与歌单行同款——Track→SongEntity,歌曲语义行(下载音频/电台/相似等)
    // 由 NowPlayingBottomSheet 按 MV 前缀内建隐藏
    menuTarget?.let { mv ->
        NowPlayingBottomSheet(
            onDismiss = {
                menuTarget = null
            },
            navController = navController,
            song = mv.toSongEntity(),
            // 列表内取消收藏:登记目标等对账确认移行(见 pendingUnlikeId)
            onLikeChanged = { liked ->
                if (!liked) pendingUnlikeId = mv.videoId
            },
        )
    }

    // 顶栏三点:添加到队列(不打断播放)/下载全部(与按钮行同款入队)
    if (showListMenu) {
        MvListMenuSheet(
            onDismiss = { showListMenu = false },
            onAddToQueue = {
                // 与"播放全部"同口径:先把剩余页拉全再入队(显式全量动作不缩水),
                // 拉取期间页脚有 loadingMore spinner
                viewModel.withAllMvs { loaded ->
                    if (loaded.isEmpty()) {
                        viewModel.makeToast(emptyText)
                    } else {
                        sharedViewModel.addListToQueue(loaded.toCollection(arrayListOf()))
                    }
                }
            },
            onDownloadAll = {
                viewModel.requestDownloadAll()
            },
        )
    }

    // 批量下载分类结果消费(2026-10-08 对齐歌单口径):全未下载=直接入队(不弹窗);
    // 含已下载=三选弹窗(跳过=只下未下载/覆盖=已下载的重下);全部已下载在 VM 已 toast
    val mvBatchRequest by viewModel.mvBatchRequest.collectAsStateWithLifecycle()
    LaunchedEffect(mvBatchRequest) {
        val req = mvBatchRequest ?: return@LaunchedEffect
        if (req.downloaded.isEmpty()) {
            viewModel.dismissMvBatchRequest()
            if (req.notDownloaded.isNotEmpty()) {
                viewModel.makeToast(downloadingText)
                req.notDownloaded.forEach { sharedViewModel.downloadMvFile(it.videoId) }
            }
        }
    }
    mvBatchRequest?.let { req ->
        if (req.downloaded.isNotEmpty()) {
            BatchDownloadConfirmDialog(
                downloadedCount = req.downloaded.size,
                onSkip = {
                    viewModel.dismissMvBatchRequest()
                    if (req.notDownloaded.isNotEmpty()) {
                        viewModel.makeToast(downloadingText)
                        req.notDownloaded.forEach { sharedViewModel.downloadMvFile(it.videoId) }
                    }
                },
                onOverwrite = {
                    viewModel.dismissMvBatchRequest()
                    viewModel.makeToast(downloadingText)
                    (req.notDownloaded + req.downloaded).forEach { sharedViewModel.downloadMvFile(it.videoId) }
                },
                onDismiss = viewModel::dismissMvBatchRequest,
            )
        }
    }
}

/**
 * 沉浸式头图(PlaylistScreen 竖屏分支同构):封面+底部渐隐+标题+液态玻璃顶栏。
 * 高度按 16:9 封面比例(wDP*9/16)而非半屏——歌曲头图是方形图 Crop 铺满半屏,
 * MV 封面是 16:9 宽幅,半屏区域前景只占中段、顶栏浮在空白上失去沉浸感;
 * 缩到 16:9 后封面铺满头部,返回/搜索按钮压在封面上(与歌曲页同感受)。
 * 双层保留:同图 Crop+blur 垫底,前景 Fit(非 16:9 原图如 1.91:1 也完整)。
 */
@Composable
private fun ImmersiveMvHeader(
    coverUrl: String?,
    paletteBg: Color,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
    onBitmap: (ImageBitmap?) -> Unit,
) {
    val screenInfo = getScreenSizeInfo()
    val artworkBackdrop = rememberBackdrop(Color.Black)
    val headerHeight = screenInfo.wDP * 9 / 16
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(headerHeight.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(artworkBackdrop)) {
            // blur 垫底必须包在 clipToBounds 的 Box 里:Unbounded 模糊的辉光外扩
            // 约一个半径(~210px),修饰链上挂 clipToBounds 会被相邻 graphicsLayer
            // 合并掉裁不动(A/B 实测),漏进下方列表区形成亮带+头部底边水平分界线
            // (封面与按钮行"泾渭分明"的真身);嵌套 Box 是硬边界,稳裁
            Box(Modifier.fillMaxSize().clipToBounds()) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(coverUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(coverUrl)
                            .crossfade(false)
                            .build(),
                    placeholder = rememberHolderPainter(isVideo = true),
                    error = rememberHolderPainter(isVideo = true),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { onBitmap(it.result.image.toImageBitmap()) },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .blur(80.dp, BlurredEdgeTreatment.Unbounded),
                )
            }
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                placeholder = rememberHolderPainter(isVideo = true),
                error = rememberHolderPainter(isVideo = true),
                // 贴底不居中:图下缘=头部底缘,直接伸进 scrim 渐变带(歌曲页封面同构),
                // 封面原色平滑渐隐进列表背景——居中的话超宽图下缘悬在半空,与按钮
                // 区之间隔一条"图→模糊带→渐变带"的断层(用户实测"泾渭分明")
                modifier =
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.BottomCenter),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // 渐变带加长(0.35f→0.5f,81→115dp):头部比歌曲页矮一半,
                        // 同比例的渐变带太短,封面色到 palette 背景的过渡仓促
                        .height((headerHeight * 0.5f).dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(paletteBg)),
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.favorite_mvs),
                    style = typo().titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // 顶栏:返回(左)+液态玻璃药丸[搜索][更多](右)——歌单页同款(收藏心对 MV 合集
        // 无语义,药丸里不放心)
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiquidGlassIconButton(
                backdrop = artworkBackdrop,
                imageVector = SimpIcons.ArrowBackIosNew,
                modifier = Modifier.size(48.dp),
            ) {
                onBack()
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier =
                    Modifier
                        .height(48.dp)
                        .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSearch) {
                    Icon(SimpIcons.Search, null, tint = Color.White)
                }
                IconButton(onClick = onMore) {
                    Icon(SimpIcons.MoreVert, contentDescription = "More", tint = Color.White)
                }
            }
        }
    }
}

/** MV 行(歌单行同构):16:9 封面+标题/艺人两行+三点;播放中标题转主题色 */
@Composable
private fun MvPlaylistRow(
    title: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = rememberHolderPainter(isVideo = true),
            error = rememberHolderPainter(isVideo = true),
            modifier =
                Modifier
                    .size(width = 104.dp, height = 58.dp)
                    .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = typo().titleSmall,
                color =
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = typo().bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore) {
            Icon(SimpIcons.MoreVert, contentDescription = "More", tint = Color.White)
        }
    }
}

/** 顶栏三点菜单(PlaylistBottomSheet 同壳):添加到队列 / 下载全部 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MvListMenuSheet(
    onDismiss: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownloadAll: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.5f),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier = Modifier.width(60.dp).height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                ActionButton(
                    icon = SimpIcons.QueueMusic,
                    text = Res.string.add_to_queue,
                ) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                    onAddToQueue()
                }
                ActionButton(
                    icon = SimpIcons.DownloadForOffline,
                    text = Res.string.download,
                ) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                    onDownloadAll()
                }
                EndOfModalBottomSheet()
            }
        }
    }
}
