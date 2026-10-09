package com.maxrave.simpmusic.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter

/**
 * 云盘封面兜底位图(2026-10-10 用户定案,亮蓝版已删——"太丑,换业内通用";
 * 库页入口兜底改 Compose 中性 tile,见 CloudDiskRow)。
 * 仅存安静版:中性深灰底+半透明云,给歌曲行/头图兜底——行内兜底会成排重复,
 * 业界通用做法是中性低饱和底+居中小图标(Spotify 灰底音符/网易白底音符同思路)。
 */
object CloudDiskArt {
    val subduedTop: Color = Color(0xFF34373D)
    val subduedBottom: Color = Color(0xFF3E424A)
    val subduedCloudTint: Color = Color.White.copy(alpha = 0.35f)
}

/** 行内/头图兜底:中性深灰底(比页面背景亮一档,占位可辨识)+ 35% 白云 */
fun cloudCoverSubduedPainter(size: Int = 256): Painter =
    BitmapPainter(
        createCloudCoverBitmap(
            size = size,
            topColor = CloudDiskArt.subduedTop,
            bottomColor = CloudDiskArt.subduedBottom,
            cloudColor = CloudDiskArt.subduedCloudTint,
        ),
    )

private fun createCloudCoverBitmap(
    size: Int,
    topColor: Color,
    bottomColor: Color,
    cloudColor: Color,
): ImageBitmap {
    val image = ImageBitmap(size, size)
    val canvas = Canvas(image)
    val s = size.toFloat()
    // 竖向双色分段近似渐变(两块纯色,小尺寸行封面看不出接缝)
    canvas.drawRect(Rect(0f, 0f, s, s / 2f), Paint().apply { color = topColor })
    canvas.drawRect(Rect(0f, s / 2f, s, s), Paint().apply { color = bottomColor })
    // 白云:三圆 + 底部矩形拉平底缘
    val white = Paint().apply { color = cloudColor }
    val unit = s / 256f
    canvas.drawCircle(Offset(88f * unit, 140f * unit), 38f * unit, white)
    canvas.drawCircle(Offset(128f * unit, 116f * unit), 52f * unit, white)
    canvas.drawCircle(Offset(170f * unit, 144f * unit), 34f * unit, white)
    canvas.drawRect(Rect(74f * unit, 150f * unit, 184f * unit, 184f * unit), white)
    return image
}
