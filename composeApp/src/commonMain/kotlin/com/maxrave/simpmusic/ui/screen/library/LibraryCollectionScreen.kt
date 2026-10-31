package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.maxrave.simpmusic.viewModel.DownloadEntryStatus
import com.maxrave.simpmusic.viewModel.DownloadManagementRow
import com.maxrave.simpmusic.ui.screen.library.DownloadViewMode
import com.maxrave.simpmusic.viewModel.sortRank
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.scrollReportingEffect
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryTilingItem
import com.maxrave.simpmusic.ui.component.LibraryTilingState
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SelectAll
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.MusicNote
import com.maxrave.simpmusic.ui.icon.PlaylistRemove
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PauseCircle
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlayCircle
import com.maxrave.simpmusic.ui.icon.RestartAlt
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.cached_empty_hint
import simpmusic.composeapp.generated.resources.cached
import simpmusic.composeapp.generated.resources.remove_cached_batch_message
import simpmusic.composeapp.generated.resources.remove_from_cache
import simpmusic.composeapp.generated.resources.n_songs_selected
import simpmusic.composeapp.generated.resources.select_all
import simpmusic.composeapp.generated.resources.download_no_completed
import simpmusic.composeapp.generated.resources.download_no_in_progress
import simpmusic.composeapp.generated.resources.download_section_in_progress
import simpmusic.composeapp.generated.resources.download_section_completed
import simpmusic.composeapp.generated.resources.download_action_pause_all
import simpmusic.composeapp.generated.resources.download_action_resume_all
import simpmusic.composeapp.generated.resources.download_action_retry_failed
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.library_podcasts
import simpmusic.composeapp.generated.resources.no_favorite_playlists
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.no_playlists_downloaded
import simpmusic.composeapp.generated.resources.no_results_found
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import simpmusic.composeapp.generated.resources.pause
import simpmusic.composeapp.generated.resources.play
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.remove_download_message
import simpmusic.composeapp.generated.resources.remove_download_title
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.shuffle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCollectionScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: String,
    viewModel: LibraryViewModel = koinViewModel(),
    dynamicPlaylistViewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
    // 🚨single VM 只能 koinInject:koinViewModel() 会把 Koin 单例登记进本 entry 的
    // ViewModelStore,entry 出栈/保存态被清时 AndroidX 对单例调 onCleared → viewModelScope
    // 永久取消,之后全部 switchSource 静默不执行(2026-10-28 真机"切不到网易"根因)
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val pageType = runCatching { LibraryChipType.valueOf(type) }.getOrDefault(LibraryChipType.LOCAL_PLAYLIST)
    val localPlaylists by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val collections by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val podcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    var showCreatePlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(pageType) {
        when (pageType) {
            LibraryChipType.LOCAL_PLAYLIST -> viewModel.getLocalPlaylist()
            LibraryChipType.FAVORITE_PLAYLIST -> viewModel.getPlaylistFavorite()
            // 下载管理页:行数据走 dynamicPlaylistViewModel 的常驻实时流,无需装载
            LibraryChipType.FAVORITE_PODCAST -> viewModel.getFavoritePodcasts()
            else -> Unit
        }
    }

    val title =
        when (pageType) {
            LibraryChipType.LOCAL_PLAYLIST -> stringResource(Res.string.playlists)
            LibraryChipType.FAVORITE_PLAYLIST -> stringResource(Res.string.favorite)
            LibraryChipType.DOWNLOADED_PLAYLIST -> stringResource(Res.string.downloaded)
            LibraryChipType.FAVORITE_PODCAST -> stringResource(Res.string.library_podcasts)
            else -> stringResource(Res.string.playlists)
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = title, style = typo().titleMedium) },
                navigationIcon = {
                    RippleIconButton(
                        imageVector = SimpIcons.ArrowBackIosNew,
                        tint = MaterialTheme.colorScheme.onBackground,
                        onClick = { navController.popBackStack() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { scaffoldPadding ->
        // 底部留白锁定展开态底栏高度(与库页 lockedBottomPadding 同理):液态玻璃底栏
        // 随滚动收展形变,若列表 contentPadding.bottom 跟随,贴底位置每次形变都重钳制
        // →上报→底栏再形变,自持振荡;锁定后形变不扰动列表几何
        var lockedBottomPadding by remember { mutableStateOf(0.dp) }
        lockedBottomPadding = maxOf(lockedBottomPadding, innerPadding.calculateBottomPadding())
        val contentPadding =
            scaffoldPadding.copy(
                bottom = lockedBottomPadding,
            )
        when (pageType) {
            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController = navController,
                    contentPadding = contentPadding,
                    data = localPlaylists,
                    emptyText = Res.string.no_playlists_added,
                    createNewPlaylist = { showCreatePlaylist = true },
                    onReload = viewModel::getLocalPlaylist,
                )
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController = navController,
                    contentPadding = contentPadding,
                    data = collections,
                    emptyText = Res.string.no_favorite_playlists,
                    onReload = viewModel::getPlaylistFavorite,
                )
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                DownloadedManagementBody(
                    topPadding = contentPadding.calculateTopPadding(),
                    bottomPadding = contentPadding.calculateBottomPadding(),
                    navController = navController,
                    viewModel = viewModel,
                    dynamicPlaylistViewModel = dynamicPlaylistViewModel,
                    sharedViewModel = sharedViewModel,
                )
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController = navController,
                    contentPadding = contentPadding,
                    data = podcasts,
                    emptyText = Res.string.no_favorite_podcasts,
                    onReload = viewModel::getFavoritePodcasts,
                )
            }

            else -> Unit
        }
    }


    if (showCreatePlaylist) {
        CreateLocalPlaylistSheet(
            viewModel = viewModel,
            onDismiss = { showCreatePlaylist = false },
        )
    }

}

/**
 * “下载管理”页体(下载中/已完成两视角):既被独立路由 [LibraryCollectionScreen] 用,也被
 * 库页 chip 页(DOWNLOADED_PLAYLIST 分支)直接内嵌——chip 页没有自己的 TopAppBar,
 * 两处共用同一份内容,各自处理顶/底 padding。
 * 2026-10-01 用户定稿:tab 不再按内容分(歌曲/视频/播客),改为按状态分——行全量混排,
 * 视角行渲染按”有无视频条目”切;统计(N首·总大小)放”已完成”磁贴副标题上。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedManagementBody(
    topPadding: Dp,
    bottomPadding: Dp,
    navController: NavController,
    viewModel: LibraryViewModel,
    dynamicPlaylistViewModel: LibraryDynamicPlaylistViewModel,
    sharedViewModel: SharedViewModel,
    // 库页 chip 内嵌时接顶栏收起信号(与其它 chip 页同款);独立路由不传,不参与
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val managementRows by dynamicPlaylistViewModel.downloadManagementRows.collectAsStateWithLifecycle()
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    var downloadedSection by remember { mutableStateOf(DownloadedSection.Completed) }
    // 已缓存未下载列表(选中该磁贴时刷新;移除后原地重载)
    val cachedSongs by dynamicPlaylistViewModel.cachedSongs.collectAsStateWithLifecycle()
    var removeCachedTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(downloadedSection) {
        if (downloadedSection == DownloadedSection.Cached) dynamicPlaylistViewModel.refreshCachedSongs()
    }
    var selectedDownloadedSong by remember { mutableStateOf<com.maxrave.domain.data.entities.SongEntity?>(null) }
    // 行删除/取消的确认目标(videoId+标题);在途”取消下载”也走确认防误触
    var deleteRowTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    // 多选模式(用户反馈 2026-10):长按行进入;批量删除所选
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var deleteSelectionConfirm by remember { mutableStateOf(false) }
    // "已完成"标签的搜索(2026-10-07 补齐旧下载页按钮逻辑):搜索图标开合,query 过滤已完成行;
    // 2026-10-31 起已缓存 tab 共用(过滤各自的列表)
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // 切磁贴退出多选(2026-10-31 用户定):多选域只属于当前 tab;
    // 搜索条同口径——搜索域也只属于当前 tab,切换时收起并清词(已完成/已缓存各有各的列表)
    LaunchedEffect(downloadedSection) {
        if (selectionMode) {
            selectionMode = false
            selectedIds = emptySet()
        }
        if (showSearchBar || query.isNotEmpty()) {
            showSearchBar = false
            query = ""
        }
    }
    // 暂停全部只对 DownloadManager 在途(下载中/排队)有效;转存中(EXPORTING)不走队列停不了
    val hasPausable = managementRows.any { row ->
        !row.directMvActive && listOf(row.audioStatus.sortRank(), row.videoStatus?.sortRank() ?: 9).any { r -> r == 0 || r == 2 }
    }
    val hasPaused = managementRows.any { it.audioStatus == DownloadEntryStatus.PAUSED || it.videoStatus == DownloadEntryStatus.PAUSED }
    val hasFailed = managementRows.any { it.audioStatus == DownloadEntryStatus.FAILED || it.videoStatus == DownloadEntryStatus.FAILED }

    // 分组=两个 tab:在途(下载中/排队/暂停/失败/转存中)与已完成(含丢失/旧缓存);混排不分类
    val rankOf: (DownloadManagementRow) -> Int = { row ->
        minOf(row.audioStatus.sortRank(), row.videoStatus?.sortRank() ?: Int.MAX_VALUE)
    }
    val inFlightRows = managementRows.filter { rankOf(it) <= 4 }
    val completedRows = managementRows.filter { rankOf(it) >= 5 }
    // “已完成”磁贴统计:只数真完成(文件在=rank 6,丢失行不计也不数)
    val completedCount = completedRows.count { rankOf(it) == 6 }

    // 磁贴页的屏幕顺序,用于切换滑动方向判定
    val sectionOrderOf: (DownloadedSection) -> Int = {
        when (it) {
            DownloadedSection.Completed -> 0
            DownloadedSection.Cached -> 1
            DownloadedSection.InProgress -> 2
        }
    }
    val isInProgressTab = downloadedSection == DownloadedSection.InProgress
    val isCachedTab = SHOW_CACHED_TAB && downloadedSection == DownloadedSection.Cached
    // 磁贴右侧操作区的播放按钮两态(2026-10-31 用户定):正在播当前 tab 的列表=暂停图标
    //(点击暂停/恢复),其余=播放图标(点击装全队列起播)
    val isPlayingNow by dynamicPlaylistViewModel.isPlaybackPlaying.collectAsStateWithLifecycle()
    val playingId = nowPlaying?.songEntity?.videoId ?: nowPlaying?.track?.videoId

    Column(modifier = Modifier.fillMaxSize().padding(top = topPadding)) {
        // "下载中"磁贴标题带动态计数(2026-10-02 用户定:页内不再有"下载中 (N)"文案,
        // 计数上磁贴;0 时不带括号)——managementRows 是实时流,增删任务自动刷新
        val inProgressTitle =
            if (inFlightRows.isNotEmpty()) {
                stringResource(Res.string.download_section_in_progress) + " (${inFlightRows.size})"
            } else {
                stringResource(Res.string.download_section_in_progress)
            }
        Column(
            // 库页统一口径:水平边距 15dp 与主页/各 chip 一致(原 10dp 与其它页不同口径)
            // 两行磁贴(2026-10-31 用户定):第一行=已完成+已缓存,第二行=下载中;三块等大(各半宽)
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 垂直居中:操作区按钮(32dp)比磁贴(~62dp)矮,不居中会顶在行首
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    // 统计直接跟在标题后(2026-10-02 用户定):"已完成 (N)"——只带计数不带容量
                    // (用户复定:容量可能展示不开);空库只显示"已完成"
                    val completedWord = stringResource(Res.string.download_section_completed)
                    LibraryTilingItem(
                        state = LibraryTilingState.DownloadCompleted,
                        selected = downloadedSection == DownloadedSection.Completed,
                        titleString = if (completedCount > 0) "$completedWord ($completedCount)" else completedWord,
                        onClick = { downloadedSection = DownloadedSection.Completed },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    // 已缓存未下载(fork 2026-10-28):完整缓存的歌,非下载物;计数同款
                    val cachedWord = stringResource(Res.string.cached)
                    LibraryTilingItem(
                        state = LibraryTilingState.DownloadCached,
                        selected = downloadedSection == DownloadedSection.Cached,
                        titleString = if (cachedSongs.isNotEmpty()) "$cachedWord (${cachedSongs.size})" else cachedWord,
                        onClick = { downloadedSection = DownloadedSection.Cached },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    LibraryTilingItem(
                        state = LibraryTilingState.DownloadInProgress,
                        selected = downloadedSection == DownloadedSection.InProgress,
                        titleString = inProgressTitle,
                        onClick = { downloadedSection = DownloadedSection.InProgress },
                    )
                }
                // 右侧空白区(2026-10-31 用户定):操作按钮常驻区,内容随选中磁贴分流,整体居中。
                // 已完成/已缓存=播放/随机/搜索三钮(原"已完成"标签下的操作行迁入);
                // 下载中=全部暂停/全部继续/重试失败图标(替代原列表节头的文字 pills)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (isInProgressTab) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasPausable) {
                                RippleIconButton(
                                    imageVector = SimpIcons.Pause,
                                    modifier = Modifier.size(32.dp),
                                    fillMaxSize = true,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    contentDescription = stringResource(Res.string.download_action_pause_all),
                                    onClick = dynamicPlaylistViewModel::pauseAllDownloads,
                                )
                            }
                            if (hasPaused) {
                                RippleIconButton(
                                    imageVector = SimpIcons.PlayArrow,
                                    modifier = Modifier.size(32.dp),
                                    fillMaxSize = true,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    contentDescription = stringResource(Res.string.download_action_resume_all),
                                    onClick = dynamicPlaylistViewModel::resumeAllDownloads,
                                )
                            }
                            if (hasFailed) {
                                RippleIconButton(
                                    imageVector = SimpIcons.RestartAlt,
                                    modifier = Modifier.size(32.dp),
                                    fillMaxSize = true,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    contentDescription = stringResource(Res.string.download_action_retry_failed),
                                    onClick = dynamicPlaylistViewModel::retryFailedDownloads,
                                )
                            }
                        }
                    } else {
                        val playingHere =
                            if (isCachedTab) {
                                cachedSongs.any { it.videoId == playingId }
                            } else {
                                completedRows.any { it.song.videoId == playingId }
                            }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 播放按钮两态,行内垂直居中,48dp 大一档(2026-10-07 用户定,迁入操作区保留)
                            RippleIconButton(
                                imageVector =
                                    if (playingHere && isPlayingNow) {
                                        SimpIcons.PauseCircle
                                    } else {
                                        SimpIcons.PlayCircle
                                    },
                                modifier = Modifier.size(48.dp),
                                fillMaxSize = true,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription =
                                    stringResource(
                                        if (playingHere && isPlayingNow) Res.string.pause else Res.string.play,
                                    ),
                                onClick = {
                                    if (playingHere) {
                                        dynamicPlaylistViewModel.togglePlayPause()
                                    } else if (isCachedTab) {
                                        dynamicPlaylistViewModel.playAllCached()
                                    } else {
                                        dynamicPlaylistViewModel.playAllDownloaded()
                                    }
                                },
                            )
                            RippleIconButton(
                                imageVector = SimpIcons.Shuffle,
                                modifier = Modifier.size(32.dp),
                                fillMaxSize = true,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = stringResource(Res.string.shuffle),
                                onClick = {
                                    if (isCachedTab) {
                                        dynamicPlaylistViewModel.shuffleCached()
                                    } else {
                                        dynamicPlaylistViewModel.shuffleDownloaded()
                                    }
                                },
                            )
                            RippleIconButton(
                                imageVector = if (showSearchBar) SimpIcons.Close else SimpIcons.Search,
                                modifier = Modifier.size(32.dp),
                                fillMaxSize = true,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = stringResource(Res.string.search),
                                onClick = { showSearchBar = !showSearchBar },
                            )
                        }
                    }
                }
            }
        }
        // 搜索作用于"已完成/已缓存"两个标签(标题/任一艺人包含即命中);下载中 tab 不参与。
        // tabRows 只装已完成口径的行;已缓存列表在下面分支直接用 cachedRows,别混进这里
        val cachedRows =
            if (showSearchBar && query.isNotEmpty()) cachedSongs.filter { it.matches(query) } else cachedSongs
        val tabRows =
            when {
                isInProgressTab -> inFlightRows
                showSearchBar && query.isNotEmpty() -> completedRows.filter { it.song.matches(query) }
                else -> completedRows
            }
        // 搜索条(磁贴右侧操作区的搜索钮开合):已完成/已缓存共用,过滤各自的列表
        AnimatedVisibility(showSearchBar, enter = SearchBarEnter, exit = SearchBarExit) {
                SearchBar(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            // 高度对齐歌单页搜索条(50dp):placeholder 升 bodyMedium 后 45dp
                            // 装不下,字显示不全(2026-10-07 用户反馈)
                            .height(50.dp)
                            .padding(horizontal = 12.dp),
                    inputField = {
                        CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                            SearchBarDefaults.InputField(
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = { showSearchBar = false },
                                expanded = showSearchBar,
                                onExpandedChange = { showSearchBar = it },
                                placeholder = {
                                    // 字体对齐歌单页搜索框(PlaylistScreen 同款 bodyMedium,
                                    // 用户 2026-10-07 反馈两处不一致)
                                    Text(
                                        stringResource(Res.string.search),
                                        style = typo().bodyMedium,
                                    )
                                },
                                leadingIcon = { Icon(SimpIcons.Search, contentDescription = null) },
                            )
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                }
        }
        Box(modifier = Modifier.weight(1f)) {
            // 已缓存 tab 有自己的空态与列表(tabRows 是'已完成'口径,不作数)
            if (tabRows.isEmpty() && !isCachedTab) {
                    // 空态=恒在顶:切页不再统一复位标题,从收起态的深滚动页切到空页时
                    // 若不上报,标题会继承收起态且没有任何滚动事件可纠正(CR-31)
                    LaunchedEffect(Unit) { onScrolling(true) }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text =
                                stringResource(
                                    when {
                                        isInProgressTab -> Res.string.download_no_in_progress
                                        showSearchBar && query.isNotEmpty() -> Res.string.no_results_found
                                        else -> Res.string.download_no_completed
                                    },
                                ),
                            style = typo().labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    // 顶栏收起信号:Songs 列表自报(与其它 chip 页同一套口径,
                    // 含几何反馈断路,见 scrollReportingEffect 注释)
                    // tab 切换过场(2026-10-31 用户定,二次改:Crossfade 两页交叠读作
                    // "闪烁",改方向感知滑动——按磁贴屏幕顺序(已完成→已缓存→下载中)
                    // 判定来向,整页滑入滑出+淡切,像连续翻页;列表态各分支独立
                    // (滚动位置/LazyListState 不跨 tab 串)
                    AnimatedContent(
                        targetState = downloadedSection,
                        transitionSpec = {
                            // 含蓄版(2026-10-31 用户二定):新页 1/5 屏滑入+淡入,旧页
                            // 原位快速淡出——有方向感但不整页翻动
                            val forward = sectionOrderOf(targetState) > sectionOrderOf(initialState)
                            val dir = if (forward) 1 else -1
                            (
                                slideInHorizontally(tween(260)) { it / 5 * dir } +
                                    fadeIn(tween(220))
                                ) togetherWith fadeOut(tween(140))
                        },
                        label = "downloadedSection",
                    ) { section ->
                        val sectionIsCached = SHOW_CACHED_TAB && section == DownloadedSection.Cached
                        val songsListState = rememberLazyListState()
                        songsListState.scrollReportingEffect(onScrolling)
                        Column(modifier = Modifier.fillMaxSize()) {
                        if (selectionMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                // 通用全选控件(2026-10-01 用户定):行首圆圈+文字两态切换
                                // (全选↔清空),视觉与行多选圆圈同款;不再是纯文字按钮
                                // 已缓存 tab 的多选域=缓存歌(与已完成 tab 互不相干;搜索态只圈可见行)
                                val allIds =
                                    if (sectionIsCached) {
                                        cachedRows.map { it.videoId }.toSet()
                                    } else {
                                        tabRows.map { it.song.videoId }.toSet()
                                    }
                                val allSelected = allIds.isNotEmpty() && selectedIds.containsAll(allIds)
                                // 方块全选钮(2026-10-31 用户定):与歌曲多选顶栏同款 SelectAll
                                // 图标,不带"全选"文字;全选态 primary 提示
                                IconButton(onClick = { selectedIds = if (allSelected) emptySet() else allIds }) {
                                    Icon(
                                        imageVector = SimpIcons.SelectAll,
                                        contentDescription = stringResource(Res.string.select_all),
                                        tint =
                                            if (allSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                    )
                                }
                                Text(
                                    text = stringResource(Res.string.n_songs_selected, selectedIds.size),
                                    style = typo().labelMedium,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                                TextButton(
                                    onClick = { if (selectedIds.isNotEmpty()) deleteSelectionConfirm = true },
                                ) {
                                    // 已缓存 tab 的批量动作=移除缓存(非删除文件)
                                    Text(
                                        stringResource(if (sectionIsCached) Res.string.remove_from_cache else Res.string.delete),
                                        style = typo().labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                TextButton(onClick = {
                                    selectionMode = false
                                    selectedIds = emptySet()
                                }) { Text(stringResource(Res.string.cancel), style = typo().labelMedium) }
                            }
                        }
                        LazyColumn(
                            state = songsListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = bottomPadding),
                        ) {
                            if (isCachedTab) {
                                if (cachedRows.isEmpty()) {
                                    item(key = "cached-empty") {
                                        Text(
                                            // 搜索无结果与"真空缓存"分开提示(2026-10-31 搜索接入已缓存 tab)
                                            text =
                                                stringResource(
                                                    if (showSearchBar && query.isNotEmpty()) {
                                                        Res.string.no_results_found
                                                    } else {
                                                        Res.string.cached_empty_hint
                                                    },
                                                ),
                                            style = typo().bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                items(cachedRows, key = { it.videoId }) { song ->
                                    CachedSongRow(
                                        song = song,
                                        selectionMode = selectionMode,
                                        isSelected = song.videoId in selectedIds,
                                        onToggle = {
                                            selectedIds =
                                                if (song.videoId in selectedIds) selectedIds - song.videoId else selectedIds + song.videoId
                                        },
                                        onEnterSelection = {
                                            selectionMode = true
                                            selectedIds = setOf(song.videoId)
                                        },
                                        onRemove = { removeCachedTarget = song.videoId to (song.title ?: "") },
                                    )
                                }
                            } else {
                            items(tabRows, key = { it.song.videoId }) { row ->
                                DownloadManagementRowItem(
                                    row = row,
                                    // 正在播放判定双源(2026-10-02):track 在队列 rebuild 窗口
                                    // (updateCatalog 清空回填)里 find 落空恒 null,拿 DB 落的
                                    // songEntity 兜底——与 NowPlayingScreen 同款口径
                                    isPlaying =
                                        (nowPlaying?.songEntity?.videoId ?: nowPlaying?.track?.videoId) == row.song.videoId,
                                    // 行视角(混排后):有视频条目的行看视频条目(mp4 是它的
                                    // 落地产物,在途进度也在视频上),纯音频行看音频
                                    viewMode = if (row.videoStatus != null) DownloadViewMode.VIDEO else DownloadViewMode.AUDIO,
                                    selectionMode = selectionMode,
                                    selectedIds = selectedIds,
                                    onEnterSelection = {
                                        selectionMode = true
                                        selectedIds = setOf(row.song.videoId)
                                    },
                                    onToggle = { id ->
                                        selectedIds =
                                            if (id in selectedIds) selectedIds - id else selectedIds + id
                                    },
                                    dynamicPlaylistViewModel = dynamicPlaylistViewModel,
                                    onRedownload = {
                                        if (row.videoStatus != null) {
                                            dynamicPlaylistViewModel.redownloadVideo(row.song.videoId)
                                        } else {
                                            dynamicPlaylistViewModel.redownload(row.song.videoId)
                                        }
                                    },
                                    onDelete = { deleteRowTarget = row.song.videoId to row.song.title },
                                    onSongMenu = { selectedDownloadedSong = row.song },
                                )
                            }
                            }
                            item { EndOfPage(includeBottomBarPadding = false) }
                        }
                        }
                    }
                }
        }
    }

    selectedDownloadedSong?.let { song ->
        NowPlayingBottomSheet(
            onDismiss = { selectedDownloadedSong = null },
            navController = navController,
            song = song,
        )
    }

    if (deleteSelectionConfirm) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onDismissRequest = { deleteSelectionConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    deleteSelectionConfirm = false
                    if (downloadedSection == DownloadedSection.Cached) {
                        // 已缓存 tab(2026-10-31 多选):批量=移除播放器缓存,不动下载文件
                        dynamicPlaylistViewModel.removeCachedSongs(selectedIds)
                    } else {
                        // 混排 tab(2026-10-01):删除=整行清理(音视频文件+条目+Room)
                        selectedIds.forEach { id ->
                            dynamicPlaylistViewModel.deleteDownload(id)
                        }
                    }
                    selectionMode = false
                    selectedIds = emptySet()
                }) {
                    Text(stringResource(Res.string.delete), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSelectionConfirm = false }) {
                    Text(stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
            title = {
                Text(
                    stringResource(if (downloadedSection == DownloadedSection.Cached) Res.string.remove_from_cache else Res.string.remove_download_title),
                    style = typo().labelSmall,
                )
            },
            text = {
                Text(
                    if (downloadedSection == DownloadedSection.Cached) {
                        stringResource(Res.string.remove_cached_batch_message, selectedIds.size)
                    } else {
                        stringResource(Res.string.remove_download_message) + " (" + selectedIds.size + ")"
                    },
                    style = typo().bodyMedium,
                )
            },
        )
    }

    // 条目删除/取消下载确认(在途取消与完成删除都破坏数据,防误触)
    deleteRowTarget?.let { (videoId, title) ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onDismissRequest = { deleteRowTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    deleteRowTarget = null
                    dynamicPlaylistViewModel.deleteDownload(videoId)
                }) {
                    Text(stringResource(Res.string.delete), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRowTarget = null }) {
                    Text(stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
            title = { Text(title, style = typo().labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = { Text(stringResource(Res.string.remove_download_message), style = typo().bodyMedium) },
        )
    }

    // 移除缓存确认(fork 2026-10-28):只清播放器缓存,不动任何下载文件
    removeCachedTarget?.let { (videoId, title) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removeCachedTarget = null },
            title = { Text(stringResource(Res.string.remove_from_cache)) },
            text = { Text(title) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        removeCachedTarget = null
                        dynamicPlaylistViewModel.removeCachedSong(videoId)
                    },
                ) { Text(stringResource(Res.string.remove_from_cache)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { removeCachedTarget = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

}

/** 已缓存未下载行(fork 2026-10-28;2026-10-31 用户定:封面占位+行尾移除图标+多选):
 *  非多选单击=移除确认,长按=进入多选;多选模式单击=勾选 */
@Composable
private fun CachedSongRow(
    song: com.maxrave.domain.data.entities.SongEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onEnterSelection: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onToggle() else onRemove() },
                    onLongClick = onEnterSelection,
                )
                .background(if (isSelected) seed.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 15.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        // 多选圆圈(与已完成 tab 行内同款)
        androidx.compose.animation.AnimatedVisibility(
            visible = selectionMode,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isSelected) seed else androidx.compose.ui.graphics.Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) seed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                shape = androidx.compose.foundation.shape.CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = SimpIcons.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        // 封面:DB 行无缩略图或加载失败时露出底下的音符占位(缓存歌的行可能缺图)
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            // thumbnails 是单个 URL 字符串(非列表):lastOrNull 会取到末尾字符,coil 加载失败只露占位
            song.thumbnails?.takeIf { it.isNotBlank() }?.let { thumb ->
                coil3.compose.AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title ?: "",
                style = typo().bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistName?.joinToString(", ") ?: "",
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        // 行尾移除图标(FM 封面同款 PlaylistRemove;2026-10-31 用户定,替代文字)
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable { if (selectionMode) onToggle() else onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.PlaylistRemove,
                contentDescription = stringResource(Res.string.remove_from_cache),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 两组节共用的行渲染(避免下载中/已完成两段 items 重复二十行接线) */
@Composable
private fun DownloadManagementRowItem(
    row: com.maxrave.simpmusic.viewModel.DownloadManagementRow,
    isPlaying: Boolean,
    viewMode: DownloadViewMode,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onEnterSelection: () -> Unit,
    onToggle: (String) -> Unit,
    dynamicPlaylistViewModel: LibraryDynamicPlaylistViewModel,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
    onSongMenu: () -> Unit,
) {
    DownloadManagementItem(
        row = row,
        isPlaying = isPlaying,
        viewMode = viewMode,
        selectionMode = selectionMode,
        isSelected = row.song.videoId in selectedIds,
        onLongClick = onEnterSelection,
        onSelectToggle = { onToggle(row.song.videoId) },
        onPlay = {
            dynamicPlaylistViewModel.playDownloadedSong(row.song.videoId)
        },
        onPause = { dynamicPlaylistViewModel.pauseDownload(row.song.videoId) },
        onResume = { dynamicPlaylistViewModel.resumeDownload(row.song.videoId) },
        onRetry = { dynamicPlaylistViewModel.retryDownload(row.song.videoId) },
        onRedownload = onRedownload,
        onDelete = onDelete,
        onSongMenu = onSongMenu,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 下载管理页两视角(2026-10-01 用户定):下载中/已完成,行混排不按内容分类 */
// 已缓存 tab(2026-10-28 上游 Cached 数据层):2026-10-31 用户提四项 UI 优化(磁贴/图标/
    // 行样式/多选)重新启用并重做样式
private const val SHOW_CACHED_TAB = true

internal enum class DownloadedSection {
    InProgress,
    Completed,
    /** 完整缓存未下载(fork 2026-10-28,上游 Cached 数据层挂到下载管理页) */
    Cached,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateLocalPlaylistSheet(
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val close = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.width(60.dp).height(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(Res.string.playlist_name)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
                TextButton(
                    onClick = {
                        if (title.isBlank()) {
                            viewModel.makeToast(runBlocking { getString(Res.string.playlist_name_cannot_be_empty) })
                        } else {
                            viewModel.createPlaylist(title)
                            close()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.create))
                }
            }
        }
    }
}

/**
 * "已完成"标签搜索的命中判定(与旧下载页同款):标题或任一艺人包含即命中——
 * 人们找"某歌手的全部歌"不比找单曲少,只搜标题看起来像坏了。
 */
private fun matchesQuery(
    title: String,
    artists: List<String>?,
    query: String,
): Boolean =
    title.contains(query, ignoreCase = true) ||
        artists?.any { it.contains(query, ignoreCase = true) } == true

private fun SongEntity.matches(query: String): Boolean = matchesQuery(title, artistName, query)
