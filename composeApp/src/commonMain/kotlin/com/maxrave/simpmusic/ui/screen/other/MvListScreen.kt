package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.LibraryGridDefaults
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.MvListUIState
import com.maxrave.simpmusic.viewModel.MvListViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.favorite_mvs
import simpmusic.composeapp.generated.resources.retry

/**
 * 收藏的 MV 页(2026-10-08 用户定:UI 对齐收藏歌单网格):16:9 tile 网格
 * (LibraryGridDefaults 口径,封面+标题+艺人两行文字与歌单 tile 同构),点击单 MV 队列起播。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvListScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: MvListViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val gridState = rememberLazyGridState()
    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.songEntity?.videoId ?: it?.track?.videoId }
    }.collectAsState(null)

    // 近底触发下一页:网格按"剩余条目"计,24 条≈手机 3 列时的 8 行,与改版前列页同跑道;
    // 翻页失败后被 loadMoreFailed 挡住,改由页脚"重试"驱动
    val shouldLoadMore by remember {
        derivedStateOf {
            val state = uiState as? MvListUIState.Success ?: return@derivedStateOf false
            state.hasMore &&
                !state.loadingMore &&
                !state.loadMoreFailed &&
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?.let { it >= gridState.layoutInfo.totalItemsCount - 24 } == true
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = uiState) { state ->
            when (state) {
                is MvListUIState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = LibraryGridDefaults.minTileSize),
                        horizontalArrangement = LibraryGridDefaults.horizontalArrangement,
                        verticalArrangement = LibraryGridDefaults.verticalArrangement,
                        contentPadding =
                            PaddingValues(
                                start = LibraryGridDefaults.horizontalPadding,
                                top = innerPadding.calculateTopPadding() + 64.dp + 4.dp,
                                end = LibraryGridDefaults.horizontalPadding,
                                // 底部让位已在 contentPadding,页尾 EndOfPage 不再叠底栏垫
                                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                            ),
                        state = gridState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState),
                    ) {
                        items(state.mvs, key = { it.videoId }) { mv ->
                            MvTile(
                                title = mv.title,
                                artist = mv.artists?.joinToString { it.name } ?: "",
                                coverUrl = mv.thumbnails?.lastOrNull()?.url,
                                isPlaying = mv.videoId == playingTrack,
                                onClick = { sharedViewModel.playMv(mv) },
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                            when {
                                state.loadingMore -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                                }

                                state.loadMoreFailed -> TextButton(
                                    onClick = viewModel::retryLoadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.retry),
                                        style = typo().labelMedium,
                                    )
                                }

                                else -> EndOfPage(includeBottomBarPadding = false)
                            }
                        }
                    }
                    TopAppBar(
                        modifier =
                            Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(MaterialTheme.colorScheme.surface, 0.3f)),
                        title = {
                            Text(
                                text = stringResource(Res.string.favorite_mvs),
                                style = typo().titleMedium,
                                maxLines = 1,
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
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                Color.Transparent,
                                Color.Unspecified,
                                Color.Unspecified,
                                Color.Unspecified,
                                Color.Unspecified,
                            ),
                    )
                }

                MvListUIState.Loading -> CenterLoadingBox(Modifier.fillMaxSize())

                is MvListUIState.Error ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = viewModel::refresh) {
                                Text(stringResource(Res.string.retry))
                            }
                        }
                    }
            }
        }
    }
}

/**
 * MV tile:收藏歌单 tile(HomeItemContentPlaylist fillMaxWidth 档)同构——封面铺满网格槽
 * (MV 缩略图天然 16:9,不裁成方卡)+ 标题两行省略 + 艺人单行 marquee(与歌单副标题同参数)。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MvTile(
    title: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(coverUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(coverUrl)
                    .crossfade(550)
                    .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = rememberHolderPainter(isVideo = true),
            error = rememberHolderPainter(isVideo = true),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
        )
        Text(
            text = title,
            style = typo().titleSmall,
            color =
                if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 8.dp),
        )
        Text(
            text = artist,
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp)
                    .basicMarquee(
                        initialDelayMillis = 2000,
                        repeatDelayMillis = 2000,
                        velocity = 25.dp,
                    ),
        )
    }
}
