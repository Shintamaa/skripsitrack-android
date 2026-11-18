package com.skripsitrack.app

import android.app.Application
import com.skripsitrack.app.data.local.SessionManager

class SkripsiApp : Application() {
    companion object {
        lateinit var session: SessionManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(applicationContext)
    }
}