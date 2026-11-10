package com.potatameister.barenotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FoldersScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val colors = LocalBareColors.current
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Folder?>(null) }
    var deleting by remember { mutableStateOf<Folder?>(null) }

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
                Text("Folders", style = BareType.ScreenTitle, color = colors.text)
            },
            actions = {
                BareIconButton(
                    iconRes = R.drawable.ic_plus,
                    contentDescription = "New folder",
                    onClick = { addOpen = true }
                )
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.folders, key = { it.id }) { folder ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clickable { editing = folder }
                                .padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = folder.name,
                                style = BareType.Body,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            BareIconButton(
                                iconRes = R.drawable.ic_delete,
                                contentDescription = "Delete folder",
                                onClick = { deleting = folder }
                            )
                        }
                        BareDivider()
                    }
                }
            }

            if (viewModel.folders.isEmpty()) {
                EmptyState(
                    title = "No folders yet",
                    hint = "Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (viewModel.folders.isNotEmpty()) {
            Text(
                text = "Tap a folder to rename it. Deleting a folder moves its notes to Unfiled.",
                style = BareType.Meta,
                color = colors.textMuted,
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = 16.dp
                )
            )
        }
    }

    if (addOpen) {
        BareNameDialog(
            title = "New folder",
            confirmText = "Create",
            initial = "",
            hint = "Folder name",
            errorFor = { name -> folderNameError(viewModel, name, null) },
            onDismiss = { addOpen = false },
            onSave = { name ->
                viewModel.createFolder(name)
                addOpen = false
            }
        )
    }

    editing?.let { folder ->
        BareNameDialog(
            title = "Rename folder",
            confirmText = "Save",
            initial = folder.name,
            hint = "Folder name",
            errorFor = { name -> folderNameError(viewModel, name, folder.id) },
            onDismiss = { editing = null },
            onSave = { name ->
                viewModel.renameFolder(folder, name)
                editing = null
            }
        )
    }

    deleting?.let { folder ->
        BareDialog(
            title = "Delete folder?",
            onDismissRequest = { deleting = null },
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteFolder(folder)
                deleting = null
            },
            content = {
                Text(
                    text = "Notes in this folder will be moved to Unfiled.",
                    style = BareType.Body,
                    color = LocalBareColors.current.textMuted
                )
            }
        )
    }
}

fun folderNameError(
    viewModel: NotesViewModel,
    name: String,
    exceptId: String?
): String? {
    val clean = name.trim()
    return when {
        clean.isEmpty() -> "Enter a folder name."
        viewModel.folders.any {
            it.id != exceptId && it.name.equals(clean, ignoreCase = true)
        } -> "That folder already exists."
        else -> null
    }
}
