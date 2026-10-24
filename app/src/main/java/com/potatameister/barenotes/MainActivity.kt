package com.potatameister.barenotes

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.potatameister.barenotes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter
    private val notes = mutableListOf<Note>()
    private var pendingThemeApply = false
    private val applyTheme = Runnable {
        if (!isFinishing && !isDestroyed) {
            if (binding.drawerLayout.isDrawerOpen(binding.sidebar)) {
                pendingThemeApply = true
                binding.drawerLayout.closeDrawer(binding.sidebar)
            } else {
                ThemePrefs.applyStored(this)
            }
        }
    }

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

        binding.darkSwitch.isChecked = ThemePrefs.isDark(this)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                if (pendingThemeApply) {
                    pendingThemeApply = false
                    ThemePrefs.applyStored(this@MainActivity)
                }
            }
        })
        binding.darkSwitch.setOnCheckedChangeListener { _, checked ->
            ThemePrefs.save(this, checked)
            binding.darkSwitch.removeCallbacks(applyTheme)
            binding.darkSwitch.postDelayed(applyTheme, 200L)
        }
        binding.themeRow.setOnClickListener { binding.darkSwitch.toggle() }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.sidebar)
        }
        binding.addButton.setOnClickListener { openEditor(null) }
        binding.navAll.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
        }
        binding.navSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.navAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        val loaded = NoteStore.load(this)
        val sorted = if (SettingsPrefs.sortOrder(this) == SettingsPrefs.SORT_CREATED) {
            loaded.sortedByDescending { it.createdAt }
        } else {
            loaded.sortedByDescending { it.updatedAt }
        }
        notes.clear()
        notes.addAll(sorted)
        adapter.submit(notes)
        binding.emptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(noteId: String?) {
        startActivity(
            Intent(this, NoteEditorActivity::class.java)
                .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
        )
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
