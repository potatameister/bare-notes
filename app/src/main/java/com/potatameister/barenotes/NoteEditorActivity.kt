package com.potatameister.barenotes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.potatameister.barenotes.databinding.ActivityEditorBinding
import java.util.UUID

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
    }

    private lateinit var binding: ActivityEditorBinding
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        note = if (noteId != null) {
            NoteStore.load(this).firstOrNull { it.id == noteId } ?: newNote()
        } else {
            newNote()
        }

        binding.titleInput.setText(note?.title.orEmpty())
        binding.bodyInput.setText(note?.body.orEmpty())

        binding.backButton.setOnClickListener { finish() }
        binding.doneButton.setOnClickListener {
            save()
            finish()
        }

        if (noteId == null) {
            binding.titleInput.requestFocus()
        }
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun newNote(): Note = Note(
        id = UUID.randomUUID().toString(),
        title = "",
        body = "",
        updatedAt = System.currentTimeMillis()
    )

    private fun save() {
        val current = note ?: return
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val body = binding.bodyInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() && body.isEmpty()) return

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
