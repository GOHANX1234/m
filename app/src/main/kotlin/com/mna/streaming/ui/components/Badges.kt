package com.mna.streaming.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.ui.theme.MAGold
import com.mna.streaming.ui.theme.MARadius

/**
 * Small "★ 8.4" chip overlaid on poster art. [compact] shrinks it for the
 * dense grids used in Anime/Search; the larger variant reads better on hero
 * banners and detail headers.
 */
@Composable
fun RatingBadge(
    rating: Double,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    GlassCapsule(
        tint = MAGold,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = if (compact) 6.dp else 8.dp,
            vertical = if (compact) 3.dp else 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MAGold,
                modifier = Modifier.size(if (compact) 10.dp else 13.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = String.format("%.1f", rating),
                color = Color.White,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Solid-tint content-type label, e.g. "MOVIE" / "ANIME" / "SERIES", used on
 * poster overlays, watch-history rows and search results.
 */
@Composable
fun LabelBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCapsule(
        tint = color,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier
        )
    }
}

/**
 * Outlined, translucent status pill used for request/admin states
 * (pending / in progress / fulfilled / rejected, etc.).
 */
@Composable
fun StatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCapsule(
        tint = color,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
        )
    }
}
