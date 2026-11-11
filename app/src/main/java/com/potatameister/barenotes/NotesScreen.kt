package com.potatameister.barenotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onMenu: () -> Unit,
    onAdd: () -> Unit,
    onOpenNote: (Note) -> Unit
) {
    val colors = LocalBareColors.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val visible = viewModel.visibleNotes
    val title = when (val filter = viewModel.filter) {
        NoteFilter.All -> "All notes"
        NoteFilter.Unfiled -> "Unfiled"
        is NoteFilter.InFolder -> viewModel.folderName(filter.id) ?: "All notes"
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        BareTopBar(
            navigation = {
                if (searchOpen) {
                    BareIconButton(
                        iconRes = R.drawable.ic_back,
                        contentDescription = "Close search",
                        onClick = {
                            searchOpen = false
                            viewModel.updateSearchQuery("")
                            keyboard?.hide()
                        }
                    )
                } else {
                    BareIconButton(
                        iconRes = R.drawable.ic_menu,
                        contentDescription = "Open menu",
                        onClick = onMenu
                    )
                }
            },
            title = {
                if (searchOpen) {
                    BareInput(
                        value = viewModel.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        hint = "Search notes",
                        style = BareType.Body,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
                    )
                } else {
                    Text(
                        text = title,
                        style = BareType.ScreenTitle,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                if (searchOpen) {
                    if (viewModel.searchQuery.isNotBlank()) {
                        BareIconButton(
                            iconRes = R.drawable.ic_close,
                            contentDescription = "Clear search",
                            onClick = { viewModel.updateSearchQuery("") }
                        )
                    }
                } else {
                    BareIconButton(
                        iconRes = R.drawable.ic_search,
                        contentDescription = "Search notes",
                        onClick = { searchOpen = true }
                    )
                    BareIconButton(
                        iconRes = R.drawable.ic_plus,
                        contentDescription = "New note",
                        onClick = onAdd,
                        enabled = viewModel.loaded
                    )
                }
            }
        )

        if (viewModel.loaded && visible.isNotEmpty()) {
            val countLabel = when {
                viewModel.searchQuery.isNotBlank() && visible.size == 1 -> "1 RESULT"
                viewModel.searchQuery.isNotBlank() -> "${visible.size} RESULTS"
                visible.size == 1 -> "1 NOTE"
                else -> "${visible.size} NOTES"
            }
            Text(
                text = countLabel,
                style = BareType.Eyebrow,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visible, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        dateLabel = if (viewModel.sortOrder == SettingsPrefs.SORT_CREATED) {
                            "Created ${NoteTime.relative(context, note.createdAt)}"
                        } else {
                            "Edited ${NoteTime.relative(context, note.updatedAt)}"
                        },
                        folderLabel = if (viewModel.filter == NoteFilter.All) {
                            viewModel.folderName(note.folderId)
                        } else {
                            null
                        },
                        onClick = { onOpenNote(note) }
                    )
                }
            }

            if (viewModel.loaded && visible.isEmpty()) {
                EmptyState(
                    title = when {
                        viewModel.searchQuery.isNotBlank() -> "No matches"
                        viewModel.filter == NoteFilter.Unfiled -> "No unfiled notes"
                        viewModel.filter is NoteFilter.InFolder -> "No notes in this folder"
                        else -> "No notes yet"
                    },
                    hint = when {
                        viewModel.searchQuery.isNotBlank() -> "Try a different search."
                        else -> "Tap + to write one."
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    dateLabel: String,
    folderLabel: String?,
    onClick: () -> Unit
) {
    val colors = LocalBareColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, top = 16.dp, end = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = BareType.ListTitle,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dateLabel,
                style = BareType.Meta,
                color = colors.textMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Text(
            text = previewFor(note.body).ifBlank { "No additional text" },
            style = BareType.Preview,
            color = colors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (!folderLabel.isNullOrBlank()) {
            Text(
                text = "in $folderLabel",
                style = BareType.Meta,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        BareDivider()
    }
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalBareColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = BareType.ScreenTitle,
            color = colors.text
        )
        Text(
            text = hint,
            style = BareType.Preview,
            color = colors.textMuted,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
