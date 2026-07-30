package com.mna.streaming.ui.admin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.network.models.AdminUser
import com.mna.streaming.ui.admin.movies.CenteredLoader
import com.mna.streaming.ui.admin.movies.ErrorRetry
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel:   AdminUsersViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var roleTarget by remember { mutableStateOf<Pair<AdminUser, String>?>(null) }
    var localSearch by remember { mutableStateOf("") }

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
                        text       = "Users (${state.total})",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                }
                // Search
                OutlinedTextField(
                    value         = localSearch,
                    onValueChange = { localSearch = it; viewModel.setSearch(it) },
                    placeholder   = { Text("Search by name or email…", color = MATextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = MATextSecondary) },
                    trailingIcon  = if (localSearch.isNotEmpty()) {{
                        IconButton(onClick = { localSearch = ""; viewModel.setSearch("") }) {
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
                    modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                )
                // Role filter chips
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to "All", "user" to "Users", "admin" to "Admins").forEach { (role, label) ->
                        val sel = state.roleFilter == role
                        FilterChip(
                            selected = sel,
                            onClick  = { viewModel.setRoleFilter(role) },
                            label    = { Text(label, fontSize = 12.sp) },
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
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorRetry(state.error!!) { viewModel.load() }
                state.users.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found.", color = MATextSecondary)
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(state.users, key = { it.id }) { user ->
                        UserRow(
                            user       = user,
                            isUpdating = state.updatingId == user.id,
                            onRoleToggle = {
                                val newRole = if (user.role == "admin") "user" else "admin"
                                roleTarget = user to newRole
                            }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    roleTarget?.let { (user, newRole) ->
        AlertDialog(
            onDismissRequest = { roleTarget = null },
            containerColor   = MACard,
            title = {
                Text(
                    "Change Role",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Set \"${user.nickname}\" to ${newRole}?",
                    color = MATextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.updateRole(user.id, newRole); roleTarget = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MARed)
                ) { Text("Confirm", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { roleTarget = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MATextSecondary)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun UserRow(
    user:        AdminUser,
    isUpdating:  Boolean,
    onRoleToggle: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MADark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier         = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF374151), Color(0xFF1F2937)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = user.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.nickname, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(user.email, color = MATextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            user.lockedUntil?.let {
                Spacer(Modifier.height(2.dp))
                Text("🔒 Locked", color = MARed, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        // Role badge + toggle button
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (user.role == "admin") MARed.copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.12f)
            ) {
                Text(
                    text          = user.role.uppercase(),
                    color         = if (user.role == "admin") MARed else Color(0xFF3B82F6),
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier      = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            if (isUpdating) {
                CircularProgressIndicator(color = MARed, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = onRoleToggle,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MATextSecondary),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text     = if (user.role == "admin") "Demote" else "Promote",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
}
