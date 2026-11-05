package com.pronotes.app

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pronotes.app.editor.EditorScreen
import com.pronotes.app.library.LibraryScreen
import com.pronotes.app.ui.ProNotesTheme

class MainActivity : ComponentActivity() {
    private var pendingPdf by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ProNotesTheme {
                val nav = rememberNavController()
                NavHost(
                    navController = nav,
                    startDestination = "library",
                    enterTransition = { fadeIn() + scaleIn(initialScale = 0.96f) },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() + scaleOut(targetScale = 0.96f) },
                ) {
                    composable("library") {
                        LibraryScreen(
                            onOpen = { id -> nav.navigate("editor/$id") },
                            pendingPdf = pendingPdf,
                            onPdfConsumed = { pendingPdf = null },
                        )
                    }
                    composable("editor/{id}") { entry ->
                        val id = entry.arguments?.getString("id") ?: return@composable
                        EditorScreen(noteId = id, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
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
