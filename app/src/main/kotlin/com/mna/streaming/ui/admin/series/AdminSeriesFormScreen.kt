package com.mna.streaming.ui.admin.series

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mna.streaming.network.models.*
import com.mna.streaming.ui.admin.movies.*
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSeriesFormScreen(
    viewModel:    AdminSeriesViewModel,
    editSeriesId: String?,
    onSaved:      () -> Unit,
    onBackClick:  () -> Unit
) {
    val state  by viewModel.state.collectAsState()
    val isEdit = editSeriesId != null

    LaunchedEffect(editSeriesId) {
        if (editSeriesId != null) viewModel.loadEditSeries(editSeriesId)
        else viewModel.clearEditSeries()
    }

    var title         by remember { mutableStateOf("") }
    var description   by remember { mutableStateOf("") }
    var posterUrl     by remember { mutableStateOf("") }
    var bannerUrl     by remember { mutableStateOf("") }
    var trailerUrl    by remember { mutableStateOf("") }
    var externalId    by remember { mutableStateOf("") }
    var totalSeasons  by remember { mutableStateOf("1") }
    var releaseYear   by remember { mutableStateOf("") }
    var rating        by remember { mutableStateOf("") }
    var seriesType    by remember { mutableStateOf("anime") }
    var airingStatus  by remember { mutableStateOf("ongoing") }
    var publishStatus by remember { mutableStateOf("draft") }
    var selectedGenreIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var castMembers   by remember { mutableStateOf<List<AdminCastMember>>(emptyList()) }

    // AniList dialog state
    var aniListSearch     by remember { mutableStateOf("") }
    var showAniListDialog by remember { mutableStateOf(false) }

    // TMDB TV dialog state
    var tmdbSearch     by remember { mutableStateOf("") }
    var showTmdbDialog by remember { mutableStateOf(false) }

    var formInited by remember { mutableStateOf(false) }

    LaunchedEffect(state.editingSeries) {
        val s = state.editingSeries ?: return@LaunchedEffect
        if (formInited) return@LaunchedEffect
        title         = s.title
        description   = s.description ?: ""
        posterUrl     = s.posterUrl ?: ""
        bannerUrl     = s.bannerUrl ?: ""
        trailerUrl    = s.trailerUrl ?: ""
        externalId    = s.externalId ?: ""
        totalSeasons  = s.totalSeasons?.toString() ?: "1"
        releaseYear   = s.releaseYear?.toString() ?: ""
        rating        = s.rating?.toString() ?: ""
        seriesType    = s.type ?: "anime"
        airingStatus  = s.status ?: "ongoing"
        publishStatus = s.publishStatus ?: "draft"
        selectedGenreIds = s.genres.map { it.id }.toSet()
        castMembers = s.cast?.map {
            AdminCastMember(it.name, it.character, it.image ?: "", it.order)
        } ?: emptyList()
        formInited    = true
    }

    // Apply AniList cast after autofill selection
    LaunchedEffect(state.aniListCast) {
        if (state.aniListCast.isNotEmpty()) {
            castMembers = state.aniListCast.map {
                AdminCastMember(it.name, it.character ?: "", it.image ?: "", it.order)
            }
        }
    }

    // Apply server-resolved genre IDs from AniList autofill (spec §11.1, §15.4).
    // The ViewModel calls POST /api/admin/genres for each genreName so that genres
    // not yet in the local list are created and their ObjectIds are returned.
    LaunchedEffect(state.aniListResolvedGenreIds) {
        val ids = state.aniListResolvedGenreIds ?: return@LaunchedEffect
        selectedGenreIds = selectedGenreIds + ids
    }

    // Apply TMDB TV cast after web series autofill selection.
    // NOTE: genres arrive separately via tmdbResolvedGenreIds (spec §10.3, §15.3).
    LaunchedEffect(state.tmdbTvDetails) {
        val d = state.tmdbTvDetails ?: return@LaunchedEffect
        if (d.cast.isNotEmpty()) {
            castMembers = d.cast.map {
                AdminCastMember(it.name, it.character ?: "", it.image ?: "", it.order)
            }
        }
    }

    // Apply server-resolved genre IDs from TMDB TV autofill (spec §10.3, §15.3).
    LaunchedEffect(state.tmdbResolvedGenreIds) {
        val ids = state.tmdbResolvedGenreIds ?: return@LaunchedEffect
        selectedGenreIds = selectedGenreIds + ids
    }

    fun applyAniListResult(r: AniListSearchResult) {
        title       = r.title
        description = r.description ?: description
        posterUrl   = r.posterUrl ?: posterUrl
        bannerUrl   = r.bannerUrl ?: bannerUrl
        releaseYear = r.releaseYear?.toString() ?: releaseYear
        rating      = r.rating?.toString() ?: rating
        externalId  = r.externalId
        // Resolve genre names via POST /api/admin/genres (spec §11.1, §15.4).
        // aniListResolvedGenreIds will be updated once resolution completes.
        viewModel.resolveAniListGenres(r.genreNames)
        viewModel.loadAniListCast(r.externalId)
        showAniListDialog = false
        viewModel.clearAniList()
    }

    fun applyTmdbResult(r: TmdbSearchResult) {
        title        = r.title
        description  = r.description ?: description
        posterUrl    = r.posterUrl ?: posterUrl
        bannerUrl    = r.bannerUrl ?: bannerUrl
        releaseYear  = r.releaseYear?.toString() ?: releaseYear
        rating       = r.rating?.toString() ?: rating
        externalId   = r.externalId
        if (r.totalSeasons != null && r.totalSeasons > 0) totalSeasons = r.totalSeasons.toString()
        viewModel.loadTmdbTvDetails(r.externalId)
        showTmdbDialog = false
        viewModel.clearTmdb()
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
                    text       = if (isEdit) "Edit ${if (seriesType == "series") "Web Series" else "Anime"}"
                                 else        "Add ${if (seriesType == "series") "Web Series" else "Anime"}",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Type selector ─────────────────────────────────────────────────
            SectionLabel("Type")
            StatusSelector(seriesType, listOf("anime", "series")) { seriesType = it }

            // ── Autofill button — type-aware ──────────────────────────────────
            if (seriesType == "series") {
                // Web series → TMDB TV
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
            } else {
                // Anime → AniList
                OutlinedButton(
                    onClick = { showAniListDialog = true },
                    border  = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6)),
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Autofill from AniList", fontWeight = FontWeight.SemiBold)
                }
            }

            SectionLabel("Basic Info")
            AdminTextField("Title *", title, { title = it })
            AdminTextField("Description", description, { description = it }, minLines = 3)
            // Dynamic external ID label based on type
            AdminTextField(
                label    = if (seriesType == "series") "TMDB TV ID" else "AniList External ID",
                value    = externalId,
                onValueChange = { externalId = it }
            )
            AdminTextField("Release Year", releaseYear, { releaseYear = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            AdminTextField("Total Seasons", totalSeasons, { totalSeasons = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            AdminTextField("Rating (0–10)", rating, { rating = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)

            SectionLabel("URLs")
            AdminTextField("Poster URL", posterUrl, { posterUrl = it })
            AdminTextField("Banner URL", bannerUrl, { bannerUrl = it })
            AdminTextField("Trailer URL", trailerUrl, { trailerUrl = it })

            SectionLabel("Airing Status")
            StatusSelector(airingStatus, listOf("ongoing", "completed")) { airingStatus = it }

            SectionLabel("Publish Status")
            StatusSelector(publishStatus, listOf("draft", "published")) { publishStatus = it }

            SectionLabel("Genres")
            GenrePicker(
                genres      = state.genres,
                selectedIds = selectedGenreIds,
                onToggle    = { id ->
                    selectedGenreIds = if (id in selectedGenreIds) selectedGenreIds - id else selectedGenreIds + id
                }
            )

            // Loading indicators for cast fetching
            val isFetchingCast = state.isSearchingAniList || state.isLoadingTmdbDetails
            if (isFetchingCast) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = if (seriesType == "series") MARed else Color(0xFF8B5CF6),
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text  = if (seriesType == "series") "Fetching cast from TMDB…" else "Fetching cast from AniList…",
                        color = MATextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else if (castMembers.isNotEmpty()) {
                SectionLabel("Cast (${castMembers.size} members)")
                CastPreviewSection(castMembers)
            }

            state.saveError?.let { Text(it, color = MARed, fontSize = 13.sp) }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val genreList = selectedGenreIds.toList()
                    val castToSave = castMembers.takeIf { it.isNotEmpty() }
                    if (isEdit && editSeriesId != null) {
                        viewModel.updateSeries(editSeriesId, UpdateSeriesRequest(
                            title         = title.trim(),
                            description   = description.trim().takeIf { it.isNotEmpty() },
                            posterUrl     = posterUrl.trim().takeIf { it.isNotEmpty() },
                            bannerUrl     = bannerUrl.trim().takeIf { it.isNotEmpty() },
                            trailerUrl    = trailerUrl.trim().takeIf { it.isNotEmpty() },
                            externalId    = externalId.trim().takeIf { it.isNotEmpty() },
                            totalSeasons  = totalSeasons.toIntOrNull(),
                            releaseYear   = releaseYear.toIntOrNull(),
                            genres        = genreList.takeIf { it.isNotEmpty() },
                            cast          = castToSave,
                            status        = airingStatus,
                            type          = seriesType,
                            publishStatus = publishStatus,
                            rating        = rating.toDoubleOrNull()
                        ), onSaved)
                    } else {
                        viewModel.createSeries(CreateSeriesRequest(
                            title         = title.trim(),
                            description   = description.trim().takeIf { it.isNotEmpty() },
                            posterUrl     = posterUrl.trim().takeIf { it.isNotEmpty() },
                            bannerUrl     = bannerUrl.trim().takeIf { it.isNotEmpty() },
                            trailerUrl    = trailerUrl.trim().takeIf { it.isNotEmpty() },
                            externalId    = externalId.trim().takeIf { it.isNotEmpty() },
                            totalSeasons  = totalSeasons.toIntOrNull(),
                            releaseYear   = releaseYear.toIntOrNull(),
                            genres        = genreList.takeIf { it.isNotEmpty() },
                            cast          = castToSave,
                            status        = airingStatus,
                            type          = seriesType,
                            publishStatus = publishStatus,
                            rating        = rating.toDoubleOrNull()
                        ), onSaved)
                    }
                },
                enabled  = title.isNotBlank() && !state.isSaving,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MARed,
                    contentColor           = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.4f),
                    disabledContentColor   = Color.White.copy(alpha = 0.5f)
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEdit) "Save Changes" else "Create", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── AniList autofill dialog ───────────────────────────────────────────────
    if (showAniListDialog) {
        Dialog(onDismissRequest = { showAniListDialog = false; viewModel.clearAniList() }) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MACard,
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search AniList", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = aniListSearch,
                        onValueChange = { aniListSearch = it; if (it.length >= 2) viewModel.searchAniList(it) },
                        placeholder   = { Text("Anime title…", color = MATextSecondary) },
                        leadingIcon   = { Icon(Icons.Default.Search, null, tint = MATextSecondary) },
                        colors        = adminTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    if (state.isSearchingAniList) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MARed, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(state.aniListResults) { result ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable { applyAniListResult(result) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(result.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        if (result.releaseYear != null) {
                                            Text(result.releaseYear.toString(), color = MATextSecondary, fontSize = 12.sp)
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

    // ── TMDB TV autofill dialog ───────────────────────────────────────────────
    if (showTmdbDialog) {
        Dialog(onDismissRequest = { showTmdbDialog = false; viewModel.clearTmdb() }) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MACard,
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search TMDB (TV)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = tmdbSearch,
                        onValueChange = { tmdbSearch = it; if (it.length >= 2) viewModel.searchTmdb(it) },
                        placeholder   = { Text("TV series title…", color = MATextSecondary) },
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
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (result.releaseYear != null)
                                                Text(result.releaseYear.toString(), color = MATextSecondary, fontSize = 12.sp)
                                            if (result.totalSeasons != null)
                                                Text("${result.totalSeasons} seasons", color = MATextSecondary, fontSize = 12.sp)
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
