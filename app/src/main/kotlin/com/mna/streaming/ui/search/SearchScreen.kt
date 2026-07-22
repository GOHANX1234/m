package com.mna.streaming.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mna.streaming.data.model.Movie
import com.mna.streaming.ui.home.MovieCard
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MATextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (Movie) -> Unit,
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
        // Top bar
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            TextField(
                value         = uiState.query,
                onValueChange = { searchViewModel.onQueryChanged(it) },
                placeholder   = { Text("Search titles…", color = MATextSecondary) },
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
                    focusedContainerColor   = Color(0xFF1E1E24),
                    unfocusedContainerColor = Color(0xFF1E1E24),
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = Color.White,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White
                ),
                shape         = RoundedCornerShape(8.dp),
                modifier      = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .focusRequester(focusRequester)
            )
        }

        Spacer(Modifier.height(4.dp))

        when {
            // Empty query — prompt
            uiState.query.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "Type at least 2 characters to search",
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodyMedium
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
                    Text(
                        text  = uiState.error ?: "Search failed",
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // No results after search
            uiState.hasSearched && uiState.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No results for", color = MATextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text       = "\"${uiState.query}\"",
                            color      = Color.White,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Results grid
            uiState.results.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(3),
                    contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxSize()
                ) {
                    items(uiState.results) { movie ->
                        MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
            }
        }
    }
}
