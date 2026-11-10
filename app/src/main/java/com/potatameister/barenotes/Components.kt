package com.potatameister.barenotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalMessenger = compositionLocalOf<(String) -> Unit> { {} }

@Composable
fun BareTopBar(
    title: @Composable () -> Unit,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigation?.invoke()
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            title()
        }
        actions()
    }
}

@Composable
fun BareIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = LocalBareColors.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = colors.icon,
            disabledContentColor = colors.textMuted
        )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) colors.icon else colors.textMuted
        )
    }
}

@Composable
fun BareDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalBareColors.current.divider)
    )
}

@Composable
fun BareNavRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalBareColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .background(if (selected) colors.divider.copy(alpha = 0.55f) else colors.surface)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = BareType.Body,
            color = colors.text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BareTextRow(
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalBareColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = BareType.Body,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = BareType.Preview,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BareFormatButton(
    label: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBareColors.current
    val style = when (label) {
        "B" -> BareType.Body.copy(fontWeight = FontWeight.Bold)
        "I" -> BareType.Body.copy(fontStyle = FontStyle.Italic)
        "U" -> BareType.Body.copy(textDecoration = TextDecoration.Underline)
        "S" -> BareType.Body.copy(textDecoration = TextDecoration.LineThrough)
        "H1", "H2" -> BareType.Body.copy(fontWeight = FontWeight.Bold)
        else -> BareType.Body
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .background(if (active) colors.divider.copy(alpha = 0.7f) else colors.bg)
            .selectable(
                selected = active,
                onClick = onClick,
                role = Role.Button
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = style,
            color = colors.text,
            fontSize = if (label == "H1" || label == "H2") 15.sp else 16.sp
        )
    }
}

@Composable
fun BareInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    style: TextStyle = BareType.Body,
    singleLine: Boolean = false,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val colors = LocalBareColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = style.copy(color = colors.text),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(colors.text),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        style = style,
                        color = colors.textMuted,
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE
                    )
                }
                inner()
            }
        }
    )
}

@Composable
fun BareNameDialog(
    title: String,
    confirmText: String,
    initial: String,
    hint: String,
    errorFor: (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    fun submit() {
        val clean = name.trim()
        val problem = errorFor(clean)
        if (problem != null) {
            error = problem
        } else {
            onSave(clean)
        }
    }

    BareDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmText = confirmText,
        onConfirm = ::submit,
        content = {
            BareInput(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                hint = hint,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    style = BareType.Meta,
                    color = LocalBareColors.current.textMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    )
}

@Composable
fun BareDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "Cancel",
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalBareColors.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = BareType.ScreenTitle,
                color = colors.text
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        },
        confirmButton = {
            if (confirmText != null && onConfirm != null) {
                TextButton(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.text,
                        disabledContentColor = colors.textMuted
                    )
                ) {
                    Text(confirmText, style = BareType.Body)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textMuted)
            ) {
                Text(dismissText, style = BareType.Body)
            }
        },
        shape = androidx.compose.foundation.shape.RectangleShape,
        containerColor = colors.bg,
        titleContentColor = colors.text,
        textContentColor = colors.textMuted,
        tonalElevation = 0.dp
    )
}
