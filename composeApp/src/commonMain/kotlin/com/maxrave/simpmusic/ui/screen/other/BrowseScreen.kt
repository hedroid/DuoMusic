package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AmbientGlowHeight
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeContentCard
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.homeCardAspectRatio
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.BrowseUIState
import com.maxrave.simpmusic.viewModel.BrowseViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import org.koin.compose.viewmodel.koinViewModel

private val RowGap = 10.dp

// The width a mood button asks for, as the old grid's cells did.
private val MoodCellWidth = 170.dp

/**
 * The page a home section's "More" endpoint opens when YouTube names no album, playlist, artist or
 * podcast behind it. Every shelf is flattened into justified rows — each card keeps its own shape
 * (16:9 video, square otherwise), every thumbnail in a row is the same height, and the number of
 * cards per row follows the shapes in it — drawn with Home's own cards, so an item opens and plays
 * exactly as it does on Home. The glow and the bar are Mix for you's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    browseId: String,
    params: String?,
    title: String?,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val listState = rememberLazyGridState()
    // Mix for you's bar rule: transparent only while pixel 0 is on screen.
    val isAtTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    var track by remember { mutableStateOf<Track?>(null) }
    var bottomSheetShow by remember { mutableStateOf(false) }

    // Mix for you's ambient top glow, tinted from this page's first item instead of its first mix.
    // The client is remembered, as WrappedScreen does, rather than built on every recomposition.
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember { mutableStateOf(backgroundColor) }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val httpClient = remember { HttpClient(CIO) }
    val networkLoader = rememberNetworkLoader(httpClient)
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )
    val firstThumbnail =
        (uiState as? BrowseUIState.Success)
            ?.page
            ?.contents
            ?.firstOrNull()
            ?.thumbnails
            ?.lastOrNull()
            ?.url
    LaunchedEffect(firstThumbnail) {
        firstThumbnail?.let { dominantColorState.updateFrom(Url(it)) }
    }
    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    LaunchedEffect(browseId, params) {
        viewModel.getBrowsePage(browseId, params)
    }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            // Drawn before the list and scrolled away with it, as on Mix for you: the draw-phase
            // translation tracks the first row and parks once it has passed.
            Box(
                Modifier
                    .graphicsLayer {
                        translationY =
                            if (listState.firstVisibleItemIndex == 0) {
                                -listState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                -size.height
                            }
                    }.fillMaxWidth()
                    .height(AmbientGlowHeight)
                    .angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(pageBackground)),
                )
            }
            Crossfade(targetState = uiState) { state ->
                when (state) {
                    is BrowseUIState.Success -> {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            // 心情/氛围条目的列数(内容网格自管列,Adaptive 按宽自适应)
                            val moodColumns = maxOf(1, ((maxWidth + RowGap) / (MoodCellWidth + RowGap)).toInt())
                        // 内容网格与网易标签分类页(NeteaseTagScreen)同款:Adaptive 160dp 方卡、
                        // 15dp 页边、4dp 横距/8dp 纵距(用户 2026-10-12 定案,弃逐行等高混排)。
                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            // Padding rather than a spacer item, so the first item is a real row and
                            // the glow hands off once a row has scrolled by, as on Mix for you.
                            contentPadding = PaddingValues(
                                start = 15.dp,
                                end = 15.dp,
                                top = innerPadding.calculateTopPadding() + 64.dp,
                                bottom = 8.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.page.moods.isNotEmpty()) {
                                // 心情/氛围条目:整行跨列,moods 自管列布局(保留旧观感)
                                item(key = "moods", span = { GridItemSpan(maxLineSpan) }) {
                                    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
                                        state.page.moods.chunked(moodColumns).forEach { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(RowGap),
                                            ) {
                                                row.forEach { mood ->
                                                    Box(Modifier.weight(1f)) {
                                                        MoodMomentAndGenreHomeItem(
                                                            title = mood.title,
                                                            stripeColor = mood.stripeColor,
                                                            fillMaxWidth = true,
                                                        ) {
                                                            navController.navigate(MoodDestination(mood.params))
                                                        }
                                                    }
                                                }
                                                repeat(moodColumns - row.size) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            itemsIndexed(
                                state.page.contents,
                                key = { index, content ->
                                    "${content.playlistId ?: content.browseId ?: content.videoId}-$index"
                                },
                            ) { _, content ->
                                HomeContentCard(
                                    temp = content,
                                    navController = navController,
                                    viewModel = viewModel,
                                    onLongClick = {
                                        track = it.toTrack()
                                        bottomSheetShow = true
                                    },
                                    fillMaxWidth = true,
                                )
                            }
                            item(key = "end", span = { GridItemSpan(maxLineSpan) }) {
                                EndOfPage()
                            }
                        }
                        }
                    }

                    is BrowseUIState.Error -> {
                        LaunchedEffect(state) {
                            viewModel.makeToast(state.message)
                        }
                    }

                    BrowseUIState.Loading -> {
                        CenterLoadingBox(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(15.dp),
                        )
                    }
                }
            }
        }
        // Transparent while the list sits at pixel zero, frosted the moment it scrolls — Mix for
        // you's crossfade and frost recipe.
        AnimatedContent(
            targetState = isAtTop,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
        ) { atTop ->
            Column(
                Modifier.then(
                    if (atTop) {
                        Modifier.background(Color.Transparent)
                    } else {
                        Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(pageBackground, 0.3f))
                    },
                ),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = (uiState as? BrowseUIState.Success)?.page?.title ?: title.orEmpty(),
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
                                tint = MaterialTheme.colorScheme.onSurface,
                            ) {
                                navController.navigateUp()
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                )
            }
        }
    }
}

