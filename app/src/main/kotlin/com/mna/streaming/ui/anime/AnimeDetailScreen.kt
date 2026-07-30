package com.mna.streaming.ui.anime

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.network.models.ApiAdminEpisode
import com.mna.streaming.network.models.ApiAnime
import com.mna.streaming.network.models.ApiReview
import com.mna.streaming.ui.theme.*

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

            uiState.anime == null && !uiState.isLoading -> {
                // anime is null when loading finishes — either a real error or cache miss
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = uiState.error ?: "Could not load anime details",
                        color = MATextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.load() },
                        colors  = ButtonDefaults.buttonColors(containerColor = MARed)
                    ) { Text("Retry") }
                }
            }

            uiState.anime != null -> {
                val anime = uiState.anime!!
                AnimeDetailContent(
                    anime               = anime,
                    episodesBySeason    = uiState.episodesBySeason,
                    reviews             = uiState.reviews,
                    inWatchlist         = uiState.inWatchlist,
                    isTogglingWatchlist = uiState.isTogglingWatchlist,
                    isSubmittingReview  = uiState.isSubmittingReview,
                    reviewSuccess       = uiState.reviewSuccess,
                    reviewError         = uiState.reviewError,
                    similarAnime        = uiState.similarAnime,
                    onWatchlistToggle   = { vm.toggleWatchlist() },
                    onSubmitReview      = { rating, comment -> vm.submitReview(rating, comment) },
                    onDeleteReview      = { reviewId -> vm.deleteReview(reviewId) },
                    onClearReviewFeedback = { vm.clearReviewFeedback() },
                    onAnimeClick        = onAnimeClick,
                    onEpisodeClick      = { episode ->
                        val intent = Intent(context, AnimePlayerActivity::class.java).apply {
                            putExtra(AnimePlayerActivity.EXTRA_EPISODE_ID, episode.id)
                            putExtra(AnimePlayerActivity.EXTRA_TITLE,
                                "${anime.title} · S${episode.season}E${episode.episodeNumber}")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Back button — always on top
        IconButton(
            onClick  = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = Color.White
            )
        }
    }
}

// ── Full detail content ────────────────────────────────────────────────────────

@Composable
private fun AnimeDetailContent(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // ── Banner ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            AsyncImage(
                model              = anime.bannerUrl ?: anime.posterUrl,
                contentDescription = anime.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            // Gradient fade at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.15f),
                                0.55f to Color.Transparent,
                                1.0f to MADark
                            )
                        )
                    )
            )
            // Status + year row
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val statusColor = if (anime.status == "ongoing") Color(0xFF22C55E) else MATextSecondary
                val statusLabel = if (anime.status == "ongoing") "Ongoing" else "Completed"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                if (anime.releaseYear != null) {
                    Text(anime.releaseYear.toString(), color = MATextSecondary, fontSize = 12.sp)
                }
                if (anime.totalSeasons != null) {
                    Text("· ${anime.totalSeasons} Season${if (anime.totalSeasons > 1) "s" else ""}",
                        color = MATextSecondary, fontSize = 12.sp)
                }
            }
        }

        // ── Title + actions ────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text       = anime.title,
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(4.dp))

            // Rating row
            if (anime.rating > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null,
                        tint = MAGold, modifier = Modifier.size(16.dp))
                    Text(
                        text  = "${"%.1f".format(anime.rating)} / 10",
                        color = MAGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text  = "(${anime.ratingCount} ratings)",
                        color = MATextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Watchlist button
            OutlinedButton(
                onClick   = onWatchlistToggle,
                enabled   = !isTogglingWatchlist,
                colors    = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (inWatchlist) MAGold else Color.White
                ),
                shape     = RoundedCornerShape(8.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                if (isTogglingWatchlist) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MARed)
                } else {
                    Icon(
                        imageVector        = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(if (inWatchlist) "Saved to Watchlist" else "Add to Watchlist")
            }

            // Watch Trailer button (only shown when a trailer URL is available)
            if (!anime.trailerUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                val context = LocalContext.current
                OutlinedButton(
                    onClick  = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(anime.trailerUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Watch Trailer", style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Genre chips
            if (anime.genres.isNotEmpty()) {
                Row(
                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    anime.genres.forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MACard)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(genre.name, color = MATextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Description
            if (!anime.description.isNullOrBlank()) {
                Text(
                    text     = anime.description,
                    color    = MATextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Episodes by season ─────────────────────────────────────────────────
        if (episodesBySeason.isNotEmpty()) {
            SectionHeader("Episodes")
            episodesBySeason.entries.sortedBy { it.key }.forEach { (season, episodes) ->
                SeasonSection(season = season, episodes = episodes, onEpisodeClick = onEpisodeClick)
            }
            Spacer(Modifier.height(8.dp))
        } else {
            // No episodes available (user might not have admin access)
            Spacer(Modifier.height(4.dp))
        }

        // ── Cast ───────────────────────────────────────────────────────────────
        if (!anime.cast.isNullOrEmpty()) {
            SectionHeader("Cast")
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                anime.cast.sortedBy { it.order }.take(10).forEach { member ->
                    CastMemberCard(member.name, member.character, member.image)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── More Like This ─────────────────────────────────────────────────────
        if (similarAnime.isNotEmpty()) {
            SectionHeader("More Like This")
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(similarAnime) { item ->
                    SimilarAnimeCard(anime = item, onClick = { onAnimeClick(item.id) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Reviews ────────────────────────────────────────────────────────────
        SectionHeader("Reviews (${reviews.size})")
        ReviewSubmitCard(
            isSubmitting  = isSubmittingReview,
            reviewSuccess = reviewSuccess,
            reviewError   = reviewError,
            onSubmit      = onSubmitReview,
            onClearFeedback = onClearReviewFeedback
        )
        if (reviews.isEmpty()) {
            Text(
                text     = "No reviews yet. Be the first!",
                color    = MATextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        } else {
            reviews.forEach { review ->
                ReviewCard(review = review, onDelete = { onDeleteReview(review.id) })
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ── Similar anime card ────────────────────────────────────────────────────────

@Composable
private fun SimilarAnimeCard(anime: ApiAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model              = anime.posterUrl,
                contentDescription = anime.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .width(120.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MASurface)
            )
            // Rating badge — top-right corner
            if (anime.rating > 0) {
                Surface(
                    color    = Color.Black.copy(alpha = 0.72f),
                    shape    = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Star,
                            contentDescription = null,
                            tint               = MAGold,
                            modifier           = Modifier.size(10.dp)
                        )
                        Text(
                            text       = "%.1f".format(anime.rating),
                            color      = Color.White,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            // Ongoing indicator — bottom-left
            if (anime.status == "ongoing") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("ON AIR", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text     = anime.title,
            color    = Color.White,
            style    = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = anime.releaseYear?.toString() ?: "",
                color = MATextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            anime.totalSeasons?.let { seasons ->
                Text(
                    text  = "${seasons}S",
                    color = MATextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ── Section header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        color      = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize   = 17.sp,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

// ── Season / episodes ──────────────────────────────────────────────────────────

@Composable
private fun SeasonSection(
    season: Int,
    episodes: List<ApiAdminEpisode>,
    onEpisodeClick: (ApiAdminEpisode) -> Unit
) {
    var expanded by remember { mutableStateOf(season == 1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Season header — tap to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Season $season",
                color      = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = "${episodes.size} episode${if (episodes.size != 1) "s" else ""}",
                    color    = MATextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint               = MATextSecondary,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        if (expanded) {
            episodes.forEach { episode ->
                EpisodeRow(episode = episode, onClick = { onEpisodeClick(episode) })
                HorizontalDivider(color = MASurface, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: ApiAdminEpisode, onClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MACard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = episode.episodeNumber.toString(),
                    color      = MATextSecondary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column {
                Text(
                    text     = episode.title ?: "Episode ${episode.episodeNumber}",
                    color    = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = "E${episode.episodeNumber}",
                    color    = MATextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Icon(
            imageVector        = Icons.Default.PlayCircle,
            contentDescription = "Play",
            tint               = MARed,
            modifier           = Modifier.size(28.dp)
        )
    }
}

// ── Cast card ──────────────────────────────────────────────────────────────────

@Composable
private fun CastMemberCard(name: String, character: String?, imageUrl: String?) {
    Column(
        modifier            = Modifier.width(70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model              = imageUrl,
            contentDescription = name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MACard)
        )
        Spacer(Modifier.height(4.dp))
        Text(name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!character.isNullOrBlank()) {
            Text(character, color = MATextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Review submit card ─────────────────────────────────────────────────────────

@Composable
private fun ReviewSubmitCard(
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors   = CardDefaults.cardColors(containerColor = MACard),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Write a Review", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            // Star picker
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..10).forEach { star ->
                    Text(
                        text     = "★",
                        color    = if (star <= selectedRating) MAGold else MATextSecondary,
                        fontSize = 20.sp,
                        modifier = Modifier.clickable { selectedRating = star; expanded = true }
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value           = comment,
                    onValueChange   = { if (it.length <= 1000) comment = it },
                    placeholder     = { Text("Your thoughts… (optional)", color = MATextSecondary, fontSize = 13.sp) },
                    modifier        = Modifier.fillMaxWidth(),
                    maxLines        = 4,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MARed,
                        unfocusedBorderColor = MASurface,
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = MARed
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick  = { if (selectedRating > 0) onSubmit(selectedRating, comment.ifBlank { null }) },
                    enabled  = selectedRating > 0 && !isSubmitting,
                    colors   = ButtonDefaults.buttonColors(containerColor = MARed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Submit Review")
                    }
                }
            }
            if (!reviewError.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(reviewError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

// ── Review card ────────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(review: ApiReview, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors   = CardDefaults.cardColors(containerColor = MACard),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Avatar + nickname + stars
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.weight(1f)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MARed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = review.user.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color      = MARed,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp
                        )
                    }
                    Column {
                        Text(
                            review.user.nickname,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(review.rating) {
                                Text("★", color = MAGold, fontSize = 10.sp)
                            }
                            repeat(10 - review.rating) {
                                Text("★", color = MATextSecondary.copy(alpha = 0.3f), fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Rating score + delete button
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text       = "${review.rating}/10",
                        color      = MAGold,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                    // Delete — server enforces ownership (403 if not owner or admin)
                    Box {
                        IconButton(
                            onClick  = { showDeleteConfirm = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Delete,
                                contentDescription = "Delete review",
                                tint               = MATextSecondary.copy(alpha = 0.6f),
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded         = showDeleteConfirm,
                            onDismissRequest = { showDeleteConfirm = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Delete review", color = MaterialTheme.colorScheme.error) },
                                onClick = { showDeleteConfirm = false; onDelete() },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text    = { Text("Cancel") },
                                onClick = { showDeleteConfirm = false }
                            )
                        }
                    }
                }
            }

            if (!review.comment.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(review.comment, color = MATextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
