package com.mna.streaming.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.ui.components.LabelBadge
import com.mna.streaming.ui.components.MAEmptyState
import com.mna.streaming.ui.components.MAErrorState
import com.mna.streaming.ui.theme.MABorderSubtle
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MAMotion
import com.mna.streaming.ui.theme.MARadius
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASpacing
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.pressScaleClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
            .statusBarsPadding()
    ) {
        // ── Top bar — frosted back button + capsule search field ────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = MASpacing.md, vertical = MASpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(MACard, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(MASpacing.sm))

            val isFocused = remember { mutableStateOf(false) }
            TextField(
                value         = uiState.query,
                onValueChange = { searchViewModel.onQueryChanged(it) },
                placeholder   = { Text("Search movies & anime…", color = MATextSecondary) },
                singleLine    = true,
                leadingIcon   = {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = MARed,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MATextSecondary)
                    }
                },
                trailingIcon  = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.clearQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MATextSecondary)
                        }
                    }
                },
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = MACard,
                    unfocusedContainerColor = MACard,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = MARed,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White
                ),
                shape         = RoundedCornerShape(50),
                modifier      = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = if (isFocused.value) MARed.copy(alpha = 0.55f) else MABorderSubtle,
                        shape = RoundedCornerShape(50)
                    )
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused.value = it.isFocused }
            )
        }

        Spacer(Modifier.height(MASpacing.xs))

        when {
            // Empty query — prompt
            uiState.query.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MAEmptyState(
                        icon        = Icons.Default.Search,
                        title       = "Find something to watch",
                        description = "Search across every movie and anime title"
                    )
                }
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
                "SERIES" -> Color(0xFF6A0DAD)
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
