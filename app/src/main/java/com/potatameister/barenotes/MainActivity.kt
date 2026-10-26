package com.potatameister.barenotes

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.potatameister.barenotes.databinding.ActivityMainBinding

private sealed interface NoteFilter {
    object All : NoteFilter
    object Unfiled : NoteFilter
    data class InFolder(val id: String) : NoteFilter
}

class MainActivity : AppCompatActivity() {

    private companion object {
        const val STATE_FILTER_TYPE = "filter_type"
        const val STATE_FILTER_ID = "filter_id"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter
    private val notes = mutableListOf<Note>()
    private var folders: List<Folder> = emptyList()
    private var filter: NoteFilter = NoteFilter.All
    private var updatingThemeSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            filter = when (savedInstanceState.getString(STATE_FILTER_TYPE)) {
                "unfiled" -> NoteFilter.Unfiled
                "folder" -> NoteFilter.InFolder(
                    savedInstanceState.getString(STATE_FILTER_ID).orEmpty()
                )
                else -> NoteFilter.All
            }
        }

        applyInsets()

        adapter = NoteAdapter { note -> openEditor(note.id) }
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.adapter = adapter
        binding.notesList.setHasFixedSize(true)

        binding.darkSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingThemeSwitch) return@setOnCheckedChangeListener
            ThemePrefs.save(this, checked)
            ThemePrefs.applyStored(this)
        }
        binding.themeRow.setOnClickListener { binding.darkSwitch.toggle() }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.sidebar)
        }
        binding.addButton.setOnClickListener { openEditor(null) }
        binding.navAll.setOnClickListener {
            filter = NoteFilter.All
            binding.drawerLayout.closeDrawer(binding.sidebar)
            loadAll()
        }
        binding.navUnfiled.setOnClickListener {
            filter = NoteFilter.Unfiled
            binding.drawerLayout.closeDrawer(binding.sidebar)
            loadAll()
        }
        binding.manageFolders.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
            startActivity(Intent(this, FoldersActivity::class.java))
        }
        binding.navSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.navAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebar)
            startActivity(Intent(this, AboutActivity::class.java))
        }

        applyThemeColors()
    }

    override fun onResume() {
        super.onResume()
        loadAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        when (val current = filter) {
            NoteFilter.All -> outState.putString(STATE_FILTER_TYPE, "all")
            NoteFilter.Unfiled -> outState.putString(STATE_FILTER_TYPE, "unfiled")
            is NoteFilter.InFolder -> {
                outState.putString(STATE_FILTER_TYPE, "folder")
                outState.putString(STATE_FILTER_ID, current.id)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeColors()
    }

    private fun loadAll() {
        folders = FolderStore.load(this)
        val knownFolderIds = folders.map { it.id }.toSet()
        val selected = filter
        if (selected is NoteFilter.InFolder && selected.id !in knownFolderIds) {
            filter = NoteFilter.All
        }

        val allNotes = NoteStore.load(this)
        val visible = when (val current = filter) {
            NoteFilter.All -> allNotes
            NoteFilter.Unfiled -> allNotes.filter {
                it.folderId.isEmpty() || it.folderId !in knownFolderIds
            }
            is NoteFilter.InFolder -> allNotes.filter { it.folderId == current.id }
        }
        val sorted = if (SettingsPrefs.sortOrder(this) == SettingsPrefs.SORT_CREATED) {
            visible.sortedByDescending { it.createdAt }
        } else {
            visible.sortedByDescending { it.updatedAt }
        }

        notes.clear()
        notes.addAll(sorted)
        adapter.submit(notes)
        renderFolders()
        updateTitle()
        updateEmptyState()
    }

    private fun updateTitle() {
        binding.screenTitle.text = when (val current = filter) {
            NoteFilter.All -> getString(R.string.app_name)
            NoteFilter.Unfiled -> getString(R.string.unfiled)
            is NoteFilter.InFolder -> folders.firstOrNull { it.id == current.id }?.name
                ?: getString(R.string.app_name)
        }
    }

    private fun renderFolders() {
        val selectedId = (filter as? NoteFilter.InFolder)?.id
        binding.navAll.setTypeface(
            null,
            if (filter === NoteFilter.All) Typeface.BOLD else Typeface.NORMAL
        )
        binding.navUnfiled.setTypeface(
            null,
            if (filter === NoteFilter.Unfiled) Typeface.BOLD else Typeface.NORMAL
        )

        binding.folderList.removeAllViews()
        folders.forEach { folder ->
            val row = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = folder.name
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
                setPadding(0, dp(14), 0, dp(14))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setBackgroundResource(selectableBackground())
                isClickable = true
                isFocusable = true
                setTypeface(
                    null,
                    if (folder.id == selectedId) Typeface.BOLD else Typeface.NORMAL
                )
                setOnClickListener {
                    filter = NoteFilter.InFolder(folder.id)
                    binding.drawerLayout.closeDrawer(binding.sidebar)
                    loadAll()
                }
            }
            binding.folderList.addView(row)
        }
    }

    private fun updateEmptyState() {
        if (notes.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            when (filter) {
                NoteFilter.All -> {
                    binding.emptyTitle.setText(R.string.no_notes)
                    binding.emptyHint.setText(R.string.no_notes_hint)
                }
                NoteFilter.Unfiled -> {
                    binding.emptyTitle.setText(R.string.no_unfiled_notes)
                    binding.emptyHint.setText(R.string.no_notes_hint)
                }
                is NoteFilter.InFolder -> {
                    binding.emptyTitle.setText(R.string.no_folder_notes)
                    binding.emptyHint.setText(R.string.no_notes_hint)
                }
            }
        } else {
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun openEditor(noteId: String?) {
        val intent = Intent(this, NoteEditorActivity::class.java)
            .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
        if (noteId == null) {
            val folderId = (filter as? NoteFilter.InFolder)?.id.orEmpty()
            intent.putExtra(NoteEditorActivity.EXTRA_FOLDER_ID, folderId)
        }
        startActivity(intent)
    }

    private fun applyThemeColors() {
        val dark = ThemePrefs.isDark(this)
        val bg = ContextCompat.getColor(this, R.color.bg)
        val surface = ContextCompat.getColor(this, R.color.surface)
        val text = ContextCompat.getColor(this, R.color.text)
        val muted = ContextCompat.getColor(this, R.color.text_muted)
        val divider = ContextCompat.getColor(this, R.color.divider)
        val icon = ContextCompat.getColor(this, R.color.icon)

        window.setBackgroundDrawable(ColorDrawable(bg))
        binding.drawerLayout.setBackgroundColor(bg)
        binding.sidebar.setBackgroundColor(surface)

        binding.screenTitle.setTextColor(text)
        binding.emptyTitle.setTextColor(text)
        binding.emptyHint.setTextColor(muted)

        binding.sidebarWordmark.setTextColor(text)
        binding.sidebarLabel.setTextColor(muted)
        binding.sidebarDivider.setBackgroundColor(divider)
        binding.navAll.setTextColor(text)
        binding.navUnfiled.setTextColor(text)
        binding.foldersLabel.setTextColor(muted)
        binding.manageFolders.setTextColor(muted)
        binding.sidebarNavDivider.setBackgroundColor(divider)
        binding.themeLabel.setTextColor(text)
        binding.navSettings.setTextColor(text)
        binding.navAbout.setTextColor(text)
        binding.sidebarFooter.setTextColor(muted)

        val iconTint = ColorStateList.valueOf(icon)
        binding.menuButton.imageTintList = iconTint
        binding.addButton.imageTintList = iconTint

        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        binding.darkSwitch.thumbTintList = ColorStateList(states, intArrayOf(text, muted))
        binding.darkSwitch.trackTintList = ColorStateList(states, intArrayOf(divider, divider))

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }

        updatingThemeSwitch = true
        binding.darkSwitch.isChecked = dark
        updatingThemeSwitch = false

        renderFolders()
        adapter.notifyDataSetChanged()
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun selectableBackground(): Int {
        val out = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
        return out.resourceId
    }
}
