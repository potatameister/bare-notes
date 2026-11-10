package com.potatameister.barenotes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(
            ColorDrawable(
                if (ThemePrefs.resolveDark(this)) Color.BLACK else Color.WHITE
            )
        )
        setContent {
            BareApp(viewModel)
        }
    }
}
