package com.mna.streaming.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shared timing/easing constants so hand-rolled animations across screens
 * feel like one coherent motion language instead of each screen picking its
 * own durations.
 */
object MAMotion {
    const val fast = 150
    const val medium = 300
    const val slow = 450
    val standardEasing = FastOutSlowInEasing
    val enterEasing = LinearOutSlowInEasing
    val liquidSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
    val softSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 260f)
}

/**
 * Drop-in replacement for `Modifier.clickable(onClick = onClick)` that also
 * eases the element down to [pressedScale] while pressed and springs back on
 * release. Gives cards, buttons and rows a tactile, premium feel with zero
 * new dependencies — used throughout the redesigned UI for a consistent
 * "addictive" tap response.
 */
fun Modifier.pressScaleClickable(
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MAMotion.liquidSpring,
        label = "pressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Animated diagonal shimmer brush for skeleton loading placeholders, so
 * loading cards read as "actively loading" rather than dead flat rectangles.
 * Pure Compose `Brush` + infinite transition — no shimmer/Accompanist
 * dependency required.
 */
@Composable
fun rememberShimmerBrush(
    baseColor: Color = MAGlassLow,
    highlightColor: Color = MAGlassHigh
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate + 300f, 300f)
    )
}

/** Convenience modifier: fills the element with an animated shimmer brush. */
@Composable
fun Modifier.shimmer(baseColor: Color = MAGlassLow, highlightColor: Color = MAGlassHigh): Modifier =
    this.background(rememberShimmerBrush(baseColor, highlightColor))
