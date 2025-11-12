package com.st.ui.composables

import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.st.ui.theme.ErrorText
import com.st.ui.theme.Grey0
import com.st.ui.theme.Grey10
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PreviewBlueMSTheme
import com.st.ui.theme.PrimaryBlue
import com.st.ui.utils.keyboardVisibility
import java.util.regex.Pattern

fun String.isValidEmail(): Boolean {
    return this.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidWebUrl(): Boolean {
    return this.isNotBlank() && Patterns.WEB_URL.matcher(this).matches()
}

fun String.isValidPhone(): Boolean {
    return this.isNotBlank() && Patterns.PHONE.matcher(this).matches()
}

// Passwords must have at least six digits and include
// one digit, one lower case letter and one upper case letter.
private const val MIN_PASS_LENGTH = 6
private const val PASS_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{4,}$"

fun String.isValidPassword(minLength: Int = MIN_PASS_LENGTH,
                           passwordPattern: String = PASS_PATTERN): Boolean {
    return this.isNotBlank() &&
            this.length >= minLength &&
            Pattern.compile(passwordPattern).matcher(this).matches()
}

@Composable
fun BlueMSOutlineTextField(
    modifier: Modifier = Modifier,
    value: String,
    placeHolder: String,
    supportText: String,
    enabled: Boolean = true,
    onCheckError: (String) -> Boolean,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    isPassword: Boolean = false,
    focusedTextColor: Color = Grey10,
    focusedContainerColor: Color = Grey0,
    unfocusedTextColor: Color = Grey10,
    unfocusedContainerColor: Color = Grey0,
    onDone: () -> Unit = {}
) {

    var newValue by remember(key1 = value) { mutableStateOf(value) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showPassword by remember { mutableStateOf(value = false) }

    val keyboardVisibility by keyboardVisibility()

    LaunchedEffect(keyboardVisibility) {
        if (!keyboardVisibility) {
            onDone()
        }
    }

    val visualTransformation = if (isPassword) {
        if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    } else {
        VisualTransformation.None
    }

    OutlinedTextField(
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
        value = newValue,
        textStyle = MaterialTheme.typography.bodyMedium,
        onValueChange = {
            newValue = it
            onValueChange(it)
        },
        colors = OutlinedTextFieldDefaults.colors().copy(
            focusedTextColor = focusedTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedTextColor = unfocusedTextColor,
            unfocusedContainerColor = unfocusedContainerColor
        ),
        placeholder = { Text(text = placeHolder, style = MaterialTheme.typography.bodySmall) },
        keyboardOptions = keyboardOptions.copy(
            imeAction = ImeAction.Done

        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                onDone()
            }
        ),
        supportingText = {
            if (onCheckError(newValue)) {
                Text(
                    text = supportText,
                    color = ErrorText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        visualTransformation = visualTransformation,
        trailingIcon = {
            if (isPassword) {
                if (showPassword) {
                    IconButton(onClick = { showPassword = false }) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "hide_password"
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showPassword = true }) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "hide_password"
                        )
                    }
                }
            } else {
                if (onCheckError(newValue)) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = ErrorText
                    )
                }
            }
        }
    )
}

/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun BlueMSOutlineTextFieldPasswordPreview() {
    PreviewBlueMSTheme {
        BlueMSOutlineTextField(
            modifier = Modifier.padding(LocalDimensions.current.paddingNormal),
            value = "YourNewPassword",
            isPassword = true,
            placeHolder = "Password",
            supportText = "At least 6 chars, 1 digit, 1 lower and 1 upper case letter",
            onCheckError = { !it.isValidPassword() && it.isNotEmpty() },
            onValueChange = { /* updatePassword(it)*/ },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSOutlineTextFieldMailPreview() {
    PreviewBlueMSTheme {
        BlueMSOutlineTextField(
            modifier = Modifier.padding(LocalDimensions.current.paddingNormal),
            value = "luca@st.com",
            placeHolder = "Email",
            focusedTextColor = Grey0,
            focusedContainerColor = PrimaryBlue,
            unfocusedTextColor = Grey0,
            unfocusedContainerColor = PrimaryBlue,
            supportText = "Not a valid email",
            onCheckError = { !it.isValidEmail() && it.isNotEmpty() },
            onValueChange = { /* updateEmail(it) */ },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSOutlineTextFieldNotValidMailPreview() {
    PreviewBlueMSTheme {
        BlueMSOutlineTextField(
            modifier = Modifier.padding(LocalDimensions.current.paddingNormal),
            value = "lucast.com",
            placeHolder = "Email",
            supportText = "Not a valid email",
            onCheckError = { !it.isValidEmail() && it.isNotEmpty() },
            onValueChange = { /* updateEmail(it) */ },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSOutlineTextFieldNamePreview() {
    PreviewBlueMSTheme {
        BlueMSOutlineTextField(
            modifier = Modifier.fillMaxWidth(),
            value = "",
            placeHolder = "Name Place Holder",
            supportText = "",
            onCheckError = { false },
            onValueChange = {
                //newName = it
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSOutlineTextFieldPositiveNumberPreview() {
    PreviewBlueMSTheme {
        var displayValueHeight by remember { mutableStateOf("180") }
        BlueMSOutlineTextField(
            value = displayValueHeight,
            placeHolder = "Height",
            supportText = "Height cannot be negative or null",
            onCheckError = {
                if (displayValueHeight.toIntOrNull() == null) {
                    true
                } else {
                    displayValueHeight.toInt() <= 0
                }
            },
            onValueChange = {
                displayValueHeight = it
                it.toIntOrNull()?.let { num ->
                    if (num > 0) {
                       /* Save new Height */
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onDone = {}
        )
    }
}


