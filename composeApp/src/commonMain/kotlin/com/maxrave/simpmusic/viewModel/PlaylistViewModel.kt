@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.NETEASE_CLOUD_DISK_PAGE_PREFIX
import com.maxrave.common.NETEASE_CLOUD_DISK_PLAYLIST_ID
import com.maxrave.data.repository.NeteaseRepositoryImpl
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.maxrave.domain.data.entities.DownloadState.STATE_PREPARING
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADING
import com.maxrave.domain.data.entities.DownloadState.STATE_NOT_DOWNLOADED
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.Author
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.simpmusic.expect.cloudUploadBodyFactory
import com.maxrave.simpmusic.expect.readCloudUploadFile
import com.maxrave.simpmusic.expect.readCloudUploadArtwork
import com.maxrave.simpmusic.expect.readCloudUploadFileName
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.collectLatestResource
import com.maxrave.domain.utils.isRadioPlaylistId
import com.maxrave.domain.utils.toListVideoId
import com.maxrave.domain.utils.toPlaylistEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Error
import com.maxrave.simpmusic.extension.neteaseWriteErrorString
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Loading
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Success
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import com.maxrave.simpmusic.viewModel.base.BatchDownloadRequest
import com.maxrave.simpmusic.viewModel.base.BatchDownloadSong
import com.maxrave.simpmusic.viewModel.base.demoteDownloadedContainers
import com.maxrave.simpmusic.viewModel.base.removeExclusiveTrackDownloads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.partially_loaded
import simpmusic.composeapp.generated.resources.auto_created_by_youtube_music
import simpmusic.composeapp.generated.resources.cloud_delete_done
import simpmusic.composeapp.generated.resources.cloud_delete_partial
import simpmusic.composeapp.generated.resources.cloud_disk
import simpmusic.composeapp.generated.resources.cloud_disk_capacity
import simpmusic.composeapp.generated.resources.cloud_upload_all_done
import simpmusic.composeapp.generated.resources.cloud_upload_all_failed
import simpmusic.composeapp.generated.resources.cloud_upload_partial_done
import simpmusic.composeapp.generated.resources.cloud_upload_read_failed
import simpmusic.composeapp.generated.resources.download_no_space
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.removed_from_playlist
import simpmusic.composeapp.generated.resources.netease_action_failed
import simpmusic.composeapp.generated.resources.cloud_action_failed_netease
import simpmusic.composeapp.generated.resources.saved_toast
import simpmusic.composeapp.generated.resources.unsaved_toast
import simpmusic.composeapp.generated.resources.cloud_action_failed_youtube
import simpmusic.composeapp.generated.resources.deleted_playlist
import simpmusic.composeapp.generated.resources.unsubscribed_netease_playlist
import simpmusic.composeapp.generated.resources.unsubscribed_youtube_playlist
import simpmusic.composeapp.generated.resources.remove_from_playlist_failed
import simpmusic.composeapp.generated.resources.download_cancelled
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.playlist_is_empty
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.radio_not_available
import simpmusic.composeapp.generated.resources.removed_download
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.shuffle_not_available
import simpmusic.composeapp.generated.resources.synced
import simpmusic.composeapp.generated.resources.syncing
import simpmusic.composeapp.generated.resources.view_count

class PlaylistViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val neteaseRepository: NeteaseRepositoryImpl,
    private val dataStoreManager: DataStoreManager,
    private val sharedViewModel: SharedViewModel,
) : BaseViewModel() {
    // 红心歌单页不做"取消红心→立即剔歌/计数-1"(用户 2026-09-22 定案:页面会闪动,
    // 效果差;云端为准,下次进入自然更新)。曾经有 B1 下降沿联动+onTrackLikeChanged
    // 即时剔歌,已整体撤除;三选一菜单的"从歌单移除"是显式移除操作,保留剔歌。
    val downloadUtils: DownloadHandler by inject<DownloadHandler>()
    private val albumRepository: AlbumRepository by inject<AlbumRepository>()
    private val mutationBus: LibraryMutationBus by inject()
    private var _uiState = MutableStateFlow<PlaylistUIState>(Loading)
    val uiState: StateFlow<PlaylistUIState> = _uiState

    private var _listColors = MutableStateFlow<List<Color>>(emptyList())
    val listColors: StateFlow<List<Color>> = _listColors

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private var _playlistEntity: MutableStateFlow<PlaylistEntity?> = MutableStateFlow(null)
    var playlistEntity: StateFlow<PlaylistEntity?> = _playlistEntity

    val downloadState = _playlistEntity.map { it?.downloadState ?: 0 }.stateIn(viewModelScope, WhileSubscribed(1000), 0)
    val liked = _playlistEntity.map { it?.liked == true }.stateIn(viewModelScope, WhileSubscribed(1000), false)

    private val _remoteSaved = MutableStateFlow<Boolean?>(null)
    val remoteSaved: StateFlow<Boolean?> = _remoteSaved
    private val _remoteSavePending = MutableStateFlow(false)
    val remoteSavePending: StateFlow<Boolean> = _remoteSavePending

    /**
     * 详情页顶栏收藏心是否该出现:自建歌单(YT=browse 响应可编辑包装+库页"创建的歌单"
     * 分区导航参数双信号,网易=creatorId==uid)与红心歌单(YT Liked Music/网易"我喜欢的
     * 音乐")没有"收藏进资料库"的语义,心隐藏。默认 true(fail-open):判定晚到时心迟一点
     * 消失,不让收藏歌单的心迟迟不出现。
     */
    private val _favoriteAvailable = MutableStateFlow(true)
    val favoriteAvailable: StateFlow<Boolean> = _favoriteAvailable

    /** 从库页"创建的歌单/系统歌单"分区进入时为 true(nav 参数,不依赖网络) */
    private var ownYouTubeNavHint = false

    private fun refreshFavoriteAvailability(
        id: String,
        ownYouTube: Boolean,
    ) {
        if (id.toLongOrNull() != null) {
            viewModelScope.launch {
                val hidden =
                    neteaseRepository.isNeteaseLikedPlaylist(id) ||
                        neteaseRepository.isOwnNeteasePlaylist(id)
                _favoriteAvailable.value = !hidden
            }
        } else {
            _favoriteAvailable.value = !(ownYouTube || ownYouTubeNavHint)
        }
    }

    private var _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    /** 是否红心歌单("我喜欢的音乐"):不可删除/不可取消收藏,详情页两个入口都排除 */
    suspend fun isNeteaseLikedPlaylist(): Boolean {
        val id = (uiState.value as? Success)?.data?.id ?: return false
        return id.toLongOrNull() != null && neteaseRepository.isNeteaseLikedPlaylist(id)
    }

    /**
     * 红心歌单页内取消红心后的原地移行(2026-10-08 用户定:对齐业内通用处理——
     * 操作语境在列表内=行应立即消失;云端为准,刷新/下次进入对账)。仅红心歌单调用:
     * 普通歌单里取消红心≠移出歌单,不移行。
     */
    fun removeTrackLocally(videoId: String) {
        _tracks.value = _tracks.value.filterNot { it.videoId == videoId }
    }

    /** 别处(迷你条/播放页)点亮红心:新歌插红心歌单首行(服务端口径=最新在前;按 id 去重) */
    fun insertTrackAtTop(track: Track) {
        if (_tracks.value.any { it.videoId == track.videoId }) return
        _tracks.value = listOf(track) + _tracks.value
    }

    /** 当前歌单是否网易自建(歌曲菜单露"从歌单移除"的判定;收藏歌单/雷达/YT 恒 false) */
    suspend fun isNeteaseOwnPlaylist(): Boolean {
        val id = (uiState.value as? Success)?.data?.id ?: return false
        return id.toLongOrNull() != null && neteaseRepository.isOwnNeteasePlaylist(id)
    }

    /** 收藏心=云端账号状态:拉到即驱动显示,并镜像本地缓存行(库页收藏分区读它)。 */
    private fun maybeAdoptRemoteSaved(playlistId: String) {
        viewModelScope.launch {
            val cloud = playlistRepository.getRemoteSavedState(playlistId)
            _remoteSaved.value = cloud
            if (cloud != null) {
                val localLiked = _playlistEntity.value?.liked == true
                if (localLiked != cloud) {
                    playlistRepository.updatePlaylistLiked(playlistId, if (cloud) 1 else 0)
                }
                _playlistEntity.update { it?.copy(liked = cloud) }
            }
        }
    }

    private fun refreshRemoteSavedState(playlistId: String) {
        _remoteSaved.value = null
        viewModelScope.launch {
            _remoteSaved.value = playlistRepository.getRemoteSavedState(playlistId)
        }
    }

    /**
     * 收藏心点击的唯一路径:直接切换云端账号收藏。成功后镜像本地缓存行并给中性 toast,
     * 失败按平台 toast。自建网易歌单无"收藏"概念,心已在 UI 隐藏不会走到这里。
     * 双向都发库页本地回写:取消收藏→移除;收藏→插入完整行(收藏发生时正在看这个歌单,
     * 实体数据现成在手,库页插入的行与网络拉回的同构,无占位跳变)。
     */
    fun setRemoteSaved(saved: Boolean) {
        val id = (uiState.value as? Success)?.data?.id ?: return
        val isNeteaseId = id.toLongOrNull() != null
        viewModelScope.launch {
            _remoteSavePending.value = true
            // 网易分支失败会抛出服务端原文(如"操作过于频繁,请稍后再试"),直接 toast 更可读
            val outcome = runCatching { playlistRepository.setRemoteSavedState(id, saved) }
            if (outcome.getOrDefault(false)) {
                _remoteSaved.value = saved
                playlistRepository.updatePlaylistLiked(id, if (saved) 1 else 0)
                _playlistEntity.update { it?.copy(liked = saved) }
                makeToast(getString(if (saved) Res.string.saved_toast else Res.string.unsaved_toast))
                if (saved) {
                    favoritedPlaylistEntity()?.let { entity ->
                        mutationBus.send(LibraryMutation.PlaylistFavorited(entity))
                    }
                } else {
                    // 取消收藏成功 → 库页歌单分区原地移除(本地回写)
                    mutationBus.send(LibraryMutation.PlaylistRemoved(id))
                }
            } else {
                makeToast(
                    outcome.exceptionOrNull()?.message
                        ?: getString(if (isNeteaseId) Res.string.cloud_action_failed_netease else Res.string.cloud_action_failed_youtube),
                )
            }
            _remoteSavePending.value = false
        }
    }

    /** 收藏事件的载荷:详情页完整实体;Room 行缺席时从页面状态兜底构造 */
    private fun favoritedPlaylistEntity(): com.maxrave.domain.data.entities.PlaylistEntity? {
        _playlistEntity.value?.let { return it }
        val state = (uiState.value as? Success)?.data ?: return null
        return com.maxrave.domain.data.entities.PlaylistEntity(
            id = state.id,
            author = state.author?.name,
            thumbnails = state.thumbnail.orEmpty(),
            title = state.title,
            trackCount = state.trackCount,
        )
    }

    /**
     * 删除自己的网易歌单(/playlist/delete,不可逆)。成功后清本地 liked(红心熄灭);
     * Room 缓存行随下次访问自然失效(歌单已不存在)。导航返回由调用方(UI)处理。
     */
    fun deleteNeteasePlaylist() {
        val id = (uiState.value as? Success)?.data?.id ?: return
        if (id.toLongOrNull() == null) return
        viewModelScope.launch { withContext(NonCancellable) {
            neteaseRepository
                .deleteNeteasePlaylist(id)
                .fold(
                    onSuccess = { ok ->
                        if (ok) {
                            playlistRepository.updatePlaylistLiked(id, 0)
                            _playlistEntity.update { it?.copy(liked = false) }
                            makeToast(getString(Res.string.deleted_playlist))
                            mutationBus.send(LibraryMutation.PlaylistRemoved(id))
                        } else {
                            makeToast(getString(Res.string.netease_action_failed))
                        }
                    },
                    onFailure = { makeToast(getString(neteaseWriteErrorString(it, Res.string.netease_action_failed))) },
                )
         } }
    }

    /**
     * 收藏歌单详情页"取消收藏"(YT):like/removelike 移出资料库,成功后发库页本地回写。
     * 不自动返回导航(歌单还在云端,调用方留在当前页)。
     */
    fun unsubscribeYouTubePlaylist() {
        val rawId = (uiState.value as? Success)?.data?.id ?: return
        if (rawId.toLongOrNull() != null) return
        val id = if (rawId.startsWith("VL")) rawId else "VL$rawId"
        viewModelScope.launch { withContext(NonCancellable) {
            if (playlistRepository.removeYouTubePlaylistFromLibrary(id)) {
                _playlistEntity.update { it?.copy(liked = false) }
                _remoteSaved.value = false
                makeToast(getString(Res.string.unsubscribed_youtube_playlist))
                mutationBus.send(LibraryMutation.PlaylistRemoved(id))
            } else {
                makeToast(getString(Res.string.netease_action_failed))
            }
         } }
    }

    /**
     * 删除自己的 YT 歌单(playlist/delete,不可逆;2026-09-22 补齐——自建歌单详情页
     * "更多"菜单,与网易自建同款入口)。成功后发库页本地回写。
     */
    fun deleteYouTubePlaylist() {
        val rawId = (uiState.value as? Success)?.data?.id ?: return
        if (rawId.toLongOrNull() != null) return
        // 详情页 id 可能缺 VL 前缀(两个来源形状不一);playlist/delete 要求原样 browseId,
        // 统一补齐(Metrolist 同款透传形状)
        val id = if (rawId.startsWith("VL")) rawId else "VL$rawId"
        viewModelScope.launch { withContext(NonCancellable) {
            if (playlistRepository.deleteYouTubePlaylist(id)) {
                makeToast(getString(Res.string.deleted_playlist))
                mutationBus.send(LibraryMutation.PlaylistRemoved(id))
            } else {
                makeToast(getString(Res.string.netease_action_failed))
            }
         } }
    }

    /**
     * 歌单页"更多"菜单取消收藏网易歌单:云端 subscribe t=0 + 本地 liked 清零(红心熄灭),
     * 与库页长按路径行为一致(那边不持有页面状态,只清云端)。
     */
    fun unsubscribeNeteasePlaylist() {
        val id = (uiState.value as? Success)?.data?.id ?: return
        if (id.toLongOrNull() == null) return
        viewModelScope.launch { withContext(NonCancellable) {
            neteaseRepository
                .subscribeNeteasePlaylist(id, subscribe = false)
                .fold(
                    onSuccess = { ok ->
                        if (ok) {
                            playlistRepository.updatePlaylistLiked(id, 0)
                            _playlistEntity.update { it?.copy(liked = false) }
                            makeToast(getString(Res.string.unsubscribed_netease_playlist))
                            mutationBus.send(LibraryMutation.PlaylistRemoved(id))
                        } else {
                            makeToast(getString(Res.string.netease_action_failed))
                        }
                    },
                    onFailure = { makeToast(getString(neteaseWriteErrorString(it, Res.string.netease_action_failed))) },
                )
         } }
    }

    /** 红心歌单的"移除歌单"=取消红心:云村 unlike + 本地 liked 清零 + 内存列表剔掉 */
    private fun unlikeNeteaseSong(videoId: String) {
        viewModelScope.launch {
            neteaseRepository
                .setSongLiked(videoId, like = false)
                .fold(
                    onSuccess = { ok ->
                        if (ok) {
                            songRepository.setLikedLocal(videoId, 0)
                            _tracks.update { list -> list.filterNot { it.videoId == videoId } }
                            (uiState.value as? Success)?.data?.let { state ->
                                _uiState.value =
                                    Success(state.copy(trackCount = (state.trackCount - 1).coerceAtLeast(0)))
                            }
                            makeToast(getString(Res.string.removed_from_playlist))
                            // 库页红心歌单行计数 -1(本地回写)
                            mutationBus.send(LibraryMutation.NeteaseHeartCountChanged(-1))
                        } else {
                            makeToast(getString(Res.string.remove_from_playlist_failed))
                        }
                    },
                    onFailure = { makeToast(getString(neteaseWriteErrorString(it, Res.string.remove_from_playlist_failed))) },
                )
        }
    }

    /**
     * 从网易自建歌单移除一首歌。**红心歌单特例:移除=取消红心**(/song/like t=0 + 本地
     * liked 清零),与播放页红心按钮同一逻辑——歌不在红心歌单里了,红心自然也没了。
     * 其余自建歌单走 manipulate op=del:云端删成功后从内存列表剔掉,Room 缓存行随下次
     * 整单拉取/EXHAUST 回写自然收敛。
     */
    fun removeTrackFromNeteasePlaylist(videoId: String) {
        val id = (uiState.value as? Success)?.data?.id ?: return
        if (id.toLongOrNull() == null) return
        if (neteaseRepository.isNeteaseLikedPlaylist(id)) {
            unlikeNeteaseSong(videoId)
            return
        }
        viewModelScope.launch {
            neteaseRepository
                .removeTracksFromNeteasePlaylist(id, listOf(videoId))
                .fold(
                    onSuccess = { ok ->
                        if (ok) {
                            _tracks.update { list -> list.filterNot { it.videoId == videoId } }
                            // 表头计数跟着减(PlaylistState.trackCount 是服务端元数据快照,
                            // 不改会一直显示删除前的数字;Room 缓存行随下次整单拉取收敛)
                            (uiState.value as? Success)?.data?.let { state ->
                                _uiState.value =
                                    Success(
                                        state.copy(trackCount = (state.trackCount - 1).coerceAtLeast(0)),
                                    )
                            }
                            makeToast(getString(Res.string.removed_from_playlist))
                        } else {
                            makeToast(getString(Res.string.remove_from_playlist_failed))
                        }
                    },
                    onFailure = { makeToast(getString(neteaseWriteErrorString(it, Res.string.remove_from_playlist_failed))) },
                )
        }
    }

    /** 从 YT 自建歌单移除一首歌(拉齐网易 removeTrackFromNeteasePlaylist):云端删成功后
     *  内存列表原位剔掉+表头计数-1,不整刷网络(防闪)。移除条目需要 setVideoId,仓储层
     *  缓存 miss 会整单拉一次;仅自建歌单有权限,入口已按 isYourYouTubePlaylist 门控。 */
    fun removeTrackFromYouTubePlaylist(videoId: String) {
        val id = (uiState.value as? Success)?.data?.id ?: return
        if (id.toLongOrNull() != null) return
        viewModelScope.launch {
            if (playlistRepository.removeTrackFromYouTubePlaylist(id, videoId)) {
                _tracks.update { list -> list.filterNot { it.videoId == videoId } }
                (uiState.value as? Success)?.data?.let { state ->
                    _uiState.value =
                        Success(
                            state.copy(trackCount = (state.trackCount - 1).coerceAtLeast(0)),
                        )
                }
                makeToast(getString(Res.string.removed_from_playlist))
            } else {
                makeToast(getString(Res.string.remove_from_playlist_failed))
            }
        }
    }

    private var _tracksListState = MutableStateFlow<ListState>(ListState.IDLE)
    val tracksListState: StateFlow<ListState> = _tracksListState

    private var collectDownloadedJob: Job? = null
    private var _downloadedList = MutableStateFlow<List<String>>(emptyList())
    val downloadedList: StateFlow<List<String>> = _downloadedList

    private var playlistEntityJob: Job? = null
    private var newUpdateJob: Job? = null
    private var checkDownloadedPlaylist: Job? = null

    init {
        viewModelScope.launch {
            val listTrackStringJob =
                launch(Dispatchers.IO) {
                    downloadState
                        .collectLatest { state ->
                            newUpdateJob?.cancel()
                            val id = playlistEntity.value?.id ?: return@collectLatest
                            if (state == STATE_DOWNLOADING || state == STATE_DOWNLOADED) {
                                getFullTracks { tracks, _ ->
                                    // The fetch outlives this collector's cancellation, so an
                                    // in-flight callback can land after removeDownloadedPlaylist()
                                    // already reset the playlist to NOT_DOWNLOADED — with the
                                    // captured "downloaded" state it would queue every track right
                                    // back. Re-read the live state instead.
                                    val liveState = downloadState.value
                                    if (liveState != STATE_DOWNLOADING && liveState != STATE_DOWNLOADED) {
                                        return@getFullTracks
                                    }
                                    newUpdateJob =
                                        launch {
                                            val listSongs = songRepository.getSongsByListVideoId(tracks.toListVideoId()).firstOrNull() ?: emptyList()
                                            if (liveState == STATE_DOWNLOADED && listSongs.isNotEmpty()) {
                                                listSongs.filter { it.downloadState != STATE_DOWNLOADED }.let { notDownloaded ->
                                                    if (notDownloaded.isNotEmpty()) {
                                                        downloadTracks(notDownloaded.map { it.videoId })
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                                    }
                                                    // 文件式完成口径(2026-10-02):转存成功即从 DownloadIndex
                                                    // 清条目,downloads 流永远凑不齐"全部 COMPLETED",下方
                                                    // count 翻转对新代下载不可达(整单下完按钮翻不了"已下载")
                                                    // ——按 song 行兜底翻转(落地对账保证有文件的行=3):一次
                                                    // 评估治"误置下载中"的卡死,持续收集让最后一首落地即翻态。
                                                    // 行数须与队列等长,防缺行歌单误翻
                                                    launch {
                                                        songRepository.getSongsByListVideoId(tracks.toListVideoId()).collect { songs ->
                                                            if (downloadState.value != STATE_DOWNLOADING) return@collect
                                                            if (
                                                                songs.size == tracks.size &&
                                                                songs.all { it.downloadState == STATE_DOWNLOADED } &&
                                                                tracks.none { downloadUtils.isAudioQueuedOrDownloading(it.videoId) }
                                                            ) {
                                                                updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                            }
                                                        }
                                                    }
                                                    downloadUtils.downloads.collectLatest { downloads ->
                                                // Same guard as the callback above: a playlist
                                                // demoted mid-flight must not be written back.
                                                val live = downloadState.value
                                                if (live != STATE_DOWNLOADING && live != STATE_DOWNLOADED) {
                                                    return@collectLatest
                                                }
                                                var count = 0
                                                tracks.forEachIndexed { index, track ->
                                                    val trackDownloadState = downloads[track.videoId]?.first?.state
                                                    val videoDownloadState =
                                                        downloads[track.videoId]?.second?.state ?: DownloadHandler.State.STATE_COMPLETED
                                                    if (trackDownloadState == DownloadHandler.State.STATE_DOWNLOADING ||
                                                        videoDownloadState == DownloadHandler.State.STATE_DOWNLOADING
                                                    ) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else if (trackDownloadState == DownloadHandler.State.STATE_COMPLETED &&
                                                        videoDownloadState == DownloadHandler.State.STATE_COMPLETED
                                                    ) {
                                                        count++
                                                    }
                                                    if (count == tracks.size) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
            listTrackStringJob.join()
        }
    }

    private fun updatePlaylistDownloadState(
        id: String,
        state: Int,
    ) {
        viewModelScope.launch {
            playlistRepository.updatePlaylistDownloadState(id, state)
            delay(500)
            _playlistEntity.update {
                it?.copy(downloadState = state)
            }
        }
    }

    private fun downloadTracks(listJob: List<String>) {
        viewModelScope.launch {
            listJob.forEach { videoId ->
                songRepository.getSongById(videoId).singleOrNull()?.let { song ->
                    if (song.downloadState != STATE_DOWNLOADED) {
                        downloadUtils.downloadTrack(videoId, song.title, song.thumbnails ?: "")
                    }
                }
            }
        }
    }

    private fun resetData() {
        _uiState.value = Loading
        _playlistEntity.value = null
        _downloadedList.value = emptyList()
        _listColors.value = emptyList()
        _favoriteAvailable.value = true
        checkDownloadedPlaylist?.cancel()
        checkDownloadedPlaylist = null
    }

    fun getData(
        id: String,
        ownYouTubeNavHint: Boolean = false,
    ) {
        this.ownYouTubeNavHint = ownYouTubeNavHint
        resetData()
        viewModelScope.launch {
            // 云盘伪歌单(NETEASE_CLOUD_DISK 哨兵):页面完全复用红心歌单详情页,数据源
            // 换成 /v1/cloud/get 分页;不是真歌单,Room/收藏心/远端收藏态全不参与
            if (id == NETEASE_CLOUD_DISK_PLAYLIST_ID) {
                getCloudDiskData(firstLoad = true)
                return@launch
            }
            // Check radio
            if (id.isRadioPlaylistId()) {
                playlistRepository
                    .getRadio(
                        id,
                        radioString = getString(Res.string.radio),
                        defaultDescription = getString(Res.string.auto_created_by_youtube_music),
                        viewString = getString(Res.string.view_count),
                    ).collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Radio data: $data")
                                _uiState.value =
                                    Success(
                                        data =
                                            PlaylistState(
                                                id = data.first.id,
                                                title = data.first.title,
                                                isRadio = true,
                                                author = data.first.author,
                                                thumbnail =
                                                    data.first.thumbnails
                                                        .lastOrNull()
                                                        ?.url,
                                                description = data.first.description,
                                                trackCount = data.first.trackCount,
                                                year = data.first.year,
                                            ),
                                    )
                                _tracks.value = data.first.tracks
                                _continuation.value = data.second
                                if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                                playlistRepository.insertRadioPlaylist(data.first.toPlaylistEntity())
                            }

                            else -> {
                                _uiState.value = Error(res.message ?: "Empty response")
                            }
                        }
                    }
            } else {
                // This is an online playlist
                playlistRepository
                    .getPlaylistData(id, getString(Res.string.view_count))
                    .collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Playlist data: $data")
                                log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                                _uiState.value =
                                    Success(
                                        data =
                                            PlaylistState(
                                                id = data.first.id,
                                                title = data.first.title,
                                                isRadio = false,
                                                author = data.first.author,
                                                thumbnail =
                                                    data.first.thumbnails
                                                        .lastOrNull()
                                                        ?.url,
                                                description = data.first.description,
                                                trackCount = data.first.trackCount,
                                                year = data.first.year,
                                                shuffleEndpoint = data.first.shuffleEndpoint,
                                                radioEndpoint = data.first.radioEndpoint,
                                            ),
                                    )
                                _tracks.value = data.first.tracks
                                _continuation.value = data.second
                                if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                                getPlaylistEntity(id = data.first.id, playlistBrowse = data.first)
                                maybeAdoptRemoteSaved(data.first.id)
                                refreshRemoteSavedState(data.first.id)
                                refreshFavoriteAvailability(data.first.id, data.first.isOwnYouTubePlaylist)
                            }

                            else -> {
                                getPlaylistEntity(id)
                            }
                        }
                    }
            }
        }
    }

    /**
     * 云盘详情页数据(首页/上传后原地刷新共用):伪歌单 PlaylistState+曲目+分页令牌。
     * 作者行=当前账号昵称(红心歌单同款纯文本语义);描述行放云盘容量。
     * firstLoad=true 时从 Loading 起步(整页 spinner),false 保持已显示内容原地替换。
     */
    private fun getCloudDiskData(firstLoad: Boolean) {
        if (firstLoad) _uiState.value = Loading
        viewModelScope.launch {
            // 发布后服务端有短暂不一致窗口(code=500,实测 1.5s 内必现):非首载刷新
            // 失败时 3s 后重拉一次,仍失败保持旧列表不动(别拿空结果覆盖已显示内容)
            var browse = neteaseRepository.getCloudDiskPage(0).getOrNull()
            if (browse == null && !firstLoad) {
                delay(3000)
                browse = neteaseRepository.getCloudDiskPage(0).getOrNull()
            }
            if (browse == null) {
                if (firstLoad) _uiState.value = Error("cloud disk load failed")
                return@launch
            }
            val nickname =
                neteaseRepository.getNeteaseAccounts().firstOrNull()
                    ?.firstOrNull { it.isUsed }?.nickname
            _uiState.value =
                Success(
                    data =
                        PlaylistState(
                            id = NETEASE_CLOUD_DISK_PLAYLIST_ID,
                            title = getString(Res.string.cloud_disk),
                            isRadio = false,
                            author = Author(id = "", name = nickname ?: "NetEase Cloud"),
                            // 头图=第一首歌的封面(用户 2026-10-09 定案;无封面歌走云朵兜底
                            // painter,调色板自动跟随封面取色)
                            thumbnail = browse.tracks.firstOrNull()?.thumbnails?.lastOrNull()?.url,
                            description = cloudDiskCapacityText(browse),
                            trackCount = browse.totalCount,
                            year = "",
                        ),
                )
            _tracks.value = browse.tracks
            _continuation.value = browse.nextOffset?.let { "$NETEASE_CLOUD_DISK_PAGE_PREFIX$it" }
            // 刷新(上传/删歌后)可能从"到底"回到"有下一页"(如上传后超过 200 首):
            // 有下一页必须显式回 IDLE,否则残留的 EXHAUST 把翻页入口锁死
            _tracksListState.value =
                if (_continuation.value == null) ListState.PAGINATION_EXHAUST else ListState.IDLE
            // 云盘没有"收藏歌单"语义,顶栏心恒藏
            _favoriteAvailable.value = false
        }
    }

    /** 容量描述("已用 X / 共 Y"),缺字段时退化为只显示已用,全缺给空 */
    private fun cloudDiskCapacityText(browse: com.maxrave.data.repository.NeteaseCloudDiskBrowse): String? {
        val used = browse.usedSizeBytes?.let(::formatCloudDiskSize) ?: return null
        val max = browse.maxSizeBytes?.let(::formatCloudDiskSize)
        return getString(
            Res.string.cloud_disk_capacity,
            used,
            max ?: "—",
        )
    }

    private fun formatCloudDiskSize(bytes: Long): String =
        if (bytes >= 1L shl 30) {
            String.format(java.util.Locale.ROOT, "%.1f GB", bytes / 1073741824.0)
        } else {
            String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1048576.0)
        }

    /** 上传在飞标志:同一时间只跑一批,再点直接忽略 */
    private var cloudUploadInFlight = false

    /** 上传队列行(云盘页列表顶部行内进度,2026-10-09 用户定案替代进度弹窗) */
    enum class CloudUploadRowState {
        /** 排队未开始(暂停态显示) */
        PENDING,

        /** 当前正在上传(带字节进度) */
        UPLOADING,

        /** 本首失败(保留行,批次结束刷新后清场) */
        FAILED,
    }

    data class CloudUploadQueueRow(
        val key: String,
        val fileName: String,
        /** 文件内嵌封面(入列时读,整个上传期间显示);null=退云朵兜底 */
        val artwork: androidx.compose.ui.graphics.ImageBitmap? = null,
        val bytesSent: Long = 0,
        val bytesTotal: Long = 0,
        val state: CloudUploadRowState = CloudUploadRowState.PENDING,
    )

    private val _cloudUploadQueue = MutableStateFlow<List<CloudUploadQueueRow>>(emptyList())
    val cloudUploadQueue: StateFlow<List<CloudUploadQueueRow>> = _cloudUploadQueue.asStateFlow()

    /**
     * 批量上传歌曲到云盘(SAF 多选回调进来):选择序整队入列(行内显示排队/上传
     * 进度),逐首串行(读元数据+流式 md5 → 端点五步上传,nos 直传带字节进度);
     * 成功即移行(队列随进度缩水),失败保留行到刷新,全部完成后汇总 toast+原地
     * 刷新列表。元数据从文件名"艺人-标题"启发式解析,只作匹配失败的显示兜底——
     * 服务端转码后按音频指纹匹配曲库并覆盖(探针实证)。
     */
    fun uploadToCloudDisk(uris: List<String>) {
        if (cloudUploadInFlight || uris.isEmpty()) return
        cloudUploadInFlight = true
        viewModelScope.launch {
            // 先整队入列:文件名+内嵌封面轻量读取(只解析头部元数据,md5 等重活留在
            // 逐首上传时做),未开始的行显示排队态
            _cloudUploadQueue.value =
                withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        CloudUploadQueueRow(
                            key = uri,
                            fileName =
                                (readCloudUploadFileName(uri) ?: uri.substringAfterLast('/'))
                                    .substringBeforeLast('.'),
                            artwork = readCloudUploadArtwork(uri),
                        )
                    }
                }
            var ok = 0
            var fail = 0
            for (uri in uris) {
                val meta = withContext(Dispatchers.IO) { readCloudUploadFile(uri) }
                val bodyFactory = cloudUploadBodyFactory(uri)
                if (meta == null || bodyFactory == null) {
                    fail++
                    _cloudUploadQueue.update { rows ->
                        rows.map { if (it.key == uri) it.copy(state = CloudUploadRowState.FAILED) else it }
                    }
                    continue
                }
                _cloudUploadQueue.update { rows ->
                    rows.map {
                        if (it.key == uri) {
                            it.copy(bytesTotal = meta.sizeBytes, state = CloudUploadRowState.UPLOADING)
                        } else {
                            it
                        }
                    }
                }
                val displayName = meta.fileName.substringBeforeLast('.')
                val parsedArtist = displayName.substringBefore('-', "").trim()
                val parsedSong = displayName.substringAfter('-', "").trim()
                val (song, artist) =
                    if (parsedArtist.isNotEmpty() && parsedSong.isNotEmpty()) {
                        parsedSong to parsedArtist
                    } else {
                        displayName to ""
                    }
                // 上传回调在 IO 线程高频触发,按 1% 步进节流再进 StateFlow
                // (update 是 CAS 重试,回调线程直接用安全)
                var lastReported = -1L
                neteaseRepository
                    .uploadToCloudDisk(
                        fileName = meta.fileName,
                        ext = meta.fileName.substringAfterLast('.', "mp3"),
                        sizeBytes = meta.sizeBytes,
                        md5 = meta.md5,
                        song = song,
                        album = "",
                        artist = artist,
                        contentType = meta.mimeType,
                        openBody = bodyFactory,
                        onUpload = { sent, _ ->
                            val pct =
                                if (meta.sizeBytes > 0) (sent * 100 / meta.sizeBytes) else 100
                            if (pct != lastReported) {
                                lastReported = pct
                                _cloudUploadQueue.update { rows ->
                                    rows.map { if (it.key == uri) it.copy(bytesSent = sent) else it }
                                }
                            }
                        },
                    ).fold(
                        onSuccess = {
                            ok++
                            // 成功即移行:队列随进度缩水,行内反馈=逐首完成
                            _cloudUploadQueue.update { rows -> rows.filterNot { it.key == uri } }
                        },
                        onFailure = { e ->
                            fail++
                            _cloudUploadQueue.update { rows ->
                                rows.map {
                                    if (it.key == uri) it.copy(state = CloudUploadRowState.FAILED) else it
                                }
                            }
                            Logger.w(
                                tag,
                                "cloud upload failed: ${meta.fileName} ${e.message}",
                            )
                        },
                    )
            }
            makeToast(
                when {
                    fail == 0 -> getString(Res.string.cloud_upload_all_done, ok)
                    ok == 0 -> getString(Res.string.cloud_upload_all_failed)
                    else -> getString(Res.string.cloud_upload_partial_done, ok, fail)
                },
            )
            // 服务端发布+转码是异步的,稍等再刷(窗口期会 500,刷新内部还会再重试一次)
            delay(2500)
            getCloudDiskData(firstLoad = false)
            // 刷新落定后清场(含失败行——汇总 toast 已报数,新歌由列表刷新呈现)
            _cloudUploadQueue.value = emptyList()
            cloudUploadInFlight = false
        }
    }

    /** 兼容单参调用点(旧单文件路径) */
    fun uploadToCloudDisk(uri: String) = uploadToCloudDisk(listOf(uri))

    /**
     * 云盘批量删除(多选面板入口):逐首 cloud/del——行的 videoId(simpleSong.id)即
     * 可删除 id(2026-10-09 探针验证,匹配后与条目 songId 同值);成功即移行,
     * 全部处理完原地刷新(容量/计数回服务端权威值)。
     */
    fun deleteCloudDiskSongs(videoIds: List<String>) {
        if (videoIds.isEmpty()) return
        viewModelScope.launch {
            var ok = 0
            for (id in videoIds) {
                val songId = id.toLongOrNull() ?: continue
                if (neteaseRepository.deleteCloudDiskSong(songId).getOrDefault(false)) {
                    ok++
                    _tracks.value = _tracks.value.filterNot { it.videoId == id }
                }
            }
            makeToast(
                if (ok == videoIds.size) {
                    getString(Res.string.cloud_delete_done, ok)
                } else {
                    getString(Res.string.cloud_delete_partial, ok, videoIds.size - ok)
                },
            )
            delay(1200)
            getCloudDiskData(firstLoad = false)
        }
    }

    fun getContinuationTrack(
        playlistId: String,
        continuation: String?,
    ) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _tracksListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                _tracksListState.value = ListState.PAGINATING
                songRepository
                    .getContinueTrack(
                        playlistId,
                        continuation,
                        fromPlaylist = true,
                    ).collectLatest { res ->
                        res.first?.forEach { track ->
                            songRepository
                                .insertSong(
                                    track
                                        .toSongEntity()
                                        .copy(
                                            inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                        ),
                                ).singleOrNull()
                                ?.let {
                                    log("Insert song: $it")
                                }
                        }
                        _tracks.update {
                            val newList = it.toMutableList()
                            newList.addAll(res.first ?: emptyList())
                            newList
                        }
                        if (res.second.isNullOrEmpty()) {
                            _continuation.value = null
                            _tracksListState.value = ListState.PAGINATION_EXHAUST
                        } else {
                            _continuation.value = res.second
                            _tracksListState.value = ListState.IDLE
                        }
                    }
            }
        }
    }

    private fun getPlaylistEntity(
        id: String,
        playlistBrowse: PlaylistBrowse? = null,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch {
                val playlistEntity = playlistRepository.getPlaylist(id).firstOrNull()
                if (playlistBrowse != null) {
                    if (playlistEntity == null) {
                        playlistRepository.insertAndReplacePlaylist(
                            playlistBrowse.toPlaylistEntity(),
                        )
                        delay(500)
                        playlistRepository.getPlaylist(id).collectLatest { playlist ->
                            _playlistEntity.value = playlist
                            playlistRepository.updatePlaylistInLibrary(
                                playlistId = id,
                                inLibrary = now(),
                            )
                        }
                    } else {
                        _playlistEntity.value = playlistEntity
                        playlistRepository.updatePlaylistInLibrary(
                            playlistId = id,
                            inLibrary = now(),
                        )
                    }
                    playlistBrowse.tracks.forEach { tracks ->
                        songRepository
                            .insertSong(
                                tracks.toSongEntity().copy(
                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                ),
                            ).firstOrNull()
                            ?.let {
                                log("Insert song: $it")
                            }
                    }
                } else if (playlistEntity != null) {
                    _playlistEntity.value = playlistEntity
                    playlistRepository.updatePlaylistInLibrary(
                        playlistId = id,
                        inLibrary = now(),
                    )
                    // 网络解析失败走 Room 兜底渲染(深链/弱网常见路径),门的判定同样要跑
                    refreshFavoriteAvailability(id, false)
                    _uiState.value =
                        Success(
                            data =
                                PlaylistState(
                                    id = playlistEntity.id,
                                    title = playlistEntity.title,
                                    isRadio = false,
                                    author =
                                        Author(
                                            id = "",
                                            name = playlistEntity.author ?: "",
                                        ),
                                    thumbnail = playlistEntity.thumbnails,
                                    description = playlistEntity.description,
                                    trackCount = playlistEntity.trackCount,
                                    year = playlistEntity.year ?: now().year.toString(),
                                ),
                        )
                    _tracksListState.value = ListState.LOADING
                    playlistEntity.tracks?.let {
                        songRepository
                            .getSongsByListVideoId(it)
                            .singleOrNull()
                            ?.let { song ->
                                _tracks.value = song.map { it.toTrack() }
                            }
                    }
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                    if (playlistEntity.downloadState != STATE_DOWNLOADED) {
                        checkDownloadedPlaylist =
                            launch {
                                val listSong =
                                    songRepository
                                        .getSongsByListVideoId(
                                            playlistEntity.tracks ?: emptyList(),
                                        ).firstOrNull()
                                Logger.d(tag, "List song: $listSong")
                                if (!listSong.isNullOrEmpty() && listSong.size == playlistEntity.tracks?.size &&
                                    listSong.all {
                                        it.downloadState == STATE_DOWNLOADED
                                    }
                                ) {
                                    updatePlaylistDownloadState(playlistEntity.id, STATE_DOWNLOADED)
                                }
                            }
                    }
                } else {
                    _uiState.value = Error("Empty response")
                }
            }
    }

    fun setBrush(listColors: List<Color>) {
        _listColors.value = listColors
    }

    fun updatePlaylistLiked(
        liked: Boolean,
        id: String,
    ) {
        // 收藏心=云端账号状态:直接转发云端切换,本地行随结果镜像
        setRemoteSaved(liked)
    }

    fun onUIEvent(event: PlaylistUIEvent) {
        val data = uiState.value.data ?: return
        when (event) {
            is PlaylistUIEvent.ItemClick -> {
                val videoId = event.videoId
                val loadedList = tracks.value
                val clickedSong = loadedList.first { it.videoId == videoId }
                val index = loadedList.indexOf(clickedSong)
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = "${
                            getString(
                                Res.string.playlist,
                            )
                        } \"${data.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    index,
                )
            }

            PlaylistUIEvent.PlayAll -> {
                val loadedList = tracks.value
                if (loadedList.isEmpty()) {
                    makeToast(
                        getString(Res.string.playlist_is_empty),
                    )
                    return
                }
                val clickedSong = loadedList.first()
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = "${
                            getString(
                                Res.string.playlist,
                            )
                        } \"${data.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }

            PlaylistUIEvent.Shuffle -> {
                val shuffleEndpoint = data.shuffleEndpoint
                // 网易歌单/云盘没有 YT 的 shuffleEndpoint:本地洗牌已加载曲目直接起播
                // (与艺人页随机播放同款,35c997bd);分页未拉完先洗已加载部分,
                // 后续仍走队列 continuation 续拉
                if (shuffleEndpoint == null &&
                    (data.id.toLongOrNull() != null || data.id == NETEASE_CLOUD_DISK_PLAYLIST_ID)
                ) {
                    val loaded = tracks.value
                    if (loaded.isEmpty()) {
                        makeToast(getString(Res.string.playlist_is_empty))
                        return
                    }
                    val shuffled = loaded.shuffled()
                    setQueueData(
                        QueueData.Data(
                            listTracks = shuffled.toCollection(arrayListOf<Track>()),
                            firstPlayedTrack = shuffled.first(),
                            playlistId = data.id,
                            playlistName = "\"${data.title}\" ${getString(Res.string.shuffle)}",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = continuation.value,
                        ),
                    )
                    loadMediaItem(shuffled.first(), Config.PLAYLIST_CLICK, 0)
                    return
                }
                if (shuffleEndpoint == null) {
                    makeToast(
                        getString(Res.string.shuffle_not_available),
                    )
                    return
                } else {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(shuffleEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Shuffle data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = shuffleEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(Res.string.shuffle)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(
                                        res.message ?: getString(Res.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.StartRadio -> {
                val radioEndpoint = data.radioEndpoint
                if (radioEndpoint == null) {
                    makeToast(
                        getString(Res.string.radio_not_available),
                    )
                    return
                } else {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(radioEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Radio data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = radioEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(Res.string.radio)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(
                                        res.message ?: getString(Res.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.Download -> {
                downloadFullPlaylist()
            }

            PlaylistUIEvent.Favorite -> {
                updatePlaylistLiked(!liked.value, data.id)
            }
        }
    }

    /**
     * 拉全曲目(整单下载/添加到队列/搜索前展开):分页未到底时循环续拉直到
     * [ListState.PAGINATION_EXHAUST]。回调第二参 complete=false = 中途续页失败
     * 放弃(云盘续页失败保留可重试状态回 IDLE,不再被当"到底"),拿到的是部分
     * 列表——调用方必须提示用户缺页,不能把部分列表当全量。
     */
    fun getFullTracks(callback: (List<Track>, Boolean) -> Unit) {
        viewModelScope.launch {
            if (tracksListState.value == ListState.PAGINATION_EXHAUST) {
                _playlistEntity.value
                    ?.copy(
                        tracks = tracks.value.toListVideoId(),
                        trackCount = tracks.value.size,
                    )?.let {
                        playlistRepository.insertAndReplacePlaylist(it)
                    }
                callback(tracks.value, true)
            } else {
                val id = uiState.value.data?.id ?: return@launch
                // 云盘续页失败回 IDLE 且令牌保留,直接循环会打死循环:按"曲目数无
                // 增长"计停,连续 2 个无进展页放弃,按部分加载回调。lastSize=-1 起
                // 步让首个 IDLE(尚未拉过任何页)不计停
                var finished = false
                var lastSize = -1
                var stalled = 0
                tracksListState.takeWhile { !finished }.collectLatest { state ->
                    when (state) {
                        ListState.PAGINATION_EXHAUST -> {
                            finished = true
                            _playlistEntity.value
                                ?.copy(
                                    tracks = tracks.value.toListVideoId(),
                                    trackCount = tracks.value.size,
                                )?.let {
                                    playlistRepository.insertAndReplacePlaylist(it)
                                }
                            callback(tracks.value, true)
                        }

                        ListState.PAGINATING -> Unit

                        else -> {
                            if (tracks.value.size == lastSize) stalled++ else stalled = 0
                            lastSize = tracks.value.size
                            if (stalled >= 2) {
                                finished = true
                                callback(tracks.value, false)
                            } else {
                                getContinuationTrack(id, continuation.value)
                            }
                        }
                    }
                }
            }
        }
    }

    fun downloadFullPlaylist() {
        viewModelScope.launch {
            // 云盘伪歌单没有 Room 歌单行:哨兵 id 顶上,仅用于批量下载流转;
            // 整单下载态回写(STATE_DOWNLOADING/NOT_DOWNLOADED)对云盘跳过,
            // 不往 playlist 表塞哨兵行(库页/Room 兜底分支会读到幽灵行)
            val isCloudDisk = uiState.value.data?.id == NETEASE_CLOUD_DISK_PLAYLIST_ID
            val id =
                playlistEntity.value?.id
                    ?: if (isCloudDisk) NETEASE_CLOUD_DISK_PLAYLIST_ID else return@launch
            getFullTracks { tracks, complete ->
                viewModelScope.launch {
                    // 部分加载(续页失败放弃):不能把部分列表当全量,先告知缺页再继续
                    if (!complete) {
                        makeToast(getString(Res.string.partially_loaded, tracks.size.toString()))
                    }
                    // 三选分类(2026-09-30 定稿):在途跳过;文件式已下载→弹窗;其余直接入队
                    val songs =
                        songRepository.getSongsByListVideoId(tracks.toListVideoId()).firstOrNull()
                            ?: emptyList()
                    val byId = songs.associateBy { it.videoId }
                    val notDownloaded = mutableListOf<BatchDownloadSong>()
                    val downloaded = mutableListOf<BatchDownloadSong>()
                    tracks.forEach { track ->
                        val meta =
                            BatchDownloadSong(
                                videoId = track.videoId,
                                title = track.title,
                                thumbnail = track.thumbnails?.lastOrNull()?.url ?: "",
                            )
                        val row = byId[track.videoId]
                        val inFlight =
                            row?.downloadState == STATE_PREPARING ||
                                row?.downloadState == STATE_DOWNLOADING ||
                                downloadUtils.isAudioQueuedOrDownloading(track.videoId)
                        val fileOnDisk = row?.downloadedFilePath?.let { java.io.File(it).exists() } == true
                        when {
                            inFlight -> Unit
                            fileOnDisk -> downloaded += meta
                            else -> notDownloaded += meta
                        }
                    }
                    if (downloaded.isEmpty()) {
                        if (notDownloaded.isNotEmpty()) {
                            if (!isCloudDisk) updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                            if (!queueBatchDownload(notDownloaded) && !isCloudDisk) {
                                updatePlaylistDownloadState(id, STATE_NOT_DOWNLOADED)
                            }
                        } else {
                            makeToast(getString(Res.string.downloaded))
                        }
                    } else {
                        _batchDownloadRequest.value = BatchDownloadRequest(notDownloaded, downloaded)
                    }
                }
            }
        }
    }

    private val _batchDownloadRequest = MutableStateFlow<BatchDownloadRequest?>(null)
    val batchDownloadRequest: StateFlow<BatchDownloadRequest?> = _batchDownloadRequest.asStateFlow()

    fun dismissBatchDownload() {
        _batchDownloadRequest.value = null
    }

    fun confirmBatchDownload(overwrite: Boolean) {
        val request = _batchDownloadRequest.value ?: return
        _batchDownloadRequest.value = null
        viewModelScope.launch {
            // 云盘伪歌单没有 Room 歌单行(downloadFullPlaylist 同款):哨兵 id 顶上,
            // 整单下载态回写对云盘跳过——否则"部分已下载→确认跳过/覆盖"会在这里
            // 因 playlistEntity 为 null 直接 return,剩余歌曲永远入不了队
            val isCloudDisk = uiState.value.data?.id == NETEASE_CLOUD_DISK_PLAYLIST_ID
            val id =
                playlistEntity.value?.id
                    ?: if (isCloudDisk) NETEASE_CLOUD_DISK_PLAYLIST_ID else return@launch
            val target =
                if (overwrite) {
                    // 覆盖:降级引用容器防 watcher 重排队,再删旧文件重新入队
                    request.downloaded.forEach { song ->
                        demoteDownloadedContainers(
                            videoId = song.videoId,
                            playlistRepository = playlistRepository,
                            albumRepository = albumRepository,
                            localPlaylistRepository = localPlaylistRepository,
                        )
                        downloadUtils.removeAudioDownload(song.videoId)
                    }
                    request.notDownloaded + request.downloaded
                } else {
                    request.notDownloaded
                }
            if (target.isEmpty()) {
                // 跳过且无可入队项=文件都在:直接落"已下载"。置"下载中"的话,文件式
                // 条目转存完即从 DownloadIndex 清除,完成事件永远不来,按钮永久卡死
                // (用户 2026-10-02 实测:整单下完→点下载→跳过→一直"下载中")。
                // 云盘没有按钮态可翻,toast 一句作为反馈
                if (isCloudDisk) {
                    makeToast(getString(Res.string.downloaded))
                } else {
                    updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                }
                return@launch
            }
            if (!isCloudDisk) updatePlaylistDownloadState(id, STATE_DOWNLOADING)
            if (!queueBatchDownload(target) && !isCloudDisk) {
                // 全部被磁盘预检拒绝=没有任何任务能纠正容器态,回滚(否则永久"下载中",CR P1-3)
                updatePlaylistDownloadState(id, STATE_DOWNLOADED)
            }
        }
    }

    /** 返回 true=有至少一首成功入队;全部被磁盘预检拒绝时 false(调用点回滚容器态) */
    private suspend fun queueBatchDownload(songs: List<BatchDownloadSong>): Boolean {
        var anyRejected = false
        var anyQueued = false
        songs.forEach { song ->
            songRepository.updateDownloadState(
                videoId = song.videoId,
                downloadState = STATE_PREPARING,
            )
            val queued =
                downloadUtils.downloadTrack(
                    videoId = song.videoId,
                    title = song.title,
                    thumbnail = song.thumbnail,
                )
            if (!queued) {
                // 磁盘预检拒绝:回滚占位态(CR P1-3——不回滚会永久停在"准备下载")
                songRepository.updateDownloadState(
                    videoId = song.videoId,
                    downloadState = STATE_NOT_DOWNLOADED,
                )
                anyRejected = true
            } else {
                anyQueued = true
            }
        }
        if (songs.isNotEmpty()) {
            makeToast(getString(if (anyRejected) Res.string.download_no_space else Res.string.downloading))
        }
        return anyQueued
    }

    /**
     * Mirror of [downloadFullPlaylist]: reset the playlist's state first so the init-block watcher
     * (which re-downloads missing tracks while the playlist claims to be downloaded) cancels,
     * then drop every track's DownloadManager entry and DB state.
     */
    fun removeDownloadedPlaylist() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            updatePlaylistDownloadState(id, STATE_NOT_DOWNLOADED)
            // Same beat as LocalPlaylistViewModel: let the NOT_DOWNLOADED flip reach the init
            // watcher (which would otherwise re-queue missing tracks) before rows change.
            delay(500)
            getFullTracks { tracks, _ ->
                // Only songs no other downloaded container references are actually removed.
                viewModelScope.launch {
                    removeExclusiveTrackDownloads(
                        tracks = tracks.toListVideoId(),
                        songRepository = songRepository,
                        downloadUtils = downloadUtils,
                        playlistRepository = playlistRepository,
                        albumRepository = albumRepository,
                        localPlaylistRepository = localPlaylistRepository,
                    )
                }
            }
            makeToast(getString(Res.string.removed_download))
        }
    }

    /**
     * Stop an in-flight full-playlist download from the header button: reset the playlist first
     * (kills the init watcher), then cancel only tracks still preparing/downloading — songs that
     * already finished keep their files, so resuming later only fetches the gap.
     */
    fun cancelDownloadingPlaylist() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            updatePlaylistDownloadState(id, STATE_NOT_DOWNLOADED)
            delay(500)
            getFullTracks { tracks, _ ->
                viewModelScope.launch {
                    songRepository.getSongsByListVideoId(tracks.toListVideoId()).firstOrNull()?.forEach { song ->
                        if (song.downloadState == STATE_PREPARING || song.downloadState == STATE_DOWNLOADING) {
                            downloadUtils.removeDownload(song.videoId)
                            songRepository.updateDownloadState(song.videoId, STATE_NOT_DOWNLOADED)
                        }
                    }
                }
            }
            makeToast(getString(Res.string.download_cancelled))
        }
    }

    fun updatePlaylistTitle(
        title: String,
        id: String,
    ) {
        viewModelScope.launch {
            playlistRepository
                .updateYourYouTubePlaylistTitle(
                    id,
                    title,
                ).collect {
                    when (it) {
                        is Resource.Success -> {
                            getData(id)
                            makeToast(it.data ?: getString(Res.string.synced))
                        }

                        is Resource.Error -> {
                            makeToast(it.message ?: getString(Res.string.error))
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectDownloadedJob?.cancel()
        playlistEntityJob?.cancel()
    }
}

sealed class PlaylistUIState(
    val data: PlaylistState? = null,
    val message: String? = null,
) {
    data object Loading : PlaylistUIState()

    class Success(
        data: PlaylistState,
    ) : PlaylistUIState(
            data = data,
        )

    class Error(
        message: String? = null,
    ) : PlaylistUIState(
            message = message,
        )
}

sealed class PlaylistUIEvent {
    data object PlayAll : PlaylistUIEvent()

    data object Shuffle : PlaylistUIEvent()

    data object StartRadio : PlaylistUIEvent()

    data class ItemClick(
        val videoId: String,
    ) : PlaylistUIEvent()

    data object Favorite : PlaylistUIEvent()

    data object Download : PlaylistUIEvent()
}

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    PAGINATION_EXHAUST,
}
