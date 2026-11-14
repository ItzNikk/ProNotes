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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.pronotes.app.data.SettingsStore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pronotes.app.editor.EditorScreen
import com.pronotes.app.library.LibraryScreen
import com.pronotes.app.settings.SettingsScreen
import com.pronotes.app.ui.ProNotesTheme
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

class MainActivity : ComponentActivity() {
    private var pendingImport by mutableStateOf<PendingImport?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.pronotes.app.data.CloudSync.init(this)
        handleIntent(intent)
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
                NavHost(
                    navController = nav,
                    startDestination = "library",
                    enterTransition = { fadeIn() + scaleIn(initialScale = 0.96f) },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() + scaleOut(targetScale = 0.96f) },
                ) {
                    composable("support") { com.pronotes.app.support.SupportScreen(onBack = { nav.popBackStack() }) }
                    composable("studio") {
                        com.pronotes.app.ai.GalaxyCreativeStudioScreen(onBack = { nav.popBackStack() })
                    }
                    composable("library") {
                        LibraryScreen(
                            onOpen = { id -> nav.navigate("editor/$id") },
                            onSettings = { nav.navigate("settings") },
                            pendingImport = pendingImport,
                            onImportConsumed = { pendingImport = null },
                            onSupport = { nav.navigate("support") },
                            onOpenStudio = { nav.navigate("studio") },
                        )
                    }
                    composable("editor/{id}") { entry ->
                        val id = entry.arguments?.getString("id") ?: return@composable
                        EditorScreen(
                            noteId = id,
                            onBack = { nav.popBackStack("library", inclusive = false) },
                            onOpenSettings = { nav.navigate("settings") },
                            onSwitchNote = { other -> nav.navigate("editor/$other") { popUpTo("library") } },
                        )
                    }
                    composable(
                        "settings",
                        enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
                    ) {
                        SettingsScreen(
                            onBack = { nav.popBackStack() },
                            onTour = { nav.navigate("welcome") },
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
        com.pronotes.app.data.Updater.resumeIfWaiting(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
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
