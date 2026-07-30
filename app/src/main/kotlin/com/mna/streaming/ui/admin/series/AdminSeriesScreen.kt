package com.mna.streaming.ui.admin.series

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
import com.mna.streaming.network.models.AdminSeries
import com.mna.streaming.ui.admin.movies.CenteredEmpty
import com.mna.streaming.ui.admin.movies.CenteredLoader
import com.mna.streaming.ui.admin.movies.ErrorRetry
import com.mna.streaming.ui.admin.movies.StatusChip
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSeriesScreen(
    viewModel:       AdminSeriesViewModel,
    onCreateClick:   () -> Unit,
    onEditClick:     (String) -> Unit,
    onEpisodesClick: (String) -> Unit,
    onBackClick:     () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<AdminSeries?>(null) }

    val displayed = state.seriesList.filter {
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
                        text       = "Anime / Series",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Default.Add, "Add", tint = MARed)
                    }
                }
                // Type tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("anime", "series").forEach { type ->
                        val sel = state.currentType == type
                        FilterChip(
                            selected = sel,
                            onClick  = { viewModel.loadSeries(type) },
                            label    = { Text(type.replaceFirstChar { it.uppercase() }) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MARed.copy(alpha = 0.18f),
                                selectedLabelColor     = MARed,
                                containerColor         = MACard,
                                labelColor             = MATextSecondary
                            ),
                            border   = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = sel,
                                selectedBorderColor = MARed.copy(alpha = 0.5f),
                                borderColor = Color.White.copy(alpha = 0.10f)
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search…", color = MATextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = MATextSecondary) },
                    trailingIcon  = if (searchQuery.isNotEmpty()) {{
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = MATextSecondary)
                        }
                    }} else null,
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MARed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = MARed
                    ),
                    shape      = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onCreateClick,
                containerColor = MARed,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorRetry(state.error!!) { viewModel.loadSeries(state.currentType) }
                displayed.isEmpty() -> CenteredEmpty(
                    if (searchQuery.isBlank()) "No ${state.currentType} yet. Tap + to add one."
                    else "No results for \"$searchQuery\""
                )
                else -> LazyColumn(
                    contentPadding      = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(displayed, key = { it.id }) { series ->
                        SeriesRow(
                            series        = series,
                            isDeleting    = state.deletingId == series.id,
                            onEdit        = { onEditClick(series.id) },
                            onEpisodes    = { onEpisodesClick(series.id) },
                            onDelete      = { deleteTarget = series }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = MACard,
            title = { Text("Delete Series", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete \"${s.title}\"? Episodes won't be deleted automatically.", color = MATextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteSeries(s.id); deleteTarget = null },
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
private fun SeriesRow(
    series:     AdminSeries,
    isDeleting: Boolean,
    onEdit:     () -> Unit,
    onEpisodes: () -> Unit,
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
            model              = series.posterUrl,
            contentDescription = series.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(width = 48.dp, height = 68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MASurface)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(series.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                series.publishStatus?.let { StatusChip(it) }
                series.status?.let { StatusChip(it) }
                series.releaseYear?.takeIf { it > 0 }?.let {
                    Text(it.toString(), color = MATextSecondary, fontSize = 11.sp)
                }
            }
        }
        if (isDeleting) {
            CircularProgressIndicator(color = MARed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onEpisodes) {
                Icon(Icons.Default.VideoLibrary, "Episodes", tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
            }
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
