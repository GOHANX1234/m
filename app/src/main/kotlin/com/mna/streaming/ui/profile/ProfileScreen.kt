package com.mna.streaming.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.data.LocalWatchEntry
import com.mna.streaming.data.LocalWatchlistItem
import com.mna.streaming.network.models.ContentRequest
import com.mna.streaming.network.models.SessionUser
import com.mna.streaming.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Helpers ───────────────────────────────────────────────────────────────────

private val StatusPending    = Color(0xFFF59E0B)
private val StatusInProgress = Color(0xFF3B82F6)
private val StatusFulfilled  = Color(0xFF10B981)

private fun statusColor(status: String): Color = when (status) {
    "pending"     -> StatusPending
    "in_progress" -> StatusInProgress
    "fulfilled"   -> StatusFulfilled
    "rejected"    -> MARed
    else          -> MATextSecondary
}

private fun statusLabel(status: String): String = when (status) {
    "pending"     -> "Pending"
    "in_progress" -> "In Progress"
    "fulfilled"   -> "Fulfilled"
    "rejected"    -> "Rejected"
    else          -> status
}

private fun formatEpochDate(epochMillis: Long): String = try {
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
} catch (_: Exception) { "" }

// ── Root screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (movieId: String) -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewRequestSheet by remember { mutableStateOf(false) }

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
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = Color.White
                    )
                }
                Text(
                    text       = "Profile",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ProfileHeader(
                user           = uiState.user,
                watchedCount   = uiState.watchHistory.size,
                watchlistCount = uiState.watchlist.size
            )

            val tabs = listOf("Watch History", "Watchlist", "Requests", "About")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MASurface,
                contentColor     = MARed,
                edgePadding      = 0.dp,
                indicator        = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = tabPositions[selectedTab].left)
                            .width(tabPositions[selectedTab].width),
                        color     = MARed,
                        height    = 2.dp
                    )
                },
                divider          = {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected               = selectedTab == index,
                        onClick                = { selectedTab = index },
                        selectedContentColor   = Color.White,
                        unselectedContentColor = MATextSecondary,
                        modifier               = Modifier.padding(vertical = 2.dp),
                        text = {
                            Text(
                                text       = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize   = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> WatchHistoryTab(
                    history      = uiState.watchHistory,
                    isLoading    = uiState.isLoadingHistory,
                    onMovieClick = onMovieClick
                )
                1 -> WatchlistTab(
                    watchlist    = uiState.watchlist,
                    isLoading    = uiState.isLoadingWatchlist,
                    onMovieClick = onMovieClick
                )
                2 -> RequestsTab(
                    uiState         = uiState,
                    onNewRequest    = { showNewRequestSheet = true },
                    onCancelRequest = { viewModel.cancelRequest(it) },
                    onRetry         = { viewModel.loadRequests() }
                )
                3 -> AboutTab(
                    user      = uiState.user,
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showNewRequestSheet) {
        NewRequestSheet(
            isSubmitting = uiState.isSubmitting,
            submitError  = uiState.submitError,
            onDismiss    = {
                showNewRequestSheet = false
                viewModel.clearSubmitError()
            },
            onSubmit = { title, type, note ->
                viewModel.submitRequest(title, type, note) {
                    showNewRequestSheet = false
                }
            }
        )
    }
}

// ── Profile header ────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    user: SessionUser?,
    watchedCount: Int,
    watchlistCount: Int
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MASurface, MADark)
                )
            )
            .padding(top = 20.dp, bottom = 18.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MARed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color      = Color.White,
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // Name row + admin badge inline
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = user?.name ?: "",
                color      = Color.White,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (user?.role == "admin") {
                Surface(
                    shape  = RoundedCornerShape(4.dp),
                    color  = MARed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MARed.copy(alpha = 0.5f))
                ) {
                    Text(
                        text       = "Admin",
                        color      = MARed,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text     = user?.email ?: "",
            color    = MATextSecondary,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(18.dp))

        // Stats row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value    = watchedCount.toString(),
                label    = "WATCHED"
            )
            StatCard(
                modifier   = Modifier.weight(1f),
                value      = if (watchlistCount > 0) watchlistCount.toString() else null,
                label      = "WATCHLIST",
                showIcon   = watchlistCount == 0
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String?,
    label: String,
    showIcon: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = MACard
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showIcon) {
                Icon(
                    imageVector        = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint               = MATextSecondary,
                    modifier           = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text       = value ?: "0",
                    color      = Color.White,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text          = label,
                color         = MATextSecondary,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Watch History tab ─────────────────────────────────────────────────────────

@Composable
private fun WatchHistoryTab(
    history: List<LocalWatchEntry>,
    isLoading: Boolean,
    onMovieClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color    = MARed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            history.isEmpty() -> {
                EmptyState(
                    modifier    = Modifier.align(Alignment.Center),
                    icon        = Icons.Default.PlayCircleOutline,
                    title       = "Nothing watched yet",
                    description = "Tap Play on any movie to start your watch history"
                )
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(history, key = { it.movieId }) { entry ->
                        MediaRow(
                            posterUrl  = entry.posterUrl,
                            title      = entry.title,
                            badge      = entry.targetType.uppercase(),
                            badgeColor = MARed,
                            meta       = formatEpochDate(entry.updatedAt),
                            onClick    = { onMovieClick(entry.movieId) }
                        )
                    }
                }
            }
        }
    }
}

// ── Watchlist tab ─────────────────────────────────────────────────────────────

@Composable
private fun WatchlistTab(
    watchlist: List<LocalWatchlistItem>,
    isLoading: Boolean,
    onMovieClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color    = MARed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            watchlist.isEmpty() -> {
                EmptyState(
                    modifier    = Modifier.align(Alignment.Center),
                    icon        = Icons.Default.Bookmark,
                    title       = "Watchlist is empty",
                    description = "Tap the bookmark icon on any movie to save it here"
                )
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(watchlist, key = { it.movieId }) { item ->
                        val meta = buildString {
                            append(item.releaseYear)
                            if (item.rating > 0) append("  ★ ${String.format("%.1f", item.rating)}")
                        }
                        MediaRow(
                            posterUrl  = item.posterUrl,
                            title      = item.title,
                            badge      = "MOVIE",
                            badgeColor = Color(0xFF8B5CF6),   // purple — distinct from history red
                            meta       = meta,
                            onClick    = { onMovieClick(item.movieId) }
                        )
                    }
                }
            }
        }
    }
}

// ── Shared media row (used by both lists) ─────────────────────────────────────

@Composable
private fun MediaRow(
    posterUrl: String,
    title: String,
    badge: String,
    badgeColor: Color,
    meta: String,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MADark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        AsyncImage(
            model              = posterUrl,
            contentDescription = title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(width = 52.dp, height = 72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MASurface)
        )

        Spacer(Modifier.width(14.dp))

        // Text block
        Column(modifier = Modifier.weight(1f)) {
            // Badge
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text       = badge,
                    color      = badgeColor,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text       = title,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 15.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = meta,
                    color = MATextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Chevron
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint               = MATextSecondary.copy(alpha = 0.5f),
            modifier           = Modifier.size(14.dp)
        )
    }

    HorizontalDivider(
        color    = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.padding(start = 82.dp)  // aligns under the title, not the poster
    )
}

// ── Shared empty state ────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier            = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MATextSecondary.copy(alpha = 0.35f),
            modifier           = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text       = title,
            color      = MATextSecondary,
            fontWeight = FontWeight.SemiBold,
            style      = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = description,
            color     = MATextSecondary.copy(alpha = 0.55f),
            style     = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ── Requests tab ──────────────────────────────────────────────────────────────

@Composable
private fun RequestsTab(
    uiState: ProfileUiState,
    onNewRequest: () -> Unit,
    onCancelRequest: (String) -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoadingRequests -> {
                CircularProgressIndicator(
                    color    = MARed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.requestsError != null -> {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = uiState.requestsError,
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = onRetry,
                        colors  = ButtonDefaults.textButtonColors(contentColor = MARed)
                    ) { Text("Retry") }
                }
            }

            uiState.requests.isEmpty() -> {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyState(
                        icon        = Icons.Default.Inbox,
                        title       = "No requests yet",
                        description = "Tap the button below to ask the admin to add a title"
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.requests, key = { it.id }) { request ->
                        RequestCard(
                            request      = request,
                            isCancelling = uiState.cancellingId == request.id,
                            onCancel     = { onCancelRequest(request.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick        = onNewRequest,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MARed,
            contentColor   = Color.White,
            icon           = { Icon(Icons.Default.Add, contentDescription = null) },
            text           = { Text("Request", fontWeight = FontWeight.SemiBold) }
        )
    }
}

// ── Request card ──────────────────────────────────────────────────────────────

@Composable
private fun RequestCard(
    request: ContentRequest,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    val color = statusColor(request.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = MACard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = request.title,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.bodyLarge,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MATextSecondary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text     = request.type.replaceFirstChar { it.uppercaseChar() },
                            color    = MATextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Surface(
                    shape    = RoundedCornerShape(4.dp),
                    color    = color.copy(alpha = 0.10f),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                ) {
                    Text(
                        text       = statusLabel(request.status),
                        color      = color,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!request.note.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text     = request.note,
                    color    = MATextSecondary,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!request.adminNote.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F1E3A)
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = "Admin:",
                            color      = Color(0xFF60A5FA),
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text  = request.adminNote,
                            color = Color(0xFFBFDBFE),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (request.status == "pending") {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isCancelling) {
                        Box(
                            modifier         = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color       = MARed,
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onCancel,
                            colors  = ButtonDefaults.textButtonColors(contentColor = MARed)
                        ) {
                            Text("Cancel Request", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── About tab ─────────────────────────────────────────────────────────────────

@Composable
private fun AboutTab(
    user: SessionUser?,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text          = "ACCOUNT",
            color         = MATextSecondary,
            style         = MaterialTheme.typography.labelSmall,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            shape    = RoundedCornerShape(10.dp),
            color    = MACard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                InfoRow(label = "Username", value = user?.name ?: "—")
                HorizontalDivider(
                    color    = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                InfoRow(label = "Email", value = user?.email ?: "—")
                HorizontalDivider(
                    color    = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                InfoRow(
                    label      = "Role",
                    value      = if (user?.role == "admin") "Administrator" else "Member",
                    valueColor = if (user?.role == "admin") MARed else Color.White
                )
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick  = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MARed),
            border   = BorderStroke(1.dp, MARed.copy(alpha = 0.5f)),
            shape    = RoundedCornerShape(8.dp)
        ) {
            Text("Sign Out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            color = MATextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text       = value,
            color      = valueColor,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier
                .padding(start = 16.dp)
                .weight(1f, fill = false)
        )
    }
}

// ── New-request bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewRequestSheet(
    isSubmitting: Boolean,
    submitError: String?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, type: String, note: String?) -> Unit
) {
    var title        by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("movie") }
    var note         by remember { mutableStateOf("") }
    val types = listOf("movie", "series", "anime")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MASurface,
        contentColor     = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .imePadding()
        ) {
            Text(
                text       = "Request Content",
                color      = Color.White,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Ask the admin to add a movie, series, or anime",
                color = MATextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value           = title,
                onValueChange   = { title = it },
                label           = { Text("Title *") },
                placeholder     = { Text("e.g. Interstellar") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors          = redFieldColors()
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text  = "Type",
                color = MATextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick  = { selectedType = type },
                        label    = { Text(type.replaceFirstChar { it.uppercaseChar() }) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MARed,
                            selectedLabelColor     = Color.White,
                            containerColor         = MACard,
                            labelColor             = MATextSecondary
                        ),
                        border   = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = selectedType == type,
                            borderColor         = Color.White.copy(alpha = 0.12f),
                            selectedBorderColor = MARed
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value         = note,
                onValueChange = { if (it.length <= 500) note = it },
                label         = { Text("Note (optional)") },
                placeholder   = { Text("Year, season, source link, etc.") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                maxLines      = 4,
                colors        = redFieldColors(),
                supportingText = {
                    Text("${note.length}/500", color = MATextSecondary)
                }
            )

            if (submitError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = submitError,
                    color = MARed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick  = {
                    if (title.isNotBlank()) {
                        onSubmit(title.trim(), selectedType, note.trim().ifBlank { null })
                    }
                },
                enabled  = title.isNotBlank() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MARed,
                    contentColor           = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.35f),
                    disabledContentColor   = Color.White.copy(alpha = 0.5f)
                ),
                shape    = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = "Submit Request",
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Shared text field colours ─────────────────────────────────────────────────

@Composable
private fun redFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = MARed,
    focusedLabelColor         = MARed,
    cursorColor               = MARed,
    unfocusedBorderColor      = Color.White.copy(alpha = 0.18f),
    unfocusedLabelColor       = MATextSecondary,
    focusedTextColor          = Color.White,
    unfocusedTextColor        = Color.White,
    focusedPlaceholderColor   = MATextSecondary,
    unfocusedPlaceholderColor = MATextSecondary
)
