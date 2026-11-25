package com.pronotes.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import com.pronotes.app.data.SettingsStore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pronotes.app.editor.EditorScreen
import com.pronotes.app.library.LibraryScreen
import com.pronotes.app.reader.BookReaderHubScreen
import com.pronotes.app.reader.BookReaderScreen
import com.pronotes.app.settings.SettingsScreen
import com.pronotes.app.ui.ProNotesTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

object BuildConfigInfo {
    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
}

data class PendingImport(
    val uris: List<Uri> = emptyList(),
    val text: String? = null,
    val title: String? = null,
)

@androidx.annotation.RequiresApi(30)
private object Api30DisplayHelper {
    fun getDisplay(activity: android.app.Activity): android.view.Display? = activity.display
}

@androidx.annotation.RequiresApi(35)
private object Api35RefreshHelper {
    fun enableTouchBoost(window: android.view.Window) {
        window.frameRateBoostOnTouchEnabled = true
    }
}

class MainActivity : ComponentActivity() {
    private var widgetAction by mutableStateOf("")
    private var pendingImport by mutableStateOf<PendingImport?>(null)

    private var displayListener: android.hardware.display.DisplayManager.DisplayListener? = null

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableHighRefreshRate()
    }

    private fun enableHighRefreshRate() {
        runCatching {
            val disp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Api30DisplayHelper.getDisplay(this)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return
            val cur = disp.mode
            val modes = disp.supportedModes
            val best = modes
                .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
                .maxByOrNull { it.refreshRate }
                ?: modes.maxByOrNull { it.refreshRate }
            if (best != null) {
                val lp = window.attributes
                if (lp.preferredDisplayModeId != best.modeId || lp.preferredRefreshRate != best.refreshRate) {
                    lp.preferredDisplayModeId = best.modeId
                    lp.preferredRefreshRate = best.refreshRate
                    window.attributes = lp
                }
            }
            // Android 15+: keep Adaptive Refresh Rate boosted to max Hz on touch & stylus input
            if (android.os.Build.VERSION.SDK_INT >= 35) {
                Api35RefreshHelper.enableTouchBoost(window)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        window.decorView.post { enableHighRefreshRate() }
        val dm = getSystemService(android.hardware.display.DisplayManager::class.java)
        if (dm != null) {
            val listener = object : android.hardware.display.DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = enableHighRefreshRate()
                override fun onDisplayRemoved(displayId: Int) = Unit
                override fun onDisplayChanged(displayId: Int) = enableHighRefreshRate()
            }
            displayListener = listener
            dm.registerDisplayListener(listener, null)
        }
        com.pronotes.app.data.CloudSync.init(this)
        if (savedInstanceState == null || intent.action in setOf(Intent.ACTION_VIEW, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            handleIntent(intent)
        }
        UpdateCheckReceiver.schedule(this, SettingsStore.get(this).settings.value.updateReminders)
        // Leftover update packages take space; clear them unless installing or resuming a partial download.
        if (savedInstanceState == null && !com.pronotes.app.data.Updater.shouldPreserveCacheOnStart(this)) {
            Thread { runCatching { java.io.File(cacheDir, "updates").deleteRecursively() } }.start()
        }
        setContent {
            ProNotesTheme {
              // A Surface gives every screen the right text colour in light and dark mode.
              androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                com.pronotes.app.ui.UpdateDialogHost()
                com.pronotes.app.data.CloudSignInDialogHost()
                val nav = rememberNavController()
                val reduceMotion = com.pronotes.app.ui.LocalReduceMotion.current
                val perfMode = com.pronotes.app.ui.LocalPerformanceMode.current
                val motionStyle = com.pronotes.app.ui.LocalMotionStyle.current
                val openSettings: () -> Unit = {
                    if (nav.currentDestination?.route != "settings") nav.navigate("settings") { launchSingleTop = true }
                }
                androidx.compose.runtime.LaunchedEffect(widgetAction) {
                    if (widgetAction.isNotEmpty()) {
                        val repo = com.pronotes.app.data.Repository.get(this@MainActivity)
                        val id = when (widgetAction) {
                            "resume_notebook" -> repo.library.value.notebooks.filter { it.deletedAt == null }.maxByOrNull { it.opened }?.id
                            "quick_note" -> repo.createNotebook("Quick note", null, 0xFF007C79.toInt(),
                                SettingsStore.get(this@MainActivity).settings.value.defaultPaper)
                            else -> repo.library.value.notebooks.firstOrNull {
                                widgetAction == "notebook/" + it.id && it.deletedAt == null
                            }?.id
                        }
                        nav.popBackStack("library", false)
                        if (id != null) nav.navigate("editor/$id")
                        widgetAction = ""
                    }
                }
                NavHost(
                    navController = nav,
                    startDestination = "library",
                    enterTransition = { com.pronotes.app.ui.ProMotion.screenEnter(reduceMotion, perfMode, motionStyle) },
                    exitTransition = { com.pronotes.app.ui.ProMotion.screenExit(reduceMotion, perfMode, motionStyle) },
                    popEnterTransition = { com.pronotes.app.ui.ProMotion.screenPopEnter(reduceMotion, perfMode, motionStyle) },
                    popExitTransition = { com.pronotes.app.ui.ProMotion.screenPopExit(reduceMotion, perfMode, motionStyle) },
                ) {
                    composable("support") { com.pronotes.app.support.SupportScreen(onBack = { nav.popBackStack() }) }
                    composable("library") {
                        LibraryScreen(
                            onOpen = { id -> nav.navigate("editor/$id") },
                            onSettings = openSettings,
                            pendingImport = pendingImport,
                            onImportConsumed = { pendingImport = null },
                            onSupport = { nav.navigate("support") },
                            onOpenBookReader = { nav.navigate("books") { launchSingleTop = true } },
                        )
                    }
                    composable("books") {
                        BookReaderHubScreen(
                            onOpenBook = { id -> nav.navigate("reader/$id") },
                            onBackToLibrary = { nav.popBackStack("library", inclusive = false) },
                            onSettings = openSettings,
                        )
                    }
                    composable("reader/{id}") { entry ->
                        val id = entry.arguments?.getString("id") ?: return@composable
                        BookReaderScreen(
                            bookId = id,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("editor/{id}") { entry ->
                        val id = entry.arguments?.getString("id") ?: return@composable
                        EditorScreen(
                            noteId = id,
                            onBack = { nav.popBackStack("library", inclusive = false) },
                            onOpenSettings = openSettings,
                            onSwitchNote = { other -> nav.navigate("editor/$other") { popUpTo("library") } },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { nav.popBackStack() },
                            onSupport = { nav.navigate("support") },
                        )
                    }
                }
              }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
        com.pronotes.app.data.Updater.resumeIfWaiting(this)
    }

    override fun onDestroy() {
        displayListener?.let {
            runCatching { getSystemService(android.hardware.display.DisplayManager::class.java)?.unregisterDisplayListener(it) }
        }
        displayListener = null
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.pronotes.app.data.MemoryTrimRegistry.dispatch(level)
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        com.pronotes.app.data.MemoryTrimRegistry.dispatch(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            "open_notebook" -> widgetAction = "notebook/" + intent.getStringExtra("notebookId").orEmpty()
            "open_library", "resume_notebook", "quick_note" -> widgetAction = intent.action.orEmpty()
            Intent.ACTION_VIEW -> intent.data?.let { uri ->
                if (uri.scheme == "com.pronotes.app" && uri.host == "auth") {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val res = com.pronotes.app.data.CloudSync.handleAuthUri(this@MainActivity, uri)
                        res.onFailure {
                            android.widget.Toast.makeText(this@MainActivity, it.message ?: "Sign-in failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    pendingImport = PendingImport(uris = listOf(uri))
                }
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                when {
                    stream != null -> pendingImport = PendingImport(uris = listOf(stream), title = sharedTitle)
                    !sharedText.isNullOrBlank() -> pendingImport = PendingImport(text = sharedText, title = sharedTitle)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                val list = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                if (list.isNotEmpty()) pendingImport = PendingImport(uris = list)
            }
        }
    }
}
