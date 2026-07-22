package com.mna.streaming.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.data.model.Movie
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MATextSecondary
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onMovieClick: (Movie) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by homeViewModel.uiState.collectAsState()

    // ── Hero auto-rotation state ───────────────────────────────────────────────
    var heroIndex by remember { mutableIntStateOf(0) }
    // Advance the hero index every 5 seconds while there are multiple movies.
    LaunchedEffect(uiState.featuredMovies.size) {
        if (uiState.featuredMovies.size > 1) {
            while (true) {
                delay(5_000)
                heroIndex = (heroIndex + 1) % uiState.featuredMovies.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = MARed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { homeViewModel.loadHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = MARed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // Hero banner — auto-rotates through featuredMovies
                    if (uiState.featuredMovies.isNotEmpty()) {
                        item {
                            val movie = uiState.featuredMovies[heroIndex]
                            Box {
                                HeroBanner(
                                    movie       = movie,
                                    onPlayClick = { onMovieClick(movie) },
                                    onInfoClick = { onMovieClick(movie) }
                                )

                                // ── Dot page indicators ───────────────────────
                                // Only shown when there are multiple hero movies.
                                if (uiState.featuredMovies.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        uiState.featuredMovies.indices.forEach { i ->
                                            // Active dot is wider (pill shape); inactive dots are small circles.
                                            val dotWidth by animateDpAsState(
                                                targetValue = if (i == heroIndex) 20.dp else 6.dp,
                                                label       = "dotWidth"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .height(6.dp)
                                                    .width(dotWidth)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (i == heroIndex) Color.White
                                                        else Color.White.copy(alpha = 0.35f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Category rows
                    items(uiState.categories) { category ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text     = category.title,
                            style    = MaterialTheme.typography.titleMedium,
                            color    = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(category.movies) { movie ->
                                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Top bar overlay (always on top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = "M&A",
                color         = MARed,
                fontSize      = 26.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                }
            }
        }
    }
}

// ── Hero banner ───────────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(
    movie: Movie,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        AsyncImage(
            model          = movie.backdropUrl,
            contentDescription = movie.title,
            contentScale   = ContentScale.Crop,
            modifier       = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.2f),
                            0.5f to Color.Transparent,
                            1.0f to MADark
                        )
                    )
                )
        )

        // Content at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Genre pills
            if (movie.genres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    movie.genres.take(3).forEachIndexed { index, genre ->
                        Text(genre, color = MATextSecondary, style = MaterialTheme.typography.labelSmall)
                        if (index < movie.genres.take(3).lastIndex) {
                            Text(" • ", color = MATextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text  = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 2
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text     = movie.description,
                color    = MATextSecondary,
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick         = onPlayClick,
                    colors          = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor   = Color.Black
                    ),
                    shape           = RoundedCornerShape(6.dp),
                    contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick        = onInfoClick,
                    colors         = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape          = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("More Info")
                }
            }
        }
    }
}

// ── Movie card ────────────────────────────────────────────────────────────────

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model          = movie.posterUrl,
            contentDescription = movie.title,
            contentScale   = ContentScale.Crop,
            modifier       = Modifier
                .width(120.dp)
                .height(170.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text     = movie.title,
            color    = Color.White,
            style    = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text  = movie.year.toString(),
            color = MATextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
