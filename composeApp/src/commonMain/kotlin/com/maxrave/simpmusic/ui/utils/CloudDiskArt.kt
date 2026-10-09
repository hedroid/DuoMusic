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
 * 云盘统一视觉:亮蓝底+白云,三处共用——库页入口行封面 / 云盘详情页头图 /
 * 无封面曲目行(thumbnails 空的未匹配歌)的兜底 painter。
 * 刻意不随主题:MaterialTheme 容器色对比太低,深色主题下渐变底与图标糊成一团,
 * 实测被读作"没有封面"。
 */
object CloudDiskArt {
    val gradientTop: Color = Color(0xFF2563EB)
    val gradientBottom: Color = Color(0xFF38BDF8)
    val cloudTint: Color = Color.White
}

/**
 * 云盘封面兜底位图(蓝底+白云,三圆+底矩形拼云形)。一次离屏成图,
 * remember 后全程复用;给 AsyncImage 的 placeholder/error 用(painter 无法
 * 自带渐变底,Compose 渐变 Box 又进不了共享行组件的封面槽,离屏位图是唯一通路)。
 */
fun createCloudCoverBitmap(size: Int = 256): ImageBitmap {
    val image = ImageBitmap(size, size)
    val canvas = Canvas(image)
    val s = size.toFloat()
    // 竖向双色分段近似渐变(两块纯色,小尺寸行封面看不出接缝)
    val topHalf = Paint().apply { color = CloudDiskArt.gradientTop }
    val bottomHalf = Paint().apply { color = CloudDiskArt.gradientBottom }
    canvas.drawRect(Rect(0f, 0f, s, s / 2f), topHalf)
    canvas.drawRect(Rect(0f, s / 2f, s, s), bottomHalf)
    // 白云:三圆 + 底部矩形拉平底缘
    val white = Paint().apply { color = CloudDiskArt.cloudTint }
    val unit = s / 256f
    canvas.drawCircle(Offset(88f * unit, 140f * unit), 38f * unit, white)
    canvas.drawCircle(Offset(128f * unit, 116f * unit), 52f * unit, white)
    canvas.drawCircle(Offset(170f * unit, 144f * unit), 34f * unit, white)
    canvas.drawRect(Rect(74f * unit, 150f * unit, 184f * unit, 184f * unit), white)
    return image
}

/** [createCloudCoverBitmap] 的 painter 包装 */
fun cloudCoverPainter(size: Int = 256): Painter = BitmapPainter(createCloudCoverBitmap(size))
