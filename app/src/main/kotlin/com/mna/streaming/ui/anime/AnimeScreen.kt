package com.mna.streaming.ui.anime

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.mna.streaming.network.models.ApiAnime
import com.mna.streaming.ui.theme.*
import java.util.Locale

private val SortOptions = listOf(
    "latest" to "New",
    "views"  to "Popular",
    "rating" to "Top Rated"
)

private val StatusOptions = listOf(
    null        to "All",
    "ongoing"   to "Live",
    "completed" to "Done"
)

// â”€â”€ Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun AnimeScreen(
    onAnimeClick: (ApiAnime) -> Unit,
    onSearchClick: () -> Unit,
    animeViewModel: AnimeViewModel = viewModel(factory = AnimeViewModel.Factory)
) {
    val uiState by animeViewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 9
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && !uiState.isLoading) {
            animeViewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        when {
            uiState.isLoading -> {
                // Skeleton shimmer grid â€” feels alive, no dead spinner
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(3),
                    modifier              = Modifier.fillMaxSize(),
                    contentPadding        = PaddingValues(
                        start  = 12.dp, end = 12.dp,
                        top    = 184.dp, bottom = 90.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    userScrollEnabled     = false
                ) {
                    items(12) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.65f)
                                .clip(RoundedCornerShape(10.dp))
                                .shimmer()
                        )
                    }
                }
            }

            uiState.error != null && uiState.anime.isEmpty() -> {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = uiState.error ?: "Unknown error",
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { animeViewModel.retry() },
                        colors  = ButtonDefaults.buttonColors(containerColor = MARed),
                        shape   = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(3),
                    state                 = gridState,
                    modifier              = Modifier.fillMaxSize(),
                    contentPadding        = PaddingValues(
                        start  = 12.dp, end = 12.dp,
                        top    = 184.dp, bottom = 90.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.anime, key = { it.id }) { anime ->
                        AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
                    }

                    if (uiState.isLoadingMore) {
                        items(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.65f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .shimmer()
                            )
                        }
                    }
                }
            }
        }

        // â”€â”€ Sticky header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        AnimeHeader(
            uiState          = uiState,
            onSearchClick    = onSearchClick,
            onSortSelected   = { animeViewModel.setSort(it) },
            onStatusSelected = { animeViewModel.setStatus(it) },
            onGenreSelected  = { animeViewModel.setGenre(it) }
        )
    }
}

// â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AnimeHeader(
    uiState: AnimeUiState,
    onSearchClick: () -> Unit,
    onSortSelected: (String) -> Unit,
    onStatusSelected: (String?) -> Unit,
    onGenreSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to MADark,
                        0.88f to MADark,
                        1.0f to Color.Transparent
                    )
                )
            )
    ) {
        // Top bar
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text          = "Anime",
                    color         = Color.White,
                    fontWeight    = FontWeight.Black,
                    fontSize      = 26.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeight    = 28.sp
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MARed)
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MACard)
                    .pressScaleClickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Sort: segmented pill control
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SortOptions.forEach { (value, label) ->
                val selected = uiState.selectedSort == value
                val bg by animateColorAsState(
                    targetValue   = if (selected) MARed else MACard,
                    animationSpec = tween(MAMotion.fast),
                    label         = "sortBg-$value"
                )
                val textColor by animateColorAsState(
                    targetValue   = if (selected) Color.White else MATextSecondary,
                    animationSpec = tween(MAMotion.fast),
                    label         = "sortText-$value"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .pressScaleClickable { onSortSelected(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = textColor,
                        fontSize   = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Status + Genre horizontal chips in one scrollable row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            StatusOptions.forEach { (value, label) ->
                val selected = uiState.selectedStatus == value
                val accentColor = when (value) {
                    "ongoing"   -> Color(0xFF22C55E)
                    "completed" -> MATextSecondary
                    else        -> MARed
                }
                val bg by animateColorAsState(
                    targetValue   = if (selected) accentColor.copy(alpha = 0.18f) else MACard,
                    animationSpec = tween(MAMotion.fast),
                    label         = "statusBg-$value"
                )
                val textColor by animateColorAsState(
                    targetValue   = if (selected) accentColor else MATextSecondary,
                    animationSpec = tween(MAMotion.fast),
                    label         = "statusText-$value"
                )
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .pressScaleClickable { onStatusSelected(value) }
                        .padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = textColor,
                        fontSize   = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(MABorderSubtle)
            )

            // Genre chips
            AnimeGenreChip(
                label    = "All",
                selected = uiState.selectedGenre == null,
                onClick  = { onGenreSelected(null) }
            )
            uiState.genres.forEach { genre ->
                AnimeGenreChip(
                    label    = genre,
                    selected = uiState.selectedGenre == genre,
                    onClick  = { onGenreSelected(genre) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AnimeGenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue   = if (selected) MARed else MACard,
        animationSpec = tween(MAMotion.fast),
        label         = "genreBg-$label"
    )
    val textColor by animateColorAsState(
        targetValue   = if (selected) Color.White else MATextSecondary,
        animationSpec = tween(MAMotion.fast),
        label         = "genreText-$label"
    )
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// â”€â”€ Anime card â€” title overlaid inside poster â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun AnimeCard(anime: ApiAnime, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .clip(RoundedCornerShape(10.dp))
            .pressScaleClickable(onClick = onClick)
    ) {
        // Poster
        AsyncImage(
            model              = anime.posterUrl,
            contentDescription = anime.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Bottom scrim for title legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                    )
                )
        )

        // Ongoing badge â€” top-left (only shown when relevant)
        if (anime.status == "ongoing") {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF22C55E).copy(alpha = 0.92f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
                Text(
                    text          = "ON AIR",
                    color         = Color.White,
                    fontSize      = 7.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // Rating badge â€” top-right
        if (anime.rating > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = null,
                    tint               = MAGold,
                    modifier           = Modifier.size(9.dp)
                )
                Text(
                    text       = String.format(Locale.US, "%.1f", anime.rating),
                    color      = Color.White,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Title + year overlaid at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp)
        ) {
            Text(
                text       = anime.title,
                color      = Color.White,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            if (anime.releaseYear != null) {
                Text(
                    text       = anime.releaseYear.toString(),
                    color      = Color.White.copy(alpha = 0.55f),
                    fontSize   = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
