package com.mna.streaming.ui.admin.appversions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.network.models.CreateAppVersionRequest
import com.mna.streaming.network.models.UpdateAppVersionRequest
import com.mna.streaming.ui.admin.movies.AdminTextField
import com.mna.streaming.ui.admin.movies.SectionLabel
import com.mna.streaming.ui.admin.movies.StatusSelector
import com.mna.streaming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppVersionFormScreen(
    viewModel:     AdminAppVersionsViewModel,
    editVersionId: String?,
    onSaved:       () -> Unit,
    onBackClick:   () -> Unit
) {
    val state  by viewModel.state.collectAsState()
    val isEdit = editVersionId != null

    LaunchedEffect(editVersionId) {
        if (editVersionId != null) viewModel.loadEditVersion(editVersionId)
        else viewModel.clearEditVersion()
    }

    var versionName       by remember { mutableStateOf("") }
    var versionCode       by remember { mutableStateOf("") }
    var platform          by remember { mutableStateOf("android") }
    var channel           by remember { mutableStateOf("stable") }
    var downloadUrl       by remember { mutableStateOf("") }
    var releaseNotes      by remember { mutableStateOf("") }
    var forceUpdate       by remember { mutableStateOf(false) }
    var minSupported      by remember { mutableStateOf("") }
    var rolloutPercent    by remember { mutableStateOf("100") }
    var isActive          by remember { mutableStateOf(true) }
    var adminNotes        by remember { mutableStateOf("") }

    var formInited by remember { mutableStateOf(false) }

    LaunchedEffect(state.editingVersion) {
        val v = state.editingVersion ?: return@LaunchedEffect
        if (formInited) return@LaunchedEffect
        versionName    = v.versionName
        versionCode    = v.versionCode.toString()
        platform       = v.platform
        channel        = v.channel
        downloadUrl    = v.downloadUrl
        releaseNotes   = v.releaseNotes ?: ""
        forceUpdate    = v.forceUpdate
        minSupported   = v.minSupportedVersionCode?.toString() ?: ""
        rolloutPercent = v.rolloutPercentage?.toString() ?: "100"
        isActive       = v.isActive
        adminNotes     = v.adminNotes ?: ""
        formInited     = true
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
                    text       = if (isEdit) "Edit Version" else "New Version",
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

            // ── Platform & Channel ────────────────────────────────────────────
            SectionLabel("Platform")
            StatusSelector(platform, listOf("android", "ios", "all")) { platform = it }

            SectionLabel("Channel")
            StatusSelector(channel, listOf("stable", "beta")) { channel = it }

            // ── Version info ──────────────────────────────────────────────────
            SectionLabel("Version Details")
            AdminTextField("Version Name *  (e.g. 2.4.1)", versionName, { versionName = it })
            AdminTextField(
                label    = "Version Code *  (integer, e.g. 241)",
                value    = versionCode,
                onValueChange = { versionCode = it },
                keyboardType = KeyboardType.Number
            )
            AdminTextField("Download URL *", downloadUrl, { downloadUrl = it })

            // ── Release notes ─────────────────────────────────────────────────
            SectionLabel("Release Notes")
            AdminTextField(
                label    = "What's new…",
                value    = releaseNotes,
                onValueChange = { releaseNotes = it },
                minLines = 3
            )

            // ── Force update ──────────────────────────────────────────────────
            SectionLabel("Update Policy")
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Force Update", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        "Users must update before using the app",
                        color    = MATextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked         = forceUpdate,
                    onCheckedChange = { forceUpdate = it },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor       = Color.White,
                        checkedTrackColor       = MARed,
                        uncheckedThumbColor     = MATextSecondary,
                        uncheckedTrackColor     = MACard
                    )
                )
            }

            AdminTextField(
                label    = "Min Supported Version Code  (users below this are force-updated)",
                value    = minSupported,
                onValueChange = { minSupported = it },
                keyboardType = KeyboardType.Number
            )

            // ── Rollout ───────────────────────────────────────────────────────
            SectionLabel("Rollout")
            AdminTextField(
                label    = "Rollout Percentage  (1–100, default 100)",
                value    = rolloutPercent,
                onValueChange = { rolloutPercent = it },
                keyboardType = KeyboardType.Number
            )

            // ── Visibility ────────────────────────────────────────────────────
            SectionLabel("Visibility")
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Active", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        "Inactive versions are ignored by the update checker",
                        color    = MATextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked         = isActive,
                    onCheckedChange = { isActive = it },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = Color(0xFF0EA5E9),
                        uncheckedThumbColor = MATextSecondary,
                        uncheckedTrackColor = MACard
                    )
                )
            }

            // ── Admin notes ───────────────────────────────────────────────────
            SectionLabel("Admin Notes (internal)")
            AdminTextField(
                label    = "Notes visible only in the admin panel…",
                value    = adminNotes,
                onValueChange = { adminNotes = it },
                minLines = 2
            )

            state.saveError?.let {
                Text(it, color = MARed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            val canSubmit = versionName.isNotBlank() &&
                            versionCode.toIntOrNull() != null &&
                            downloadUrl.isNotBlank() &&
                            !state.isSaving

            Button(
                onClick = {
                    val code        = versionCode.toIntOrNull() ?: return@Button
                    val rollout     = rolloutPercent.toIntOrNull() ?: 100
                    val minCode     = minSupported.toIntOrNull()
                    val notes       = releaseNotes.trim().takeIf { it.isNotEmpty() }
                    val adminNote   = adminNotes.trim().takeIf { it.isNotEmpty() }

                    if (isEdit && editVersionId != null) {
                        viewModel.updateVersion(editVersionId, UpdateAppVersionRequest(
                            versionName             = versionName.trim(),
                            platform                = platform,
                            channel                 = channel,
                            downloadUrl             = downloadUrl.trim(),
                            releaseNotes            = notes,
                            forceUpdate             = forceUpdate,
                            minSupportedVersionCode = minCode,
                            rolloutPercentage       = rollout,
                            isActive                = isActive,
                            adminNotes              = adminNote
                        ), onSaved)
                    } else {
                        viewModel.createVersion(CreateAppVersionRequest(
                            versionName             = versionName.trim(),
                            versionCode             = code,
                            platform                = platform,
                            channel                 = channel,
                            downloadUrl             = downloadUrl.trim(),
                            releaseNotes            = notes,
                            forceUpdate             = forceUpdate,
                            minSupportedVersionCode = minCode,
                            rolloutPercentage       = rollout,
                            isActive                = isActive,
                            adminNotes              = adminNote
                        ), onSaved)
                    }
                },
                enabled  = canSubmit,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF0EA5E9),
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFF0EA5E9).copy(alpha = 0.4f),
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
                    Text(
                        if (isEdit) "Save Changes" else "Create Version",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
