package com.pronotes.app.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Supported app display languages across the world.
 * Only English is built into the APK to keep the default download size small;
 * all 25 additional world UI language packs download optionally on demand (~3 KB each).
 */
@Immutable
data class AppLanguageOption(
    val tag: String,
    val englishName: String,
    val nativeName: String,
    val region: String,
    val isRtl: Boolean = false,
    val builtIn: Boolean = false,
    val approxKb: Int = 3,
)

object AppLanguages {
    val SYSTEM = AppLanguageOption(
        tag = "",
        englishName = "System default",
        nativeName = "System default",
        region = "Auto",
        isRtl = false,
        builtIn = true,
        approxKb = 0,
    )

    val ALL: List<AppLanguageOption> = listOf(
        SYSTEM,
        AppLanguageOption("en-US", "English (US)", "English (US)", "Popular", builtIn = true, approxKb = 0),
        AppLanguageOption("en-GB", "English (UK)", "English (UK)", "Popular", builtIn = true, approxKb = 0),
        AppLanguageOption("es", "Spanish", "Español", "Popular", approxKb = 3),
        AppLanguageOption("fr", "French", "Français", "Popular", approxKb = 3),
        AppLanguageOption("de", "German", "Deutsch", "Popular", approxKb = 3),
        AppLanguageOption("hi", "Hindi", "हिन्दी", "South Asia", approxKb = 4),
        AppLanguageOption("zh-CN", "Chinese (Simplified)", "简体中文", "East & SE Asia", approxKb = 3),
        AppLanguageOption("zh-TW", "Chinese (Traditional)", "繁體中文", "East & SE Asia", approxKb = 3),
        AppLanguageOption("ja", "Japanese", "日本語", "East & SE Asia", approxKb = 3),
        AppLanguageOption("ko", "Korean", "한국어", "East & SE Asia", approxKb = 3),
        AppLanguageOption("ar", "Arabic", "العربية", "Middle East", isRtl = true, approxKb = 4),
        AppLanguageOption("pt", "Portuguese", "Português", "Europe & Americas", approxKb = 3),
        AppLanguageOption("it", "Italian", "Italiano", "Europe & Americas", approxKb = 3),
        AppLanguageOption("ru", "Russian", "Русский", "Europe & Americas", approxKb = 4),
        AppLanguageOption("tr", "Turkish", "Türkçe", "Europe & Americas", approxKb = 3),
        AppLanguageOption("id", "Indonesian", "Bahasa Indonesia", "East & SE Asia", approxKb = 3),
        AppLanguageOption("vi", "Vietnamese", "Tiếng Việt", "East & SE Asia", approxKb = 3),
        AppLanguageOption("th", "Thai", "ไทย", "East & SE Asia", approxKb = 4),
        AppLanguageOption("bn", "Bengali", "বাংলা", "South Asia", approxKb = 4),
        AppLanguageOption("ta", "Tamil", "தமிழ்", "South Asia", approxKb = 4),
        AppLanguageOption("ur", "Urdu", "اردو", "South Asia", isRtl = true, approxKb = 4),
        AppLanguageOption("fa", "Persian", "فارسی", "Middle East", isRtl = true, approxKb = 4),
        AppLanguageOption("he", "Hebrew", "עברית", "Middle East", isRtl = true, approxKb = 3),
        AppLanguageOption("pl", "Polish", "Polski", "Europe & Americas", approxKb = 3),
        AppLanguageOption("nl", "Dutch", "Nederlands", "Europe & Americas", approxKb = 3),
        AppLanguageOption("uk", "Ukrainian", "Українська", "Europe & Americas", approxKb = 4),
        AppLanguageOption("sv", "Swedish", "Svenska", "Europe & Americas", approxKb = 3),
    )

    fun findByTag(tag: String): AppLanguageOption {
        if (tag.isBlank()) return SYSTEM
        return ALL.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
            ?: ALL.firstOrNull { it.tag.isNotEmpty() && tag.lowercase(Locale.ROOT).startsWith(it.tag.lowercase(Locale.ROOT)) }
            ?: SYSTEM
    }

    fun resolveEffectiveTag(appLanguageTag: String): String {
        if (appLanguageTag.isNotBlank()) return findByTag(appLanguageTag).tag.ifBlank { "en-US" }
        val sys = Locale.getDefault()
        val lang = sys.language.lowercase(Locale.ROOT)
        val country = sys.country.uppercase(Locale.ROOT)
        return when (lang) {
            "zh" -> if (country in setOf("TW", "HK", "MO")) "zh-TW" else "zh-CN"
            "en" -> if (country == "GB") "en-GB" else "en-US"
            "iw", "he" -> "he"
            "in", "id" -> "id"
            else -> findByTag(lang).tag.ifBlank { "en-US" }
        }
    }

    fun isBuiltIn(tag: String): Boolean {
        if (tag.isBlank()) return true
        val eff = resolveEffectiveTag(tag).lowercase(Locale.ROOT)
        return eff.startsWith("en")
    }

    fun isRtl(appLanguageTag: String): Boolean {
        val eff = resolveEffectiveTag(appLanguageTag).lowercase(Locale.ROOT)
        return eff.startsWith("ar") || eff.startsWith("he") || eff.startsWith("iw") ||
            eff.startsWith("fa") || eff.startsWith("ur")
    }
}

/**
 * Localized UI copy for Pro Notes' core workspace, notebook creation studio,
 * settings, and dialogs. Default English values are built-in; optional
 * downloaded language packs deserialize directly into this data class.
 */
@Immutable
@Serializable
data class AppStrings(
    // Library & Sidebar
    val documents: String = "Documents",
    val favorites: String = "Favorites",
    val recent: String = "Recent",
    val trash: String = "Trash",
    val readerMode: String = "Kindle Mode",
    val studyFriendsAndFiles: String = "Study Friends & Files",
    val settings: String = "Settings",
    val searchPrompt: String = "What are you looking for?",
    val newAction: String = "New",
    val folder: String = "Folder",
    val subfolder: String = "Subfolder",
    val emptyTrash: String = "Empty trash",
    val notebook: String = "Notebook",
    val infiniteWhiteboard: String = "Infinite whiteboard",
    val conceptMapCanvas: String = "Concept map canvas",
    val quickNote: String = "Quick note",
    val importFiles: String = "Import PDF, Word, PPT, Sheets, EPUB, Images",
    val sortDateModified: String = "Date modified",
    val sortDateCreated: String = "Date created",
    val sortName: String = "Name",
    val signInGoogle: String = "Sign in with Google",
    val syncNow: String = "Sync Now",
    val syncing: String = "Syncing…",
    // Notebook Creation Studio
    val title: String = "Title",
    val untitledNotebook: String = "Untitled Notebook",
    val cover: String = "Cover",
    val paper: String = "Paper",
    val createNotebook: String = "Create Notebook",
    val cancel: String = "Cancel",
    val includeCoverPage: String = "Include cover as Page 1",
    val sectionEssentials: String = "Essentials",
    val sectionWritingPapers: String = "Writing papers",
    val sectionPlanner: String = "Planner",
    val sectionGridTechnical: String = "Grid & Technical",
    val sectionCreativeMusic: String = "Creative & Music",
    val customPaperColor: String = "Custom paper color…",
    // Settings Categories
    val catStylus: String = "Stylus & input",
    val catStylusSub: String = "Drawing, feel, eraser targets, pens",
    val catGestures: String = "Gestures",
    val catGesturesSub: String = "Live scribble sensitivity, circle, taps",
    val catPages: String = "Pages & paper",
    val catPagesSub: String = "Scroll, templates, paper colour",
    val catToolbar: String = "Toolbar",
    val catToolbarSub: String = "Floating capsule, left/right dock, timer",
    val catLook: String = "Appearance & Theme",
    val catLookSub: String = "UI colour schemes, palette, typography, motion",
    val catLanguage: String = "Language & Region",
    val catLanguageSub: String = "Optional downloadable UI & handwriting packs",
    val catLibrary: String = "Library & backup",
    val catLibrarySub: String = "Covers, cloud sync, backup and restore",
    val catAbout: String = "About",
    val catAboutSub: String = "Version, resumable updates",
    // Language Settings & Pickers
    val appDisplayLanguage: String = "App display language",
    val appDisplayLanguageSub: String = "Optional downloadable language packs with instant RTL support",
    val handwritingRecognitionLang: String = "Handwriting recognition language",
    val handwritingRecognitionLangSub: String = "Optional on-device ML Kit packs across 58+ world languages",
    val offlineLanguagePacks: String = "Offline handwriting packs",
    val searchLanguages: String = "Search languages…",
    val allLanguagesCount: String = "All 58+ languages…",
    val downloaded: String = "Installed",
    val download: String = "Download",
    val downloading: String = "Downloading…",
    val deletePack: String = "Remove",
    val systemDefault: String = "System default",
    val done: String = "Done",
    val delete: String = "Delete",
    val rename: String = "Rename",
    val moveTo: String = "Move to",
    val back: String = "Back",
)

val LocalAppStrings = compositionLocalOf { AppStrings() }
val LocalAppLanguage = compositionLocalOf { AppLanguages.SYSTEM }

/**
 * Manages optional on-demand downloadable UI language packs stored in
 * `filesDir/language_packs/<tag>.json`. Keeping packs outside the APK minimizes
 * default app size while letting international users install only the languages they want.
 */
object LanguagePackManager {
    private const val PRIMARY_BASE_URL = "https://raw.githubusercontent.com/ItzNikk/ProNotes/main/language-packs"
    private const val MIRROR_BASE_URL = "https://cdn.jsdelivr.net/gh/ItzNikk/ProNotes@main/language-packs"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val cache = ConcurrentHashMap<String, AppStrings>()
    private val _installedTags = MutableStateFlow<Set<String>>(emptySet())
    val installedTags: StateFlow<Set<String>> = _installedTags.asStateFlow()

    private val _downloadingTags = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTags: StateFlow<Set<String>> = _downloadingTags.asStateFlow()

    private val _packVersion = MutableStateFlow(0)
    val packVersion: StateFlow<Int> = _packVersion.asStateFlow()

    private fun packDir(context: Context): File = File(context.filesDir, "language_packs").apply { mkdirs() }
    private fun packFile(context: Context, tag: String): File = File(packDir(context), "$tag.json")

    fun refreshInstalled(context: Context) {
        val dir = packDir(context)
        val found = dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) && it.length() > 16L }
            ?.map { it.nameWithoutExtension }
            ?.toSet()
            .orEmpty()
        _installedTags.value = found
    }

    fun isInstalled(context: Context, tag: String): Boolean {
        if (AppLanguages.isBuiltIn(tag)) return true
        val eff = AppLanguages.findByTag(tag).tag
        if (eff.isBlank() || AppLanguages.isBuiltIn(eff)) return true
        return eff in _installedTags.value || packFile(context, eff).let { it.isFile && it.length() > 16L }
    }

    fun installedBytes(context: Context): Long {
        val dir = packDir(context)
        return dir.listFiles()?.filter { it.isFile && it.extension == "json" }?.sumOf { it.length() } ?: 0L
    }

    /** Downloads an optional UI language pack from GitHub (with jsDelivr mirror fallback). */
    suspend fun downloadPack(context: Context, tag: String): Result<Unit> = withContext(Dispatchers.IO) {
        val opt = AppLanguages.findByTag(tag)
        if (opt.builtIn || opt.tag.isBlank()) return@withContext Result.success(Unit)
        val canonical = opt.tag
        _downloadingTags.value = _downloadingTags.value + canonical
        try {
            val urls = listOf(
                "$PRIMARY_BASE_URL/$canonical.json",
                "$MIRROR_BASE_URL/$canonical.json",
            )
            var lastError: Throwable? = null
            for (urlStr in urls) {
                val attempt = runCatching {
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        readTimeout = 12_000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "ProNotes-Android")
                    }
                    try {
                        val code = conn.responseCode
                        if (code !in 200..299) {
                            error("HTTP $code")
                        }
                        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val parsed = json.decodeFromString<AppStrings>(body)
                        val target = packFile(context, canonical)
                        val tmp = File(target.parentFile, "$canonical.json.tmp")
                        tmp.writeText(body, Charsets.UTF_8)
                        if (!tmp.renameTo(target)) {
                            target.writeText(body, Charsets.UTF_8)
                            tmp.delete()
                        }
                        cache[canonical] = parsed
                    } finally {
                        conn.disconnect()
                    }
                }
                if (attempt.isSuccess) {
                    refreshInstalled(context)
                    _packVersion.value = _packVersion.value + 1
                    return@withContext Result.success(Unit)
                } else {
                    lastError = attempt.exceptionOrNull()
                }
            }
            Result.failure(lastError ?: IllegalStateException("Unable to download language pack"))
        } finally {
            _downloadingTags.value = _downloadingTags.value - canonical
        }
    }

    /** Imports an offline `.json` UI language pack chosen by the user from device storage. */
    suspend fun importPack(context: Context, uri: Uri, tag: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val canonical = AppLanguages.findByTag(tag).tag.ifBlank { error("Select a target language first") }
            val body = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Could not read file")
            val parsed = json.decodeFromString<AppStrings>(body)
            packFile(context, canonical).writeText(body, Charsets.UTF_8)
            cache[canonical] = parsed
            refreshInstalled(context)
            _packVersion.value = _packVersion.value + 1
        }
    }

    /** Deletes a downloaded UI language pack to reclaim disk space. */
    fun deletePack(context: Context, tag: String): Boolean {
        val canonical = AppLanguages.findByTag(tag).tag
        if (canonical.isBlank() || AppLanguages.isBuiltIn(canonical)) return false
        cache.remove(canonical)
        val deleted = packFile(context, canonical).delete()
        refreshInstalled(context)
        _packVersion.value = _packVersion.value + 1
        return deleted
    }

    /** Deletes all downloaded optional UI language packs. */
    fun clearAllPacks(context: Context) {
        packDir(context).listFiles()?.forEach { if (it.isFile) it.delete() }
        cache.clear()
        refreshInstalled(context)
        _packVersion.value = _packVersion.value + 1
    }

    /** Loads the active [AppStrings] for [appLanguageTag], falling back to built-in English if not installed. */
    fun stringsFor(context: Context, appLanguageTag: String): AppStrings {
        val eff = AppLanguages.resolveEffectiveTag(appLanguageTag)
        if (AppLanguages.isBuiltIn(eff)) return AppStrings()
        val canonical = AppLanguages.findByTag(eff).tag
        if (canonical.isBlank()) return AppStrings()
        cache[canonical]?.let { return it }
        val file = packFile(context, canonical)
        if (file.isFile && file.length() > 16L) {
            val loaded = runCatching {
                json.decodeFromString<AppStrings>(file.readText(Charsets.UTF_8))
            }.getOrNull()
            if (loaded != null) {
                cache[canonical] = loaded
                return loaded
            }
        }
        return AppStrings()
    }
}

object Localization {
    private val defaultEnglish = AppStrings()

    fun stringsFor(context: Context, appLanguageTag: String): AppStrings =
        LanguagePackManager.stringsFor(context, appLanguageTag)

    fun stringsFor(appLanguageTag: String): AppStrings = defaultEnglish
}
