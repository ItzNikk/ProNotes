package com.pronotes.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ScrollDirection(val label: String) { VERTICAL("Vertical"), HORIZONTAL("Horizontal") }
enum class ToolbarDock(val label: String) { TOP("Top"), BOTTOM("Bottom"), LEFT("Left"), RIGHT("Right") }
enum class SideButton(val label: String) { ERASER("Eraser"), LASSO("Lasso"), NONE("Nothing") }
enum class PaperTint(val label: String, val argb: Int) {
    WHITE("White", 0xFFFFFFFF.toInt()),
    CREAM("Cream", 0xFFFBF7EC.toInt()),
    GRAY("Gray", 0xFFF1F2F4.toInt()),
    DARK("Dark", 0xFF22252B.toInt()),
}

data class AppSettings(
    val stylusOnly: Boolean = true,
    val sideButton: SideButton = SideButton.ERASER,
    val scribbleToErase: Boolean = true,
    val holdToShape: Boolean = true,
    val circleToLasso: Boolean = true,
    val fingerTapUndo: Boolean = true,
    val scroll: ScrollDirection = ScrollDirection.VERTICAL,
    val pullToAddPage: Boolean = true,
    val paperTint: PaperTint = PaperTint.WHITE,
    val defaultPaper: PaperStyle = PaperStyle.RULED,
    val toolbarDock: ToolbarDock = ToolbarDock.TOP,
    val laserSeconds: Float = 3f,
    val thumbCovers: Boolean = false,
    val keepScreenOn: Boolean = false,
)

/** Small key-value store; every screen observes [settings]. */
class SettingsStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    private inline fun <reified E : Enum<E>> enumOf(key: String, def: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, def.name)!!) }.getOrDefault(def)

    private fun load() = AppSettings(
        stylusOnly = prefs.getBoolean("stylusOnly", true),
        sideButton = enumOf("sideButton", SideButton.ERASER),
        scribbleToErase = prefs.getBoolean("scribble", true),
        holdToShape = prefs.getBoolean("holdShape", true),
        circleToLasso = prefs.getBoolean("circleLasso", true),
        fingerTapUndo = prefs.getBoolean("fingerTapUndo", true),
        scroll = enumOf("scroll", ScrollDirection.VERTICAL),
        pullToAddPage = prefs.getBoolean("pullToAdd", true),
        paperTint = enumOf("paperTint", PaperTint.WHITE),
        defaultPaper = enumOf("defaultPaper", PaperStyle.RULED),
        toolbarDock = enumOf("toolbarDock", ToolbarDock.TOP),
        laserSeconds = prefs.getFloat("laserSeconds", 3f),
        thumbCovers = prefs.getBoolean("thumbCovers", false),
        keepScreenOn = prefs.getBoolean("keepScreenOn", false),
    )

    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_settings.value)
        _settings.value = s
        prefs.edit()
            .putBoolean("stylusOnly", s.stylusOnly)
            .putString("sideButton", s.sideButton.name)
            .putBoolean("scribble", s.scribbleToErase)
            .putBoolean("holdShape", s.holdToShape)
            .putBoolean("circleLasso", s.circleToLasso)
            .putBoolean("fingerTapUndo", s.fingerTapUndo)
            .putString("scroll", s.scroll.name)
            .putBoolean("pullToAdd", s.pullToAddPage)
            .putString("paperTint", s.paperTint.name)
            .putString("defaultPaper", s.defaultPaper.name)
            .putString("toolbarDock", s.toolbarDock.name)
            .putFloat("laserSeconds", s.laserSeconds)
            .putBoolean("thumbCovers", s.thumbCovers)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .apply()
    }

    /** Per-tool colour swatches survive restarts. */
    fun swatches(tool: String, default: List<Int>): List<Int> =
        prefs.getString("swatch_$tool", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == default.size } ?: default

    fun saveSwatches(tool: String, colors: List<Int>) {
        prefs.edit().putString("swatch_$tool", colors.joinToString(",")).apply()
    }

    fun recentColors(): List<Int> =
        prefs.getString("recentColors", "")!!.split(',').mapNotNull { it.toIntOrNull() }

    fun pushRecentColor(c: Int) {
        val list = (listOf(c) + recentColors().filter { it != c }).take(12)
        prefs.edit().putString("recentColors", list.joinToString(",")).apply()
    }

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
