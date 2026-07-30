package com.mna.streaming.ui.admin.appversions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.network.models.AdminAppVersion
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppVersionsScreen(
    viewModel:    AdminAppVersionsViewModel,
    onCreateClick: () -> Unit,
    onEditClick:  (String) -> Unit,
    onBackClick:  () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Local delete-confirm dialog state
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

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
                    "App Updates",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = onCreateClick) {
                    Icon(Icons.Default.Add, "New version", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Filter chips ──────────────────────────────────────────────────
            val platforms = listOf(null to "All", "android" to "Android", "ios" to "iOS", "all" to "Universal")
            val channels  = listOf(null to "Any channel", "stable" to "Stable", "beta" to "Beta")

            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(platforms) { (value, label) ->
                    FilterChip(
                        selected = state.filterPlatform == value,
                        onClick  = { viewModel.setFilter(value, state.filterChannel) },
                        label    = { Text(label, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = Color(0xFF0EA5E9),
                            selectedLabelColor        = Color.White,
                            containerColor            = MACard,
                            labelColor                = MATextSecondary
                        )
                    )
                }
                item { Spacer(Modifier.width(8.dp)) }
                items(channels) { (value, label) ->
                    FilterChip(
                        selected = state.filterChannel == value,
                        onClick  = { viewModel.setFilter(state.filterPlatform, value) },
                        label    = { Text(label, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = Color(0xFF10B981),
                            selectedLabelColor        = Color.White,
                            containerColor            = MACard,
                            labelColor                = MATextSecondary
                        )
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MARed)
                    }
                }
                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error ?: "Error", color = MARed, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadVersions() },
                                colors  = ButtonDefaults.buttonColors(containerColor = MARed)
                            ) { Text("Retry") }
                        }
                    }
                }
                state.versions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                null,
                                tint     = MATextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No versions yet", color = MATextSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onCreateClick,
                                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                            ) { Text("Add First Version") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding        = PaddingValues(16.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.versions, key = { it.id }) { version ->
                            AppVersionCard(
                                version    = version,
                                isDeleting = state.deletingId == version.id,
                                onEdit     = { onEditClick(version.id) },
                                onDelete   = { confirmDeleteId = version.id }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (confirmDeleteId != null) {
        val target = state.versions.find { it.id == confirmDeleteId }
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            containerColor   = MACard,
            title   = { Text("Delete Version?", color = Color.White) },
            text    = {
                Text(
                    "Remove ${target?.versionName ?: "this version"} (${target?.platform}/${target?.channel})?\nThis cannot be undone.",
                    color = MATextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteId?.let { viewModel.deleteVersion(it) }
                    confirmDeleteId = null
                }) {
                    Text("Delete", color = MARed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) {
                    Text("Cancel", color = MATextSecondary)
                }
            }
        )
    }
}

// ── App version card ──────────────────────────────────────────────────────────

@Composable
private fun AppVersionCard(
    version:    AdminAppVersion,
    isDeleting: Boolean,
    onEdit:     () -> Unit,
    onDelete:   () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MACard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = version.versionName,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                        Text(
                            text      = "(${version.versionCode})",
                            color     = MATextSecondary,
                            fontSize  = 12.sp
                        )
                        if (version.forceUpdate) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MARed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "FORCE",
                                    color    = MARed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlatformBadge(version.platform)
                        ChannelBadge(version.channel)
                        if (!version.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MATextSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "INACTIVE",
                                    color    = MATextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = MATextSecondary, modifier = Modifier.size(18.dp))
                    }
                    if (isDeleting) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = MARed,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Delete", tint = MARed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (!version.releaseNotes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = version.releaseNotes,
                    color    = MATextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (version.rolloutPercentage != null && version.rolloutPercentage < 100) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Tune, null, tint = MATextSecondary, modifier = Modifier.size(12.dp))
                    Text(
                        text  = "Rollout: ${version.rolloutPercentage}%",
                        color = MATextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformBadge(platform: String) {
    val color = when (platform) {
        "android" -> Color(0xFF10B981)
        "ios"     -> Color(0xFF3B82F6)
        else      -> Color(0xFF8B5CF6)
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            platform.uppercase(),
            color    = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ChannelBadge(channel: String) {
    val color = if (channel == "stable") Color(0xFF0EA5E9) else Color(0xFFF59E0B)
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            channel.uppercase(),
            color    = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
