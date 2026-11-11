package com.potatameister.barenotes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.input.TextFieldDecorator
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
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
        var wordCount by remember { mutableStateOf(0) }
        var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
        var dirty by rememberSaveable(id) { mutableStateOf(false) }
        var revision by remember { mutableStateOf(0) }
        var lastBody by rememberSaveable(id) { mutableStateOf(body.text.toString()) }
        var isEditing by rememberSaveable(id) { mutableStateOf(existing == null) }
        val titleFocus = remember { FocusRequester() }
        val bodyFocus = remember { FocusRequester() }
        val bodyScroll = rememberScrollState()
        val focusManager = LocalFocusManager.current

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

        fun toggleCheck(start: Int, end: Int) {
            toggleCheckAt(body, start, end)
            dirty = true
            revision++
            save()
        }

        fun finishEditing() {
            val ok = save()
            keyboard?.hide()
            focusManager.clearFocus()
            if (ok) {
                isEditing = false
            } else if (!saved) {
                onClose()
            }
        }

        fun close() {
            save()
            keyboard?.hide()
            focusManager.clearFocus()
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

        LaunchedEffect(isEditing) {
            if (isEditing) {
                val target = if (noteId == null && title.isBlank()) {
                    titleFocus
                } else {
                    bodyFocus
                }
                target.requestFocus()
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
        val wordLabel = context.resources.getQuantityString(
            R.plurals.word_count,
            wordCount,
            wordCount
        )
        val status = when {
            !saved -> "Draft"
            dirty -> "Unsaved"
            else -> "Saved"
        }
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
                    if (isEditing) {
                        BareTextAction("Save") { finishEditing() }
                    } else {
                        BareIconButton(
                            iconRes = R.drawable.ic_edit,
                            contentDescription = "Edit note",
                            onClick = { isEditing = true }
                        )
                    }
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
                if (isEditing) {
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
                        keyboardActions = KeyboardActions(
                            onNext = { bodyFocus.requestFocus() }
                        )
                    )
                } else {
                    Text(
                        text = title.ifBlank { "Untitled" },
                        style = BareType.EditorTitle,
                        color = colors.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                        text = if (isEditing) "$status · $wordLabel" else wordLabel,
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
                    if (isEditing) {
                        NoteBodyField(
                            state = body,
                            editable = true,
                            textStyle = bodyStyle,
                            scrollState = bodyScroll,
                            textLayout = textLayout,
                            onTextLayout = { textLayout = it },
                            onToggleCheck = { start, end -> toggleCheck(start, end) },
                            placeholder = "Start writing…",
                            colors = colors,
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(bodyFocus)
                        )
                    } else {
                        NoteBodyField(
                            state = body,
                            editable = false,
                            textStyle = bodyStyle,
                            scrollState = bodyScroll,
                            textLayout = textLayout,
                            onTextLayout = { textLayout = it },
                            onToggleCheck = { start, end -> toggleCheck(start, end) },
                            placeholder = "Empty note",
                            colors = colors,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (isEditing) {
                BareDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 4.dp, end = 4.dp),
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
                BareFormatIconButton(
                    iconRes = R.drawable.ic_checklist,
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
                BareFormatDivider()
                BareIconButton(
                    iconRes = R.drawable.ic_keyboard_hide,
                    contentDescription = "Hide keyboard",
                    onClick = { keyboard?.hide() }
                )
                }
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
private fun NoteBodyField(
    state: TextFieldState,
    editable: Boolean,
    textStyle: TextStyle,
    scrollState: ScrollState,
    textLayout: TextLayoutResult?,
    onTextLayout: (TextLayoutResult?) -> Unit,
    onToggleCheck: (Int, Int) -> Unit,
    placeholder: String,
    colors: BareColors,
    modifier: Modifier = Modifier
) {
    val input = remember { listContinuationTransformation() }
    val output = remember { checkboxOutputTransformation() }

    BasicTextField(
        state = state,
        modifier = modifier
            .drawWithContent {
                drawContent()
                textLayout?.let { layout ->
                    drawCheckboxes(
                        layout = layout,
                        text = state.text,
                        scrollOffset = scrollState.value,
                        colors = colors
                    )
                }
            }
            .pointerInput(textLayout) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val layout = textLayout ?: return@awaitEachGesture
                    val offset = layout.getOffsetForPosition(
                        Offset(
                            down.position.x,
                            down.position.y + scrollState.value
                        )
                    )
                    val line = checkboxPrefixAt(state, offset)
                        ?: return@awaitEachGesture
                    val box = layout.getBoundingBox(line.first)
                    val left = box.left
                    if (down.position.x >= left - 4.dp.toPx() &&
                        down.position.x <= left + 30.dp.toPx()
                    ) {
                        onToggleCheck(line.first, line.second)
                        down.consume()
                    }
                }
            },
        readOnly = !editable,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions.Default,
        lineLimits = TextFieldLineLimits.MultiLine(),
        inputTransformation = input,
        outputTransformation = output,
        onTextLayout = { onTextLayout(it()) },
        scrollState = scrollState,
        cursorBrush = SolidColor(if (editable) colors.text else Color.Transparent),
        decorator = TextFieldDecorator { inner ->
            Box {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = colors.textMuted
                    )
                }
                inner()
            }
        }
    )
}

private fun DrawScope.drawCheckboxes(
    layout: TextLayoutResult,
    text: CharSequence,
    scrollOffset: Int,
    colors: BareColors
) {
    clipRect(0f, 0f, size.width, size.height) {
        for (line in 0 until layout.lineCount) {
            val start = layout.getLineStart(line)
            if (start >= text.length) continue
            val checked = text[start] == '☑'
            if (text[start] != '☐' && !checked) continue

            val lineTop = layout.getLineTop(line) - scrollOffset
            val lineHeight = layout.getLineBottom(line) - layout.getLineTop(line)
            val boxSize = minOf(20.dp.toPx(), lineHeight - 2.dp.toPx())
                .coerceAtLeast(12.dp.toPx())
            val top = lineTop + (lineHeight - boxSize) / 2f
            val left = layout.getBoundingBox(start).left

            if (checked) {
                drawRect(
                    color = colors.text,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize)
                )
                val check = Path().apply {
                    moveTo(left + boxSize * 0.20f, top + boxSize * 0.55f)
                    lineTo(left + boxSize * 0.42f, top + boxSize * 0.76f)
                    lineTo(left + boxSize * 0.82f, top + boxSize * 0.28f)
                }
                drawPath(
                    path = check,
                    color = colors.bg,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                drawRect(
                    color = colors.text,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    style = Stroke(1.8.dp.toPx())
                )
            }
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
