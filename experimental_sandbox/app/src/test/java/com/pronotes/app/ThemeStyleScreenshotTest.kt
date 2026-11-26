package com.pronotes.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pronotes.app.data.AtomicFiles
import com.pronotes.app.data.PerformanceMode
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.library.LibraryScreen
import com.pronotes.app.reader.BookReaderHubScreen
import com.pronotes.app.reader.BookTheme
import com.pronotes.app.settings.SettingsScreen
import com.pronotes.app.support.SupportScreen
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.LocalCornerRoundness
import com.pronotes.app.ui.LocalPerformanceMode
import com.pronotes.app.ui.ProNotesTheme
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.contrastRatio
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot, theme-parity, contrast, and persistence checks across Pro Notes screens.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-xhdpi")
class ThemeStyleScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    private fun configureAppearance(
        themeMode: ThemeMode,
        cornerRoundnessDp: Float = 20f,
        performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    ) {
        val store = SettingsStore.get(appContext())
        store.update {
            it.copy(
                themeMode = themeMode,
                cornerRoundnessDp = cornerRoundnessDp,
                performanceMode = performanceMode,
            )
        }
    }

    private fun captureViewScreenshot(rootView: View, name: String) {
        val w = rootView.width.coerceAtLeast(1)
        val h = rootView.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        rootView.draw(Canvas(bmp))

        val center = bmp.getPixel(w / 2, h / 2)
        val corner = bmp.getPixel(4, 4)
        val quarter = bmp.getPixel(w / 4, h / 4)
        assertTrue(
            "Screenshot '$name' appears blank",
            center != 0 || corner != 0 || quarter != 0,
        )

        val outDir = File("build/outputs/screenshots").apply { mkdirs() }
        FileOutputStream(File(outDir, "$name.png")).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private data class AppearanceVariant(
        val themeMode: ThemeMode,
        val roundnessDp: Float = 20f,
        val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    )

    private fun renderScreenVariants(
        screenName: String,
        variants: List<AppearanceVariant>,
        assertUi: () -> Unit,
    ) {
        require(variants.isNotEmpty())
        val first = variants.first()
        configureAppearance(first.themeMode, first.roundnessDp, first.performanceMode)
        var capturedView: View? = null
        var resolvedRoundnessDp: Float? = null
        var resolvedPerformanceMode: PerformanceMode? = null

        rule.setContent {
            ProNotesTheme {
                capturedView = LocalView.current
                resolvedRoundnessDp = LocalCornerRoundness.current.value
                resolvedPerformanceMode = LocalPerformanceMode.current
                when (screenName) {
                    "library" -> LibraryScreen(
                        onOpen = {},
                        onSettings = {},
                        pendingImport = null,
                        onImportConsumed = {},
                    )
                    "settings" -> SettingsScreen(onBack = {})
                    "books" -> BookReaderHubScreen(
                        onOpenBook = {},
                        onBackToLibrary = {},
                        onSettings = {},
                    )
                    "support" -> SupportScreen(onBack = {})
                    "editor_chrome" -> EditorChromeHarness()
                }
            }
        }

        for (variant in variants) {
            rule.runOnIdle {
                configureAppearance(variant.themeMode, variant.roundnessDp, variant.performanceMode)
            }
            rule.waitForIdle()
            assertEquals(variant.roundnessDp, resolvedRoundnessDp ?: -1f, 0.01f)
            assertEquals(variant.performanceMode, resolvedPerformanceMode)
            assertUi()
            val view = requireNotNull(capturedView)
            captureViewScreenshot(view, "${screenName}_${variant.themeMode.name.lowercase()}")
        }
    }

    @Test
    fun libraryScreen_rendersLightAndDark() {
        renderScreenVariants(
            screenName = "library",
            variants = listOf(AppearanceVariant(ThemeMode.LIGHT), AppearanceVariant(ThemeMode.DARK)),
        ) {
            rule.onAllNodesWithText("Pro Notes")[0].assertIsDisplayed()
            rule.onAllNodesWithText("Documents")[0].assertIsDisplayed()
            rule.onNodeWithText("New").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_rendersLightAndDark() {
        renderScreenVariants(
            screenName = "settings",
            variants = listOf(AppearanceVariant(ThemeMode.LIGHT), AppearanceVariant(ThemeMode.DARK)),
        ) {
            rule.onAllNodesWithText("Settings")[0].assertIsDisplayed()
        }
    }

    @Test
    fun bookReaderHubScreen_rendersCleanly() {
        renderScreenVariants(
            screenName = "books",
            variants = listOf(AppearanceVariant(ThemeMode.LIGHT), AppearanceVariant(ThemeMode.DARK)),
        ) {
            rule.onAllNodesWithText("Reader Mode")[0].assertIsDisplayed()
        }
    }

    @Test
    fun supportScreen_rendersCleanly() {
        renderScreenVariants(
            screenName = "support",
            variants = listOf(AppearanceVariant(ThemeMode.LIGHT), AppearanceVariant(ThemeMode.DARK)),
        ) {
            rule.onNodeWithText("Highlights").assertIsDisplayed()
            rule.onNodeWithText("Send Applause").assertIsDisplayed()
        }
    }

    @Test
    fun editorChromeHarness_appliesCornerRoundnessAndPerformanceMode() {
        renderScreenVariants(
            screenName = "editor_chrome",
            variants = listOf(
                AppearanceVariant(ThemeMode.DARK, roundnessDp = 8f, performanceMode = PerformanceMode.BATTERY_SAVER),
                AppearanceVariant(ThemeMode.LIGHT, roundnessDp = 28f, performanceMode = PerformanceMode.MAXIMUM),
            ),
        ) {
            rule.onNodeWithText("Lecture Notes").assertIsDisplayed()
            rule.onNodeWithText("Pen").assertIsDisplayed()
        }
    }

    @Test
    fun bookThemes_haveReadableContrast() {
        for (theme in BookTheme.entries) {
            val ratio = contrastRatio(theme.inkText, theme.pageBg)
            assertTrue(
                "Book theme ${theme.label} failed 4.5:1 contrast: $ratio",
                ratio >= 4.5f,
            )
        }
    }

    @Test
    fun atomicFiles_writesSafelyWithoutLeftoverTempFiles() {
        val dir = File(appContext().filesDir, "atomic_test").apply { mkdirs() }
        val target = File(dir, "sample.json")
        AtomicFiles.writeTextAtomic(target, "{\"ok\":1}")
        AtomicFiles.writeTextAtomic(target, "{\"ok\":2}")
        assertEquals("{\"ok\":2}", target.readText())
        val leftoverTmps = dir.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertTrue("Unexpected leftover temp files: $leftoverTmps", leftoverTmps.isEmpty())
    }
}

@Composable
private fun EditorChromeHarness() {
    AppSurface(
        modifier = Modifier.fillMaxSize(),
        role = SurfaceRole.Chrome,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppSurface(
                role = SurfaceRole.Frost,
                shape = appCardShape(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Lecture Notes", style = MaterialTheme.typography.titleMedium)
                    AppPill(selected = true) { Text("Pen") }
                    AppPill(selected = false) { Text("Highlighter") }
                }
            }
            AppCard(
                hero = true,
                shape = appCardShape(),
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Canvas workspace preview", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
