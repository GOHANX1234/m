package com.mna.streaming.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.network.models.ApiCastMember
import com.mna.streaming.network.models.ApiReview
import com.mna.streaming.ui.components.MAPrimaryButton
import com.mna.streaming.ui.home.MovieCard
import com.mna.streaming.ui.player.PlayerActivity
import com.mna.streaming.ui.theme.MABorderSubtle
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MAGold
import com.mna.streaming.ui.theme.MAMotion
import com.mna.streaming.ui.theme.MARadius
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASpacing
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.pressScaleClickable

@Composable
fun DetailScreen(
    movieId: String,
    onBackClick: () -> Unit,
    onMovieClick: (String) -> Unit = {}
) {
    val detailViewModel: DetailViewModel = viewModel(
        key     = movieId,
        factory = DetailViewModel.factory(movieId)
    )
    val uiState by detailViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color    = MARed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Column(
                    modifier              = Modifier.align(Alignment.Center),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error ?: "Failed to load", color = MATextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { detailViewModel.load() },
                        colors  = ButtonDefaults.buttonColors(containerColor = MARed)
                    ) { Text("Retry") }
                }
            }

            uiState.movie != null -> {
                val movie = uiState.movie!!

                var contentVisible by remember(movieId) { mutableStateOf(false) }
                LaunchedEffect(movieId) { contentVisible = true }

                AnimatedVisibility(
                    visible = contentVisible,
                    enter   = fadeIn(tween(MAMotion.slow))
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                        // â”€â”€ Backdrop â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        ) {
                            AsyncImage(
                                model              = movie.backdropUrl,
                                contentDescription = movie.title,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            // Layered scrim: dark enough at top for the back button,
                            // fully transparent mid-image, then melts into the solid
                            // background so the overlapping content sheet below has
                            // a seamless edge instead of a hard color seam.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f  to Color.Black.copy(alpha = 0.55f),
                                                0.32f to Color.Transparent,
                                                0.74f to MADark.copy(alpha = 0.55f),
                                                1.0f  to MADark
                                            )
                                        )
                                    )
                            )
                            // Central play button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(68.dp)
                                    .background(Color.Black.copy(alpha = 0.28f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .pressScaleClickable(pressedScale = 0.9f) {
                                        detailViewModel.recordWatched()
                                        val intent = Intent(context, PlayerActivity::class.java).apply {
                                            putExtra(PlayerActivity.EXTRA_MOVIE_ID, movie.id)
                                            putExtra(PlayerActivity.EXTRA_TITLE,    movie.title)
                                        }
                                        context.startActivity(intent)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint               = Color.White,
                                    modifier           = Modifier.size(34.dp)
                                )
                            }
                        }

                        // â”€â”€ Content sheet â€” overlaps the backdrop's rounded top
                        // corners for a layered, premium "sliding sheet" look.
                        Column(
                            modifier = Modifier
                                .offset(y = (-20).dp)
                                .clip(RoundedCornerShape(topStart = MARadius.xl, topEnd = MARadius.xl))
                                .background(MADark)
                                .padding(horizontal = MASpacing.lg)
                                .padding(top = MASpacing.lg)
                        ) {

                            Text(
                                text       = movie.title,
                                style      = MaterialTheme.typography.headlineMedium,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(MASpacing.sm))

                            // Meta row: rating â€¢ year â€¢ duration â€¢ views
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MASpacing.sm)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MAGold, modifier = Modifier.size(15.dp))
                                Text(
                                    text       = String.format("%.1f", movie.rating),
                                    color      = MAGold,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                MetaDot()
                                Text(movie.year.toString(), color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                                MetaDot()
                                Text(movie.durationFormatted, color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                                MetaDot()
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = MATextSecondary, modifier = Modifier.size(13.dp))
                                Text("${movie.views}", color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                            }

                            if (movie.genres.isNotEmpty()) {
                                Spacer(Modifier.height(MASpacing.md))
                                Row(
                                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(MASpacing.xs)
                                ) {
                                    movie.genres.forEach { genre ->
                                        Surface(
                                            shape  = RoundedCornerShape(50),
                                            color  = Color.Transparent,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MABorderSubtle)
                                        ) {
                                            Text(
                                                text     = genre,
                                                color    = MATextSecondary,
                                                style    = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(MASpacing.lg))

                            // â”€â”€ Expandable description â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                            ExpandableDescription(movie.description)

                            Spacer(Modifier.height(MASpacing.xl))

                            // â”€â”€ Primary CTA â€” full-width â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                            MAPrimaryButton(
                                text    = "Play Now",
                                icon    = Icons.Default.PlayArrow,
                                onClick = {
                                    detailViewModel.recordWatched()
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra(PlayerActivity.EXTRA_MOVIE_ID, movie.id)
                                        putExtra(PlayerActivity.EXTRA_TITLE,    movie.title)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                height   = 54.dp
                            )

                            Spacer(Modifier.height(MASpacing.md))

                            // â”€â”€ Secondary actions â€” equal-weight columns â”€â”€â”€â”€â”€â”€
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                DetailActionButton(
                                    icon      = if (uiState.inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    label     = if (uiState.inWatchlist) "In List" else "My List",
                                    tint      = if (uiState.inWatchlist) MARed else Color.White,
                                    isLoading = uiState.isWatchlistLoading,
                                    onClick   = { detailViewModel.toggleWatchlist() }
                                )

                                DetailActionButton(
                                    icon    = if (uiState.userReview != null) Icons.Default.Star else Icons.Default.StarBorder,
                                    label   = if (uiState.userReview != null) "Rated" else "Rate",
                                    tint    = if (uiState.userReview != null) MAGold else Color.White,
                                    onClick = { detailViewModel.showRatingDialog() }
                                )

                                if (movie.trailerUrl != null) {
                                    DetailActionButton(
                                        icon    = Icons.Default.PlayCircleOutline,
                                        label   = "Trailer",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.trailerUrl))
                                            context.startActivity(intent)
                                        }
                                    )
                                } else {
                                    // Keep row balanced when no trailer
                                    Spacer(Modifier.width(56.dp))
                                }
                            }

                            // â”€â”€ Cast â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                            if (uiState.cast.isNotEmpty()) {
                                Spacer(Modifier.height(MASpacing.xxl))
                                Text(
                                    text       = "Cast",
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style      = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(MASpacing.md))
                                Row(
                                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(MASpacing.md)
                                ) {
                                    uiState.cast.take(10).forEach { member ->
                                        CastCard(member)
                                    }
                                }
                            }

                            // â”€â”€ More Like This â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                            if (uiState.similarMovies.isNotEmpty()) {
                                Spacer(Modifier.height(MASpacing.xxl))
                                Text(
                                    text       = "More Like This",
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style      = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(MASpacing.md))
                                LazyRow(
                                    contentPadding        = PaddingValues(end = MASpacing.sm),
                                    horizontalArrangement = Arrangement.spacedBy(MASpacing.sm)
                                ) {
                                    items(uiState.similarMovies) { similar ->
                                        MovieCard(
                                            movie   = similar,
                                            onClick = { onMovieClick(similar.id) }
                                        )
                                    }
                                }
                            }

                            // â”€â”€ Reviews â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                            if (uiState.reviews.isNotEmpty()) {
                                Spacer(Modifier.height(MASpacing.xxl))
                                Text(
                                    text       = "Reviews (${uiState.reviews.size})",
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style      = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(MASpacing.md))
                                uiState.reviews.take(10).forEach { review ->
                                    val isOwnReview = review.id == uiState.userReview?.id
                                    ReviewCard(
                                        review   = review,
                                        onEdit   = if (isOwnReview) { { detailViewModel.showRatingDialog() } } else null,
                                        onDelete = if (isOwnReview) { { detailViewModel.deleteReview(review.id) } } else null
                                    )
                                    Spacer(Modifier.height(MASpacing.sm))
                                }
                            }

                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }
        }

        // Back button (always visible, frosted glass style)
        IconButton(
            onClick  = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(MASpacing.md)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint     = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // â”€â”€ Rating dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (uiState.showRatingDialog) {
        RatingDialog(
            currentRating = uiState.userReview?.rating ?: 0,
            currentComment = uiState.userReview?.comment ?: "",
            onDismiss     = { detailViewModel.dismissRatingDialog() },
            onSubmit      = { rating, comment -> detailViewModel.submitReview(rating, comment) }
        )
    }
}

// â”€â”€ Small "â€¢" separator used in meta rows â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MetaDot() {
    Text("\u2022", color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
}

// â”€â”€ Expandable synopsis â€” collapses long descriptions to 3 lines with a
// tappable "Read more" / "Show less" affordance instead of always showing
// the full block of text. â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ExpandableDescription(description: String) {
    var expanded by remember { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clickable(enabled = overflowing || expanded) { expanded = !expanded }
            .animateContentSize(animationSpec = tween(MAMotion.medium))
    ) {
        Text(
            text        = description,
            style       = MaterialTheme.typography.bodyMedium,
            color       = MATextSecondary,
            lineHeight  = MaterialTheme.typography.bodyMedium.lineHeight,
            maxLines    = if (expanded) Int.MAX_VALUE else 3,
            overflow    = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded && result.hasVisualOverflow) overflowing = true
            }
        )
        if (overflowing) {
            Spacer(Modifier.height(4.dp))
            Text(
                text       = if (expanded) "Show less" else "Read more",
                color      = MARed,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// â”€â”€ Compact icon+label secondary action (My List, Rate, Trailer) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    isLoading: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier             = Modifier
            .width(56.dp)
            .pressScaleClickable(enabled = !isLoading, onClick = onClick)
    ) {
        Box(
            modifier         = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MACard),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text     = label,
            color    = MATextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// â”€â”€ Cast card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun CastCard(member: ApiCastMember) {
    Column(
        modifier            = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MACard)
                .border(1.dp, MABorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val image = member.image?.takeIf { it.isNotBlank() }
            if (image != null) {
                AsyncImage(
                    model              = image,
                    contentDescription = member.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = member.name,
                    tint               = MATextSecondary,
                    modifier           = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text      = member.name,
            color     = Color.White,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(76.dp)
        )
        Text(
            text      = member.character,
            color     = MATextSecondary,
            fontSize  = 10.sp,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(76.dp)
        )
    }
}

// â”€â”€ Review card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ReviewCard(
    review: ApiReview,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(MARadius.md),
        color    = MACard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MASpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                // Initial-letter avatar
                Box(
                    modifier         = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MARed.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = review.user.nickname.take(1).uppercase(),
                        color      = MARed,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                }
                Spacer(Modifier.width(MASpacing.sm))
                Text(
                    text       = review.user.nickname,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodySmall,
                    modifier   = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(50), color = MAGold.copy(alpha = 0.14f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MAGold, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text       = "${review.rating}/10",
                            color      = MAGold,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // 3-dot menu shown only for the current user's own review
                if (onEdit != null || onDelete != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Review options",
                                tint     = MATextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onEdit != null) {
                                DropdownMenuItem(
                                    text        = { Text("Edit") },
                                    onClick     = { showMenu = false; onEdit() },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text        = { Text("Delete", color = MARed) },
                                    onClick     = { showMenu = false; onDelete() },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MARed) }
                                )
                            }
                        }
                    }
                }
            }
            if (!review.comment.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = review.comment,
                    color    = MATextSecondary,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 42.dp)
                )
            }
        }
    }
}

// â”€â”€ Rating dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun RatingDialog(
    currentRating: Int,
    currentComment: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String?) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(if (currentRating > 0) currentRating else 5) }
    var comment by remember { mutableStateOf(currentComment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MACard,
        shape            = RoundedCornerShape(MARadius.lg),
        title = {
            Text("Rate this movie", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // Star rating row 1â€“10
                Text("Rating: $selectedRating / 10", color = MATextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                // SpaceEvenly distributes stars across the full dialog width so
                // all 10 are always visible regardless of screen size.
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..10).forEach { n ->
                        Icon(
                            imageVector        = if (n <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$n stars",
                            tint               = MAGold,
                            modifier           = Modifier
                                .size(24.dp)
                                .clickable { selectedRating = n }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value        = comment,
                    onValueChange = { if (it.length <= 1000) comment = it },
                    label        = { Text("Comment (optional)", color = MATextSecondary) },
                    colors       = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MARed,
                        unfocusedBorderColor = MATextSecondary
                    ),
                    shape        = RoundedCornerShape(MARadius.sm),
                    maxLines     = 4,
                    modifier     = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating, comment.takeIf { it.isNotBlank() }) },
                shape   = RoundedCornerShape(MARadius.sm),
                colors  = ButtonDefaults.buttonColors(containerColor = MARed)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MATextSecondary)
            }
        }
    )
}
