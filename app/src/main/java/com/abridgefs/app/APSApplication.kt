package com.owla19s.aps

import android.app.Application

class APSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuntimeDiagnostics.initialize(this)
        RuntimeDiagnostics.installCrashHandler()
    }
}
