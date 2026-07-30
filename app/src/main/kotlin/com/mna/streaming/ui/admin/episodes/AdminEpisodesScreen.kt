package com.mna.streaming.ui.admin.episodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mna.streaming.network.models.ApiAdminEpisode
import com.mna.streaming.ui.admin.movies.*
import com.mna.streaming.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEpisodesScreen(
    viewModel:   AdminEpisodesViewModel,
    seriesId:    String,
    onBackClick: () -> Unit
) {
    val state  by viewModel.state.collectAsState()
    val scope  = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<ApiAdminEpisode?>(null) }

    LaunchedEffect(seriesId) { viewModel.loadEpisodes(seriesId) }

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
                    text       = "Episodes",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.openCreateForm() }) {
                    Icon(Icons.Default.Add, "Add episode", tint = MARed)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { viewModel.openCreateForm() },
                containerColor = MARed,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorRetry(state.error!!) { viewModel.loadEpisodes(seriesId) }
                state.episodes.isEmpty() -> CenteredEmpty("No episodes yet. Tap + to add one.")
                else -> {
                    // Group by season
                    val bySeason = state.episodes.groupBy { it.season }
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        bySeason.keys.sorted().forEach { season ->
                            item(key = "season_$season") {
                                Text(
                                    text     = "Season $season",
                                    color    = MATextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(
                                items = bySeason[season] ?: emptyList(),
                                key   = { it.id }
                            ) { ep ->
                                EpisodeRow(
                                    ep          = ep,
                                    isDeleting  = state.deletingId == ep.id,
                                    onEdit      = { viewModel.openEditForm(ep) },
                                    onDelete    = { deleteTarget = ep }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Episode form dialog
    if (state.showForm) {
        EpisodeFormDialog(
            state     = state,
            seriesId  = seriesId,
            viewModel = viewModel,
            scope     = scope,
            onDismiss = { viewModel.closeForm() }
        )
    }

    deleteTarget?.let { ep ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = MACard,
            title = { Text("Delete Episode", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete S${ep.season}E${ep.episodeNumber}? This cannot be undone.", color = MATextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteEpisode(ep.id); deleteTarget = null },
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
private fun EpisodeRow(
    ep:         ApiAdminEpisode,
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
        // Episode number badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MASurface
        ) {
            Text(
                text       = String.format("%02d", ep.episodeNumber),
                color      = MATextSecondary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${ep.episodeNumber}",
                color    = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ep.videoType?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(it.uppercase(), color = MATextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp)
            }
        }
        if (isDeleting) {
            CircularProgressIndicator(color = MARed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = MATextSecondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MARed.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 60.dp))
}

@Composable
private fun EpisodeFormDialog(
    state:    AdminEpisodesUiState,
    seriesId: String,
    viewModel: AdminEpisodesViewModel,
    scope:    kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit
) {
    val ep = state.editingEpisode
    var season    by remember { mutableStateOf(ep?.season?.toString() ?: "1") }
    var epNum     by remember { mutableStateOf(ep?.episodeNumber?.toString() ?: "") }
    var title     by remember { mutableStateOf(ep?.title ?: "") }
    var videoUrl  by remember { mutableStateOf(ep?.videoUrl ?: "") }
    var videoType by remember { mutableStateOf(ep?.videoType ?: "auto") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = MACard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text       = if (ep != null) "Edit Episode" else "Add Episode",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value         = season,
                        onValueChange = { season = it },
                        label         = { Text("Season") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors        = adminTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value         = epNum,
                        onValueChange = { epNum = it },
                        label         = { Text("Episode #*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors        = adminTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Title") },
                    colors        = adminTextFieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = videoUrl,
                    onValueChange = { videoUrl = it },
                    label         = { Text("Video URL") },
                    colors        = adminTextFieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                SectionLabel("Video Type")
                VideoTypeSelector(videoType) { videoType = it }

                state.saveError?.let { Text(it, color = MARed, fontSize = 12.sp) }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MATextSecondary)) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val s = season.toIntOrNull() ?: 1
                            val n = epNum.toIntOrNull() ?: return@Button
                            if (ep != null) {
                                viewModel.updateEpisode(ep.id, s, n, title, videoUrl, videoType)
                            } else {
                                viewModel.createEpisode(seriesId, s, n, title, videoUrl, videoType)
                            }
                        },
                        enabled  = epNum.toIntOrNull() != null && !state.isSaving,
                        colors   = ButtonDefaults.buttonColors(containerColor = MARed, contentColor = Color.White),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (ep != null) "Save" else "Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
