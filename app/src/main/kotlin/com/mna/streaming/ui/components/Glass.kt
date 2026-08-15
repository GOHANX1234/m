package com.mna.streaming.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mna.streaming.ui.theme.MAGlass
import com.mna.streaming.ui.theme.MAGlassHighlight
import com.mna.streaming.ui.theme.MAGlassLow
import com.mna.streaming.ui.theme.MAGlassShadow
import com.mna.streaming.ui.theme.MARadius

enum class GlassLevel { Low, Regular, Elevated }

/**
 * Core Liquid Glass surface. Compose cannot sample a true live backdrop on every
 * supported Android version, so this builds the same depth cues from translucent
 * layers, adaptive tint, a directional specular edge, inner hairline and shadow.
 * Poster-derived colors can be passed through [tint] to make the material adapt.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MARadius.lg),
    level: GlassLevel = GlassLevel.Regular,
    tint: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val base = when (level) {
        GlassLevel.Low -> MAGlassLow
        GlassLevel.Regular -> MAGlass
        GlassLevel.Elevated -> Color.White.copy(alpha = 0.19f)
    }
    val shadow = if (level == GlassLevel.Elevated) 24.dp else 12.dp
    val fill = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (level == GlassLevel.Low) 0.10f else 0.17f),
            tint.copy(alpha = if (tint == Color.Transparent) 0f else 0.18f),
            base
        )
    )

    Box(
        modifier = modifier
            .shadow(shadow, shape, ambientColor = MAGlassShadow, spotColor = MAGlassShadow)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(0.75.dp, MAGlassHighlight.copy(alpha = 0.48f)), shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    tint: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    content: @Composable BoxScope.() -> Unit
) = GlassSurface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    level = GlassLevel.Low,
    tint = tint,
    contentPadding = contentPadding,
    content = content
)
