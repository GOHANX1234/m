package com.mna.streaming.ui.admin.requests

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mna.streaming.network.models.AdminContentRequest
import com.mna.streaming.ui.admin.movies.CenteredLoader
import com.mna.streaming.ui.admin.movies.ErrorRetry
import com.mna.streaming.ui.admin.movies.adminTextFieldColors
import com.mna.streaming.ui.theme.*

private fun statusColor(status: String): Color = when (status) {
    "pending"     -> Color(0xFFF59E0B)
    "in_progress" -> Color(0xFF3B82F6)
    "fulfilled"   -> Color(0xFF10B981)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestsScreen(
    viewModel:   AdminRequestsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var reviewTarget by remember { mutableStateOf<AdminContentRequest?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminContentRequest?>(null) }

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
                    text       = "Content Requests (${state.requests.size})",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.load() }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = MATextSecondary)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorRetry(state.error!!) { viewModel.load() }
                state.requests.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No content requests.", color = MATextSecondary)
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.requests, key = { it.id }) { req ->
                        RequestCard(
                            request    = req,
                            isUpdating = state.updatingId == req.id,
                            isDeleting = state.deletingId == req.id,
                            onReview   = { reviewTarget = req },
                            onDelete   = { deleteTarget = req }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    // Review / status update dialog
    reviewTarget?.let { req ->
        var selectedStatus by remember(req.id) { mutableStateOf(req.status) }
        var adminNote      by remember(req.id) { mutableStateOf(req.adminNote ?: "") }
        Dialog(onDismissRequest = { reviewTarget = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MACard, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Update Request", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "\"${req.title}\" by ${req.user?.nickname ?: "Unknown"}",
                        color    = MATextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Status picker
                    Text("Status", color = MATextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    val statuses = listOf("pending", "in_progress", "fulfilled", "rejected")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        statuses.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { s ->
                                    val sel = selectedStatus == s
                                    FilterChip(
                                        selected = sel,
                                        onClick  = { selectedStatus = s },
                                        label    = { Text(statusLabel(s), fontSize = 12.sp) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = statusColor(s).copy(alpha = 0.18f),
                                            selectedLabelColor     = statusColor(s),
                                            containerColor         = MASurface,
                                            labelColor             = MATextSecondary
                                        ),
                                        border   = FilterChipDefaults.filterChipBorder(
                                            enabled = true, selected = sel,
                                            selectedBorderColor = statusColor(s).copy(alpha = 0.5f),
                                            borderColor = Color.White.copy(alpha = 0.10f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    OutlinedTextField(
                        value         = adminNote,
                        onValueChange = { adminNote = it },
                        label         = { Text("Admin note (optional)") },
                        colors        = adminTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        minLines      = 2,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { reviewTarget = null },
                            colors  = ButtonDefaults.textButtonColors(contentColor = MATextSecondary)
                        ) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateStatus(req.id, selectedStatus, adminNote.takeIf { it.isNotBlank() })
                                reviewTarget = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MARed, contentColor = Color.White),
                            shape  = RoundedCornerShape(10.dp)
                        ) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    deleteTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = MACard,
            title = { Text("Delete Request", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete this request by ${req.user?.nickname ?: "Unknown"}?", color = MATextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(req.id); deleteTarget = null },
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
private fun RequestCard(
    request:    AdminContentRequest,
    isUpdating: Boolean,
    isDeleting: Boolean,
    onReview:   () -> Unit,
    onDelete:   () -> Unit
) {
    val statusColor = statusColor(request.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MACard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = request.title,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Type badge
                        Surface(shape = RoundedCornerShape(4.dp), color = MATextSecondary.copy(alpha = 0.10f)) {
                            Text(
                                text          = request.type.replaceFirstChar { it.uppercase() },
                                color         = MATextSecondary,
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // Status badge
                        Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.12f)) {
                            Text(
                                text          = statusLabel(request.status),
                                color         = statusColor,
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (isUpdating || isDeleting) {
                    CircularProgressIndicator(color = MARed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onReview, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Review", tint = MATextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MARed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // User info
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(Modifier.height(8.dp))

            request.user?.let { user ->
                Text("By @${user.nickname}", color = MATextSecondary, fontSize = 12.sp)
            }

            request.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(4.dp))
                Text("\"$note\"", color = MATextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            request.adminNote?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF3B82F6).copy(alpha = 0.10f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(note, color = Color(0xFF3B82F6), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
