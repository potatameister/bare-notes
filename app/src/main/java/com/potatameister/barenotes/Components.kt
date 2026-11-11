package com.potatameister.barenotes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

val LocalMessenger = compositionLocalOf<(String) -> Unit> { {} }

@Composable
fun BareLockup() {
    val colors = LocalBareColors.current
    Column {
        Text(
            text = "BARE",
            style = BareType.Wordmark,
            color = colors.text
        )
        Text(
            text = "NOTES",
            style = BareType.Eyebrow,
            color = colors.textMuted,
            modifier = Modifier.padding(start = 1.dp, top = 6.dp)
        )
    }
}

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
    onClick: () -> Unit,
    trailing: String? = null
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(if (selected) colors.text else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = BareType.Body,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = BareType.Meta,
                color = colors.textMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun BareTextAction(label: String, onClick: () -> Unit) {
    val colors = LocalBareColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BareType.Body,
            color = colors.text
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
fun BareFormatIconButton(
    iconRes: Int,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBareColors.current
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
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.text
        )
    }
}

@Composable
fun BareFormatDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(LocalBareColors.current.divider)
    )
}

@Composable
fun BareInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    style: TextStyle = BareType.Body,
    singleLine: Boolean = false,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = LocalBareColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = style.copy(color = colors.text),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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
    val colors = LocalBareColors.current

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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "NAME",
                    style = BareType.Eyebrow,
                    color = colors.textMuted
                )
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
                        .focusRequester(focus)
                        .padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                BareDivider()
                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        style = BareType.Meta,
                        color = colors.text
                    )
                }
            }
        }
    )
}

@Composable
private fun DialogAction(
    text: String,
    enabled: Boolean = true,
    muted: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalBareColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = BareType.Body,
            color = when {
                !enabled -> colors.textMuted
                muted -> colors.textMuted
                else -> colors.text
            }
        )
    }
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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .background(colors.bg)
                    .border(1.dp, colors.divider)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = BareType.ScreenTitle,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
                Spacer(modifier = Modifier.height(20.dp))
                BareDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogAction(
                        text = dismissText,
                        muted = true,
                        onClick = onDismissRequest
                    )
                    if (confirmText != null && onConfirm != null) {
                        DialogAction(
                            text = confirmText,
                            enabled = confirmEnabled,
                            onClick = onConfirm
                        )
                    }
                }
            }
        }
    }
}
