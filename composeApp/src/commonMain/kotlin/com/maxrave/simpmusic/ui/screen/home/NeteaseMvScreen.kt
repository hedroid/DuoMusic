package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeContentCard
import com.maxrave.simpmusic.ui.component.HomeItemVideo
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.other.ImmersiveMvHeader
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.utils.toHiResArtworkUrl
import com.maxrave.simpmusic.viewModel.NeteaseMvGridSource
import com.maxrave.simpmusic.viewModel.NeteaseMvViewModel
import com.maxrave.simpmusic.viewModel.NeteaseRadarViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.mv_count_short
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.videos

/**
 * MV 二级页(主页"最新 MV/MV 总榜"行、艺人页"视频"行的更多入口):YT 再听一遍更多页
 * (BrowseScreen)同款 Adaptive 160dp 网格,视频卡 16:9 铺满槽宽;点击=playMv 单 MV
 * 队列起播,长按=歌曲菜单(MV 语义行内建)。TOPLIST/ARTIST 近底自动翻页。
 */
@Composable
fun NeteaseMvGridScreen(
    navController: NavController,
    source: String,
    artistId: Long,
    title: String,
    viewModel: NeteaseMvViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedViewModel: SharedViewModel = koinInject()
    LaunchedEffect(source, artistId) { viewModel.load(source, artistId) }

    val gridState = rememberLazyGridState()
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
    LaunchedEffect((state as? NeteaseMvViewModel.State.Ready)?.mvs?.size) {
        val ready = state as? NeteaseMvViewModel.State.Ready ?: return@LaunchedEffect
        if (nearEnd && ready.hasMore && !ready.loadingMore) viewModel.loadMore()
    }

    // 长按菜单(歌曲菜单对 NETEASE_MV_ 哨兵行内建 MV 语义)
    var menuMv by remember { mutableStateOf<Track?>(null) }
    menuMv?.let { mv ->
        NowPlayingBottomSheet(
            onDismiss = { menuMv = null },
            navController = navController,
            song = mv.toSongEntity(),
        )
    }

    NeteaseSecondaryPageScaffold(
        title = title,
        onBack = navController::navigateUp,
    ) {
        Crossfade(state) { current ->
            when (current) {
                is NeteaseMvViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is NeteaseMvViewModel.State.Error ->
                    NeteaseSecondaryErrorColumn(
                        onRetry = { viewModel.load(source, artistId, force = true) },
                    )

                is NeteaseMvViewModel.State.Ready ->
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            current.mvs,
                            key = { index, mv -> "${mv.videoId}-$index" },
                        ) { _, mv ->
                            HomeItemVideo(
                                onClick = { sharedViewModel.playMv(mv) },
                                onLongClick = { menuMv = mv },
                                data = mv.toVideoContent(),
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
    }
}

/**
 * 艺人视频二级页(艺人页"视频"行的更多入口,2026-10-13 用户定稿对齐 YTM):
 * YTM 该入口打开的是歌单页(PlaylistDestination+videoListParam)——封面沉浸头+
 * 播放列表形式,本页同构:沉浸头(首条 MV 封面+"视频"+艺人名)+[随机][播放]按钮行+
 * "N 视频"计数+MV 列表行(三点菜单/播放中着色);/artist/mvs 30/页近底翻页;
 * 顶栏搜索=先翻完剩余页再本地过滤(与收藏 MV 页同口径)。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NeteaseArtistVideosScreen(
    navController: NavController,
    artistId: Long,
    artistName: String,
    viewModel: NeteaseMvViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(artistId) { viewModel.load(NeteaseMvGridSource.ARTIST, artistId) }

    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 8
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadMore()
    }
    LaunchedEffect((state as? NeteaseMvViewModel.State.Ready)?.mvs?.size) {
        val ready = state as? NeteaseMvViewModel.State.Ready ?: return@LaunchedEffect
        if (nearEnd && ready.hasMore && !ready.loadingMore) viewModel.loadMore()
    }

    // 正在播放判定双源(track 在队列 rebuild 窗口恒 null,songEntity 兜底——九轮铁律)
    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.songEntity?.videoId ?: it?.track?.videoId }
    }.collectAsState(null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)
    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingThisList = queueData?.data?.playlistId == NeteaseMvViewModel.NETEASE_ARTIST_VIDEOS_QUEUE_ID

    // 行三点菜单(MV 语义行内建)
    var menuMv by remember { mutableStateOf<Track?>(null) }
    menuMv?.let { mv ->
        NowPlayingBottomSheet(
            onDismiss = { menuMv = null },
            navController = navController,
            song = mv.toSongEntity(),
        )
    }

    // 顶栏三点:列表级动作(添加到队列=先翻完再入队/下载全部),与收藏 MV 页同款
    var showListMenu by remember { mutableStateOf(false) }
    if (showListMenu) {
        com.maxrave.simpmusic.ui.screen.other.MvListMenuSheet(
            onDismiss = { showListMenu = false },
            onAddToQueue = {
                viewModel.withAllMvs { loaded ->
                    if (loaded.isNotEmpty()) {
                        sharedViewModel.addListToQueue(ArrayList(loaded))
                    }
                }
            },
            onDownloadAll = {
                viewModel.requestDownloadAll()
            },
        )
    }

    // 批量下载分类消费(收藏 MV 页同口径):全未下载=直接入队(不弹窗);
    // 含已下载=三选弹窗(跳过=只下未下载/覆盖=已下载的重下)
    val downloadingText = stringResource(Res.string.downloading)
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
            com.maxrave.simpmusic.ui.component.BatchDownloadConfirmDialog(
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

    // 顶栏搜索态:打开=先把剩余页翻完再过滤(歌单页 getFullTracks 同口径)
    var showSearchBar by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            viewModel.withAllMvs()
            listState.animateScrollToItem(0)
        }
    }

    val mvs = (state as? NeteaseMvViewModel.State.Ready)?.mvs.orEmpty()
    val filtered =
        if (query.isEmpty() || !showSearchBar) {
            mvs
        } else {
            mvs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artists?.joinToString(", ")?.contains(query, ignoreCase = true) == true
            }
        }

    // 沉浸头调色板(MvListScreen 同配方:首条封面位图→palette→渐隐底色,防滚动回收重放);
    // 封面升档 toHiResArtworkUrl(16:9 小图→w960-h540/网易 param 保比例宽 1080)——
    // 头图槽位 ~1080 物理像素,源 544/240 直接上采样即糊(2026-10-13 用户"封面模糊")
    val paletteState = rememberPaletteState()
    val headerCover = mvs.firstOrNull()?.thumbnails?.lastOrNull()?.url?.toHiResArtworkUrl()
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && headerCover != null && paletteGeneratedFor != headerCover) {
            paletteState.generate(bm)
            paletteGeneratedFor = headerCover
        }
    }

    Crossfade(state) { current ->
        when (current) {
            is NeteaseMvViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

            is NeteaseMvViewModel.State.Error ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.retry),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load(NeteaseMvGridSource.ARTIST, artistId, force = true) }) {
                        Text(stringResource(Res.string.retry))
                    }
                }

            is NeteaseMvViewModel.State.Ready ->
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier =
                            // fillMaxSize 而非 fillMaxWidth:深色底画在列表自身,搜索过滤后
                            // 内容变短时底色随内容缩水会漏浅色窗口底(歌单页模式 A 同因)
                            Modifier
                                .fillMaxSize()
                                .background(mutedPaletteBg),
                        state = listState,
                    ) {
                        if (!showSearchBar) {
                            item(contentType = "header") {
                                ImmersiveMvHeader(
                                    coverUrl = headerCover,
                                    title = stringResource(Res.string.videos),
                                    authorName = artistName,
                                    paletteBg = mutedPaletteBg,
                                    onBack = navController::navigateUp,
                                    onSearch = { showSearchBar = !showSearchBar },
                                    onMore = { showListMenu = true },
                                    onBitmap = { bitmap = it },
                                )
                            }
                            item(contentType = "actions") {
                                Column(Modifier.padding(horizontal = 32.dp)) {
                                    val isThisPlaying = isPlaying && playingThisList
                                    // stringResource 不能进 onClick 协程/回调——组合期先解析好(项目惯例)
                                    val videosTitle = stringResource(Res.string.videos)
                                    val queueName = artistName.ifBlank { videosTitle }
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
                                                    .clickable { viewModel.playAll(shuffle = true, queueName = queueName) },
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
                                                            sharedViewModel.onUIEvent(com.maxrave.simpmusic.viewModel.UIEvent.PlayPause)
                                                        } else {
                                                            viewModel.playAll(queueName = queueName)
                                                        }
                                                    }
                                                    .padding(horizontal = 20.dp),
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
                                                    // 播放按钮文字不翻译,保持 Play/Pause(用户 2026-10-13 定案)
                                                    text = if (isThisPlaying) "Pause" else "Play",
                                                    color = Color.Black,
                                                    style = typo().labelLarge,
                                                )
                                            }
                                        }
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
                                    // 计数=已加载数累计(端点无服务端总数,翻页自然增长)
                                    Text(
                                        text = stringResource(Res.string.mv_count_short, mvs.size.toString()),
                                        color = Color.White,
                                        style = typo().bodyMedium,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            }
                        } else {
                            item {
                                // 搜索态:列表顶部让位给覆盖层搜索条(近似高度:状态栏+两行 padding+50dp 输入框)
                                Spacer(Modifier.height(140.dp))
                            }
                        }
                        // 行=YTM 歌单页同款 SongFullWidthItems(48dp 方形封面 4dp 圆角+两行文字+三点,
                        // 播放中=Lottie 指示),2026-10-13 用户定稿对齐
                        listItemsIndexed(filtered, key = { _, mv -> mv.videoId }) { _, mv ->
                            com.maxrave.simpmusic.ui.component.SongFullWidthItems(
                                forceDark = true,
                                isPlaying = mv.videoId == playingTrack && isPlaying,
                                track = mv,
                                onMoreClickListener = { menuMv = mv },
                                onClickListener = { sharedViewModel.playMv(mv) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(contentType = "footer") {
                            when {
                                current.loadingMore -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.5.dp, color = Color.White)
                                }

                                else -> EndOfPage()
                            }
                        }
                    }

                    // 搜索条覆盖层(不透明 palette 底兜底防透叠)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSearchBar,
                        enter = SearchBarEnter,
                        exit = SearchBarExit,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(mutedPaletteBg),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        // statusBars 避让(歌单页同款)
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
                                        androidx.compose.runtime.CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
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
                                // 取消:只收起搜索条回列表(返回箭头是退出页面,不能兼任)
                                IconButton(onClick = { showSearchBar = false }) {
                                    Icon(SimpIcons.Close, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
        }
    }
}


/**
 * 私人雷达二级页(主页"私人雷达"行的更多入口):与主页行同源的 30 首雷达歌,
 * BrowseScreen 同款 Adaptive 网格,方形歌曲卡铺满槽宽;点击=单曲电台起播(与行内同语义)。
 */
@Composable
fun NeteaseRadarScreen(
    navController: NavController,
    title: String,
    viewModel: NeteaseRadarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    NeteaseSecondaryPageScaffold(
        title = title,
        onBack = navController::navigateUp,
    ) {
        Crossfade(state) { current ->
            when (current) {
                is NeteaseRadarViewModel.State.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is NeteaseRadarViewModel.State.Error ->
                    NeteaseSecondaryErrorColumn(onRetry = { viewModel.load(force = true) })

                is NeteaseRadarViewModel.State.Ready ->
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            current.contents,
                            key = { index, content -> "${content.videoId ?: content.title}-$index" },
                        ) { _, content ->
                            HomeContentCard(
                                temp = content,
                                navController = navController,
                                viewModel = viewModel,
                                onLongClick = { },
                                fillMaxWidth = true,
                            )
                        }
                        item(key = "end", span = { GridItemSpan(maxLineSpan) }) { EndOfPage() }
                    }
            }
        }
    }
}
