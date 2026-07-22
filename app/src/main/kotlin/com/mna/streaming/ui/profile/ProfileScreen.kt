package com.mna.streaming.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayCircle
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

// ── Status badge helpers ──────────────────────────────────────────────────────

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

private fun formatEpochDate(epochMillis: Long): String {
    return try {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
    } catch (_: Exception) { "" }
}

// ── Root screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onBackClick: () -> Unit,
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
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Profile header ────────────────────────────────────────────────
            ProfileHeader(
                user          = uiState.user,
                watchedCount  = uiState.watchHistory.size,
                watchlistCount = uiState.watchlist.size
            )

            // ── Tab row ───────────────────────────────────────────────────────
            val tabs = listOf("Watch History", "Watchlist", "Requests", "About")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MADark,
                contentColor     = MARed,
                edgePadding      = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected             = selectedTab == index,
                        onClick              = { selectedTab = index },
                        selectedContentColor   = Color.White,
                        unselectedContentColor = MATextSecondary,
                        text = {
                            Text(
                                text       = title,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 13.sp
                            )
                        }
                    )
                }
            }

            // ── Tab content ───────────────────────────────────────────────────
            when (selectedTab) {
                0 -> WatchHistoryTab(
                    history    = uiState.watchHistory,
                    isLoading  = uiState.isLoadingHistory
                )
                1 -> WatchlistTab(
                    watchlist  = uiState.watchlist,
                    isLoading  = uiState.isLoadingWatchlist
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

    // ── New request bottom sheet ──────────────────────────────────────────────
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to MASurface,
                        1f to MADark
                    )
                )
            )
            .padding(top = 24.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Avatar — first letter of username in a red circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MARed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color      = Color.White,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            // Username
            Text(
                text       = user?.name ?: "",
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text     = user?.email ?: "",
                color    = MATextSecondary,
                fontSize = 14.sp
            )

            // Admin badge — only shown for admin role
            if (user?.role == "admin") {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape    = RoundedCornerShape(4.dp),
                    color    = MARed.copy(alpha = 0.12f),
                    modifier = Modifier.border(
                        width  = 1.dp,
                        color  = MARed.copy(alpha = 0.45f),
                        shape  = RoundedCornerShape(4.dp)
                    )
                ) {
                    Text(
                        text       = "Admin",
                        color      = MARed,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Stats row: Watched | Watchlist ────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Watched stat card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    color    = MACard
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text       = watchedCount.toString(),
                            color      = Color.White,
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text          = "WATCHED",
                            color         = MATextSecondary,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Watchlist stat card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    color    = MACard
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (watchlistCount > 0) {
                            Text(
                                text       = watchlistCount.toString(),
                                color      = Color.White,
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint               = MATextSecondary,
                                modifier           = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text          = "WATCHLIST",
                            color         = MATextSecondary,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Watch History tab ─────────────────────────────────────────────────────────

@Composable
private fun WatchHistoryTab(
    history: List<LocalWatchEntry>,
    isLoading: Boolean
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
                Column(
                    modifier            = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint               = MATextSecondary.copy(alpha = 0.5f),
                        modifier           = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text       = "Nothing watched yet",
                        color      = MATextSecondary,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text  = "Tap Play on any movie to start\nyour watch history",
                        color = MATextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history, key = { it.movieId }) { entry ->
                        WatchHistoryCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchHistoryCard(entry: LocalWatchEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = MACard)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            AsyncImage(
                model              = entry.posterUrl,
                contentDescription = entry.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MASurface)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Type badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MARed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text     = entry.targetType.uppercase(),
                        color    = MARed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = entry.title,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyLarge,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Date
            Text(
                text  = formatEpochDate(entry.updatedAt),
                color = MATextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ── Watchlist tab ─────────────────────────────────────────────────────────────

@Composable
private fun WatchlistTab(
    watchlist: List<LocalWatchlistItem>,
    isLoading: Boolean
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
                Column(
                    modifier            = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint               = MATextSecondary.copy(alpha = 0.5f),
                        modifier           = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text       = "Your watchlist is empty",
                        color      = MATextSecondary,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text  = "Tap the bookmark icon on any movie\nto save it here",
                        color = MATextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(watchlist, key = { it.movieId }) { item ->
                        WatchlistCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistCard(item: LocalWatchlistItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = MACard)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            AsyncImage(
                model              = item.posterUrl,
                contentDescription = item.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MASurface)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.title,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyLarge,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text  = item.releaseYear.toString(),
                        color = MATextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (item.rating > 0) {
                        Text("•", color = MATextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text  = "★ ${String.format("%.1f", item.rating)}",
                            color = MAGold,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Added date
            Text(
                text  = formatEpochDate(item.addedAt),
                color = MATextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
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
                    ) {
                        Text("Retry")
                    }
                }
            }

            uiState.requests.isEmpty() -> {
                Column(
                    modifier            = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = Icons.Default.Inbox,
                        contentDescription = null,
                        tint               = MATextSecondary.copy(alpha = 0.5f),
                        modifier           = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text       = "No requests yet",
                        color      = MATextSecondary,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text  = "Tap the button below to ask the admin\nto add a title",
                        color = MATextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.requests, key = { it.id }) { request ->
                        RequestCard(
                            request      = request,
                            isCancelling = uiState.cancellingId == request.id,
                            onCancel     = { onCancelRequest(request.id) }
                        )
                    }
                    // Space for the FAB
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // FAB — always visible in Requests tab
        ExtendedFloatingActionButton(
            onClick          = onNewRequest,
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor   = MARed,
            contentColor     = Color.White,
            icon             = { Icon(Icons.Default.Add, contentDescription = null) },
            text             = { Text("Request", fontWeight = FontWeight.SemiBold) }
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

            // ── Header: title + status badge ──────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                verticalAlignment    = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text      = request.title,
                        color     = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style     = MaterialTheme.typography.bodyLarge,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    // Type chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MATextSecondary.copy(alpha = 0.12f)
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

                // Status badge
                Surface(
                    shape    = RoundedCornerShape(4.dp),
                    color    = color.copy(alpha = 0.12f),
                    modifier = Modifier.border(
                        width  = 1.dp,
                        color  = color.copy(alpha = 0.35f),
                        shape  = RoundedCornerShape(4.dp)
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

            // ── User note ─────────────────────────────────────────────────────
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

            // ── Admin reply ───────────────────────────────────────────────────
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

            // ── Cancel button — only for pending requests ─────────────────────
            if (request.status == "pending") {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isCancelling) {
                        Box(
                            modifier = Modifier.padding(12.dp),
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
                    color    = Color.White.copy(alpha = 0.07f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                InfoRow(label = "Email", value = user?.email ?: "—")
                HorizontalDivider(
                    color    = Color.White.copy(alpha = 0.07f),
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
            border   = BorderStroke(1.dp, MARed.copy(alpha = 0.55f)),
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

// ── New request bottom sheet ──────────────────────────────────────────────────

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

            // ── Title ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title *") },
                placeholder   = { Text("e.g. Interstellar") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MARed,
                    focusedLabelColor    = MARed,
                    cursorColor          = MARed,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    unfocusedLabelColor  = MATextSecondary,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedPlaceholderColor   = MATextSecondary,
                    unfocusedPlaceholderColor = MATextSecondary
                )
            )

            Spacer(Modifier.height(18.dp))

            // ── Type selector ─────────────────────────────────────────────────
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
                        label    = {
                            Text(type.replaceFirstChar { it.uppercaseChar() })
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MARed,
                            selectedLabelColor     = Color.White,
                            containerColor         = MACard,
                            labelColor             = MATextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = selectedType == type,
                            borderColor         = Color.White.copy(alpha = 0.15f),
                            selectedBorderColor = MARed
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Note ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { if (it.length <= 500) note = it },
                label         = { Text("Note (optional)") },
                placeholder   = { Text("Year, season, source link, etc.") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                maxLines      = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MARed,
                    focusedLabelColor    = MARed,
                    cursorColor          = MARed,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    unfocusedLabelColor  = MATextSecondary,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedPlaceholderColor   = MATextSecondary,
                    unfocusedPlaceholderColor = MATextSecondary
                ),
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
                        onSubmit(
                            title.trim(),
                            selectedType,
                            note.trim().ifBlank { null }
                        )
                    }
                },
                enabled  = title.isNotBlank() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MARed,
                    contentColor           = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.4f),
                    disabledContentColor   = Color.White.copy(alpha = 0.6f)
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
                    Text("Submit Request", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
