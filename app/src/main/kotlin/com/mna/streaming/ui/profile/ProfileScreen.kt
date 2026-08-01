package com.mna.streaming.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mna.streaming.data.LocalWatchEntry
import com.mna.streaming.data.LocalWatchlistItem
import com.mna.streaming.network.models.ContentRequest
import com.mna.streaming.network.models.SessionUser
import com.mna.streaming.security.NativeApiSecurity
import com.mna.streaming.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// â”€â”€ Status Color & Formatting Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))
} catch (_: Exception) { "" }

private fun formatJoinDate(iso: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
    val date = parser.parse(iso)
        ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(iso)
        ?: return "â€”"
    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
} catch (_: Exception) { "â€”" }

private fun nativeAboutValue(read: () -> String, fallback: String): String =
    try {
        read().takeIf { it.isNotBlank() } ?: fallback
    } catch (_: Throwable) {
        fallback
    }

private fun openTelegram(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: android.content.ActivityNotFoundException) {
        // Fallback or leave unchanged if no handler
    }
}

// â”€â”€ Main Profile Screen Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (movieId: String) -> Unit,
    onAnimeClick: (seriesId: String) -> Unit,
    onAdminClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewRequestSheet by remember { mutableStateOf(false) }

    // Refresh history and watchlist whenever screen becomes RESUMED
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadHistory()
            viewModel.loadWatchlist()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Glassmorphic Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MACard)
                        .border(1.dp, MABorderSubtle, CircleShape)
                        .pressScaleClickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "My Profile",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Refresh Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MACard)
                        .border(1.dp, MABorderSubtle, CircleShape)
                        .pressScaleClickable {
                            viewModel.loadUserAndStats()
                            viewModel.loadHistory()
                            viewModel.loadWatchlist()
                            viewModel.loadRequests()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MATextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Hero Profile Header Card
            ProfileHeader(
                user = uiState.user,
                watchedCount = uiState.watchedCount,
                watchlistCount = uiState.watchlistCount,
                requestsCount = uiState.requests.size
            )

            // Segmented Scrollable Tab Bar
            val tabs = remember(uiState.watchHistory, uiState.watchlist, uiState.requests) {
                listOf(
                    "History${if (uiState.watchHistory.isNotEmpty()) " (${uiState.watchHistory.size})" else ""}",
                    "Watchlist${if (uiState.watchlist.isNotEmpty()) " (${uiState.watchlist.size})" else ""}",
                    "Requests${if (uiState.requests.isNotEmpty()) " (${uiState.requests.size})" else ""}",
                    "About & Settings"
                )
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MADark,
                contentColor = Color.White,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = MABorderSubtle, thickness = 1.dp) },
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(MARed)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val active = selectedTab == index
                    Tab(
                        selected = active,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (active) MARedLight else MATextSecondary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Crossfade Tab Views
            Crossfade(
                targetState = selectedTab,
                label = "profileTabCrossfade",
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> WatchHistoryTab(
                        history = uiState.watchHistory,
                        isLoading = uiState.isLoadingHistory,
                        onMovieClick = onMovieClick,
                        onAnimeClick = onAnimeClick
                    )
                    1 -> WatchlistTab(
                        watchlist = uiState.watchlist,
                        isLoading = uiState.isLoadingWatchlist,
                        onMovieClick = onMovieClick,
                        onAnimeClick = onAnimeClick
                    )
                    2 -> RequestsTab(
                        uiState = uiState,
                        onNewRequest = { showNewRequestSheet = true },
                        onCancelRequest = { viewModel.cancelRequest(it) },
                        onRetry = { viewModel.loadRequests() }
                    )
                    3 -> AboutTab(
                        user = uiState.user,
                        joinedAt = uiState.joinedAt,
                        onSignOut = onSignOut,
                        onAdminClick = onAdminClick
                    )
                }
            }
        }
    }

    // Modal Sheet for submitting new content requests
    if (showNewRequestSheet) {
        NewRequestSheet(
            isSubmitting = uiState.isSubmitting,
            submitError = uiState.submitError,
            onDismiss = {
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

// â”€â”€ Profile Header Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ProfileHeader(
    user: SessionUser?,
    watchedCount: Int,
    watchlistCount: Int,
    requestsCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MACardElevated,
                        MACard
                    )
                )
            )
            .border(1.dp, MABorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with glowing ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(MARed, MAGold, MARedLight, MARed)
                        )
                    )
                    .padding(2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF2A080C), Color(0xFF121212))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user?.image.isNullOrBlank()) {
                        AsyncImage(
                            model = user?.image,
                            contentDescription = user?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Nickname + VIP / Admin Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = user?.name ?: "Guest User",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val isAdmin = user?.role == "admin"
                val badgeText = if (isAdmin) "ADMIN" else "MEMBER"
                val badgeBg = if (isAdmin) MARed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                val badgeFg = if (isAdmin) MARedLight else Color.White

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeFg.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                            contentDescription = null,
                            tint = badgeFg,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = badgeText,
                            color = badgeFg,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = user?.email ?: "Sign in to sync watchlist",
                color = MATextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            // Quick Stats Row (Watched, Watchlist, Requests)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(value = watchedCount.toString(), label = "WATCHED")
                VerticalDivider(color = MABorderSubtle, modifier = Modifier.height(28.dp))
                StatItem(value = watchlistCount.toString(), label = "WATCHLIST")
                VerticalDivider(color = MABorderSubtle, modifier = Modifier.height(28.dp))
                StatItem(value = requestsCount.toString(), label = "REQUESTS")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = MATextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// â”€â”€ Watch History Tab Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun WatchHistoryTab(
    history: List<LocalWatchEntry>,
    isLoading: Boolean,
    onMovieClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MARed, modifier = Modifier.size(32.dp))
                }
            }

            history.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "No Watch History Yet",
                    description = "Titles you play will automatically appear here so you can pick up where you left off."
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history, key = { it.movieId }) { entry ->
                        val isAnime = entry.targetType == "Episode" || entry.seriesId != null
                        MediaRowCard(
                            posterUrl = entry.posterUrl,
                            title = entry.title,
                            badge = if (isAnime) "ANIME" else "MOVIE",
                            badgeColor = if (isAnime) MARed else Color(0xFF8B5CF6),
                            meta = if (entry.updatedAt > 0) "Watched ${formatEpochDate(entry.updatedAt)}" else "",
                            onClick = {
                                if (isAnime) {
                                    val seriesId = entry.seriesId ?: entry.movieId
                                    onAnimeClick(seriesId)
                                } else {
                                    onMovieClick(entry.movieId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€ Watchlist Tab Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun WatchlistTab(
    watchlist: List<LocalWatchlistItem>,
    isLoading: Boolean,
    onMovieClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MARed, modifier = Modifier.size(32.dp))
                }
            }

            watchlist.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "Your Watchlist is Empty",
                    description = "Tap the bookmark icon on any movie or anime to save it for later."
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(watchlist, key = { it.movieId }) { item ->
                        val isAnime = item.targetType == "anime" || item.targetType == "series"
                        val meta = buildString {
                            if (item.releaseYear > 0) append(item.releaseYear)
                            if (item.rating > 0) {
                                if (isNotEmpty()) append(" â€¢ ")
                                append("â˜… %.1f".format(item.rating))
                            }
                        }

                        MediaRowCard(
                            posterUrl = item.posterUrl,
                            title = item.title,
                            badge = if (isAnime) "ANIME" else "MOVIE",
                            badgeColor = if (isAnime) MARed else Color(0xFF8B5CF6),
                            meta = meta,
                            onClick = {
                                if (isAnime) onAnimeClick(item.movieId)
                                else onMovieClick(item.movieId)
                            }
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€ Media Row Card Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MediaRowCard(
    posterUrl: String,
    title: String,
    badge: String,
    badgeColor: Color,
    meta: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MACard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MABorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail Poster
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MACardElevated)
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(badgeColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meta,
                        color = MATextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Right Action Arrow Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MARed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Open",
                    tint = MARed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// â”€â”€ Requests Tab Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MARed, modifier = Modifier.size(32.dp))
                }
            }

            uiState.requestsError != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.requestsError,
                        color = MATextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = MARed)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.requests.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = "No Content Requests Yet",
                    description = "Can't find a movie or anime? Tap the Request button below to let our team know!"
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.requests, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            isCancelling = uiState.cancellingId == request.id,
                            onCancel = { onCancelRequest(request.id) }
                        )
                    }
                }
            }
        }

        // Extended Floating Action Button
        ExtendedFloatingActionButton(
            onClick = onNewRequest,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp)
                .pressScaleClickable(onClick = onNewRequest),
            containerColor = MARed,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Request Content", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        )
    }
}

@Composable
private fun RequestCard(
    request: ContentRequest,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    val color = statusColor(request.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MACard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MABorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MACardElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = request.type.replaceFirstChar { it.uppercaseChar() },
                            color = MATextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Glowing Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f))
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = statusLabel(request.status),
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!request.note.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = request.note,
                    color = MATextSecondary,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Admin response box
            if (!request.adminNote.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1E3A))
                        .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Admin Response:",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = request.adminNote,
                                color = Color(0xFFBFDBFE),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Cancel Button for Pending Requests
            if (request.status == "pending") {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MABorderSubtle, thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            color = MARed,
                            modifier = Modifier.padding(8.dp).size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(contentColor = MARed)
                        ) {
                            Text("Cancel Request", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€ About & Settings Tab Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AboutTab(
    user: SessionUser?,
    joinedAt: String?,
    onSignOut: () -> Unit,
    onAdminClick: () -> Unit = {}
) {
    val isAdmin = user?.role == "admin"
    val developerCredit = remember {
        nativeAboutValue(
            read = { NativeApiSecurity.getDeveloperCredit() },
            fallback = "Developed By @Gohan52"
        )
    }
    val developerTelegramUrl = remember {
        nativeAboutValue(
            read = { NativeApiSecurity.getDeveloperTelegramUrl() },
            fallback = "https://t.me/Gohan52"
        )
    }
    val telegramChannelUrl = remember {
        nativeAboutValue(
            read = { NativeApiSecurity.getTelegramChannelUrl() },
            fallback = "https://t.me/ClerXin"
        )
    }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ACCOUNT SECTION
        item {
            Column {
                SectionTitle("ACCOUNT DETAILS")
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MACard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MABorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        InfoRow(icon = Icons.Default.Person, label = "Username", value = user?.name ?: "Guest")
                        AboutDivider()
                        InfoRow(icon = Icons.Default.Email, label = "Email", value = user?.email ?: "Not signed in")
                        AboutDivider()
                        InfoRow(
                            icon = Icons.Default.Shield,
                            label = "Role",
                            value = if (isAdmin) "Administrator" else "Member",
                            valueColor = if (isAdmin) MARedLight else Color.White
                        )
                        if (!joinedAt.isNullOrBlank()) {
                            AboutDivider()
                            InfoRow(
                                icon = Icons.Default.CalendarToday,
                                label = "Joined On",
                                value = formatJoinDate(joinedAt)
                            )
                        }
                    }
                }
            }
        }

        // ADMINISTRATION SECTION (If Admin)
        if (isAdmin) {
            item {
                Column {
                    SectionTitle("ADMINISTRATION")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScaleClickable(onClick = onAdminClick),
                        colors = CardDefaults.cardColors(containerColor = MARed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MARed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MARed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Admin Management Panel",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Manage movies, anime, episodes & user requests",
                                    color = MATextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MARedLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // COMMUNITY & DEVELOPER
        item {
            Column {
                SectionTitle("COMMUNITY & DEVELOPER")
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MACard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MABorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AboutLinkRow(
                            icon = Icons.Default.Code,
                            label = developerCredit,
                            supporting = "Contact lead developer on Telegram",
                            onClick = { openTelegram(context, developerTelegramUrl) }
                        )
                        AboutDivider()
                        AboutLinkRow(
                            icon = Icons.AutoMirrored.Filled.Send,
                            label = "Official Telegram Channel",
                            supporting = telegramChannelUrl,
                            onClick = { openTelegram(context, telegramChannelUrl) }
                        )
                    }
                }
            }
        }

        // SIGN OUT BUTTON
        item {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MACard,
                    contentColor = MARed
                ),
                border = BorderStroke(1.dp, MARed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .pressScaleClickable(onClick = onSignOut)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sign Out Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// â”€â”€ About Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MATextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(color = MABorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun AboutLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    supporting: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MARed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = supporting,
                color = MATextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MATextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MATextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = MATextSecondary,
                fontSize = 13.sp
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MACard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MATextTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                color = MATextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// â”€â”€ New Request Modal Bottom Sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewRequestSheet(
    isSubmitting: Boolean,
    submitError: String?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, type: String, note: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("movie") }
    var note by remember { mutableStateOf("") }
    val types = listOf("movie", "series", "anime")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MASurface,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .imePadding()
        ) {
            Text(
                text = "Request Content",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Let our admin team know what title you'd like added next.",
                color = MATextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("e.g. Solo Leveling, Interstellar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = redFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Content Type",
                color = MATextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { type ->
                    val selected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MARed else MACard)
                            .border(1.dp, if (selected) MARed else MABorderSubtle, RoundedCornerShape(8.dp))
                            .pressScaleClickable { selectedType = type }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type.replaceFirstChar { it.uppercaseChar() },
                            color = if (selected) Color.White else MATextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("Season, year, language, source link, etc.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp),
                colors = redFieldColors(),
                supportingText = {
                    Text("${note.length}/500", color = MATextSecondary, fontSize = 11.sp)
                }
            )

            if (!submitError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = submitError,
                    color = MARed,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(title.trim(), selectedType, note.trim().ifBlank { null })
                    }
                },
                enabled = title.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressScaleClickable(
                        enabled = title.isNotBlank() && !isSubmitting,
                        onClick = {
                            if (title.isNotBlank()) {
                                onSubmit(title.trim(), selectedType, note.trim().ifBlank { null })
                            }
                        }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MARed,
                    contentColor = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Request",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun redFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MARed,
    focusedLabelColor = MARed,
    cursorColor = MARed,
    unfocusedBorderColor = MABorderSubtle,
    unfocusedLabelColor = MATextSecondary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = MATextTertiary,
    unfocusedPlaceholderColor = MATextTertiary
)
