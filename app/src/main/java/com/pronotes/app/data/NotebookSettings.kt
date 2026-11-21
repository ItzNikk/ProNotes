package com.pronotes.app.data

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Per-notebook settings facade backed by [SettingsStore].
 * Keeps notebook editor state synchronized with shared app settings.
 */
class NotebookSettings(
    app: Application,
    @Suppress("UNUSED_PARAMETER") val noteId: String,
    @Suppress("UNUSED_PARAMETER") private val scope: CoroutineScope,
) {
    private val store = SettingsStore.get(app)

    val settings: StateFlow<AppSettings> get() = store.settings

    fun <T> select(selector: (AppSettings) -> T): Flow<T> =
        settings.map(selector).distinctUntilChanged()

    fun update(transform: (AppSettings) -> AppSettings) {
        store.update(transform)
    }
}
