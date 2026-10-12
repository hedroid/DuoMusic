package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.maxrave.common.AppIdentity
import com.maxrave.common.Config
import com.maxrave.common.NETEASE_MV_MEDIA_ID_PREFIX
import com.maxrave.common.NETEASE_PODCAST_RADIO_QUEUE_PREFIX
import com.maxrave.common.Config.ALBUM_CLICK
import com.maxrave.common.Config.DOWNLOAD_CACHE
import com.maxrave.common.Config.PLAYLIST_CLICK
import com.maxrave.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.common.Config.SHARE
import com.maxrave.common.Config.SONG_CLICK
import com.maxrave.common.Config.VIDEO_CLICK
import com.maxrave.common.LibraryChipType
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.STATUS_DONE
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.LyricsEntity
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.NeteaseSongInfoEntity
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.entities.TranslatedLyricsEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.netease.model.NeteaseMvDetail
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.promo.Promo
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.data.player.LiveStreamRegistry
import com.maxrave.domain.extension.decodeHtmlEntities
import com.maxrave.domain.extension.isSong
import com.maxrave.domain.extension.isVideo
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.manager.DataStoreManager.Values.FALSE
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.mediaservice.handler.SleepTimerState
import com.maxrave.domain.mediaservice.handler.addToQueueBlock
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.repository.UpdateRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toLyrics
import com.maxrave.domain.utils.toLyricsEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toSyncedLyrics
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.onEach
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.getDownloadFolderPath
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import com.maxrave.simpmusic.expect.ui.toByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.utils.decodePromos
import com.maxrave.simpmusic.utils.shownPromoIds
import com.maxrave.simpmusic.utils.withShownPromo
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import org.simpmusic.lastfm.completeLogin
import simpmusic.composeapp.generated.resources.mv_subscribed
import simpmusic.composeapp.generated.resources.cloud_delete_done
import simpmusic.composeapp.generated.resources.cloud_match_done
import simpmusic.composeapp.generated.resources.cloud_unlink_done
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.mv_saved_to_download
import simpmusic.composeapp.generated.resources.mv_unsubscribed
import simpmusic.composeapp.generated.resources.watch_mv
import simpmusic.composeapp.generated.resources.download_mv
import simpmusic.composeapp.generated.resources.no_mv_available
import simpmusic.composeapp.generated.resources.podcast_trial_toast
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.already_in_queue
import simpmusic.composeapp.generated.resources.login_netease_first
import simpmusic.composeapp.generated.resources.update_check_failed
import simpmusic.composeapp.generated.resources.added_to_youtube_liked
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.lastfm_login_failed
import simpmusic.composeapp.generated.resources.login_success
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_from_youtube_liked
import simpmusic.composeapp.generated.resources.cloud_action_failed_netease
import simpmusic.composeapp.generated.resources.netease_rate_limited
import simpmusic.composeapp.generated.resources.cloud_action_failed_youtube
import simpmusic.composeapp.generated.resources.liked_toast
import simpmusic.composeapp.generated.resources.need_login_toast
import simpmusic.composeapp.generated.resources.shared
import simpmusic.composeapp.generated.resources.unliked_toast
import simpmusic.composeapp.generated.resources.updated
import simpmusic.composeapp.generated.resources.vote_submitted
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.reflect.KClass

// DataStore key for the kotlin-footguns star prompt. Cleared on every version bump,
// so the ask comes back after an update - same policy as OPEN_APP_TIME.
const val FOOTGUNS_STAR_KEY = "footguns_starred"

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val updateRepository: UpdateRepository,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val cacheRepository: CacheRepository,
    private val neteaseRepository: com.maxrave.data.repository.NeteaseRepositoryImpl,
) : BaseViewModel() {

    init {
        // 付费播客节目试听 toast:repo 取流层检测到 26KB 占位时发事件。
        // 放这里(进程级 single 常驻)而非播客页 VM——播客页没进过时该 VM 不存在,
        // SharedFlow 无订阅者 tryEmit 直接丢弃,toast 永远不弹(2026-09-29 实测踩过)
        viewModelScope.launch {
            var lastSongId = ""
            var lastAtMs = 0L
            neteaseRepository.trialToastFlow.collect { songId ->
                val now = System.currentTimeMillis()
                if (songId != lastSongId || now - lastAtMs > 3000) {
                    makeToast(getString(Res.string.podcast_trial_toast))
                    lastSongId = songId
                    lastAtMs = now
                }
            }
        }
        // 播客收听位置记忆(原电台详情页 VM 的 init 循环搬来,CR-26 余项):挂页面 VM 的
        // viewModelScope 会随页面销毁断流,而音频还能继续播——离开详情页再听一段,重进
        // 读到的续播位置偏早。这里是进程级 single,radioId 从队列 playlistId 前缀反解,
        // 不依赖任何页面上下文;pid 双回退 track/songEntity 的 neteaseProgramId(v32 起
        // 播客构造 Track 携带,core 续页追加的节目同)。10s 粒度轮询,进程死最多丢 10s。
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                if (mediaPlayerHandler.controlState.value.isPlaying) {
                    writePodcastResumeSnapshot()
                }
            }
        }
        // 暂停/停止下降沿补写一次:位置已冻结,把轮询尾巴窗口(≤10s)补上。等 500ms 再写
        // 并复核仍非播放——节目自动连播时 isPlaying 会短暂闪断,立刻写会拿到"新节目 id
        // 配上一条末尾位置"的错配;闪断在 500ms 内恢复即跳过(StateFlow conflate,统一读
        // .value 为准,不用收集到的旧发射值)
        viewModelScope.launch {
            var wasPlaying = mediaPlayerHandler.controlState.value.isPlaying
            mediaPlayerHandler.controlState.collect {
                val playingNow = mediaPlayerHandler.controlState.value.isPlaying
                if (wasPlaying && !playingNow) {
                    delay(500)
                    if (!mediaPlayerHandler.controlState.value.isPlaying) writePodcastResumeSnapshot()
                }
                wasPlaying = mediaPlayerHandler.controlState.value.isPlaying
            }
        }
    }

    /** 播客收听记忆快照落盘:队列是播客电台(NETEASE_PODCAST_RADIO_<id>)且处于在播族
     *  状态(Progress/Ready/Buffering)时写 "programId|positionMs" 单键。曲尾 3s 内不写
     *  (续播 seek 到结尾会立即跳完,不如留在上一次正常写点)。 */
    private suspend fun writePodcastResumeSnapshot() {
        val radioId =
            mediaPlayerHandler.queueData.value?.data?.playlistId
                ?.takeIf { it.startsWith(NETEASE_PODCAST_RADIO_QUEUE_PREFIX) }
                ?.removePrefix(NETEASE_PODCAST_RADIO_QUEUE_PREFIX)
                ?.toLongOrNull() ?: return
        val np = mediaPlayerHandler.nowPlayingState.value
        val pid = np.track?.neteaseProgramId ?: np.songEntity?.neteaseProgramId ?: return
        val state = mediaPlayerHandler.simpleMediaState.value
        val active =
            state is SimpleMediaState.Progress ||
                state is SimpleMediaState.Ready ||
                state is SimpleMediaState.Buffering
        if (!active) return
        val pos = mediaPlayerHandler.player.currentPosition
        val durMs = np.track?.durationSeconds?.let { it * 1000L } ?: 0L
        if (durMs > 0 && pos >= durMs - 3_000) return
        dataStoreManager.putString("podcast_resume_$radioId", "$pid|$pos")
    }

    // ---------------------------------------------------------------- 音源切换(feat/netease-source)

    /** 当前激活音源,扇形菜单与各页面 TODO 分支共用这一份状态 */
    val selectedSource: StateFlow<String> =
        dataStoreManager.selectedSource
            .onEach { Logger.w("SRCPROBE", "selectedSource emission: $it") }
            .stateIn(viewModelScope, SharingStarted.Eagerly, com.maxrave.domain.source.MusicSource.YOUTUBE_MUSIC.name)

    val neteaseLoggedIn: StateFlow<Boolean> =
        dataStoreManager.neteaseCookie
            .map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 顶栏头像=当前音源登录账号(用户 2026-09-30):网易源=云村账号,其它(含 YT)=Google
     *  账号;未登录空串=隐藏。库页(LibraryViewModel 同款实现)与两个混合页共用这套口径。 */
    val sourceAccountThumbnail: StateFlow<String> =
        combine(
            dataStoreManager.selectedSource,
            dataStoreManager.neteaseAccountThumbUrl,
            dataStoreManager.getString("AccountThumbUrl"),
        ) { source, neteaseThumb, ytThumb ->
            if (source == com.maxrave.domain.source.MusicSource.NETEASE.name) neteaseThumb.trim() else (ytThumb ?: "").trim()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setSelectedSource(source: com.maxrave.domain.source.MusicSource) {
        viewModelScope.launch { dataStoreManager.setSelectedSource(source.name) }
    }

    /**
     * 统一的切源入口:三处切源路径(底栏长按菜单、网易登录成功自动切、登出/访客回落 YT)
     * 必须都走这里。只切 setting,**不停播、不清播放状态**(用户 2026-09-22 定案):播放管线
     * 按歌曲 ID 形状路由,混源队列本就能播,切源不打断正在听的音乐。持久化的队列/recentMediaId
     * 也保留,跨源残留在下次启动时由 core mayBeRestoreQueue 的跨源守卫(恢复曲 ID 形状≠当前源
     * 则跳过)兜住。同源重复调用无副作用。
     */
    fun switchSource(source: com.maxrave.domain.source.MusicSource) {
        Logger.w("SRCPROBE", "switchSource -> $source (current=${selectedSource.value})")
        if (selectedSource.value == source.name) return
        viewModelScope.launch {
            // 网易未登录选网易源:不切换,toast 引导登录(菜单项保持可点,别毫无反应)
            if (source == com.maxrave.domain.source.MusicSource.NETEASE &&
                dataStoreManager.neteaseCookie.first().isBlank()
            ) {
                Logger.w("SRCPROBE", "switchSource blocked: netease not logged in")
                makeToast(getString(Res.string.login_netease_first))
                return@launch
            }
            setSelectedSource(source)
        }
    }

    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

    private val appIdentity: AppIdentity by inject()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    private val _remoteSongLikeState = MutableStateFlow(RemoteSongLikeState())
    val remoteSongLikeState: StateFlow<RemoteSongLikeState> = _remoteSongLikeState

    /**
     * 点赞变更事件(videoId+liked+可选 Track):迷你条/播放页红心写成功(或 MV 写入成功
     * 且回读**确认成请求方向**——写失败/回读停在原状态/回读碰巧已是请求方向,都不发,
     * 那不是这笔写造成的变化)后发,开着的收藏列表页(红心歌单/MV 收藏)据此原地增删行
     * (2026-10-08 用户定案补齐业内实时档)。错过事件无妨——云端为准,下次进入/刷新自然
     * 对账;通知栏点赞走 core 另一路不发此事件。
     */
    private val _likeChangeEvent = MutableSharedFlow<LikeChangeEvent>(extraBufferCapacity = 8)
    val likeChangeEvent: kotlinx.coroutines.flow.SharedFlow<LikeChangeEvent> = _likeChangeEvent

    /**
     * 单次红心/MV 收藏写的落定事件(任意目标,含非在播):列表页对账移行(MV 收藏页/
     * 红心歌单)、sheet 复选框非乐观确认的消费源。与 remoteSongLikeState 分工——后者是
     * "在播曲的云端快照"(只随在播曲走),这里才是"这一笔写成了没"的事实通道,别混用。
     */
    private val _likeWriteResults = MutableSharedFlow<LikeWriteResult>(extraBufferCapacity = 16)
    val likeWriteResults: kotlinx.coroutines.flow.SharedFlow<LikeWriteResult> = _likeWriteResults

    var isServiceRunning: Boolean = false

    private val downloadUtils: DownloadHandler by inject<DownloadHandler>()

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    /**
     * Which extractor and cipher decoder produced the current track's URLs, shown in the info sheet.
     *
     * Read alongside the format rather than stored with it: the format row is cached and reused,
     * while this describes the extraction that happened in this run of the app.
     */
    private val _extractSource: MutableStateFlow<String?> = MutableStateFlow(null)
    val extractSource: StateFlow<String?> = _extractSource.asStateFlow()

    private var _canvas: MutableStateFlow<CanvasResult?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResult?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<GenericIntent?> = MutableStateFlow(null)
    val intent: StateFlow<GenericIntent?> = _intent

    private val _showNotificationPermissionDialog = MutableStateFlow(false)
    val showNotificationPermissionDialog: StateFlow<Boolean> = _showNotificationPermissionDialog

    private val _isOfficialBuild = MutableStateFlow(true)
    val isOfficialBuild: StateFlow<Boolean> = _isOfficialBuild

    // One-shot: the Desktop capsule asks the Now Playing panel, which hosts the page, to open
    // full-screen lyrics. The panel consumes it once shown.
    private val _fullscreenLyricsRequest = MutableStateFlow(false)
    val fullscreenLyricsRequest: StateFlow<Boolean> = _fullscreenLyricsRequest

    /**
     * 播放页三点删除云盘当前曲(不可逆,调用方负责确认弹窗):删除+toast;
     * 库页入口的 CloudDiskChanged 回写事件由调用方发(SharedViewModel 不持事件总线),
     * [onResult] 在删除落定后于主线程回调(true=删除成功)——库页回写必须等它,
     * 提前发事件会让库页轻拉抢在服务端可见前拿到旧计数/旧封面。
     */
    fun deleteCloudDiskSong(
        videoId: String,
        onResult: ((Boolean) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val id = videoId.toLongOrNull()
            val ok = id != null && neteaseRepository.deleteCloudDiskSong(id).getOrDefault(false)
            makeToast(
                if (ok) {
                    getString(Res.string.cloud_delete_done, 1)
                } else {
                    getString(Res.string.error_occurred)
                },
            )
            onResult?.invoke(ok)
        }
    }

    /**
     * 云盘歌曲信息纠正(2026-10-11 探针实证端点行为):adjustSongId=目标曲库歌曲 id。
     * 成功后服务端把条目 id 换掉——拿旧 videoId 的一切(队列行/正在播的歌)就此过期,
     * 调用方须整页重拉;解除路径见 [unlinkCloudDiskSong]。
     */
    fun matchCloudDiskSong(
        videoId: String,
        adjustSongId: Long,
        onResult: ((Boolean) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val id = videoId.toLongOrNull()
            val result = id?.let { neteaseRepository.matchCloudDiskSong(it, adjustSongId) }
            val ok = result?.isSuccess == true
            // 失败透传服务端消息("当前歌曲不支持匹配"等业务拒绝自解释,建单同款口径)
            makeToast(
                if (ok) {
                    getString(Res.string.cloud_match_done)
                } else {
                    result?.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: getString(Res.string.error_occurred)
                },
            )
            onResult?.invoke(ok)
        }
    }

    /**
     * 解除云盘歌曲的信息关联(asid=0):清掉手动/自动匹配,服务端会立刻重新指纹
     * 识别(并非回到裸文件,确认文案按此表述)。
     */
    fun unlinkCloudDiskSong(
        videoId: String,
        onResult: ((Boolean) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val id = videoId.toLongOrNull()
            val result = id?.let { neteaseRepository.matchCloudDiskSong(it, 0L) }
            val ok = result?.isSuccess == true
            makeToast(
                if (ok) {
                    getString(Res.string.cloud_unlink_done)
                } else {
                    result?.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: getString(Res.string.error_occurred)
                },
            )
            onResult?.invoke(ok)
        }
    }

    fun requestFullscreenLyrics() {
        _fullscreenLyricsRequest.value = true
    }

    fun consumeFullscreenLyricsRequest() {
        _fullscreenLyricsRequest.value = false
    }

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    fun getQueueDataState() = mediaPlayerHandler.queueData

    val castState: StateFlow<GenericCastState> get() = mediaPlayerHandler.castState

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
                volume = 1f,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    /**
     * Which body of the Apple Music player was open last — held by ENUM NAME so this class stays
     * ignorant of the UI enum, which is internal to the player package.
     *
     * It cannot live in the composable: that player is inside a ModalBottomSheet, so dismissing
     * the sheet disposes the whole tree and takes any rememberSaveable with it — the tab snapped
     * back to the artwork every single time it was reopened. This class is a Koin `single`, so it
     * outlives the sheet while still resetting on app restart, which is the right lifetime for
     * "where I was a moment ago".
     */
    private val _lastPlayerViewTab = MutableStateFlow<String?>(null)
    val lastPlayerViewTab: StateFlow<String?> = _lastPlayerViewTab

    fun setLastPlayerViewTab(tabName: String) {
        _lastPlayerViewTab.value = tabName
    }

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    init {
        viewModelScope.launch {
            nowPlayingState.collectLatest { state ->
                com.maxrave.domain.data.player.ExplicitVideoPlayback.onCurrentMediaChanged(
                    state?.mediaItem?.mediaId,
                )
            }
        }
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            val timeLineJob =
                launch {
                    nowPlayingState
                        .filterNotNull()
                        .flatMapLatest { nowPlayingState ->
                            timeline.map { timeLine ->
                                Pair(timeLine, nowPlayingState)
                            }
                        }.distinctUntilChanged { old, new ->
                            // A live stream's "total" is the seek window mpv reports, and it grows with
                            // every new segment — not a new length, so it must not re-run what a new
                            // length triggers (this fired every few seconds for as long as one played).
                            val sameLiveStream = old.first.isLive && new.first.isLive
                            (old.first.total.toString() + old.second.songEntity?.videoId).hashCode() ==
                                (new.first.total.toString() + new.second.songEntity?.videoId).hashCode() ||
                                (sameLiveStream && old.second.songEntity?.videoId == new.second.songEntity?.videoId)
                        }.collectLatest {
                            val nowPlaying = it.second
                            val timeline = it.first
                            // Lyrics and canvas both key off the track's length, which a live stream has none of.
                            if (timeline.isLive) return@collectLatest
                            log("Timeline job ${(it.first.total.toString() + it.second.songEntity?.videoId).hashCode()}")
                            if (timeline.total > 0 && nowPlaying.songEntity != null) {
                                if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                    Logger.w(tag, "Duration is ${timeline.total}")
                                    Logger.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                    getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                                }
                                nowPlaying.songEntity?.let { song ->
                                    // 判"该不该拉词"看 lyricsVideoId 而非 lyricsData==null:
                                    // 切歌保留旧词期间 lyricsData 非空但 id 是旧歌的,照样要拉;
                                    // 真没词的歌 updateLyrics(null) 会把 id 落成当前曲,不会重拉。
                                    if (nowPlayingScreenData.value.lyricsVideoId != song.videoId) {
                                        Logger.w(tag, "Get lyrics from format")
                                        getLyricsFromFormat(nowPlaying.mediaItem.isVideo(), song, (timeline.total / 1000).toInt())
                                    }
                                }
                            }
                        }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.distinctUntilChanged().collectLatest {
                        setLyricsProvider()
                    }
                }
            val shareSavedLyricsJob =
                launch {
                    dataStoreManager.helpBuildLyricsDatabase.distinctUntilChanged().collectLatest {
                        _shareSavedLyrics.value = it == TRUE
                    }
                }
            // Feed the parsed lyric lines to the service handler so it can drive the
            // notification-lyrics line (media card / capsule) even while no player screen is open.
            // Both modes carry the ORIGINAL lyrics — the notification replaces the artist slot
            // with the current line, and showing a translation there instead would swap the
            // language the user is actually listening to.
            val notificationLyricsJob =
                launch {
                    dataStoreManager.notificationLyricsMode
                        .combine(
                            nowPlayingScreenData
                                .map { it.lyricsData }
                                .distinctUntilChanged(),
                        ) { mode, data ->
                            mode to data
                        }.collect { (mode, data) ->
                            val lines =
                                data?.let { d ->
                                    val originalLines = d.lyrics.lines.orEmpty()
                                    val translatedLines = d.translatedLyrics?.first?.lines.orEmpty()
                                    if (mode == DataStoreManager.NOTIFICATION_LYRICS_MODE_ORIGINAL_AND_TRANSLATION && translatedLines.isNotEmpty()) {
                                        mergeTranslationIntoLines(originalLines, translatedLines)
                                    } else {
                                        originalLines
                                    }
                                }
                            mediaPlayerHandler.updateLyricLines(lines)
                        }
                }
//            val controllerStateJob =
//                launch {
//                    controllerState.map { it.isLiked }.distinctUntilChanged().collectLatest {
//                        if (dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE) {
//                            nowPlayingState.value?.mediaItem?.mediaId?.let {
//                                getLikeStatus(it)
//                            }
//                        }
//                    }
//                }
            timeLineJob.join()
            checkGetVideoJob.join()
            lyricsProviderJob.join()
            shareSavedLyricsJob.join()
            notificationLyricsJob.join()
//            controllerStateJob.join()
        }

        runBlocking {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    canvasJob?.cancel()
                    _nowPlayingState.value = state

                    // Seed the timeline from METADATA as soon as the track is known, instead of
                    // waiting for the player. SimpleMediaState.Ready carries a duration and only
                    // fires once the container is parsed; after a queue restore nothing plays, the
                    // position poll never starts (it runs only while isPlaying), and so nothing
                    // ever reports a length — which is why a restored queue showed no times at all.
                    // SongEntity has known the length since the song was first seen.
                    //
                    // Written on EVERY track change, not just when missing: leaving the old value
                    // in place would show the previous track's length over the new one.
                    // Order matters: metadata first because it is available immediately, then the
                    // player's own duration, and only -1 ("not known yet") when neither has one.
                    //
                    // Writing -1 whenever metadata is missing — which this did at first — makes
                    // every radio track and every first-time track flash NA:NA on the clock until
                    // the container is parsed, because those rows have no durationSeconds stored.
                    // Asking the player before giving up covers exactly that case: on a normal
                    // track change it usually already knows.
                    val metadataDurationMs = (state.songEntity?.durationSeconds ?: 0).toLong() * 1000L
                    val seededTotal =
                        metadataDurationMs.takeIf { it > 0L }
                            ?: mediaPlayerHandler.getPlayerDuration().takeIf { it > 0L }
                            ?: -1L
                    _timeline.update { it.copy(total = seededTotal) }
                    state.songEntity?.let { track ->
                        // 歌词 stale-while-revalidate:整包重建时旧词原样保留(lyricsVideoId
                        // 还是旧歌的 id = "过期"标记),新词由 updateLyrics 落地时整体替换。
                        // 之前的 `lyricsData = null` 让歌词区/整行歌词先塌掉再重建,就是
                        // 切歌"歌词闪一下"的根因;拉词触发改为按 lyricsVideoId 判过期。
                        val previousData = _nowPlayingScreenData.value
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = track.title,
                                artistName =
                                    track
                                        .artistName
                                        ?.joinToString(", ") ?: "",
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = previousData.lyricsData,
                                lyricsVideoId = previousData.lyricsVideoId,
                                // bitmap 同歌词的 stale-while-revalidate:Apple Music 主题的
                                // 磨砂背景直接画它,整包重建丢回 null 会让背景层塌成纯渐变,
                                // 新位图到位前是"背景闪一下"。旧值最多存活到下一次
                                // onArtworkBitmap 喂入(滑动 settle/翻页补发,内存缓存命中
                                // 几乎立即),远好于塌灰。
                                bitmap = previousData.bitmap,
                                songInfoData = null,
                                playlistName =
                                    mediaPlayerHandler.queueData.value
                                        ?.data
                                        ?.playlistName ?: "",
                            )
                        // 兜底:拉词链路若极端失败、始终没调 updateLyrics 落地,保留的旧词
                        // 不能挂一整首歌 — 10s 后仍是过期词就清空,回到"无词"态。
                        viewModelScope.launch {
                            delay(10_000)
                            if (_nowPlayingState.value?.songEntity?.videoId == track.videoId &&
                                _nowPlayingScreenData.value.lyricsVideoId != track.videoId
                            ) {
                                _nowPlayingScreenData.update {
                                    it.copy(lyricsData = null, lyricsVideoId = track.videoId)
                                }
                            }
                        }
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        if (now.mediaId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                            // 网易 MV:详情(计数/收藏态/评论线程)顶替 songInfo 卡;YT 详情全清
                            _nowPlayingScreenData.update {
                                it.copy(songInfoData = null, neteaseSongData = null, neteaseMvData = null)
                            }
                            getNeteaseMvData(now.mediaId)
                        } else if (now.mediaId.toLongOrNull() != null) {
                            // 网易歌:YT songInfo(含 RYD 踩数)/YT 点赞状态对数字 ID 必失败,
                            // 白打两个请求还刷 logcat;songInfoData 必须显式清空,否则
                            // copy(...) 会把上一首的详情卡一直带着走。点赞状态换成云村红心。
                            _nowPlayingScreenData.update {
                                it.copy(
                                    songInfoData = null,
                                    neteaseSongData = null,
                                    neteaseMvData = null,
                                )
                            }
                        } else {
                            _nowPlayingScreenData.update { it.copy(neteaseSongData = null, neteaseMvData = null) }
                            getSongInfo(now.mediaId)
                        }
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = now.metadata.artworkUri,
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                isExplicit = song.isExplicit,
                            )
                        }
                        refreshRemoteSongLike(song)
                        // 网易详情卡要 song 行的 artistId/albumId,在 songEntity 到位后拉
                        if (song.videoId.toLongOrNull() != null) {
                            getNeteaseSongInfo(song)
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    mediaPlayerHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }

                            SimpleMediaState.Stopped -> {
                                // 错误/灰歌动作后的闲置:停转圈,进度现场原样冻结——
                                // 灰歌暂停后 player 停在 IDLE,走 Initial 会永远转圈
                                _timeline.update { it.copy(loading = false) }
                            }

                            SimpleMediaState.Ended -> {
                                // Park at the end of the track rather than at -1. The only formatter
                                // for these fields renders any negative as "NA:NA", and nothing here
                                // is guaranteed to follow: at the end of the queue the player simply
                                // stays ended, so a -1 written here stays on screen. Worse, the
                                // Progress branch below ignores negative values and the Loading
                                // branch restores `total` without touching `current`, which is how
                                // the player ends up showing a correct duration next to "NA:NA".
                                _timeline.update {
                                    it.copy(
                                        current = it.total.coerceAtLeast(0L),
                                        bufferedPercent = 0,
                                        loading = false,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    // A live stream never reports a length (ExoPlayer: C.TIME_UNSET), so
                                    // a missing one means "loading" for everything except a live stream.
                                    if (_timeline.value.total > 0L || _timeline.value.isLive) {
                                        _timeline.update {
                                            it.copy(
                                                total = mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: it.total,
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: it.total,
                                            )
                                        }
                                    }
                                }
                                // When progress hasn't changed (same value polled again) or is negative,
                                // don't modify loading state. The loading flag is already managed by
                                // Buffering/Ready/Loading state events. Setting loading=true here would
                                // cause rapid flickering whenever the same value arrives twice,
                                // which it can: the handler's ticker and the adapter's position
                                // poll both run at 50ms and are not in step, so a poll is sometimes
                                // read twice. A repeat is not evidence of a stall, and the loading
                                // flag belongs to the Buffering/Ready events rather than to a
                                // guess made here.
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = mediaPlayerHandler.getProgress(),
                                        loading = false,
                                        // The player's own duration wins, but ONLY when it has one.
                                        // ExoPlayer answers C.TIME_UNSET (a large negative, not
                                        // null) until it has parsed the container, and Ready is
                                        // also published from onIsLoadingChanged — which fires
                                        // before STATE_READY. Writing that would throw away the
                                        // metadata duration seeded on the track change above.
                                        total = mediaState.duration.takeIf { d -> d > 0L } ?: it.total,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Logger.w(tag, "ControllerJob is running")
                    mediaPlayerHandler.controlState.collectLatest {
                        Logger.w(tag, "ControlState is $it")
                        _controllerState.value = it
                        // Propagate crossfade state to timeline so UI can react
                        _timeline.update { timeline ->
                            timeline.copy(isCrossfading = it.isCrossfading)
                        }
                    }
                }
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    mediaPlayerHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            val liveStreamJob =
                launch {
                    // A track is only known to be live once its stream has been resolved, so this
                    // follows the registry as well as the track itself.
                    combine(mediaPlayerHandler.nowPlayingState, LiveStreamRegistry.liveVideoIds) { state, liveVideoIds ->
                        state.mediaItem.mediaId in liveVideoIds
                    }.distinctUntilChanged()
                        .collect { isLive -> _timeline.update { it.copy(isLive = isLive) } }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
            liveStreamJob.join()
        }
        // Reset downloading songs & playlists to not downloaded
        checkAllDownloadingSongs()
        checkAllDownloadingPlaylists()
        checkAllDownloadingLocalPlaylists()
        // The counterpart the resets above have never had: a container is only promoted to
        // STATE_DOWNLOADED from its own screen's ViewModel, so a download finishing after the
        // user left the screen left it stuck at "downloading" forever.
        promoteCompletedDownloads()
    }

    fun setIntent(intent: GenericIntent?) {
        _intent.value = intent
    }

    /**
     * Finishes a Last.fm login from the `wordbyword://lastfm-auth` callback.
     *
     * It lands here, and not on the login screen, because the callback arrives at the app rather
     * than at any one screen: the user left for their browser, and the screen they left from may
     * not even exist any more if the process was killed. Routing the token onwards through
     * navigation would push a second copy of the login screen on top of the one already open.
     *
     * The screen learns it succeeded by watching the stored session key, not by being told.
     */
    fun completeLastfmLogin(token: String) {
        if (token.isEmpty()) return
        viewModelScope.launch {
            val session = completeLogin(token)
            if (session != null) {
                dataStoreManager.setLastfmSession(
                    sessionKey = session.sessionKey,
                    username = session.username,
                )
                makeToast(getString(Res.string.login_success))
            } else {
                makeToast(getString(Res.string.lastfm_login_failed))
            }
        }
    }

    fun showNotificationPermissionDialog() {
        _showNotificationPermissionDialog.value = true
    }

    fun dismissNotificationPermissionDialog(doNotShowAgain: Boolean) {
        _showNotificationPermissionDialog.value = false
        if (doNotShowAgain) {
            putString("notification_permission_do_not_ask", "true")
        }
    }

    /** 网易歌详情卡:艺人(头像/粉丝)+专辑(发行/简介)+评论(总数/热评),各路独立降级 */
    private var neteaseSongInfoJob: Job? = null

    private fun playingPodcastProgramId(videoId: String): Long? {
        return nowPlayingState.value?.track?.takeIf { it.videoId == videoId }?.neteaseProgramId
            ?: nowPlayingState.value?.songEntity?.takeIf { it.videoId == videoId }?.neteaseProgramId
            ?: mediaPlayerHandler.queueData.value?.data?.listTracks?.firstOrNull { it.videoId == videoId }?.neteaseProgramId
    }

    /**
     * 红心=云端账号状态(云端是唯一事实源):切歌时拉云端态,驱动显示与本地缓存镜像
     * (song.liked 行,库页"喜欢的歌曲"等本地视图读它)。未登录/拉取失败时保持本地
     * 缓存值显示,操作会走云端并给出失败提示。
     */
    private fun refreshRemoteSongLike(song: SongEntity) {
        _remoteSongLikeState.value = RemoteSongLikeState(videoId = song.videoId)
        // MV 收藏态由 getNeteaseMvData 的 subed 驱动,不走歌曲云端红心(YT 端点对 MV id 白打)
        if (song.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) return
        if (song.neteaseProgramId != null || mediaPlayerHandler.queueData.value?.data?.isNeteasePodcastQueue == true) {
            val programId = song.neteaseProgramId ?: playingPodcastProgramId(song.videoId) ?: return
            viewModelScope.launch {
                val liked = neteaseRepository.getDjProgramLikeInfo(programId).getOrNull()?.liked
                if (playingMediaId() == song.videoId) {
                    _remoteSongLikeState.value = RemoteSongLikeState(liked = liked, videoId = song.videoId)
                    if (liked != null) {
                        _liked.value = liked
                        mediaPlayerHandler.like(liked)
                    }
                }
                // 镜像回写 Room(歌曲分支同款):handler 的曲目装载/通知刷新路径都读
                // song.liked,不回写的话云端真值落地后 100ms 内就会被 Room 旧值翻回
                if (liked != null) {
                    val localLiked = songRepository.getSongById(song.videoId).lastOrNull()?.liked == true
                    if (localLiked != liked) {
                        songRepository.setLikedLocal(song.videoId, if (liked) 1 else 0)
                    }
                }
            }
            return
        }
        viewModelScope.launch {
            val cloudLiked = songRepository.getRemoteLikeStatus(song.videoId)
            _remoteSongLikeState.value = RemoteSongLikeState(liked = cloudLiked, videoId = song.videoId)
            if (cloudLiked != null) {
                _liked.value = cloudLiked
                mediaPlayerHandler.like(cloudLiked)
                val localLiked = songRepository.getSongById(song.videoId).lastOrNull()?.liked == true
                if (localLiked != cloudLiked) {
                    songRepository.setLikedLocal(song.videoId, if (cloudLiked) 1 else 0)
                }
            }
        }
    }

    /**
     * 红心点击的唯一路径:直接切换云端账号红心。成功后镜像本地缓存(song.liked,供
     * 库页本地视图)并同步通知栏图标;失败 toast。迷你播放条红心也走这里。
     */
    /** 红心/收藏在未登录源上被点击时:置灰按钮仍可点,点这里给统一提示。 */
    fun notifyFavoriteNeedsLogin() {
        makeToast(getString(Res.string.need_login_toast))
    }

    /**
     * 云端红心写入口。videoId=null(默认)=作用于正在播放的曲目(播放页红心);
     * 传 videoId=作用于指定曲目——列表行菜单(收藏 MV 列表/歌单页等)打开的 sheet 里,
     * 目标行未必在播,不传会错打到 nowPlayingState 的当前曲。
     *
     * 目标≠在播曲时只动目标自己的账(本地镜像行+likeChangeEvent+likeWriteResults),
     * 绝不碰全局在播态(_liked/controllerState 红心/Info 卡/remoteSongLikeState)——
     * 那些是"在播曲的快照",被别曲的写污染会让播放器红心/通知图标错翻、ToggleLike
     * 按错方向翻转(2026-10-09 CR)。
     */
    fun setRemoteSongLiked(
        liked: Boolean,
        videoId: String? = null,
    ) {
        val targetId = videoId ?: nowPlayingState.value?.songEntity?.videoId ?: nowPlayingState.value?.track?.videoId ?: return
        val isPlayingTarget = playingMediaId() == targetId
        if (isPlayingTarget && mediaPlayerHandler.queueData.value?.data?.isNeteasePodcastQueue == true && playingPodcastProgramId(targetId) == null) {
            makeToast(getString(Res.string.cloud_action_failed_netease))
            return
        }
        val programId = if (isPlayingTarget) playingPodcastProgramId(targetId) else null
        if (programId != null) {
            if (!neteaseLoggedIn.value) {
                notifyFavoriteNeedsLogin()
                return
            }
            if (_remoteSongLikeState.value.pending) return
            _remoteSongLikeState.value = _remoteSongLikeState.value.copy(pending = true, failed = false, videoId = targetId)
            viewModelScope.launch {
                val written = neteaseRepository.setDjProgramLiked(programId, liked).isSuccess
                var confirmed: Boolean? = null
                // 节目资源赞是异步落地；2026-10-09 探针观察到即时详情仍是旧态。
                for (attempt in 0 until 10) {
                    delay(800)
                    confirmed = neteaseRepository.getDjProgramLikeInfo(programId).getOrNull()?.liked
                    if (confirmed == liked || !written) break
                }
                val success = written && confirmed == liked
                // 镜像回写 Room(歌曲写路径同款):写已确认落服务端,不回写的话 handler
                // 任意一次 Room 读都会把显示翻回旧值(节目赞此前不落库的根因)
                if (confirmed != null) {
                    songRepository.setLikedLocal(targetId, if (confirmed) 1 else 0)
                }
                if (playingMediaId() == targetId && playingPodcastProgramId(targetId) == programId) {
                    if (confirmed != null) {
                        _liked.value = confirmed
                        mediaPlayerHandler.like(confirmed)
                    }
                    _remoteSongLikeState.value = RemoteSongLikeState(
                        liked = confirmed,
                        pending = false,
                        failed = !success,
                        videoId = targetId,
                    )
                }
                makeToast(getString(if (success) { if (liked) Res.string.liked_toast else Res.string.unliked_toast } else Res.string.cloud_action_failed_netease))
            }
            return
        }
        // 网易 MV:红心=收藏/取消收藏 MV。**服务端对账模式**:写操作后一律重拉 mvDetail
        // 的 subed 驱动显示,避免写入延迟或服务端拒绝时本地状态误报成功。
        if (targetId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
            val mvid = targetId.removePrefix(NETEASE_MV_MEDIA_ID_PREFIX).toLongOrNull() ?: return
            if (isPlayingTarget) {
                _remoteSongLikeState.value = _remoteSongLikeState.value.copy(pending = true, failed = false, videoId = targetId)
            }
            viewModelScope.launch {
                val ok = neteaseRepository.subscribeMv(mvid, liked).getOrDefault(false)
                // 回读校正(写成功/失败都读:失败可能是假失败,读到的才是真相)。
                // 延迟 800ms:收藏写库是异步的,写完立刻回读撞上主从延迟会拿到旧态
                // (实测 mv/sub 200 后 88ms 回读 subed 仍是 false,3s 后已变 true)
                delay(800)
                val fresh = neteaseRepository.getMvDetail(mvid).getOrNull()
                val subed = fresh?.subed
                // toast 以回读结果为准。回读失败时不能确认取消已生效。
                when {
                    !ok -> makeToast(getString(Res.string.cloud_action_failed_netease))
                    subed == null ->
                        makeToast(
                            getString(
                                if (liked) Res.string.mv_subscribed else Res.string.cloud_action_failed_netease,
                            ),
                        )
                    subed == liked ->
                        makeToast(getString(if (liked) Res.string.mv_subscribed else Res.string.mv_unsubscribed))
                    else -> makeToast(getString(Res.string.cloud_action_failed_netease))
                }
                if (subed != null) {
                    // 在播判定回读后复核:写等待窗口里可能已切歌
                    val stillPlayingTarget = playingMediaId() == targetId
                    if (stillPlayingTarget) {
                        _liked.value = subed
                        mediaPlayerHandler.like(subed)
                        _nowPlayingScreenData.update {
                            it.copy(neteaseMvData = fresh ?: it.neteaseMvData)
                        }
                    }
                    // 收藏变化事件只在"写入成功且回读确认成请求方向"(ok && subed==liked)时发
                    // ——与 toast 首分支/likeWriteResults 的成功判定三处同一口径。只看回读
                    // 不看写结果的话:写失败+回读碰巧已是请求方向(菜单初态过期,如未收藏
                    // MV 实际早已收藏)会被收藏页当成变化,误插行/误减数,而这笔写被判失败。
                    // 回读旧态仍照常校正播放器红心(上面分支),两件事口径不同。
                    if (ok && subed == liked) {
                        _likeChangeEvent.tryEmit(
                            LikeChangeEvent(
                                videoId = targetId,
                                liked = subed,
                                track = fresh?.let { neteaseRepository.mvTrackFromDetail(it) },
                            ),
                        )
                    }
                }
                // UI 状态与 toast 同口径:取消方向 subed=null 时保持点前状态(仅对在播目标,
                // 复核切歌;非在播目标写 remoteSongLikeState 会盖掉在播曲的快照)。
                if (playingMediaId() == targetId) {
                    _remoteSongLikeState.value =
                        if (ok && (subed != null || liked)) {
                            RemoteSongLikeState(liked = subed ?: liked, videoId = targetId)
                        } else {
                            _remoteSongLikeState.value.copy(pending = false, failed = true)
                        }
                }
                // 任意目标都发落定事件:列表页对账移行/sheet 复选框确认靠它
                _likeWriteResults.tryEmit(LikeWriteResult(targetId, liked, ok && subed == liked))
            }
            return
        }
        viewModelScope.launch {
            if (isPlayingTarget) {
                _remoteSongLikeState.value = _remoteSongLikeState.value.copy(pending = true, failed = false, videoId = targetId)
            }
            val result = songRepository.setRemoteLikeStatus(targetId, liked)
            val ok = result.getOrDefault(false)
            if (!ok) {
                makeToast(
                    getString(
                        when {
                            result.exceptionOrNull() is com.maxrave.netease.NeteaseRateLimitException ->
                                Res.string.netease_rate_limited
                            targetId.toLongOrNull() != null -> Res.string.cloud_action_failed_netease
                            else -> Res.string.cloud_action_failed_youtube
                        },
                    ),
                )
            } else {
                makeToast(getString(if (liked) Res.string.liked_toast else Res.string.unliked_toast))
                songRepository.setLikedLocal(targetId, if (liked) 1 else 0)
                if (playingMediaId() == targetId) {
                    _liked.value = liked
                    mediaPlayerHandler.like(liked)
                }
                // 迷你条/播放页红心写成功:带在播 Track(目标=在播曲时)通知开着的收藏页。
                // track 在队列 rebuild 窗口恒 null,回退 songEntity(九轮铁律)——
                // 恢复队列起播的场景 track 长时间为 null,不兜底收藏方向永远插不了行
                val playingTrack =
                    nowPlayingState.value?.track
                        ?: nowPlayingState.value?.songEntity?.toTrack()
                playingTrack
                    ?.takeIf { it.videoId == targetId }
                    .let { _likeChangeEvent.tryEmit(LikeChangeEvent(targetId, liked, it)) }
            }
            if (isPlayingTarget) {
                _remoteSongLikeState.value =
                    if (ok) {
                        RemoteSongLikeState(liked = liked, videoId = targetId)
                    } else {
                        _remoteSongLikeState.value.copy(pending = false, failed = true)
                    }
            }
            _likeWriteResults.tryEmit(LikeWriteResult(targetId, liked, ok))
        }
    }

    /** 当前在播曲目 id:mediaItem 为准(切歌窗口权威),songEntity 兜底(rebuild 窗口) */
    private fun playingMediaId(): String? =
        mediaPlayerHandler.nowPlayingState.value.mediaItem?.mediaId
            ?: nowPlayingState.value?.songEntity?.videoId

    private fun getNeteaseSongInfo(song: SongEntity) {
        neteaseSongInfoJob?.cancel()
        neteaseSongInfoJob =
            viewModelScope.launch {
                val meta =
                    neteaseRepository.getSongInfo(
                        songId = song.videoId,
                        artistId = song.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() },
                        albumId = song.albumId,
                    )
                // 切歌竞态:发布前确认还是这首歌,别把上一首的卡盖到新歌上
                if (mediaPlayerHandler.nowPlayingState.value.songEntity?.videoId == song.videoId) {
                    _nowPlayingScreenData.update { it.copy(neteaseSongData = meta) }
                }
            }
    }

    /** 网易 MV 详情:Info 卡数据 + 收藏态(红心初态=subed)+ 评论线程;切歌竞态防护同上 */
    private var neteaseMvJob: Job? = null

    private fun getNeteaseMvData(mediaId: String) {
        neteaseMvJob?.cancel()
        neteaseMvJob =
            viewModelScope.launch {
                val mvid = mediaId.removePrefix(NETEASE_MV_MEDIA_ID_PREFIX).toLongOrNull() ?: return@launch
                val detail = neteaseRepository.getMvDetail(mvid).getOrNull() ?: return@launch
                // 发布前确认还是这首 MV,别把上一支的卡盖到新曲上
                if (mediaPlayerHandler.nowPlayingState.value.mediaItem?.mediaId == mediaId) {
                    val firstArtist = detail.artists.firstOrNull()
                    _nowPlayingScreenData.update {
                        it.copy(
                            neteaseMvData = detail,
                            neteaseSongData = firstArtist?.let { (id, name) ->
                                NeteaseSongInfoEntity(artistId = id.toString(), artistName = name)
                            },
                        )
                    }
                    detail.subed?.let { sub ->
                        _liked.value = sub
                        mediaPlayerHandler.like(sub)
                    }
                }
                // mv/detail 的评论数可能滞后；列表 v2 返回该线程总数，回写信息卡。
                launch {
                    val count = neteaseRepository.getSongCommentsPage(
                        songId = mediaId,
                        sortType = 3,
                        cursor = "0",
                        pageSize = 1,
                        threadId = detail.commentThreadId ?: "R_MV_5_$mvid",
                    )?.totalCount
                    if (count != null && mediaPlayerHandler.nowPlayingState.value.mediaItem?.mediaId == mediaId) {
                        _nowPlayingScreenData.update { state ->
                            state.copy(neteaseMvData = state.neteaseMvData?.copy(commentCount = count.toLong()))
                        }
                    }
                }
                val artistInfo = neteaseRepository.getMvArtistInfo(detail)
                if (mediaPlayerHandler.nowPlayingState.value.mediaItem?.mediaId == mediaId) {
                    _nowPlayingScreenData.update { it.copy(neteaseSongData = artistInfo) }
                }
            }
    }

    /** 网易 MV 播放入口(菜单"看 MV"/歌手页/主页 MV 货架共用):单一 MV 队列,
     *  playlistId=NETEASE_MV_<mvid> 哨兵(loadMore 见前缀即止);PLAYLIST_CLICK 整队装载。
     *  不走 loadMediaItemFromTrack——它先 insertSong,MV 不落 song 表。 */
    fun playMv(track: Track) {
        viewModelScope.launch {
            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = arrayListOf(track),
                    firstPlayedTrack = track,
                    playlistId = track.videoId,
                    playlistName = track.title,
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            mediaPlayerHandler.loadMediaItem(track, PLAYLIST_CLICK, 0)
        }
    }

    fun playYouTubeMv(track: Track) {
        com.maxrave.domain.data.player.ExplicitVideoPlayback.select(track.videoId)
        playMv(track)
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Logger.w(tag, "Start getCanvas: $videoId $duration")
//        canvasJob?.cancel()
        viewModelScope.launch {
            // Both sources fill the same slot, so they are tried in order rather than raced:
            // animated artwork first, and a Spotify canvas only if that found nothing AND the user
            // has that switch on too. Neither switch touches the other — a track with no animated
            // artwork still gets its canvas, and turning Spotify off still means no canvas at all.
            val sources =
                buildList {
                    if (dataStoreManager.amAnimatedArtwork.first() == TRUE) {
                        add(lyricsCanvasRepository.getAMAnimatedArtwork(videoId))
                    }
                    if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                        add(lyricsCanvasRepository.getCanvas(dataStoreManager, videoId, duration))
                    }
                }
            if (sources.isEmpty()) return@launch

            var resolved = false
            for (source in sources) {
                if (resolved) break
                source.cancellable().collect { response ->
                    val data = response.data
                    if (response is Resource.Success && data != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) {
                        resolved = true
                        _canvas.value = data
                        _nowPlayingScreenData.update {
                            it.copy(
                                canvasData =
                                    NowPlayingScreenData.CanvasData(
                                        isVideo = data.isVideo,
                                        url = data.canvasUrl,
                                        thumbUrl = data.canvasThumbUrl,
                                    ),
                            )
                        }
                        // Save canvas video url
                        if (data.isVideo) lyricsCanvasRepository.updateCanvasUrl(videoId, data.canvasUrl)
                        // Save canvas thumb url
                        data.canvasThumbUrl?.let { lyricsCanvasRepository.updateCanvasThumbUrl(videoId, it) }
                    } else {
                        log("Get canvas miss from a source: ${response.message}")
                    }
                }
            }

            if (!resolved) {
                nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                    _nowPlayingScreenData.update {
                        it.copy(
                            canvasData =
                                NowPlayingScreenData.CanvasData(
                                    isVideo = url.isCanvasVideoUrl(),
                                    url = url,
                                    thumbUrl = nowPlayingState.value?.songEntity?.canvasThumbUrl,
                                ),
                        )
                    }
                }
            }
        }
    }

    fun getString(key: String): String? = runBlocking { dataStoreManager.getString(key).first() }

    /**
     * The `promos` block the last config fetch saved, as it changes. Every DataStore write re-emits
     * the whole settings file (the playback position is saved every few seconds), so only a change
     * to this block's own text gets through.
     */
    fun cachedPromos(): Flow<List<Promo>> =
        dataStoreManager
            .getString(Promo.cacheKey(appIdentity.isDevBuild))
            .distinctUntilChanged()
            .map(::decodePromos)

    /** The ids of the launch banners this app version has already shown. */
    suspend fun shownPromos(): Set<String> =
        shownPromoIds(
            stored = dataStoreManager.getString(Promo.shownKey(appIdentity.isDevBuild)).first(),
            appVersion = VersionManager.getVersionName(),
        )

    /** Each banner once per app version, however it was closed. */
    fun markPromoShown(id: String) {
        viewModelScope.launch {
            val key = Promo.shownKey(appIdentity.isDevBuild)
            dataStoreManager.putString(
                key,
                withShownPromo(dataStoreManager.getString(key).first(), VersionManager.getVersionName(), id),
            )
        }
    }

    // A Library tab asked for from outside the Library screen (a deep link); the screen selects it
    // the next time it composes and clears the request.
    private val _libraryTabRequest = MutableStateFlow<LibraryChipType?>(null)
    val libraryTabRequest: StateFlow<LibraryChipType?> = _libraryTabRequest.asStateFlow()

    fun requestLibraryTab(tab: LibraryChipType?) {
        _libraryTabRequest.value = tab
    }

    fun putString(
        key: String,
        value: String,
    ) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        mediaPlayerHandler.sleepStop()
    }

    private var _downloadState: MutableStateFlow<DownloadHandler.Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<DownloadHandler.Download?> = _downloadState.asStateFlow()

    fun checkIsRestoring() {
        viewModelScope.launch {
            val downloadedCacheKeys = cacheRepository.getAllCacheKeys(DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    // 文件式(2026-10 二期)以文件为准:转存成功后 SimpleCache 已清,按缓存
                    // key 对账会把"文件在+state3"全数误杀成 0(实测 75 行全灭,点击无反应+
                    // 菜单显示未下载)。缓存 key 对账只管旧 SimpleCache 代;文件丢失态由
                    // 下载管理页的 FILE_MISSING 展示与播放 resolver 自愈负责。
                    val fileBased =
                        song.downloadedFilePath != null || song.downloadedVideoFilePath != null
                    if (!fileBased && !downloadedCacheKeys.contains(song.videoId)) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            playlistRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                albumRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                playlistRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                localPlaylistRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            lyricsCanvasRepository.insertLyrics(lyrics)
        }
    }

    private fun getSavedLyrics(track: Track) {
        viewModelScope.launch {
            lyricsCanvasRepository.getSavedLyrics(track.videoId).cancellable().collectLatest { lyrics ->
                if (lyrics != null) {
                    val lyricsData = lyrics.toLyrics()
                    Logger.d(tag, "Saved Lyrics $lyricsData")
                    updateLyrics(
                        track.videoId,
                        track.durationSeconds ?: 0,
                        lyricsData,
                        false,
                        LyricsProvider.OFFLINE,
                    )
                    getAITranslationLyrics(
                        track.videoId,
                        lyricsData,
                    )
                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            val localSong = songRepository.getSongById(videoId).firstOrNull()
            if (localSong != null) {
                val track = localSong.toTrack()
                mediaPlayerHandler.setQueueData(
                    QueueData.Data(
                        listTracks = arrayListOf(track),
                        firstPlayedTrack = track,
                        playlistId = "RDAMVM$videoId",
                        playlistName = getString(Res.string.shared),
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItemFromTrack(track, SONG_CLICK)
            } else if (videoId.toLongOrNull() != null) {
                // 网易歌本地无行:getFullMetadata 是 YT 管线,数字 id 必抓取失败(分享回流断的
                // 根因)——按 id 走网易 songDetail 直取,与 YT 分享同构起播
                val track = neteaseRepository.getNeteaseSongTrack(videoId)
                if (track != null) {
                    mediaPlayerHandler.setQueueData(
                        QueueData.Data(
                            listTracks = arrayListOf(track),
                            firstPlayedTrack = track,
                            playlistId = "RDAMVM$videoId",
                            playlistName = getString(Res.string.shared),
                            playlistType = PlaylistType.RADIO,
                            continuation = null,
                        ),
                    )
                    loadMediaItemFromTrack(track, SONG_CLICK)
                } else {
                    makeToast(getString(Res.string.error))
                }
            } else {
                streamRepository.getFullMetadata(videoId).collectLatest { response ->
                    val track = response.data
                    when (response) {
                        is Resource.Success if (track != null) -> {
                            mediaPlayerHandler.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(track),
                                    firstPlayedTrack = track,
                                    playlistId = "RDAMVM$videoId",
                                    playlistName = getString(Res.string.shared),
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            loadMediaItemFromTrack(track, SONG_CLICK)
                        }

                        else -> {
                            log("Load shared media item error: ${response.message}", LogLevel.WARN)
                            makeToast("${getString(Res.string.error)}: ${response.message}")
                        }
                    }
                }
            }
        }
    }

    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.clearMediaItems()
            songRepository.insertSong(track.toSongEntity()).lastOrNull()?.let {
                println("insertSong: $it")
                songRepository
                    .getSongById(track.videoId)
                    .collect { songEntity ->
                        if (songEntity != null) {
                            Logger.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                            _liked.value = songEntity.liked
                        }
                    }
            }
            track.durationSeconds?.let {
                songRepository.updateDurationSeconds(
                    it,
                    track.videoId,
                )
            }
            withContext(Dispatchers.Main) {
                mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }

            when (type) {
                SONG_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                VIDEO_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                SHARE -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                PLAYLIST_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                ALBUM_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }
            }
        }
    }

    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )
                }

                UIEvent.Forward -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Forward)
                }

                UIEvent.PlayPause -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )
                }

                UIEvent.Next -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                }

                UIEvent.Previous -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )
                }

                UIEvent.SkipToPrevious -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.SkipToPrevious,
                    )
                }

                UIEvent.Stop -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Stop)
                }

                is UIEvent.UpdateProgress -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                }

                UIEvent.Shuffle -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle)
                }

                UIEvent.ToggleLike -> {
                    // 红心直连云端:当前态优先读云端快照——只认属于在播曲的快照(列表菜单
                    // 对别曲的写不再进这里),未知/不属于时回落本地镜像
                    Logger.w(tag, "ToggleLike")
                    val snapshot = _remoteSongLikeState.value
                    val current =
                        snapshot.liked
                            .takeIf { snapshot.videoId == playingMediaId() }
                            ?: _liked.value
                    setRemoteSongLiked(!current)
                }

                is UIEvent.UpdateVolume -> {
                    val newVolume = uiEvent.newVolume
                    // Apply to the player first: persisting to DataStore is a suspending disk write
                    // and must not sit between the user's gesture and the audible change.
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateVolume(newVolume))
                    dataStoreManager.setPlayerVolume(newVolume)
                }
            }
        }

    override fun onCleared() {
        Logger.w("Check onCleared", "onCleared")
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    private fun checkAllDownloadingLocalPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllDownloadingLocalPlaylists().collectLatest { playlists ->
                playlists.forEach { playlist ->
                    localPlaylistRepository.updateDownloadState(playlist.id, 0, successMessage = getString(Res.string.updated)).lastOrNull()
                }
            }
        }
    }

    private fun checkAllDownloadingPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadingPlaylist().collectLatest { list ->
                list.forEach { data ->
                    when (data) {
                        is AlbumEntity -> {
                            albumRepository.updateAlbumDownloadState(data.browseId, 0)
                        }

                        is PlaylistEntity -> {
                            playlistRepository.updatePlaylistDownloadState(data.id, 0)
                        }

                        else -> {
                            // Skip
                        }
                    }
                }
            }
        }
    }

    private fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            // 只清理"不在 DownloadManager 队列"的 1/2 态遗留(进程被杀后的孤儿态)——
            // 文件式的"转存中"(state2,条目 COMPLETED 等 exporter)在队列里,误杀会被
            // collect 重放反复打 0(2026-10 实测与启动对账竞态后 state 全灭)
            songRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    if (downloadUtils.downloads.value[song.videoId] == null) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            songRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    if (downloadUtils.downloads.value[song.videoId] == null) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
        }
    }

    /**
     * Promote "downloading" containers to STATE_DOWNLOADED from this app-scoped singleton, so a
     * playlist/album still gets marked when its downloads finish after the user has left its
     * screen — the screen ViewModels only do this while they are alive.
     */
    private fun promoteCompletedDownloads() {
        viewModelScope.launch {
            downloadUtils.downloadTask.collect { tasks ->
                // Only re-check when at least one track has actually completed; progress noise
                // never reaches this map, but a failure-only emission has nothing to promote.
                if (!tasks.values.any { it == DownloadState.STATE_DOWNLOADED }) {
                    return@collect
                }
                playlistRepository.getAllDownloadingPlaylist().first().forEach { container ->
                    val tracks =
                        when (container) {
                            is AlbumEntity -> container.tracks
                            is PlaylistEntity -> container.tracks
                            else -> null
                        }
                    if (!tracks.isNullOrEmpty() && allTracksDownloaded(tracks)) {
                        when (container) {
                            is AlbumEntity -> albumRepository.updateAlbumDownloadState(container.browseId, DownloadState.STATE_DOWNLOADED)
                            is PlaylistEntity -> playlistRepository.updatePlaylistDownloadState(container.id, DownloadState.STATE_DOWNLOADED)
                            else -> {}
                        }
                    }
                }
                localPlaylistRepository.getAllDownloadingLocalPlaylists().first().forEach { playlist ->
                    val tracks = playlist.tracks
                    if (!tracks.isNullOrEmpty() && allTracksDownloaded(tracks)) {
                        localPlaylistRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, playlist.id)
                    }
                }
            }
        }
    }

    private suspend fun allTracksDownloaded(tracks: List<String>): Boolean =
        songRepository.getSongsByListVideoId(tracks).first().let { songs ->
            songs.size == tracks.size && songs.all { it.downloadState == DownloadState.STATE_DOWNLOADED }
        }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            _extractSource.value = streamRepository.getExtractSource(mediaId)
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.d(tag, "Get format for $mediaId: itag ${f?.itag}, expires ${f?.expiredTime}")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                        // Re-read on every emission: the first one usually lands before the
                        // extractor has finished, so the source is only known on a later pass.
                        _extractSource.value = streamRepository.getExtractSource(mediaId)
                    }
                }
        }
    }

    private var songInfoJob: Job? = null

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    songRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }

    private var _updateResponse = MutableStateFlow<UpdateData?>(null)
    val updateResponse: StateFlow<UpdateData?> = _updateResponse

    fun checkForUpdate() {
        // Raised before the coroutine starts, so Home's launch banner, which reads it right after
        // startup, can never miss a check that is already under way but not yet marked.
        _isCheckingUpdate.value = true
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            // 更新检查是纯增益功能,任何残余异常都不许把 app 带崩(2026-09-30 真机崩溃:
            // 无代理环境两段 GitHub 请求先后超时,兜底路径曾把 SocketTimeoutException 裸抛
            // 出 collectLatest)。scraper/repo 层已捕获,这里是第二道防线;finally 同时
            // 保证失败路径不会把 _isCheckingUpdate 永远留在 true。
            try {
                // Keep this read ahead of the first DataStore write: on a fresh install
                // MainActivity performs runBlocking DataStore writes on the main thread,
                // and enqueuing our write before theirs can wedge the DataStore actor
                // behind the blocked main looper, leaving the app stuck on the splash.
                dataStoreManager.updateChannel.first()
                dataStoreManager.putString(
                    "CheckForUpdateAt",
                    System.currentTimeMillis().toString(),
                )
                updateRepository.checkForGithubReleaseUpdate().collectLatest { response ->
                    val data = response.data
                    when (response) {
                        is Resource.Success if (data != null) -> {
                            _updateResponse.value = data
                            showedUpdateDialog = true
                        }

                        else -> {
                            // API 路径失败(最常见=api.github.com 匿名 60 次/小时限流,共享出口 IP
                            // 极易撞上):改走 HTML 重定向兜底——releases/latest 302 目标含最新 tag,
                            // 网页路径限额宽松。双路都失败才提示(此前失败静默吞掉,用户读作"没反应")。
                            log("Check for update via API failed (${response.message}), falling back to redirect probe", LogLevel.WARN)
                            updateRepository.checkForGithubReleaseUpdateViaRedirect().collectLatest { fb ->
                                val fbData = fb.data
                                if (fb is Resource.Success && fbData != null) {
                                    _updateResponse.value = fbData
                                    showedUpdateDialog = true
                                } else {
                                    log("Check for update via redirect also failed: ${fb.message}", LogLevel.WARN)
                                    makeToast(getString(Res.string.update_check_failed))
                                }
                            }
                            return@collectLatest
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("Check for update threw outside repo layer: ${e.message}", LogLevel.WARN)
                makeToast(getString(Res.string.update_check_failed))
            } finally {
                _isCheckingUpdate.value = false
            }
        }.invokeOnCompletion {
            // Also for a channel neither branch handles, and for a check that throws: the launch
            // banner waits for this flag, so it must never stay raised.
            _isCheckingUpdate.value = false
        }
    }

    /**
     * [signingCerts]: SHA-256 hex of each certificate this APK is signed with. A failed fetch leaves
     * the app usable — it plays offline, and an unknown answer must not lock out our own users.
     */
    fun checkOfficialBuild(
        packageName: String,
        signingCerts: List<String>,
    ) {
        if (packageName !in Config.OFFICIAL_PACKAGE_NAMES) {
            _isOfficialBuild.value = false
            return
        }
        viewModelScope.launch {
            updateRepository.getFdroidSigningKeys().collect { response ->
                val keys = response.data
                // No certificate read at all is an unknown answer, and unknown never blocks.
                if (response is Resource.Success && keys != null && signingCerts.isNotEmpty() && keys.none { it in signingCerts }) {
                    _isOfficialBuild.value = false
                }
            }
        }
    }

    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        mediaPlayerHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        mediaPlayerHandler.loadPlaylistOrAlbum(index)
    }

    private fun updateLyrics(
        videoId: String,
        duration: Int, // 0 if translated lyrics
        inputLyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.SIMPMUSIC,
    ) {
        if (inputLyrics == null) {
            // "这首歌没有词"也是一次落地:把 lyricsVideoId 落成当前曲,拉词触发就此收口
            // (retained 旧词同时被清掉,不会把上一首的词永远挂在屏上)。
            _nowPlayingScreenData.update {
                it.copy(
                    lyricsData = null,
                    lyricsVideoId = videoId,
                )
            }
            return
        }

        val lyrics =
            inputLyrics.copy(
                lines =
                    inputLyrics.lines?.map { line ->
                        line.copy(
                            words = decodeHtmlEntities(line.words),
                        )
                    },
            )

        if (isTranslatedLyrics && lyricsProvider != LyricsProvider.AI) {
            // Skip sync validation for AI translations — timestamps are copied programmatically
            val originalLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
            val originalLines = originalLyrics?.lines
            val lyricsLines = lyrics.lines
            if (originalLyrics != null && originalLines != null && lyricsLines != null) {
                var timeSyncErrorCount = 0
                val totalLines = originalLines.size

                if (originalLines.size == lyricsLines.size) {
                    // Line counts match: compare by index (1:1 mapping)
                    originalLines.forEachIndexed { index, originalLine ->
                        val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                        val translatedLine = lyricsLines[index]
                        val translatedTime = translatedLine.startTimeMs.toLongOrNull() ?: 0L
                        val timeDiff = abs(originalTime - translatedTime)

                        if (timeDiff > 1000L) {
                            timeSyncErrorCount++
                        }
                    }
                } else {
                    // Line count mismatch: use timestamp-based matching with used-line tracking
                    val usedIndices = mutableSetOf<Int>()
                    originalLines.forEach { originalLine ->
                        val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                        var bestIndex = -1
                        var bestDiff = Long.MAX_VALUE
                        lyricsLines.forEachIndexed { index, line ->
                            if (index !in usedIndices) {
                                val diff = abs((line.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                                if (diff < bestDiff) {
                                    bestDiff = diff
                                    bestIndex = index
                                }
                            }
                        }
                        if (bestIndex >= 0) {
                            usedIndices.add(bestIndex)
                            if (bestDiff > 1000L) {
                                timeSyncErrorCount++
                            }
                        } else {
                            timeSyncErrorCount++
                        }
                    }
                }

                // Use percentage-based threshold: reject if >25% of lines are out of sync
                val syncErrorRatio = if (totalLines > 0) timeSyncErrorCount.toFloat() / totalLines else 0f
                if (syncErrorRatio > 0.25f || (totalLines > 0 && timeSyncErrorCount > totalLines / 2)) {
                    Logger.w(
                        tag,
                        "Translated lyrics out of sync: $timeSyncErrorCount/$totalLines lines with time diff > 1s (${(syncErrorRatio * 100).toInt()}%)",
                    )

                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        lyricsCanvasRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        log("Removed out-of-sync translated lyrics for $videoId")
                        val simpMusicLyricsId = lyrics.simpMusicLyrics?.id
                        if (lyricsProvider == LyricsProvider.SIMPMUSIC && !simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                lyricsCanvasRepository
                                    .voteSimpMusicTranslatedLyrics(
                                        translatedLyricsId = simpMusicLyricsId,
                                        false,
                                    ).collectLatest {
                                        when (it) {
                                            is Resource.Error -> {
                                                Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${it.message}")
                                            }

                                            is Resource.Success -> {
                                                Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                        nowPlayingScreenData.value.lyricsData?.lyrics?.let {
                            getAITranslationLyrics(
                                videoId,
                                it,
                            )
                        }
                    }
                    return
                }
            }
        }

        val shouldSendLyricsToSimpMusic =
            runBlocking {
                dataStoreManager.helpBuildLyricsDatabase.first() == TRUE
            } &&
                lyricsProvider != LyricsProvider.SIMPMUSIC
        if (_nowPlayingState.value?.songEntity?.videoId == videoId) {
            val track = _nowPlayingState.value?.track
            when (isTranslatedLyrics) {
                true -> {
                    if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                        _translatedVoteState.value =
                            VoteData(
                                id = lyrics.simpMusicLyrics?.id ?: "",
                                vote = lyrics.simpMusicLyrics?.vote ?: 0,
                                state = VoteState.Idle,
                            )
                    }
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = lyrics to lyricsProvider,
                                ),
                        )
                    }
                    if (shouldSendLyricsToSimpMusic && track != null) {
                        viewModelScope.launch {
                            lyricsCanvasRepository
                                .insertSimpMusicTranslatedLyrics(
                                    dataStoreManager,
                                    track,
                                    lyrics,
                                    dataStoreManager.translationLanguage.first(),
                                ).collect {
                                    when (it) {
                                        is Resource.Error -> {
                                            log("Insert SimpMusic Translated Lyrics Error ${it.message}", LogLevel.WARN)
                                        }

                                        is Resource.Success -> {
                                            log("Insert SimpMusic Translated Lyrics Success")
                                        }
                                    }
                                }
                        }
                    }
                }

                false -> {
                    if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                        _lyricsVoteState.value =
                            VoteData(
                                id = lyrics.simpMusicLyrics?.id ?: "",
                                vote = lyrics.simpMusicLyrics?.vote ?: 0,
                                state = VoteState.Idle,
                            )
                    }
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                NowPlayingScreenData.LyricsData(
                                    lyrics = lyrics,
                                    lyricsProvider = lyricsProvider,
                                ),
                            lyricsVideoId = videoId,
                        )
                    }
                    // Save lyrics to database
                    viewModelScope.launch {
                        lyricsCanvasRepository.insertLyrics(
                            LyricsEntity(
                                videoId = videoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }
                    if (shouldSendLyricsToSimpMusic && track != null) {
                        viewModelScope.launch {
                            lyricsCanvasRepository
                                .insertSimpMusicLyrics(
                                    dataStoreManager,
                                    track,
                                    duration,
                                    lyrics,
                                ).collect {
                                    when (it) {
                                        is Resource.Error -> {
                                            Logger.w(tag, "Insert SimpMusic Lyrics Error ${it.message}")
                                        }

                                        is Resource.Success -> {
                                            Logger.d(tag, "Insert SimpMusic Lyrics Success")
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun getLyricsFromFormat(
        isVideo: Boolean,
        song: SongEntity,
        duration: Int,
    ) {
        // A live broadcast has nothing to sync lyrics to — the "duration" mpv reports for it is only
        // its seek window — so no provider is asked. Every caller comes through here.
        if (LiveStreamRegistry.isLive(song.videoId)) return
        viewModelScope.launch {
            val videoId = song.videoId
            log("Get Lyrics From Format for $videoId", LogLevel.WARN)
            val artistName = song.artistName
            val artist =
                if (artistName?.firstOrNull() != null &&
                    artistName
                        .firstOrNull()
                        ?.contains("Various Artists") == false
                ) {
                    artistName.firstOrNull()
                } else {
                    mediaPlayerHandler.nowPlaying
                        .first()
                        ?.metadata
                        ?.artist
                        ?: ""
                }
            resetLyricsVoteState()
            // 网易歌(纯数字 id):走 NETEASE 官方专线(官方原文/官方翻译),现有供应商退居兜底
            if (videoId.toLongOrNull() != null) {
                getNeteaseLyrics(videoId, song, (artist ?: ""), duration)
                return@launch
            }
            if (videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                val mvId = videoId.removePrefix(NETEASE_MV_MEDIA_ID_PREFIX).toLongOrNull()
                val detail = mvId?.let { id ->
                    nowPlayingScreenData.value.neteaseMvData?.takeIf { it.id == id }
                        ?: neteaseRepository.getMvDetail(id).getOrNull()
                }
                val linkedSongId = detail?.let { neteaseRepository.getMvLinkedSongId(it) }
                if (linkedSongId != null) {
                    getNeteaseLyrics(videoId, song, (artist ?: ""), duration, linkedSongId)
                    return@launch
                }
            }
            val lyricsProvider = dataStoreManager.lyricsProvider.first()
            when (lyricsProvider) {
                DataStoreManager.SIMPMUSIC -> {
                    getSimpMusicLyrics(
                        videoId,
                        song,
                        (artist ?: ""),
                        duration,
                    )
                }

                DataStoreManager.LRCLIB -> {
                    getLrclibLyrics(
                        song,
                        (artist ?: ""),
                        duration,
                    )
                }

                DataStoreManager.YOUTUBE -> {
                    getYouTubeCaption(
                        videoId,
                        song,
                        (artist ?: ""),
                        duration,
                    )
                }

                DataStoreManager.BETTER_LYRICS -> {
                    getBetterLyrics(
                        song,
                        (artist ?: "").toString(),
                        duration,
                    )
                }

                // NETEASE 只对网易歌有意义(上方数字 ID 分支);设置页入口隐藏前可能存下该值,
                // YT 歌遇到时回落默认供应商,否则 when 落空导致一首词都不取
                else -> {
                    getSimpMusicLyrics(
                        videoId,
                        song,
                        (artist ?: ""),
                        duration,
                    )
                }
            }
        }
    }

    /**
     * 网易官方歌词专线:主选恒为 NETEASE(与歌曲同源,逐字 yrc+官方翻译,第三方库不可能更准),
     * 设置项(网易模式下已隐藏不可用项)不影响主选,仅决定兜底顺位;NETEASE 失败 →
     * LRCLIB(按名搜) → 本地已保存。官方翻译直接喂 translatedLyrics,缺失才走 AI。
     */
    private fun getNeteaseLyrics(
        videoId: String,
        song: SongEntity,
        artist: String,
        duration: Int,
        sourceSongId: String = videoId,
    ) {
        viewModelScope.launch {
            neteaseRepository.getNeteaseLyricsData(sourceSongId)
                .fold(
                    onSuccess = { (lyrics, officialTranslation, officialRomanization) ->
                        updateLyrics(
                            videoId,
                            duration,
                            lyrics,
                            false,
                            LyricsProvider.NETEASE,
                        )
                        insertLyrics(lyrics.toLyricsEntity(videoId))
                        if (officialRomanization != null) {
                            // 官方罗马音不落库(本地歌词表无该槽),切回同歌重拉时重新喂。
                            // 竞态守卫:请求在途时切歌,这里照写会把旧歌罗马音挂到新歌整首——
                            // 本写不经过 updateLyrics、不触碰 lyricsVideoId,吃不到
                            // "lyricsVideoId != song.videoId 触发重拉"的自愈,必须显式校验归属。
                            _nowPlayingScreenData.update {
                                if (it.lyricsVideoId == videoId) {
                                    it.copy(
                                        lyricsData = it.lyricsData?.copy(
                                            romanizedLyrics = officialRomanization to LyricsProvider.NETEASE,
                                        ),
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                        if (officialTranslation != null) {
                            // 官方翻译与原文同源,时间轴天然对齐
                            updateLyrics(
                                videoId,
                                0,
                                officialTranslation,
                                true,
                                LyricsProvider.NETEASE,
                            )
                        } else {
                            getAITranslationLyrics(videoId, lyrics)
                        }
                    },
                    onFailure = {
                        log("Netease lyrics miss: ${it.message}")
                        getLrclibLyrics(song, artist, duration)
                    },
                )
        }
    }

    private suspend fun getSimpMusicLyrics(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
    ) {
        lyricsCanvasRepository.getSimpMusicLyrics(videoId).collectLatest {
            Logger.d(tag, "Get SimpMusic Lyrics for $videoId: ${it::class.simpleName}")
            val data = it.data
            if (it is Resource.Success && data != null) {
                Logger.d(tag, "Get SimpMusic Lyrics Success")
                updateLyrics(
                    videoId,
                    duration,
                    data,
                    false,
                    LyricsProvider.SIMPMUSIC,
                )
                insertLyrics(
                    data.toLyricsEntity(videoId),
                )
                getSimpMusicTranslatedLyrics(
                    videoId,
                    data,
                )
            } else if (dataStoreManager.spotifyLyrics.first() == TRUE) {
                getSpotifyLyrics(
                    song.toTrack().copy(durationSeconds = duration),
                    "${song.title} $artist",
                    duration,
                )
            } else {
                getLrclibLyrics(
                    song,
                    (artist ?: ""),
                    duration,
                )
            }
        }
    }

    private suspend fun getYouTubeCaption(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
    ) {
        lyricsCanvasRepository
            .getYouTubeCaption(dataStoreManager.youtubeSubtitleLanguage.first(), videoId)
            .cancellable()
            .collect { response ->
                val data = response.data
                when (response) {
                    is Resource.Success if (data != null) -> {
                        val lyrics = data.first
                        val translatedLyrics = data.second
                        insertLyrics(lyrics.toLyricsEntity(videoId))
                        updateLyrics(
                            videoId,
                            duration,
                            lyrics,
                            false,
                            LyricsProvider.YOUTUBE,
                        )
                        if (translatedLyrics != null) {
                            updateLyrics(
                                videoId,
                                duration,
                                translatedLyrics,
                                true,
                                LyricsProvider.YOUTUBE,
                            )
                        } else {
                            getAITranslationLyrics(
                                videoId,
                                lyrics,
                            )
                        }
                    }

                    else -> {
                        getSimpMusicLyrics(
                            videoId,
                            song,
                            (artist ?: ""),
                            duration,
                        )
                    }
                }
            }
    }

    private fun getLrclibLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            lyricsCanvasRepository
                .getLrclibLyricsData(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Logger.d(tag, "Get Lyrics Data Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                res.data,
                                false,
                                LyricsProvider.LRCLIB,
                            )
                            insertLyrics(
                                res.data?.toLyricsEntity(
                                    song.videoId,
                                ) ?: return@collectLatest,
                            )
                            getAITranslationLyrics(
                                song.videoId,
                                data,
                            )
                        }

                        else -> {
                            getSavedLyrics(
                                song.toTrack().copy(
                                    durationSeconds = duration,
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun getBetterLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            lyricsCanvasRepository
                .getBetterLyrics(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Logger.d(tag, "Get BetterLyrics Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                data,
                                false,
                                LyricsProvider.BETTER_LYRICS,
                            )
                            insertLyrics(
                                data.toLyricsEntity(
                                    song.videoId,
                                ),
                            )
                            getAITranslationLyrics(
                                song.videoId,
                                data,
                            )
                        }

                        else -> {
                            log("Get BetterLyrics Error: ${res.message}", LogLevel.WARN)
                            getSimpMusicLyrics(
                                song.videoId,
                                song,
                                artist,
                                duration,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun getSimpMusicTranslatedLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        val translationLanguage =
            dataStoreManager.translationLanguage.first()
        lyricsCanvasRepository.getSimpMusicTranslatedLyrics(videoId, translationLanguage).collectLatest { response ->
            val data = response.data
            when (response) {
                is Resource.Success if (data != null) -> {
                    // If SimpMusic translated lyrics are RICH_SYNCED (word-by-word),
                    // convert to LINE_SYNCED, downvote, and fallback to AI translation
                    if (data.syncType == "RICH_SYNCED") {
                        Logger.w(tag, "SimpMusic translated lyrics are RICH_SYNCED, downvoting and falling back to AI")
                        val simpMusicLyricsId = data.simpMusicLyrics?.id
                        if (!simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                lyricsCanvasRepository
                                    .voteSimpMusicTranslatedLyrics(simpMusicLyricsId, false)
                                    .collectLatest { voteResult ->
                                        when (voteResult) {
                                            is Resource.Error -> Logger.w(tag, "Downvote RICH_SYNCED translated lyrics error: ${voteResult.message}")
                                            is Resource.Success -> Logger.d(tag, "Downvote RICH_SYNCED translated lyrics success")
                                        }
                                    }
                            }
                        }
                        // Fallback to AI translation
                        getAITranslationLyrics(videoId, lyrics)
                    } else {
                        Logger.d(tag, "Get SimpMusic Translated Lyrics Success")
                        updateLyrics(
                            videoId,
                            0,
                            data,
                            true,
                            LyricsProvider.SIMPMUSIC,
                        )
                    }
                }

                else -> {
                    Logger.d(tag, "Get SimpMusic Translated Lyrics Error: ${response.message}")
                    getAITranslationLyrics(
                        videoId,
                        lyrics,
                    )
                }
            }
        }
    }

    /**
     * The notification line renders one text per timestamp, so ORIGINAL_AND_TRANSLATION mode
     * appends the translation to the original words instead of showing it on its own. Every
     * provider pairs translations with the original timestamps (AI copies them per index,
     * SimpMusic/YouTube pairs are time-validated on arrival), so match by exact start time
     * and fall back to the same-index line when a provider's timing drifted.
     */
    private fun mergeTranslationIntoLines(
        originalLines: List<Line>,
        translatedLines: List<Line>,
    ): List<Line> {
        if (originalLines.isEmpty() || translatedLines.isEmpty()) return originalLines
        val translationByTime = HashMap<Long, String>(translatedLines.size)
        translatedLines.forEach { line ->
            val ts = line.startTimeMs.toLongOrNull() ?: return@forEach
            val words = line.words.trim()
            if (words.isNotEmpty()) translationByTime[ts] = words
        }
        val sameIndexFallback = translatedLines.size == originalLines.size
        return originalLines.mapIndexed { index, line ->
            val translation =
                translationByTime[line.startTimeMs.toLongOrNull()]
                    ?: if (sameIndexFallback) translatedLines[index].words.trim() else null
            if (translation.isNullOrEmpty() || translation == line.words) {
                line
            } else {
                Line(
                    startTimeMs = line.startTimeMs,
                    endTimeMs = line.endTimeMs,
                    words = "${line.words.trim()} · $translation",
                    syllables = line.syllables,
                )
            }
        }
    }

    private suspend fun getAITranslationLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        Logger.d(tag, "Get AI Translation Lyrics for $videoId")
        if (dataStoreManager.useAITranslation.first() == TRUE &&
            dataStoreManager.aiApiKey.first().isNotEmpty() &&
            dataStoreManager.enableTranslateLyric.first() == FALSE
        ) {
            val savedTranslatedLyrics =
                lyricsCanvasRepository
                    .getSavedTranslatedLyrics(
                        videoId,
                        dataStoreManager.translationLanguage.first(),
                    ).firstOrNull()
            if (savedTranslatedLyrics != null) {
                Logger.d(tag, "Get Saved Translated Lyrics")
                updateLyrics(
                    videoId,
                    0,
                    savedTranslatedLyrics.toLyrics(),
                    true,
                    LyricsProvider.AI,
                )
            } else {
                // Convert RICH_SYNCED to LINE_SYNCED before sending to AI
                // AI should only work with line-level or plain lyrics
                val lyricsForAi =
                    if (lyrics.syncType == "RICH_SYNCED") {
                        lyrics.toSyncedLyrics()
                    } else {
                        lyrics
                    }
                lyricsCanvasRepository
                    .getAITranslationLyrics(
                        lyricsForAi,
                        dataStoreManager.translationLanguage.first(),
                    ).cancellable()
                    .collectLatest {
                        val data = it.data
                        when (it) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Get AI Translate Lyrics Success")
                                lyricsCanvasRepository.insertTranslatedLyrics(
                                    TranslatedLyricsEntity(
                                        videoId = videoId,
                                        language = dataStoreManager.translationLanguage.first(),
                                        error = false,
                                        lines = data.lines,
                                        syncType = data.syncType,
                                    ),
                                )
                                updateLyrics(
                                    videoId,
                                    0,
                                    data,
                                    true,
                                    LyricsProvider.AI,
                                )
                            }

                            else -> {
                                Logger.w(tag, "Get AI Translate Lyrics Error: ${it.message}")
                            }
                        }
                    }
            }
        }
    }

    private fun getSpotifyLyrics(
        track: Track,
        query: String,
        duration: Int? = null,
    ) {
        viewModelScope.launch {
            Logger.d("Check SpotifyLyrics", "SpotifyLyrics $query")
            lyricsCanvasRepository.getSpotifyLyrics(dataStoreManager, query, duration).cancellable().collect { response ->
                Logger.d("Check SpotifyLyrics", response.toString())
                val data = response.data
                when (response) {
                    is Resource.Success -> {
                        if (data != null) {
                            insertLyrics(
                                data.toLyricsEntity(
                                    track.videoId,
                                ),
                            )
                            updateLyrics(
                                track.videoId,
                                duration ?: 0,
                                data,
                                false,
                                LyricsProvider.SPOTIFY,
                            )
                            getAITranslationLyrics(
                                track.videoId,
                                data,
                            )
                        }
                    }

                    else -> {
                        getLrclibLyrics(
                            track.toSongEntity(),
                            track.artists.toListName().firstOrNull() ?: "",
                            duration ?: 0,
                        )
                    }
                }
            }
        }
    }

    private fun setLyricsProvider() {
        viewModelScope.launch {
            val songEntity = nowPlayingState.value?.songEntity ?: return@launch
            val isVideo = nowPlayingState.value?.mediaItem?.isVideo() ?: false
            getLyricsFromFormat(isVideo, songEntity, timeline.value.total.toInt() / 1000)
        }
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            // A radio, a mix or an Endless queue has no end to append to, so a single song (the swipe
            // on a song row) goes right after the current one instead.
            val endlessQueue = dataStoreManager.endlessQueue.first() == TRUE
            if (listTrack.size == 1 && mediaPlayerHandler.queueData.value.addToQueueBlock(endlessQueue) != null) {
                mediaPlayerHandler.playNext(listTrack.first())
                makeToast(getString(Res.string.play_next))
            } else {
                // isAddToQueue=true:已在队列的歌不加副本(2026-10-08),全撞重时如实相告
                val added = mediaPlayerHandler.loadMoreCatalog(listTrack, isAddToQueue = true)
                makeToast(
                    getString(if (added) Res.string.added_to_queue else Res.string.already_in_queue),
                )
            }
        }
    }


    fun getEnableLiquidGlass() = dataStoreManager.enableLiquidGlass

    fun getLocalTrackingEnabled() = dataStoreManager.localTrackingEnabled

    // Drives the Mix for you tab: YouTube hands an anonymous session no mixes at all.
    fun getYouTubeLoggedIn() = dataStoreManager.loggedIn

    fun getThemeMode() = dataStoreManager.themeMode

    fun getThemeColorSource() = dataStoreManager.themeColorSource

    fun getCustomThemeColor() = dataStoreManager.customThemeColor

    fun getNowPlayingStyle() = dataStoreManager.nowPlayingStyle

    fun getLyricsStyle() = dataStoreManager.lyricsStyle

    fun getLyricsOffsetMs() = dataStoreManager.lyricsOffsetMs

    fun setLyricsOffsetMs(offsetMs: Int) {
        viewModelScope.launch {
            dataStoreManager.setLyricsOffsetMs(offsetMs)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
        }
    }

    fun setThemeColorSource(source: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeColorSource(source)
        }
    }

    fun setCustomThemeColor(argbHex: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomThemeColor(argbHex)
        }
    }

    fun setNowPlayingStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setNowPlayingStyle(style)
        }
    }

    fun setLyricsStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsStyle(style)
        }
    }

    fun getRomanizationLanguages() = dataStoreManager.romanizationLanguages

    fun setRomanizationLanguages(languages: Set<RomanizationLanguage>) {
        viewModelScope.launch {
            // Sorted by name so the stored string is stable: an unsorted Set writes a different
            // value for the same selection depending on iteration order, which makes the DataStore
            // flow emit on a change that did not happen.
            dataStoreManager.setRomanizationLanguages(languages.map { it.name }.sorted().joinToString(","))
        }
    }

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    fun shouldCheckForUpdate(): Boolean = runBlocking { dataStoreManager.autoCheckForUpdates.first() == TRUE }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress
    /** MV 直写文件不经过 Media3 DownloadManager；下载管理页从这里读取在途 ID 和进度。 */
    private val _mvDownloadProgress = MutableStateFlow<Map<String, Float?>>(emptyMap())
    val mvDownloadProgress: StateFlow<Map<String, Float?>> get() = _mvDownloadProgress

    /** 在途 MV 直写协程表(id→Job):下载管理页删除/取消要能真正停掉写文件的协程,
     *  否则删后协程照跑、完成时把路径+完成态写回 Room,文件"复活"(2026-10 CR P1) */
    private val mvDownloadJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    /** 取消在途 MV 直写下载:协程收到取消后自清已提交文件与 Room 在途态,不再回写完成 */
    fun cancelMvDownload(videoId: String) {
        mvDownloadJobs.remove(videoId)?.cancel()
    }

    fun downloadFile(bitmap: ImageBitmap) {
        viewModelScope.launch {
            // track 在队列重建窗口(冷启恢复队列/重进后点当前曲)会查空且永不回填,
            // 之前整段 ?.let 静默跳过就是"点了没反应"的病根;songEntity 恒有值,回落它
            val track =
                nowPlayingState.value?.track
                    ?: nowPlayingState.value?.songEntity?.toTrack()
                    ?: return@launch
            val fileName = sanitizeDownloadFileName(
                "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}",
                track.videoId,
            )
            val path =
                "${getDownloadFolderPath()}/$fileName"
            withContext(Dispatchers.IO) {
                val bytesArray = bitmap.toByteArray()
                try {
                    val jpgPart = File("$path.jpg.part")
                    FileOutputStream(jpgPart).use { it.write(bytesArray) }
                    commitDownloadFile(jpgPart, File("$path.jpg"))
                    Logger.d(tag, "Thumbnail saved to $path.jpg")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.e(tag, "thumbnail write failed: ${e.message}")
                    _downloadFileProgress.value = DownloadProgress.failed(e.message ?: "thumbnail write failed")
                    return@withContext
                }
                // 网易歌:CDN 直链音频(mpeg/flac),youTube.download 的 itag 合并管线不适用,
                // 直接取流下载写文件;进度喂同一个 DownloadProgress 弹窗
                if (track.videoId.toLongOrNull() != null) {
                    downloadNeteaseFile(track.videoId, path)
                } else {
                    songRepository
                        .downloadToFile(
                            track = track,
                            videoId = track.videoId,
                            path = path,
                            isVideo = nowPlayingScreenData.value.isVideo,
                        ).collectLatest {
                            _downloadFileProgress.value = it
                        }
                }
            }
        }
    }

    /** 下载文件名清理:路径分隔符与非法字符(含 /,否则 AC/DC 这类名字被解释成子目录致写入必失败)、
     *  控制字符全部滤掉,尾部点号修剪;清理后为空兜底 videoId,截 100 字符防超长文件名 */
    private fun sanitizeDownloadFileName(
        raw: String,
        fallback: String,
    ): String =
        raw
            .replace(Regex("[/|\\\\?*<\":>\\x00-\\x1f]"), "")
            .replace(" ", "_")
            .trimEnd('.')
            .take(100)
            .ifBlank { "download_$fallback" }

    /** staged write 提交:同目录 .part 已写满并 close,这里 rename 到正式路径——中途断网/
     *  超时/磁盘满只留下被清理的 .part,旧文件原样保留;同目录 rename 对已存在目标是原子
     *  覆盖,顺带绕开 FUSE 上"新建同名文件"的 EEXIST(原先删旧文件再直写的病根) */
    private fun commitDownloadFile(
        part: File,
        target: File,
    ) {
        try {
            if (!part.renameTo(target)) {
                // 个别 FUSE 实现不接受覆盖 rename:此时新文件已完整,删旧目标后重试提交
                target.delete()
                check(part.renameTo(target)) { "commit failed: ${target.name}" }
            }
        } finally {
            part.delete()
        }
    }

    /** 网易歌导出到设备目录:取流(带格式定扩展名)→ 分块下载到 .part → 校验后 rename 提交 + 进度 */
    private suspend fun downloadNeteaseFile(
        videoId: String,
        path: String,
    ) {
        _downloadFileProgress.value = DownloadProgress()
        val stream =
            neteaseRepository.getStreamInfo(videoId, isDownload = true).getOrNull()
        if (stream == null) {
            _downloadFileProgress.value = DownloadProgress.failed("netease stream unavailable")
            return
        }
        val ext = if (stream.mimeType?.contains("flac", ignoreCase = true) == true) "flac" else "mp3"
        downloadUrlToFile(stream.url, path, ext)
    }

    /** 网易 MV 下载(菜单"下载 MV"):mvDetail→brs 按设置选档→mp4 直存(单文件自带音轨,
     *  免 YT 式双流合并);进度走同一个 DownloadProgress 弹窗,失败落错误弹窗不裸抛 */
    fun downloadMvFile(videoId: String? = null) {
        // track 在队列 rebuild 窗口恒 null,回退 songEntity(九轮铁律);两者取名一致
        val playingTrack =
            nowPlayingState.value?.track
                ?: nowPlayingState.value?.songEntity?.toTrack()
        val requestedId = videoId ?: playingTrack?.videoId ?: return
        val mvid = requestedId.removePrefix(NETEASE_MV_MEDIA_ID_PREFIX).toLongOrNull() ?: return
        if (!requestedId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX) || _mvDownloadProgress.value.containsKey(requestedId)) return
        // 同步占位，连续点两次时第二次不会在首个协程启动前再建一个写同名文件的任务。
        _mvDownloadProgress.update { it + (requestedId to null) }
        viewModelScope.launch {
            var rowId: String? = null
            var committedPath: String? = null
            try {
                _downloadFileProgress.value = DownloadProgress()
                val detail = neteaseRepository.getMvDetail(mvid).getOrNull()
                val brs = detail?.brs
                if (brs.isNullOrEmpty()) {
                    // 进度弹窗只在 Info 面板可见;菜单直点路径的终态反馈只能靠 toast,
                    // 这里早退不补的话用户只见过场的"正在下载",失败原因永远不可见
                    _downloadFileProgress.value = DownloadProgress.failed("mv stream unavailable")
                    makeToast(_downloadFileProgress.value.errorMessage ?: getString(Res.string.error))
                    return@launch
                }
                val wanted = dataStoreManager.videoQuality.first().removeSuffix("p").toIntOrNull() ?: 720
                val candidates = brs.keys.filter { it in 240..1080 }.sorted()
                val pick = candidates.lastOrNull { it <= wanted } ?: candidates.firstOrNull()
                val url = pick?.let { brs[it] }
                if (url == null) {
                    _downloadFileProgress.value = DownloadProgress.failed("mv stream unavailable")
                    makeToast(_downloadFileProgress.value.errorMessage ?: getString(Res.string.error))
                    return@launch
                }
                val track = playingTrack?.takeIf { it.videoId == requestedId }
                    ?: neteaseRepository.mvTrackFromDetail(detail)
                rowId = track.videoId
                val fileName = sanitizeDownloadFileName(detail.name, track.videoId)
                val path = "${getDownloadFolderPath()}/$fileName"
                committedPath = "$path.mp4"
                // Room 活动行与进度 map 合并，管理页在下载中展示；成功后转入已完成。
                persistMvSongRow(track, detail, com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADING, null)
                val saved =
                    withContext(Dispatchers.IO) {
                        downloadUrlToFile(url, path, "mp4") { progress ->
                            _mvDownloadProgress.update { it + (requestedId to progress) }
                        }
                    }
                // 菜单路径下 Info 面板未开,进度弹窗不可见——完成/失败补 toast
                if (saved) {
                    // 接入下载管理体系:先写文件路径，再转完成态，避免短暂"文件缺失"。
                    persistMvSongRow(track, detail, com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADED, committedPath)
                    makeToast(getString(Res.string.mv_saved_to_download))
                } else {
                    settleMvRowAfterAbandon(rowId, committedPath)
                    val err = _downloadFileProgress.value.errorMessage
                    makeToast(err ?: getString(Res.string.error))
                }
            } catch (e: CancellationException) {
                // 下载管理页删除/取消:不能让取消后的回写把文件"复活"(先删协程再删条目也赶不上)
                rowId?.let { settleMvRowAfterAbandon(it, committedPath) }
                throw e
            } finally {
                mvDownloadJobs.remove(requestedId)
                _mvDownloadProgress.update { it - requestedId }
            }
        }.also { mvDownloadJobs[requestedId] = it }
    }

    /** MV 下载中止(取消/失败)后的 Room 对账:重下被中止时旧成品仍在路径列上,恢复完成态
     *  (旧文件仍是有效下载);首下被中止则删可能已 rename 提交的成品(取消落在提交后、
     *  回写前的窗口)+清路径列+归零,行从管理页消失 */
    private suspend fun settleMvRowAfterAbandon(
        rowId: String,
        committedPath: String?,
    ) {
        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
            val stillValid =
                songRepository.getSongById(rowId).firstOrNull()?.downloadedVideoFilePath
                    ?.let { java.io.File(it).exists() } == true
            if (stillValid) {
                songRepository.updateDownloadState(
                    rowId,
                    com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADED,
                )
            } else {
                committedPath?.let { java.io.File(it).delete() }
                songRepository.updateDownloadedVideoFilePath(rowId, null)
                songRepository.updateDownloadState(
                    rowId,
                    com.maxrave.domain.data.entities.DownloadState.STATE_NOT_DOWNLOADED,
                )
            }
        }
    }

    /** MV song 行 upsert(下载管理体系):行在(野行/在途行)时 IGNORE 后仅 UPDATE 状态与路径;
     *  videoFilePath 非空才写(在途态传 null 不动列) */
    private suspend fun persistMvSongRow(
        track: com.maxrave.domain.data.model.browse.album.Track,
        detail: com.maxrave.netease.model.NeteaseMvDetail,
        state: Int,
        videoFilePath: String?,
    ) {
        runCatching {
            songRepository
                .insertSong(
                    com.maxrave.domain.data.entities.SongEntity(
                        videoId = track.videoId,
                        source = com.maxrave.domain.source.MusicSource.NETEASE.name,
                        albumId = null,
                        albumName = null,
                        artistId = detail.artists.map { it.first.toString() },
                        artistName = detail.artists.map { it.second },
                        duration = detail.durationMs.toCompactDuration(),
                        durationSeconds = (detail.durationMs / 1000).toInt(),
                        isAvailable = true,
                        isExplicit = false,
                        likeStatus = "INDIFFERENT",
                        thumbnails = detail.coverUrl,
                        title = detail.name,
                        videoType = "MUSIC_VIDEO_TYPE_OMV",
                        category = null,
                        resultType = "mv",
                        downloadState = state,
                        inLibrary = com.maxrave.domain.extension.now(),
                    ),
                ).collect { }
            if (videoFilePath != null && java.io.File(videoFilePath).exists()) {
                songRepository.updateDownloadedVideoFilePath(track.videoId, videoFilePath)
            }
            songRepository.updateDownloadState(track.videoId, state)
        }
    }

    /** MV 行时长(mm:ss);毫秒直转,毫秒 0(端点不给)回退 track 值 */
    private fun Long.toCompactDuration(): String {
        val totalSec = this / 1000
        if (totalSec <= 0) return ""
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    /** 共用流式下载体:URL → .part 分块写(校验 Content-Length)→ rename 提交 + 进度;
     *  返回成败——调用方(MV 落库/音频 toast)以文件提交为准,不回读 progress flow
     *  (IO 协程回 Main 的间隙进度可能被其它写者复位,实测快照不可靠) */
    private suspend fun downloadUrlToFile(
        url: String,
        path: String,
        ext: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Boolean {
        val part = File("$path.$ext.part")
        try {
            // prepareGet+execute 才是真流式:ktor3 的 client.get() 会把整个响应体先
            // save 进内存(SavedCall),无损 flac 动辄几十 MB,曾在 ~192MB 堆上直接 OOM
            // (SavedCallKt.save→readByteArray,主线程);Content-Length 头兜底算百分比。
            // 超时:裸 CIO 默认 15s 整请求上限,百 MB 级无损在慢链路必超时——放宽到
            // 10min 总上限+60s socket 空闲(进度在走就不会触发)
            HttpClient(CIO) {
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 10 * 60_000L
                    socketTimeoutMillis = 60_000L
                }
            }.use { client ->
                client.prepareGet(url).execute { response ->
                    // ktor 默认不按状态码判失败;audioUrl 有 10 分钟有效期,过期/区域拒绝
                    // 返回的 4xx/5xx 正文是 HTML/JSON,不拦会存成 .mp3 还报下载完成
                    if (!response.status.isSuccess()) {
                        throw IOException("HTTP ${response.status.value}")
                    }
                    val total = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
                    val channel = response.bodyAsChannel()
                    FileOutputStream(part).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = 0L
                        val startedAt = System.currentTimeMillis()
                        while (true) {
                            val n = channel.readAvailable(buffer, 0, buffer.size)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            read += n
                            if (total > 0) {
                                onProgress?.invoke((read.toFloat() / total).coerceIn(0f, 1f))
                                val elapsedS = (System.currentTimeMillis() - startedAt).coerceAtLeast(1) / 1000f
                                _downloadFileProgress.value =
                                    DownloadProgress(
                                        audioDownloadProgress = (read.toFloat() / total).coerceIn(0f, 1f),
                                        downloadSpeed = (read / 1024f / elapsedS).toInt(),
                                    )
                            }
                        }
                        // 声明了长度却没读满=半截文件(连接中断但 EOF 提前到达),按失败处理
                        if (total > 0 && read != total) {
                            throw IOException("incomplete download: $read/$total")
                        }
                    }
                }
            }
            commitDownloadFile(part, File("$path.$ext"))
            _downloadFileProgress.value = DownloadProgress.AUDIO_DONE
            Logger.d(tag, "Netease file saved to $path.$ext")
            return true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag, "netease download failed: ${e.message}")
            _downloadFileProgress.value = DownloadProgress.failed(e.message ?: "download failed")
            return false
        } finally {
            // 成功路径 part 已被 rename 走,这里只兜异常/取消路径的残留清理
            part.delete()
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun onDoneReview(isDismissOnly: Boolean = true) {
        viewModelScope.launch {
            if (!isDismissOnly) {
                dataStoreManager.doneOpenAppTime()
            } else {
                dataStoreManager.openApp()
            }
        }
    }

    fun onDoneRequestingShareLyrics(contributor: Pair<String, String>? = null) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(true)
            dataStoreManager.setContributorLyricsDatabase(
                contributor,
            )
        }
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    // Vote state for translated lyrics
    private val _translatedVoteState = MutableStateFlow<VoteData?>(null)
    val translatedVoteState: StateFlow<VoteData?> = _translatedVoteState.asStateFlow()

    // Vote state for original lyrics
    private val _lyricsVoteState = MutableStateFlow<VoteData?>(null)
    val lyricsVoteState: StateFlow<VoteData?> = _lyricsVoteState.asStateFlow()

    /**
     * Vote for SimpMusic original lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteLyrics(upvote: Boolean) {
        val lyricsData = _nowPlayingScreenData.value.lyricsData
        val lyricsProvider = lyricsData?.lyricsProvider
        val simpMusicLyricsId = lyricsData?.lyrics?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic lyrics or missing ID")
            return
        }

        viewModelScope.launch {
            _lyricsVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
            lyricsCanvasRepository
                .voteSimpMusicLyrics(
                    lyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Lyrics Error ${result.message}")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Lyrics Success")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Success(upvote),
                                    vote = it.vote + if (upvote) 1 else -1,
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    private fun resetLyricsVoteState() {
        _lyricsVoteState.value = null
        _translatedVoteState.value = null
    }

    /**
     * Vote for SimpMusic translated lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteTranslatedLyrics(upvote: Boolean) {
        val translatedLyrics = _nowPlayingScreenData.value.lyricsData?.translatedLyrics
        val lyricsProvider = translatedLyrics?.second
        val simpMusicLyricsId = translatedLyrics?.first?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic translated lyrics or missing ID")
            return
        }

        viewModelScope.launch {
            _translatedVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
            lyricsCanvasRepository
                .voteSimpMusicTranslatedLyrics(
                    translatedLyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${result.message}")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Success(upvote),
                                    vote = it.vote + if (upvote) 1 else -1,
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    fun isUserLoggedIn(): Boolean = runBlocking { dataStoreManager.cookie.first().isNotEmpty() }

    // Flow-based variant of [isUserLoggedIn] so composables can collect login state once
    // instead of calling runBlocking inside composition (used by NowPlayingScreenContent).
    fun isUserLoggedInFlow(): Flow<Boolean> = dataStoreManager.cookie.map { it.isNotEmpty() }

}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    /**
     * Always advances to the previous track — bypasses the 3-second
     * "seek to start of current track" rule used by [Previous]. Used by the
     * NowPlaying artwork pager swipe.
     */
    data object SkipToPrevious : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

/** Independent source-account state. `liked == null` means signed out or not yet known.
 *  videoId=该快照所属曲目(=在播曲):ToggleLike 只认属于在播曲的快照,列表菜单对
 *  别曲的写不进这里,防止翻转方向被污染。 */
data class RemoteSongLikeState(
    val liked: Boolean? = null,
    val pending: Boolean = false,
    val failed: Boolean = false,
    val videoId: String? = null,
)

/** 见 SharedViewModel.likeWriteResults:一次云端写(红心/MV 收藏)的最终落定结果 */
data class LikeWriteResult(
    val videoId: String,
    val liked: Boolean,
    /** 服务端已确认成请求方向(红心=写成功;MV=subed 回读一致) */
    val success: Boolean,
)

/** 见 SharedViewModel.likeChangeEvent:收藏列表页原地增删行的数据载荷 */
data class LikeChangeEvent(
    val videoId: String,
    val liked: Boolean,
    val track: Track? = null,
)

enum class LyricsProvider {
    SIMPMUSIC,
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    BETTER_LYRICS,
    NETEASE,
    AI,
    OFFLINE,
}

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    /**
     * lyricsData 属于哪首歌。切歌时旧词会被保留(stale-while-revalidate,防歌词区闪空),
     * 这个标记就是"旧词是不是当前曲的":拉词触发与落地写入都看它,防止旧词被当成新词
     * 永远留在屏上。
     */
    val lyricsVideoId: String? = null,
    val songInfoData: SongInfoEntity? = null,
    /** 网易歌详情卡数据(艺人/专辑/热评),与 songInfoData 按源互斥 */
    val neteaseSongData: NeteaseSongInfoEntity? = null,
    /** 网易 MV 详情(计数/收藏态/评论线程),当前曲是 MV 时非 null,与上两者互斥 */
    val neteaseMvData: NeteaseMvDetail? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
        // A still of the clip at the clip's own size — Apple Music's animated artwork sends one.
        // The Apple Music player shows it under the clip while that loads, and reads the clip's
        // proportions off it, so the frame is the right shape before the first video frame arrives.
        val thumbUrl: String? = null,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        // 官方罗马音(网易 romalrc,行级与原文同源对齐);null=无官方数据,渲染端回退本地引擎
        val romanizedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

data class VoteData(
    val id: String,
    val vote: Int,
    val state: VoteState,
)

sealed class VoteState {
    data object Idle : VoteState()

    data object Loading : VoteState()

    data class Success(
        val upvote: Boolean,
    ) : VoteState()

    data class Error(
        val message: String,
    ) : VoteState()
}

/**
 * Whether a stored canvas url points at something a player should open rather than an image.
 *
 * The column holds whatever the active source wrote: a Spotify canvas is an `.mp4`, while AM
 * animated artwork is an HLS `.m3u8` master playlist. Testing only for `.mp4` — as this did before
 * AM existed — sends every AM artwork down the still-image branch, and because the branch that
 * reads this is the one that restores a *cached* url, the failure only appears from the second play
 * of a track onwards.
 */
private fun String.isCanvasVideoUrl(): Boolean = contains(".mp4") || contains(".m3u8")
