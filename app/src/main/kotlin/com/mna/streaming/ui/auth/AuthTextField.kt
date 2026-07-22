package com.mna.streaming.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import com.mna.streaming.ui.theme.MACard
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.MARed

/**
 * Shared styled text field used across Login and Signup screens.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = isError,
        enabled = enabled,
        singleLine = true,
        supportingText = when {
            isError && errorMessage != null ->
                { -> Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            supportingText != null ->
                { -> Text(supportingText, color = MATextSecondary) }
            else -> null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = MARed,
            unfocusedBorderColor  = MACard,
            errorBorderColor      = MaterialTheme.colorScheme.error,
            focusedLabelColor     = MARed,
            unfocusedLabelColor   = MATextSecondary,
            cursorColor           = MARed,
            focusedTextColor      = Color.White,
            unfocusedTextColor    = Color.White,
            focusedContainerColor   = MACard.copy(alpha = 0.6f),
            unfocusedContainerColor = MACard.copy(alpha = 0.4f),
            errorContainerColor     = MACard.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}
