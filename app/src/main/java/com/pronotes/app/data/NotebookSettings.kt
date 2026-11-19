package com.pronotes.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/** A notebook inherits the global editor defaults until its first customization. */
internal class NotebookSettings(context: Context, id: String, scope: CoroutineScope) {
    private val global = SettingsStore.get(context)
    private val local = SettingsStore.notebook(context, id)
    private val customized = MutableStateFlow(local.pref("custom") == "1")
    private fun resolved(base: AppSettings, overrides: AppSettings, active: Boolean) =
        if (active) base.withEditorOptions(overrides) else base

    val settings = combine(global.settings, local.settings, customized, ::resolved)
        .stateIn(scope, SharingStarted.Eagerly, resolved(global.settings.value, local.settings.value, customized.value))

    fun <T> select(mapper: (AppSettings) -> T) = settings.map { mapper(it) }.distinctUntilChanged()

    fun update(change: (AppSettings) -> AppSettings) {
        val updated = change(resolved(global.settings.value, local.settings.value, customized.value))
        local.update { AppSettings().withEditorOptions(updated) }
        local.setPref("custom", "1")
        customized.value = true
    }
}

private fun AppSettings.withEditorOptions(note: AppSettings) = copy(
    toolbarDock = note.toolbarDock,
    toolbarPos = note.toolbarPos,
    toolbarStyle = note.toolbarStyle,
    toolbarTools = note.toolbarTools,
    scroll = note.scroll,
    toolDefaults = note.toolDefaults,
    paperSound = note.paperSound,
    paperVolume = note.paperVolume,
    paperHaptics = note.paperHaptics,
    penName = note.penName,
    highlighterMode = note.highlighterMode,
    highlighterOpacity = note.highlighterOpacity,
    highlighterBehind = note.highlighterBehind,
    eraserMode = note.eraserMode,
    eraseHighlighterOnly = note.eraseHighlighterOnly,
    eraserFilter = note.eraserFilter,
    scribbleSensitivity = note.scribbleSensitivity,
    eraserAutoSwitch = note.eraserAutoSwitch,
    scribbleToErase = note.scribbleToErase,
    liveScribbleErase = note.liveScribbleErase,
    lassoMode = note.lassoMode,
    lassoTouching = note.lassoTouching,
    lassoFilter = note.lassoFilter,
    circleToLasso = note.circleToLasso,
    laserSeconds = note.laserSeconds,
)
