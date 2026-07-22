package com.mna.streaming.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MARed
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        // Subtle top gradient for depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MARed.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            Text(
                text = "M&A",
                color = MARed,
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
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLogin(email, password)
                },
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MARed,
                    contentColor   = Color.White,
                    disabledContainerColor = MARed.copy(alpha = 0.4f),
                    disabledContentColor   = Color.White.copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MACard)
                Text(
                    text = "  OR  ",
                    color = MATextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MACard)
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
