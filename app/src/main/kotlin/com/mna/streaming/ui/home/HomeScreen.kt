package com.mna.streaming.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.data.model.ActorItem
import com.mna.streaming.data.model.FeaturedItem
import com.mna.streaming.data.model.Movie
import com.mna.streaming.network.models.ApiAnime
import com.mna.streaming.ui.components.MAErrorState
import com.mna.streaming.ui.components.MediaCardSkeleton
import com.mna.streaming.ui.components.MediaPosterCard
import com.mna.streaming.ui.components.SectionHeader
import com.mna.streaming.ui.theme.MAAccentSeries
import com.mna.streaming.ui.theme.MABorderSubtle
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MAGold
import com.mna.streaming.ui.theme.MAMotion
import com.mna.streaming.ui.theme.MARadius
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASurface
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.pressScaleClickable
import com.mna.streaming.ui.theme.shimmer
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onMovieClick:   (Movie) -> Unit,
    onSearchClick:  () -> Unit,
    onProfileClick: () -> Unit    = {},
    onActorClick:   (String) -> Unit = {},
    onSeriesClick:  (String) -> Unit = {},
    homeViewModel:  HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // ── Hero auto-rotation ─────────────────────────────────────────────────────
    var heroIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.featuredItems.size) {
        if (uiState.featuredItems.size > 1) {
            while (true) {
                delay(5_000)
                heroIndex = (heroIndex + 1) % uiState.featuredItems.size
            }
        }
    }

    // Top bar gains a solid backdrop once the user scrolls past the hero,
    // instead of sitting on a hard-coded scrim the whole time.
    val topBarAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 260f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        when {
            uiState.isLoading -> HomeLoadingSkeleton()

            uiState.error != null -> {
                MAErrorState(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { homeViewModel.loadHome() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {

                    // ── Hero banner ─────────────────────────────────────────
                    if (uiState.featuredItems.isNotEmpty()) {
                        item {
                            Box {
                                Crossfade(
                                    targetState = heroIndex,
                                    animationSpec = tween(550),
                                    label = "heroCrossfade"
                                ) { index ->
                                    val featuredItem = uiState.featuredItems.getOrNull(index)
                                        ?: uiState.featuredItems.first()
                                    HeroBanner(
                                        item        = featuredItem,
                                        onPlayClick = {
                                            when (featuredItem) {
                                                is FeaturedItem.MovieFeatured  -> onMovieClick(featuredItem.movie)
                                                is FeaturedItem.SeriesFeatured -> onSeriesClick(featuredItem.series.id)
                                            }
                                        },
                                        onInfoClick = {
                                            when (featuredItem) {
                                                is FeaturedItem.MovieFeatured  -> onMovieClick(featuredItem.movie)
                                                is FeaturedItem.SeriesFeatured -> onSeriesClick(featuredItem.series.id)
                                            }
                                        }
                                    )
                                }
                                if (uiState.featuredItems.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        uiState.featuredItems.indices.forEach { i ->
                                            val dotWidth by animateDpAsState(
                                                targetValue = if (i == heroIndex) 20.dp else 6.dp,
                                                animationSpec = tween(MAMotion.medium, easing = MAMotion.standardEasing),
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

                    // ── Genre filter chips ──────────────────────────────────
                    if (uiState.availableGenres.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(14.dp))
                            GenreChipsRow(
                                genres        = uiState.availableGenres,
                                selectedGenre = uiState.selectedGenre,
                                onGenreSelect = { homeViewModel.selectGenre(it) }
                            )
                        }
                    }

                    // ── Movie content ─────────────────────────────────────────
                    // If a genre is selected, show a single filtered row.
                    // Otherwise show the standard New Releases / Popular / Top Rated rows.
                    val selectedGenre = uiState.selectedGenre
                    if (selectedGenre != null) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            SectionHeader(title = selectedGenre)
                            Spacer(Modifier.height(10.dp))
                            if (uiState.isGenreLoading) {
                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    userScrollEnabled      = false
                                ) {
                                    items(5) { MediaCardSkeleton() }
                                }
                            } else if (uiState.genreMovies.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text  = "No movies found for \"$selectedGenre\"",
                                        color = MATextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(uiState.genreMovies, key = { it.id }) { movie ->
                                        MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.categories) { category ->
                            Spacer(Modifier.height(20.dp))
                            SectionHeader(
                                title = category.title,
                                subtitle = "${category.movies.size} titles"
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding        = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(category.movies, key = { it.id }) { movie ->
                                    MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                                }
                            }
                        }
                    }

                    // ── Web Series section ────────────────────────────────
                    if (uiState.webSeries.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            SectionHeader(title = "Web Series", subtitle = "${uiState.webSeries.size} shows")
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding        = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.webSeries, key = { it.id }) { series ->
                                    SeriesCard(series = series, onClick = { onSeriesClick(series.id) })
                                }
                            }
                        }
                    }

                    // ── Top Actors section (bottom) ────────────────────────
                    if (uiState.actors.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            SectionHeader(title = "Top Actors", subtitle = "${uiState.actors.size} stars")
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding        = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.actors, key = { it.rank }) { actor ->
                                    ActorCard(actor = actor, onClick = { onActorClick(actor.name) })
                                }
                            }
                        }
                    }

                    // Extra scroll room keeps the last actor cards and names
                    // clear of the floating bottom navigation bar.
                    item { Spacer(Modifier.height(170.dp)) }
                }
            }
        }

        // ── Top bar overlay ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MADark.copy(alpha = topBarAlpha))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
}

// ── Loading skeleton ─────────────────────────────────────────────────────────

@Composable
private fun HomeLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(56.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(360.dp)
                .clip(RoundedCornerShape(MARadius.lg))
                .shimmer()
        )
        Spacer(Modifier.height(28.dp))
        repeat(3) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(130.dp)
                    .height(15.dp)
                    .clip(RoundedCornerShape(MARadius.xs))
                    .shimmer()
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled      = false
            ) {
                items(5) { MediaCardSkeleton() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Genre filter chips ────────────────────────────────────────────────────────

@Composable
private fun GenreChipsRow(
    genres:        List<String>,
    selectedGenre: String?,
    onGenreSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GenreChip(
                label    = "All",
                selected = selectedGenre == null,
                onClick  = { onGenreSelect(null) }
            )
        }
        items(genres) { genre ->
            GenreChip(
                label    = genre,
                selected = selectedGenre == genre,
                onClick  = { onGenreSelect(genre) }
            )
        }
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = if (selected) MARed else Color.Transparent,
        border  = if (selected) null else BorderStroke(1.dp, MABorderSubtle),
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text       = label,
                color      = if (selected) Color.White else MATextSecondary,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ── Netflix-style numbered actor card ─────────────────────────────────────────
//
// Layout:  [ big number ][ poster image ]
//                  [ actor name ]
//
// The large rank number sits at the bottom-left behind the image, creating
// that recognisable "Top 10 / Top Actors" look.

@Composable
fun ActorCard(actor: ActorItem, onClick: () -> Unit) {
    val imageWidth  = 84.dp
    val imageHeight = 118.dp
    val numberPad   = 30.dp   // how much the number sticks out to the left of the image

    Column(
        modifier            = Modifier
            .width(imageWidth + numberPad)
            .pressScaleClickable(onClick = onClick),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
        ) {
            // ── Large rank number — drawn first so image sits on top of it ──
            Text(
                text       = actor.rank.toString(),
                fontSize   = 72.sp,
                fontWeight = FontWeight.Black,
                color      = Color.White,
                lineHeight = 72.sp,
                style      = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(2f, 3f),
                        blurRadius = 10f
                    )
                ),
                modifier   = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp)
            )

            // ── Actor photo — overlaps the right half of the number ──────────
            if (actor.image != null) {
                AsyncImage(
                    model              = actor.image,
                    contentDescription = actor.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .width(imageWidth)
                        .height(imageHeight)
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(MARadius.md))
                )
            } else {
                // Placeholder when no photo is available
                Box(
                    modifier         = Modifier
                        .width(imageWidth)
                        .height(imageHeight)
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(MARadius.md))
                        .background(MASurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = actor.name,
                        tint               = MATextSecondary,
                        modifier           = Modifier.size(36.dp)
                    )
                }
            }
        }

        // ── Actor name — right-aligned under the image ────────────────────
        Spacer(Modifier.height(6.dp))
        Text(
            text     = actor.name,
            color    = Color.White,
            style    = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(imageWidth)
        )
    }
}

// ── Hero banner ───────────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(
    item:        FeaturedItem,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    // Extract display data from whichever content type this slot holds
    val backdropUrl: String?
    val genres:      List<String>
    val title:       String
    val rating:      Double
    val year:        Int?
    val metaExtra:   String?   // duration for movies, season count for series
    val description: String

    when (item) {
        is FeaturedItem.MovieFeatured -> {
            val m   = item.movie
            backdropUrl = m.backdropUrl
            genres      = m.genres
            title       = m.title
            rating      = m.rating
            year        = m.year
            metaExtra   = if (m.durationSeconds > 0) m.durationFormatted else null
            description = m.description
        }
        is FeaturedItem.SeriesFeatured -> {
            val s   = item.series
            backdropUrl = s.bannerUrl ?: s.posterUrl
            genres      = s.genres.map { it.name }
            title       = s.title
            rating      = s.rating
            year        = s.releaseYear
            metaExtra   = s.totalSeasons?.let { n -> "$n Season${if (n > 1) "s" else ""}" }
            description = s.description ?: ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        AsyncImage(
            model              = backdropUrl,
            contentDescription = title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.25f),
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
            if (genres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    genres.take(3).forEachIndexed { index, genre ->
                        Text(genre, color = MATextSecondary, style = MaterialTheme.typography.labelSmall)
                        if (index < genres.take(3).lastIndex) {
                            Text(" • ", color = MATextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text     = title,
                color    = Color.White,
                style    = MaterialTheme.typography.displayLarge,
                maxLines = 2
            )

            Spacer(Modifier.height(6.dp))

            // Rating + year + meta row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = null,
                    tint               = MAGold,
                    modifier           = Modifier.size(14.dp)
                )
                Text(
                    text       = "%.1f".format(rating),
                    color      = MAGold,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                if (year != null) {
                    Text("•", color = MATextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text  = year.toString(),
                        color = MATextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (metaExtra != null) {
                    Text("•", color = MATextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text  = metaExtra,
                        color = MATextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text     = description,
                color    = MATextSecondary,
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick        = onPlayClick,
                    colors         = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor   = Color.Black
                    ),
                    shape          = RoundedCornerShape(MARadius.xs),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick        = onInfoClick,
                    colors         = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape          = RoundedCornerShape(MARadius.xs),
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

// ── Series card — for web series row ─────────────────────────────────────────

@Composable
fun SeriesCard(series: ApiAnime, onClick: () -> Unit) {
    MediaPosterCard(
        posterUrl = series.posterUrl,
        title     = series.title,
        onClick   = onClick,
        rating    = series.rating.takeIf { it > 0 },
        badgeText = "SERIES",
        badgeColor = MAAccentSeries,
        subtitle  = series.releaseYear?.toString()
    )
}

// ── Movie card — with rating badge and duration ───────────────────────────────

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    val subtitle = buildString {
        append(movie.year)
        if (movie.durationSeconds > 0) {
            append("  •  ")
            append(movie.durationFormatted)
        }
    }
    MediaPosterCard(
        posterUrl = movie.posterUrl,
        title     = movie.title,
        onClick   = onClick,
        rating    = movie.rating.takeIf { it > 0 },
        subtitle  = subtitle
    )
}
