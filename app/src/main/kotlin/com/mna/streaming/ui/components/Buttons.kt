package com.mna.streaming.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.ui.theme.MABorderStrong
import com.mna.streaming.ui.theme.MARadius
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.pressScaleClickable

/**
 * The app's single filled call-to-action style (Play, Submit, Update Now,
 * etc.). Centralising it means every primary button across the app shares
 * the same height, corner radius and loading-state behaviour.
 */
@Composable
fun MAPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    containerColor: Color = MARed
) {
    GlassSurface(
        modifier = modifier
            .height(height)
            .pressScaleClickable(
                pressedScale = 0.94f,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        shape = RoundedCornerShape(MARadius.md),
        level = GlassLevel.Elevated,
        tint = if (enabled) containerColor else containerColor.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

/** Outlined counterpart to [MAPrimaryButton] — secondary actions like Trailer/Sign Out. */
@Composable
fun MASecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = Color.White,
    height: Dp = 52.dp
) {
    GlassSurface(
        modifier = modifier
            .height(height)
            .pressScaleClickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(MARadius.md),
        level = GlassLevel.Regular
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
