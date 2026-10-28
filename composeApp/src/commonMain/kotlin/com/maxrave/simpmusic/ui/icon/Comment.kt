package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.Comment: ImageVector
  get() {
    if (_Comment != null) {
      return _Comment!!
    }
    _Comment =
      ImageVector.Builder(
          name = "Comment",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            // Material Symbols Rounded "comment" 24px(默认 fill0 描边形态):单气泡+三行文字,
            // 「评论」的通用形,与动作行其余描边图标视觉一致;2026-10-28 定案替换 forum 双气泡。
            // 源 SVG 使用 960 网格,这里按 1:40 精确换算到 ImageVector 的 24x24 viewport;
            // 末尾零面积轮廓丢弃。字形恰好贴满 20x20 活区(x 2..22),不触发 buildSimpIcon 缩放。
            moveTo(7f, 14f)
            horizontalLineToRelative(10f)
            quadToRelative(0.425f, 0f, 0.7125f, -0.2875f)
            reflectiveQuadTo(18f, 13f)
            quadToRelative(0f, -0.425f, -0.2875f, -0.7125f)
            reflectiveQuadTo(17f, 12f)
            horizontalLineTo(7f)
            quadToRelative(-0.425f, 0f, -0.7125f, 0.2875f)
            reflectiveQuadTo(6f, 13f)
            quadToRelative(0f, 0.425f, 0.2875f, 0.7125f)
            reflectiveQuadTo(7f, 14f)
            close()
            moveTo(7f, 11f)
            horizontalLineToRelative(10f)
            quadToRelative(0.425f, 0f, 0.7125f, -0.2875f)
            reflectiveQuadTo(18f, 10f)
            quadToRelative(0f, -0.425f, -0.2875f, -0.7125f)
            reflectiveQuadTo(17f, 9f)
            horizontalLineTo(7f)
            quadToRelative(-0.425f, 0f, -0.7125f, 0.2875f)
            reflectiveQuadTo(6f, 10f)
            quadToRelative(0f, 0.425f, 0.2875f, 0.7125f)
            reflectiveQuadTo(7f, 11f)
            close()
            moveTo(7f, 8f)
            horizontalLineToRelative(10f)
            quadToRelative(0.425f, 0f, 0.7125f, -0.2875f)
            reflectiveQuadTo(18f, 7f)
            quadToRelative(0f, -0.425f, -0.2875f, -0.7125f)
            reflectiveQuadTo(17f, 6f)
            horizontalLineTo(7f)
            quadToRelative(-0.425f, 0f, -0.7125f, 0.2875f)
            reflectiveQuadTo(6f, 7f)
            quadToRelative(0f, 0.425f, 0.2875f, 0.7125f)
            reflectiveQuadTo(7f, 8f)
            close()
            moveTo(4f, 18f)
            quadToRelative(-0.825f, 0f, -1.4125f, -0.5875f)
            reflectiveQuadTo(2f, 16f)
            verticalLineToRelative(-12f)
            quadToRelative(0f, -0.825f, 0.5875f, -1.4125f)
            reflectiveQuadTo(4f, 2f)
            horizontalLineToRelative(16f)
            quadToRelative(0.825f, 0f, 1.4125f, 0.5875f)
            reflectiveQuadTo(22f, 4f)
            verticalLineToRelative(15.575f)
            quadToRelative(0f, 0.675f, -0.6125f, 0.9375f)
            reflectiveQuadTo(20.3f, 20.3f)
            lineToRelative(-2.3f, -2.3f)
            horizontalLineTo(4f)
            close()
            moveTo(18.85f, 16f)
            lineTo(20f, 17.125f)
            verticalLineToRelative(-13.125f)
            horizontalLineTo(4f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(14.85f)
            close()
          }
        }
        .buildSimpIcon()
    return _Comment!!
  }

private var _Comment: ImageVector? = null
