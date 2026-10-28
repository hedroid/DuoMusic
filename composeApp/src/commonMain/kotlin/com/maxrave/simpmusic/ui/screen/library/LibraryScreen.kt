package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import com.maxrave.simpmusic.ui.screen.home.chipStripEdgeFades
import androidx.compose.animation.core.animateFloatAsState
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.NeteaseLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.icon.Notifications
import simpmusic.composeapp.generated.resources.category
import simpmusic.composeapp.generated.resources.log_in_to_netease
import simpmusic.composeapp.generated.resources.log_in
import simpmusic.composeapp.generated.resources.notification
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.source.MusicSource
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.CreateLocalPlaylistSheet
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryItem
import com.maxrave.simpmusic.ui.component.LibraryItemState
import com.maxrave.simpmusic.ui.component.LibraryItemType
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.taste.TasteCard
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryCollectionDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.NeteasePodcastViewModel
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.TasteUiState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.download_management
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.netease_podcast

import simpmusic.composeapp.generated.resources.chart
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.downloaded_playlists
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.favorite_playlists
import simpmusic.composeapp.generated.resources.favorite_podcasts
import simpmusic.composeapp.generated.resources.library
import simpmusic.composeapp.generated.resources.library_podcasts
import simpmusic.composeapp.generated.resources.mix_for_you
import simpmusic.composeapp.generated.resources.no_YouTube_playlists
import simpmusic.composeapp.generated.resources.no_charts_found
import simpmusic.composeapp.generated.resources.no_favorite_playlists
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.remove_download_message
import simpmusic.composeapp.generated.resources.remove_download_title
import simpmusic.composeapp.generated.resources.simpmusic_charts
import simpmusic.composeapp.generated.resources.wrapped
import simpmusic.composeapp.generated.resources.your_library
import simpmusic.composeapp.generated.resources.your_netease
import simpmusic.composeapp.generated.resources.your_playlists
import simpmusic.composeapp.generated.resources.your_youtube_music

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    // Wrapped(年度回顾)chip 已隐藏(2026-09-30):recaps/localTrackingEnabled 两个收集器随之
    // 下岗,恢复 chip 时一并加回(monthlyRecaps/localTrackingEnabled + getMonthlyRecaps)。
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val youTubeLikedPlaylists by viewModel.youTubeLikedPlaylists.collectAsStateWithLifecycle()
    val youTubeAutoPlaylists by viewModel.youTubeAutoPlaylists.collectAsStateWithLifecycle()
    val youTubeAlbums by viewModel.youTubeAlbums.collectAsStateWithLifecycle()
    val followedYTArtists by viewModel.followedYTArtists.collectAsStateWithLifecycle()
    val listCanvasSong by viewModel.listCanvasSong.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val chartPlaylists by viewModel.chartPlaylists.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val neteaseLoggedIn by viewModel.neteaseLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val neteasePlaylist by viewModel.neteasePlaylist.collectAsStateWithLifecycle()
    val subscribedArtists by viewModel.subscribedArtists.collectAsStateWithLifecycle()
    val starredAlbums by viewModel.starredAlbums.collectAsStateWithLifecycle()
    val neteaseRefreshing by viewModel.neteaseRefreshing.collectAsStateWithLifecycle()
    val youTubeRefreshing by viewModel.youTubeRefreshing.collectAsStateWithLifecycle()
    val ownNeteasePlaylistIds by viewModel.ownNeteasePlaylistIds.collectAsStateWithLifecycle()
    val neteaseLikedPlaylistId by viewModel.neteaseLikedPlaylistId.collectAsStateWithLifecycle()

    // 网易云播客 chip 页 VM(Koin single:数据随 single 存活,tab 往返零重拉)。
    // 🚨必须 koinInject 不能 koinViewModel——后者会把 single 登记进库页 entry 的
    // ViewModelStore,store 被清时播客单例陪葬 onCleared(SharedViewModel 同款事故,2026-10-28)
    val neteasePodcastViewModel: NeteasePodcastViewModel = koinInject()

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    val allSelectedDownloaded by selectionViewModel.allSelectedDownloaded.collectAsStateWithLifecycle()
    LaunchedEffect(showSelectionSheet) {
        if (showSelectionSheet) selectionViewModel.checkAllDownloaded(selectionState.selected.toList())
    }
    // 顶栏头像=当前音源登录账号的头像(用户 2026-09-30):网易源=云村账号,其它=Google 账号
    val accountThumbnail by viewModel.sourceAccountThumbnail.collectAsStateWithLifecycle()
    val hazeState =
        rememberHazeState()

    // 网格页内容留白:锁定"展开态"高度(maxOf),不跟随收起动画——contentPadding 若逐帧
    // 跟随动画,LazyGrid 每帧重锚定首可见项→触发新一轮滚动上报→翻转标题显隐→又改
    // padding,自持振荡(用户实测"滑到底顶部来回跳");锁定后标题行收/展是纯覆盖层,
    // 内容零位移。首页不受此害是因为它的留白是第 0 项内部的 Spacer,滚远后改高度
    // 不碰当前锚点。只增不减,进程重启从展开态起步(下面 showTitleBar 非 saveable)。
    // 常驻头部高度恒定且几何已知(状态栏 inset+56dp 行+8dp 底距),直接计算——不再
    // onGloballyPositioned 测量:覆盖式布局下测量链曾量到整页高,把内容推出屏(实测)。
    val headerHeight =
        with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 56.dp + 8.dp
    // 顶栏背景态(主页同款):true=在顶(透明)/false=滚走(hazeBlur 模糊条淡入)
    var headerAtTop by remember { mutableStateOf(true) }
    // chips 收起/展开(用户 2026-10-28 定):休息态只显当前标签(收缩);点当前标签或
    // 左右滑动→展开全部;展开态点任意标签(含当前)→选中并收缩回休息态
    var chipsExpanded by rememberSaveable { mutableStateOf(false) }
    // 各 chip 列表的底部留白:锁定"展开态"底栏高度(maxOf),不跟随底栏形变动画。
    // 液态玻璃底栏随 isScrolledToTop 在两行(迷你条+导航)与一行(沉到导航行)间形变,
    // innerPadding.bottom 逐帧变——若列表 contentPadding.bottom 跟随,贴底位置每次
    // 形变都重钳制→index 变化→上报→底栏再形变,自持振荡(2026-10-03 用户实测"下载页
    // 贴底迷你条+导航栏来回跳");锁定后形变不再扰动列表几何,上报可全量放行(方向
    // 恒真实,含贴底回弹),代价仅收起态下列表尾部多留一段空白。只增不减。
    var lockedBottomPadding by remember {
        mutableStateOf(0.dp)
    }
    lockedBottomPadding = maxOf(lockedBottomPadding, innerPadding.calculateBottomPadding())
    var showAddSheet by remember { mutableStateOf(false) }


    LaunchedEffect(nowPlaying) {
        Logger.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    // 主页同构:普通 Row+horizontalScroll(LazyRow 的 item 垂直顶对齐会让 chips 偏上 ~8dp)
    val chipRowState = rememberScrollState()
    // 选中项滚入视野用实测几何(CR P2:固定 120dp/320dp 估算在窄屏/长文案下会把屏外
    // 选中项误判为可见——深链/登录态回落路径可触发):chip 在行内的真实起点与宽度,
    // 以及 chips 行视口的真实宽度,onGloballyPositioned 回填。
    val chipMeasured = remember { mutableStateMapOf<LibraryChipType, Long>() } // type -> start shl32 or width
    var chipViewportPx by remember { mutableIntStateOf(0) }
    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()
    // 2026-10-28 顶栏改主页式常驻(头像+chips 一行,滚走只剩模糊条):高度恒定,
    // 收起/展开机制(标题行显隐/500ms 静默窗/回放/双轨高度)整体退役——高度不变
    // 则 contentPadding 不变,三轮振荡修复针对的反馈环从根上不存在了。
    val openLibraryPlaylists = {
        navController.navigate(LibraryCollectionDestination(LibraryChipType.LOCAL_PLAYLIST.name))
    }
    val openLibraryCollections = {
        navController.navigate(LibraryCollectionDestination(LibraryChipType.FAVORITE_PLAYLIST.name))
    }
    val openLibraryPodcasts = {
        navController.navigate(LibraryCollectionDestination(LibraryChipType.FAVORITE_PODCAST.name))
    }
    val openLibraryDownloads = {
        navController.navigate(LibraryCollectionDestination(LibraryChipType.DOWNLOADED_PLAYLIST.name))
    }

    // chip 按音源显隐(用户 2026-09-30 定稿):网易云/网易播客=网易源+网易登录;
    // 您的 YouTube Music/排行榜=YT 源+YT 登录;下载管理恒可见。selectedSource 是
    // StateFlow,无 initialValue 收集即首帧取 .value(VM 新建、源键首读落地前是
    // 空串,按 YT 侧处理,与 App 默认源同款口径);两个登录态是 DataStore 冷流,
    // 重组首帧恒为 initialValue,回落判定因此不吃这里的值(见下)。
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val sourceIsNetease = selectedSource == MusicSource.NETEASE.name
    val neteaseChipsVisible = neteaseLoggedIn && sourceIsNetease
    val ytChipsVisible = loggedIn && !sourceIsNetease

    // "您的库"chip 页已下线;下载管理升为顶层 chip,本地歌单等独立路由保留但不再从
    // chip 行进入。顺序(用户 2026-09-20 定序):网易云 → YouTube Music → 排行榜 →
    // 下载管理(Wrapped 年度回顾 chip 已于 2026-09-30 隐藏,恢复时加回
    // LibraryChipType.WRAPPED 并放开其内容分支);"进库默认选第一个可见 chip"的
    // 取值顺序与此保持一致。
    val topLevelLibraryChips =
        listOf(
            LibraryChipType.NETEASE_PLAYLIST,
            // 网易云播客(仅网易登录显示,未登录不出现——与您的网易云同款门控)
            LibraryChipType.NETEASE_PODCAST,
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST,
            LibraryChipType.CHART,
            LibraryChipType.DOWNLOADED_PLAYLIST,
        )
    // 上面门控的落地点:把显隐规则折成实际渲染的 chip 列表(索引同时供下方滚动 effect 用)
    val visibleLibraryChips =
        topLevelLibraryChips.filter { type ->
            val neteaseChip = type == LibraryChipType.NETEASE_PLAYLIST || type == LibraryChipType.NETEASE_PODCAST
            val ytChip = type == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST || type == LibraryChipType.CHART
            (neteaseChip && neteaseChipsVisible) || (ytChip && ytChipsVisible) ||
                (!neteaseChip && !ytChip)
        }
    // simpmusic://library?tab=… 深链要去的 chip:一次性消费,不可见就静默忽略保持当前。
    // 可见性判定在 effect 内 combine 三条门控流取真值——组合级 state 在重组首帧是
    // initialValue:false,直接判会把已登录用户的冷启动深链目标 chip 当"不可见"错杀
    // 并把一次性请求清掉(与上方回落判定同款约束,见其注释)。
    val libraryTabRequest by sharedViewModel.libraryTabRequest.collectAsStateWithLifecycle()
    LaunchedEffect(libraryTabRequest) {
        val requested = libraryTabRequest ?: return@LaunchedEffect
        val (netease, yt, source) =
            combine(
                viewModel.neteaseLoggedIn,
                viewModel.youtubeLoggedIn,
                viewModel.selectedSource,
            ) { n, y, s -> Triple(n, y, s) }
                // 空串=selectedSource 镜像首值未落地(DataStore 键恒有非空默认),等重发
                .first { (_, _, s) -> s.isNotEmpty() }
        val applicable =
            when (requested) {
                LibraryChipType.NETEASE_PLAYLIST, LibraryChipType.NETEASE_PODCAST ->
                    netease && source == MusicSource.NETEASE.name
                LibraryChipType.YOUTUBE_MUSIC_PLAYLIST, LibraryChipType.CHART ->
                    yt && source != MusicSource.NETEASE.name
                LibraryChipType.DOWNLOADED_PLAYLIST -> true
                // YOUR_LIBRARY/WRAPPED/旧混源值:已下线,不切;真值下的不可见是终局,
                // 同样消费——请求不留到用户登录后突然跳转
                else -> false
            }
        if (applicable) viewModel.setCurrentScreen(requested)
        sharedViewModel.requestLibraryTab(null)
    }
    // 选中 chip 必须可见(UI-CR-15):程序性变化(回落弹回/切源/登出重定向/深滚动位恢复)
    // 之后把选中项滚入视野——完全可见时不动,避免与用户手点可见 chip 的滚动打架。
    // 可见性也作 key:chip 集合增删会让同一选中项的 index 平移(如网易两 chip 插到前面)。
    LaunchedEffect(currentFilter, neteaseChipsVisible, ytChipsVisible, chipMeasured.toMap()) {
        // 实测几何判定(CR P2):选中 chip 的真实起止不在真实视口内才滚入;留 16dp 前导
        // 防止贴边。完全可见时不动,避免与用户手点可见 chip 的滚动打架。
        val measured = chipMeasured[currentFilter] ?: return@LaunchedEffect
        val itemStart = (measured shr 32).toInt()
        val itemEnd = itemStart + (measured and 0xffffffffL).toInt()
        val viewportStart = chipRowState.value
        val viewportEnd = viewportStart + chipViewportPx
        if (itemStart < viewportStart || itemEnd > viewportEnd) {
            val leading = with(density) { 16.dp.toPx().roundToInt() }
            chipRowState.animateScrollTo(
                (itemStart - leading).coerceIn(0, chipRowState.maxValue),
            )
        }
    }

    // 回落判定:选中 chip 被音源/登录态变化藏掉时弹回第一个可见 chip。门控三流
    // (网易登录/YT 登录/音源)在 effect 内 combine 后消费——组合级 state 在进歌单
    // 详情返回等重组首帧是 initialValue(false),会把"未初始化"当"未登录",曾把
    // "您的网易云"选中误弹回下载管理(2026-09-30 回归);combine 的首个 emission
    // 保证判定时三条门控全是真值。已下线 enum(YOUR_LIBRARY/WRAPPED/混源旧值等)
    // 走 else 分支同样在此回落,下方 load effect 的重定向分支已并入本处。
    // 让位门:深链请求挂起期间不写——冷启动三写者(VM 默认写/本回落/深链消费)竞速
    // 会互相覆盖,深链目标是权威值,消费前回落不得抢写;请求清掉(含"目标不可见被
    // 忽略"的情形,此时 VM 默认写也让了位、选中停在初始 YOUR_LIBRARY)后 key 变化
    // 重启本 effect,从真值正常回落。
    LaunchedEffect(currentFilter, libraryTabRequest) {
        val filter = currentFilter
        combine(
            viewModel.neteaseLoggedIn,
            viewModel.youtubeLoggedIn,
            viewModel.selectedSource,
        ) { netease, yt, source -> Triple(netease, yt, source) }
            .collect { (netease, yt, source) ->
                // 空串只出现在 VM 新建、selectedSource 镜像首值落地前(DataStore 键
                // 恒有非空默认),此时不做判定,等真值落地 combine 重发——否则网易
                // 用户首进库页会被空串当 YT 侧误判。
                if (source.isEmpty()) return@collect
                if (libraryTabRequest != null) return@collect
                val neteaseVisible = netease && source == MusicSource.NETEASE.name
                val ytVisible = yt && source != MusicSource.NETEASE.name
                val visible =
                    when (filter) {
                        LibraryChipType.NETEASE_PLAYLIST, LibraryChipType.NETEASE_PODCAST -> neteaseVisible
                        LibraryChipType.YOUTUBE_MUSIC_PLAYLIST, LibraryChipType.CHART -> ytVisible
                        LibraryChipType.DOWNLOADED_PLAYLIST -> true
                        else -> false
                    }
                if (!visible) {
                    viewModel.setCurrentScreen(
                        when {
                            neteaseVisible -> LibraryChipType.NETEASE_PLAYLIST
                            ytVisible -> LibraryChipType.YOUTUBE_MUSIC_PLAYLIST
                            else -> LibraryChipType.DOWNLOADED_PLAYLIST
                        },
                    )
                }
            }
    }

    LaunchedEffect(currentFilter) {
        // 标题行显隐不做切页复位:每个 chip 页自己上报滚动状态(含播客/下载管理),
        // Crossfade 按页恢复滚动位置(SaveableStateProvider),首帧上报的就是真实
        // 状态(深处=false)——复位会跟它打架,把刚展开的标题又压回去。
        Logger.w(
            "LIBPROBE",
            "currentFilter=$currentFilter netease=${neteasePlaylist::class.simpleName}/${subscribedArtists::class.simpleName}/${starredAlbums::class.simpleName}",
        )
        when (currentFilter) {
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                // 未加载过、或"登录态下系统歌单置顶行为空"(首拉撞上 cookie 恢复竞态/
                // split 间歇失败时 auto 分区空——登录账号必有 LM/SE,空=那次数据不可信)
                // 才拉。不看 created 空:删除唯一自建歌单后 YTM 服务端删除是异步的(~1min),
                // 按空重拉会把还没删掉的歌单又拉回来(2026-09-22)。子页写操作走
                // LibraryMutationBus 本地回写,不依赖返回时刷新。
                if (youTubePlaylist !is LocalResource.Success ||
                    youTubeAutoPlaylists.data.isNullOrEmpty()
                ) {
                    viewModel.getYouTubeLibrary()
                }
            }

            // "您的网易云"三分区(歌单/关注的歌手/收藏的专辑):空数据才拉;子页动作本地回写。
            // 登出回落由 VM 的 neteaseCookie collect 负责,这里不会停在无数据的分区上。
            LibraryChipType.NETEASE_PLAYLIST -> {
                if (neteasePlaylist !is LocalResource.Success) {
                    viewModel.getNeteaseLibrary()
                }
            }

            // 网易云播客:首拉整页;之后每次选中只静默刷新订阅区(详情页订阅动作要反映回来)
            LibraryChipType.NETEASE_PODCAST -> {
                neteasePodcastViewModel.onPageSelected()
            }

            // 下载管理 chip 页:行数据由 LibraryDynamicPlaylistViewModel 的
            // observeDownloadManagement 常驻实时流提供,选中时无需再拉
            // (旧"已下载歌单"网格已于 2026-10-07 随旧下载页一起删除)
            LibraryChipType.DOWNLOADED_PLAYLIST -> Unit

            LibraryChipType.CHART -> {
                if (chartPlaylists.data.isNullOrEmpty()) {
                    viewModel.getChartPlaylists()
                }
            }

            // 已下线 enum(YOUTUBE_MIX_FOR_YOU/WRAPPED/YOUR_LIBRARY/LOCAL_PLAYLIST/
            // FAVORITE_*)的重定向已并入上方的回落 effect(统一吃 combine 真值),
            // 这里只管各 chip 页的数据装载;K2 要求枚举 when 穷尽,else 吸掉旧值。
            else -> {}
        }
    }

    // The floating chip row refracts the page scrolling under it, so the page is the glass source and
    // the row its sibling (nesting the row inside the source is the RuntimeShader feedback loop).
    val chipBackdrop = rememberBackdrop(MaterialTheme.colorScheme.background)
    Crossfade(
        modifier = Modifier.then(if (LocalLiquidGlassEnabled.current) Modifier.layerBackdrop(chipBackdrop) else Modifier).hazeSource(hazeState),
        targetState = currentFilter,
    ) { filter ->
        // 各 chip 页滚动上报:①驱动顶栏"透明↔模糊条"状态(主页同款 atTop 语义——
        // 顶部=透明融入内容,滚走=模糊条淡入即"收缩");②转发给 App 驱动底栏形变。
        // 头部高度恒定,背景切换无几何反馈,旧静默窗/回放机制不需要。
        val tabScrolling: (onTop: Boolean) -> Unit = { onTop ->
            headerAtTop = onTop
            onScrolling(onTop)
        }
        when (filter) {
            // 下载管理 chip 页:复用独立页的内容体,chip 页无 TopAppBar(库页自带标题区)
            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                val dynamicViewModel: LibraryDynamicPlaylistViewModel = koinViewModel()
                val sharedVm: SharedViewModel = koinInject()
                DownloadedManagementBody(
                    topPadding = headerHeight,
                    bottomPadding = lockedBottomPadding,
                    navController = navController,
                    viewModel = viewModel,
                    dynamicPlaylistViewModel = dynamicViewModel,
                    sharedViewModel = sharedVm,
                    onScrolling = tabScrolling,
                )
            }

            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                LibraryYouTubeTab(
                    navController = navController,
                    contentPadding = innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    playlists = youTubePlaylist,
                    likedPlaylists = youTubeLikedPlaylists,
                    autoPlaylists = youTubeAutoPlaylists,
                    albums = youTubeAlbums,
                    artists = followedYTArtists,
                    isRefreshing = youTubeRefreshing,
                    onRefresh = { viewModel.getYouTubeLibrary(force = true) },
                    onDeletePlaylist = { viewModel.deleteYouTubePlaylist(it) },
                    onUnsubscribePlaylist = { viewModel.unsubscribeYouTubePlaylist(it) },
                    onCreatePlaylist = { viewModel.createYouTubePlaylistInLibrary(it) },
                    onScrolling = tabScrolling,
                )
            }

            LibraryChipType.NETEASE_PLAYLIST -> {
                val subscribedMvSummary by viewModel.subscribedMvSummary.collectAsStateWithLifecycle()
                val cloudDiskEntry by viewModel.cloudDiskEntry.collectAsStateWithLifecycle()
                LibraryNeteaseTab(
                    navController = navController,
                    contentPadding = innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    playlists = neteasePlaylist,
                    artists = subscribedArtists,
                    albums = starredAlbums,
                    isRefreshing = neteaseRefreshing,
                    onRefresh = { viewModel.getNeteaseLibrary(force = true) },
                    ownPlaylistIds = ownNeteasePlaylistIds,
                    likedPlaylistId = neteaseLikedPlaylistId,
                    onUnsubscribePlaylist = { viewModel.unsubscribeNeteasePlaylist(it) },
                    onDeletePlaylist = { viewModel.deleteNeteasePlaylist(it) },
                    onUnsubscribeAlbum = { viewModel.unsubscribeNeteaseAlbum(it) },
                    onCreatePlaylist = { viewModel.createNeteasePlaylistInLibrary(it) },
                    subscribedMv = subscribedMvSummary,
                    cloudDiskEntry = cloudDiskEntry,
                    onScrolling = tabScrolling,
                )
            }

            LibraryChipType.NETEASE_PODCAST -> {
                NeteasePodcastScreen(
                    innerPadding = innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    navController = navController,
                    viewModel = neteasePodcastViewModel,
                    onScrolling = tabScrolling,
                )
            }

            // Nothing to draw: MixForYouScreen owns this content now, and the effect above bounces
            // the filter back to YOUR_LIBRARY the moment it lands here.
            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> Unit

            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    yourLocalPlaylist,
                    onScrolling = tabScrolling,
                    emptyText = Res.string.no_playlists_added,
                    header = {
                        LibrarySectionHeader(
                            navController = navController,
                            title = stringResource(Res.string.playlists),
                            onOpenPlaylists = openLibraryPlaylists,
                            onOpenCollections = openLibraryCollections,
                            onOpenPodcasts = openLibraryPodcasts,
                            onOpenDownloads = openLibraryDownloads,
                        )
                    },
                    createNewPlaylist = {
                        showAddSheet = true
                    },
                ) {
                    viewModel.getLocalPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    favoritePlaylist,
                    emptyText = Res.string.no_favorite_playlists,
                    // 混源网格:网易来源的收藏条目带品牌角标
                    onScrolling = tabScrolling,
                    header = {
                        LibrarySectionHeader(
                            navController = navController,
                            title = stringResource(Res.string.favorite),
                            onOpenPlaylists = openLibraryPlaylists,
                            onOpenCollections = openLibraryCollections,
                            onOpenPodcasts = openLibraryPodcasts,
                            onOpenDownloads = openLibraryDownloads,
                        )
                    },
                ) {
                    viewModel.getPlaylistFavorite()
                }
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    favoritePodcasts,
                    emptyText = Res.string.no_favorite_podcasts,
                    onScrolling = tabScrolling,
                    header = {
                        LibrarySectionHeader(
                            navController = navController,
                            title = stringResource(Res.string.library_podcasts),
                            onOpenPlaylists = openLibraryPlaylists,
                            onOpenCollections = openLibraryCollections,
                            onOpenPodcasts = openLibraryPodcasts,
                            onOpenDownloads = openLibraryDownloads,
                        )
                    },
                ) {
                    viewModel.getFavoritePodcasts()
                }
            }

            LibraryChipType.CHART -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = headerHeight, bottom = lockedBottomPadding),
                    chartPlaylists,
                    emptyText = Res.string.no_charts_found,
                    onScrolling = tabScrolling,
                    // 排行榜 tile 封面右上角标 YTM 品牌角标(替代原 SimpMusic 图标)
                    showSourceBadge = true,
                ) {
                    viewModel.getChartPlaylists()
                }
            }

            // Nothing to draw: chip 已隐藏(2026-09-30),上面的 effect 会立刻弹回 CHART,
            // 与 YOUTUBE_MIX_FOR_YOU 同款;恢复 chip 时换回 LibraryWrappedTab(...) 分支。
            LibraryChipType.WRAPPED -> Unit
            else -> Unit
        }
    }
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        CreateLocalPlaylistSheet(
            onDismiss = { showAddSheet = false },
            onCreate = viewModel::createPlaylist,
        )
    }
    AnimatedContent(
        targetState = headerAtTop,
        transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
    ) { atTopState ->
    Column(
        Modifier
            .background(Color.Transparent)
            .then(
                // 顶部=透明(融入内容);滚走=模糊条(主页头部一字不差的形态切换)
                if (atTopState) {
                    Modifier
                } else {
                    Modifier.hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle())
                },
            ),
    ) {
        // 主页式常驻头部(2026-10-28 统一):头像(源账号,44dp,菜单=通知/登录) +
        // chips 行(滚动+渐隐)+ Listen Together。滚走后整个顶栏只剩这条模糊条。
        Box(Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Row(
            // 几何与主页逐字段一致:头像盒自带 start16/end8(=首 chip 距头像 8dp),
            // 行本身不加横向 padding;尾部控件 start8/end16
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.padding(start = 16.dp, end = 8.dp)) {
                LibraryAccountButton(
                    thumbnailUrl = accountThumbnail,
                    neteaseLoggedIn = neteaseLoggedIn,
                    youtubeLoggedIn = loggedIn,
                    navController = navController,
                )
            }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { chipViewportPx = it.size.width },
            ) {
                val chipHeadFade by animateFloatAsState(if (chipRowState.canScrollBackward) 1f else 0f)
                val chipTailFade by animateFloatAsState(if (chipRowState.canScrollForward) 1f else 0f)
                val chipLabel: (@Composable (LibraryChipType) -> String) = { type ->
                    stringResource(
                        when (type) {
                            LibraryChipType.YOUR_LIBRARY -> Res.string.your_library
                            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> Res.string.your_youtube_music
                            LibraryChipType.NETEASE_PLAYLIST -> Res.string.your_netease
                            LibraryChipType.NETEASE_PODCAST -> Res.string.netease_podcast
                            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> Res.string.mix_for_you
                            LibraryChipType.LOCAL_PLAYLIST -> Res.string.your_playlists
                            LibraryChipType.FAVORITE_PLAYLIST -> Res.string.favorite_playlists
                            LibraryChipType.DOWNLOADED_PLAYLIST -> Res.string.download_management
                            LibraryChipType.FAVORITE_PODCAST -> Res.string.favorite_podcasts
                            LibraryChipType.CHART -> Res.string.simpmusic_charts
                            LibraryChipType.WRAPPED -> Res.string.wrapped
                        },
                    )
                }
                // 主页同款单结构(2026-10-28):chips 组 + 一起听同住一条可滚行(SpaceBetween,
                // widthIn(min=maxWidth))——收缩时一起听贴右缘,展开变宽把它推出右缘(视觉=让位
                // 消失),左滑可滑回;展开动画=尺寸展开(ExpandableChip 同款语义),非 Crossfade 换壳
                Row(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .pointerInput(chipsExpanded) {
                                // 仅收缩态吃横滑(展开后让位给行滚动);key 在状态上,展开即拆监听
                                if (!chipsExpanded) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, _ ->
                                            change.consume()
                                            chipsExpanded = true
                                        },
                                    )
                                }
                            }
                            .chipStripEdgeFades(32.dp, start = { chipHeadFade }, end = { chipTailFade })
                            .horizontalScroll(chipRowState)
                            .widthIn(min = maxWidth)
                            .padding(end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 当前标签常驻:点击=收/展切换
                        Box(
                            Modifier.onGloballyPositioned { coords ->
                                chipMeasured[currentFilter] =
                                    (coords.positionInParent().x.roundToInt().toLong() shl 32) or
                                        coords.size.width.toLong()
                            },
                        ) {
                            Chip(
                                isAnimated = false,
                                isSelected = true,
                                text = chipLabel(currentFilter),
                                backdrop = chipBackdrop,
                            ) { chipsExpanded = !chipsExpanded }
                        }
                        // 展开指示:随状态旋转 180°(收=向下/展=向上),点击同切换
                        val arrowRotation by animateFloatAsState(
                            if (chipsExpanded) 180f else 0f,
                            label = "libraryChipsArrow",
                        )
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { chipsExpanded = !chipsExpanded },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = SimpIcons.KeyboardArrowDown,
                                contentDescription = stringResource(Res.string.category),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = arrowRotation },
                            )
                        }
                        // 其余标签:内联尺寸展开/收起(主页 ExpandableChip 的三态单结构同款)
                        AnimatedVisibility(
                            visible = chipsExpanded,
                            enter = expandHorizontally(tween(300)) + fadeIn(tween(300)),
                            exit = shrinkHorizontally(tween(300)) + fadeOut(tween(300)),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                visibleLibraryChips
                                    .filter { it != currentFilter }
                                    .forEach { type ->
                                        Box(
                                            Modifier.onGloballyPositioned { coords ->
                                                chipMeasured[type] =
                                                    (coords.positionInParent().x.roundToInt().toLong() shl 32) or
                                                        coords.size.width.toLong()
                                            },
                                        ) {
                                            Chip(
                                                isAnimated = false,
                                                isSelected = false,
                                                text = chipLabel(type),
                                                backdrop = chipBackdrop,
                                            ) {
                                                viewModel.setCurrentScreen(type)
                                                chipsExpanded = false
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    // 一起听骑行尾:收缩贴右缘;展开被推出(主页药丸同款)
                    Box(Modifier.padding(start = 8.dp)) {
                        ListenTogetherIconButton(
                            contentDescription = stringResource(Res.string.listen_together),
                        ) { navController.navigate(ListenTogetherDestination) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = selectionState.isActive) {
            SongSelectionTopAppBar(
                state = selectionState,
                // Stacked BELOW the header row in the same Column, which already consumed
                // the status-bar inset — leaving the default here reserved it twice.
                windowInsets = WindowInsets(0),
                onSelectAll = {
                    selectionState.toggleSelectAll(
                        (recentlyAdded.data ?: emptyList())
                            .filterIsInstance<SongEntity>()
                            .map { it.videoId },
                    )
                },
                onOpenActions = { showSelectionSheet = true },
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
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
                allDownloaded = allSelectedDownloaded,
                onRemoveDownload = {
                    selectionViewModel.removeDownload(selectedIds)
                    selectionState.exit()
                },
                onAddToFavorite = {
                    selectionViewModel.addToFavorite(selectedIds)
                    selectionState.exit()
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
                // 本地分区按政策隐藏(此前传 localPlaylists 但组件不渲染,弹窗实际为空);
                // 2026-09-24 多选路径接云端分区,与单曲弹窗同款
                listLocalPlaylist = emptyList(),
                listYouTubePlaylist = youTubePlaylists,
                listNeteasePlaylist = neteasePlaylists,
                youTubeLoadFailed = youTubeLoadFailedState,
                neteaseLoadFailed = neteaseLoadFailedState,
                onRetryCloudPlaylists = { selectionViewModel.loadCloudPlaylists() },
                videoIds = selectedIds,
                onDismiss = { showSelectionAddToPlaylist = false },
                onClick = {},
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
    }
    }
}

/** 库页头像钮(主页式常驻头部):当前音源登录账号头像,菜单=通知+未登录登录项(按源路由) */
@Composable
private fun LibraryAccountButton(
    thumbnailUrl: String?,
    // 登录态用真实流判定(CR P2:头像 URL 判定在无头像账号/头像未加载时会把已登录
    // 误判成未登录,菜单错误显示登录按钮)
    neteaseLoggedIn: Boolean,
    youtubeLoggedIn: Boolean,
    navController: NavController,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sharedViewModel: com.maxrave.simpmusic.viewModel.SharedViewModel = koinInject()
    val isNeteaseSource by sharedViewModel.selectedSource
        .collectAsStateWithLifecycle(initialValue = com.maxrave.domain.source.MusicSource.YOUTUBE_MUSIC.name)
    val signedIn = if (isNeteaseSource == com.maxrave.domain.source.MusicSource.NETEASE.name) neteaseLoggedIn else youtubeLoggedIn
    Box {
        com.maxrave.simpmusic.ui.screen.home.AccountAvatar(
            signedIn = signedIn,
            thumbnailUrl = thumbnailUrl,
            size = 44.dp,
            contentDescription = null,
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
                        navController.navigate(
                            if (isNeteaseSource == com.maxrave.domain.source.MusicSource.NETEASE.name) {
                                NeteaseLoginDestination
                            } else {
                                LoginDestination
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(
                            if (isNeteaseSource == com.maxrave.domain.source.MusicSource.NETEASE.name) {
                                Res.string.log_in_to_netease
                            } else {
                                Res.string.log_in
                            },
                        ),
                    )
                }
            }
        }
    }
}


@Composable
private fun LibrarySectionHeader(
    navController: NavController,
    title: String,
    onOpenPlaylists: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenPodcasts: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LibraryTilingBox(
            navController = navController,
            onOpenPlaylists = onOpenPlaylists,
            onOpenCollections = onOpenCollections,
            onOpenPodcasts = onOpenPodcasts,
            onOpenDownloads = onOpenDownloads,
        )
        Text(
            text = title,
            style = typo().titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 4.dp),
        )
    }
}
