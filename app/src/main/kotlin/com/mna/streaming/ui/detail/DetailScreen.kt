package com.mna.streaming.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.network.models.ApiCastMember
import com.mna.streaming.network.models.ApiReview
import com.mna.streaming.ui.player.PlayerActivity
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MAGold
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MATextSecondary

@Composable
fun DetailScreen(
    movieId: String,
    onBackClick: () -> Unit
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

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    // ── Backdrop ────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        AsyncImage(
                            model          = movie.backdropUrl,
                            contentDescription = movie.title,
                            contentScale   = ContentScale.Crop,
                            modifier       = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(listOf(Color.Transparent, MADark))
                                )
                        )
                        // Central play button
                        IconButton(
                            onClick  = {
                                // Record locally before launching the player so the
                                // Profile → Watch History tab shows this film immediately.
                                detailViewModel.recordWatched()
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra(PlayerActivity.EXTRA_MOVIE_ID, movie.id)
                                    putExtra(PlayerActivity.EXTRA_TITLE,    movie.title)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .background(MARed, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint     = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // ── Info section ────────────────────────────────────────
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment    = Alignment.Top
                        ) {
                            Text(
                                text       = movie.title,
                                style      = MaterialTheme.typography.headlineMedium,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.weight(1f)
                            )
                            // Watchlist bookmark button
                            IconButton(
                                onClick  = { detailViewModel.toggleWatchlist() },
                                enabled  = !uiState.isWatchlistLoading
                            ) {
                                Icon(
                                    imageVector = if (uiState.inWatchlist)
                                        Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (uiState.inWatchlist) "Remove from watchlist" else "Add to watchlist",
                                    tint     = if (uiState.inWatchlist) MARed else Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Meta row: rating • year • duration
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MAGold, modifier = Modifier.size(16.dp))
                            Text(
                                text       = String.format("%.1f", movie.rating),
                                color      = MAGold,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("•", color = MATextSecondary)
                            Text(movie.year.toString(), color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("•", color = MATextSecondary)
                            Text(movie.durationFormatted, color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("•", color = MATextSecondary)
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = MATextSecondary, modifier = Modifier.size(14.dp))
                            Text("${movie.views}", color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(10.dp))

                        // Genre chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            movie.genres.forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MACard
                                ) {
                                    Text(
                                        text     = genre,
                                        color    = Color.White,
                                        style    = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text      = movie.description,
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MATextSecondary,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )

                        Spacer(Modifier.height(24.dp))

                        // ── Play Now button ─────────────────────────────────
                        Button(
                            onClick  = {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra(PlayerActivity.EXTRA_MOVIE_ID, movie.id)
                                    putExtra(PlayerActivity.EXTRA_TITLE,    movie.title)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MARed,
                                contentColor   = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play Now", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Watch Trailer button (only shown when a trailer URL is available)
                        if (movie.trailerUrl != null) {
                            OutlinedButton(
                                onClick  = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.trailerUrl))
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
                            Spacer(Modifier.height(8.dp))
                        }

                        // Rate button
                        OutlinedButton(
                            onClick  = { detailViewModel.showRatingDialog() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MAGold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text  = if (uiState.userReview != null) "Update Your Rating" else "Rate This Movie",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // ── Cast ────────────────────────────────────────────
                        if (uiState.cast.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text       = "Cast",
                                color      = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style      = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier              = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.cast.take(10).forEach { member ->
                                    CastCard(member)
                                }
                            }
                        }

                        // ── Reviews ─────────────────────────────────────────
                        if (uiState.reviews.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text       = "Reviews (${uiState.reviews.size})",
                                color      = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style      = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            uiState.reviews.take(10).forEach { review ->
                                val isOwnReview = review.id == uiState.userReview?.id
                                ReviewCard(
                                    review   = review,
                                    onEdit   = if (isOwnReview) { { detailViewModel.showRatingDialog() } } else null,
                                    onDelete = if (isOwnReview) { { detailViewModel.deleteReview(review.id) } } else null
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }

        // Back button (always visible)
        IconButton(
            onClick  = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }

    // ── Rating dialog ─────────────────────────────────────────────────────────
    if (uiState.showRatingDialog) {
        RatingDialog(
            currentRating = uiState.userReview?.rating ?: 0,
            currentComment = uiState.userReview?.comment ?: "",
            onDismiss     = { detailViewModel.dismissRatingDialog() },
            onSubmit      = { rating, comment -> detailViewModel.submitReview(rating, comment) }
        )
    }
}

// ── Cast card ─────────────────────────────────────────────────────────────────

@Composable
private fun CastCard(member: ApiCastMember) {
    Column(
        modifier            = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model          = member.image?.takeIf { it.isNotBlank() },
            contentDescription = member.name,
            contentScale   = ContentScale.Crop,
            modifier       = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MACard)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = member.name,
            color     = Color.White,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 2,
            textAlign = TextAlign.Center,
            fontSize  = 10.sp
        )
        Text(
            text      = member.character,
            color     = MATextSecondary,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
            textAlign = TextAlign.Center,
            fontSize  = 9.sp
        )
    }
}

// ── Review card ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(
    review: ApiReview,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = MACard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = review.user.nickname,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodySmall,
                    modifier   = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MAGold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text       = "${review.rating}/10",
                        color      = MAGold,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    // 3-dot menu shown only for the current user's own review
                    if (onEdit != null || onDelete != null) {
                        Box {
                            // No size() on IconButton — keeps the default 48dp touch target.
                            // The visual icon is constrained to 16dp inside.
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Review options",
                                    tint     = MATextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded          = showMenu,
                                onDismissRequest  = { showMenu = false }
                            ) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text         = { Text("Edit") },
                                        onClick      = { showMenu = false; onEdit() },
                                        leadingIcon  = { Icon(Icons.Default.Edit, contentDescription = null) }
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
            }
            if (!review.comment.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = review.comment,
                    color = MATextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ── Rating dialog ─────────────────────────────────────────────────────────────

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
        title = {
            Text("Rate this movie", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // Star rating row 1–10
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
                    maxLines     = 4,
                    modifier     = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating, comment.takeIf { it.isNotBlank() }) },
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

