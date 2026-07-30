package com.mna.streaming.ui.admin.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.mna.streaming.network.models.*
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminMovieFormScreen(
    viewModel:   AdminMoviesViewModel,
    editMovieId: String?,
    onSaved:     () -> Unit,
    onBackClick: () -> Unit
) {
    val state  by viewModel.state.collectAsState()
    val isEdit = editMovieId != null

    // Load existing movie for edit
    LaunchedEffect(editMovieId) {
        if (editMovieId != null) viewModel.loadEditMovie(editMovieId)
        else viewModel.clearEditMovie()
    }

    // Form fields
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var posterUrl   by remember { mutableStateOf("") }
    var bannerUrl   by remember { mutableStateOf("") }
    var trailerUrl  by remember { mutableStateOf("") }
    var videoUrl    by remember { mutableStateOf("") }
    var videoType   by remember { mutableStateOf("auto") }
    var releaseYear by remember { mutableStateOf("") }
    var duration    by remember { mutableStateOf("") }
    var rating      by remember { mutableStateOf("") }
    var status      by remember { mutableStateOf("draft") }
    var selectedGenreIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var externalId  by remember { mutableStateOf("") }
    var castMembers by remember { mutableStateOf<List<AdminCastMember>>(emptyList()) }
    var tmdbSearch  by remember { mutableStateOf("") }
    var showTmdbDialog by remember { mutableStateOf(false) }
    var formInited  by remember { mutableStateOf(false) }

    // Pre-fill form when editing
    LaunchedEffect(state.editingMovie) {
        val m = state.editingMovie ?: return@LaunchedEffect
        if (formInited) return@LaunchedEffect
        title       = m.title
        description = m.description ?: ""
        posterUrl   = m.posterUrl ?: ""
        bannerUrl   = m.bannerUrl ?: ""
        trailerUrl  = m.trailerUrl ?: ""
        videoUrl    = m.videoUrl ?: ""
        videoType   = m.videoType ?: "auto"
        releaseYear = m.releaseYear?.toString() ?: ""
        duration    = m.duration?.toString() ?: ""
        rating      = m.rating?.toString() ?: ""
        status      = m.status
        externalId  = m.externalId ?: ""
        selectedGenreIds = m.genres.map { it.id }.toSet()
        castMembers = m.cast?.map {
            AdminCastMember(it.name, it.character, it.image ?: "", it.order)
        } ?: emptyList()
        formInited  = true
    }

    // TMDB autofill — apply search result
    fun applyTmdbResult(result: TmdbSearchResult) {
        title       = result.title
        description = result.description ?: description
        posterUrl   = result.posterUrl ?: posterUrl
        bannerUrl   = result.bannerUrl ?: bannerUrl
        releaseYear = result.releaseYear?.toString() ?: releaseYear
        rating      = result.rating?.toString() ?: rating
        externalId  = result.externalId
        viewModel.loadTmdbDetails(result.externalId)
        showTmdbDialog = false
        viewModel.clearTmdb()
    }

    // Apply TMDB details (duration, cast) — duration and cast come directly from the response.
    LaunchedEffect(state.tmdbDetails) {
        val d = state.tmdbDetails ?: return@LaunchedEffect
        if (d.duration != null && d.duration > 0) duration = d.duration.toString()
        // Apply cast fetched from TMDB
        if (d.cast.isNotEmpty()) {
            castMembers = d.cast.map {
                AdminCastMember(it.name, it.character ?: "", it.image ?: "", it.order)
            }
        }
        // NOTE: genre IDs arrive separately via tmdbResolvedGenreIds once
        // POST /api/admin/genres has been called for each name (spec §10.2, §15.2).
    }

    // Apply server-resolved genre IDs from TMDB autofill (spec §10.2, §15.2).
    // The ViewModel calls POST /api/admin/genres for each genreName so that genres
    // not yet in the local list are created and their ObjectIds are returned.
    LaunchedEffect(state.tmdbResolvedGenreIds) {
        val ids = state.tmdbResolvedGenreIds ?: return@LaunchedEffect
        selectedGenreIds = selectedGenreIds + ids
    }

    Scaffold(
        containerColor = MADark,
        contentColor   = Color.White,
        topBar = {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    text       = if (isEdit) "Edit Movie" else "Add Movie",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->

        if (isEdit && state.isLoadingEdit) {
            CenteredLoader()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // TMDB autofill button
            OutlinedButton(
                onClick = { showTmdbDialog = true },
                border  = androidx.compose.foundation.BorderStroke(1.dp, MARed.copy(alpha = 0.6f)),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = MARed),
                shape   = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Autofill from TMDB", fontWeight = FontWeight.SemiBold)
            }

            SectionLabel("Basic Info")
            AdminTextField("Title *", title, { title = it })
            AdminTextField("Description", description, { description = it }, minLines = 3)
            AdminTextField("Release Year", releaseYear, { releaseYear = it }, keyboardType = KeyboardType.Number)
            AdminTextField("Duration (seconds)", duration, { duration = it }, keyboardType = KeyboardType.Number)
            AdminTextField("Rating (0–10)", rating, { rating = it }, keyboardType = KeyboardType.Decimal)
            AdminTextField("TMDB External ID", externalId, { externalId = it })

            SectionLabel("URLs")
            AdminTextField("Poster URL", posterUrl, { posterUrl = it })
            AdminTextField("Banner URL", bannerUrl, { bannerUrl = it })
            AdminTextField("Trailer URL", trailerUrl, { trailerUrl = it })
            AdminTextField("Video URL", videoUrl, { videoUrl = it })

            SectionLabel("Video Type")
            VideoTypeSelector(videoType) { videoType = it }

            SectionLabel("Publish Status")
            StatusSelector(status, listOf("draft", "published")) { status = it }

            SectionLabel("Genres")
            GenrePicker(
                genres          = state.genres,
                selectedIds     = selectedGenreIds,
                onToggle        = { id ->
                    selectedGenreIds = if (id in selectedGenreIds)
                        selectedGenreIds - id else selectedGenreIds + id
                }
            )

            if (state.isLoadingTmdbDetails) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = MARed,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Fetching cast from TMDB…", color = MATextSecondary, fontSize = 12.sp)
                }
            } else if (castMembers.isNotEmpty()) {
                SectionLabel("Cast (${castMembers.size} members)")
                CastPreviewSection(castMembers)
            }

            state.saveError?.let {
                Text(it, color = MARed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val req = buildSaveRequest(
                        title, description, posterUrl, bannerUrl, trailerUrl,
                        videoUrl, videoType, externalId, releaseYear, duration,
                        rating, status, selectedGenreIds.toList(),
                        castMembers.takeIf { it.isNotEmpty() }
                    )
                    if (isEdit && editMovieId != null) {
                        viewModel.updateMovie(editMovieId, req.toUpdate(), onSaved)
                    } else {
                        viewModel.createMovie(req, onSaved)
                    }
                },
                enabled  = title.isNotBlank() && !state.isSaving,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MARed,
                    contentColor   = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.4f),
                    disabledContentColor   = Color.White.copy(alpha = 0.5f)
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEdit) "Save Changes" else "Create Movie", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // TMDB search dialog
    if (showTmdbDialog) {
        Dialog(onDismissRequest = { showTmdbDialog = false; viewModel.clearTmdb() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MACard,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search TMDB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = tmdbSearch,
                        onValueChange = { tmdbSearch = it; if (it.length >= 2) viewModel.searchTmdb(it) },
                        placeholder   = { Text("Movie title…", color = MATextSecondary) },
                        leadingIcon   = { Icon(Icons.Default.Search, null, tint = MATextSecondary) },
                        colors        = adminTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    if (state.isSearchingTmdb) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MARed, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(state.tmdbResults) { result ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable { applyTmdbResult(result) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(result.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        result.releaseYear?.let {
                                            Text(it.toString(), color = MATextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MATextSecondary, modifier = Modifier.size(18.dp))
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private data class MovieFormData(
    val title: String, val description: String?, val posterUrl: String?,
    val bannerUrl: String?, val trailerUrl: String?, val videoUrl: String?,
    val videoType: String?, val externalId: String?, val releaseYear: Int?,
    val duration: Int?, val rating: Double?, val status: String,
    val genreIds: List<String>
)

private fun MovieFormData.toUpdate() = UpdateMovieRequest(
    title, description, posterUrl, bannerUrl, trailerUrl, videoUrl, videoType,
    externalId, duration, releaseYear, genreIds, null, status, rating
)

private fun buildSaveRequest(
    title: String, description: String, posterUrl: String, bannerUrl: String,
    trailerUrl: String, videoUrl: String, videoType: String, externalId: String,
    releaseYear: String, duration: String, rating: String, status: String,
    genreIds: List<String>, cast: List<AdminCastMember>?
): CreateMovieRequest = CreateMovieRequest(
    title       = title.trim(),
    description = description.trim().takeIf { it.isNotEmpty() },
    posterUrl   = posterUrl.trim().takeIf { it.isNotEmpty() },
    bannerUrl   = bannerUrl.trim().takeIf { it.isNotEmpty() },
    trailerUrl  = trailerUrl.trim().takeIf { it.isNotEmpty() },
    videoUrl    = videoUrl.trim().takeIf { it.isNotEmpty() },
    videoType   = videoType.takeIf { it != "auto" },
    externalId  = externalId.trim().takeIf { it.isNotEmpty() },
    duration    = duration.toIntOrNull(),
    releaseYear = releaseYear.toIntOrNull(),
    genres      = genreIds.takeIf { it.isNotEmpty() },
    cast        = cast,
    status      = status,
    rating      = rating.toDoubleOrNull()
)

// Convert CreateMovieRequest to UpdateMovieRequest
private fun CreateMovieRequest.toUpdate() = UpdateMovieRequest(
    title, description, posterUrl, bannerUrl, trailerUrl, videoUrl, videoType,
    externalId, duration, releaseYear, genres, cast, status, rating
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CastPreviewSection(cast: List<AdminCastMember>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp)
    ) {
        cast.take(20).forEach { member ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MACard
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(
                        text       = member.name,
                        color      = Color.White,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines   = 1,
                        overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (member.character.isNotBlank()) {
                        Text(
                            text     = member.character,
                            color    = MATextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        if (cast.size > 20) {
            Surface(shape = RoundedCornerShape(6.dp), color = MACard) {
                Text(
                    text     = "+${cast.size - 20} more",
                    color    = MATextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        color         = MATextSecondary,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
internal fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = MARed,
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = MARed,
    focusedLabelColor    = MARed,
    unfocusedLabelColor  = MATextSecondary
)

@Composable
internal fun AdminTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        colors        = adminTextFieldColors(),
        shape         = RoundedCornerShape(10.dp),
        minLines      = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier      = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GenrePicker(
    genres: List<com.mna.streaming.network.models.ApiGenre>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    if (genres.isEmpty()) {
        Text("No genres loaded", color = MATextSecondary, fontSize = 13.sp)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            val selected = genre.id in selectedIds
            FilterChip(
                selected = selected,
                onClick  = { onToggle(genre.id) },
                label    = { Text(genre.name, fontSize = 12.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor     = MARed.copy(alpha = 0.18f),
                    selectedLabelColor         = MARed,
                    containerColor             = MACard,
                    labelColor                 = MATextSecondary
                ),
                border   = FilterChipDefaults.filterChipBorder(
                    enabled          = true,
                    selected         = selected,
                    selectedBorderColor = MARed.copy(alpha = 0.5f),
                    borderColor      = Color.White.copy(alpha = 0.10f)
                )
            )
        }
    }
}

@Composable
internal fun VideoTypeSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("auto", "hls", "direct", "embed")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick  = { onSelect(opt) },
                label    = { Text(opt, fontSize = 12.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MARed.copy(alpha = 0.18f),
                    selectedLabelColor     = MARed,
                    containerColor         = MACard,
                    labelColor             = MATextSecondary
                ),
                border   = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected == opt,
                    selectedBorderColor = MARed.copy(alpha = 0.5f),
                    borderColor = Color.White.copy(alpha = 0.10f)
                )
            )
        }
    }
}

@Composable
internal fun StatusSelector(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick  = { onSelect(opt) },
                label    = { Text(opt.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MARed.copy(alpha = 0.18f),
                    selectedLabelColor     = MARed,
                    containerColor         = MACard,
                    labelColor             = MATextSecondary
                ),
                border   = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected == opt,
                    selectedBorderColor = MARed.copy(alpha = 0.5f),
                    borderColor = Color.White.copy(alpha = 0.10f)
                )
            )
        }
    }
}
