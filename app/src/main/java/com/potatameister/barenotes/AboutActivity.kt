package com.potatameister.barenotes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.potatameister.barenotes.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private companion object {
        const val SOURCE_URL = "https://github.com/potatameister/bare-notes"
        const val SUPPORT_URL = "https://potatameister.github.io/support"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.aboutRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }
        binding.versionValue.text = BuildConfig.VERSION_NAME
        binding.sourceRow.setOnClickListener { openUrl(SOURCE_URL) }
        binding.supportRow.setOnClickListener { openUrl(SUPPORT_URL) }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }
}
