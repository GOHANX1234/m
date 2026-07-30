package com.mna.streaming.ui.admin.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.mna.streaming.network.models.AdminMovie
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMoviesScreen(
    viewModel:     AdminMoviesViewModel,
    onCreateClick: () -> Unit,
    onEditClick:   (String) -> Unit,
    onBackClick:   () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<AdminMovie?>(null) }

    val displayed = state.movies.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MADark,
        contentColor   = Color.White,
        topBar = {
            Column {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        text       = "Movies",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Default.Add, "Add movie", tint = MARed)
                    }
                }
                // Search bar
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search movies…", color = MATextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = MATextSecondary) },
                    trailingIcon  = if (searchQuery.isNotEmpty()) {{
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = MATextSecondary)
                        }
                    }} else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MARed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = MARed
                    ),
                    shape    = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onCreateClick,
                containerColor    = MARed,
                contentColor      = Color.White,
                shape             = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CenteredLoader()

                state.error != null -> ErrorRetry(state.error!!) { viewModel.loadMovies() }

                displayed.isEmpty() -> CenteredEmpty(
                    if (searchQuery.isBlank()) "No movies yet. Tap + to add one."
                    else "No movies match \"$searchQuery\""
                )

                else -> LazyColumn(
                    contentPadding      = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(displayed, key = { it.id }) { movie ->
                        MovieRow(
                            movie       = movie,
                            isDeleting  = state.deletingId == movie.id,
                            onEdit      = { onEditClick(movie.id) },
                            onDelete    = { deleteTarget = movie }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Confirm delete dialog
    deleteTarget?.let { movie ->
        AlertDialog(
            onDismissRequest   = { deleteTarget = null },
            containerColor     = MACard,
            title              = { Text("Delete Movie", color = Color.White, fontWeight = FontWeight.Bold) },
            text               = {
                Text(
                    "Delete \"${movie.title}\"? This cannot be undone.",
                    color = MATextSecondary
                )
            },
            confirmButton      = {
                TextButton(
                    onClick = { viewModel.deleteMovie(movie.id); deleteTarget = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MARed)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton      = {
                TextButton(
                    onClick = { deleteTarget = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MATextSecondary)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MovieRow(
    movie:      AdminMovie,
    isDeleting: Boolean,
    onEdit:     () -> Unit,
    onDelete:   () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MADark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model              = movie.posterUrl,
            contentDescription = movie.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(width = 48.dp, height = 68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MASurface)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = movie.title,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip(movie.status)
                if (movie.releaseYear != null && movie.releaseYear > 0) {
                    Text(
                        text  = movie.releaseYear.toString(),
                        color = MATextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        if (isDeleting) {
            CircularProgressIndicator(
                color    = MARed,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = MATextSecondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MARed.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 78.dp))
}

@Composable
internal fun StatusChip(status: String) {
    val (color, label) = when (status) {
        "published"  -> Pair(Color(0xFF10B981), "Published")
        "draft"      -> Pair(MATextSecondary,   "Draft")
        "ongoing"    -> Pair(Color(0xFF3B82F6), "Ongoing")
        "completed"  -> Pair(Color(0xFF10B981), "Completed")
        else         -> Pair(MATextSecondary,   status)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text          = label,
            color         = color,
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines      = 1,
            softWrap      = false,
            modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MARed)
    }
}

@Composable
internal fun CenteredEmpty(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = MATextSecondary, fontSize = 14.sp)
    }
}

@Composable
internal fun ErrorRetry(error: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(error, color = MATextSecondary)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = MARed)) {
            Text("Retry")
        }
    }
}
