package com.mna.streaming.ui.anime

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.mna.streaming.network.models.ApiAdminEpisode
import com.mna.streaming.network.models.ApiAnime
import com.mna.streaming.network.models.ApiReview
import com.mna.streaming.ui.theme.*

/**
 * Enterprise-Grade Anime Detail Screen
 *
 * Implements a rich, addictive, cinema-style UI with:
 * - Parallax-style backdrop with multi-stage gradient blending
 * - Floating poster with glowing elevation and quick metadata
 * - High-impact Primary "Watch Now" action CTA
 * - Glassmorphic quick action bar (Watchlist, Trailer, Share)
 * - Segmented Navigation Tabs (Episodes, Overview, Reviews, More Like This)
 * - Smooth season selector rail and detailed episode cards with press feedback
 * - Interactive rating & review composer with star animations
 * - Animated shimmer skeleton state for instant visual feedback
 */
@Composable
fun AnimeDetailScreen(
    animeId: String,
    onBackClick: () -> Unit,
    onAnimeClick: (String) -> Unit = {}
) {
    val vm: AnimeDetailViewModel = viewModel(
        key     = animeId,
        factory = AnimeDetailViewModel.factory(animeId)
    )
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Calculate app bar opacity based on scroll offset for smooth translucent transition
    val topBarAlpha by remember {
        derivedStateOf {
            (scrollState.value / 300f).coerceIn(0f, 0.95f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        when {
            uiState.isLoading -> {
                AnimeDetailSkeleton(onBackClick = onBackClick)
            }

            uiState.anime == null && !uiState.isLoading -> {
                AnimeDetailErrorState(
                    error = uiState.error ?: "Could not load anime details",
                    onRetry = { vm.load() },
                    onBackClick = onBackClick
                )
            }

            uiState.anime != null -> {
                val anime = uiState.anime!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    AnimeDetailHeader(
                        anime = anime,
                        inWatchlist = uiState.inWatchlist,
                        isTogglingWatchlist = uiState.isTogglingWatchlist,
                        episodes = uiState.episodes,
                        onWatchlistToggle = { vm.toggleWatchlist() },
                        onEpisodeClick = { episode ->
                            launchPlayer(context, anime, episode)
                        }
                    )

                    // Detail Body Content with Tabbed View
                    AnimeDetailBody(
                        anime = anime,
                        episodesBySeason = uiState.episodesBySeason,
                        reviews = uiState.reviews,
                        inWatchlist = uiState.inWatchlist,
                        isTogglingWatchlist = uiState.isTogglingWatchlist,
                        isSubmittingReview = uiState.isSubmittingReview,
                        reviewSuccess = uiState.reviewSuccess,
                        reviewError = uiState.reviewError,
                        similarAnime = uiState.similarAnime,
                        onWatchlistToggle = { vm.toggleWatchlist() },
                        onSubmitReview = { rating, comment -> vm.submitReview(rating, comment) },
                        onDeleteReview = { reviewId -> vm.deleteReview(reviewId) },
                        onClearReviewFeedback = { vm.clearReviewFeedback() },
                        onAnimeClick = onAnimeClick,
                        onEpisodeClick = { episode ->
                            launchPlayer(context, anime, episode)
                        }
                    )
                }

                // Sticky / Collapsing Top Bar
                TopGlassNavBar(
                    title = anime.title,
                    alpha = topBarAlpha,
                    inWatchlist = uiState.inWatchlist,
                    onBackClick = onBackClick,
                    onWatchlistToggle = { vm.toggleWatchlist() },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, anime.title)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out ${anime.title} on M&A Streaming!"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                )
            }
        }
    }
}

private fun launchPlayer(
    context: android.content.Context,
    anime: ApiAnime,
    episode: ApiAdminEpisode
) {
    val intent = Intent(context, AnimePlayerActivity::class.java).apply {
        putExtra(AnimePlayerActivity.EXTRA_EPISODE_ID, episode.id)
        putExtra(
            AnimePlayerActivity.EXTRA_TITLE,
            "${anime.title} Â· S${episode.season}E${episode.episodeNumber}"
        )
    }
    context.startActivity(intent)
}

// â”€â”€ Top Glass Navigation Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun TopGlassNavBar(
    title: String,
    alpha: Float,
    inWatchlist: Boolean,
    onBackClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Color.Transparent)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Frosted Glass Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, MABorderSubtle, CircleShape)
                    .pressScaleClickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Animated Header Title (fades in cleanly as user scrolls)
            Text(
                text = title,
                color = Color.White.copy(alpha = (alpha * 1.5f).coerceAtMost(1f)),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center
            )

            // Right Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick Bookmark Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, MABorderSubtle, CircleShape)
                        .pressScaleClickable(onClick = onWatchlistToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (inWatchlist) MAGold else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, MABorderSubtle, CircleShape)
                        .pressScaleClickable(onClick = onShareClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// â”€â”€ Hero Banner Header with Floating Poster â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AnimeDetailHeader(
    anime: ApiAnime,
    inWatchlist: Boolean,
    isTogglingWatchlist: Boolean,
    episodes: List<ApiAdminEpisode>,
    onWatchlistToggle: () -> Unit,
    onEpisodeClick: (ApiAdminEpisode) -> Unit
) {
    val context = LocalContext.current
    val firstEpisode = remember(episodes) {
        episodes.sortedWith(compareBy({ it.season }, { it.episodeNumber })).firstOrNull()
    }

    val mediaTypeLabel = remember(anime.type) {
        val t = anime.type.lowercase().trim()
        when {
            t == "anime" -> "ANIME"
            t.contains("movie") -> "MOVIE"
            t.contains("series") || t.contains("tv") || t.contains("show") -> "SERIES"
            t.isNotBlank() -> t.uppercase()
            else -> "SERIES"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(410.dp)
        ) {
            // Backdrop Image
            AsyncImage(
                model = anime.bannerUrl ?: anime.posterUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Multi-Stage Ambient Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.6f),
                                0.35f to Color.Transparent,
                                0.70f to MADark.copy(alpha = 0.7f),
                                1.0f to MADark
                            )
                        )
                    )
            )

            // Bottom Content: Floating Poster + Main Title & Badges
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Floating Poster Image
                Box(
                    modifier = Modifier
                        .width(125.dp)
                        .height(180.dp)
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MABorderStrong, RoundedCornerShape(12.dp))
                        .background(MACard)
                ) {
                    AsyncImage(
                        model = anime.posterUrl ?: anime.bannerUrl,
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dynamic Type Badge Over Poster (ANIME, SERIES, MOVIE)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MARed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mediaTypeLabel,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Info Column adjacent to poster
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status & Quality Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isOngoing = anime.status == "ongoing"
                        val statusBg = if (isOngoing) MASuccess.copy(alpha = 0.2f) else MACardElevated
                        val statusText = if (isOngoing) MASuccess else MATextSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusText)
                                )
                                Text(
                                    text = if (isOngoing) "ON AIR" else "COMPLETED",
                                    color = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MACardElevated)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "HD 1080p",
                                color = MATextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Main Title
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Score & Seasons Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (anime.rating > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MAGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MAGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "%.1f".format(anime.rating),
                                    color = MAGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        anime.releaseYear?.let { year ->
                            Text(text = "$year", color = MATextSecondary, fontSize = 12.sp)
                        }

                        anime.totalSeasons?.let { seasons ->
                            Text(
                                text = "\u2022 $seasons Season${if (seasons > 1) "s" else ""}",
                                color = MATextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // â”€â”€ Primary Call to Action & Quick Action Grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Vibrant "WATCH NOW" Button
            Button(
                onClick = {
                    firstEpisode?.let { ep -> onEpisodeClick(ep) }
                        ?: Toast.makeText(context, "No episodes available yet", Toast.LENGTH_SHORT).show()
                },
                enabled = firstEpisode != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MARed,
                    contentColor = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressScaleClickable(
                        enabled = firstEpisode != null,
                        onClick = {
                            firstEpisode?.let { ep -> onEpisodeClick(ep) }
                        }
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (firstEpisode != null) "WATCH EPISODE 1" else "NO EPISODES AVAILABLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Quick Actions Bar: Watchlist & Trailer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Watchlist Toggle Button
                OutlinedButton(
                    onClick = onWatchlistToggle,
                    enabled = !isTogglingWatchlist,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (inWatchlist) MAGold else Color.White,
                        containerColor = MACard
                    ),
                    border = BorderStroke(1.dp, if (inWatchlist) MAGold.copy(alpha = 0.5f) else MABorderSubtle),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pressScaleClickable(onClick = onWatchlistToggle)
                ) {
                    if (isTogglingWatchlist) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MARed
                        )
                    } else {
                        Icon(
                            imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (inWatchlist) "Saved" else "Watchlist",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Trailer Button (if present)
                if (!anime.trailerUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(anime.trailerUrl))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            containerColor = MACard
                        ),
                        border = BorderStroke(1.dp, MABorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Trailer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€ Segmented Navigation Body View â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AnimeDetailBody(
    anime: ApiAnime,
    episodesBySeason: Map<Int, List<ApiAdminEpisode>>,
    reviews: List<ApiReview>,
    inWatchlist: Boolean,
    isTogglingWatchlist: Boolean,
    isSubmittingReview: Boolean,
    reviewSuccess: Boolean,
    reviewError: String?,
    similarAnime: List<ApiAnime>,
    onWatchlistToggle: () -> Unit,
    onSubmitReview: (Int, String?) -> Unit,
    onDeleteReview: (String) -> Unit,
    onClearReviewFeedback: () -> Unit,
    onAnimeClick: (String) -> Unit,
    onEpisodeClick: (ApiAdminEpisode) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember(episodesBySeason, reviews) {
        val epCount = episodesBySeason.values.sumOf { it.size }
        listOf(
            "Episodes${if (epCount > 0) " ($epCount)" else ""}",
            "Overview",
            "Reviews${if (reviews.isNotEmpty()) " (${reviews.size})" else ""}",
            "More Like This"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Segmented Tab Selector Rail
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MADark,
            contentColor = Color.White,
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = MABorderSubtle, thickness = 1.dp) },
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(MARed)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val active = selectedTab == index
                Tab(
                    selected = active,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (active) MARedLight else MATextSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Dynamic Tab Content View
        Crossfade(
            targetState = selectedTab,
            label = "tabCrossfade",
            animationSpec = tween(durationMillis = 200)
        ) { page ->
            when (page) {
                0 -> {
                    // EPISODES TAB
                    EpisodesTabContent(
                        episodesBySeason = episodesBySeason,
                        onEpisodeClick = onEpisodeClick
                    )
                }

                1 -> {
                    // OVERVIEW TAB
                    OverviewTabContent(
                        anime = anime
                    )
                }

                2 -> {
                    // REVIEWS TAB
                    ReviewsTabContent(
                        reviews = reviews,
                        isSubmitting = isSubmittingReview,
                        reviewSuccess = reviewSuccess,
                        reviewError = reviewError,
                        onSubmitReview = onSubmitReview,
                        onDeleteReview = onDeleteReview,
                        onClearReviewFeedback = onClearReviewFeedback
                    )
                }

                3 -> {
                    // MORE LIKE THIS TAB
                    MoreLikeThisTabContent(
                        similarAnime = similarAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// â”€â”€ Tab 0: Episodes View â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun EpisodesTabContent(
    episodesBySeason: Map<Int, List<ApiAdminEpisode>>,
    onEpisodeClick: (ApiAdminEpisode) -> Unit
) {
    if (episodesBySeason.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = MATextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No episodes released yet.",
                    color = MATextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        val seasons = remember(episodesBySeason) { episodesBySeason.keys.sorted() }
        var selectedSeason by remember { mutableIntStateOf(seasons.firstOrNull() ?: 1) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Season Selector Rail
            if (seasons.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    seasons.forEach { season ->
                        val active = selectedSeason == season
                        val bg = if (active) MARed else MACard
                        val fg = if (active) Color.White else MATextSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bg)
                                .border(
                                    1.dp,
                                    if (active) MARed else MABorderSubtle,
                                    RoundedCornerShape(20.dp)
                                )
                                .pressScaleClickable { selectedSeason = season }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Season $season",
                                color = fg,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Episode List for Selected Season
            val currentEpisodes = episodesBySeason[selectedSeason] ?: emptyList()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentEpisodes.forEach { episode ->
                    EpisodeCard(episode = episode, onClick = { onEpisodeClick(episode) })
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: ApiAdminEpisode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MACard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, MABorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Number / Thumbnail Container
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MACardElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "E${episode.episodeNumber}",
                    color = MARedLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Episode Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title ?: "Episode ${episode.episodeNumber}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Season ${episode.season}",
                        color = MATextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "\u2022",
                        color = MATextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "1080p HD",
                        color = MATextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Play Icon Action
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MARed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Episode",
                    tint = MARed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// â”€â”€ Tab 1: Overview & Cast View â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun OverviewTabContent(anime: ApiAnime) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Synopsis Section
        if (!anime.description.isNullOrBlank()) {
            Column {
                SectionTitle("Synopsis")
                Spacer(Modifier.height(6.dp))

                var expanded by remember { mutableStateOf(false) }
                Text(
                    text = anime.description,
                    color = MATextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize()
                )
                if (anime.description.length > 180) {
                    Text(
                        text = if (expanded) "Show Less" else "Read More",
                        color = MARedLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { expanded = !expanded }
                    )
                }
            }
        }

        // Genres Section
        if (anime.genres.isNotEmpty()) {
            Column {
                SectionTitle("Genres")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    anime.genres.forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MACard)
                                .border(1.dp, MABorderSubtle, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Cast & Voice Actors Section
        if (!anime.cast.isNullOrEmpty()) {
            Column {
                SectionTitle("Cast & Voice Actors")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    anime.cast.sortedBy { it.order }.take(12).forEach { member ->
                        CastMemberCard(
                            name = member.name,
                            character = member.character,
                            imageUrl = member.image
                        )
                    }
                }
            }
        }

        // Metadata Spec Sheet
        Column {
            SectionTitle("Information")
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MACard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, MABorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoRow("Status", anime.status.replaceFirstChar { it.uppercase() })
                    InfoRow("Type", anime.type)
                    anime.releaseYear?.let { InfoRow("Release Year", "$it") }
                    anime.totalSeasons?.let { InfoRow("Total Seasons", "$it") }
                    if (anime.rating > 0) InfoRow("Community Score", "%.1f / 10 (${anime.ratingCount} votes)".format(anime.rating))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MATextSecondary, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// â”€â”€ Tab 2: Reviews View â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ReviewsTabContent(
    reviews: List<ApiReview>,
    isSubmitting: Boolean,
    reviewSuccess: Boolean,
    reviewError: String?,
    onSubmitReview: (Int, String?) -> Unit,
    onDeleteReview: (String) -> Unit,
    onClearReviewFeedback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Review Submission Composer Card
        ReviewComposerCard(
            isSubmitting = isSubmitting,
            reviewSuccess = reviewSuccess,
            reviewError = reviewError,
            onSubmit = onSubmitReview,
            onClearFeedback = onClearReviewFeedback
        )

        // Reviews List Header
        SectionTitle("User Reviews (${reviews.size})")

        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reviews yet. Share your thoughts above!",
                    color = MATextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            reviews.forEach { review ->
                ReviewCard(review = review, onDelete = { onDeleteReview(review.id) })
            }
        }
    }
}

@Composable
private fun ReviewComposerCard(
    isSubmitting: Boolean,
    reviewSuccess: Boolean,
    reviewError: String?,
    onSubmit: (Int, String?) -> Unit,
    onClearFeedback: () -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(reviewSuccess) {
        if (reviewSuccess) {
            comment = ""
            selectedRating = 0
            expanded = false
            onClearFeedback()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MACard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MABorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Rate & Review",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(10.dp))

            // Interactive Star Rating Bar (10 Stars)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..10).forEach { star ->
                        val filled = star <= selectedRating
                        Text(
                            text = "\u2605",
                            color = if (filled) MAGold else MATextTertiary,
                            fontSize = 22.sp,
                            modifier = Modifier.clickable {
                                selectedRating = star
                                expanded = true
                            }
                        )
                    }
                }
                if (selectedRating > 0) {
                    Text(
                        text = "$selectedRating/10",
                        color = MAGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 1000) comment = it },
                    placeholder = { Text("Write your review (optional)...", color = MATextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MARed,
                        unfocusedBorderColor = MABorderSubtle,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MARed
                    )
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (selectedRating > 0) onSubmit(selectedRating, comment.ifBlank { null })
                    },
                    enabled = selectedRating > 0 && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MARed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Post Review", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!reviewError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(reviewError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ReviewCard(review: ApiReview, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MACard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, MABorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar & Nickname
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MARed.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.user.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = MARedLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column {
                        Text(
                            text = review.user.nickname,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(review.rating) {
                                Text("\u2605", color = MAGold, fontSize = 10.sp)
                            }
                            repeat((10 - review.rating).coerceAtLeast(0)) {
                                Text("\u2605", color = MATextTertiary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Score Badge & Delete Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${review.rating}/10",
                        color = MAGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Box {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete review",
                                tint = MATextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showDeleteConfirm,
                            onDismissRequest = { showDeleteConfirm = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete review", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel") },
                                onClick = { showDeleteConfirm = false }
                            )
                        }
                    }
                }
            }

            if (!review.comment.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    color = MATextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// â”€â”€ Tab 3: More Like This View â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MoreLikeThisTabContent(
    similarAnime: List<ApiAnime>,
    onAnimeClick: (String) -> Unit
) {
    if (similarAnime.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recommendations found.",
                color = MATextSecondary,
                fontSize = 13.sp
            )
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(similarAnime) { item ->
                SimilarAnimeCard(anime = item, onClick = { onAnimeClick(item.id) })
            }
        }
    }
}

@Composable
private fun SimilarAnimeCard(anime: ApiAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .pressScaleClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(185.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(0.5.dp, MABorderSubtle, RoundedCornerShape(10.dp))
                .background(MACard)
        ) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Rating badge overlay
            if (anime.rating > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MAGold,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "%.1f".format(anime.rating),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (anime.status == "ongoing") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MASuccess)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("ON AIR", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = anime.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = anime.releaseYear?.toString() ?: "",
                color = MATextSecondary,
                fontSize = 11.sp
            )
            anime.totalSeasons?.let { seasons ->
                Text(
                    text = "${seasons} Season${if (seasons > 1) "s" else ""}",
                    color = MATextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// â”€â”€ Shared Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )
}

@Composable
private fun CastMemberCard(name: String, character: String?, imageUrl: String?) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(1.dp, MABorderSubtle, CircleShape)
                .background(MACard)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!character.isNullOrBlank()) {
            Text(
                text = character,
                color = MATextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// â”€â”€ Skeleton Placeholder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AnimeDetailSkeleton(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Skeleton Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .shimmer()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmer()
                )
            }
        }

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

// â”€â”€ Error State View â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AnimeDetailErrorState(
    error: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MARed,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MARed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}
