package com.potatameister.barenotes

import android.app.Application

class BareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePrefs.applyStored(this)
    }
}
