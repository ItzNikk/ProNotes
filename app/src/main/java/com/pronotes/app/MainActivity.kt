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

object BuildConfigInfo {
    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
}

class MainActivity : ComponentActivity() {
    private var pendingPdf by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ProNotesTheme {
              // A Surface gives every screen the right text colour in light and dark mode.
              androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                com.pronotes.app.ui.UpdateDialogHost()
                val nav = rememberNavController()
                val firstRun = remember { !SettingsStore.get(this@MainActivity).settings.value.onboardingDone }
                NavHost(
                    navController = nav,
                    startDestination = if (firstRun) "welcome" else "library",
                    enterTransition = { fadeIn() + scaleIn(initialScale = 0.96f) },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() + scaleOut(targetScale = 0.96f) },
                ) {
                    composable("welcome") {
                        com.pronotes.app.onboarding.WelcomeScreen(onDone = {
                            if (!nav.popBackStack()) nav.navigate("library") { popUpTo("welcome") { inclusive = true } }
                        })
                    }
                    composable("support") { com.pronotes.app.support.SupportScreen(onBack = { nav.popBackStack() }) }
                    composable("library") {
                        LibraryScreen(
                            onOpen = { id -> nav.navigate("editor/$id") },
                            onSettings = { nav.navigate("settings") },
                            pendingPdf = pendingPdf,
                            onPdfConsumed = { pendingPdf = null },
                            onSupport = { nav.navigate("support") },
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
        if (intent?.action == Intent.ACTION_VIEW && intent.type == "application/pdf") {
            pendingPdf = intent.data
        }
    }
}
