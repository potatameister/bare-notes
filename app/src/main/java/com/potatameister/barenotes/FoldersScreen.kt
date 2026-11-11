package com.potatameister.barenotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FoldersScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit
) {
    val colors = LocalBareColors.current
    val context = LocalContext.current
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

        if (viewModel.folders.isNotEmpty()) {
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.folder_count,
                    viewModel.folders.size,
                    viewModel.folders.size
                ) + " · " + context.resources.getQuantityString(
                    R.plurals.note_count,
                    viewModel.notes.size,
                    viewModel.notes.size
                ),
                style = BareType.Meta,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.folders, key = { it.id }) { folder ->
                    val count = viewModel.noteCount(folder.id)
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { onOpenFolder(folder.id) }
                                .padding(start = 20.dp, end = 4.dp),
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
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    style = BareType.Meta,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            BareIconButton(
                                iconRes = R.drawable.ic_edit,
                                contentDescription = "Rename folder",
                                onClick = { editing = folder }
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
                text = "Tap a folder to open it. The pencil renames it; deleting clears the folder from its notes.",
                style = BareType.Meta,
                color = colors.textMuted,
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 20.dp
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
        val count = viewModel.noteCount(folder.id)
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
                    text = folder.name,
                    style = BareType.Body,
                    color = LocalBareColors.current.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (count > 0) {
                        context.resources.getQuantityString(
                            R.plurals.note_count,
                            count,
                            count
                        ) + " in this folder will no longer be in a folder."
                    } else {
                        "This folder is empty."
                    },
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
