package com.maxrave.simpmusic.ui.component

import simpmusic.composeapp.generated.resources.netease
import simpmusic.composeapp.generated.resources.create_new_playlist
import simpmusic.composeapp.generated.resources.login_required_short
import com.maxrave.simpmusic.ui.screen.library.CreatePlaylistDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.maxrave.domain.source.MusicSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.create_playlist_description
import simpmusic.composeapp.generated.resources.create_room_description
import simpmusic.composeapp.generated.resources.create_room_live
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.local_playlist
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import simpmusic.composeapp.generated.resources.settings

/**
 * The top-bar "+" (Spotify's Create): a local playlist or a Listen Together room.
 *
 * With liquid glass on it is a glass pill, shared with Settings when [onSettings] is given, like the
 * like/search/more pill on the playlist screens; [backdrop] must be a sibling of this, never an
 * ancestor. With glass off the "+" gets a grey fill and a border so it reads as the special one, and
 * Settings stays a plain icon.
 */
@Composable
fun CreateActions(
    backdrop: PlatformBackdrop,
    navController: NavController,
    onSettings: (() -> Unit)? = null,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val room by koinInject<ListenTogetherRepository>().room.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var showPlaylistName by remember { mutableStateOf(false) }
    // fork A 案(2026-10-27):"新建歌单"=云端建单,按当前选中音源路由;
    // 未登录该源→行置灰,副标题换"登录 XX 后可用"(与全局门控约定一致)
    val isNeteaseSource by sharedViewModel.selectedSource
        .collectAsStateWithLifecycle(initialValue = MusicSource.YOUTUBE_MUSIC.name)
    val neteaseLoggedIn by sharedViewModel.neteaseLoggedIn.collectAsStateWithLifecycle()
    val youtubeLoggedIn by sharedViewModel.isUserLoggedInFlow().collectAsStateWithLifecycle(initialValue = false)
    val createEnabled = if (isNeteaseSource == MusicSource.NETEASE.name) neteaseLoggedIn else youtubeLoggedIn
    val sourceLabel =
        if (isNeteaseSource == MusicSource.NETEASE.name) {
            stringResource(Res.string.netease)
        } else {
            "YouTube Music"
        }
    val tint = MaterialTheme.colorScheme.onBackground

    val createButton: @Composable () -> Unit = {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (LocalLiquidGlassEnabled.current) {
                IconButton(onClick = { showCreate = true }) {
                    Icon(SimpIcons.Add, contentDescription = stringResource(Res.string.create), tint = tint)
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { showCreate = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SimpIcons.Add, contentDescription = stringResource(Res.string.create), tint = tint)
                }
            }
            // Listen Together moved into the sheet, so its live-room dot moved onto this button.
            if (room.inRoom) LiveRoomDot()
        }
    }
    val settingsButton: @Composable () -> Unit = {
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                Icon(SimpIcons.Settings, contentDescription = stringResource(Res.string.settings), tint = tint)
            }
        }
    }

    if (LocalLiquidGlassEnabled.current) {
        Row(
            modifier =
                Modifier
                    // 44dp, the Home avatar's size: Apple sizes a bar's avatar and glass buttons alike.
                    .height(44.dp)
                    .liquidGlass(
                        backdrop,
                        RoundedCornerShape(24.dp),
                        // Alone, the "+" is a small circle, which the directional default rim misses.
                        highlight = if (onSettings == null) Highlight(width = 1.dp) else Highlight.Default,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            createButton()
            settingsButton()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            createButton()
            settingsButton()
        }
    }

    if (showCreate) {
        CreateSheet(
            liveRoom = room.inRoom,
            createEnabled = createEnabled,
            sourceLabel = sourceLabel,
            onDismiss = { showCreate = false },
            onPlaylist = {
                showCreate = false
                showPlaylistName = true
            },
            onListenTogether = {
                showCreate = false
                navController.navigate(ListenTogetherDestination)
            },
        )
    }
    if (showPlaylistName) {
        // 复用库页建单 tile 的命名弹窗(AlertDialog 形态,IME 教训);未登录不该走到这
        CreatePlaylistDialog(
            onDismiss = { showPlaylistName = false },
            onCreate = { name ->
                showPlaylistName = false
                sharedViewModel.createCloudPlaylistForCurrentSource(name)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSheet(
    liveRoom: Boolean,
    createEnabled: Boolean,
    sourceLabel: String,
    onDismiss: () -> Unit,
    onPlaylist: () -> Unit,
    onListenTogether: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(bottom = 8.dp)) {
                SheetHandle()
                Text(
                    text = stringResource(Res.string.create),
                    style = typo().titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                CreateRow(
                    icon = SimpIcons.PlaylistAdd,
                    title = stringResource(Res.string.create_new_playlist),
                    subtitle =
                        if (createEnabled) {
                            sourceLabel
                        } else {
                            stringResource(Res.string.login_required_short, sourceLabel)
                        },
                    enabled = createEnabled,
                    onClick = onPlaylist,
                )
                CreateRow(
                    icon = SimpIcons.Groups,
                    title = stringResource(Res.string.listen_together),
                    subtitle = stringResource(if (liveRoom) Res.string.create_room_live else Res.string.create_room_description),
                    onClick = onListenTogether,
                )
                EndOfModalBottomSheet()
            }
        }
    }
}

@Composable
private fun CreateRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(alpha = contentAlpha)) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column {
            Text(text = title, style = typo().titleSmall)
            Text(text = subtitle, style = typo().bodySmall)
        }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(5.dp))
            Card(
                modifier =
                    Modifier
                        .width(60.dp)
                        .height(4.dp),
                colors =
                    CardDefaults.cardColors().copy(
                        containerColor = MaterialTheme.colorScheme.outline,
                    ),
                shape = RoundedCornerShape(50),
            ) {}
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

/** Name a new local playlist. Shared by Library's create tile and the Create sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLocalPlaylistSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var newTitle by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // The sheet is handed no insets, so nothing else lifts the field above the keyboard.
                // Navigation bars are left out: EndOfModalBottomSheet already pads for them.
                modifier = Modifier.windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
            ) {
                SheetHandle()
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { s -> newTitle = s },
                    label = {
                        Text(text = stringResource(Res.string.playlist_name))
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(5.dp))
                TextButton(
                    onClick = {
                        if (newTitle.isBlank()) {
                            coroutineScope.launch { sharedViewModel.makeToast(getString(Res.string.playlist_name_cannot_be_empty)) }
                        } else {
                            onCreate(newTitle)
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                ) {
                    Text(text = stringResource(Res.string.create))
                }
                EndOfModalBottomSheet()
            }
        }
    }
}
