package com.mna.streaming.ui.security

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen blocking screen shown when the APK integrity / signature check
 * fails.  The user cannot navigate past this screen.
 *
 * Back-press is silently consumed — the user must either open the official
 * Telegram channel or force-quit the app.
 *
 * Design goals:
 *  - Unambiguous, professional error communication.
 *  - Single clear call-to-action: open https://t.me/ClerXin.
 *  - No way to dismiss or bypass within the app.
 */
@Composable
fun TamperDetectedScreen() {
    val context = LocalContext.current

    // Swallow all back-press events — user cannot navigate away.
    BackHandler(enabled = true) { /* intentionally empty */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Icon ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF200808)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.GppBad,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Title ─────────────────────────────────────────────────────
            Text(
                text = "Integrity Check Failed",
                color = Color(0xFFFFFFFF),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Modified or re-signed build detected",
                color = Color(0xFFE57373),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = Color(0xFF2A2A2A),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Body ──────────────────────────────────────────────────────
            Text(
                text = "This copy of M&A has been tampered with or signed with an unauthorized key.",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To protect the integrity of the service and its users, modified builds are permanently blocked from running. Please obtain the official release from our Telegram channel.",
                color = Color(0xFF777777),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Primary CTA ───────────────────────────────────────────────
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ClerXin"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Get Official App  →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "t.me/ClerXin",
                color = Color(0xFF444444),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
