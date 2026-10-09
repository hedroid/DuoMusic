package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kyant.backdrop.highlight.Highlight
import com.kmpalette.rememberPaletteState
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.ui.component.DownloadingIndicator
import com.maxrave.simpmusic.ui.component.BatchDownloadConfirmDialog
import com.maxrave.simpmusic.ui.component.selection.SongSelectionAction
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.common.NETEASE_MV_MEDIA_ID_PREFIX
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.utils.toHiResArtworkUrl
import com.maxrave.common.NETEASE_CLOUD_DISK_PLAYLIST_ID
import com.maxrave.simpmusic.expect.ui.multiFilePickerResult
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Cloud
import com.maxrave.simpmusic.ui.icon.CloudUpload
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.utils.CloudDiskArt
import com.maxrave.simpmusic.ui.utils.cloudCoverSubduedPainter
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.PlaylistUIEvent
import com.maxrave.simpmusic.viewModel.PlaylistUIState
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.partially_loaded
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.delete_from_cloud_disk
import simpmusic.composeapp.generated.resources.delete_from_cloud_disk_title
import simpmusic.composeapp.generated.resources.delete_from_cloud_disk_message
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.cloud_upload_progress_title
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.unlimited
import simpmusic.composeapp.generated.resources.upload_to_cloud_disk

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    playlistId: String,
    isYourYouTubePlaylist: Boolean,
    navController: NavController,
) {
    // Home shelves navigate with the browseEndpoint id, which is "VL" + the playlist id
    // (HomeParser reads title.runs[0].navigationEndpoint.browseEndpoint.browseId). Every radio
    // prefix check and the watch endpoint expect the bare id, so normalise once on the way in
    // rather than stripping "VL" again at each consumer.
    val id = playlistId.removePrefix("VL")
    val tag = "PlaylistScreen"

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val listColors by viewModel.listColors.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    // 自建歌单/网易红心歌单没有收藏语义,顶栏心隐藏(PlaylistViewModel.favoriteAvailable)
    val favoriteAvailable by viewModel.favoriteAvailable.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val tracksListState by viewModel.tracksListState.collectAsStateWithLifecycle()

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var searchBarHeightPx by remember { mutableStateOf(0) }

    // 云盘批量上传:SAF 多选,回调进 VM 逐首串行(进度弹窗由 cloudUploadProgress 驱动)
    val uploadPicker =
        multiFilePickerResult(mimeType = "audio/*") { uris ->
            if (uris.isNotEmpty()) viewModel.uploadToCloudDisk(uris)
        }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    var showCloudDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // 单曲三点菜单的云盘删除目标(SongEntity 形状,带歌名)
    var pendingCloudDeleteSong by remember { mutableStateOf<SongEntity?>(null) }

    val filteredTrack by remember {
        derivedStateOf {
            if (query.isEmpty() || !showSearchBar) {
                tracks
            } else {
                tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artists?.joinToString(", ")?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        Logger.d(tag, "uiState hash: ${uiState.hashCode()}")
        Logger.d(tag, "uiState data: ${uiState.data}")
    }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            viewModel.getFullTracks { _, _ -> }
            lazyState.animateScrollToItem(0)
        }
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                tracksListState != ListState.PAGINATION_EXHAUST &&
                    (
                        lazyState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (lazyState.layoutInfo.totalItemsCount - 6)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d(tag, "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d(tag, "tracksListState: $tracksListState")
        Logger.d(tag, "Continuation: $continuation")
        if (shouldStartPaginate.value && tracksListState == ListState.IDLE) {
            viewModel.getContinuationTrack(
                id,
                continuation,
            )
        }
    }

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    var currentItem by remember {
        mutableStateOf<Track?>(null)
    }

    // 红心歌单判定在取消回调发生时做(见 onLikeChanged):页面数据那时必然已就位;
    // 组合期预判会被 Loading 态抢先钉死成 false
    val heartCheckScope = rememberCoroutineScope()

    // 页内行菜单取消红心:登记目标等写落定(对账确认)再移行,失败保留行(见 onLikeChanged)
    var pendingUnlikeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        sharedViewModel.likeWriteResults.collect { result ->
            val pending = pendingUnlikeId ?: return@collect
            if (result.videoId == pending) {
                if (result.success && !result.liked) {
                    heartCheckScope.launch {
                        if (viewModel.isNeteaseLikedPlaylist()) {
                            viewModel.removeTrackLocally(pending)
                        }
                    }
                }
                pendingUnlikeId = null
            }
        }
    }

    // 别处(迷你条/播放页)红心 → 开着的红心歌单页原地增删行(Spotify 式实时档);
    // 仅红心歌单消费,普通歌单取消红心≠移出歌单
    LaunchedEffect(Unit) {
        sharedViewModel.likeChangeEvent.collect { ev ->
            if (!ev.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                if (viewModel.isNeteaseLikedPlaylist()) {
                    if (ev.liked) {
                        ev.track?.let(viewModel::insertTrackAtTop)
                    } else {
                        viewModel.removeTrackLocally(ev.videoId)
                    }
                }
            }
        }
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            PlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = tracks.firstOrNull { it.videoId == videoId }
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.data?.id) {
            Logger.w(tag, "new id: $id")
            viewModel.getData(id, isYourYouTubePlaylist)
        }
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val paletteState = rememberPaletteState()
    val hazeState =
        rememberHazeState()
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    // Track which thumbnail URL we've already extracted a palette from.
    // Prevents palette flash when LazyColumn recycles the header item on scroll —
    // AsyncImage re-mount fires onSuccess again, but we skip the regenerate.
    var paletteGeneratedFor by remember {
        mutableStateOf<String?>(null)
    }
    val currentThumbnail = (uiState as? PlaylistUIState.Success)?.data?.thumbnail

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && currentThumbnail != null && paletteGeneratedFor != currentThumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentThumbnail
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), Color.Black))
            }
    }

    // Apple Music-inspired immersive treatment. Which header is used depends on the window's
    // aspect ratio alone, not on the platform: a portrait window (a phone held upright, or a
    // narrow desktop window) gets the edge-to-edge artwork header, a landscape one gets the
    // side-by-side header. Everything else on the page — the palette background, the row
    // dividers, the blurred top bar — is shared by both.
    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP
    val dominantColor = listColors.firstOrNull() ?: Color.Black
    // Apple Music-style page background from the artwork's dominant tone (see UIExt.toImmersiveBackground).
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
//    Box {
    Crossfade(
        targetState = uiState,
    ) { state ->
        Logger.w(tag, "State hash: ${state.hashCode()}")
        when (state) {
            is PlaylistUIState.Success -> {
                val data = state.data
                Logger.d(tag, "data: $data")
                if (data == null) return@Crossfade
                // 网易歌单(红心/自建/收藏)的作者=用户 id,跳艺人页无语义(点击实际
                // 无反应/空页)——作者行做纯文本;YT 歌单作者=艺人频道 id,保留可点
                val authorClickable = data.id.toLongOrNull() == null
                // 云盘伪歌单:顶栏露上传按钮,藏收藏心;整单下载按钮保留(VM 侧云盘分支不落 Room)
                val isCloudDisk = data.id == NETEASE_CLOUD_DISK_PLAYLIST_ID
                // 无封面曲目行(未匹配歌 thumbnails 空)/头图兜底:安静版深灰+半透明云
                // (行内兜底成排重复,高饱和蓝会扎眼——业界通用=中性低饱和底+小图标)
                val cloudCoverFallback = if (isCloudDisk) remember { cloudCoverSubduedPainter() } else null
                val hazeState =
                    rememberHazeState()
                LazyColumn(
                    modifier =
                        Modifier
                            // fillMaxSize 而非 fillMaxWidth:页面深色底画在 LazyColumn 自身,
                            // 搜索过滤后内容变短时它会被重测成内容高度(实测 506 行→1 行时
                            // 2424→1250),底色随内容一起缩,之下漏出浅色窗口底=「下半部白屏」
                            // (红心歌单搜歌必现)。钉满视口高度,短内容时底色仍然全屏。
                            .fillMaxSize()
                            .background(mutedPaletteBg)
                            .hazeSource(hazeState),
                    state = lazyState,
                ) {
                    if (!showSearchBar) {
                        item(contentType = "header") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .background(Color.Transparent)
                                        .animateItem(),
                            ) {
                                Column(
                                    Modifier
                                        .background(Color.Transparent),
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                    ) {
                                        if (isPortrait) {
                                            // Apple Music-style: edge-to-edge artwork + liquid glass buttons.
                                            // Glass buttons MUST be siblings of the backdrop source (not children)
                                            // to avoid render feedback loop / RuntimeShader crash.
                                            val artworkBackdrop = rememberBackdrop(Color.Black)
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height((screenInfo.hDP / 2).dp),
                                            ) {
                                                // Inner Box — backdrop SOURCE (artwork + overlays only, NO glass)
                                                Box(modifier = Modifier.fillMaxSize().layerBackdrop(artworkBackdrop)) {
                                                    // 头图全屏宽展示,数据侧 URL 只有 544/500 —— 请求侧升 1080
                                                    // 云盘头图=第一首歌封面(thumbnail 由 VM 填);空云盘/未匹配
                                                    // 歌无封面,走蓝底白云兜底不落音符占位
                                                    val hiResThumb = data.thumbnail.toHiResArtworkUrl()
                                                    AsyncImage(
                                                        model =
                                                            ImageRequest
                                                                .Builder(LocalPlatformContext.current)
                                                                .data(hiResThumb)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(hiResThumb)
                                                                .memoryCacheKey(hiResThumb)
                                                                .crossfade(false)
                                                                .build(),
                                                        placeholder = cloudCoverFallback ?: rememberHolderPainter(),
                                                        error = cloudCoverFallback ?: rememberHolderPainter(),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        onSuccess = {
                                                            bitmap = it.result.image.toImageBitmap()
                                                        },
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                    // Scrim spans 70% of the artwork (not a fixed 200dp): the
                                                    // shorter the ramp, the steeper the alpha, and a steep ramp
                                                    // is what makes the fade read as an edge. See
                                                    // artworkScrimBrush for the curve itself.
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height((screenInfo.hDP * 0.35f).dp)
                                                                .align(Alignment.BottomCenter)
                                                                .background(artworkScrimBrush(mutedPaletteBg)),
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
                                                            text = data.title,
                                                            style = typo().titleLarge,
                                                            color = Color.White,
                                                            maxLines = 2,
                                                            textAlign = TextAlign.Center,
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        if (authorClickable) {
                                                            CompositionLocalProvider(
                                                                LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                                                            ) {
                                                                TextButton(
                                                                    modifier =
                                                                        Modifier
                                                                            .wrapContentHeight()
                                                                            .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
                                                                    contentPadding = PaddingValues(vertical = 1.dp),
                                                                    onClick = {
                                                                        if (data.author.id.isNotEmpty()) {
                                                                            navController.navigate(
                                                                                ArtistDestination(
                                                                                    data.author.id,
                                                                                ),
                                                                            )
                                                                        }
                                                                    },
                                                                ) {
                                                                    Text(
                                                                        text = data.author.name,
                                                                        style = typo().titleSmall,
                                                                        color = Color.White,
                                                                        textAlign = TextAlign.Center,
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            // 网易歌单(红心/自建/收藏):作者行纯文本(2026-10-09
                                                            // 用户定案)——样式与可点版一致,仅去掉无效跳转
                                                            Text(
                                                                text = data.author.name,
                                                                style = typo().titleSmall,
                                                                color = Color.White,
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        // 云盘不是歌单,"歌单/电台 • 年份"行不渲染(2026-10-09 用户反馈)
                                                        if (!isCloudDisk) {
                                                            Text(
                                                                text = "${
                                                                    if (data.isRadio) {
                                                                        stringResource(Res.string.radio)
                                                                    } else {
                                                                        stringResource(Res.string.playlist)
                                                                    }
                                                                } • ${data.year}",
                                                                style = typo().bodyMedium,
                                                                color = Color(0xC4FFFFFF),
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                    }
                                                }
                                                // Back + Heart + Search button overlays on artwork top — liquid glass
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
                                                        modifier =
                                                            Modifier
                                                                .size(48.dp),
                                                    ) {
                                                        navController.navigateUp()
                                                    }
                                                    Spacer(Modifier.weight(1f))
                                                    Row(
                                                        modifier =
                                                            Modifier
                                                                .height(48.dp)
                                                                .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        if (!data.isRadio && favoriteAvailable) {
                                                            Box(
                                                                modifier = Modifier.size(48.dp),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                HeartCheckBox(
                                                                    size = 28,
                                                                    checked = liked,
                                                                    onStateChange = {
                                                                        viewModel.onUIEvent(PlaylistUIEvent.Favorite)
                                                                    },
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                showSearchBar = !showSearchBar
                                                            },
                                                        ) {
                                                            Icon(SimpIcons.Search, null, tint = Color.White)
                                                        }
                                                        if (isCloudDisk) {
                                                            IconButton(
                                                                onClick = { uploadPicker.launch() },
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.CloudUpload,
                                                                    contentDescription = stringResource(
                                                                        Res.string.upload_to_cloud_disk,
                                                                    ),
                                                                    tint = Color.White,
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = onPlaylistMoreClick,
                                                        ) {
                                                            Icon(
                                                                imageVector = SimpIcons.MoreVert,
                                                                contentDescription = "More",
                                                                tint = Color.White,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Apple Music desktop header: back and the overlay actions on
                                            // their own top row, then a square artwork with the text column
                                            // and the action cluster laid out beside it rather than under it.
                                            // Built exactly like the portrait branch, which renders correctly on both
                                            // platforms: the backdrop SOURCE is the content column — artwork included,
                                            // so the recorded layer holds something to refract — and the glass buttons
                                            // are SIBLINGS placed with align(), never children of the source (that
                                            // nesting is the render-feedback loop that kills the RuntimeShader).
                                            val headerBackdrop = rememberBackdrop(mutedPaletteBg)
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Column(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .layerBackdrop(headerBackdrop)
                                                            .windowInsetsPadding(WindowInsets.statusBars)
                                                            .padding(horizontal = 32.dp, vertical = 16.dp),
                                                ) {
                                                    // Reserves the strip the sibling glass buttons are drawn over.
                                                    Spacer(modifier = Modifier.height(48.dp))
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                                        verticalAlignment = Alignment.Top,
                                                    ) {
                                                        // 280dp≈735px 槽位,同样请求侧升 1080(与竖屏头图同源同缓存)
                                                        // 云盘头图=第一首歌封面,无封面走蓝底白云兜底
                                                        val landscapeThumb = data.thumbnail.toHiResArtworkUrl()
                                                        AsyncImage(
                                                            model =
                                                                ImageRequest
                                                                    .Builder(LocalPlatformContext.current)
                                                                    .data(landscapeThumb)
                                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                                    .diskCacheKey(landscapeThumb)
                                                                    .memoryCacheKey(landscapeThumb)
                                                                    .crossfade(false)
                                                                    .build(),
                                                            placeholder = cloudCoverFallback ?: rememberHolderPainter(),
                                                            error = cloudCoverFallback ?: rememberHolderPainter(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            onSuccess = {
                                                                bitmap = it.result.image.toImageBitmap()
                                                            },
                                                            modifier =
                                                                Modifier
                                                                    .size(280.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                        )
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                        ) {
                                                            Text(
                                                                text = data.title,
                                                                style = typo().headlineSmall,
                                                                color = Color.White,
                                                                maxLines = 2,
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = data.author.name,
                                                                style = typo().titleMedium,
                                                                // The app accent, standing in for the brand red
                                                                // Apple uses on this line.
                                                                color = seed,
                                                                modifier =
                                                                    // 网易歌单作者行不可点(与竖屏分支同判定)
                                                                    if (authorClickable) {
                                                                        Modifier.clickable {
                                                                            if (data.author.id.isNotEmpty()) {
                                                                                navController.navigate(
                                                                                    ArtistDestination(
                                                                                        data.author.id,
                                                                                    ),
                                                                                )
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Modifier
                                                                    },
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = "${
                                                                    if (data.isRadio) {
                                                                        stringResource(Res.string.radio)
                                                                    } else {
                                                                        stringResource(Res.string.playlist)
                                                                    }
                                                                } • ${data.year}",
                                                                style = typo().labelMedium,
                                                                color = Color(0xC4FFFFFF),
                                                            )
                                                            Spacer(modifier = Modifier.height(20.dp))
                                                            // Apple Music-style action row:
                                                            // [Shuffle][Play pill][Download/More] (cluster centered, all 48dp matching size)
                                                            val isThisPlaying = isPlaying && playingPlaylistId == data.id
                                                            Row(
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 8.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                if (!data.isRadio) {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .size(48.dp)
                                                                                .clip(CircleShape)
                                                                                .background(Color.White.copy(alpha = 0.12f))
                                                                                .clickable {
                                                                                    viewModel.onUIEvent(PlaylistUIEvent.Shuffle)
                                                                                },
                                                                        contentAlignment = Alignment.Center,
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = SimpIcons.Shuffle,
                                                                            contentDescription = "Shuffle",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(22.dp),
                                                                        )
                                                                    }
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
                                                                                    viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                                                                }
                                                                            }.padding(horizontal = 20.dp),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                            imageVector =
                                                                                if (isThisPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
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
                                                                if (!data.isRadio) {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .size(48.dp)
                                                                                .clip(CircleShape)
                                                                                .background(Color.White.copy(alpha = 0.12f)),
                                                                        contentAlignment = Alignment.Center,
                                                                    ) {
                                                                        Crossfade(targetState = downloadState) { state ->
                                                                            when (state) {
                                                                                DownloadState.STATE_DOWNLOADED -> {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .fillMaxSize()
                                                                                                .clickable {
                                                                                                    viewModel.makeToast(
                                                                                                        getStringBlocking(Res.string.downloaded),
                                                                                                    )
                                                                                                },
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        Icon(
                                                                                            painter = painterResource(Res.drawable.baseline_downloaded),
                                                                                            tint = Color(0xFF00A0CB),
                                                                                            contentDescription = "",
                                                                                            modifier = Modifier.size(22.dp),
                                                                                        )
                                                                                    }
                                                                                }

                                                                                DownloadState.STATE_DOWNLOADING -> {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .fillMaxSize()
                                                                                                .clickable {
                                                                                                    viewModel.makeToast(
                                                                                                        getStringBlocking(Res.string.downloading),
                                                                                                    )
                                                                                                },
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        DownloadingIndicator(
                                                                                            modifier = Modifier.size(28.dp),
                                                                                        )
                                                                                    }
                                                                                }

                                                                                else -> {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .fillMaxSize()
                                                                                                .clickable {
                                                                                                    Logger.w(
                                                                                                        "PlaylistScreen",
                                                                                                        "downloadState: $downloadState",
                                                                                                    )
                                                                                                    viewModel.onUIEvent(PlaylistUIEvent.Download)
                                                                                                },
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        Icon(
                                                                                            imageVector = SimpIcons.DownloadForOffline,
                                                                                            tint = Color.White,
                                                                                            contentDescription = "Download",
                                                                                            modifier = Modifier.size(22.dp),
                                                                                        )
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                // Glass overlays — siblings of the source, over the strip the column reserved.
                                                Box(Modifier.padding(start = 12.dp)) {
                                                    LiquidGlassIconButton(
                                                        backdrop = headerBackdrop,
                                                        imageVector = SimpIcons.ArrowBackIosNew,
                                                        shape = RoundedCornerShape(24.dp),
                                                        // Same directional style as the like/⋯ pill, a touch thicker. The default
                                                        // width of 0.5.dp becomes a ~2px stroke (HighlightModifier: ceil(width.toPx()) * 2),
                                                        // which reads along the pill's long edge but vanishes around a 48dp circle. 1.dp
                                                        // is the smallest step up that stays visible without looking like a border.
                                                        highlight = Highlight(width = 1.dp),
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.TopStart)
                                                                .padding(12.dp)
                                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                                .size(48.dp),
                                                    ) {
                                                        navController.navigateUp()
                                                    }
                                                }
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.TopEnd)
                                                            .windowInsetsPadding(WindowInsets.statusBars)
                                                            .padding(end = 32.dp, top = 16.dp)
                                                            .height(48.dp)
                                                            .liquidGlass(headerBackdrop, RoundedCornerShape(24.dp)),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    if (!data.isRadio && favoriteAvailable) {
                                                        Box(
                                                            modifier = Modifier.size(48.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            HeartCheckBox(
                                                                size = 28,
                                                                checked = liked,
                                                                onStateChange = {
                                                                    viewModel.onUIEvent(PlaylistUIEvent.Favorite)
                                                                },
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            showSearchBar = !showSearchBar
                                                        },
                                                    ) {
                                                        Icon(SimpIcons.Search, null, tint = Color.White)
                                                    }
                                                    if (isCloudDisk) {
                                                        IconButton(
                                                            onClick = { uploadPicker.launch() },
                                                        ) {
                                                            Icon(
                                                                imageVector = SimpIcons.CloudUpload,
                                                                contentDescription = stringResource(
                                                                    Res.string.upload_to_cloud_disk,
                                                                ),
                                                                tint = Color.White,
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = onPlaylistMoreClick,
                                                    ) {
                                                        Icon(
                                                            imageVector = SimpIcons.MoreVert,
                                                            contentDescription = "More",
                                                            tint = Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                        ) {
                                            Column(Modifier.padding(horizontal = 32.dp)) {
                                                if (isPortrait) {
                                                    // Apple Music-style action row:
                                                    // [Shuffle][Play pill][Download/More] (cluster centered, all 48dp matching size)
                                                    val isThisPlaying = isPlaying && playingPlaylistId == data.id
                                                    Row(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        if (!data.isRadio) {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(48.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.White.copy(alpha = 0.12f))
                                                                        .clickable {
                                                                            viewModel.onUIEvent(PlaylistUIEvent.Shuffle)
                                                                        },
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.Shuffle,
                                                                    contentDescription = "Shuffle",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(22.dp),
                                                                )
                                                            }
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
                                                                            viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                                                        }
                                                                    }.padding(horizontal = 20.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector =
                                                                        if (isThisPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
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
                                                        if (!data.isRadio) {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(48.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.White.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Crossfade(targetState = downloadState) { state ->
                                                                    when (state) {
                                                                        DownloadState.STATE_DOWNLOADED -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            viewModel.makeToast(
                                                                                                getStringBlocking(Res.string.downloaded),
                                                                                            )
                                                                                        },
                                                                                contentAlignment = Alignment.Center,
                                                                            ) {
                                                                                Icon(
                                                                                    painter = painterResource(Res.drawable.baseline_downloaded),
                                                                                    tint = Color(0xFF00A0CB),
                                                                                    contentDescription = "",
                                                                                    modifier = Modifier.size(22.dp),
                                                                                )
                                                                            }
                                                                        }

                                                                        DownloadState.STATE_DOWNLOADING -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            viewModel.makeToast(
                                                                                                getStringBlocking(Res.string.downloading),
                                                                                            )
                                                                                        },
                                                                                contentAlignment = Alignment.Center,
                                                                            ) {
                                                                                DownloadingIndicator(
                                                                                    modifier = Modifier.size(28.dp),
                                                                                )
                                                                            }
                                                                        }

                                                                        else -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            Logger.w(
                                                                                                "PlaylistScreen",
                                                                                                "downloadState: $downloadState",
                                                                                            )
                                                                                            viewModel.onUIEvent(PlaylistUIEvent.Download)
                                                                                        },
                                                                                contentAlignment = Alignment.Center,
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = SimpIcons.DownloadForOffline,
                                                                                    tint = Color.White,
                                                                                    contentDescription = "Download",
                                                                                    modifier = Modifier.size(22.dp),
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                // 无描述的歌单(收藏歌单常无)整块隐藏,不显示"无描述"占位
                                                val playlistDescription = state.data.description
                                                if (!playlistDescription.isNullOrEmpty()) {
                                                    val uriHandler = LocalUriHandler.current
                                                    DescriptionView(
                                                        modifier =
                                                            Modifier
                                                                .padding(
                                                                    top = 8.dp,
                                                                ),
                                                        text = playlistDescription,
                                                        limitLine = 3,
                                                        onTimeClicked = {},
                                                        onURLClicked = { url ->
                                                            uriHandler.openUri(url)
                                                        },
                                                    )
                                                }
                                                Text(
                                                    text =
                                                        if (data.isRadio) {
                                                            stringResource(Res.string.unlimited)
                                                        } else {
                                                            stringResource(
                                                                Res.string.album_length,
                                                                (data.trackCount).toString(),
                                                                "",
                                                            )
                                                        },
                                                    color = Color.White,
                                                    style = typo().bodyMedium,
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                )
                                            }
                                        }
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
                    items(count = filteredTrack.size, key = { index ->
                        val item = filteredTrack.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val item = filteredTrack.getOrNull(index)
                        if (item != null) {
                            Column(modifier = Modifier.animateItem()) {
                                if (playingTrack?.videoId == item.videoId && isPlaying) {
                                    SongFullWidthItems(
                                        forceDark = true,
                                        isPlaying = true,
                                        track = item,
                                        onMoreClickListener = { onItemMoreClick(it) },
                                        onClickListener = {
                                            Logger.w("PlaylistScreen", "index: $index")
                                            onPlaylistItemClick(it)
                                        },
                                        onAddToQueue = {
                                            sharedViewModel.addListToQueue(
                                                arrayListOf(item),
                                            )
                                        },
                                        selectionMode = selectionState.isActive,
                                        isSelected = selectionState.isSelected(item.videoId),
                                        onLongClick = { selectionState.start(it) },
                                        onSelectToggle = { selectionState.toggle(it) },
                                        coverFallbackPainter = cloudCoverFallback,
                                        modifier = Modifier,
                                    )
                                } else {
                                    SongFullWidthItems(
                                        forceDark = true,
                                        isPlaying = false,
                                        track = item,
                                        onMoreClickListener = { onItemMoreClick(it) },
                                        onClickListener = {
                                            Logger.w("PlaylistScreen", "index: $index")
                                            onPlaylistItemClick(it)
                                        },
                                        onAddToQueue = {
                                            sharedViewModel.addListToQueue(
                                                arrayListOf(item),
                                            )
                                        },
                                        selectionMode = selectionState.isActive,
                                        isSelected = selectionState.isSelected(item.videoId),
                                        onLongClick = { selectionState.start(it) },
                                        onSelectToggle = { selectionState.toggle(it) },
                                        coverFallbackPainter = cloudCoverFallback,
                                        modifier = Modifier,
                                    )
                                }
                                if (index < filteredTrack.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                    )
                                }
                            }
                        }
                    }
                    when (tracksListState) {
                        ListState.IDLE -> {
                            // DO NOTHING
                            item {
                                EndOfPage()
                            }
                        }

                        ListState.LOADING, ListState.PAGINATING -> {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CenterLoadingBox(
                                        modifier = Modifier.size(80.dp),
                                    )
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }

                        ListState.ERROR -> {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.error),
                                        style = typo().bodyMedium,
                                    )
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }

                        ListState.PAGINATION_EXHAUST -> {
                            item {
                                EndOfPage()
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = SearchBarEnter,
                    exit = SearchBarExit,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { searchBarHeightPx = it.size.height }
                            // 不透明兜底底色:haze 2.0 的 hazeBlur 层在覆盖层刚出现时会渲染成
                            // 未初始化的纯白(实测:开搜索条后 y~355-475 全宽 #FFFFFF 静态白带,
                            // 滚动触发 haze source 重绘后自愈)。垫上 palette 底色后 blur 失灵
                            // 最多退化成实色条,不再漏白
                            .background(mutedPaletteBg)
                            .hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(mutedPaletteBg, 0.55f)),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
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
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
                            ) {
                                Icon(SimpIcons.Close, null, tint = Color.White)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectionState.isActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        onSelectAll = {
                            selectionState.toggleSelectAll(filteredTrack.map { it.videoId })
                        },
                        onOpenActions = { showSelectionSheet = true },
                        modifier =
                            Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(mutedPaletteBg, 0.55f)),
                    )
                }
                if (showSelectionSheet) {
                    val selectedIds = selectionState.selected.toList()
                    SelectedSongsBottomSheet(
                        count = selectedIds.size,
                        selectionIds = selectedIds,
                        onDismiss = { showSelectionSheet = false },
                        onPlayNext = {
                            selectionViewModel.playNext(selectedIds)
                            selectionState.exit()
                        },
                        onAddToQueue = {
                            selectionViewModel.addToQueue(selectedIds)
                            selectionState.exit()
                        },
                        onAddToPlaylist = {
                            selectionViewModel.loadCloudPlaylists()
                            showSelectionAddToPlaylist = true
                        },
                        onDownload = {
                            selectionViewModel.download(selectedIds)
                            selectionState.exit()
                        },
                        onAddToFavorite = {
                            selectionViewModel.addToFavorite(selectedIds)
                            selectionState.exit()
                        },
                        // 云盘多选删除(2026-10-09):删除类用 error 色,确认弹窗兜底
                        extraActions =
                            if (isCloudDisk) {
                                listOf(
                                    SongSelectionAction(
                                        icon = SimpIcons.Delete,
                                        label = stringResource(Res.string.delete_from_cloud_disk),
                                        tint = MaterialTheme.colorScheme.error,
                                    ) { showCloudDeleteDialog = true },
                                )
                            } else {
                                emptyList()
                            },
                    )
                }
                pendingCloudDeleteSong?.let { song ->
                    AlertDialog(
                        containerColor = rememberSurfaceDarkColors().container,
                        onDismissRequest = { pendingCloudDeleteSong = null },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteCloudDiskSongs(listOf(song.videoId))
                                    pendingCloudDeleteSong = null
                                },
                            ) {
                                Text(text = stringResource(Res.string.delete), style = typo().labelSmall)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingCloudDeleteSong = null }) {
                                Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(Res.string.delete_from_cloud_disk_title, 1),
                                style = typo().labelSmall,
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(Res.string.delete_from_cloud_disk_message),
                                style = typo().bodyMedium,
                            )
                        },
                    )
                }
                if (showCloudDeleteDialog) {
                    val selectedIds = selectionState.selected.toList()
                    AlertDialog(
                        containerColor = rememberSurfaceDarkColors().container,
                        onDismissRequest = { showCloudDeleteDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showCloudDeleteDialog = false
                                    viewModel.deleteCloudDiskSongs(selectedIds)
                                    selectionState.exit()
                                },
                            ) {
                                Text(text = stringResource(Res.string.delete), style = typo().labelSmall)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCloudDeleteDialog = false }) {
                                Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(Res.string.delete_from_cloud_disk_title, selectedIds.size),
                                style = typo().labelSmall,
                            )
                        },
                        text = {
                            Text(text = stringResource(Res.string.delete_from_cloud_disk_message), style = typo().bodyMedium)
                        },
                    )
                }
                if (showSelectionAddToPlaylist) {
                    val selectedIds = selectionState.selected.toList()
                    val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
                    val youTubePlaylists by selectionViewModel.youTubePlaylists.collectAsStateWithLifecycle()
                    val neteasePlaylists by selectionViewModel.neteasePlaylists.collectAsStateWithLifecycle()
                    val youTubeLoadFailedState by selectionViewModel.youTubePlaylistsFailed.collectAsStateWithLifecycle()
                    val neteaseLoadFailedState by selectionViewModel.neteasePlaylistsFailed.collectAsStateWithLifecycle()
                    AddToPlaylistModalBottomSheet(
                        isBottomSheetVisible = true,
                        listLocalPlaylist = localPlaylists,
                        listYouTubePlaylist = youTubePlaylists,
                        listNeteasePlaylist = neteasePlaylists,
                        youTubeLoadFailed = youTubeLoadFailedState,
                        neteaseLoadFailed = neteaseLoadFailedState,
                        onRetryCloudPlaylists = { selectionViewModel.loadCloudPlaylists() },
                        videoIds = selectedIds,
                        onDismiss = { showSelectionAddToPlaylist = false },
                        onClick = { playlist ->
                        selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                        selectionState.exit()
                        },
                        onYTPlaylistClick = { playlist ->
                        selectionViewModel.addToYouTubePlaylist(playlist.browseId, selectedIds)
                        selectionState.exit()
                        },
                        onNeteasePlaylistClick = { playlist ->
                        selectionViewModel.addToNeteasePlaylist(playlist.browseId, selectedIds)
                        selectionState.exit()
                        },
                    )
                }
                if (itemBottomSheetShow && currentItem != null) {
                    val track = currentItem?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = {
                            itemBottomSheetShow = false
                            currentItem = null
                        },
                        navController = navController,
                        song = track,
                        // 红心歌单内取消红心:对账确认后移行(2026-10-09 CR 改)——不再
                        // 点击即删:云端写失败/被拒时行直接消失,页面与云端不一致且无自愈
                        // (下次进入才对账回归)。onLikeChanged 只登记目标,likeWriteResults
                        // 落定(带目标 id+成败)再动行;失败保留行,toast 由写路径自报。
                        // 与 MV 收藏页 pendingUnlikeId 同款模式。
                        onLikeChanged = { liked ->
                            if (!liked) pendingUnlikeId = track.videoId
                        },
                        // 云盘单曲删除:登记目标,确认弹窗(不可逆)落定再调 VM
                        onDeleteFromCloudDisk = if (isCloudDisk) {
                            { pendingCloudDeleteSong = track }
                        } else {
                            null
                        },
                    )
                }
                if (playlistBottomSheetShow) {
                    Logger.w("PlaylistScreen", "PlaylistBottomSheet")
                    // 模板在组合期解析(回调里不能取 stringResource);count 回调时才知道
                    val partiallyLoadedPattern = stringResource(Res.string.partially_loaded)
                    val addToQueue = {
                        viewModel.getFullTracks { track, complete ->
                            // 部分加载(云盘续页失败放弃):先告知缺页,再添加已加载部分
                            if (!complete) {
                                viewModel.makeToast(partiallyLoadedPattern.format(track.size))
                            }
                            sharedViewModel.addListToQueue(
                                track.toCollection(arrayListOf()),
                            )
                        }
                    }
                    PlaylistBottomSheet(
                        onDismiss = { playlistBottomSheetShow = false },
                        playlistId = data.id,
                        playlistName = data.title,
                        isYourYouTubePlaylist = isYourYouTubePlaylist && !data.isRadio,
                        showShare = !isCloudDisk,
                        onEditTitle = { newTitle ->
                            viewModel.updatePlaylistTitle(newTitle, data.id)
                        },
                        onAddToQueue = if (data.isRadio) null else addToQueue,
                    )
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar && !showSearchBar && !selectionState.isActive,
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
                                text = data.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(
                                            align = Alignment.CenterVertically,
                                        ).basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        },
                        navigationIcon = {
                            Box(Modifier.padding(horizontal = 5.dp)) {
                                RippleIconButton(
                                    SimpIcons.ArrowBackIosNew,
                                    Modifier
                                        .size(32.dp),
                                    true,
                                ) {
                                    navController.navigateUp()
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
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

            is PlaylistUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier = Modifier.size(80.dp),
                    )
                }
            }

            is PlaylistUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                navController.navigateUp()
            }
        }

        // 云盘批量上传进度弹窗(模态:上传不可中断,无按钮)
        val cloudUploadProgress by viewModel.cloudUploadProgress.collectAsStateWithLifecycle()
        cloudUploadProgress?.let { p ->
            val fileFraction =
                if (p.bytesTotal > 0) (p.bytesSent.toFloat() / p.bytesTotal).coerceIn(0f, 1f) else 0f
            val overall = ((p.index - 1) + fileFraction) / p.total.coerceAtLeast(1)
            AlertDialog(
                containerColor = rememberSurfaceDarkColors().container,
                onDismissRequest = {},
                confirmButton = {},
                title = {
                    Text(
                        text = stringResource(Res.string.cloud_upload_progress_title),
                        style = typo().labelSmall,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "${p.index}/${p.total}  ${p.fileName}",
                            style = typo().bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { overall },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(overall * 100).toInt()}%",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }

        // 整歌单下载"跳过/覆盖/取消"三选弹窗(2026-10 二期)
        val batchDownloadRequest by viewModel.batchDownloadRequest.collectAsStateWithLifecycle()
        batchDownloadRequest?.let { request ->
            BatchDownloadConfirmDialog(
                downloadedCount = request.downloaded.size,
                onSkip = { viewModel.confirmBatchDownload(false) },
                onOverwrite = { viewModel.confirmBatchDownload(true) },
                onDismiss = { viewModel.dismissBatchDownload() },
            )
        }
    }
}