package com.potatameister.barenotes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onFolders: () -> Unit
) {
    val colors = LocalBareColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Backup.BackupData?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val data = viewModel.exportData()
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(data)
                        } ?: error("No stream")
                    }.isSuccess
                }
                messenger(
                    if (ok) "Markdown backup exported." else "Could not export backup."
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val data = withContext(Dispatchers.IO) {
                    readBackup(context, uri)
                }
                if (data == null) {
                    messenger("Could not read that backup.")
                } else {
                    pendingImport = data
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        BareTopBar(
            navigation = {
                BareIconButton(
                    iconRes = R.drawable.ic_back,
                    contentDescription = "Back",
                    onClick = onBack
                )
            },
            title = {
                Text("Settings", style = BareType.ScreenTitle, color = colors.text)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = "NOTES",
                style = BareType.Eyebrow,
                color = colors.textMuted
            )
            BareTextRow(
                label = "Editor text size",
                value = textSizeLabel(viewModel.textSizeSp),
                onClick = { dialog = "text_size" }
            )
            BareDivider()
            BareTextRow(
                label = "Sort notes",
                value = if (viewModel.sortOrder == SettingsPrefs.SORT_CREATED) {
                    "Date created"
                } else {
                    "Last modified"
                },
                onClick = { dialog = "sort" }
            )
            BareDivider()
            BareTextRow(
                label = "Theme",
                value = when (viewModel.themeMode) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                onClick = { dialog = "theme" }
            )
            BareDivider()
            BareTextRow(
                label = "Folders",
                value = "Manage",
                onClick = onFolders
            )
            BareDivider()

            Text(
                text = "DATA",
                style = BareType.Eyebrow,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 24.dp)
            )
            BareTextRow(
                label = "Export Markdown",
                value = null,
                onClick = {
                    exportLauncher.launch("bare-notes-markdown.zip")
                }
            )
            BareDivider()
            BareTextRow(
                label = "Import Markdown",
                value = null,
                onClick = {
                    importLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/json",
                            "text/plain",
                            "application/octet-stream"
                        )
                    )
                }
            )
            BareDivider()

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Settings stay on this device. Backups go wherever you save them.",
                style = BareType.Meta,
                color = colors.textMuted
            )
        }
    }

    when (dialog) {
        "text_size" -> ChoiceDialog(
            title = "Editor text size",
            options = listOf(
                "Small" to 14,
                "Default" to 16,
                "Large" to 18,
                "Extra large" to 20
            ),
            selected = viewModel.textSizeSp,
            onDismiss = { dialog = null },
            onSelect = {
                viewModel.setTextSize(it)
                dialog = null
            }
        )
        "sort" -> ChoiceDialog(
            title = "Sort notes",
            options = listOf(
                "Last modified" to SettingsPrefs.SORT_MODIFIED,
                "Date created" to SettingsPrefs.SORT_CREATED
            ),
            selected = viewModel.sortOrder,
            onDismiss = { dialog = null },
            onSelect = {
                viewModel.updateSortOrder(it)
                dialog = null
            }
        )
        "theme" -> ChoiceDialog(
            title = "Theme",
            options = listOf(
                "System" to ThemeMode.SYSTEM,
                "Light" to ThemeMode.LIGHT,
                "Dark" to ThemeMode.DARK
            ),
            selected = viewModel.themeMode,
            onDismiss = { dialog = null },
            onSelect = {
                viewModel.updateThemeMode(it)
                dialog = null
            }
        )
    }

    pendingImport?.let { data ->
        BareDialog(
            title = "Import backup?",
            onDismissRequest = { pendingImport = null },
            confirmText = "Replace",
            onConfirm = {
                viewModel.replaceAll(data)
                pendingImport = null
                messenger("Backup imported.")
            },
            content = {
                Text(
                    text = "This replaces ${viewModel.notes.size} notes and " +
                        "${viewModel.folders.size} folders with ${data.notes.size} notes and " +
                        "${data.folders.size} folders.",
                    style = BareType.Body,
                    color = LocalBareColors.current.textMuted
                )
            }
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    BareDialog(
        title = title,
        onDismissRequest = onDismiss,
        content = {
            options.forEach { (label, value) ->
                ChoiceRow(
                    label = label,
                    selected = value == selected,
                    onClick = { onSelect(value) }
                )
            }
        }
    )
}

@Composable
private fun ChoiceRow(
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
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = BareType.Body,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.icon,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun textSizeLabel(sp: Int): String = when (sp) {
    14 -> "Small"
    18 -> "Large"
    20 -> "Extra large"
    else -> "Default"
}

private fun readBackup(context: android.content.Context, uri: Uri): Backup.BackupData? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            Backup.decode(stream.readBytes())
        }
    }.getOrNull()
