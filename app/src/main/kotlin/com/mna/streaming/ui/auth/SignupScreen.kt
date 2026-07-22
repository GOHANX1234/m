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
import androidx.compose.material.icons.filled.Person
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
fun SignupScreen(
    uiState: AuthUiState,
    onSignUp: (nickname: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onClearErrors: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var nickname by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    // Navigate back to login on successful signup
    LaunchedEffect(uiState.signupSuccess) {
        if (uiState.signupSuccess) {
            onNavigateToLogin()
        }
    }

    // Clear errors when user starts editing
    LaunchedEffect(nickname, email, password) { onClearErrors() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MARed.copy(alpha = 0.10f), Color.Transparent)
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
            Spacer(Modifier.height(56.dp))

            // Logo
            Text(
                text = "M&A",
                color = MARed,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "STREAMING",
                color = MATextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 6.sp
            )

            Spacer(Modifier.height(44.dp))

            Text(
                text = "Create Account",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Join M&A Streaming — it's free.",
                color = MATextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            // Nickname field
            AuthTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = "Nickname",
                leadingIcon = { Icon(Icons.Default.Person, null, tint = MATextSecondary) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = uiState.signupFieldErrors.containsKey("nickname"),
                errorMessage = uiState.signupFieldErrors["nickname"],
                supportingText = "3–20 characters: letters, numbers, underscores"
            )

            Spacer(Modifier.height(12.dp))

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
                isError = uiState.signupFieldErrors.containsKey("email"),
                errorMessage = uiState.signupFieldErrors["email"]
            )

            Spacer(Modifier.height(12.dp))

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
                            contentDescription = if (showPass) "Hide" else "Show",
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
                        if (nickname.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                            onSignUp(nickname, email, password)
                        }
                    }
                ),
                isError = uiState.signupFieldErrors.containsKey("password"),
                errorMessage = uiState.signupFieldErrors["password"],
                supportingText = "Minimum 8 characters"
            )

            // General error
            if (uiState.signupError != null && uiState.signupFieldErrors.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = uiState.signupError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // Sign Up button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSignUp(nickname, email, password)
                },
                enabled = !uiState.isLoading &&
                    nickname.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
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
                        text = "Create Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Sign In link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = MATextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Sign In",
                    color = MARed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                        onNavigateToLogin()
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
