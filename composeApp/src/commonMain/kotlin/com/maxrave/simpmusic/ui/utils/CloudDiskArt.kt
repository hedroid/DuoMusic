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
 * 云盘封面兜底位图。两个档位(2026-10-09 用户定案):
 *  - 亮版 [cloudCoverPainter]:蓝底白云,只给库页入口行的空云盘兜底——单次出现的
 *    品牌标识,可以亮。
 *  - 安静版 [cloudCoverSubduedPainter]:中性深灰底+半透明云,给歌曲行/头图兜底——
 *    行内兜底会成排重复,业界通用做法是中性低饱和底+居中小图标(Spotify 灰底音符/
 *    网易白底音符同思路),高饱和色块混在真实封面堆里非常扎眼。
 */
fun cloudCoverPainter(size: Int = 256): Painter =
    BitmapPainter(
        createCloudCoverBitmap(
            size = size,
            topColor = CloudDiskArt.gradientTop,
            bottomColor = CloudDiskArt.gradientBottom,
            cloudColor = CloudDiskArt.cloudTint,
        ),
    )

/** 行内/头图兜底:中性深灰底(比页面背景亮一档,占位可辨识)+ 35% 白云 */
fun cloudCoverSubduedPainter(size: Int = 256): Painter =
    BitmapPainter(
        createCloudCoverBitmap(
            size = size,
            topColor = Color(0xFF34373D),
            bottomColor = Color(0xFF3E424A),
            cloudColor = Color.White.copy(alpha = 0.35f),
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
