package com.potatameister.barenotes

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.potatameister.barenotes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter
    private val notes = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()

        adapter = NoteAdapter { note -> openEditor(note.id) }
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.adapter = adapter
        binding.notesList.setHasFixedSize(true)

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.sidebar)
        }
        binding.addButton.setOnClickListener { openEditor(null) }
        binding.navAll.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
        }
        binding.navAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
            showAbout()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        notes.clear()
        notes.addAll(NoteStore.load(this).sortedByDescending { it.updatedAt })
        adapter.submit(notes)
        binding.emptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(noteId: String?) {
        startActivity(
            Intent(this, NoteEditorActivity::class.java)
                .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
        )
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.sidebar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }
}
