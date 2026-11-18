package com.pronotes.app.data

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ScrollDirection(val label: String) { VERTICAL("Vertical"), HORIZONTAL("Horizontal") }
enum class ToolbarDock(val label: String) {
    HEADER("Top bar"),
    TOP("Floating top"),
    BOTTOM("Floating bottom"),
    LEFT("Floating left"),
    RIGHT("Floating right"),
    FREE("Custom position"),
}
enum class ToolbarSize(val label: String, val scale: Float) { COMPACT("Compact", 0.85f), REGULAR("Regular", 1f), LARGE("Large", 1.18f) }
enum class SideButton(val label: String) { ERASER("Eraser"), LASSO("Lasso"), NONE("Nothing") }
/** How long the app waits after a pen-button click before acting (it waits to see if a second click follows). */
enum class PenClickDelay(val label: String, val ms: Long) { QUICK("Quick", 450), NORMAL("Normal", 700), RELAXED("Relaxed", 1000) }
enum class EraserMode(val label: String) { STROKE("Whole stroke"), PIXEL("Pixel") }
enum class ScribbleSensitivity(val label: String) { GENTLE("Gentle"), BALANCED("Balanced"), INSTANT("Instant") }
enum class HighlighterMode(val label: String) { FREEHAND("Freehand"), AUTO("Auto-straighten"), STRAIGHT("Always straight") }
enum class LassoMode(val label: String) { FREEFORM("Freeform"), RECT("Rectangle") }
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class AppFont(val label: String) { MODERN_SANS("Modern Sans"), EDITORIAL_SERIF("Editorial Serif"), STUDIO_MONO("Studio Mono"), ROUNDED("Soft Rounded") }
enum class MotionStyle(val label: String) { SPRING("Spring"), MINIMAL_CRISP("Crisp"), REDUCED("Reduced") }
enum class PerformanceMode(
    val label: String,
    val autosaveMs: Long,
    val animationScale: Float,
    val thumbWidthPx: Int,
    val pdfPrefetchPages: Int,
) {
    BATTERY_SAVER("Battery saver", autosaveMs = 2400L, animationScale = 0f, thumbWidthPx = 240, pdfPrefetchPages = 1),
    BALANCED("Balanced", autosaveMs = 1200L, animationScale = 1f, thumbWidthPx = 360, pdfPrefetchPages = 3),
    MAXIMUM("Maximum", autosaveMs = 600L, animationScale = 1.1f, thumbWidthPx = 480, pdfPrefetchPages = 5),
}

enum class AccentColor(val label: String, val argb: Long, val argb2: Long) {
    GREEN("Leaf", 0xFF24483F, 0xFF35655A),
    BLUE("Cobalt", 0xFF1A84FF, 0xFF00A3F5),
    PINK("Magenta", 0xFFF01E6B, 0xFFFB7185),
    ORANGE("Amber", 0xFFFF8C00, 0xFFD95926),
    VIOLET("Violet", 0xFF6250D6, 0xFFD55181),
    GRAPHITE("Ink", 0xFF1F2421, 0xFF5A5F58),
}

enum class PaperTint(val label: String, val argb: Int) {
    WHITE("White", 0xFFFFFFFF.toInt()),
    CREAM("Cream", 0xFFFBF7EC.toInt()),
    GRAY("Gray", 0xFFF1F2F4.toInt()),
    DARK("Dark", 0xFF22252B.toInt()),
}

enum class DeskColor(val label: String, val argb: Int) {
    AUTO("Match system", 0),
    MIST("Mist", 0xFFE9ECF1.toInt()),
    SAND("Sand", 0xFFEDE8DE.toInt()),
    SAGE("Sage", 0xFFE1E8E1.toInt()),
    SLATE("Slate", 0xFF2C2F36.toInt()),
}

/** What a pen button click (without touching the screen) does. */
enum class PenAction(val label: String) {
    QUICK_WHEEL("Tool wheel"), UNDO("Undo"), TOGGLE_ERASER("Pen / eraser"), LAST_TOOL("Previous tool"), NONE("Nothing")
}

/** Per-pen settings, keyed by the pen's input device name. */
@Immutable
data class PenProfile(
    val name: String,
    val brand: String,
    val sideButton: SideButton = SideButton.ERASER,
    val click: PenAction = PenAction.QUICK_WHEEL,
    val doubleClick: PenAction = PenAction.UNDO,
    val alias: String = "",
) {
    fun displayName(): String = alias.ifBlank { brand.ifBlank { name } }
}

/** Recognises the pen maker from the Android input device name. */
object PenBrands {
    val manual = listOf("Samsung S Pen", "Wacom", "Lenovo Precision Pen", "Xiaomi Smart Pen", "USI pen", "Microsoft Pen", "Huawei M-Pencil", "OnePlus Stylo", "Other stylus")

    fun detect(deviceName: String?): String? {
        val n = (deviceName ?: return null).lowercase()
        return when {
            "s pen" in n || "spen" in n || "sec_e-pen" in n || "e-pen" in n || "samsung" in n -> "Samsung S Pen"
            "wacom" in n -> "Wacom"
            "lenovo" in n -> "Lenovo Precision Pen"
            "xiaomi" in n || "mi pen" in n || "smart pen" in n -> "Xiaomi Smart Pen"
            "usi" in n -> "USI pen"
            "microsoft" in n || "mpp" in n || "surface" in n -> "Microsoft Pen"
            "huawei" in n || "m-pencil" in n || "honor" in n -> "Huawei M-Pencil"
            "oneplus" in n || "stylo" in n -> "OnePlus Stylo"
            "stylus" in n || "pen" in n || "touch" in n || "digitizer" in n -> "Stylus"
            else -> null
        }
    }
}

/** Which kinds of content the lasso picks up. */
@Immutable
data class LassoFilter(
    val ink: Boolean = true,
    val highlighter: Boolean = true,
    val text: Boolean = true,
    val images: Boolean = true,
    val other: Boolean = true,
)

/** Which kinds of content the eraser removes. */
@Immutable
data class EraserFilter(
    val ink: Boolean = true,
    val highlighter: Boolean = true,
    val shapes: Boolean = true,
    val text: Boolean = false,
    val stickyAndTape: Boolean = false,
    val images: Boolean = false,
)

enum class UiColorScheme(
    val label: String,
    val subtitle: String,
    val seedPrimary: Long,
    val seedSecondary: Long,
    val lightBg: Long,
    val lightSurface: Long,
    val darkBg: Long,
    val darkSurface: Long,
) {
    SLATE_STUDIO(
        label = "Slate Studio",
        subtitle = "Crisp studio dark & warm paper",
        seedPrimary = 0xFF0044EA,
        seedSecondary = 0xFF00A3F5,
        lightBg = 0xFFF3F1EA,
        lightSurface = 0xFFFBF9F4,
        darkBg = 0xFF141519,
        darkSurface = 0xFF1C1C1C,
    ),
    WARM_IVORY(
        label = "Warm Ivory",
        subtitle = "Cozy cream & botanical ink",
        seedPrimary = 0xFF24483F,
        seedSecondary = 0xFF35655A,
        lightBg = 0xFFF6F1E6,
        lightSurface = 0xFFFDFBF6,
        darkBg = 0xFF171614,
        darkSurface = 0xFF211F1C,
    ),
    OCEAN_BREEZE(
        label = "Ocean Breeze",
        subtitle = "Coastal blue & airy slate",
        seedPrimary = 0xFF0284C7,
        seedSecondary = 0xFF38BDF8,
        lightBg = 0xFFEEF6FA,
        lightSurface = 0xFFF8FCFF,
        darkBg = 0xFF0D1722,
        darkSurface = 0xFF152232,
    ),
    FOREST_SAGE(
        label = "Forest Sage",
        subtitle = "Botanical green & parchment",
        seedPrimary = 0xFF15803D,
        seedSecondary = 0xFF34D399,
        lightBg = 0xFFEEF4EF,
        lightSurface = 0xFFF7FBF8,
        darkBg = 0xFF0F1913,
        darkSurface = 0xFF17261D,
    ),
    ROSE_QUARTZ(
        label = "Rose Quartz",
        subtitle = "Soft blush & berry accents",
        seedPrimary = 0xFFE11D48,
        seedSecondary = 0xFFFB7185,
        lightBg = 0xFFFBF1F3,
        lightSurface = 0xFFFFF8F9,
        darkBg = 0xFF1C1115,
        darkSurface = 0xFF28181E,
    ),
    MIDNIGHT_OLED(
        label = "Midnight OLED",
        subtitle = "True black OLED & electric indigo",
        seedPrimary = 0xFF4F46E5,
        seedSecondary = 0xFF818CF8,
        lightBg = 0xFFF1F3F9,
        lightSurface = 0xFFFFFFFF,
        darkBg = 0xFF000000,
        darkSurface = 0xFF0D0E14,
    ),
    NORDIC_SLATE(
        label = "Nordic Slate",
        subtitle = "Cool architectural steel",
        seedPrimary = 0xFF334155,
        seedSecondary = 0xFF64748B,
        lightBg = 0xFFEDF1F5,
        lightSurface = 0xFFF8FAFC,
        darkBg = 0xFF11151C,
        darkSurface = 0xFF1B222C,
    ),
    SOLAR_AMBER(
        label = "Solar Amber",
        subtitle = "Golden honey & warm espresso",
        seedPrimary = 0xFFD97706,
        seedSecondary = 0xFFFBBF24,
        lightBg = 0xFFFBF5E8,
        lightSurface = 0xFFFFFCF5,
        darkBg = 0xFF1B160F,
        darkSurface = 0xFF261F15,
    ),
}

@Immutable
data class AppSettings(
    // stylus & input
    val stylusOnly: Boolean = true,
    val sideButton: SideButton = SideButton.ERASER,
    val palmRejection: Boolean = true,
    val haptics: Boolean = true,
    // gestures
    val scribbleToErase: Boolean = true,
    val liveScribbleErase: Boolean = true,
    val scribbleSensitivity: ScribbleSensitivity = ScribbleSensitivity.BALANCED,
    val holdToShape: Boolean = true,
    val circleToLasso: Boolean = true,
    val fingerTapUndo: Boolean = true,
    val doubleTapZoom: Boolean = true,
    // pages
    val scroll: ScrollDirection = ScrollDirection.VERTICAL,
    val pullToAddPage: Boolean = true,
    val paperTint: PaperTint = PaperTint.WHITE,
    val defaultPaper: PaperStyle = PaperStyle.RULED,
    val deskColor: DeskColor = DeskColor.AUTO,
    val pageNumbers: Boolean = true,
    val keepScreenOn: Boolean = false,
    val darkPdf: Boolean = false,
    // toolbar
    val toolbarDock: ToolbarDock = ToolbarDock.TOP,
    val toolbarPos: String = "0.5,0.03",
    /** Hide the toolbar while the screen is being recorded (Android 15+). */
    val hideToolsWhenRecording: Boolean = false,
    /** Study stopwatch in the editor. */
    val studyTimer: Boolean = true,
    /** Start counting as soon as a notebook opens (off: tap to start). */
    val studyAutoStart: Boolean = true,
    val studySeconds: Boolean = true,
    /** Pause when nothing was written or scrolled for this many minutes (0 = never). */
    val studyIdleMinutes: Int = 0,
    // new notebooks
    val defaultPreset: PagePreset = PagePreset.A4,
    val defaultLandscape: Boolean = false,
    val defaultCoverPage: Boolean = true,
    val toolbarSize: ToolbarSize = ToolbarSize.REGULAR,
    val showTabs: Boolean = true,
    /** Tool order, comma separated enum names. Hidden tools are prefixed with "-". */
    val toolbarTools: String = "",
    // in-app updates
    val autoUpdateCheck: Boolean = true,
    /** Remind about new versions: a notification once a day, and the update sheet on launch. */
    val updateReminders: Boolean = true,
    /** A light tick when the pen touches down and when shapes snap. */
    val penHaptics: Boolean = true,
    /** Paper feel while writing: soft scratch sound and grain-like haptic ticks. */
    val paperSound: Boolean = true,
    val paperHaptics: Boolean = true,
    val paperVolume: Float = 0.4f,
    val bookFlipSound: Boolean = true,
    /** Device-specific performance profile; never synced to the cloud. */
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    /** Corner roundness in dp (8 = soft rectangle, 28 = rounded studio). */
    val cornerRoundnessDp: Float = 20f,
    // pens
    val penName: String = "Pro Stylus",
    val hoverCursor: Boolean = true,
    val penClickDelay: PenClickDelay = PenClickDelay.NORMAL,
    val onboardingDone: Boolean = false,
    val supportReminders: Boolean = true,
    // tools
    val eraserMode: EraserMode = EraserMode.STROKE,
    val eraseHighlighterOnly: Boolean = false,
    val eraserFilter: EraserFilter = EraserFilter(),
    val eraserAutoSwitch: Boolean = false,
    val highlighterBehind: Boolean = true,
    val highlighterMode: HighlighterMode = HighlighterMode.AUTO,
    val laserSeconds: Float = 3f,
    /** Highlighter ink opacity, 0.3 to 0.9. Text stays on top thanks to blending. */
    val highlighterOpacity: Float = 0.65f,
    val lassoAfterShape: Boolean = false,
    val lassoMode: LassoMode = LassoMode.FREEFORM,
    val lassoFilter: LassoFilter = LassoFilter(),
    val lassoTouching: Boolean = false,
    // look & customization
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val uiColorScheme: UiColorScheme = UiColorScheme.SLATE_STUDIO,
    val accent: AccentColor = AccentColor.BLUE,
    val gradientTitles: Boolean = false,
    val appFont: AppFont = AppFont.MODERN_SANS,
    val motionStyle: MotionStyle = MotionStyle.SPRING,
    val showHoverTooltips: Boolean = true,
    // library
    val thumbCovers: Boolean = false,
    val pomodoroMinutes: Int = 25,
    val googleUid: String = "",
    val googleEmail: String = "",
    val googleName: String = "",
    val lastCloudSyncMs: Long = 0L,
)

/** A saved pen: type, colour and thickness, one tap away, with an optional user label. */
@Immutable
data class PenPreset(val type: String, val color: Int, val size: Float, val name: String = "")

/** Small key-value store; every screen observes [settings]. */
class SettingsStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    /** Narrow lifecycle-safe selector stream so composables recompose only when their slice changes. */
    inline fun <T> select(crossinline mapper: (AppSettings) -> T): Flow<T> =
        settings.map { mapper(it) }.distinctUntilChanged()

    private inline fun <reified E : Enum<E>> enumOf(key: String, def: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, def.name) ?: def.name) }.getOrDefault(def)

    private fun uiColorSchemeOf(key: String, def: UiColorScheme): UiColorScheme =
        enumOf(key, def)

    private fun motionStyleOf(key: String, def: MotionStyle): MotionStyle =
        enumOf(key, def)

    private fun b(key: String, def: Boolean) = prefs.getBoolean(key, def)

    private fun load(): AppSettings {
        val d = AppSettings()
        val legacyStraight = prefs.getBoolean("hlStraight", false)
        return AppSettings(
            stylusOnly = b("stylusOnly", d.stylusOnly),
            sideButton = enumOf("sideButton", d.sideButton),
            palmRejection = b("palmRejection", d.palmRejection),
            haptics = b("haptics", d.haptics),
            scribbleToErase = b("scribble", d.scribbleToErase),
            liveScribbleErase = b("liveScribble", d.liveScribbleErase),
            scribbleSensitivity = enumOf("scribbleSens", d.scribbleSensitivity),
            holdToShape = b("holdShape", d.holdToShape),
            circleToLasso = b("circleLasso", d.circleToLasso),
            fingerTapUndo = b("fingerTapUndo", d.fingerTapUndo),
            doubleTapZoom = b("doubleTapZoom", d.doubleTapZoom),
            scroll = enumOf("scroll", d.scroll),
            pullToAddPage = b("pullToAdd", d.pullToAddPage),
            paperTint = enumOf("paperTint", d.paperTint),
            defaultPaper = enumOf("defaultPaper", d.defaultPaper),
            deskColor = enumOf("deskColor2", d.deskColor),
            pageNumbers = b("pageNumbers", d.pageNumbers),
            keepScreenOn = b("keepScreenOn", d.keepScreenOn),
            darkPdf = b("darkPdf", d.darkPdf),
            toolbarDock = enumOf("toolbarDock", d.toolbarDock),
            toolbarPos = prefs.getString("toolbarPos", d.toolbarPos) ?: d.toolbarPos,
            hideToolsWhenRecording = b("hideRec", d.hideToolsWhenRecording),
            studyTimer = b("studyTimer", d.studyTimer),
            studyAutoStart = b("studyAuto", d.studyAutoStart),
            studySeconds = b("studySec", d.studySeconds),
            studyIdleMinutes = prefs.getInt("studyIdle", d.studyIdleMinutes),
            defaultPreset = enumOf("defPreset", d.defaultPreset),
            defaultLandscape = b("defLand", d.defaultLandscape),
            defaultCoverPage = b("defCover", d.defaultCoverPage),
            toolbarSize = enumOf("toolbarSize", d.toolbarSize),
            showTabs = b("showTabs", d.showTabs),
            toolbarTools = prefs.getString("toolbarTools", "") ?: "",
            autoUpdateCheck = b("autoUpdate", d.autoUpdateCheck),
            updateReminders = b("updRemind", d.updateReminders),
            penHaptics = b("penHaptics", d.penHaptics),
            paperSound = b("paperSound", d.paperSound),
            paperHaptics = b("paperHaptics", d.paperHaptics),
            paperVolume = prefs.getFloat("paperVol", d.paperVolume),
            bookFlipSound = b("bookFlipSound", d.bookFlipSound),
            performanceMode = enumOf("performanceMode", d.performanceMode),
            cornerRoundnessDp = prefs.getFloat("cornerRoundnessDp", d.cornerRoundnessDp).coerceIn(8f, 32f),
            penName = prefs.getString("penName", d.penName) ?: d.penName,
            hoverCursor = b("hoverCursor", d.hoverCursor),
            penClickDelay = enumOf("penClickDelay", d.penClickDelay),
            onboardingDone = b("onboardingDone", d.onboardingDone),
            supportReminders = b("supportReminders", d.supportReminders),
            eraserMode = enumOf("eraserMode", d.eraserMode),
            eraseHighlighterOnly = b("eraseHlOnly", d.eraseHighlighterOnly),
            eraserFilter = EraserFilter(
                ink = b("eraseInk", true),
                highlighter = b("eraseHl", true),
                shapes = b("eraseShapes", true),
                text = b("eraseText", false),
                stickyAndTape = b("eraseStickyTape", false),
                images = b("eraseImages", false),
            ),
            eraserAutoSwitch = b("eraserAutoSwitch", d.eraserAutoSwitch),
            highlighterBehind = b("hlBehind", d.highlighterBehind),
            highlighterMode = if (!prefs.contains("hlMode") && legacyStraight) HighlighterMode.STRAIGHT else enumOf("hlMode", d.highlighterMode),
            laserSeconds = prefs.getFloat("laserSeconds", d.laserSeconds),
            highlighterOpacity = prefs.getFloat("hlOpacity2", d.highlighterOpacity),
            lassoAfterShape = b("lassoAfterShape", d.lassoAfterShape),
            lassoMode = enumOf("lassoMode", d.lassoMode),
            lassoFilter = LassoFilter(
                ink = b("lassoInk", true), highlighter = b("lassoHl", true), text = b("lassoText", true),
                images = b("lassoImages", true), other = b("lassoOther", true),
            ),
            lassoTouching = b("lassoTouching", d.lassoTouching),
            themeMode = enumOf("themeMode", d.themeMode),
            uiColorScheme = uiColorSchemeOf("uiColorScheme", d.uiColorScheme),
            accent = enumOf("accent", d.accent),
            gradientTitles = b("gradientTitles", d.gradientTitles),
            appFont = enumOf("appFont", d.appFont),
            motionStyle = motionStyleOf("motionStyle", d.motionStyle),
            showHoverTooltips = b("showHoverTooltips", d.showHoverTooltips),
            thumbCovers = b("thumbCovers", d.thumbCovers),
            pomodoroMinutes = prefs.getInt("pomodoroMinutes", d.pomodoroMinutes),
            googleUid = prefs.getString("googleUid", "") ?: "",
            googleEmail = prefs.getString("googleEmail", "") ?: "",
            googleName = prefs.getString("googleName", "") ?: "",
            lastCloudSyncMs = prefs.getLong("lastCloudSyncMs", 0L),
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
            .putBoolean("liveScribble", s.liveScribbleErase)
            .putString("scribbleSens", s.scribbleSensitivity.name)
            .putBoolean("holdShape", s.holdToShape)
            .putBoolean("circleLasso", s.circleToLasso)
            .putBoolean("fingerTapUndo", s.fingerTapUndo)
            .putBoolean("doubleTapZoom", s.doubleTapZoom)
            .putString("scroll", s.scroll.name)
            .putBoolean("pullToAdd", s.pullToAddPage)
            .putString("paperTint", s.paperTint.name)
            .putString("defaultPaper", s.defaultPaper.name)
            .putString("deskColor2", s.deskColor.name)
            .putBoolean("pageNumbers", s.pageNumbers)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .putBoolean("darkPdf", s.darkPdf)
            .putString("toolbarDock", s.toolbarDock.name)
            .putString("toolbarPos", s.toolbarPos)
            .putBoolean("hideRec", s.hideToolsWhenRecording)
            .putBoolean("studyTimer", s.studyTimer)
            .putBoolean("studyAuto", s.studyAutoStart)
            .putBoolean("studySec", s.studySeconds)
            .putInt("studyIdle", s.studyIdleMinutes)
            .putString("defPreset", s.defaultPreset.name)
            .putBoolean("defLand", s.defaultLandscape)
            .putBoolean("defCover", s.defaultCoverPage)
            .putString("toolbarSize", s.toolbarSize.name)
            .putBoolean("showTabs", s.showTabs)
            .putString("toolbarTools", s.toolbarTools)
            .putBoolean("autoUpdate", s.autoUpdateCheck)
            .putBoolean("updRemind", s.updateReminders)
            .putBoolean("penHaptics", s.penHaptics)
            .putBoolean("paperSound", s.paperSound)
            .putBoolean("paperHaptics", s.paperHaptics)
            .putFloat("paperVol", s.paperVolume)
            .putBoolean("bookFlipSound", s.bookFlipSound)
            .putString("performanceMode", s.performanceMode.name)
            .putFloat("cornerRoundnessDp", s.cornerRoundnessDp.coerceIn(8f, 32f))
            .putString("penName", s.penName)
            .putBoolean("hoverCursor", s.hoverCursor)
            .putString("penClickDelay", s.penClickDelay.name)
            .putBoolean("onboardingDone", s.onboardingDone)
            .putBoolean("supportReminders", s.supportReminders)
            .putString("eraserMode", s.eraserMode.name)
            .putBoolean("eraseHlOnly", s.eraseHighlighterOnly)
            .putBoolean("eraseInk", s.eraserFilter.ink)
            .putBoolean("eraseHl", s.eraserFilter.highlighter)
            .putBoolean("eraseShapes", s.eraserFilter.shapes)
            .putBoolean("eraseText", s.eraserFilter.text)
            .putBoolean("eraseStickyTape", s.eraserFilter.stickyAndTape)
            .putBoolean("eraseImages", s.eraserFilter.images)
            .putBoolean("eraserAutoSwitch", s.eraserAutoSwitch)
            .putBoolean("hlBehind", s.highlighterBehind)
            .putString("hlMode", s.highlighterMode.name)
            .putFloat("laserSeconds", s.laserSeconds)
            .putFloat("hlOpacity2", s.highlighterOpacity)
            .putBoolean("lassoAfterShape", s.lassoAfterShape)
            .putString("lassoMode", s.lassoMode.name)
            .putBoolean("lassoInk", s.lassoFilter.ink)
            .putBoolean("lassoHl", s.lassoFilter.highlighter)
            .putBoolean("lassoText", s.lassoFilter.text)
            .putBoolean("lassoImages", s.lassoFilter.images)
            .putBoolean("lassoOther", s.lassoFilter.other)
            .putBoolean("lassoTouching", s.lassoTouching)
            .putString("themeMode", s.themeMode.name)
            .putString("uiColorScheme", s.uiColorScheme.name)
            .putString("accent", s.accent.name)
            .putBoolean("gradientTitles", s.gradientTitles)
            .putString("appFont", s.appFont.name)
            .putString("motionStyle", s.motionStyle.name)
            .putBoolean("showHoverTooltips", s.showHoverTooltips)
            .putBoolean("thumbCovers", s.thumbCovers)
            .putInt("pomodoroMinutes", s.pomodoroMinutes)
            .putString("googleUid", s.googleUid)
            .putString("googleEmail", s.googleEmail)
            .putString("googleName", s.googleName)
            .putLong("lastCloudSyncMs", s.lastCloudSyncMs)
            .apply()
    }

    /** Small remembered values (last tool, colours, page numbers). */
    fun pref(key: String, def: String = ""): String = prefs.getString("p_$key", def) ?: def
    fun setPref(key: String, value: String) { prefs.edit().putString("p_$key", value).apply() }

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

    // --------------------------------------------------------------- pens
    private val _pens = MutableStateFlow(loadPens())
    /** Every pen used on this device, with its own settings. */
    val pens: StateFlow<List<PenProfile>> = _pens

    private fun esc(s: String): String = s.replace("\\", "\\\\").replace("\t", " ").replace("\n", " ").replace("|", "/").replace(";", ",")

    private fun loadPens(): List<PenProfile> =
        prefs.getString("pens", "")!!.split('\n').mapNotNull { line ->
            val p = line.split('\t')
            if (p.size < 5) null else runCatching {
                PenProfile(
                    name = p[0],
                    brand = p[1],
                    sideButton = SideButton.valueOf(p[2]),
                    click = PenAction.valueOf(p[3]),
                    doubleClick = PenAction.valueOf(p[4]),
                    alias = p.getOrNull(5) ?: "",
                )
            }.getOrNull()
        }

    private fun savePens(list: List<PenProfile>) {
        _pens.value = list
        prefs.edit().putString(
            "pens",
            list.joinToString("\n") {
                "${esc(it.name)}\t${esc(it.brand)}\t${it.sideButton}\t${it.click}\t${it.doubleClick}\t${esc(it.alias)}"
            },
        ).apply()
    }

    fun pen(name: String): PenProfile? = _pens.value.firstOrNull { it.name == name }

    /** Remembers a pen the first time it is seen. */
    fun registerPen(name: String, brand: String? = null, alias: String? = null): PenProfile {
        pen(name)?.let { existing ->
            val nextBrand = brand ?: existing.brand
            val nextAlias = alias ?: existing.alias
            if (nextBrand != existing.brand || nextAlias != existing.alias) {
                updatePen(existing.copy(brand = nextBrand, alias = nextAlias))
            }
            return pen(name)!!
        }
        val p = PenProfile(name, brand ?: PenBrands.detect(name) ?: "Stylus", alias = alias ?: "")
        savePens(_pens.value + p)
        return p
    }

    fun updatePen(p: PenProfile) = savePens(_pens.value.map { if (it.name == p.name) p else it })
    fun forgetPen(name: String) = savePens(_pens.value.filterNot { it.name == name })

    // ------------------------------------------------------------ support
    fun shouldRemindSupport(): Boolean {
        if (!_settings.value.supportReminders) return false
        val launches = prefs.getInt("launches", 0) + 1
        prefs.edit().putInt("launches", launches).apply()
        val last = prefs.getLong("supportShown", 0L)
        val days = (System.currentTimeMillis() - last) / 86_400_000L
        return launches >= 2 && days >= 2 && launches % 4 == 0
    }

    fun markSupportShown() = prefs.edit().putLong("supportShown", System.currentTimeMillis()).apply()

    // ------------------------------------------------------------ pen presets
    private val _presets = MutableStateFlow(loadPresets())
    val presets: StateFlow<List<PenPreset>> = _presets

    private fun loadPresets(): List<PenPreset> =
        prefs.getString("penPresets", "")!!.split(';').mapNotNull { e ->
            val p = e.split('|')
            if (p.size >= 3) runCatching { PenPreset(p[0], p[1].toInt(), p[2].toFloat(), p.getOrNull(3) ?: "") }.getOrNull() else null
        }

    fun addPreset(p: PenPreset) = savePresets((_presets.value.filterNot { it.type == p.type && it.color == p.color && it.size == p.size && it.name == p.name } + p).takeLast(10))
    fun renamePreset(target: PenPreset, newName: String) =
        savePresets(_presets.value.map { if (it == target) it.copy(name = newName.trim()) else it })
    fun removePreset(p: PenPreset) = savePresets(_presets.value - p)
    private fun savePresets(list: List<PenPreset>) {
        _presets.value = list
        prefs.edit().putString("penPresets", list.joinToString(";") { "${it.type}|${it.color}|${it.size}|${esc(it.name)}" }).apply()
    }

    // ------------------------------------------------------------------ tabs
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

    // ------------------------------------------------------------ Reader Mode progress
    fun readerPage(bookId: String): Int = prefs.getInt("reader_page_$bookId", 0).coerceAtLeast(0)

    fun setReaderPage(bookId: String, page: Int) {
        prefs.edit()
            .putInt("reader_page_$bookId", page.coerceAtLeast(0))
            .putString("reader_last_book", bookId)
            .apply()
    }

    fun lastReaderBookId(): String = prefs.getString("reader_last_book", "") ?: ""

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
