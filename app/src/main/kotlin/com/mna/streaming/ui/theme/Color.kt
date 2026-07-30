package com.mna.streaming.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * M&A design tokens — single source of truth for color across the app.
 *
 * Enterprise-grade dark cinema theme: layered near-black surfaces with a
 * Netflix-red primary and a warm gold accent reserved for ratings and
 * premium moments. Prefer these tokens over inline `Color(0xFF...)`
 * literals so new and existing screens stay visually consistent.
 */

// ── Brand ────────────────────────────────────────────────────────────────
val MARed = Color(0xFFE50914)
val MARedDark = Color(0xFFB20710)   // pressed state / darker gradient stop
val MARedLight = Color(0xFFFF4D57)  // glow / highlighted gradient stop
val MAGold = Color(0xFFFFD700)
val MAGoldDark = Color(0xFFC9A600)

// ── Surfaces — elevation scale, darkest to lightest ─────────────────────────
val MADark = Color(0xFF0A0A0F)          // app background (level 0)
val MASurface = Color(0xFF141414)       // sunken sections, chip rails (level 1)
val MACard = Color(0xFF1E1E24)          // cards, rows, list items (level 2)
val MACardElevated = Color(0xFF262631)  // sheets, dialogs, popovers, menus (level 3)

// ── Text ─────────────────────────────────────────────────────────────────
val MATextPrimary = Color(0xFFFFFFFF)
val MATextSecondary = Color(0xFFB3B3B3)
val MATextTertiary = Color(0xFF7C7C88)

// ── Borders / hairlines ──────────────────────────────────────────────────
val MABorderSubtle = Color(0x1FFFFFFF)  // ~12% white — default hairline
val MABorderStrong = Color(0x3DFFFFFF)  // ~24% white — focused / emphasised

// ── Semantic ─────────────────────────────────────────────────────────────
val MASuccess = Color(0xFF10B981)
val MAWarning = Color(0xFFF59E0B)
val MAInfo = Color(0xFF3B82F6)
val MACyan = Color(0xFF0EA5E9)
val MAPurple = Color(0xFF8B5CF6)

// ── Content-type accents (badges across Home / Search / Profile / Admin) ───
val MAAccentMovie = MAPurple
val MAAccentAnime = MARed
val MAAccentSeries = MACyan
