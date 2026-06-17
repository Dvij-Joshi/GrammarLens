package com.example.grammarlens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Neumorphic Color Palette
object NeuColors {
    val Background = Color(0xFFE0E5EC)
    val LightShadow = Color(0xFFFFFFFF)
    val DarkShadow = Color(0xFFA3B1C6)
    val TextMain = Color(0xFF4A5568)
    val Primary = Color(0xFF6B46C1) // Purple accent
}

fun Modifier.neumorphic(
    lightShadow: Color = NeuColors.LightShadow,
    darkShadow: Color = NeuColors.DarkShadow,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp
) = this.drawBehind {
    val radiusPx = cornerRadius.toPx()
    val elevationPx = elevation.toPx()
    
    val paintDark = Paint().apply {
        color = Color.Transparent
        asFrameworkPaint().apply {
            setShadowLayer(
                elevationPx * 1.5f,
                elevationPx / 2,
                elevationPx / 2,
                darkShadow.copy(alpha = 0.5f).toArgb()
            )
        }
    }
    val paintLight = Paint().apply {
        color = Color.Transparent
        asFrameworkPaint().apply {
            setShadowLayer(
                elevationPx * 1.5f,
                -elevationPx / 2,
                -elevationPx / 2,
                lightShadow.toArgb()
            )
        }
    }

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            radiusPx, radiusPx, paintDark
        )
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            radiusPx, radiusPx, paintLight
        )
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neumorphic(cornerRadius = cornerRadius, elevation = elevation)
            .clip(RoundedCornerShape(cornerRadius))
            .background(NeuColors.Background),
        content = content
    )
}
