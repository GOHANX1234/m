package com.mna.streaming.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.ui.components.GlassCapsule
import com.mna.streaming.ui.components.GlassLevel
import com.mna.streaming.ui.components.GlassSurface
import com.mna.streaming.ui.theme.*

// ── Dashboard tile model ──────────────────────────────────────────────────────

private data class DashTile(
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

// ── Dashboard screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onMoviesClick:     () -> Unit,
    onAnimeClick:      () -> Unit,
    onGenresClick:     () -> Unit,
    onUsersClick:      () -> Unit,
    onRequestsClick:   () -> Unit,
    onAppUpdatesClick: () -> Unit,
    onBackClick:       () -> Unit
) {
    val tiles = listOf(
        DashTile("Movies",       Icons.Default.LocalMovies,    MARed,              onMoviesClick),
        DashTile("Anime / Series", Icons.Default.PlayCircle,   Color(0xFF8B5CF6),  onAnimeClick),
        DashTile("Genres",       Icons.Default.Category,       Color(0xFF10B981),  onGenresClick),
        DashTile("Users",        Icons.Default.People,         Color(0xFF3B82F6),  onUsersClick),
        DashTile("Requests",     Icons.Default.Inbox,          Color(0xFFF59E0B),  onRequestsClick),
        DashTile("App Updates",  Icons.Default.SystemUpdate,   Color(0xFF0EA5E9),  onAppUpdatesClick),
    )

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
                    text       = "Admin Panel",
                    color      = Color.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                GlassCapsule(tint = MARed) {
                    Text(
                        text          = "ADMIN",
                        color         = MARed,
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier      = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Hero gradient banner
            GlassSurface(
                modifier = Modifier.fillMaxWidth().height(104.dp),
                shape = RoundedCornerShape(MARadius.xl),
                level = GlassLevel.Elevated,
                tint = MARed,
                contentPadding = PaddingValues(horizontal = 22.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text       = "Content Manager",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text  = "Full control over M&A platform",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector        = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint               = Color.White.copy(alpha = 0.12f),
                    modifier           = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(64.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "Sections",
                color      = MATextSecondary,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns             = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(tiles) { tile ->
                    DashboardTile(tile)
                }
            }
        }
    }
}

@Composable
private fun DashboardTile(tile: DashTile) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .pressScaleClickable(onClick = tile.onClick),
        shape  = RoundedCornerShape(MARadius.lg),
        level = GlassLevel.Regular,
        tint = tile.accentColor.copy(alpha = 0.35f)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tile.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = tile.icon,
                    contentDescription = null,
                    tint               = tile.accentColor,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Text(
                text       = tile.label,
                color      = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
