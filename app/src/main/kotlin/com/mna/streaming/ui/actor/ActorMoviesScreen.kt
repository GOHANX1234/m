package com.mna.streaming.ui.actor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mna.streaming.data.model.Movie
import com.mna.streaming.ui.home.MovieCard
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASurface
import com.mna.streaming.ui.theme.MATextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorMoviesScreen(
    actorName:    String,
    onBackClick:  () -> Unit,
    onMovieClick: (String) -> Unit
) {
    val viewModel: ActorMoviesViewModel = viewModel(
        key     = actorName,
        factory = ActorMoviesViewModel.factory(actorName)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = actorName,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            fontSize   = 18.sp,
                            maxLines   = 1
                        )
                        Text(
                            text  = "Filmography",
                            color = MATextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MADark,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Actor avatar placeholder in top-right
                    Box(
                        modifier         = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .background(MASurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Person,
                            contentDescription = null,
                            tint               = MATextSecondary,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        containerColor = MADark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = uiState.error ?: "Error",
                            color = MATextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.load() },
                            colors  = ButtonDefaults.buttonColors(containerColor = MARed)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }

                uiState.noCastData && uiState.movies.isEmpty() -> {
                    // The API didn't include cast in list responses for this actor.
                    Column(
                        modifier            = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(72.dp)
                                .background(MASurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = null,
                                tint               = MATextSecondary,
                                modifier           = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = actorName,
                            color     = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize  = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text      = "No movies found in the current library for this actor.",
                            color     = MATextSecondary,
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    Column {
                        // Movie count header
                        Text(
                            text     = "${uiState.movies.size} movie${if (uiState.movies.size != 1) "s" else ""}",
                            color    = MATextSecondary,
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        // 3-column poster grid
                        LazyVerticalGrid(
                            columns             = GridCells.Fixed(3),
                            contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier            = Modifier.fillMaxSize()
                        ) {
                            items(uiState.movies) { movie ->
                                MovieCard(
                                    movie   = movie,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
