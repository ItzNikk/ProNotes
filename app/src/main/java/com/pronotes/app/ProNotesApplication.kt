package com.pronotes.app

import android.app.Application
import android.content.ComponentCallbacks2
import com.pronotes.app.data.MemoryTrimRegistry

class ProNotesApplication : Application() {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryTrimRegistry.dispatch(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        @Suppress("DEPRECATION")
        MemoryTrimRegistry.dispatch(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
    }
}
