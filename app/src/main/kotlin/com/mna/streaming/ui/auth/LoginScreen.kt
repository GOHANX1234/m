package com.mna.streaming.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.ui.components.MAPrimaryButton
import com.mna.streaming.ui.theme.MABorderSubtle
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MAMotion
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MARedLight
import com.mna.streaming.ui.theme.MATextSecondary

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onClearErrors: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    // Clear errors when user starts editing
    LaunchedEffect(email, password) { onClearErrors() }

    // Gentle settle-in on first composition — fade + rise, no new dependency.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(MAMotion.slow, easing = MAMotion.enterEasing),
        label = "loginContentAlpha"
    )
    val contentOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 18f,
        animationSpec = tween(MAMotion.slow, easing = MAMotion.standardEasing),
        label = "loginContentOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        // Cinematic glow behind the wordmark for depth.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MARed.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(0.5f, 0f),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentOffset
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            Text(
                text = "M&A",
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(listOf(MARedLight, MARed))
                ),
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "STREAMING",
                color = MATextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 6.sp
            )

            Spacer(Modifier.height(56.dp))

            Text(
                text = "Sign In",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Welcome back. Enter your credentials to continue.",
                color = MATextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Email field
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = { Icon(Icons.Default.Email, null, tint = MATextSecondary) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = uiState.loginFieldErrors.containsKey("email"),
                errorMessage = uiState.loginFieldErrors["email"]
            )

            Spacer(Modifier.height(14.dp))

            // Password field
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = MATextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPass) "Hide password" else "Show password",
                            tint = MATextSecondary
                        )
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (email.isNotBlank() && password.isNotBlank()) {
                            onLogin(email, password)
                        }
                    }
                ),
                isError = uiState.loginFieldErrors.containsKey("password"),
                errorMessage = uiState.loginFieldErrors["password"]
            )

            // General error (wrong credentials, locked account, etc.)
            if (uiState.loginError != null && uiState.loginFieldErrors.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.loginError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))

            // Sign In button
            MAPrimaryButton(
                text = "Sign In",
                onClick = {
                    focusManager.clearFocus()
                    onLogin(email, password)
                },
                enabled = email.isNotBlank() && password.isNotBlank(),
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MABorderSubtle)
                Text(
                    text = "  OR  ",
                    color = MATextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MABorderSubtle)
            }

            Spacer(Modifier.height(28.dp))

            // Sign Up link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = MATextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Sign Up",
                    color = MARed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                        onNavigateToSignup()
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
