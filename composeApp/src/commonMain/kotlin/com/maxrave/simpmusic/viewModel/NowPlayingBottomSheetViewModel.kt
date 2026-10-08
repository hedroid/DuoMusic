package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.NETEASE_MV_MEDIA_ID_PREFIX
import com.maxrave.common.songRadioPlaylistId
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.BETTER_LYRICS
import com.maxrave.domain.manager.DataStoreManager.Values.LRCLIB
import com.maxrave.domain.manager.DataStoreManager.Values.SIMPMUSIC
import com.maxrave.domain.manager.DataStoreManager.Values.YOUTUBE
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SleepTimerState
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.collectLatestResource
import com.maxrave.domain.utils.collectResource
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.LogLevel
import com.maxrave.netease.NeteaseNotLoggedInException
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.maxrave.simpmusic.viewModel.base.demoteDownloadedContainers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_playlist
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.already_in_queue
import simpmusic.composeapp.generated.resources.added_to_youtube_playlist
import simpmusic.composeapp.generated.resources.added_to_netease_playlist
import simpmusic.composeapp.generated.resources.cloud_action_failed_netease
import simpmusic.composeapp.generated.resources.cloud_action_failed_youtube
import simpmusic.composeapp.generated.resources.netease_action_failed
import simpmusic.composeapp.generated.resources.netease_rate_limited
import com.maxrave.simpmusic.extension.neteaseWriteErrorString
import simpmusic.composeapp.generated.resources.delete_song_from_playlist
import simpmusic.composeapp.generated.resources.download_no_space
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.no_mv_available
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_download
import simpmusic.composeapp.generated.resources.removed_from_YouTube_playlist
import simpmusic.composeapp.generated.resources.share_url
import simpmusic.composeapp.generated.resources.sleep_timer_off_done

class NowPlayingBottomSheetViewModel(
    private val dataStoreManager: DataStoreManager,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject()
    private val albumRepository: AlbumRepository by inject()
    private val neteaseRepository: com.maxrave.data.repository.NeteaseRepositoryImpl by inject()
    private val sharedViewModel: SharedViewModel by inject()
    private val _uiState: MutableStateFlow<NowPlayingBottomSheetUIState> =
        MutableStateFlow(
            NowPlayingBottomSheetUIState(
                listLocalPlaylist = emptyList(),
                listYouTubePlaylist = null,
                listNeteasePlaylist = null,
                mainLyricsProvider = SIMPMUSIC,
                sleepTimer =
                    SleepTimerState(
                        false,
                        0,
                    ),
            ),
        )
    val uiState: StateFlow<NowPlayingBottomSheetUIState> get() = _uiState.asStateFlow()

    private var getSongAsFlow: Job? = null
    private var mvLookupJob: Job? = null

    // 云端歌单加载链(单飞):VM init 与弹窗打开的 resetPlaylists 曾各自起一套重试链
    // (各 3 次退避),首次打开弹窗最多 6 次全量 YT 拉取且旧回包可能覆盖新回包
    // (2026-09-30 二轮 CR)——同名 job 新起前先取消旧的,保证任一时刻每源至多一条链。
    private var ytPlaylistsJob: Job? = null
    private var neteasePlaylistsJob: Job? = null

    init {
        viewModelScope.launch {
            // 只观察当前 MV 的任务;其它歌曲/视频导出的共享进度不能把本行染成下载中。
            sharedViewModel.mvDownloadProgress.collectLatest { progress ->
                _uiState.update { state ->
                    state.copy(mvDownloading = progress.containsKey(state.songUIState.videoId))
                }
            }
        }
        viewModelScope.launch {
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest { sl ->
                        _uiState.update { it.copy(sleepTimer = sl) }
                    }
                }
            val listLocalPlaylistJob =
                launch {
                    localPlaylistRepository.getAllLocalPlaylists().collectLatest { list ->
                        _uiState.update { it.copy(listLocalPlaylist = list) }
                    }
                }
            val mainLyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.collectLatest { lyricsProvider ->
                        when (lyricsProvider) {
                            SIMPMUSIC -> {
                                _uiState.update { it.copy(mainLyricsProvider = SIMPMUSIC) }
                            }

                            YOUTUBE -> {
                                _uiState.update { it.copy(mainLyricsProvider = YOUTUBE) }
                            }

                            LRCLIB -> {
                                _uiState.update { it.copy(mainLyricsProvider = LRCLIB) }
                            }

                            BETTER_LYRICS -> {
                                _uiState.update { it.copy(mainLyricsProvider = BETTER_LYRICS) }
                            }

                            else -> {
                                log("Unknown lyrics provider", LogLevel.ERROR)
                            }
                        }
                    }
                }
            // 账号看门狗:MUSIC_U 身份变化/登出沿上立即取消在途链并清缓存列表。
            // 身份取 MUSIC_U 而非整串 cookie——__csrf/NMTID 等随 Set-Cookie 合而变,
            // 整串比对会把同账号误判成换号(2026-09-30 三轮 CR)。取消在途 job 很关键:
            // 只清列表的话,旧账号请求晚到成功仍会把旧列表写回来。跳过首帧(DataStore
            // 冷流首读可能是默认值,把"未初始化"当"登出"会误清),只在"见过真值后的
            // 变化沿"动作。
            val accountWatchdogJob =
                launch {
                    var seenNeteaseIdentity: String? = null
                    var seenHasNeteaseIdentity = false
                    var seenLoggedIn: Boolean? = null
                    combine(dataStoreManager.neteaseCookie, dataStoreManager.loggedIn) { c, l -> c to l }
                        .collect { (cookie, loggedIn) ->
                            val identity = com.maxrave.simpmusic.extension.neteaseAccountIdentity(cookie)
                            currentNeteaseIdentity = identity
                            if (seenHasNeteaseIdentity && identity != seenNeteaseIdentity) {
                                neteasePlaylistsJob?.cancel()
                                _uiState.update { it.copy(listNeteasePlaylist = null, neteasePlaylistsFailed = false) }
                            }
                            if (seenLoggedIn == true && loggedIn != DataStoreManager.TRUE) {
                                ytPlaylistsJob?.cancel()
                                _uiState.update { it.copy(listYouTubePlaylist = null, youTubePlaylistsFailed = false) }
                            }
                            seenNeteaseIdentity = identity
                            seenHasNeteaseIdentity = identity != null
                            seenLoggedIn = loggedIn == DataStoreManager.TRUE
                        }
                }
            loadYouTubePlaylists()
            sleepTimerJob.join()
            listLocalPlaylistJob.join()
            mainLyricsProviderJob.join()
            accountWatchdogJob.join()
        }
    }

    /**
     * YT 歌单加载(单飞):[force]=false 且已有在途链路时直接返回(init 预热路径);
     * force=true(弹窗打开要新鲜数据)取消旧链重起。getLibraryPlaylist 是冷流 emit
     * 一次即完,null=失败或服务端空。只在有结果时落 state——重试期间保留旧列表,
     * 弹窗不闪空;终态失败保留旧值并置 failed(列表空时弹窗出"重试"行)。
     */
    private fun loadYouTubePlaylists(force: Boolean = false) {
        if (!force && ytPlaylistsJob?.isActive == true) return
        ytPlaylistsJob?.cancel()
        ytPlaylistsJob =
            viewModelScope.launch {
                _uiState.update { it.copy(youTubePlaylistsFailed = false) }
                val data =
                    com.maxrave.simpmusic.extension.retryIf(
                        tag = "AddToPlaylist",
                        retryOn = { it == null },
                    ) { _ ->
                        playlistRepository.getLibraryPlaylist().firstOrNull()
                    }
                if (data != null) {
                    _uiState.update { state ->
                        state.copy(
                            listYouTubePlaylist = data.filterEditable(),
                        )
                    }
                } else {
                    _uiState.update { it.copy(youTubePlaylistsFailed = true) }
                }
            }
    }

    /** "VLLM"(自动混合歌单)不可加歌,弹窗列表恒过滤 */
    private fun List<PlaylistsResult>.filterEditable() = filter { it.browseId != "VLLM" }

    /** 当前网易账号身份(MUSIC_U),看门狗每次 cookie 发射时刷新;回包落库前校验用 */
    private var currentNeteaseIdentity: String? = null

    /**
     * 网易自建歌单加载(单飞,重入取消旧链):
     * - 非网易歌/未登录/账号失效([NeteaseNotLoggedInException])=清空——留着旧账号
     *   的歌单只会加错地方;
     * - 网络/风控/业务失败=退避重试(不可重试异常不耗退避),期间与终态都保留旧列表,
     *   终败置 failed(列表空时弹窗出"重试"行)。
     * 回包写入前校验账号身份仍与请求启动时一致——取消是主防线,这里是同线程兜底
     * (看门狗清列表后,在途旧回包成功仍会把旧列表写回,三轮 CR 实锤)。
     */
    private fun loadNeteasePlaylists(isNeteaseSong: Boolean) {
        neteasePlaylistsJob?.cancel()
        neteasePlaylistsJob =
            viewModelScope.launch {
                if (!isNeteaseSong) {
                    _uiState.update { it.copy(listNeteasePlaylist = null, neteasePlaylistsFailed = false) }
                    return@launch
                }
                val cookie = dataStoreManager.neteaseCookie.first()
                if (cookie.isBlank()) {
                    _uiState.update { it.copy(listNeteasePlaylist = null, neteasePlaylistsFailed = false) }
                    return@launch
                }
                val startIdentity = com.maxrave.simpmusic.extension.neteaseAccountIdentity(cookie)
                _uiState.update { it.copy(neteasePlaylistsFailed = false) }
                val result =
                    com.maxrave.simpmusic.extension.retryIf(
                        tag = "AddToPlaylist",
                        retryOn = { it.isFailure && it.exceptionOrNull() !is NeteaseNotLoggedInException },
                    ) { _ ->
                        neteaseRepository.getOwnNeteasePlaylistsResult()
                    }
                // 看门狗未首帧时 currentNeteaseIdentity 尚未知(null),放行写入——
                // 后续身份变化沿的看门狗会再清;已知且不一致=换号后旧回包,丢弃
                val identityStillCurrent =
                    currentNeteaseIdentity == null || currentNeteaseIdentity == startIdentity
                result
                    .onSuccess { list ->
                        if (identityStillCurrent) {
                            _uiState.update { it.copy(listNeteasePlaylist = list) }
                        } else {
                            com.maxrave.logger.Logger.w(
                                "AddToPlaylist",
                                "discard stale netease playlists: identity changed during flight",
                            )
                        }
                    }.onFailure { e ->
                        if (e is NeteaseNotLoggedInException) {
                            _uiState.update { it.copy(listNeteasePlaylist = null, neteasePlaylistsFailed = false) }
                        } else if (identityStillCurrent) {
                            _uiState.update { it.copy(neteasePlaylistsFailed = true) }
                        }
                    }
            }
    }

    fun resetPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collectLatest { list ->
                _uiState.update { it.copy(listLocalPlaylist = list) }
            }
        }
        loadYouTubePlaylists(force = true)
    }

    fun setSongEntity(songEntity: SongEntity?) {
        mvLookupJob?.cancel()
        _uiState.update { it.copy(mvCandidates = null, mvSearchLoading = false, mvPlaybackStarted = false) }
        val nowPlaying = mediaPlayerHandler.nowPlayingState.value
        val currentMvId = nowPlaying.mediaItem?.mediaId?.takeIf { it.startsWith(NETEASE_MV_MEDIA_ID_PREFIX) }
        val songOrNowPlaying = songEntity ?: (nowPlaying.songEntity?.let { current ->
            if (currentMvId != null) current.copy(videoId = currentMvId) else current
        } ?: return)
        loadNeteasePlaylists(songOrNowPlaying.videoId.toLongOrNull() != null)
        if (songOrNowPlaying.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
            // MV 不属于歌曲库:菜单使用队列实体，已有下载行只读观察，绝不插入 song 表。
            getSongAsFlow?.cancel()
            _uiState.update {
                it.copy(
                    songUIState = songOrNowPlaying.toSheetSongState(),
                    mvDownloading = sharedViewModel.mvDownloadProgress.value.containsKey(songOrNowPlaying.videoId),
                )
            }
            refreshCloudLiked(songOrNowPlaying.videoId)
            getSongAsFlow = viewModelScope.launch {
                songRepository.getSongAsFlow(songOrNowPlaying.videoId).collectLatest { downloaded ->
                    if (downloaded != null) {
                        _uiState.update { state ->
                            state.copy(
                                songUIState = state.songUIState.copy(
                                    downloadedVideoFilePath = downloaded.downloadedVideoFilePath,
                                ),
                            )
                        }
                    }
                }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    songUIState =
                        state.songUIState.copy(
                            isAddedToYouTubeLiked = false,
                        ),
                )
            }
            songRepository.getSongById(songOrNowPlaying.videoId).lastOrNull().let { song ->
                if (song != null) {
                    getSongEntityFlow(videoId = song.videoId)
                } else {
                    songRepository.insertSong(songOrNowPlaying).singleOrNull()?.let {
                        getSongEntityFlow(videoId = songOrNowPlaying.videoId)
                    }
                }
            }
        }
    }

    /**
     * 云端账号红心态(三点菜单"喜欢到云端账号"行):登录才拉得到,未登录保持 null=不显示行。
     * 与本地红心完全独立——显式操作云端,不 adopt 不镜像。
     */
    private val _cloudLiked = MutableStateFlow<Boolean?>(null)
    val cloudLiked: StateFlow<Boolean?> = _cloudLiked
    private var cloudLikeJob: Job? = null

    /**
     * "下载视频"行的下载中态:DownloadManager 视频条目在途实时流 × 当前歌曲。
     * 视频在途态不落 Room,音频行的 downloadState 流盖不住它,走内存条目流
     * (isVideoQueuedOrDownloading 的口径=视频条目在途,或双产物任务整体在途)。
     */
    val videoDownloading: StateFlow<Boolean> =
        combine(_uiState, downloadUtils.downloads) { state, _ ->
            val id = state.songUIState.videoId
            id.isNotEmpty() && downloadUtils.isVideoQueuedOrDownloading(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun refreshCloudLiked(videoId: String) {
        cloudLikeJob?.cancel()
        _cloudLiked.value = null
        cloudLikeJob = viewModelScope.launch {
            if (videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                val mvid = videoId.removePrefix(NETEASE_MV_MEDIA_ID_PREFIX).toLongOrNull() ?: return@launch
                // 正在播的 MV:跟播放页状态流(对账回写即时反映);列表行菜单里的
                // 非播放行播放页流覆盖不到,一次性拉 mv/detail 的 subed 兜底——
                // 没有它复选框恒灰死(收藏 MV 列表内取消收藏的入口就断了)
                var oneShotFetched = false
                sharedViewModel.nowPlayingScreenData.collectLatest { data ->
                    val playing = data.neteaseMvData?.takeIf { it.id == mvid }?.subed
                    if (playing != null) {
                        _cloudLiked.value = playing
                    } else if (!oneShotFetched) {
                        oneShotFetched = true
                        _cloudLiked.value = neteaseRepository.getMvDetail(mvid).getOrNull()?.subed
                    }
                }
            } else {
                _cloudLiked.value = songRepository.getRemoteLikeStatus(videoId)
            }
        }
    }

    private fun SongEntity.toSheetSongState() = NowPlayingBottomSheetUIState.SongUIState(
        videoId = videoId,
        title = title,
        listArtists = artistName?.mapIndexed { i, name ->
            Artist(name = name, id = artistId?.getOrNull(i) ?: "")
        } ?: emptyList(),
        thumbnails = thumbnails,
        liked = liked,
        downloadState = downloadState,
        album = albumName?.takeIf { it.isNotEmpty() }?.let { Album(name = it, id = albumId ?: "") },
        videoType = videoType,
        neteaseProgramId = neteaseProgramId,
        downloadedVideoFilePath = downloadedVideoFilePath,
    )

    private fun getSongEntityFlow(videoId: String) {
        getSongAsFlow?.cancel()
        if (videoId.isEmpty()) return
        refreshCloudLiked(videoId)
        getSongAsFlow =
            viewModelScope.launch {
                songRepository.getSongAsFlow(videoId).collectLatest { song ->
                    log("getSongEntityFlow: $song", LogLevel.WARN)
                    if (song != null) {
                        _uiState.update { state ->
                            state.copy(
                                songUIState =
                                    NowPlayingBottomSheetUIState.SongUIState(
                                        videoId = song.videoId,
                                        title = song.title,
                                        listArtists =
                                            song.artistName?.mapIndexed { i, name ->
                                                Artist(name = name, id = song.artistId?.getOrNull(i) ?: "")
                                            } ?: emptyList(),
                                        thumbnails = song.thumbnails,
                                        liked = song.liked,
                                        downloadState = song.downloadState,
                                        album =
                                            song.albumName?.takeIf { it.isNotEmpty() }?.let { name ->
                                                Album(name = name, id = song.albumId ?: "")
                                            },
                                        videoType = song.videoType,
                                        neteaseProgramId = song.neteaseProgramId,
                                        downloadedVideoFilePath = song.downloadedVideoFilePath,
                                    ),
                            )
                        }
                    }
                }
            }
    }

    /**
     * After the song's own download was removed, every downloaded container referencing it must
     * drop out of "downloaded" too, or its re-download watcher queues the song right back.
     */
    private suspend fun demoteDownloadedContainersOf(videoId: String) =
        demoteDownloadedContainers(
            videoId = videoId,
            playlistRepository = playlistRepository,
            albumRepository = albumRepository,
            localPlaylistRepository = localPlaylistRepository,
        )

    fun onUIEvent(ev: NowPlayingBottomSheetUIEvent) {
        val songUIState = uiState.value.songUIState
        if (songUIState.videoId.isEmpty()) return
        viewModelScope.launch {
            when (ev) {
                is NowPlayingBottomSheetUIEvent.DeleteFromPlaylist -> {
                    localPlaylistRepository
                        .removeTrackFromLocalPlaylist(
                            id = ev.playlistId,
                            song =
                                songRepository.getSongById(ev.videoId).lastOrNull() ?: run {
                                    makeToast(getString(Res.string.error_occurred))
                                    return@launch
                                },
                            successMessage = getString(Res.string.delete_song_from_playlist),
                            updatedYtMessage = getString(Res.string.removed_from_YouTube_playlist),
                            errorMessage = getString(Res.string.error_occurred),
                        ).collectResource(
                            onSuccess = {
                                makeToast(it ?: getString(Res.string.delete_song_from_playlist))
                            },
                            onError = {
                                makeToast(it)
                            },
                        )
                }

                is NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist -> {
                    localPlaylistRepository
                        .addYouTubePlaylistItem(
                            youtubePlaylistId = ev.browseId,
                            videoId = songUIState.videoId,
                        ).collectLatestResource(
                            // Success 载荷是响应里的状态枚举名("STATUS_SUCCEEDED"),不是给人读的文案,
                            // 曾被直接 toast 出裸 key;失败载荷同理是仓库写死的 "FAILED"
                            onSuccess = {
                                makeToast(getString(Res.string.added_to_youtube_playlist))
                            },
                            onError = {
                                makeToast(getString(Res.string.error_occurred))
                            },
                        )
                }

                is NowPlayingBottomSheetUIEvent.AddToNeteasePlaylist -> {
                    neteaseRepository
                        .addTracksToNeteasePlaylist(ev.playlistId, listOf(songUIState.videoId))
                        .fold(
                            onSuccess = { ok ->
                                makeToast(getString(if (ok) Res.string.added_to_netease_playlist else Res.string.netease_action_failed))
                            },
                            onFailure = { makeToast(getString(neteaseWriteErrorString(it, Res.string.netease_action_failed))) },
                        )
                }

                is NowPlayingBottomSheetUIEvent.ToggleLike -> {
                    if (songUIState.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                        // 显式传目标:列表行菜单打开的 sheet 里这行未必在播,
                        // 不传会错打到 nowPlayingState 的当前曲
                        val target = !(_cloudLiked.value ?: songUIState.liked)
                        sharedViewModel.setRemoteSongLiked(target, songUIState.videoId)
                        // 复选框不乐观翻转(对账铁律):按本笔写的落定事件(带目标 id,不会
                        // 被别曲写的结果串台)确认后回写
                        launch {
                            val settled =
                                sharedViewModel.likeWriteResults.first { it.videoId == songUIState.videoId }
                            if (settled.success) {
                                _cloudLiked.value = settled.liked
                            }
                        }
                        return@launch
                    }
                    // 点赞=云端账号红心,统一走 SharedViewModel 单一写路径(2026-10-09):
                    // 成功镜像本地缓存行(库页"喜欢的歌曲"读它)+目标在播时同步播放器红心/
                    // 通知图标+发列表页对账事件;失败 toast 按源分流(405 频控专属文案在内)。
                    // 此前这里直连仓库,给在播曲点赞时播放器红心不翻、也不发对账事件。
                    val target = !(_cloudLiked.value ?: songUIState.liked)
                    sharedViewModel.setRemoteSongLiked(target, songUIState.videoId)
                    launch {
                        val settled =
                            sharedViewModel.likeWriteResults.first { it.videoId == songUIState.videoId }
                        if (settled.success) {
                            _cloudLiked.value = settled.liked
                        }
                    }
                }


                is NowPlayingBottomSheetUIEvent.Download -> {
                    // 文件式下载(2026-10)三态:以"文件在不在"为准,不再信 downloadState 快照
                    // ——旧 SimpleCache 下载的歌 state=3 却无文件,按定稿走"未下载"直接文件
                    // 下载(旧缓存保留两份并存);"真已下载"=覆盖(删旧文件重新入队)。
                    val queuedOrDownloading =
                        songUIState.downloadState == DownloadState.STATE_PREPARING ||
                            songUIState.downloadState == DownloadState.STATE_DOWNLOADING ||
                            downloadUtils.isAudioQueuedOrDownloading(songUIState.videoId)
                    when {
                        queuedOrDownloading -> {
                            // Demote FIRST: while a referencing container still claims to be
                            // downloaded, its re-download watcher can observe the song vanishing
                            // and queue it right back — undoing the removal the user just asked
                            // for. Once demoted, nothing watches the song anymore.
                            demoteDownloadedContainersOf(songUIState.videoId)
                            downloadUtils.removeDownload(songUIState.videoId)
                            songRepository.updateDownloadState(
                                songUIState.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED,
                            )
                            makeToast(getString(Res.string.removed_download))
                        }

                        downloadUtils.isAudioFileDownloaded(songUIState.videoId) -> {
                            // 覆盖:删旧文件(文件+MediaStore 行+Room 列)后重新入队
                            demoteDownloadedContainersOf(songUIState.videoId)
                            downloadUtils.removeAudioDownload(songUIState.videoId)
                            songRepository.updateDownloadState(
                                videoId = songUIState.videoId,
                                downloadState = DownloadState.STATE_PREPARING,
                            )
                            val queued =
                                downloadUtils.downloadTrack(
                                    videoId = songUIState.videoId,
                                    title = songUIState.title,
                                    thumbnail = songUIState.thumbnails ?: "",
                                )
                            if (!queued) {
                                // 磁盘预检拒绝:回滚占位态,否则 UI/Room 永久停在"准备下载"
                                // (没有任务入队,DownloadManager 不会有状态来纠正它;CR P1-3)
                                songRepository.updateDownloadState(
                                    videoId = songUIState.videoId,
                                    downloadState = DownloadState.STATE_NOT_DOWNLOADED,
                                )
                                makeToast(getString(Res.string.download_no_space))
                            } else {
                                makeToast(getString(Res.string.downloading))
                            }
                        }

                        else -> {
                            songRepository.updateDownloadState(
                                videoId = songUIState.videoId,
                                downloadState = DownloadState.STATE_PREPARING,
                            )
                            val queued =
                                downloadUtils.downloadTrack(
                                    videoId = songUIState.videoId,
                                    title = songUIState.title,
                                    thumbnail = songUIState.thumbnails ?: "",
                                )
                            if (!queued) {
                                songRepository.updateDownloadState(
                                    videoId = songUIState.videoId,
                                    downloadState = DownloadState.STATE_NOT_DOWNLOADED,
                                )
                                makeToast(getString(Res.string.download_no_space))
                            } else {
                                makeToast(getString(Res.string.downloading))
                            }
                        }
                    }
                }

                is NowPlayingBottomSheetUIEvent.DownloadVideo -> {
                    // 仅 YT 歌(网易无视频流);重复提交在 DownloadManager 侧天然幂等
                    if (songUIState.videoId.toLongOrNull() == null) {
                        // 已下载视频的重新下载=覆盖:先删旧视频文件+条目再入队
                        // (addDownload 对存量 COMPLETED 条目不重启,不清等于白点)
                        if (downloadUtils.isVideoFileDownloaded(songUIState.videoId)) {
                            downloadUtils.removeVideoDownload(songUIState.videoId)
                        }
                        val queued =
                            downloadUtils.downloadVideo(
                                videoId = songUIState.videoId,
                                title = songUIState.title,
                                thumbnail = songUIState.thumbnails ?: "",
                            )
                        if (queued) {
                            makeToast(getString(Res.string.downloading))
                        } else {
                            // 磁盘预检拒绝(未入队),提示与音频下载行同源
                            makeToast(getString(Res.string.download_no_space))
                        }
                    }
                }

                is NowPlayingBottomSheetUIEvent.CancelVideoDownload -> {
                    // 只撤视频条目:双产物任务的音频半程(在途或已落文件)原样保留——
                    // 与 DeleteDownload(音频+视频全清)语义区分
                    downloadUtils.removeVideoDownload(songUIState.videoId)
                    makeToast(getString(Res.string.removed_download))
                }

                is NowPlayingBottomSheetUIEvent.DeleteVideoDownload -> {
                    // 删除已下载视频(只删视频文件+条目;视频歌本就不产音频)
                    downloadUtils.removeVideoDownload(songUIState.videoId)
                    makeToast(getString(Res.string.removed_download))
                }

                is NowPlayingBottomSheetUIEvent.DeleteDownload -> {
                    // 显式删除下载(2026-10 用户反馈:sheet 里"下载"保持三态,删除独立成行):
                    // 文件式删文件+Room,旧缓存条目引擎内分流;容器先降级防 watcher 重排队
                    demoteDownloadedContainersOf(songUIState.videoId)
                    downloadUtils.removeDownload(songUIState.videoId)
                    songRepository.updateDownloadState(
                        songUIState.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                    makeToast(getString(Res.string.removed_download))
                }

                is NowPlayingBottomSheetUIEvent.AddToPlaylist -> {
                    val targetPlaylist = uiState.value.listLocalPlaylist.find { it.id == ev.playlistId } ?: return@launch
                    val newList = (targetPlaylist.tracks ?: emptyList<String>()).toMutableList()
                    if (newList.contains(songUIState.videoId)) {
                        return@launch
                    } else {
                        val songEntity = songRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                        localPlaylistRepository
                            .addTrackToLocalPlaylist(
                                id = ev.playlistId,
                                song = songEntity,
                                successMessage = getString(Res.string.added_to_playlist),
                                updatedYtMessage = getString(Res.string.added_to_youtube_playlist),
                                errorMessage = getString(Res.string.error),
                            ).collectLatestResource(
                                onSuccess = {
                                    makeToast(it ?: getString(Res.string.added_to_playlist))
                                },
                                onError = {
                                    makeToast(it)
                                },
                            )
                    }
                }

                is NowPlayingBottomSheetUIEvent.PlayNext -> {
                    val songEntity = songRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    mediaPlayerHandler.playNext(songEntity.toTrack())
                    makeToast(getString(Res.string.play_next))
                }

                is NowPlayingBottomSheetUIEvent.AddToQueue -> {
                    val songEntity = songRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    val added =
                        mediaPlayerHandler.loadMoreCatalog(arrayListOf(songEntity.toTrack()), isAddToQueue = true)
                    // 已在队列的歌不加副本(2026-10-08),toast 按实际结果分流
                    makeToast(
                        getString(if (added) Res.string.added_to_queue else Res.string.already_in_queue),
                    )
                }

                is NowPlayingBottomSheetUIEvent.ChangeLyricsProvider -> {
                    if (listOf(SIMPMUSIC, YOUTUBE, LRCLIB, BETTER_LYRICS).contains(ev.lyricsProvider)) {
                        dataStoreManager.setLyricsProvider(ev.lyricsProvider)
                    } else {
                        return@launch
                    }
                }

                is NowPlayingBottomSheetUIEvent.SetSleepTimer -> {
                    if (ev.cancel) {
                        mediaPlayerHandler.sleepStop()
                        makeToast(getString(Res.string.sleep_timer_off_done))
                    } else if (ev.minutes > 0) {
                        mediaPlayerHandler.sleepStart(ev.minutes)
                    }
                }

                is NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch -> {
                    dataStoreManager.setPlaybackSpeed(ev.speed)
                    dataStoreManager.setPitch(ev.pitch)
                }

                is NowPlayingBottomSheetUIEvent.Share -> {
                    // 网易数字 ID 拼进 YT 链接是无效地址;按 ID 形状分源拼分享链接
                    val url =
                        if (songUIState.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)) {
                            "https://music.163.com/mv?id=${songUIState.videoId.removePrefix(NETEASE_MV_MEDIA_ID_PREFIX)}"
                        } else if (songUIState.videoId.toLongOrNull() != null) {
                            "https://music.163.com/song?id=${songUIState.videoId}"
                        } else {
                            "https://music.youtube.com/watch?v=${songUIState.videoId}"
                        }
                    shareUrl(
                        title = getString(Res.string.share_url),
                        url,
                    )
                }

                is NowPlayingBottomSheetUIEvent.WatchMv -> {
                    // 歌曲→MV id→详情→单 MV 队列起播;无 MV/解析失败都给明确反馈
                    val mvId = neteaseRepository.getNeteaseSongMvId(ev.videoId)
                    if (mvId == null || mvId <= 0L) {
                        makeToast(getString(Res.string.no_mv_available))
                    } else {
                        val track = neteaseRepository.getMvTrack(mvId)
                        if (track == null) {
                            makeToast(getString(Res.string.error))
                        } else {
                            sharedViewModel.playMv(track)
                        }
                    }
                }

                is NowPlayingBottomSheetUIEvent.WatchYouTubeMv -> {
                    mvLookupJob?.cancel()
                    _uiState.update { it.copy(mvCandidates = null, mvSearchLoading = true, mvPlaybackStarted = false) }
                    mvLookupJob = viewModelScope.launch {
                        val candidates = try {
                            songRepository.findMusicVideos(ev.videoId, ev.title, ev.artist)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            _uiState.update { it.copy(mvSearchLoading = false) }
                            makeToast(getString(Res.string.error))
                            return@launch
                        }
                        when (candidates.size) {
                            0 -> {
                                _uiState.update { it.copy(mvSearchLoading = false) }
                                makeToast(getString(Res.string.no_mv_available))
                            }
                            1 -> {
                                sharedViewModel.playYouTubeMv(candidates.first())
                                _uiState.update { it.copy(mvSearchLoading = false, mvPlaybackStarted = true) }
                            }
                            else -> _uiState.update { it.copy(mvCandidates = candidates, mvSearchLoading = false) }
                        }
                    }
                }

                is NowPlayingBottomSheetUIEvent.PlayYouTubeMv -> {
                    sharedViewModel.playYouTubeMv(ev.track)
                    _uiState.update { it.copy(mvCandidates = null, mvPlaybackStarted = true) }
                }

                is NowPlayingBottomSheetUIEvent.DismissMvCandidates -> {
                    _uiState.update { it.copy(mvCandidates = null) }
                }

                is NowPlayingBottomSheetUIEvent.DownloadMv -> {
                    // MV mp4 直存(单文件自带音轨免合并);进度弹窗挂在 Info 面板里,菜单直点
                    // 看不到——点下即 toast 确认受理,完成/失败由 downloadMvFile 再 toast
                    sharedViewModel.downloadMvFile(songUIState.videoId)
                    makeToast(getString(Res.string.downloading))
                }

                is NowPlayingBottomSheetUIEvent.StartRadio -> {
                    songRepository
                        .getRadioFromEndpoint(
                            YouTubeWatchEndpoint(
                                videoId = ev.videoId,
                                playlistId = songRadioPlaylistId(ev.videoId),
                            ),
                        ).collectLatest { res ->
                            val data = res.data
                            when (res) {
                                is Resource.Success if (data != null && data.first.isNotEmpty()) -> {
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = data.first,
                                            firstPlayedTrack = data.first.first(),
                                            playlistId = songRadioPlaylistId(ev.videoId),
                                            playlistName = ev.name,
                                            playlistType = PlaylistType.RADIO,
                                            continuation = data.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        data.first.first(),
                                        Config.PLAYLIST_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(res.message ?: getString(Res.string.error))
                                }
                            }
                        }
                }
            }
        }
    }
}

data class NowPlayingBottomSheetUIState(
    val songUIState: SongUIState = SongUIState(),
    val listLocalPlaylist: List<LocalPlaylistEntity>,
    /** null=本会话还没成功拉到过(弹窗出加载态);非 null=已有结果,刷新期间保留旧值不闪空 */
    val listYouTubePlaylist: List<PlaylistsResult>?,
    val listNeteasePlaylist: List<PlaylistsResult>?,
    /** 终态失败(重试耗尽):列表为 null/空时弹窗出"重试"行;有旧列表则静默保留 */
    val youTubePlaylistsFailed: Boolean = false,
    /** MV 导出下载在途(共享 downloadFileProgress);下载 MV 行三态用 */
    val mvDownloading: Boolean = false,
    val mvCandidates: List<Track>? = null,
    val mvSearchLoading: Boolean = false,
    val mvPlaybackStarted: Boolean = false,
    val neteasePlaylistsFailed: Boolean = false,
    val mainLyricsProvider: String,
    val sleepTimer: SleepTimerState,
) {
    data class SongUIState(
        val videoId: String = "",
        val title: String = "",
        val listArtists: List<Artist> = emptyList(),
        val thumbnails: String? = null,
        val liked: Boolean = false,
        val isAddedToYouTubeLiked: Boolean = false,
        val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
        val album: Album? = null,
        /** YT 的 MUSIC_VIDEO_TYPE_*(ATV=纯音频曲目,无视频流可下);网易歌为空串 */
        val videoType: String = "",
        /** 播客节目行(网易电台剧集):非空=下载入口整组隐藏(2026-10-01 用户定,播客不提供下载) */
        val neteaseProgramId: Long? = null,
        /** 视频文件路径(Room 实时流):非空且文件在="下载视频"行显示已下载态 */
        val downloadedVideoFilePath: String? = null,
    )
}

sealed class NowPlayingBottomSheetUIEvent {
    data class DeleteFromPlaylist(
        val videoId: String,
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data object ToggleLike : NowPlayingBottomSheetUIEvent()


    data object Download : NowPlayingBottomSheetUIEvent()

    /** 视频文件下载(文件式,仅 YT 歌):音视频双流 merge mp4 落 Music/SimpMusic[/主艺人/专辑](与音频同树,回落 Movies) */
    data object DownloadVideo : NowPlayingBottomSheetUIEvent()

    /** 取消视频下载(只撤视频条目,音频任务/已落文件不动) */
    data object CancelVideoDownload : NowPlayingBottomSheetUIEvent()

    /** 删除已下载视频(只删视频文件+条目) */
    data object DeleteVideoDownload : NowPlayingBottomSheetUIEvent()

    /** 删除已下载的文件(独立于下载行的覆盖语义;仅已下载的歌显示入口) */
    data object DeleteDownload : NowPlayingBottomSheetUIEvent()

    data class AddToPlaylist(
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data class AddToYouTubePlaylist(
        val browseId: String,
    ) : NowPlayingBottomSheetUIEvent()

    data class AddToNeteasePlaylist(
        val playlistId: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object PlayNext : NowPlayingBottomSheetUIEvent()

    data object AddToQueue : NowPlayingBottomSheetUIEvent()

    data class ChangeLyricsProvider(
        val lyricsProvider: String,
    ) : NowPlayingBottomSheetUIEvent()

    data class SetSleepTimer(
        val cancel: Boolean = false,
        val minutes: Int = 0,
    ) : NowPlayingBottomSheetUIEvent()

    data class ChangePlaybackSpeedPitch(
        val speed: Float,
        val pitch: Int,
    ) : NowPlayingBottomSheetUIEvent()

    data class WatchMv(
        val videoId: String,
    ) : NowPlayingBottomSheetUIEvent()

    data class WatchYouTubeMv(
        val videoId: String,
        val title: String,
        val artist: String?,
    ) : NowPlayingBottomSheetUIEvent()

    data class PlayYouTubeMv(val track: Track) : NowPlayingBottomSheetUIEvent()

    data object DismissMvCandidates : NowPlayingBottomSheetUIEvent()

    data object DownloadMv : NowPlayingBottomSheetUIEvent()

    data class StartRadio(
        val videoId: String,
        val name: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object Share : NowPlayingBottomSheetUIEvent()
}
