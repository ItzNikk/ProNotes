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
    TOP("Top edge"),
    BOTTOM("Bottom edge"),
    LEFT("Left edge"),
    RIGHT("Right edge"),
    FREE("Floating"),
}
enum class ToolbarStyle(val label: String) {
    FIXED_BAR("Fixed bar"),
    FLOATING_PILL("Floating pill"),
    COMPACT("Compact"),
}
enum class ToolbarSize(val label: String, val scale: Float) { COMPACT("Compact", 0.85f), REGULAR("Regular", 1f), LARGE("Large", 1.18f) }

/** Persisted per-tool defaults (size, opacity, colour behaviour, aspect lock). */
@Immutable
data class ToolDefaults(
    val handwritingLanguage: String = "en-US",
    val autoScript: String = "CURSIVE",
    val customHandwritingFontPath: String = "",
    val autoHandwriting: Boolean = false,
    val alignHandwriting: Boolean = true,
    val alignGuideLine: Boolean = true,
    val dynamicBoldHandwriting: Boolean = true,
    val autoSpellCheck: Boolean = true,
    val pressureEnabled: Boolean = true,
    val pressureSensitivity: Float = 1.0f,
    val penStabilization: Float = 0.25f,
    val penTaper: Float = 0.35f,
    val penTiltShading: Boolean = true,
    val penNibRoundness: Float = 1.0f,
    val penNibAngleDeg: Float = 35f,
    val penStrokeStyle: String = "SOLID",
    val crayonGrain: Float = 0.65f,
    val crayonOpacity: Float = 0.88f,
    val tapSelect: Boolean = false,
    val penOpacity: Float = 1f,
    val shapeOpacity: Float = 1f,
    val textOpacity: Float = 1f,
    val stickerScale: Float = 1f,
    val stickerOpacity: Float = 1f,
    val stickerLockAspect: Boolean = true,
    val imageWidthRatio: Float = 0.6f,
    val imageOpacity: Float = 1f,
    val imageLockAspect: Boolean = true,
    val imageBorder: Boolean = false,
)

/** Unified model of the editor toolbar state persisted through [SettingsStore]. */
@Immutable
data class ToolbarState(
    val dockEdge: ToolbarDock = ToolbarDock.TOP,
    val style: ToolbarStyle = ToolbarStyle.FIXED_BAR,
    val floatingPosX: Float = 0.5f,
    val floatingPosY: Float = 0.04f,
    val size: ToolbarSize = ToolbarSize.REGULAR,
    val toolOrderSpec: String = "",
    val perToolDefaults: ToolDefaults = ToolDefaults(),
)

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
    QUICK_WHEEL("Quick Actions"), UNDO("Undo"), TOGGLE_ERASER("Pen / eraser"), LAST_TOOL("Previous tool"), NONE("Nothing")
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
    COLORFUL(
        label = "Colorful",
        subtitle = "Violet, teal, coral and indigo",
        seedPrimary = 0xFF6134C4,
        seedSecondary = 0xFF007C79,
        lightBg = 0xFFF3EDFF,
        lightSurface = 0xFFFFFAFF,
        darkBg = 0xFF16132B,
        darkSurface = 0xFF242045,
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
    val touchSensitivity: Float = 1.0f,
    val haptics: Boolean = true,
    // gestures
    val scribbleToErase: Boolean = true,
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
    val toolbarStyle: ToolbarStyle = ToolbarStyle.FIXED_BAR,
    val toolbarPos: String = "0.5,0.03",
    val toolDefaults: ToolDefaults = ToolDefaults(),
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
    val customHoverIconPath: String = "",
    val hoverIconSizeDp: Float = 24f,
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
    val gradientTitles: Boolean = true,
    val colorfulUiText: Boolean = true,
    val appFont: AppFont = AppFont.MODERN_SANS,
    val motionStyle: MotionStyle = MotionStyle.SPRING,
    val liveBlur: Boolean = true,
    val topScrollBlur: Boolean = true,
    val classicAppearance: Boolean = false,
    val reduceEffects: Boolean = false,
    val showHoverTooltips: Boolean = true,
    // library
    val thumbCovers: Boolean = false,
    val pomodoroMinutes: Int = 25,
    val googleUid: String = "",
    val googleEmail: String = "",
    val googleName: String = "",
    val lastCloudSyncMs: Long = 0L,
    /** Optional Android SAF tree URI where notes are mirrored/saved inside user storage. */
    val storageFolderUri: String = "",
    val storageFolderLabel: String = "",
    /** Preferred app display language BCP-47 tag (empty string = match Android system locale). */
    val appLanguage: String = "",
    /** User-customized sticky note presets persisted as JSON. */
    val customStickyPresetsJson: String = "",
) {
    fun toolbarState(): ToolbarState {
        val parts = toolbarPos.split(',')
        val fx = parts.getOrNull(0)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
        val fy = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.04f
        return ToolbarState(
            dockEdge = toolbarDock,
            style = toolbarStyle,
            floatingPosX = fx,
            floatingPosY = fy,
            size = toolbarSize,
            toolOrderSpec = toolbarTools,
            perToolDefaults = toolDefaults,
        )
    }

    fun withToolbarState(state: ToolbarState): AppSettings = copy(
        toolbarDock = state.dockEdge,
        toolbarStyle = state.style,
        toolbarPos = String.format(java.util.Locale.US, "%.3f,%.3f", state.floatingPosX.coerceIn(0f, 1f), state.floatingPosY.coerceIn(0f, 1f)),
        toolbarSize = state.size,
        toolbarTools = state.toolOrderSpec,
        toolDefaults = state.perToolDefaults,
    )
}

/** A saved pen: type, colour and thickness, one tap away, with an optional user label. */
@Immutable
data class PenPreset(val type: String, val color: Int, val size: Float, val name: String = "")

/** User-created or built-in sticky note style preset with Notion-like customization. */
@Immutable
data class StickyPreset(
    val id: String,
    val name: String,
    val color: Int,
    val style: String = "classic",
    val paperStyle: String = "plain",
    val font: String = "SANS",
    val fontSize: Float = 28f,
    val cornerRadius: Float = 18f,
    val pinned: Boolean = false,
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            StickyPreset("p_yellow", "Classic Yellow", 0xFFFFF176.toInt(), "classic", "plain", "SANS", 28f, 18f),
            StickyPreset("p_lined", "Study Lined", 0xFFFFE082.toInt(), "lined", "lined", "SANS", 26f, 18f),
            StickyPreset("p_grid", "Grid Checklist", 0xFFC8E6C9.toInt(), "grid", "grid", "MONO", 24f, 18f),
            StickyPreset("p_rose", "Rose Memo", 0xFFFFCDD2.toInt(), "tinted", "plain", "SERIF", 28f, 20f),
            StickyPreset("p_lavender", "Lavender Idea", 0xFFE1BEE7.toInt(), "classic", "plain", "HAND", 30f, 22f),
            StickyPreset("p_ocean", "Ocean Action", 0xFFB3E5FC.toInt(), "outline", "dotted", "SANS", 26f, 18f),
        )
    }
}

/** Small key-value store; every screen observes [settings]. */
class SettingsStore private constructor(context: Context, file: String = "settings") {
    private val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)
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
            stylusOnly = !b("fingerDrawingOptIn", false),
            sideButton = enumOf("sideButton", d.sideButton),
            palmRejection = b("palmRejection", d.palmRejection),
            touchSensitivity = prefs.getFloat("touchSensitivity", d.touchSensitivity).coerceIn(0.4f, 2.0f),
            haptics = b("haptics", d.haptics),
            scribbleToErase = b("scribble", d.scribbleToErase),
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
            toolbarDock = prefs.getString("toolbarDock", d.toolbarDock.name).let { raw ->
                if (raw == "HEADER") ToolbarDock.TOP
                else runCatching { ToolbarDock.valueOf(raw ?: d.toolbarDock.name) }.getOrDefault(d.toolbarDock)
            },
            toolbarStyle = enumOf("toolbarStyle", d.toolbarStyle),
            toolbarPos = prefs.getString("toolbarPos", d.toolbarPos) ?: d.toolbarPos,
            toolDefaults = ToolDefaults(
                handwritingLanguage = prefs.getString("handwritingLanguage", "en-US") ?: "en-US",
                autoScript = prefs.getString("autoScript", null).orEmpty().ifBlank { d.toolDefaults.autoScript },
                customHandwritingFontPath = prefs.getString("customHwFont", "") ?: "",
                autoHandwriting = b("autoHandwriting", prefs.getString("autoScript", null).orEmpty().isNotBlank()),
                alignHandwriting = b("alignHandwriting", true),
                alignGuideLine = b("alignGuideLine", d.toolDefaults.alignGuideLine),
                dynamicBoldHandwriting = b("dynamicBoldHw", d.toolDefaults.dynamicBoldHandwriting),
                autoSpellCheck = b("autoSpellCheck", d.toolDefaults.autoSpellCheck),
                pressureEnabled = b("pressureEnabled", true),
                pressureSensitivity = prefs.getFloat("tdPressSens", d.toolDefaults.pressureSensitivity).coerceIn(0.3f, 2.0f),
                penStabilization = prefs.getFloat("tdPenStab", d.toolDefaults.penStabilization).coerceIn(0f, 1f),
                penTaper = prefs.getFloat("tdPenTaper", d.toolDefaults.penTaper).coerceIn(0f, 1f),
                penTiltShading = b("tdPenTilt", d.toolDefaults.penTiltShading),
                penNibRoundness = prefs.getFloat("tdPenRound", d.toolDefaults.penNibRoundness).coerceIn(0.25f, 1f),
                penNibAngleDeg = prefs.getFloat("tdPenAngle", d.toolDefaults.penNibAngleDeg).coerceIn(0f, 180f),
                penStrokeStyle = prefs.getString("tdPenStyle", d.toolDefaults.penStrokeStyle) ?: d.toolDefaults.penStrokeStyle,
                crayonGrain = prefs.getFloat("tdCrayonGrain", d.toolDefaults.crayonGrain).coerceIn(0.15f, 1f),
                crayonOpacity = prefs.getFloat("tdCrayonOp", d.toolDefaults.crayonOpacity).coerceIn(0.35f, 1f),
                tapSelect = b("tapSelect", false),
                penOpacity = prefs.getFloat("tdPenOp", d.toolDefaults.penOpacity).coerceIn(0.15f, 1f),
                shapeOpacity = prefs.getFloat("tdShapeOp", d.toolDefaults.shapeOpacity).coerceIn(0.15f, 1f),
                textOpacity = prefs.getFloat("tdTextOp", d.toolDefaults.textOpacity).coerceIn(0.2f, 1f),
                stickerScale = prefs.getFloat("tdStickerScale", d.toolDefaults.stickerScale).coerceIn(0.4f, 2f),
                stickerOpacity = prefs.getFloat("tdStickerOp", d.toolDefaults.stickerOpacity).coerceIn(0.2f, 1f),
                stickerLockAspect = b("tdStickerAspect", d.toolDefaults.stickerLockAspect),
                imageWidthRatio = prefs.getFloat("tdImgRatio", d.toolDefaults.imageWidthRatio).coerceIn(0.2f, 0.95f),
                imageOpacity = prefs.getFloat("tdImgOp", d.toolDefaults.imageOpacity).coerceIn(0.2f, 1f),
                imageLockAspect = b("tdImgAspect", d.toolDefaults.imageLockAspect),
                imageBorder = b("tdImgBorder", d.toolDefaults.imageBorder),
            ),
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
            customHoverIconPath = prefs.getString("customHoverIconPath", "") ?: "",
            hoverIconSizeDp = prefs.getFloat("hoverIconSizeDp", d.hoverIconSizeDp).coerceIn(12f, 48f),
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
            colorfulUiText = b("colorfulUiText", d.colorfulUiText),
            appFont = enumOf("appFont", d.appFont),
            motionStyle = motionStyleOf("motionStyle", d.motionStyle),
            liveBlur = b("liveBlur", true),
            topScrollBlur = b("topScrollBlur", d.topScrollBlur),
            classicAppearance = b("classicAppearance", d.classicAppearance),
            reduceEffects = b("reduceEffects", d.reduceEffects),
            showHoverTooltips = b("showHoverTooltips", d.showHoverTooltips),
            thumbCovers = b("thumbCovers", d.thumbCovers),
            pomodoroMinutes = prefs.getInt("pomodoroMinutes", d.pomodoroMinutes),
            googleUid = prefs.getString("googleUid", "") ?: "",
            googleEmail = prefs.getString("googleEmail", "") ?: "",
            googleName = prefs.getString("googleName", "") ?: "",
            lastCloudSyncMs = prefs.getLong("lastCloudSyncMs", 0L),
            storageFolderUri = prefs.getString("storageFolderUri", "") ?: "",
            storageFolderLabel = prefs.getString("storageFolderLabel", "") ?: "",
            appLanguage = prefs.getString("appLanguage", "") ?: "",
            customStickyPresetsJson = prefs.getString("customStickyPresetsJson", "") ?: "",
        )
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_settings.value)
        _settings.value = s
        prefs.edit()
            .putBoolean("fingerDrawingOptIn", !s.stylusOnly)
            .putString("sideButton", s.sideButton.name)
            .putBoolean("palmRejection", s.palmRejection)
            .putFloat("touchSensitivity", s.touchSensitivity.coerceIn(0.4f, 2.0f))
            .putBoolean("haptics", s.haptics)
            .putBoolean("scribble", s.scribbleToErase)
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
            .putString("toolbarStyle", s.toolbarStyle.name)
            .putString("toolbarPos", s.toolbarPos)
            .putBoolean("autoHandwriting", s.toolDefaults.autoHandwriting)
            .putBoolean("alignHandwriting", s.toolDefaults.alignHandwriting)
            .putBoolean("alignGuideLine", s.toolDefaults.alignGuideLine)
            .putBoolean("dynamicBoldHw", s.toolDefaults.dynamicBoldHandwriting)
            .putBoolean("autoSpellCheck", s.toolDefaults.autoSpellCheck)
            .putString("handwritingLanguage", s.toolDefaults.handwritingLanguage)
            .putString("autoScript", s.toolDefaults.autoScript)
            .putString("customHwFont", s.toolDefaults.customHandwritingFontPath)
            .putBoolean("pressureEnabled", s.toolDefaults.pressureEnabled)
            .putFloat("tdPressSens", s.toolDefaults.pressureSensitivity.coerceIn(0.3f, 2.0f))
            .putFloat("tdPenStab", s.toolDefaults.penStabilization.coerceIn(0f, 1f))
            .putFloat("tdPenTaper", s.toolDefaults.penTaper.coerceIn(0f, 1f))
            .putBoolean("tdPenTilt", s.toolDefaults.penTiltShading)
            .putFloat("tdPenRound", s.toolDefaults.penNibRoundness.coerceIn(0.25f, 1f))
            .putFloat("tdPenAngle", s.toolDefaults.penNibAngleDeg.coerceIn(0f, 180f))
            .putString("tdPenStyle", s.toolDefaults.penStrokeStyle)
            .putFloat("tdCrayonGrain", s.toolDefaults.crayonGrain.coerceIn(0.15f, 1f))
            .putFloat("tdCrayonOp", s.toolDefaults.crayonOpacity.coerceIn(0.35f, 1f))
            .putBoolean("tapSelect", s.toolDefaults.tapSelect)
            .putFloat("tdPenOp", s.toolDefaults.penOpacity.coerceIn(0.15f, 1f))
            .putFloat("tdShapeOp", s.toolDefaults.shapeOpacity.coerceIn(0.15f, 1f))
            .putFloat("tdTextOp", s.toolDefaults.textOpacity.coerceIn(0.2f, 1f))
            .putFloat("tdStickerScale", s.toolDefaults.stickerScale.coerceIn(0.4f, 2f))
            .putFloat("tdStickerOp", s.toolDefaults.stickerOpacity.coerceIn(0.2f, 1f))
            .putBoolean("tdStickerAspect", s.toolDefaults.stickerLockAspect)
            .putFloat("tdImgRatio", s.toolDefaults.imageWidthRatio.coerceIn(0.2f, 0.95f))
            .putFloat("tdImgOp", s.toolDefaults.imageOpacity.coerceIn(0.2f, 1f))
            .putBoolean("tdImgAspect", s.toolDefaults.imageLockAspect)
            .putBoolean("tdImgBorder", s.toolDefaults.imageBorder)
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
            .putString("customHoverIconPath", s.customHoverIconPath)
            .putFloat("hoverIconSizeDp", s.hoverIconSizeDp.coerceIn(12f, 48f))
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
            .putBoolean("colorfulUiText", s.colorfulUiText)
            .putString("appFont", s.appFont.name)
            .putString("motionStyle", s.motionStyle.name)
            .putBoolean("liveBlur", s.liveBlur)
            .putBoolean("topScrollBlur", s.topScrollBlur)
            .putBoolean("classicAppearance", s.classicAppearance)
            .putBoolean("reduceEffects", s.reduceEffects)
            .putBoolean("showHoverTooltips", s.showHoverTooltips)
            .putBoolean("thumbCovers", s.thumbCovers)
            .putInt("pomodoroMinutes", s.pomodoroMinutes)
            .putString("googleUid", s.googleUid)
            .putString("googleEmail", s.googleEmail)
            .putString("googleName", s.googleName)
            .putLong("lastCloudSyncMs", s.lastCloudSyncMs)
            .putString("storageFolderUri", s.storageFolderUri)
            .putString("storageFolderLabel", s.storageFolderLabel)
            .putString("appLanguage", s.appLanguage)
            .putString("customStickyPresetsJson", s.customStickyPresetsJson)
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

    fun stickyPresets(): List<StickyPreset> {
        val raw = _settings.value.customStickyPresetsJson
        if (raw.isBlank()) return StickyPreset.DEFAULT_PRESETS
        val userList = raw.split(";;").mapNotNull { entry ->
            val p = entry.split("||")
            if (p.size >= 8) {
                StickyPreset(
                    id = p[0],
                    name = p[1],
                    color = p[2].toIntOrNull() ?: 0xFFFFF176.toInt(),
                    style = p[3],
                    paperStyle = p[4],
                    font = p[5],
                    fontSize = p[6].toFloatOrNull() ?: 28f,
                    cornerRadius = p[7].toFloatOrNull() ?: 18f,
                    pinned = p.getOrNull(8)?.toBooleanStrictOrNull() ?: false,
                )
            } else null
        }
        val customIds = userList.map { it.id }.toSet()
        return userList + StickyPreset.DEFAULT_PRESETS.filterNot { it.id in customIds }
    }

    fun saveStickyPreset(preset: StickyPreset) {
        val current = stickyPresets().filterNot { it.id == preset.id }
        val updated = listOf(preset) + current
        val serialized = updated.joinToString(";;") {
            "${it.id}||${it.name}||${it.color}||${it.style}||${it.paperStyle}||${it.font}||${it.fontSize}||${it.cornerRadius}||${it.pinned}"
        }
        update { it.copy(customStickyPresetsJson = serialized) }
    }

    fun deleteStickyPreset(id: String) {
        val updated = stickyPresets().filterNot { it.id == id }
        val serialized = updated.joinToString(";;") {
            "${it.id}||${it.name}||${it.color}||${it.style}||${it.paperStyle}||${it.font}||${it.fontSize}||${it.cornerRadius}||${it.pinned}"
        }
        update { it.copy(customStickyPresetsJson = serialized) }
    }

    // --------------------------------------------------------------- pens
    private val _pens = MutableStateFlow(loadPens())
    /** Every pen used on this device, with its own settings. */
    val pens: StateFlow<List<PenProfile>> = _pens

    private fun esc(s: String): String = s.replace("\\", "\\\\").replace("\t", " ").replace("\n", " ").replace("|", "/").replace(";", ",")

    private fun normalizePenKey(name: String): String =
        name.trim().lowercase()
            .replace(Regex("[-_ ]?(ble|bt|hid|stylus|pen|digitizer|touchscreen|virtual)$"), "")
            .replace(Regex("[^a-z0-9]"), "")

    private fun dedupePens(list: List<PenProfile>): List<PenProfile> {
        if (list.size <= 1) return list
        val result = mutableListOf<PenProfile>()
        for (p in list) {
            val isGeneric = p.name.equals("Stylus", ignoreCase = true) || p.name.isBlank()
            val norm = normalizePenKey(p.name)
            val existingIdx = result.indexOfFirst { ex ->
                ex.name == p.name ||
                    (isGeneric && result.isNotEmpty()) ||
                    (norm.isNotEmpty() && norm == normalizePenKey(ex.name)) ||
                    (ex.brand == p.brand && ex.brand != "Stylus" && (ex.name.startsWith("pen-") || p.name.startsWith("pen-")))
            }
            if (existingIdx >= 0) {
                val ex = result[existingIdx]
                // Prefer hardware device ID with user's custom alias & configured button actions
                val keepHardwareName = if (ex.name.startsWith("pen-") && !p.name.startsWith("pen-") && !isGeneric) p.name else ex.name
                val keepAlias = ex.alias.ifBlank { p.alias }
                val keepBrand = if (ex.brand != "Stylus") ex.brand else p.brand
                result[existingIdx] = ex.copy(name = keepHardwareName, brand = keepBrand, alias = keepAlias)
            } else {
                result.add(p)
            }
        }
        return result
    }

    private fun loadPens(): List<PenProfile> {
        val raw = prefs.getString("pens", "")!!.split('\n').mapNotNull { line ->
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
        val clean = dedupePens(raw)
        if (clean.size != raw.size) {
            prefs.edit().putString(
                "pens",
                clean.joinToString("\n") {
                    "${esc(it.name)}\t${esc(it.brand)}\t${it.sideButton}\t${it.click}\t${it.doubleClick}\t${esc(it.alias)}"
                },
            ).apply()
        }
        return clean
    }

    private fun savePens(list: List<PenProfile>) {
        val clean = dedupePens(list)
        _pens.value = clean
        prefs.edit().putString(
            "pens",
            clean.joinToString("\n") {
                "${esc(it.name)}\t${esc(it.brand)}\t${it.sideButton}\t${it.click}\t${it.doubleClick}\t${esc(it.alias)}"
            },
        ).apply()
    }

    fun pen(name: String): PenProfile? {
        val list = _pens.value
        list.firstOrNull { it.name == name }?.let { return it }
        val norm = normalizePenKey(name)
        if (norm.isNotEmpty()) {
            list.firstOrNull { normalizePenKey(it.name) == norm }?.let { return it }
        }
        if (name.equals("Stylus", ignoreCase = true) || name.isBlank()) {
            return list.firstOrNull()
        }
        return null
    }

    /** Remembers a pen the first time it is seen, merging duplicate hardware/manual aliases cleanly. */
    fun registerPen(name: String, brand: String? = null, alias: String? = null): PenProfile {
        val cleanName = name.trim().ifBlank { "Stylus" }
        val detectedBrand = brand ?: PenBrands.detect(cleanName) ?: "Stylus"
        val current = _pens.value

        // 1. Exact or normalized match
        pen(cleanName)?.let { existing ->
            val nextBrand = brand ?: existing.brand
            val nextAlias = alias ?: existing.alias
            if (nextBrand != existing.brand || nextAlias != existing.alias) {
                updatePen(existing.copy(brand = nextBrand, alias = nextAlias))
            }
            return pen(cleanName) ?: existing
        }

        // 2. If a user manually set up a pen ("pen-...") and now touches with hardware, bind to it instead of duplicating
        if (!cleanName.startsWith("pen-")) {
            val manualMatch = current.firstOrNull {
                it.name.startsWith("pen-") && (it.brand == detectedBrand || current.size == 1)
            }
            if (manualMatch != null) {
                val upgraded = manualMatch.copy(
                    name = cleanName,
                    brand = if (manualMatch.brand != "Stylus") manualMatch.brand else detectedBrand,
                    alias = alias ?: manualMatch.alias,
                )
                savePens(current.map { if (it.name == manualMatch.name) upgraded else it })
                return upgraded
            }
            // Also collapse generic fallback "Stylus" into the real hardware stylus once seen
            val genericMatch = current.firstOrNull { it.name.equals("Stylus", ignoreCase = true) }
            if (genericMatch != null) {
                val upgraded = genericMatch.copy(
                    name = cleanName,
                    brand = if (genericMatch.brand != "Stylus") genericMatch.brand else detectedBrand,
                    alias = alias ?: genericMatch.alias,
                )
                savePens(current.map { if (it.name == genericMatch.name) upgraded else it })
                return upgraded
            }
        }

        val p = PenProfile(cleanName, detectedBrand, alias = alias ?: "")
        savePens(current + p)
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

    // ------------------------------------------------------------ Settings JSON Import / Export
    fun exportSettingsJson(): String {
        val root = org.json.JSONObject()
        root.put("_format", "pronotes_settings_v1")
        val data = org.json.JSONObject()
        for ((k, v) in prefs.all) {
            when (v) {
                is Boolean -> data.put(k, org.json.JSONObject().put("t", "b").put("v", v))
                is Int -> data.put(k, org.json.JSONObject().put("t", "i").put("v", v))
                is Long -> data.put(k, org.json.JSONObject().put("t", "l").put("v", v))
                is Float -> data.put(k, org.json.JSONObject().put("t", "f").put("v", v.toDouble()))
                is String -> data.put(k, org.json.JSONObject().put("t", "s").put("v", v))
                is Set<*> -> {
                    val arr = org.json.JSONArray()
                    v.filterIsInstance<String>().forEach { arr.put(it) }
                    data.put(k, org.json.JSONObject().put("t", "ss").put("v", arr))
                }
            }
        }
        root.put("prefs", data)
        return root.toString(2)
    }

    fun importSettingsJson(raw: String): Boolean = runCatching {
        val root = org.json.JSONObject(raw)
        val data = root.optJSONObject("prefs") ?: return@runCatching false
        val ed = prefs.edit()
        val keys = data.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val entry = data.optJSONObject(k) ?: continue
            when (entry.optString("t")) {
                "b" -> ed.putBoolean(k, entry.optBoolean("v"))
                "i" -> ed.putInt(k, entry.optInt("v"))
                "l" -> ed.putLong(k, entry.optLong("v"))
                "f" -> ed.putFloat(k, entry.optDouble("v").toFloat())
                "s" -> ed.putString(k, entry.optString("v"))
                "ss" -> {
                    val arr = entry.optJSONArray("v") ?: org.json.JSONArray()
                    val set = LinkedHashSet<String>()
                    for (i in 0 until arr.length()) set += arr.optString(i)
                    ed.putStringSet(k, set)
                }
            }
        }
        ed.apply()
        _settings.value = load()
        _pens.value = loadPens()
        true
    }.getOrDefault(false)

    companion object {
        internal fun notebook(context: Context, id: String) = SettingsStore(context.applicationContext, "editor_$id")
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
