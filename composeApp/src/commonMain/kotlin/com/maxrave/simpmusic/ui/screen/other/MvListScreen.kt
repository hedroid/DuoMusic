package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
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

/** 收藏的 MV 列表页(SimilarSongsScreen 同款骨架):16:9 行卡,点击单 MV 队列起播 */
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
    val listState = rememberLazyListState()
    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.songEntity?.videoId ?: it?.track?.videoId }
    }.collectAsState(null)

    // 近底触发下一页(剩 8 行内)
    val shouldLoadMore by remember {
        derivedStateOf {
            val state = uiState as? MvListUIState.Success ?: return@derivedStateOf false
            state.hasMore &&
                !state.loadingMore &&
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?.let { it >= listState.layoutInfo.totalItemsCount - 8 } == true
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = uiState) { state ->
            when (state) {
                is MvListUIState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState),
                    ) {
                        item(contentType = "header") {
                            Spacer(Modifier.size(innerPadding.calculateTopPadding() + 64.dp))
                        }
                        itemsIndexed(state.mvs, key = { _, mv -> mv.videoId }) { _, mv ->
                            MvRow(
                                title = mv.title,
                                artist = mv.artists?.joinToString { it.name } ?: "",
                                coverUrl = mv.thumbnails?.lastOrNull()?.url,
                                isPlaying = mv.videoId == playingTrack,
                                onClick = { sharedViewModel.playMv(mv) },
                            )
                        }
                        item(contentType = "footer") {
                            if (state.loadingMore) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                                }
                            } else {
                                EndOfPage()
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
                        Text(
                            text = state.message,
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            }
        }
    }
}

/** MV 行:16:9 封面 + 标题/艺人两行 + 播放中高亮(与歌曲行同 72dp 档视觉) */
@Composable
private fun MvRow(
    title: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(width = 112.dp, height = 63.dp)
                    .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = typo().titleSmall,
                color =
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
            )
            Text(
                text = artist,
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
