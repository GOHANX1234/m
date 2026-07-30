package com.mna.streaming.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.ui.components.LabelBadge
import com.mna.streaming.ui.components.MAEmptyState
import com.mna.streaming.ui.components.MAErrorState
import com.mna.streaming.ui.components.MediaCardSkeleton
import com.mna.streaming.ui.components.MediaPosterCard
import com.mna.streaming.ui.components.SectionHeader
import com.mna.streaming.ui.theme.MABorderSubtle
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MAMotion
import com.mna.streaming.ui.theme.MAPurple
import com.mna.streaming.ui.theme.MARadius
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASpacing
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.MATextTertiary
import com.mna.streaming.ui.theme.pressScaleClickable

@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
            .statusBarsPadding()
    ) {
        // ── Top bar — frosted back button + compact capsule search field ────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = MASpacing.md, vertical = MASpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onBackClick,
                modifier = Modifier
                    .size(34.dp)
                    .background(MACard, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint     = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(MASpacing.sm))

            CompactSearchField(
                query          = uiState.query,
                onQueryChange  = { searchViewModel.onQueryChanged(it) },
                onClear        = { searchViewModel.clearQuery() },
                onSubmit       = {
                    searchViewModel.commitCurrentQueryToHistory()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                isSearching    = uiState.isSearching,
                focusRequester = focusRequester,
                modifier       = Modifier.weight(1f)
            )
        }

        when {
            // Empty query — search history + recently added rails
            uiState.query.isBlank() -> {
                SearchIdleContent(
                    uiState          = uiState,
                    onHistoryClick   = { term -> searchViewModel.searchFromHistory(term) },
                    onRemoveHistory  = { term -> searchViewModel.removeHistoryItem(term) },
                    onClearHistory   = { searchViewModel.clearHistory() },
                    onRetryDiscover  = { searchViewModel.loadDiscoverContent() },
                    onMovieClick     = onMovieClick,
                    onAnimeClick     = onAnimeClick
                )
            }

            // Query too short
            uiState.query.length == 1 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "Keep typing…",
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Network error
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MAErrorState(
                        message = uiState.error ?: "Search failed",
                        onRetry = { searchViewModel.onQueryChanged(uiState.query) }
                    )
                }
            }

            // No results after search
            uiState.hasSearched && uiState.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MAEmptyState(
                        icon        = Icons.Default.SearchOff,
                        title       = "No results for \"${uiState.query}\"",
                        description = "Try a different title, genre or spelling"
                    )
                }
            }

            // Results grid
            uiState.results.isNotEmpty() -> {
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn(tween(MAMotion.medium))
                ) {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(3),
                        contentPadding        = PaddingValues(horizontal = MASpacing.md, vertical = MASpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(MASpacing.sm),
                        verticalArrangement   = Arrangement.spacedBy(MASpacing.md),
                        modifier              = Modifier.fillMaxSize()
                    ) {
                        items(uiState.results, key = { it.id }) { result ->
                            SearchResultCard(
                                result = result,
                                onClick = {
                                    searchViewModel.commitCurrentQueryToHistory()
                                    when (result) {
                                        is SearchResult.MovieItem -> onMovieClick(result.id)
                                        is SearchResult.AnimeItem -> onAnimeClick(result.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Compact capsule search field ─────────────────────────────────────────────
//
// A hand-rolled BasicTextField pill instead of a Material TextField — Material's
// default is ~56dp tall with fixed internal padding it won't shrink below.
// This one is a deliberately smaller, tighter 42dp so the search bar reads as
// a focused input rather than a big empty box.

@Composable
private fun CompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    isSearching: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) MARed.copy(alpha = 0.55f) else MABorderSubtle

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(42.dp)
            .background(MACard, RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = MASpacing.md)
    ) {
        if (isSearching) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp),
                color       = MARed,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint     = MATextSecondary,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text     = "Search movies, anime & series…",
                    color    = MATextTertiary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value            = query,
                onValueChange    = onQueryChange,
                singleLine       = true,
                textStyle        = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush      = SolidColor(MARed),
                keyboardOptions  = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions  = KeyboardActions(onSearch = { onSubmit() }),
                modifier         = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }

        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Clear",
                tint     = MATextSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

// ── Idle state — search history + "recently added" rails ─────────────────────

@Composable
private fun SearchIdleContent(
    uiState: SearchUiState,
    onHistoryClick: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetryDiscover: () -> Unit,
    onMovieClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit
) {
    val discover = uiState.discover

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Recent searches ──────────────────────────────────────────────────
        if (uiState.history.isNotEmpty()) {
            item(key = "history_header") {
                SectionHeader(
                    title         = "Recent Searches",
                    actionLabel   = "Clear All",
                    onActionClick = onClearHistory
                )
            }
            items(uiState.history, key = { "history_$it" }) { term ->
                SearchHistoryRow(
                    query    = term,
                    onClick  = { onHistoryClick(term) },
                    onRemove = { onRemoveHistory(term) }
                )
            }
            item(key = "history_spacer") { Spacer(Modifier.height(MASpacing.md)) }
        }

        // ── Recently added rails ─────────────────────────────────────────────
        when {
            discover.isLoading && discover.isEmpty -> {
                item(key = "skeleton_movies") { DiscoverRailSkeleton("New Movies") }
                item(key = "skeleton_anime")  { DiscoverRailSkeleton("New Anime") }
                item(key = "skeleton_series") { DiscoverRailSkeleton("New Web Series") }
            }

            discover.error != null && discover.isEmpty -> {
                val errorMessage = discover.error ?: "Failed to load"
                item(key = "discover_error") {
                    MAErrorState(
                        message  = errorMessage,
                        onRetry  = onRetryDiscover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MASpacing.xxxl)
                    )
                }
            }

            discover.isEmpty && uiState.history.isEmpty() -> {
                item(key = "empty_prompt") {
                    Box(
                        modifier         = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        MAEmptyState(
                            icon        = Icons.Default.Search,
                            title       = "Find something to watch",
                            description = "Search across every movie, anime and web series"
                        )
                    }
                }
            }

            else -> {
                if (discover.movies.isNotEmpty()) {
                    item(key = "rail_movies") {
                        DiscoverRail(
                            title = "New Movies",
                            items = discover.movies.map {
                                DiscoverRailItem(it.id, it.title, it.posterUrl, it.rating, "MOVIE", Color(0xFF333340))
                            },
                            onClick = onMovieClick
                        )
                    }
                }
                if (discover.anime.isNotEmpty()) {
                    item(key = "rail_anime") {
                        DiscoverRail(
                            title = "New Anime",
                            items = discover.anime.map {
                                DiscoverRailItem(it.id, it.title, it.posterUrl ?: "", it.rating, "ANIME", MARed)
                            },
                            onClick = onAnimeClick
                        )
                    }
                }
                if (discover.webSeries.isNotEmpty()) {
                    item(key = "rail_series") {
                        DiscoverRail(
                            title = "New Web Series",
                            items = discover.webSeries.map {
                                DiscoverRailItem(it.id, it.title, it.posterUrl ?: "", it.rating, "SERIES", MAPurple)
                            },
                            onClick = onAnimeClick
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(MASpacing.xxl)) }
    }
}

/** A single past search term — tap to re-run, tap the "x" to forget it. */
@Composable
private fun SearchHistoryRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MASpacing.lg, vertical = MASpacing.sm)
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint     = MATextSecondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(MASpacing.md))
        Text(
            text     = query,
            color    = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove from history",
                tint     = MATextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Poster-card data unified across movie / anime / web-series discover items. */
private data class DiscoverRailItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val rating: Double,
    val badgeText: String,
    val badgeColor: Color
)

@Composable
private fun DiscoverRail(
    title: String,
    items: List<DiscoverRailItem>,
    onClick: (String) -> Unit
) {
    Column {
        SectionHeader(title = title)
        LazyRow(
            contentPadding        = PaddingValues(horizontal = MASpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MASpacing.sm)
        ) {
            items(items, key = { it.id }) { item ->
                MediaPosterCard(
                    posterUrl  = item.posterUrl,
                    title      = item.title,
                    onClick    = { onClick(item.id) },
                    rating     = item.rating,
                    badgeText  = item.badgeText,
                    badgeColor = item.badgeColor
                )
            }
        }
    }
}

@Composable
private fun DiscoverRailSkeleton(title: String) {
    Column {
        SectionHeader(title = title)
        LazyRow(
            contentPadding        = PaddingValues(horizontal = MASpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MASpacing.sm)
        ) {
            items(5) { MediaCardSkeleton() }
        }
    }
}

// ── Unified search result card ─────────────────────────────────────────────────

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(pressedScale = 0.95f, onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model              = result.posterUrl,
                contentDescription = result.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(MARadius.sm))
                    .background(MACard)
                    .border(1.dp, MABorderSubtle, RoundedCornerShape(MARadius.sm))
            )

            // Pill badge — "Anime", "Series", or "Movie"
            val badgeLabel = when {
                result is SearchResult.AnimeItem && result.anime.type == "series" -> "SERIES"
                result is SearchResult.AnimeItem -> "ANIME"
                else -> "MOVIE"
            }
            val badgeColor = when (badgeLabel) {
                "ANIME"  -> MARed
                "SERIES" -> MAPurple
                else     -> Color(0xFF333340)
            }
            LabelBadge(
                text     = badgeLabel,
                color    = badgeColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text       = result.title,
            color      = Color.White,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis
        )

        result.year?.let { year ->
            Text(
                text     = year.toString(),
                color    = MATextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
