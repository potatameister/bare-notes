package com.potatameister.barenotes

import android.os.Bundle
import android.util.TypedValue
import android.view.View
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
    }

    private lateinit var binding: ActivityEditorBinding
    private var note: Note? = null
    private var isSaved = false
    private var deleted = false
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
        binding.bodyInput.doAfterTextChanged { updateMeta() }

        binding.backButton.setOnClickListener { finish() }
        binding.doneButton.setOnClickListener {
            save()
            finish()
        }
        binding.deleteButton.setOnClickListener { confirmDelete() }

        updateMeta()

        if (noteId == null) {
            binding.titleInput.requestFocus()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_NOTE_ID, note?.id)
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
        val body = binding.bodyInput.text?.toString()?.trim().orEmpty()

        if (!isSaved && title.isEmpty() && body.isEmpty()) return
        if (title == current.title && body == current.body) return

        val updated = current.copy(
            title = title,
            body = body,
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
