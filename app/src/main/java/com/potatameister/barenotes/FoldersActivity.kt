package com.potatameister.barenotes

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.potatameister.barenotes.databinding.ActivityFoldersBinding
import java.util.UUID

class FoldersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoldersBinding
    private var folders: List<Folder> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.foldersRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }
        binding.addButton.setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        folders = FolderStore.load(this)
        binding.foldersEmpty.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
        binding.foldersHint.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
        binding.folderList.removeAllViews()

        folders.forEach { folder ->
            binding.folderList.addView(createRow(folder))
            binding.folderList.addView(createDivider())
        }
    }

    private fun createRow(folder: Folder): View {
        val textColor = ContextCompat.getColor(this, R.color.text)
        val icon = ContextCompat.getColor(this, R.color.icon)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52)
            isClickable = true
            isFocusable = true
            setBackgroundResource(selectableBackground())
            setOnClickListener { showRenameDialog(folder) }
        }

        val name = TextView(this).apply {
            text = folder.name
            textSize = 16f
            setTextColor(textColor)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val delete = ImageButton(this).apply {
            setImageResource(R.drawable.ic_delete)
            imageTintList = ColorStateList.valueOf(icon)
            contentDescription = getString(R.string.delete_folder)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundResource(borderlessBackground())
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener { confirmDelete(folder) }
        }

        row.addView(name)
        row.addView(delete)
        return row
    }

    private fun createDivider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1)
        )
        setBackgroundColor(ContextCompat.getColor(this@FoldersActivity, R.color.divider))
    }

    private fun showAddDialog() {
        showNameDialog(R.string.new_folder, R.string.create, "") { name ->
            createFolder(name)
        }
    }

    private fun showRenameDialog(folder: Folder) {
        showNameDialog(R.string.rename_folder, R.string.save, folder.name) { name ->
            renameFolder(folder, name)
        }
    }

    private fun showNameDialog(
        titleRes: Int,
        positiveRes: Int,
        initial: String,
        onSave: (String) -> Boolean
    ) {
        val input = folderInput(initial)
        val dialog = AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(input)
            .setPositiveButton(positiveRes, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            input.requestFocus()
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text?.toString()?.trim().orEmpty()
                if (onSave(name)) {
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun createFolder(name: String): Boolean {
        if (name.isBlank()) {
            toast(R.string.folder_name_required)
            return false
        }
        if (folders.any { it.name.equals(name, ignoreCase = true) }) {
            toast(R.string.folder_name_exists)
            return false
        }
        val updated = folders + Folder(UUID.randomUUID().toString(), name)
        FolderStore.save(this, updated)
        refresh()
        return true
    }

    private fun renameFolder(folder: Folder, name: String): Boolean {
        if (name.isBlank()) {
            toast(R.string.folder_name_required)
            return false
        }
        if (folders.any { it.id != folder.id && it.name.equals(name, ignoreCase = true) }) {
            toast(R.string.folder_name_exists)
            return false
        }
        val updated = folders.map { if (it.id == folder.id) it.copy(name = name) else it }
        FolderStore.save(this, updated)
        refresh()
        return true
    }

    private fun confirmDelete(folder: Folder) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_folder_title)
            .setMessage(R.string.delete_folder_message)
            .setPositiveButton(R.string.delete) { _, _ -> deleteFolder(folder) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteFolder(folder: Folder) {
        val notes = NoteStore.load(this).map {
            if (it.folderId == folder.id) it.copy(folderId = "") else it
        }
        NoteStore.save(this, notes)
        FolderStore.save(this, folders.filterNot { it.id == folder.id })
        refresh()
    }

    private fun folderInput(value: String): EditText = EditText(this).apply {
        hint = getString(R.string.folder_name)
        setText(value)
        setSelection(text.length)
        setSingleLine()
        setTextColor(ContextCompat.getColor(this@FoldersActivity, R.color.text))
        setHintTextColor(ContextCompat.getColor(this@FoldersActivity, R.color.text_muted))
    }

    private fun selectableBackground(): Int {
        val out = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
        return out.resourceId
    }

    private fun borderlessBackground(): Int {
        val out = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, out, true)
        return out.resourceId
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
