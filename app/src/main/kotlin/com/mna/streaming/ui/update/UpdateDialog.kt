package com.mna.streaming.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mna.streaming.network.models.VersionCheckResponse

/* ── Shared helpers ──────────────────────────────────────────────────────── */

/** Opens the download / Play Store URL in the system browser or Play Store app. */
@Composable
private fun rememberUrlOpener(url: String?): () -> Unit {
    val context = LocalContext.current
    return {
        if (!url.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}

/** Whether a download URL is present and usable. */
private fun String?.isUsable(): Boolean = !isNullOrBlank()

/* ── Force-update dialog ─────────────────────────────────────────────────── */

/**
 * Full-screen, non-dismissible dialog shown when:
 *  • [UpdateState.ForceUpdate.reason] == "tooOld"  → version below minSupportedVersionCode
 *  • [UpdateState.ForceUpdate.reason] == "forced"  → admin set forceUpdate = true
 *
 * The user cannot back out of this screen; navigation is fully blocked until
 * they tap "Update Now" and install the new build.
 */
@Composable
fun ForceUpdateDialog(
    response: VersionCheckResponse,
    reason: String
) {
    val openUrl = rememberUrlOpener(response.downloadUrl)

    Dialog(
        onDismissRequest = { /* non-dismissible — intentionally empty */ },
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = if (reason == "tooOld") "Update Required" else "Mandatory Update",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (reason == "tooOld") {
                        "This version of M&A is no longer supported. " +
                        "Please update to continue watching."
                    } else {
                        buildString {
                            append("A required update is available")
                            response.latestVersionName?.let { append(" (v$it)") }
                            append(".")
                            if (!response.releaseNotes.isNullOrBlank()) {
                                append("\n\n")
                                append(response.releaseNotes)
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                val hasUrl = response.downloadUrl.isUsable()

                Button(
                    onClick = openUrl,
                    enabled = hasUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Update Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = if (hasUrl)
                        "You must update to continue using the app."
                    else
                        "Update link unavailable. Please contact support via our Telegram channel.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ── Optional-update dialog ──────────────────────────────────────────────── */

/**
 * Dismissible dialog shown when an update is available but not mandatory.
 * The user can tap "Later" to continue using the current version.
 */
@Composable
fun OptionalUpdateDialog(
    response: VersionCheckResponse,
    onDismiss: () -> Unit
) {
    val openUrl = rememberUrlOpener(response.downloadUrl)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = buildString {
                    append("Update Available")
                    response.latestVersionName?.let { append(" — v$it") }
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (!response.releaseNotes.isNullOrBlank())
                    response.releaseNotes
                else
                    "A new version of M&A is available. Update for the latest features and fixes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    openUrl()
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
