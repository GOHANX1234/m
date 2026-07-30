package com.mna.streaming.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.R
import com.mna.streaming.ui.theme.MADark
import kotlinx.coroutines.delay

/**
 * Short branded handoff shown after Android's system splash.
 *
 * The system splash is intentionally kept for fast cold-start behavior; this
 * Compose layer provides the custom logo-to-wordmark motion that a system
 * splash layout cannot express.
 */
@Composable
fun LaunchSplash(onFinished: () -> Unit) {
    var startMotion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(90)
        startMotion = true
        delay(1_150)
        onFinished()
    }

    val logoOffset by animateDpAsState(
        targetValue = if (startMotion) (-52).dp else 0.dp,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "splashLogoOffset"
    )
    val nameOffset by animateDpAsState(
        targetValue = if (startMotion) 48.dp else 118.dp,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "splashNameOffset"
    )
    val nameAlpha by animateFloatAsState(
        targetValue = if (startMotion) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = LinearOutSlowInEasing),
        label = "splashNameAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_splash_logo),
            contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .offset(x = logoOffset)
        )

        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .offset(x = nameOffset)
                .alpha(nameAlpha)
                .wrapContentSize()
        )
    }
}
