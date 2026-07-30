package com.mna.streaming.ui.admin.genres

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mna.streaming.network.models.ApiGenre
import com.mna.streaming.ui.admin.movies.CenteredLoader
import com.mna.streaming.ui.admin.movies.ErrorRetry
import com.mna.streaming.ui.admin.movies.adminTextFieldColors
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGenresScreen(
    viewModel:   AdminGenresViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget  by remember { mutableStateOf<ApiGenre?>(null) }
    var newGenreName  by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MADark,
        contentColor   = Color.White,
        topBar = {
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
                    text       = "Genres (${state.genres.size})",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = { newGenreName = ""; showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add genre", tint = MARed)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { newGenreName = ""; showAddDialog = true },
                containerColor = MARed,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorRetry(state.error!!) { viewModel.load() }
                state.genres.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No genres yet.", color = MATextSecondary, fontSize = 14.sp)
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(state.genres, key = { it.id }) { genre ->
                        GenreRow(
                            genre      = genre,
                            isDeleting = state.deletingId == genre.id,
                            onDelete   = { deleteTarget = genre }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add genre dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MACard, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("New Genre", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value         = newGenreName,
                        onValueChange = { newGenreName = it },
                        label         = { Text("Genre name") },
                        colors        = adminTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    state.createError?.let { Text(it, color = MARed, fontSize = 12.sp) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            colors  = ButtonDefaults.textButtonColors(contentColor = MATextSecondary)
                        ) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick  = {
                                viewModel.create(newGenreName) { showAddDialog = false }
                            },
                            enabled  = newGenreName.isNotBlank() && !state.isCreating,
                            colors   = ButtonDefaults.buttonColors(containerColor = MARed, contentColor = Color.White),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            if (state.isCreating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { g ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = MACard,
            title = { Text("Delete Genre", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete \"${g.name}\"? Existing content will keep the ID but the name won't resolve.", color = MATextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(g.id); deleteTarget = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MARed)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteTarget = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MATextSecondary)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GenreRow(genre: ApiGenre, isDeleting: Boolean, onDelete: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MADark)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Category, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Text(genre.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (isDeleting) {
            CircularProgressIndicator(color = MARed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MARed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
}
