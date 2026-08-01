package com.mna.streaming.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MARadius
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.pressScaleClickable
import com.mna.streaming.ui.theme.shimmer

/**
 * Shared poster-card renderer used internally by `MovieCard`, `AnimeCard`,
 * `SeriesCard` and search-result cards so every rectangular poster in the
 * app â€” Home rails, Anime grid, Search results, Actor filmography â€” shares
 * identical corners, badge placement, scrim and press feedback. Callers keep
 * their own public composable names/signatures; only the pixels come from
 * here.
 */
@Composable
fun MediaPosterCard(
    posterUrl: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 112.dp,
    aspectRatio: Float = 2f / 3f,
    rating: Double? = null,
    badgeText: String? = null,
    badgeColor: Color = MARed,
    subtitle: String? = null,
    showTitle: Boolean = true,
    titleMaxLines: Int = 1
) {
    val cardModifier = if (width != Dp.Unspecified) modifier.width(width) else modifier
    Column(
        modifier = cardModifier.pressScaleClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(MARadius.sm))
                .background(MACard)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(posterUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Soft top scrim so corner badges stay legible over bright poster art.
            if (rating != null || badgeText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
            }

            if (badgeText != null) {
                LabelBadge(
                    text = badgeText,
                    color = badgeColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }

            if (rating != null && rating > 0) {
                RatingBadge(
                    rating = rating,
                    compact = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }

        if (showTitle) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MATextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Shimmering placeholder shaped like [MediaPosterCard], shown in horizontal
 * rails and grids while the first page of data is loading â€” reads as
 * "actively fetching" instead of a blank screen or a single spinner.
 */
@Composable
fun MediaCardSkeleton(
    modifier: Modifier = Modifier,
    width: Dp = 112.dp,
    aspectRatio: Float = 2f / 3f,
    showTitle: Boolean = true
) {
    Column(modifier = modifier.width(width)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(MARadius.sm))
                .shimmer()
        )
        if (showTitle) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(11.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmer()
            )
        }
    }
}
