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
val SimpIcons.LinkOff: ImageVector
    get() {
        if (_LinkOff != null) {
            return _LinkOff!!
        }
        _LinkOff =
            ImageVector.Builder(
                name = "LinkOff",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
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
            moveTo(15.625f, 12.775f)
            lineToRelative(-1.775f, -1.775f)
            horizontalLineToRelative(1.15f)
            quadToRelative(0.425f, 0f, 0.713f, 0.288f)
            reflectiveQuadTo(16f, 12f)
            quadToRelative(0f, 0.25f, -0.1f, 0.45f)
            reflectiveQuadToRelative(-0.275f, 0.325f)
            moveTo(20.5f, 21.9f)
            quadToRelative(-0.275f, 0.275f, -0.7f, 0.275f)
            reflectiveQuadToRelative(-0.7f, -0.275f)
            lineTo(2.1f, 4.9f)
            quadToRelative(-0.275f, -0.275f, -0.275f, -0.7f)
            reflectiveQuadToRelative(0.275f, -0.7f)
            quadToRelative(0.275f, -0.275f, 0.7f, -0.275f)
            reflectiveQuadToRelative(0.7f, 0.275f)
            lineToRelative(17f, 17f)
            quadToRelative(0.275f, 0.275f, 0.275f, 0.7f)
            reflectiveQuadToRelative(-0.275f, 0.7f)
            moveTo(7f, 17f)
            quadToRelative(-2.075f, 0f, -3.538f, -1.463f)
            reflectiveQuadTo(2f, 12f)
            quadToRelative(0f, -1.725f, 1.05f, -3.075f)
            reflectiveQuadToRelative(2.7f, -1.775f)
            lineToRelative(1.85f, 1.85f)
            horizontalLineToRelative(-0.6f)
            quadToRelative(-1.25f, 0f, -2.125f, 0.875f)
            reflectiveQuadToRelative(-0.875f, 2.125f)
            quadToRelative(0f, 1.25f, 0.875f, 2.125f)
            reflectiveQuadToRelative(2.125f, 0.875f)
            horizontalLineToRelative(3f)
            quadToRelative(0.425f, 0f, 0.713f, 0.288f)
            reflectiveQuadTo(11f, 16f)
            quadToRelative(0f, 0.425f, -0.288f, 0.713f)
            reflectiveQuadTo(10f, 17f)
            horizontalLineTo(7f)
            moveToRelative(2f, -4f)
            quadToRelative(-0.425f, 0f, -0.713f, -0.288f)
            reflectiveQuadTo(8f, 12f)
            quadToRelative(0f, -0.425f, 0.288f, -0.713f)
            reflectiveQuadTo(9f, 11f)
            horizontalLineToRelative(0.625f)
            lineToRelative(1.975f, 2f)
            horizontalLineTo(9f)
            moveToRelative(9.5f, 2.8f)
            quadToRelative(-0.225f, -0.35f, -0.163f, -0.75f)
            reflectiveQuadToRelative(0.413f, -0.625f)
            quadToRelative(0.575f, -0.425f, 0.913f, -1.05f)
            reflectiveQuadToRelative(0.338f, -1.375f)
            quadToRelative(0f, -1.25f, -0.875f, -2.125f)
            reflectiveQuadToRelative(-2.125f, -0.875f)
            horizontalLineTo(14f)
            quadToRelative(-0.425f, 0f, -0.713f, -0.288f)
            reflectiveQuadTo(13f, 8f)
            quadToRelative(0f, -0.425f, 0.288f, -0.713f)
            reflectiveQuadTo(14f, 7f)
            horizontalLineToRelative(3f)
            quadToRelative(2.075f, 0f, 3.538f, 1.463f)
            reflectiveQuadTo(22f, 12f)
            quadToRelative(0f, 1.225f, -0.562f, 2.288f)
            reflectiveQuadTo(19.875f, 16.05f)
            quadToRelative(-0.35f, 0.225f, -0.75f, 0.163f)
            reflectiveQuadTo(18.5f, 15.8f)
                }
            }.build()
        return _LinkOff!!
    }

@Suppress("ObjectPropertyName")
private var _LinkOff: ImageVector? = null
