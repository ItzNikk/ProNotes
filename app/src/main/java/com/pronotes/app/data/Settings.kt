package com.pronotes.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ScrollDirection(val label: String) { VERTICAL("Vertical"), HORIZONTAL("Horizontal") }
enum class ToolbarDock(val label: String) { HEADER("In top bar"), TOP("Floating top"), BOTTOM("Floating bottom"), LEFT("Floating left"), RIGHT("Floating right") }
enum class ToolbarSize(val label: String, val scale: Float) { COMPACT("Compact", 0.85f), REGULAR("Regular", 1f), LARGE("Large", 1.18f) }
enum class SideButton(val label: String) { ERASER("Eraser"), LASSO("Lasso"), NONE("Nothing") }
enum class EraserMode(val label: String) { STROKE("Whole stroke"), PIXEL("Pixel") }
enum class HighlighterMode(val label: String) { FREEHAND("Freehand"), AUTO("Auto-straighten"), STRAIGHT("Always straight") }
enum class LassoMode(val label: String) { FREEFORM("Freeform"), RECT("Rectangle") }
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class AccentColor(val label: String, val argb: Long, val argb2: Long) {
    BLUE("Blue", 0xFF2A78D6, 0xFF7161E0),
    VIOLET("Violet", 0xFF6250D6, 0xFFD55181),
    GREEN("Green", 0xFF0F7E5C, 0xFF3987E5),
    ORANGE("Orange", 0xFFD95926, 0xFFE34948),
    PINK("Pink", 0xFFC04873, 0xFF8173E3),
    GRAPHITE("Graphite", 0xFF383835, 0xFF7B7974),
}
enum class PaperTint(val label: String, val argb: Int) {
    WHITE("White", 0xFFFFFFFF.toInt()),
    CREAM("Cream", 0xFFFBF7EC.toInt()),
    GRAY("Gray", 0xFFF1F2F4.toInt()),
    DARK("Dark", 0xFF22252B.toInt()),
}
enum class DeskColor(val label: String, val argb: Int) {
    MIST("Mist", 0xFFE9ECF1.toInt()),
    SAND("Sand", 0xFFEDE8DE.toInt()),
    SAGE("Sage", 0xFFE1E8E1.toInt()),
    SLATE("Slate", 0xFF2C2F36.toInt()),
}

/** Which kinds of content the lasso picks up. */
data class LassoFilter(
    val ink: Boolean = true,
    val highlighter: Boolean = true,
    val text: Boolean = true,
    val images: Boolean = true,
    val other: Boolean = true,
)

data class AppSettings(
    // stylus & input
    val stylusOnly: Boolean = true,
    val sideButton: SideButton = SideButton.ERASER,
    val palmRejection: Boolean = true,
    val haptics: Boolean = true,
    // gestures
    val scribbleToErase: Boolean = true,
    val holdToShape: Boolean = true,
    val circleToLasso: Boolean = true,
    val fingerTapUndo: Boolean = true,
    val doubleTapZoom: Boolean = true,
    // pages
    val scroll: ScrollDirection = ScrollDirection.VERTICAL,
    val pullToAddPage: Boolean = true,
    val paperTint: PaperTint = PaperTint.WHITE,
    val defaultPaper: PaperStyle = PaperStyle.RULED,
    val deskColor: DeskColor = DeskColor.MIST,
    val pageNumbers: Boolean = true,
    val keepScreenOn: Boolean = false,
    val darkPdf: Boolean = false,
    // toolbar
    val toolbarDock: ToolbarDock = ToolbarDock.TOP,
    val toolbarSize: ToolbarSize = ToolbarSize.REGULAR,
    val showTabs: Boolean = true,
    /** Tool order, comma separated enum names. Hidden tools are prefixed with "-". */
    val toolbarTools: String = "",
    // tools
    val eraserMode: EraserMode = EraserMode.STROKE,
    val eraseHighlighterOnly: Boolean = false,
    val eraserAutoSwitch: Boolean = false,
    val highlighterBehind: Boolean = true,
    val highlighterMode: HighlighterMode = HighlighterMode.AUTO,
    val laserSeconds: Float = 3f,
    val lassoAfterShape: Boolean = false,
    val lassoMode: LassoMode = LassoMode.FREEFORM,
    val lassoFilter: LassoFilter = LassoFilter(),
    val lassoTouching: Boolean = false,
    // look
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.BLUE,
    val gradientTitles: Boolean = true,
    // library
    val thumbCovers: Boolean = false,
)

/** A saved pen: type, colour and thickness, one tap away. */
data class PenPreset(val type: String, val color: Int, val size: Float)

/** Small key-value store; every screen observes [settings]. */
class SettingsStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    private inline fun <reified E : Enum<E>> enumOf(key: String, def: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, def.name)!!) }.getOrDefault(def)

    private fun b(key: String, def: Boolean) = prefs.getBoolean(key, def)

    private fun load(): AppSettings {
        val d = AppSettings()
        // v2.1 had a "straight" switch; carry it over.
        val legacyStraight = prefs.getBoolean("hlStraight", false)
        return AppSettings(
            stylusOnly = b("stylusOnly", d.stylusOnly),
            sideButton = enumOf("sideButton", d.sideButton),
            palmRejection = b("palmRejection", d.palmRejection),
            haptics = b("haptics", d.haptics),
            scribbleToErase = b("scribble", d.scribbleToErase),
            holdToShape = b("holdShape", d.holdToShape),
            circleToLasso = b("circleLasso", d.circleToLasso),
            fingerTapUndo = b("fingerTapUndo", d.fingerTapUndo),
            doubleTapZoom = b("doubleTapZoom", d.doubleTapZoom),
            scroll = enumOf("scroll", d.scroll),
            pullToAddPage = b("pullToAdd", d.pullToAddPage),
            paperTint = enumOf("paperTint", d.paperTint),
            defaultPaper = enumOf("defaultPaper", d.defaultPaper),
            deskColor = enumOf("deskColor", d.deskColor),
            pageNumbers = b("pageNumbers", d.pageNumbers),
            keepScreenOn = b("keepScreenOn", d.keepScreenOn),
            darkPdf = b("darkPdf", d.darkPdf),
            toolbarDock = enumOf("toolbarDock", d.toolbarDock),
            toolbarSize = enumOf("toolbarSize", d.toolbarSize),
            showTabs = b("showTabs", d.showTabs),
            toolbarTools = prefs.getString("toolbarTools", "") ?: "",
            eraserMode = enumOf("eraserMode", d.eraserMode),
            eraseHighlighterOnly = b("eraseHlOnly", d.eraseHighlighterOnly),
            eraserAutoSwitch = b("eraserAutoSwitch", d.eraserAutoSwitch),
            highlighterBehind = b("hlBehind", d.highlighterBehind),
            highlighterMode = if (!prefs.contains("hlMode") && legacyStraight) HighlighterMode.STRAIGHT else enumOf("hlMode", d.highlighterMode),
            laserSeconds = prefs.getFloat("laserSeconds", d.laserSeconds),
            lassoAfterShape = b("lassoAfterShape", d.lassoAfterShape),
            lassoMode = enumOf("lassoMode", d.lassoMode),
            lassoFilter = LassoFilter(
                ink = b("lassoInk", true), highlighter = b("lassoHl", true), text = b("lassoText", true),
                images = b("lassoImages", true), other = b("lassoOther", true),
            ),
            lassoTouching = b("lassoTouching", d.lassoTouching),
            themeMode = enumOf("themeMode", d.themeMode),
            accent = enumOf("accent", d.accent),
            gradientTitles = b("gradientTitles", d.gradientTitles),
            thumbCovers = b("thumbCovers", d.thumbCovers),
        )
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_settings.value)
        _settings.value = s
        prefs.edit()
            .putBoolean("stylusOnly", s.stylusOnly)
            .putString("sideButton", s.sideButton.name)
            .putBoolean("palmRejection", s.palmRejection)
            .putBoolean("haptics", s.haptics)
            .putBoolean("scribble", s.scribbleToErase)
            .putBoolean("holdShape", s.holdToShape)
            .putBoolean("circleLasso", s.circleToLasso)
            .putBoolean("fingerTapUndo", s.fingerTapUndo)
            .putBoolean("doubleTapZoom", s.doubleTapZoom)
            .putString("scroll", s.scroll.name)
            .putBoolean("pullToAdd", s.pullToAddPage)
            .putString("paperTint", s.paperTint.name)
            .putString("defaultPaper", s.defaultPaper.name)
            .putString("deskColor", s.deskColor.name)
            .putBoolean("pageNumbers", s.pageNumbers)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .putBoolean("darkPdf", s.darkPdf)
            .putString("toolbarDock", s.toolbarDock.name)
            .putString("toolbarSize", s.toolbarSize.name)
            .putBoolean("showTabs", s.showTabs)
            .putString("toolbarTools", s.toolbarTools)
            .putString("eraserMode", s.eraserMode.name)
            .putBoolean("eraseHlOnly", s.eraseHighlighterOnly)
            .putBoolean("eraserAutoSwitch", s.eraserAutoSwitch)
            .putBoolean("hlBehind", s.highlighterBehind)
            .putString("hlMode", s.highlighterMode.name)
            .putFloat("laserSeconds", s.laserSeconds)
            .putBoolean("lassoAfterShape", s.lassoAfterShape)
            .putString("lassoMode", s.lassoMode.name)
            .putBoolean("lassoInk", s.lassoFilter.ink)
            .putBoolean("lassoHl", s.lassoFilter.highlighter)
            .putBoolean("lassoText", s.lassoFilter.text)
            .putBoolean("lassoImages", s.lassoFilter.images)
            .putBoolean("lassoOther", s.lassoFilter.other)
            .putBoolean("lassoTouching", s.lassoTouching)
            .putString("themeMode", s.themeMode.name)
            .putString("accent", s.accent.name)
            .putBoolean("gradientTitles", s.gradientTitles)
            .putBoolean("thumbCovers", s.thumbCovers)
            .apply()
    }

    /** Per-tool colour swatches survive restarts. Up to 8 per tool. */
    fun swatches(tool: String, default: List<Int>): List<Int> =
        prefs.getString("swatch_$tool", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() } ?: default

    fun saveSwatches(tool: String, colors: List<Int>) {
        prefs.edit().putString("swatch_$tool", colors.joinToString(",")).apply()
    }

    fun recentColors(): List<Int> =
        prefs.getString("recentColors", "")!!.split(',').mapNotNull { it.toIntOrNull() }

    fun pushRecentColor(c: Int) {
        val list = (listOf(c) + recentColors().filter { it != c }).take(12)
        prefs.edit().putString("recentColors", list.joinToString(",")).apply()
    }

    // ------------------------------------------------------------ pen presets
    private val _presets = MutableStateFlow(loadPresets())
    val presets: StateFlow<List<PenPreset>> = _presets

    private fun loadPresets(): List<PenPreset> =
        prefs.getString("penPresets", "")!!.split(';').mapNotNull { e ->
            val p = e.split('|')
            if (p.size == 3) runCatching { PenPreset(p[0], p[1].toInt(), p[2].toFloat()) }.getOrNull() else null
        }

    fun addPreset(p: PenPreset) = savePresets((_presets.value.filterNot { it == p } + p).takeLast(8))
    fun removePreset(p: PenPreset) = savePresets(_presets.value - p)
    private fun savePresets(list: List<PenPreset>) {
        _presets.value = list
        prefs.edit().putString("penPresets", list.joinToString(";") { "${it.type}|${it.color}|${it.size}" }).apply()
    }

    // ------------------------------------------------------------------ tabs
    /** Open notebook tabs, most recent last. */
    private val _tabs = MutableStateFlow(prefs.getString("tabs", "")!!.split(',').filter { it.isNotBlank() })
    val tabs: StateFlow<List<String>> = _tabs

    fun openTab(id: String) {
        if (id in _tabs.value) return
        setTabs((_tabs.value + id).takeLast(8))
    }

    fun closeTab(id: String) = setTabs(_tabs.value - id)

    fun retainTabs(valid: Set<String>) { if (_tabs.value.any { it !in valid }) setTabs(_tabs.value.filter { it in valid }) }

    private fun setTabs(list: List<String>) {
        _tabs.value = list
        prefs.edit().putString("tabs", list.joinToString(",")).apply()
    }

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
