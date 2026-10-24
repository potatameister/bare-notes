package com.potatameister.barenotes

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.potatameister.barenotes.databinding.ActivityEditorBinding
import java.util.UUID

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        private const val EXTRA_EDITING = "editing"
    }

    private lateinit var binding: ActivityEditorBinding
    private var note: Note? = null
    private var isSaved = false
    private var deleted = false
    private var isEditing = false
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

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
            ?: savedInstanceState?.getString(EXTRA_NOTE_ID)
        note = if (noteId != null) {
            NoteStore.load(this).firstOrNull { it.id == noteId }
                ?.also { isSaved = true }
                ?: newNote()
        } else {
            newNote()
        }
        note?.let { intent.putExtra(EXTRA_NOTE_ID, it.id) }

        binding.titleInput.setText(note?.title.orEmpty())
        binding.bodyInput.setText(note?.body.orEmpty())
        applySpans(note?.spans.orEmpty())

        binding.bodyInput.doAfterTextChanged { updateMeta() }

        binding.backButton.setOnClickListener { finish() }
        binding.doneButton.setOnClickListener {
            save()
            finish()
        }
        binding.deleteButton.setOnClickListener { confirmDelete() }
        binding.formatBold.setOnClickListener { toggleStyle(Typeface.BOLD) }
        binding.formatItalic.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        binding.formatH1.setOnClickListener { toggleHeading(1) }
        binding.formatH2.setOnClickListener { toggleHeading(2) }

        setupTapToEdit(binding.titleInput, focusTitle = true)
        setupTapToEdit(binding.bodyInput, focusTitle = false)

        isEditing = savedInstanceState?.getBoolean(EXTRA_EDITING) ?: (noteId == null)
        renderMode()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_NOTE_ID, note?.id)
        outState.putBoolean(EXTRA_EDITING, isEditing)
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun newNote(): Note {
        val now = System.currentTimeMillis()
        return Note(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            createdAt = now,
            updatedAt = now
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

    private fun showKeyboard(view: View) {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updateMeta() {
        val body = binding.bodyInput.text?.toString().orEmpty()
        val characters = body.length
        val words = if (body.isBlank()) 0 else body.trim().split(wordSplit).size

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

    private fun toggleHeading(level: Int) {
        val text = binding.bodyInput.text
        val cursor = binding.bodyInput.selectionStart.coerceAtLeast(0)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
            .let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor)
            .let { if (it < 0) text.length else it }
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

    private fun wordBounds(text: CharSequence, position: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        var start = position.coerceIn(0, text.length)
        var end = start
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        return start to end
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

        if (!isSaved && title.isEmpty() && body.isEmpty()) return
        if (title == current.title && body == current.body && spans == current.spans) return

        val updated = current.copy(
            title = title,
            body = body,
            spans = spans,
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
