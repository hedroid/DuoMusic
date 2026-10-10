package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
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
import com.maxrave.common.Config
import com.maxrave.common.NETEASE_MV_MEDIA_ID_PREFIX
import com.maxrave.common.NETEASE_RADIO_SEARCH_RESULT_TYPE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.logger.Logger
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.source.MusicSource
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.MoodCategoryCard
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.toAppDeepLinkOrNull
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistFullWidthItems
import com.maxrave.simpmusic.ui.component.ShimmerSearchItem
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.ArrowOutward
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NeteaseTagDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.NeteaseRadioDetailDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SearchScreenUIState
import com.maxrave.simpmusic.viewModel.SearchType
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.toStringRes
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.clear_search_history
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.everything_you_need
import simpmusic.composeapp.generated.resources.artist_page_coming_soon
import simpmusic.composeapp.generated.resources.hot_search
import simpmusic.composeapp.generated.resources.in_search
import simpmusic.composeapp.generated.resources.no_results_found
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.partial_categories_failed
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.search_for
import simpmusic.composeapp.generated.resources.search_for_songs_artists_albums_playlists_and_more
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.videos
import simpmusic.composeapp.generated.resources.what_do_you_want_to_listen_to

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchScreenState by searchViewModel.searchScreenState.collectAsStateWithLifecycle()
    val uiState by searchViewModel.searchScreenUIState.collectAsStateWithLifecycle()
    val searchHistory by searchViewModel.searchHistory.collectAsStateWithLifecycle()
    val moodAndGenres by searchViewModel.moodAndGenres.collectAsStateWithLifecycle()
    val moodArtwork by searchViewModel.moodArtwork.collectAsStateWithLifecycle()
    val hotSearch by searchViewModel.hotSearch.collectAsStateWithLifecycle()
    val selectedSource by sharedViewModel.selectedSource.collectAsStateWithLifecycle()
    val isNeteaseSource = selectedSource == MusicSource.NETEASE.name

    // 网易源下只保留有对应能力的 tab;若停在已隐藏的 tab(换源后 VM single 保留旧状态)回落 ALL
    val visibleSearchTabs = if (isNeteaseSource) NETEASE_SEARCH_TABS else SearchType.entries

    var searchUIType by rememberSaveable { mutableStateOf(SearchUIType.EMPTY) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchSubmitted by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var isFocused by rememberSaveable { mutableStateOf(false) }

 // 切源原地刷新(2026-09-26 修复):tab 回落 + 已在结果页时按新源重跑当前 tab 的搜索。
    // 原实现只回落 tab 从不重搜——切源后结果页永远停留旧源内容(SEARCHPROBE 实测:
    // effect 正常触发但无任何 searchXxx 调用,网易结果在 YTM 源下停留 5s+ 不动)。
    LaunchedEffect(isNeteaseSource) {
        Logger.w(
            "SEARCHPROBE",
            "source effect fired: isNetease=$isNeteaseSource tab=${searchScreenState.searchType} uiType=$searchUIType",
        )
        val effectiveTab =
            if (isNeteaseSource && searchScreenState.searchType !in NETEASE_SEARCH_TABS) {
                searchViewModel.setSearchType(SearchType.ALL)
                SearchType.ALL
            } else {
                searchScreenState.searchType
            }
        if (searchText.isNotEmpty() && searchUIType == SearchUIType.SEARCH_RESULTS) {
            Logger.w("SEARCHPROBE", "re-dispatch '$searchText' on source switch, tab=$effectiveTab")
            when (effectiveTab) {
                SearchType.ALL -> searchViewModel.searchAll(searchText)
                SearchType.SONGS -> searchViewModel.searchSongs(searchText)
                SearchType.VIDEOS -> searchViewModel.searchVideos(searchText)
                SearchType.ALBUMS -> searchViewModel.searchAlbums(searchText)
                SearchType.ARTISTS -> searchViewModel.searchArtists(searchText)
                SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(searchText)
                SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(searchText)
                SearchType.PODCASTS -> searchViewModel.searchPodcast(searchText)
            }
        }
    }

   val isSearchPanelVisible =
        searchUIType == SearchUIType.SEARCH_HISTORY || searchUIType == SearchUIType.SEARCH_SUGGESTIONS
    val dismissSearchPanel: () -> Unit = {
        isExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    PlatformBackHandler(enabled = isSearchPanelVisible, onBack = dismissSearchPanel)
  // The bar floats OVER the content (a Box, not a Column) so there is something behind it to
    // blur — same arrangement HomeScreen uses. Each branch owns a scroll state, hoisted here so
    // the bar can tell whether the branch currently on screen is scrolled away from the top.
    // Two columns only on a phone held upright. Anywhere wider — tablet, landscape, desktop — two
    // columns stretch each tile to half the window, and since the tile keeps a 2:1 ratio it grows
    // absurdly tall with it.
    val screenInfo = getScreenSizeInfo()
    val isMobilePortrait = getPlatform() == Platform.Android && screenInfo.wDP < screenInfo.hDP
    val moodGridColumns = if (isMobilePortrait) 2 else 4

    val hazeState = rememberHazeState()
    val suggestionsState = rememberLazyListState()
    val historyState = rememberLazyListState()
    val moodGridState = rememberLazyGridState()
    val resultsState = rememberLazyListState()
    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    val searchBarHeight = with(LocalDensity.current) { searchBarHeightPx.toDp() }
    val isContentAtTop by remember {
        derivedStateOf {
            when (searchUIType) {
                SearchUIType.EMPTY ->
                    moodGridState.firstVisibleItemIndex == 0 && moodGridState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_HISTORY ->
                    historyState.firstVisibleItemIndex == 0 && historyState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_SUGGESTIONS ->
                    suggestionsState.firstVisibleItemIndex == 0 && suggestionsState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_RESULTS ->
                    resultsState.firstVisibleItemIndex == 0 && resultsState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    val searchForString = stringResource(Res.string.search_for)
    val songString = stringResource(Res.string.song).lowercase()
    val artistString = stringResource(Res.string.artists).lowercase()
    val albumString = stringResource(Res.string.albums).lowercase()
    val playlistString = stringResource(Res.string.playlists).lowercase()
    val videoString = stringResource(Res.string.videos).lowercase()
    val podcastString = stringResource(Res.string.podcasts).lowercase()

    // Animated Placeholder——双源 tab 集已对齐(网易视频/播客/专辑搜索均接入),占位词同单
    val placeholderTexts =
        listOf(
            "$searchForString $songString...",
            "$searchForString $artistString...",
            "$searchForString $albumString...",
            "$searchForString $playlistString...",
            "$searchForString $videoString...",
            "$searchForString $podcastString...",
        )

    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }

    // Animate placeholder - pause when focused
    LaunchedEffect(isFocused) {
        while (!isFocused) {
            delay(3000) // Change every 3 seconds
            currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholderTexts.size
        }
    }

    var sheetSong by remember { mutableStateOf<SongEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    val allSelectedDownloaded by selectionViewModel.allSelectedDownloaded.collectAsStateWithLifecycle()
    LaunchedEffect(showSelectionSheet) {
        if (showSelectionSheet) selectionViewModel.checkAllDownloaded(selectionState.selected.toList())
    }
    val currentVideoId by searchViewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val chipRowState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()

    val onMoreClick: (SongEntity) -> Unit = { song ->
        sheetSong = song
        showBottomSheet = true
    }

    /** 按当前 tab 提交一次搜索(热搜词/建议词点击入口共用) */
    val submitSearch: (String) -> Unit = { query ->
        searchText = query
        focusManager.clearFocus()
        isSearchSubmitted = true
        searchViewModel.insertSearchHistory(query)
        when (searchScreenState.searchType) {
            SearchType.ALL -> searchViewModel.searchAll(query)
            SearchType.SONGS -> searchViewModel.searchSongs(query)
            SearchType.VIDEOS -> searchViewModel.searchVideos(query)
            SearchType.ALBUMS -> searchViewModel.searchAlbums(query)
            SearchType.ARTISTS -> searchViewModel.searchArtists(query)
            SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(query)
            SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(query)
            SearchType.PODCASTS -> searchViewModel.searchPodcast(query)
        }
    }

    /** 按当前 tab 重跑一次搜索(不动焦点/历史):错误态重试与 ALL 聚合失败分类的空 tab 重试用 */
    val retryCurrentTabSearch: () -> Unit = {
        if (searchText.isNotEmpty()) {
            when (searchScreenState.searchType) {
                SearchType.ALL -> searchViewModel.searchAll(searchText)
                SearchType.SONGS -> searchViewModel.searchSongs(searchText)
                SearchType.VIDEOS -> searchViewModel.searchVideos(searchText)
                SearchType.ALBUMS -> searchViewModel.searchAlbums(searchText)
                SearchType.ARTISTS -> searchViewModel.searchArtists(searchText)
                SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(searchText)
                SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(searchText)
                SearchType.PODCASTS -> searchViewModel.searchPodcast(searchText)
            }
        }
    }

    LaunchedEffect(searchText) {
        if (isFocused) {
            isSearchSubmitted = false
            isExpanded = true
        }
        if (searchText.isNotEmpty() && isFocused) {
            searchViewModel.suggestQuery(searchText)
        }
    }

    LaunchedEffect(isSearchSubmitted) {
        if (isSearchSubmitted) {
            isExpanded = false
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            isExpanded = true
        }
    }

    LaunchedEffect(isExpanded, searchText, isFocused) {
        searchUIType =
            if (searchText.isNotEmpty() && isExpanded) {
                SearchUIType.SEARCH_SUGGESTIONS
            } else if (isFocused && isExpanded) {
                SearchUIType.SEARCH_HISTORY
            } else if (searchText.isEmpty()) {
                SearchUIType.EMPTY
            } else {
                SearchUIType.SEARCH_RESULTS
            }
    }

    //On search icon click while on search screen, open keyboard. Android only feature
    if (getPlatform() == Platform.Android) {
        val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
        LaunchedEffect(reloadDestination) {
            if (reloadDestination == SearchDestination::class) {
                if (!selectionState.isActive && searchUIType == SearchUIType.EMPTY) {
                    isExpanded = true
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                sharedViewModel.reloadDestinationDone()
            }
        }
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
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                sheetSong = null
            },
            navController = navController,
            song = sheetSong,
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Transparent),
    ) {
        // Content scrolls under the bar (it is the haze source), so it needs top padding
        // equal to the bar's measured height to keep its first item clear of it.
        Crossfade(
            targetState = searchUIType,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        ) {
            when (it) {
                SearchUIType.SEARCH_SUGGESTIONS -> {
                    LazyColumn(
                        Modifier.padding(horizontal = 16.dp),
                        state = suggestionsState,
                        contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp),
                    ) {
                        items(searchScreenState.suggestYTItems) { item ->
                            SuggestItemRow(
                                searchResult = item,
                                onItemClick = { item ->
                                    when (item) {
                                        is SongsResult, is VideosResult -> {
                                            val firstTrack: Track = (item as? SongsResult)?.toTrack() ?: (item as VideosResult).toTrack()
                                            searchViewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(firstTrack),
                                                    firstPlayedTrack = firstTrack,
                                                    playlistId = "RDAMVM${firstTrack.videoId}",
                                                    playlistName = "\"${searchText}\" ${getStringBlocking(Res.string.in_search)}",
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            searchViewModel.loadMediaItem(firstTrack, type = Config.SONG_CLICK)
                                        }

                                        is ArtistsResult -> {
                                            navController.navigate(
                                                ArtistDestination(item.browseId),
                                            )
                                        }

                                        is AlbumsResult -> {
                                            navController.navigate(
                                                AlbumDestination(item.browseId),
                                            )
                                        }

                                        is PlaylistsResult -> {
                                            navController.navigate(
                                                PlaylistDestination(
                                                    item.browseId,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        items(searchScreenState.suggestQueries) { suggestion ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(),
                                            onClick = {
                                                searchText = suggestion
                                                focusManager.clearFocus()
                                                isSearchSubmitted = true
                                                searchViewModel.insertSearchHistory(suggestion)
                                                when (searchScreenState.searchType) {
                                                    SearchType.ALL -> searchViewModel.searchAll(suggestion)
                                                    SearchType.SONGS -> searchViewModel.searchSongs(suggestion)
                                                    SearchType.VIDEOS -> searchViewModel.searchVideos(suggestion)
                                                    SearchType.ALBUMS -> searchViewModel.searchAlbums(suggestion)
                                                    SearchType.ARTISTS -> searchViewModel.searchArtists(suggestion)
                                                    SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(suggestion)
                                                    SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(suggestion)
                                                    SearchType.PODCASTS -> searchViewModel.searchPodcast(suggestion)
                                                }
                                            },
                                        ).padding(horizontal = 12.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = suggestion,
                                    style = typo().bodyMedium,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        searchText = suggestion
                                        focusRequester.requestFocus()
                                    },
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.ArrowOutward,
                                        contentDescription = "Search suggestion",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                        item {
                            EndOfPage(
                                withoutCredit = true,
                            )
                        }
                    }
                }

                SearchUIType.SEARCH_HISTORY -> {
                    // Search history state
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                    ) {
                        LazyColumn(
                            state = historyState,
                            contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp),
                        ) {
                            stickyHeader {
                                Crossfade(
                                    targetState = searchHistory.isNotEmpty(),
                                ) {
                                    if (it) {
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background),
                                        ) {
                                            TextButton(
                                                onClick = { searchViewModel.deleteSearchHistory() },
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.clear_search_history),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            items(searchHistory) { historyItem ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchText = historyItem
                                                focusManager.clearFocus()
                                                isSearchSubmitted = true
                                                searchViewModel.insertSearchHistory(historyItem)
                                                when (searchScreenState.searchType) {
                                                    SearchType.ALL -> searchViewModel.searchAll(historyItem)
                                                    SearchType.SONGS -> searchViewModel.searchSongs(historyItem)
                                                    SearchType.VIDEOS -> searchViewModel.searchVideos(historyItem)
                                                    SearchType.ALBUMS -> searchViewModel.searchAlbums(historyItem)
                                                    SearchType.ARTISTS -> searchViewModel.searchArtists(historyItem)
                                                    SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(historyItem)
                                                    SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(historyItem)
                                                    SearchType.PODCASTS -> searchViewModel.searchPodcast(historyItem)
                                                }
                                            }.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.History,
                                        contentDescription = "Search history",
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                                    Text(
                                        text = historyItem,
                                        style = typo().bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            searchText = historyItem
                                            focusRequester.requestFocus()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowOutward,
                                            contentDescription = "Search suggestion",
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                            item {
                                EndOfPage(
                                    withoutCredit = true,
                                )
                            }
                        }
                    }
                }

                SearchUIType.EMPTY -> {
                    val mood = moodAndGenres
                    if (mood == null) {
                        // First run only: the repository serves its cached copy before hitting the
                        // network, so this spinner is never seen again after the first fetch.
                        CenterLoadingBox(Modifier.fillMaxSize())
                    } else {
                        // Capped and centred: on a wide desktop window the grid would otherwise
                        // span the whole width, stretching four tiles into long bars. 1100.dp
                        // keeps a tile near 250.dp, which is its natural size.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(moodGridColumns),
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 1100.dp)
                                    .padding(horizontal = 16.dp),
                            state = moodGridState,
                            contentPadding = PaddingValues(top = searchBarHeight),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            // Breathing room on both sides of this block: above it
                                            // sits the floating search bar, below it the tile grid.
                                            .padding(top = 36.dp, bottom = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.everything_you_need),
                                        style = typo().titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(Res.string.search_for_songs_artists_albums_playlists_and_more),
                                        style = typo().bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            // 网易源空态特有:热搜词榜(点词即搜),置顶在分类网格前
                            if (isNeteaseSource && hotSearch.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.hot_search),
                                            style = typo().titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            hotSearch.take(10).forEachIndexed { index, hot ->
                                                Chip(
                                                    text = "${index + 1}  ${hot.word}",
                                                    onClick = { submitSearch(hot.word) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            mood.sections.forEachIndexed { index, section ->
                                // First section runs straight on from the header block above it,
                                // so its own heading would just be a second title in a row.
                                if (index > 0) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(
                                            // Section titles come from YouTube already localised,
                                            // so there is no string resource to pick here.
                                            text = section.title,
                                            style = typo().titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                                // Key must include the section: every section lives in this ONE
                                // grid, and "For you" repeats categories that also appear under
                                // Moods or Genres, so params alone collides.
                                items(section.items, key = { "${section.title}/${it.params}" }) { item ->
                                    // LazyVerticalGrid only composes tiles inside the viewport, so
                                    // putting the request here IS the laziness — a category the
                                    // user never scrolls to never costs a browse.
                                    LaunchedEffect(item.params) {
                                        searchViewModel.loadMoodArtwork(item.params)
                                    }
                                    MoodCategoryCard(
                                        title = item.title,
                                        artworkUrl = moodArtwork[item.params],
                                        source = if (isNeteaseSource) MusicSource.NETEASE else MusicSource.YOUTUBE_MUSIC,
                                    ) {
                                        if (isNeteaseSource) {
                                            // 分类卡点击与主页同源同页:进 NeteaseTagScreen(两列网格),
                                            // 不走 YT 的 MoodScreen
                                            navController.navigate(NeteaseTagDestination(item.params))
                                        } else {
                                            navController.navigate(MoodDestination(item.params))
                                        }
                                    }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EndOfPage()
                            }
                        }
                        }
                    }
                }

                SearchUIType.SEARCH_RESULTS -> {
                    // Content area — chips now live in the blurred bar block above.
                    Column(modifier = Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            onRefresh = {
                                val query = searchText.trim()
                                if (query.isNotEmpty()) {
                                    isSearchSubmitted = true
                                    searchViewModel.insertSearchHistory(query)
                                    when (searchScreenState.searchType) {
                                        SearchType.ALL -> searchViewModel.searchAll(query)
                                        SearchType.SONGS -> searchViewModel.searchSongs(query)
                                        SearchType.VIDEOS -> searchViewModel.searchVideos(query)
                                        SearchType.ALBUMS -> searchViewModel.searchAlbums(query)
                                        SearchType.ARTISTS -> searchViewModel.searchArtists(query)
                                        SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(query)
                                        SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(query)
                                        SearchType.PODCASTS -> searchViewModel.searchPodcast(query)
                                    }
                                }
                            },
                            isRefreshing = uiState is SearchScreenUIState.Loading,
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullToRefreshState,
                                    isRefreshing = uiState is SearchScreenUIState.Loading,
                                    // Anchored to the top of the box, which now starts under the
                                    // bar — without this offset the spinner sits behind the bar
                                    // and only its top sliver shows.
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = searchBarHeight),
                                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                                    color = PullToRefreshDefaults.indicatorColor,
                                    maxDistance = PullToRefreshDefaults.PositionalThreshold - 5.dp,
                                )
                            },
                        ) {
                            Crossfade(targetState = uiState) { uiState ->
                                when (uiState) {
                                    is SearchScreenUIState.Loading -> {
                                        // Loading state — same top inset as the results list, or
                                        // the first shimmer row hides behind the bar and chips.
                                        LazyColumn(
                                            contentPadding =
                                                PaddingValues(
                                                    top = searchBarHeight,
                                                    bottom = 10.dp,
                                                ),
                                        ) {
                                            items(10) {
                                                ShimmerSearchItem()
                                            }
                                        }
                                    }

                                    is SearchScreenUIState.Success -> {
                                        // Success state with results
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Search Results List
                                            val currentResults =
                                                when (searchScreenState.searchType) {
                                                    SearchType.ALL -> searchScreenState.searchAllResult
                                                    SearchType.SONGS -> searchScreenState.searchSongsResult
                                                    SearchType.VIDEOS -> searchScreenState.searchVideosResult
                                                    SearchType.ALBUMS -> searchScreenState.searchAlbumsResult
                                                    SearchType.ARTISTS -> searchScreenState.searchArtistsResult
                                                    SearchType.PLAYLISTS -> searchScreenState.searchPlaylistsResult
                                                    SearchType.FEATURED_PLAYLISTS -> searchScreenState.searchFeaturedPlaylistsResult
                                                    SearchType.PODCASTS -> searchScreenState.searchPodcastsResult
                                                }

                                            // SONGS tab 分页:近底(剩 6 行)自动追加下一页;
                                            // 其余 tab 一次拉完(token 恒 null,触发器自然静默)
                                            val songsPagingActive =
                                                searchScreenState.searchType == SearchType.SONGS &&
                                                    searchScreenState.songsNextPageToken != null
                                            if (songsPagingActive) {
                                                val nearEnd by remember {
                                                    derivedStateOf {
                                                        resultsState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                                            ?.let { last ->
                                                                last >= resultsState.layoutInfo.totalItemsCount - 6
                                                            } == true
                                                    }
                                                }
                                                LaunchedEffect(nearEnd) {
                                                    if (nearEnd) searchViewModel.loadMoreSongs()
                                                }
                                            }
                                            Crossfade(targetState = currentResults.isNotEmpty()) {
                                                if (it) {
                                                    LazyColumn(
                                                        contentPadding =
                                                            PaddingValues(
                                                                start = 4.dp,
                                                                end = 4.dp,
                                                                top = searchBarHeight,
                                                                bottom = 10.dp,
                                                            ),
                                                        state = resultsState,
                                                    ) {
                                                        // ALL 聚合部分失败横幅:失败分类在混排里静默缺席,
                                                        // 用户停留在本页时由此得知缺席是请求失败所致(切到该分类
                                                        // tab 的空态错误提示之外的第二落点)。重试=重跑聚合。
                                                        if (searchScreenState.searchType == SearchType.ALL &&
                                                            searchScreenState.searchAllFailedCategories.isNotEmpty() &&
                                                            // 横幅描述的是聚合本身:聚合属于别的源时(切源未及重聚合的窗口)不显示
                                                            searchScreenState.searchAllSource == selectedSource
                                                        ) {
                                                            item(key = "all_partial_failed_banner") {
                                                                AllPartialFailedBanner(
                                                                    failedTypes = searchScreenState.searchAllFailedCategories,
                                                                    onRetry = retryCurrentTabSearch,
                                                                )
                                                            }
                                                        }
                                                        items(currentResults) { result ->
                                                            when (result) {
                                                                is SongsResult -> {
                                                                    SongFullWidthItems(
                                                                        track = result.toTrack(),
                                                                        isPlaying = result.videoId == currentVideoId,
                                                                        modifier = Modifier,
                                                                        onMoreClickListener = {
                                                                            onMoreClick(result.toTrack().toSongEntity())
                                                                        },
                                                                        onClickListener = {
                                                                            val firstTrack = result.toTrack()
                                                                            searchViewModel.setQueueData(
                                                                                QueueData.Data(
                                                                                    listTracks = arrayListOf(firstTrack),
                                                                                    firstPlayedTrack = firstTrack,
                                                                                    playlistId = "RDAMVM${result.videoId}",
                                                                                    playlistName =
                                                                                        "\"${searchText}\" ${
                                                                                            getStringBlocking(
                                                                                                Res.string.in_search,
                                                                                            )
                                                                                        }",
                                                                                    playlistType = PlaylistType.RADIO,
                                                                                    continuation = null,
                                                                                ),
                                                                            )
                                                                            searchViewModel.loadMediaItem(firstTrack, Config.SONG_CLICK)
                                                                        },
                                                                        onAddToQueue = {
                                                                            sharedViewModel.addListToQueue(
                                                                                arrayListOf(result.toTrack()),
                                                                            )
                                                                        },
                                                                        selectionMode = selectionState.isActive,
                                                                        isSelected = selectionState.isSelected(result.videoId),
                                                                        onLongClick = { selectionState.start(it) },
                                                                        onSelectToggle = { selectionState.toggle(it) },
                                                                    )
                                                                }

                                                                is VideosResult -> {
                                                                    // 网易 MV(videoId=NETEASE_MV_ 哨兵):与歌手页 MV 货架/收藏列表
                                                                    // 同链路——playMv 单曲哨兵队列(loadMore 见前缀即止);不走
                                                                    // RDAMVM 电台包装,也不给三点菜单(MV 不落 song 表,菜单动作全是歌语义)
                                                                    val isNeteaseMv = result.videoId.startsWith(NETEASE_MV_MEDIA_ID_PREFIX)
                                                                    SongFullWidthItems(
                                                                        track = result.toTrack(),
                                                                        isPlaying = result.videoId == currentVideoId,
                                                                        modifier = Modifier,
                                                                        onMoreClickListener =
                                                                            if (isNeteaseMv) {
                                                                                null
                                                                            } else {
                                                                                {
                                                                                    onMoreClick(result.toTrack().toSongEntity())
                                                                                }
                                                                            },
                                                                        onClickListener = {
                                                                            if (isNeteaseMv) {
                                                                                // 选区激活中点按吞掉(selectionMode=false 让点击落到这里,别打断选歌)
                                                                                if (!selectionState.isActive) sharedViewModel.playMv(result.toTrack())
                                                                            } else {
                                                                                val firstTrack = result.toTrack()
                                                                                searchViewModel.setQueueData(
                                                                                    QueueData.Data(
                                                                                        listTracks = arrayListOf(firstTrack),
                                                                                        firstPlayedTrack = firstTrack,
                                                                                        playlistId = "RDAMVM${result.videoId}",
                                                                                        playlistName =
                                                                                            "\"${searchText}\" ${
                                                                                                getStringBlocking(
                                                                                                    Res.string.in_search,
                                                                                                )
                                                                                            }",
                                                                                        playlistType = PlaylistType.RADIO,
                                                                                        continuation = null,
                                                                                    ),
                                                                                )
                                                                                searchViewModel.loadMediaItem(firstTrack, Config.VIDEO_CLICK)
                                                                            }
                                                                        },
                                                                        onAddToQueue = {
                                                                            sharedViewModel.addListToQueue(
                                                                                arrayListOf(result.toTrack()),
                                                                            )
                                                                        },
                                                                        // 网易 MV 不参与歌曲多选:哨兵 ID 在 song 表无行,选区动作
                                                                        // (下一首/下载/点赞/加歌单)全是歌语义,加 YT 歌单还会把
                                                                        // NETEASE_MV_… 当非数字 YT ID 提交——不起选也不可勾
                                                                        selectionMode = !isNeteaseMv && selectionState.isActive,
                                                                        isSelected = !isNeteaseMv && selectionState.isSelected(result.videoId),
                                                                        onLongClick =
                                                                            if (isNeteaseMv) {
                                                                                null
                                                                            } else {
                                                                                { selectionState.start(it) }
                                                                            },
                                                                        onSelectToggle =
                                                                            if (isNeteaseMv) {
                                                                                null
                                                                            } else {
                                                                                { selectionState.toggle(it) }
                                                                            },
                                                                    )
                                                                }

                                                                is AlbumsResult -> {
                                                                    PlaylistFullWidthItems(
                                                                        data = result,
                                                                        onClickListener = {
                                                                            navController.navigate(
                                                                                AlbumDestination(
                                                                                    result.browseId,
                                                                                ),
                                                                            )
                                                                        },
                                                                    )
                                                                }

                                                is ArtistsResult -> {
                                                    ArtistFullWidthItems(
                                                        data = result,
                                                        onClickListener = {
                                                            navController.navigate(
                                                                ArtistDestination(
                                                                    result.browseId,
                                                                ),
                                                            )
                                                        },
                                                    )
                                                }

                                                                is PlaylistsResult -> {
                                                                    PlaylistFullWidthItems(
                                                                        data = result,
                                                                        onClickListener = {
                                                                            when (result.resultType) {
                                                                                                // YT 播客(独立播客详情页)
                                                                                                "Podcast" -> navController.navigate(
                                                                                                    PodcastDestination(
                                                                                                        result.browseId,
                                                                                                    ),
                                                                                                )
                                                                                                // 网易电台(数字 id→现有电台详情页,与播客 chip 页同页)
                                                                                                NETEASE_RADIO_SEARCH_RESULT_TYPE -> navController.navigate(
                                                                                                    NeteaseRadioDetailDestination(
                                                                                                        radioId = result.browseId.toLongOrNull() ?: 0L,
                                                                                                        radioName = result.title,
                                                                                                    ),
                                                                                                )
                                                                                                else -> navController.navigate(
                                                                                                    PlaylistDestination(
                                                                                                        result.browseId,
                                                                                                    ),
                                                                                                )
                                                                                            }
                                                                        },
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        // SONGS tab 追加下一页时的尾部指示行
                                                        if (searchScreenState.searchType == SearchType.SONGS &&
                                                            searchScreenState.songsLoadingMore
                                                        ) {
                                                            item {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxWidth()
                                                                            .padding(vertical = 12.dp),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                                                                }
                                                            }
                                                        }
                                                        // Space at bottom to account for bottom navigation and mini player
                                                        item { Spacer(modifier = Modifier.height(150.dp)) }
                                                    }
                                                } else if (
                                                    // 空列表 + 失败 → 错误+重试(重跑该分类),别误报"无结果"。
                                                    // 失败的三类来源(标记都须与当前**词/源**一致):
                                                    // ① 当前分类 ∈ 聚合失败标记(混排中静默缺席,聚合源=当前源);
                                                    // ② 当前分类 ∈ 单发失败标记(失败时用户已切走,全局 Error 未写,
                                                    //    靠它保留;词+源都符合才渲染——换词后旧词失败不显示在新词下);
                                                    // ③ 当前是 ALL 且聚合有失败(全败/空+败在切走又切回时的兜底)
                                                    (searchScreenState.searchType in searchScreenState.searchAllFailedCategories &&
                                                        searchScreenState.searchAllSource == selectedSource) ||
                                                    searchScreenState.tabSearchFailed[searchScreenState.searchType]
                                                        ?.let { (failedQuery, failedSource) -> failedQuery == searchText && failedSource == selectedSource } == true ||
                                                    (searchScreenState.searchType == SearchType.ALL &&
                                                        searchScreenState.searchAllFailedCategories.isNotEmpty() &&
                                                        searchScreenState.searchAllSource == selectedSource)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        SearchErrorColumn(onRetry = retryCurrentTabSearch)
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.no_results_found),
                                                            style = typo().titleMedium,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth(),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is SearchScreenUIState.Error -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            // Error state(重试按当前 tab 重跑,不再恒 searchAll)
                                            SearchErrorColumn(onRetry = retryCurrentTabSearch)
                                        }
                                    }

                                    SearchScreenUIState.Empty -> {
                                        // Empty state
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.no_results_found),
                                                style = typo().titleMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
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
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { searchBarHeightPx = it.size.height },
        ) {
            // Animate only the background so scrolling keeps the search input and its focus.
            AnimatedVisibility(
                visible = !isContentAtTop,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                label = "search_bar_scrim",
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .hazeBlur(HazeInput.Sources(hazeState), HazeMaterials.ultraThin().then { blurEnabled(true) }),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(vertical = 10.dp),
            ) {
        AnimatedVisibility(visible = selectionState.isActive) {
            SongSelectionTopAppBar(
                state = selectionState,
                onSelectAll = {
                    // 网易 MV 哨兵 ID 剔除:MV 行不可选(见 VideosResult 分支注释),
                    // 全选也不带它们——选区动作按歌曲表取行,哨兵 ID 无行
                    val visible =
                        when (searchScreenState.searchType) {
                            SearchType.SONGS -> searchScreenState.searchSongsResult.map { it.videoId }
                            SearchType.VIDEOS ->
                                searchScreenState.searchVideosResult
                                    .map { it.videoId }
                                    .filterNot { it.startsWith(NETEASE_MV_MEDIA_ID_PREFIX) }
                            SearchType.ALL ->
                                searchScreenState.searchAllResult.mapNotNull {
                                    (it as? SongsResult)?.videoId ?: (it as? VideosResult)?.videoId
                                }.filterNot { it.startsWith(NETEASE_MV_MEDIA_ID_PREFIX) }
                            else -> emptyList()
                        }
                    selectionState.toggleSelectAll(visible)
                },
                onOpenActions = { showSelectionSheet = true },
                containerColor = Color.Transparent,
                // Zero here AND on the SearchBar below: the Column that holds them both consumes
                // the status bar once, for the whole stack. Leaving it on either child reserves it
                // a second time — which is the slab of padding this screen used to show.
                windowInsets = WindowInsets(0),
            )
        }
        // Search Bar with Animated Placeholder
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = { newText ->
                        searchText = newText
                    },
                    onSearch = { query ->
                        // A pasted YouTube link is a destination, not a query. Translating it into
                        // the app's own deep link hands it to the same intent flow that handles
                        // shared links, so it plays or opens straight away instead of being
                        // searched for as text. Anything else falls through to a normal search.
                        val deepLink = query.toAppDeepLinkOrNull()
                        if (deepLink != null) {
                            focusManager.clearFocus()
                            sharedViewModel.setIntent(GenericIntent(data = deepLink))
                        } else if (query.isNotEmpty()) {
                            isSearchSubmitted = true
                            focusManager.clearFocus()
                            searchViewModel.insertSearchHistory(query)
                            when (searchScreenState.searchType) {
                                SearchType.ALL -> searchViewModel.searchAll(query)
                                SearchType.SONGS -> searchViewModel.searchSongs(query)
                                SearchType.VIDEOS -> searchViewModel.searchVideos(query)
                                SearchType.ALBUMS -> searchViewModel.searchAlbums(query)
                                SearchType.ARTISTS -> searchViewModel.searchArtists(query)
                                SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(query)
                                SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(query)
                                SearchType.PODCASTS -> searchViewModel.searchPodcast(query)
                            }
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    enabled = true,
                    placeholder = {
                        // Animated placeholder text
                        AnimatedContent(
                            targetState = currentPlaceholderIndex,
                            transitionSpec = {
                                (
                                    fadeIn(animationSpec = tween(500)) +
                                        slideInVertically { height -> height }
                                ).togetherWith(
                                    fadeOut(animationSpec = tween(500)) +
                                        slideOutVertically { height -> -height },
                                )
                            },
                            label = "placeholder_animation",
                        ) { index ->
                            Text(
                                // 取模兜底:换源后列表 6→3,rememberSaveable 的 index 可能短暂越界
                                text = placeholderTexts[index % placeholderTexts.size],
                                style = typo().labelMedium,
                            )
                        }
                    },
                    leadingIcon = {
                        Crossfade(
                            targetState = isSearchPanelVisible,
                            modifier = Modifier.size(48.dp),
                            animationSpec = tween(200),
                            label = "search_back_icon",
                        ) { showBack ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showBack) {
                                    IconButton(
                                        onClick = dismissSearchPanel,
                                        enabled = isSearchPanelVisible,
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowBackIosNew,
                                            contentDescription = "Back",
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = SimpIcons.Search,
                                        contentDescription = "Search",
                                    )
                                }
                            }
                        }
                    },
                    trailingIcon = {
                        // X button only shows when there's text
                        if (searchText.isNotEmpty()) {
                            IconButton(
                                modifier = Modifier.clip(CircleShape),
                                onClick = {
                                    searchText = ""
                                    isSearchSubmitted = false
                                },
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            // See the note on SongSelectionTopAppBar above — the Column owns the status-bar inset.
            windowInsets = WindowInsets(0),
            content = {},
        )
                // Filter chips ride along inside the blurred block instead of sitting in the
                // results branch. That way searchBarHeight covers them too, results scroll
                // underneath the whole thing, and the glass has something to blur.
                AnimatedVisibility(visible = searchUIType == SearchUIType.SEARCH_RESULTS) {
                    Row(
                        modifier =
                            Modifier
                                .horizontalScroll(chipRowState)
                                .padding(top = 10.dp)
                                .padding(horizontal = 12.dp),
                    ) {
                        visibleSearchTabs.forEach { id ->
                            val isSelected = id == searchScreenState.searchType
                            Spacer(modifier = Modifier.width(4.dp))
                            Chip(
                                isAnimated = uiState is SearchScreenUIState.Loading,
                                isSelected = isSelected,
                                text = stringResource(id.toStringRes()),
                            ) {
                                searchViewModel.setSearchType(id)
                                // 词/源核对:ALL 比对聚合绑定,分类比对各自绑定(categoryBindings);
                                // 不符(含从未搜过)且搜索框非空 → 就地重搜该 tab——旧词/旧源
                                // 结果与"尚未搜索"都不再当展示内容(与 ALL 的重聚合对称)。
                                // 当前 tab 正在加载则跳过:在途搜索必属当前 tab(互斥+切 tab 即
                                // 取消+派发按当前 tab),此刻绑定还停在上一轮——重查会取消并
                                // 重启同一请求(加载中重复点当前 tab 的重启问题)
                                val loadingThisTab =
                                    searchScreenState.searchType == id && uiState is SearchScreenUIState.Loading
                                val bindingStale =
                                    if (id == SearchType.ALL) {
                                        searchScreenState.searchAllQuery != searchText ||
                                            searchScreenState.searchAllSource != selectedSource
                                    } else {
                                        searchScreenState.categoryBindings[id]
                                            ?.let { (q, s) -> q == searchText && s == selectedSource } != true
                                    }
                                if (!loadingThisTab && searchText.isNotEmpty() && bindingStale) {
                                    when (id) {
                                        SearchType.ALL -> searchViewModel.searchAll(searchText)
                                        SearchType.SONGS -> searchViewModel.searchSongs(searchText)
                                        SearchType.VIDEOS -> searchViewModel.searchVideos(searchText)
                                        SearchType.ALBUMS -> searchViewModel.searchAlbums(searchText)
                                        SearchType.ARTISTS -> searchViewModel.searchArtists(searchText)
                                        SearchType.PLAYLISTS -> searchViewModel.searchPlaylists(searchText)
                                        SearchType.FEATURED_PLAYLISTS -> searchViewModel.searchFeaturedPlaylist(searchText)
                                        SearchType.PODCASTS -> searchViewModel.searchPodcast(searchText)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestItemRow(
    searchResult: SearchResultType,
    onItemClick: (SearchResultType) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onItemClick(searchResult) }
                .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val url =
            when (searchResult) {
                is SongsResult -> {
                    searchResult.thumbnails?.lastOrNull()?.url
                }

                is AlbumsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is ArtistsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is PlaylistsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is VideosResult -> {
                    searchResult.thumbnails?.lastOrNull()?.url
                }

                else -> {
                    null
                }
            }

        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(url)
                        .crossfade(true)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            if (searchResult is ArtistsResult) {
                                CircleShape
                            } else {
                                RoundedCornerShape(4.dp)
                            },
                        ),
            )
        }

        Spacer(modifier = Modifier.padding(horizontal = 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val title =
                when (searchResult) {
                    is SongsResult -> {
                        searchResult.title
                    }

                    is AlbumsResult -> {
                        searchResult.title
                    }

                    is ArtistsResult -> {
                        searchResult.artist
                    }

                    is PlaylistsResult -> {
                        searchResult.title
                    }

                    is VideosResult -> {
                        searchResult.title
                    }

                    else -> {
                        null
                    }
                } ?: "Unknown"

            Text(
                text = title,
                style = typo().labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))

            val subtitle =
                when (searchResult) {
                    is SongsResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    is AlbumsResult -> searchResult.artists.map { it.name }.connectArtists()
                    is PlaylistsResult -> searchResult.author.ifEmpty { "YouTube Music" }
                    is ArtistsResult -> stringResource(Res.string.artists)
                    is VideosResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    else -> null
                } ?: "Unknown"

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

enum class SearchUIType {
    EMPTY,
    SEARCH_HISTORY,
    SEARCH_SUGGESTIONS,
    SEARCH_RESULTS,
}

/** 搜索错误列(全局错误态与 ALL 聚合失败分类的空 tab 共用):文案+重试按钮 */
@Composable
private fun SearchErrorColumn(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.error_occurred),
            style = typo().titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(Res.string.retry))
        }
    }
}

/** ALL 聚合搜索部分失败横幅:点名失败分类(本地化名),重试重跑聚合。
 *  只在 ALL tab 且失败集合非空时由列表头部挂载;空结果+失败走全局 Error 态不在此。 */
@Composable
private fun AllPartialFailedBanner(
    failedTypes: Set<SearchType>,
    onRetry: () -> Unit,
) {
    // sortedBy/map 均为 inline lambda,composable 调用合法
    val names =
        failedTypes
            .sortedBy { it.ordinal }
            .map { stringResource(it.toStringRes()) }
            .joinToString(", ")
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = stringResource(Res.string.partial_categories_failed, names),
                style = typo().labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(Res.string.retry))
            }
        }
    }
}

/** 网易源下可见的搜索 tab:视频=cloudsearch type1004(MV→playMv)、播客=type1009(电台→
 *  详情页)均已接链路;仅"精选歌单"无网易维度保持隐藏 */
private val NETEASE_SEARCH_TABS =
    listOf(
        SearchType.ALL,
        SearchType.SONGS,
        SearchType.VIDEOS,
        SearchType.ARTISTS,
        SearchType.ALBUMS, // M6 专辑页已通,放开
        SearchType.PLAYLISTS,
        SearchType.PODCASTS,
    )
