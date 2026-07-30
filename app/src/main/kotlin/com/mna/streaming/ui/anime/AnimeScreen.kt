package com.mna.streaming.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.network.models.ApiAnime
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MAGold
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASurface
import com.mna.streaming.ui.theme.MATextSecondary
import java.util.Locale

private val SortOptions = listOf(
    "latest" to "New",
    "views"  to "Popular",
    "rating" to "Top Rated"
)

private val StatusOptions = listOf(
    null       to "All",
    "ongoing"  to "Ongoing",
    "completed" to "Completed"
)

@Composable
fun AnimeScreen(
    onAnimeClick: (ApiAnime) -> Unit,
    onSearchClick: () -> Unit,
    animeViewModel: AnimeViewModel = viewModel(factory = AnimeViewModel.Factory)
) {
    val uiState by animeViewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // Load more when near the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 6
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
                CircularProgressIndicator(
                    color    = MARed,
                    modifier = Modifier.align(Alignment.Center)
                )
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
                        colors  = ButtonDefaults.buttonColors(containerColor = MARed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns             = GridCells.Fixed(2),
                    state               = gridState,
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(
                        start  = 12.dp,
                        end    = 12.dp,
                        top    = 140.dp,   // leave room for the filter header
                        bottom = 90.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.anime, key = { it.id }) { anime ->
                        AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
                    }

                    if (uiState.isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MARed, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }

        // ── Sticky header (top bar + filters) ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MADark)
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
                Text(
                    text          = "Anime",
                    color         = Color.White,
                    fontSize      = 24.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
            }

            // Sort tabs
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOptions.forEach { (value, label) ->
                    val selected = uiState.selectedSort == value
                    FilterChip(
                        selected = selected,
                        onClick  = { animeViewModel.setSort(value) },
                        label    = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor  = MARed,
                            selectedLabelColor      = Color.White,
                            containerColor          = MACard,
                            labelColor              = MATextSecondary
                        ),
                        border   = null
                    )
                }

                Spacer(Modifier.width(4.dp))
                // Visual separator between sort and status chips
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MACard)
                        .align(Alignment.CenterVertically)
                )
                Spacer(Modifier.width(4.dp))

                // Status filter
                StatusOptions.forEach { (value, label) ->
                    val selected = uiState.selectedStatus == value
                    FilterChip(
                        selected = selected,
                        onClick  = { animeViewModel.setStatus(value) },
                        label    = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor  = MASurface,
                            selectedLabelColor      = Color.White,
                            containerColor          = MACard,
                            labelColor              = MATextSecondary
                        ),
                        border   = null
                    )
                }
            }

            // Genre filter chips
            if (uiState.genres.isNotEmpty()) {
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // "All" chip
                    val allSelected = uiState.selectedGenre == null
                    FilterChip(
                        selected = allSelected,
                        onClick  = { animeViewModel.setGenre(null) },
                        label    = { Text("All", fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor  = MARed,
                            selectedLabelColor      = Color.White,
                            containerColor          = MACard,
                            labelColor              = MATextSecondary
                        ),
                        border   = null
                    )
                    uiState.genres.forEach { genre ->
                        val selected = uiState.selectedGenre == genre
                        FilterChip(
                            selected = selected,
                            onClick  = { animeViewModel.setGenre(genre) },
                            label    = { Text(genre, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor  = MARed,
                                selectedLabelColor      = Color.White,
                                containerColor          = MACard,
                                labelColor              = MATextSecondary
                            ),
                            border   = null
                        )
                    }
                }
            }

            HorizontalDivider(color = MASurface, thickness = 1.dp)
        }
    }
}

// ── Anime card ─────────────────────────────────────────────────────────────────

@Composable
fun AnimeCard(anime: ApiAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model              = anime.posterUrl,
                contentDescription = anime.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            // Status badge
            val statusLabel = if (anime.status == "ongoing") "Ongoing" else "Completed"
            val badgeColor  = if (anime.status == "ongoing") Color(0xFF22C55E) else Color(0xFF6B7280)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.9f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(statusLabel, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            }

            // Always reserve a rating badge so card layout stays consistent.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = MAGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (anime.rating > 0) {
                            String.format(Locale.US, "%.1f", anime.rating)
                        } else {
                            "—"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text     = anime.title,
            color    = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
        Text(
            text  = anime.releaseYear?.toString() ?: "",
            color = MATextSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}
