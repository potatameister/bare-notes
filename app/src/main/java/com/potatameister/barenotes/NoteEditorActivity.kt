package com.potatameister.barenotes

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.potatameister.barenotes.databinding.ActivityEditorBinding
import java.util.UUID

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_FOLDER_ID = "folder_id"
        private const val EXTRA_EDITING = "editing"

        private const val BULLET = "• "
        private const val CHECK_OPEN = "☐ "
        private const val CHECK_DONE = "☑ "
        private const val QUOTE = "❝ "
        private val NUMBER_PREFIX = Regex("^(\\d+)\\.\\s+")
    }

    private lateinit var binding: ActivityEditorBinding
    private var note: Note? = null
    private var isSaved = false
    private var deleted = false
    private var isEditing = false
    private var folderId = ""
    private var folders: List<Folder> = emptyList()
    private var autoListGuard = false
    private val wordSplit = Regex("\\s+")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()

        binding.bodyInput.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            SettingsPrefs.textSizeSp(this).toFloat()
        )
        binding.formatUnderline.paintFlags =
            binding.formatUnderline.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.formatStrike.paintFlags =
            binding.formatStrike.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
            ?: savedInstanceState?.getString(EXTRA_NOTE_ID)
        note = if (noteId != null) {
            NoteStore.load(this).firstOrNull { it.id == noteId }
                ?.also { isSaved = true }
                ?: newNote(intent.getStringExtra(EXTRA_FOLDER_ID).orEmpty())
        } else {
            newNote(intent.getStringExtra(EXTRA_FOLDER_ID).orEmpty())
        }
        note?.let { intent.putExtra(EXTRA_NOTE_ID, it.id) }
        folderId = note?.folderId.orEmpty()

        folders = FolderStore.load(this)
        if (folderId.isNotEmpty() && folders.none { it.id == folderId }) {
            folderId = ""
        }

        binding.titleInput.setText(note?.title.orEmpty())
        binding.bodyInput.setText(note?.body.orEmpty())
        applySpans(note?.spans.orEmpty())

        binding.bodyInput.doAfterTextChanged { editable ->
            updateMeta()
            if (!autoListGuard && editable != null) continueList(editable)
        }

        binding.backButton.setOnClickListener { finish() }
        binding.doneButton.setOnClickListener { saveAndStay() }
        binding.deleteButton.setOnClickListener { confirmDelete() }
        binding.folderRow.setOnClickListener { chooseFolder() }

        binding.formatBold.setOnClickListener { toggleStyle(Typeface.BOLD) }
        binding.formatItalic.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        binding.formatUnderline.setOnClickListener { toggleUnderline() }
        binding.formatStrike.setOnClickListener { toggleStrike() }
        binding.formatH1.setOnClickListener { toggleHeading(1) }
        binding.formatH2.setOnClickListener { toggleHeading(2) }
        binding.formatBullet.setOnClickListener { toggleBullet() }
        binding.formatNumber.setOnClickListener { toggleNumber() }
        binding.formatCheck.setOnClickListener { toggleCheck() }
        binding.formatQuote.setOnClickListener { toggleQuote() }
        binding.formatClear.setOnClickListener { clearFormatting() }

        setupTapToEdit(binding.titleInput, focusTitle = true)
        setupTapToEdit(binding.bodyInput, focusTitle = false)

        isEditing = savedInstanceState?.getBoolean(EXTRA_EDITING) ?: (noteId == null)
        renderMode()
        refreshFolderRow()
        updateMeta()

        if (isEditing) {
            val target = if (noteId == null) binding.titleInput else binding.bodyInput
            target.post {
                target.requestFocus()
                if (target === binding.titleInput) {
                    target.setSelection(target.text.length)
                }
                showKeyboard(target)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        folders = FolderStore.load(this)
        if (folderId.isNotEmpty() && folders.none { it.id == folderId }) {
            folderId = ""
            refreshFolderRow()
            save()
        } else {
            refreshFolderRow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_NOTE_ID, note?.id)
        outState.putBoolean(EXTRA_EDITING, isEditing)
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun newNote(folderId: String): Note {
        val now = System.currentTimeMillis()
        return Note(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            createdAt = now,
            updatedAt = now,
            folderId = folderId
        )
    }

    private fun renderMode() {
        binding.doneButton.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.formatBar.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.formatDivider.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.titleInput.isCursorVisible = isEditing
        binding.titleInput.showSoftInputOnFocus = isEditing
        binding.bodyInput.isCursorVisible = isEditing
        binding.bodyInput.showSoftInputOnFocus = isEditing
        binding.deleteButton.visibility = if (isSaved) View.VISIBLE else View.GONE
    }

    private fun setupTapToEdit(view: EditText, focusTitle: Boolean) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!isEditing) {
                    if (view === binding.bodyInput && toggleChecklistAt(e)) {
                        return true
                    }
                    enterEditMode(focusTitle)
                    return true
                }
                return false
            }
        })
        view.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }

    private fun enterEditMode(focusTitle: Boolean) {
        if (isEditing) return
        isEditing = true
        renderMode()

        val target = if (focusTitle) binding.titleInput else binding.bodyInput
        target.requestFocus()
        if (focusTitle || target.selectionStart < 0) {
            target.setSelection(target.text.length)
        }
        target.post { showKeyboard(target) }
    }

    private fun saveAndStay() {
        save()
        if (!isSaved) {
            finish()
            return
        }
        isEditing = false
        renderMode()
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.bodyInput.windowToken, 0)
        binding.bodyInput.clearFocus()
        binding.titleInput.clearFocus()
        updateMeta()
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun refreshFolderRow() {
        val name = folders.firstOrNull { it.id == folderId }?.name
            ?: getString(R.string.unfiled)
        binding.folderRow.text = getString(R.string.folder_label, name)
    }

    private fun updateMeta() {
        val body = binding.bodyInput.text?.toString().orEmpty()
        val characters = body.length
        val words = if (body.isBlank()) {
            0
        } else {
            body.trim().split(wordSplit).count { token ->
                token.any { it.isLetterOrDigit() }
            }
        }

        binding.metaCounts.text = getString(
            R.string.meta_counts,
            resources.getQuantityString(R.plurals.character_count, characters, characters),
            resources.getQuantityString(R.plurals.word_count, words, words)
        )
        binding.editedAt.text = if (isSaved) {
            note?.let {
                getString(R.string.edited_absolute, NoteTime.absolute(this, it.updatedAt))
            } ?: getString(R.string.new_note)
        } else {
            getString(R.string.new_note)
        }
        binding.deleteButton.visibility = if (isSaved) View.VISIBLE else View.GONE
    }

    private fun applySpans(spans: List<NoteSpan>) {
        val text = binding.bodyInput.text
        spans.forEach { span ->
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(0, text.length)
            if (start >= end) return@forEach
            when (span.type) {
                NoteSpan.BOLD -> text.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                )
                NoteSpan.ITALIC -> text.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                )
                NoteSpan.UNDERLINE -> text.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                )
                NoteSpan.STRIKE -> text.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                )
                NoteSpan.H1 -> text.setSpan(
                    HeadingSpan(1),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                NoteSpan.H2 -> text.setSpan(
                    HeadingSpan(2),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun collectSpans(): List<NoteSpan> {
        val text = binding.bodyInput.text
        val result = mutableListOf<NoteSpan>()

        text.getSpans(0, text.length, StyleSpan::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            when (span.style) {
                Typeface.BOLD -> result.add(NoteSpan(NoteSpan.BOLD, start, end))
                Typeface.ITALIC -> result.add(NoteSpan(NoteSpan.ITALIC, start, end))
                Typeface.BOLD_ITALIC -> {
                    result.add(NoteSpan(NoteSpan.BOLD, start, end))
                    result.add(NoteSpan(NoteSpan.ITALIC, start, end))
                }
            }
        }

        text.getSpans(0, text.length, UnderlineSpan::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            if (start >= 0 && end > start) {
                result.add(NoteSpan(NoteSpan.UNDERLINE, start, end))
            }
        }

        text.getSpans(0, text.length, StrikethroughSpan::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            if (start >= 0 && end > start) {
                result.add(NoteSpan(NoteSpan.STRIKE, start, end))
            }
        }

        text.getSpans(0, text.length, HeadingSpan::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            result.add(
                NoteSpan(
                    if (span.level == 1) NoteSpan.H1 else NoteSpan.H2,
                    start,
                    end
                )
            )
        }

        return result.distinct().sortedWith(compareBy({ it.start }, { it.end }, { it.type }))
    }

    private fun toggleStyle(style: Int) {
        val text = binding.bodyInput.text
        var start = binding.bodyInput.selectionStart
        var end = binding.bodyInput.selectionEnd
        if (start > end) {
            val swap = start
            start = end
            end = swap
        }
        if (start == end) {
            val bounds = wordBounds(text, start)
            start = bounds.first
            end = bounds.second
        }
        if (start >= end) return

        val existing = text.getSpans(start, end, StyleSpan::class.java).filter {
            it.style == style &&
                text.getSpanStart(it) <= start &&
                text.getSpanEnd(it) >= end
        }
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(
                StyleSpan(style),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }

        binding.bodyInput.setSelection(start, end)
        updateMeta()
    }

    private fun toggleUnderline() {
        val text = binding.bodyInput.text
        var start = binding.bodyInput.selectionStart
        var end = binding.bodyInput.selectionEnd
        if (start > end) {
            val swap = start
            start = end
            end = swap
        }
        if (start == end) {
            val bounds = wordBounds(text, start)
            start = bounds.first
            end = bounds.second
        }
        if (start >= end) return

        val existing = text.getSpans(start, end, UnderlineSpan::class.java).filter {
            text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
        }
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(
                UnderlineSpan(),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }
        binding.bodyInput.setSelection(start, end)
        updateMeta()
    }

    private fun toggleStrike() {
        val text = binding.bodyInput.text
        var start = binding.bodyInput.selectionStart
        var end = binding.bodyInput.selectionEnd
        if (start > end) {
            val swap = start
            start = end
            end = swap
        }
        if (start == end) {
            val bounds = wordBounds(text, start)
            start = bounds.first
            end = bounds.second
        }
        if (start >= end) return

        val existing = text.getSpans(start, end, StrikethroughSpan::class.java).filter {
            text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
        }
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(
                StrikethroughSpan(),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }
        binding.bodyInput.setSelection(start, end)
        updateMeta()
    }

    private fun toggleHeading(level: Int) {
        val text = binding.bodyInput.text
        val cursor = binding.bodyInput.selectionStart.coerceAtLeast(0)
        val (lineStart, lineEnd) = lineBounds(cursor, text)
        if (lineStart >= lineEnd) return

        val headings = text.getSpans(lineStart, lineEnd, HeadingSpan::class.java)
        val same = headings.firstOrNull {
            it.level == level &&
                text.getSpanStart(it) == lineStart &&
                text.getSpanEnd(it) == lineEnd
        }
        headings.forEach { text.removeSpan(it) }
        if (same == null) {
            text.setSpan(
                HeadingSpan(level),
                lineStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.bodyInput.setSelection(cursor)
        updateMeta()
    }

    private fun currentLine(): Triple<Int, Int, String> {
        val text = binding.bodyInput.text
        val cursor = binding.bodyInput.selectionStart.coerceAtLeast(0)
        val (start, end) = lineBounds(cursor, text)
        return Triple(start, end, text.subSequence(start, end).toString())
    }

    private fun lineBounds(cursor: Int, text: CharSequence): Pair<Int, Int> {
        val start = if (cursor <= 0) {
            0
        } else {
            text.lastIndexOf('\n', cursor - 1).let { if (it < 0) 0 else it + 1 }
        }
        val end = text.indexOf('\n', cursor)
            .let { if (it < 0) text.length else it }
        return start to end
    }

    private fun splitPrefix(line: String): Pair<String, String> {
        for (prefix in listOf(BULLET, CHECK_OPEN, CHECK_DONE, QUOTE)) {
            if (line.startsWith(prefix)) {
                return prefix to line.substring(prefix.length)
            }
        }
        val match = NUMBER_PREFIX.find(line)
        if (match != null) {
            return match.value to line.substring(match.value.length)
        }
        return "" to line
    }

    private fun numberedPrefix(prefix: String): Int? =
        NUMBER_PREFIX.find(prefix)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun applyPrefix(newPrefix: String) {
        val text = binding.bodyInput.text
        val cursor = binding.bodyInput.selectionStart.coerceAtLeast(0)
        val (lineStart, lineEnd) = lineBounds(cursor, text)
        val line = text.subSequence(lineStart, lineEnd).toString()
        val (oldPrefix, content) = splitPrefix(line)
        val offset = (cursor - lineStart - oldPrefix.length).coerceIn(0, content.length)

        autoListGuard = true
        text.replace(lineStart, lineEnd, newPrefix + content)
        binding.bodyInput.setSelection(lineStart + newPrefix.length + offset)
        autoListGuard = false
        updateMeta()
    }

    private fun toggleBullet() {
        val (_, _, line) = currentLine()
        val (old, _) = splitPrefix(line)
        applyPrefix(if (old == BULLET) "" else BULLET)
    }

    private fun toggleQuote() {
        val (_, _, line) = currentLine()
        val (old, _) = splitPrefix(line)
        applyPrefix(if (old == QUOTE) "" else QUOTE)
    }

    private fun toggleCheck() {
        val (_, _, line) = currentLine()
        val (old, _) = splitPrefix(line)
        applyPrefix(
            when (old) {
                CHECK_OPEN -> CHECK_DONE
                CHECK_DONE -> ""
                else -> CHECK_OPEN
            }
        )
    }

    private fun toggleNumber() {
        val text = binding.bodyInput.text
        val (lineStart, _, line) = currentLine()
        val (old, _) = splitPrefix(line)

        if (numberedPrefix(old) != null) {
            applyPrefix("")
            return
        }

        val previousNumber = if (lineStart > 1) {
            val prevEnd = lineStart - 1
            val prevStart = text.lastIndexOf('\n', prevEnd - 1)
                .let { if (it < 0) 0 else it + 1 }
            val prevLine = text.subSequence(prevStart, prevEnd).toString()
            numberedPrefix(splitPrefix(prevLine).first) ?: 0
        } else {
            0
        }
        applyPrefix("${previousNumber + 1}. ")
    }

    private fun clearFormatting() {
        val text = binding.bodyInput.text
        var start = binding.bodyInput.selectionStart
        var end = binding.bodyInput.selectionEnd
        if (start > end) {
            val swap = start
            start = end
            end = swap
        }
        val blockStart = if (start <= 0) {
            0
        } else {
            text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
        }
        val blockEnd = text.indexOf('\n', end)
            .let { if (it < 0) text.length else it }

        if (blockStart > blockEnd) return

        text.getSpans(blockStart, blockEnd, StyleSpan::class.java)
            .forEach { text.removeSpan(it) }
        text.getSpans(blockStart, blockEnd, UnderlineSpan::class.java)
            .forEach { text.removeSpan(it) }
        text.getSpans(blockStart, blockEnd, StrikethroughSpan::class.java)
            .forEach { text.removeSpan(it) }
        text.getSpans(blockStart, blockEnd, HeadingSpan::class.java)
            .forEach { text.removeSpan(it) }

        val block = text.subSequence(blockStart, blockEnd).toString()
        val stripped = block.split('\n')
            .joinToString("\n") { splitPrefix(it).second }

        autoListGuard = true
        text.replace(blockStart, blockEnd, stripped)
        binding.bodyInput.setSelection(
            (blockStart + stripped.length).coerceAtMost(text.length)
        )
        autoListGuard = false
        updateMeta()
    }

    private fun wordBounds(text: CharSequence, position: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        var start = position.coerceIn(0, text.length)
        var end = start
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        return start to end
    }

    private fun continueList(editable: android.text.Editable) {
        val cursor = binding.bodyInput.selectionStart
        if (cursor <= 0 || cursor > editable.length) return
        if (editable[cursor - 1] != '\n') return

        val previousEnd = cursor - 1
        if (previousEnd <= 0) return
        val previousStart = editable.lastIndexOf('\n', previousEnd - 1)
            .let { if (it < 0) 0 else it + 1 }
        val previousLine = editable.subSequence(previousStart, previousEnd).toString()
        val (prefix, content) = splitPrefix(previousLine)
        if (prefix.isEmpty()) return

        if (content.isBlank()) {
            autoListGuard = true
            editable.replace(previousStart, previousEnd, content)
            binding.bodyInput.setSelection(previousStart + content.length + 1)
            autoListGuard = false
            return
        }

        val nextPrefix = when {
            prefix == BULLET -> BULLET
            prefix == CHECK_OPEN || prefix == CHECK_DONE -> CHECK_OPEN
            prefix == QUOTE -> QUOTE
            numberedPrefix(prefix) != null -> "${(numberedPrefix(prefix) ?: 0) + 1}. "
            else -> return
        }

        autoListGuard = true
        editable.insert(cursor, nextPrefix)
        binding.bodyInput.setSelection(cursor + nextPrefix.length)
        autoListGuard = false
    }

    private fun toggleChecklistAt(event: MotionEvent): Boolean {
        val text = binding.bodyInput.text
        if (text.isEmpty()) return false
        val offset = binding.bodyInput.getOffsetForPosition(event.x, event.y)
        val position = offset.coerceIn(0, text.length - 1)
        val (lineStart, lineEnd) = lineBounds(position, text)
        val line = text.subSequence(lineStart, lineEnd).toString()
        val replacement = when {
            line.startsWith(CHECK_OPEN) -> CHECK_DONE
            line.startsWith(CHECK_DONE) -> CHECK_OPEN
            else -> return false
        }
        val oldLength = if (line.startsWith(CHECK_OPEN)) CHECK_OPEN.length else CHECK_DONE.length

        autoListGuard = true
        text.replace(lineStart, lineStart + oldLength, replacement)
        autoListGuard = false
        save()
        updateMeta()
        return true
    }

    private fun chooseFolder() {
        folders = FolderStore.load(this)
        val items = mutableListOf<CharSequence>()
        items.add(getString(R.string.unfiled))
        items.addAll(folders.map { it.name })
        items.add(getString(R.string.folder_new))
        items.add(getString(R.string.folder_manage))

        AlertDialog.Builder(this)
            .setTitle(R.string.folder_pick_title)
            .setItems(items.toTypedArray()) { _, which ->
                when {
                    which == 0 -> setFolder("")
                    which in 1..folders.size -> setFolder(folders[which - 1].id)
                    which == folders.size + 1 -> showNewFolderDialog()
                    which == folders.size + 2 -> {
                        startActivity(Intent(this, FoldersActivity::class.java))
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setFolder(id: String) {
        folderId = id
        refreshFolderRow()
        save()
    }

    private fun showNewFolderDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.folder_name)
            setSingleLine()
            setTextColor(ContextCompat.getColor(this@NoteEditorActivity, R.color.text))
            setHintTextColor(
                ContextCompat.getColor(this@NoteEditorActivity, R.color.text_muted)
            )
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_folder)
            .setView(input)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            input.requestFocus()
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text?.toString()?.trim().orEmpty()
                if (createFolderForNote(name)) {
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun createFolderForNote(name: String): Boolean {
        if (name.isBlank()) {
            Toast.makeText(this, R.string.folder_name_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (folders.any { it.name.equals(name, ignoreCase = true) }) {
            Toast.makeText(this, R.string.folder_name_exists, Toast.LENGTH_SHORT).show()
            return false
        }
        val folder = Folder(UUID.randomUUID().toString(), name)
        FolderStore.save(this, folders + folder)
        folders = FolderStore.load(this)
        setFolder(folder.id)
        return true
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_note)
            .setMessage(R.string.delete_note_message)
            .setPositiveButton(R.string.delete) { _, _ -> deleteNote() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteNote() {
        val current = note ?: return
        deleted = true
        NoteStore.save(this, NoteStore.load(this).filterNot { it.id == current.id })
        finish()
    }

    private fun save() {
        if (deleted) return
        val current = note ?: return
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val body = binding.bodyInput.text?.toString().orEmpty()
        val spans = collectSpans()

        if (!isSaved && title.isEmpty() && body.isBlank()) return
        if (title == current.title &&
            body == current.body &&
            spans == current.spans &&
            folderId == current.folderId
        ) return

        val updated = current.copy(
            title = title,
            body = body,
            spans = spans,
            folderId = folderId,
            updatedAt = System.currentTimeMillis()
        )

        val all = NoteStore.load(this).toMutableList()
        val index = all.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            all[index] = updated
        } else {
            all.add(updated)
        }
        NoteStore.save(this, all)
        note = updated
        isSaved = true
        updateMeta()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.editorRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                maxOf(bars.bottom, ime.bottom)
            )
            insets
        }
    }
}
