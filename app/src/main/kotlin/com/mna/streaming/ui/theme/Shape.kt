package com.mna.streaming.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale used across the app. Prefer these named radii over
 * hand-picked `RoundedCornerShape(N.dp)` values so cards, sheets, chips and
 * buttons read as one consistent, "enterprise-grade" system rather than a
 * grab-bag of slightly different roundnesses.
 */
object MARadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 26.dp
    val xxl = 34.dp
}

/** Wired into [MATheme] so Material3 components without an explicit shape inherit these. */
val MAShapes = Shapes(
    extraSmall = RoundedCornerShape(MARadius.xs),
    small = RoundedCornerShape(MARadius.sm),
    medium = RoundedCornerShape(MARadius.md),
    large = RoundedCornerShape(MARadius.lg),
    extraLarge = RoundedCornerShape(MARadius.xxl)
)
