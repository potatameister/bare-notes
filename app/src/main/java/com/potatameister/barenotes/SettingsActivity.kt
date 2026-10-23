package com.potatameister.barenotes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.potatameister.barenotes.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }
        binding.editorTextRow.setOnClickListener { chooseTextSize() }
        binding.sortRow.setOnClickListener { chooseSort() }

        render()
    }

    private fun render() {
        binding.textSizeValue.setText(labelForSize(SettingsPrefs.textSizeSp(this)))
        binding.sortValue.setText(
            if (SettingsPrefs.sortOrder(this) == SettingsPrefs.SORT_CREATED) {
                R.string.sort_created
            } else {
                R.string.sort_modified
            }
        )
    }

    private fun chooseTextSize() {
        val values = intArrayOf(14, 16, 18, 20)
        val labels = arrayOf<CharSequence>(
            getString(R.string.size_small),
            getString(R.string.size_default),
            getString(R.string.size_large),
            getString(R.string.size_xlarge)
        )
        val checked = values.indexOf(SettingsPrefs.textSizeSp(this)).coerceAtLeast(1)

        AlertDialog.Builder(this)
            .setTitle(R.string.editor_text_size)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                SettingsPrefs.setTextSizeSp(this, values[which])
                render()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun chooseSort() {
        val values = arrayOf(SettingsPrefs.SORT_MODIFIED, SettingsPrefs.SORT_CREATED)
        val labels = arrayOf<CharSequence>(
            getString(R.string.sort_modified),
            getString(R.string.sort_created)
        )
        val checked = values.indexOf(SettingsPrefs.sortOrder(this)).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.sort_notes)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                SettingsPrefs.setSortOrder(this, values[which])
                render()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun labelForSize(sp: Int): Int = when (sp) {
        14 -> R.string.size_small
        18 -> R.string.size_large
        20 -> R.string.size_xlarge
        else -> R.string.size_default
    }
}
