package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.maxrave.netease.model.NeteaseMvDetail
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.utils.formatCompactCount
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.likes_count
import simpmusic.composeapp.generated.resources.published_at
import simpmusic.composeapp.generated.resources.view_count

/** 网易 MV 说明卡复用歌曲的排版，数字只展示 MV 端点实际提供的值。 */
@Composable
internal fun NeteaseMvDescription(
    detail: NeteaseMvDetail,
    artistBio: String?,
) {
    val uriHandler = LocalUriHandler.current
    Column {
        detail.publishTimeStr?.takeIf { it.isNotBlank() }?.let {
            Text(stringResource(Res.string.published_at, it), style = typo().labelSmall, color = Color.White)
            Spacer(Modifier.height(10.dp))
        }
        detail.playCount?.let {
            Text(stringResource(Res.string.view_count, formatCompactCount(it)), style = typo().labelMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
        }
        detail.likeCount?.let {
            Text(stringResource(Res.string.likes_count, formatCompactCount(it)), style = typo().labelMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
        }
        val bio = detail.description?.takeIf { it.isNotBlank() }
            ?: detail.briefDesc?.takeIf { it.isNotBlank() }
            ?: artistBio
        if (!bio.isNullOrBlank()) {
            Text(stringResource(Res.string.description), style = typo().labelSmall, color = Color.White)
            Spacer(Modifier.height(10.dp))
            DescriptionView(text = bio, onTimeClicked = {}, onURLClicked = uriHandler::openUri)
        }
    }
}
