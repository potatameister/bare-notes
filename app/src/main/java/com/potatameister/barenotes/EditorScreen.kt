package com.potatameister.barenotes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun EditorScreen(
    viewModel: NotesViewModel,
    noteId: String?,
    onClose: () -> Unit,
    onOpenFolders: (String?) -> Unit
) {
    val colors = LocalBareColors.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        if (!viewModel.loaded) {
            return@Box
        }

        val existing = noteId?.let(viewModel::noteById)
        if (noteId != null && existing == null) {
            LaunchedEffect(Unit) { onClose() }
            return@Box
        }

        val generatedId = rememberSaveable { UUID.randomUUID().toString() }
        val id = noteId ?: generatedId
        val createdAt = rememberSaveable(id) {
            existing?.createdAt ?: System.currentTimeMillis()
        }
        var title by rememberSaveable(id) { mutableStateOf(existing?.title.orEmpty()) }
        var folderId by rememberSaveable(id) {
            mutableStateOf(existing?.folderId ?: viewModel.newNoteFolderId)
        }
        val body = rememberSaveable(id, saver = RichTextStateSaver) {
            createRichTextState(
                existing?.body.orEmpty(),
                existing?.spans.orEmpty()
            )
        }
        var saved by rememberSaveable(id) { mutableStateOf(existing != null) }
        var deleted by remember { mutableStateOf(false) }
        var showDelete by remember { mutableStateOf(false) }
        var showFolderPicker by remember { mutableStateOf(false) }
        var showNewFolder by remember { mutableStateOf(false) }
        var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
        var wordCount by remember { mutableStateOf(0) }
        var dirty by rememberSaveable(id) { mutableStateOf(false) }
        var revision by remember { mutableStateOf(0) }
        var lastBody by rememberSaveable(id) { mutableStateOf(body.text.toString()) }
        val titleFocus = remember { FocusRequester() }
        val bodyFocus = remember { FocusRequester() }

        fun save(): Boolean {
            if (deleted) return false
            val text = body.text.toString()
            val cleanTitle = title.trim()
            val current = viewModel.noteById(id)
            if (!saved && cleanTitle.isEmpty() && text.isBlank()) return false
            val spans = serializeSpans(body)
            if (current != null &&
                current.title == cleanTitle &&
                current.body == text &&
                current.spans == spans &&
                current.folderId == folderId
            ) {
                saved = true
                dirty = false
                return true
            }
            viewModel.saveNote(
                Note(
                    id = id,
                    title = cleanTitle,
                    body = text,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    spans = spans,
                    folderId = folderId
                )
            )
            saved = true
            dirty = false
            return true
        }

        fun applyFormat(action: () -> Unit) {
            action()
            dirty = true
            revision++
            bodyFocus.requestFocus()
        }

        fun close() {
            save()
            keyboard?.hide()
            onClose()
        }

        BackHandler { close() }

        val latestSave = rememberUpdatedState { save() }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    latestSave.value()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(Unit) {
            if (noteId == null) {
                titleFocus.requestFocus()
                keyboard?.show()
            }
        }

        LaunchedEffect(body.text) {
            val value = body.text.toString()
            if (value != lastBody) {
                dirty = true
                revision++
                lastBody = value
            }
            delay(250)
            wordCount = withContext(Dispatchers.Default) { countWords(value) }
        }

        LaunchedEffect(revision) {
            if (dirty) {
                delay(1500)
                if (dirty && !save()) dirty = false
            }
        }

        LaunchedEffect(viewModel.folders) {
            if (folderId.isNotEmpty() && viewModel.folders.none { it.id == folderId }) {
                folderId = ""
                dirty = true
                revision++
            }
        }

        val folderLabel = viewModel.folderName(folderId) ?: "Unfiled"
        val listInput = remember { listContinuationTransformation() }
        val bodyStyle = BareType.Body.copy(
            color = colors.text,
            fontSize = viewModel.textSizeSp.sp,
            lineHeight = (viewModel.textSizeSp + 4).sp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
        ) {
            BareTopBar(
                navigation = {
                    BareIconButton(
                        iconRes = R.drawable.ic_back,
                        contentDescription = "Back",
                        onClick = { close() }
                    )
                },
                title = {
                    Text(
                        text = "Note",
                        style = BareType.ScreenTitle,
                        color = colors.text
                    )
                },
                actions = {
                    BareIconButton(
                        iconRes = R.drawable.ic_folder,
                        contentDescription = "Move to folder",
                        onClick = { showFolderPicker = true }
                    )
                    if (saved) {
                        BareIconButton(
                            iconRes = R.drawable.ic_delete,
                            contentDescription = "Delete note",
                            onClick = { showDelete = true }
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            ) {
                BareInput(
                    value = title,
                    onValueChange = {
                        title = it
                        dirty = true
                        revision++
                    },
                    hint = "Title",
                    style = BareType.EditorTitle,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() })
                )
                BareDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folder · $folderLabel",
                        style = BareType.Meta,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = buildString {
                            append(
                                when {
                                    !saved -> "Draft"
                                    dirty -> "Unsaved"
                                    else -> "Saved"
                                }
                            )
                            append(" · ")
                            append(
                                context.resources.getQuantityString(
                                    R.plurals.word_count,
                                    wordCount,
                                    wordCount
                                )
                            )
                        },
                        style = BareType.Meta,
                        color = colors.textMuted,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    BasicTextField(
                        state = body,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(bodyFocus)
                            .pointerInput(textLayout) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial
                                    )
                                    val layout = textLayout ?: return@awaitEachGesture
                                    val offset = layout.getOffsetForPosition(down.position)
                                    val line = checkboxPrefixAt(body, offset)
                                        ?: return@awaitEachGesture
                                    val box = layout.getBoundingBox(line.first)
                                    if (down.position.x <= box.left + 28.dp.toPx()) {
                                        toggleCheckAt(body, line.first, line.second)
                                        dirty = true
                                        revision++
                                        save()
                                        down.consume()
                                    }
                                }
                            },
                        textStyle = bodyStyle,
                        keyboardOptions = KeyboardOptions.Default,
                        lineLimits = TextFieldLineLimits.MultiLine(),
                        inputTransformation = listInput,
                        onTextLayout = { getResult -> textLayout = getResult() },
                        cursorBrush = SolidColor(colors.text),
                        decorator = androidx.compose.foundation.text.input.TextFieldDecorator { inner ->
                            Box {
                                if (body.text.isEmpty()) {
                                    Text(
                                        text = "Start writing…",
                                        style = bodyStyle,
                                        color = colors.textMuted
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            BareDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BareFormatButton(
                    label = "B",
                    description = "Bold",
                    active = isInlineStyleActive(body, BoldStyle),
                    onClick = { applyFormat { toggleInlineStyle(body, BoldStyle) } }
                )
                BareFormatButton(
                    label = "I",
                    description = "Italic",
                    active = isInlineStyleActive(body, ItalicStyle),
                    onClick = { applyFormat { toggleInlineStyle(body, ItalicStyle) } }
                )
                BareFormatButton(
                    label = "U",
                    description = "Underline",
                    active = isInlineStyleActive(body, UnderlineStyle),
                    onClick = { applyFormat { toggleInlineStyle(body, UnderlineStyle) } }
                )
                BareFormatButton(
                    label = "S",
                    description = "Strikethrough",
                    active = isInlineStyleActive(body, StrikeStyle),
                    onClick = { applyFormat { toggleInlineStyle(body, StrikeStyle) } }
                )
                BareFormatDivider()
                BareFormatButton(
                    label = "H1",
                    description = "Heading 1",
                    active = isHeadingActive(body, 1),
                    onClick = { applyFormat { toggleHeading(body, 1) } }
                )
                BareFormatButton(
                    label = "H2",
                    description = "Heading 2",
                    active = isHeadingActive(body, 2),
                    onClick = { applyFormat { toggleHeading(body, 2) } }
                )
                BareFormatDivider()
                BareFormatButton(
                    label = "•",
                    description = "Bullet list",
                    active = isPrefixActive(body, PrefixKind.BULLET),
                    onClick = { applyFormat { togglePrefix(body, PrefixKind.BULLET) } }
                )
                BareFormatButton(
                    label = "1.",
                    description = "Numbered list",
                    active = isPrefixActive(body, PrefixKind.NUMBER),
                    onClick = { applyFormat { togglePrefix(body, PrefixKind.NUMBER) } }
                )
                BareFormatButton(
                    label = "☑\uFE0E",
                    description = "Checklist",
                    active = isPrefixActive(body, PrefixKind.CHECK),
                    onClick = { applyFormat { togglePrefix(body, PrefixKind.CHECK) } }
                )
                BareFormatButton(
                    label = "❝\uFE0E",
                    description = "Quote",
                    active = isPrefixActive(body, PrefixKind.QUOTE),
                    onClick = { applyFormat { togglePrefix(body, PrefixKind.QUOTE) } }
                )
                BareFormatDivider()
                BareFormatButton(
                    label = "Clear",
                    description = "Clear formatting",
                    active = false,
                    onClick = { applyFormat { clearFormatting(body) } }
                )
            }
        }

        if (showDelete) {
            BareDialog(
                title = "Delete note?",
                onDismissRequest = { showDelete = false },
                confirmText = "Delete",
                onConfirm = {
                    deleted = true
                    viewModel.deleteNote(id)
                    showDelete = false
                    onClose()
                },
                content = {
                    Text(
                        text = "This cannot be undone.",
                        style = BareType.Body,
                        color = colors.textMuted
                    )
                }
            )
        }

        if (showFolderPicker) {
            BareDialog(
                title = "Move to folder",
                onDismissRequest = { showFolderPicker = false },
                content = {
                    FolderChoiceRow(
                        label = "Unfiled",
                        count = viewModel.unfiledCount,
                        selected = folderId.isEmpty(),
                        onClick = {
                            if (folderId.isNotEmpty()) {
                                folderId = ""
                                dirty = true
                                revision++
                            }
                            showFolderPicker = false
                        }
                    )
                    viewModel.folders.forEach { folder ->
                        FolderChoiceRow(
                            label = folder.name,
                            count = viewModel.noteCount(folder.id),
                            selected = folderId == folder.id,
                            onClick = {
                                if (folderId != folder.id) {
                                    folderId = folder.id
                                    dirty = true
                                    revision++
                                }
                                showFolderPicker = false
                            }
                        )
                    }
                    BareDivider()
                    BareTextRow(
                        label = "New folder…",
                        onClick = {
                            showFolderPicker = false
                            showNewFolder = true
                        }
                    )
                    BareTextRow(
                        label = "Manage folders…",
                        onClick = {
                            showFolderPicker = false
                            val hasNote = save()
                            onOpenFolders(if (hasNote) id else null)
                        }
                    )
                }
            )
        }

        if (showNewFolder) {
            BareNameDialog(
                title = "New folder",
                confirmText = "Create",
                initial = "",
                hint = "Folder name",
                errorFor = { name -> folderNameError(viewModel, name, null) },
                onDismiss = { showNewFolder = false },
                onSave = { name ->
                    val folder = viewModel.createFolder(name)
                    if (folder != null) {
                        folderId = folder.id
                        dirty = true
                        revision++
                    }
                    showNewFolder = false
                }
            )
        }
    }
}

@Composable
private fun FolderChoiceRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalBareColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
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
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "Selected",
                tint = colors.icon,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
