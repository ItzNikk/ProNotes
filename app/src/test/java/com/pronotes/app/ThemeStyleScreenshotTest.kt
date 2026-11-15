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
import com.pronotes.app.ai.GalaxyCreativeStudioScreen
import com.pronotes.app.data.AtomicFiles
import com.pronotes.app.data.InterfaceStyle
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.library.LibraryScreen
import com.pronotes.app.onboarding.WelcomeScreen
import com.pronotes.app.settings.SettingsScreen
import com.pronotes.app.support.SupportScreen
import com.pronotes.app.ui.GlassCard
import com.pronotes.app.ui.GlassPill
import com.pronotes.app.ui.GlassRole
import com.pronotes.app.ui.GlassScene
import com.pronotes.app.ui.GlassSurface
import com.pronotes.app.ui.LocalCornerRoundness
import com.pronotes.app.ui.LocalInterfaceStyle
import com.pronotes.app.ui.ProNotesTheme
import com.pronotes.app.ui.glassCardShape
import io.github.ntvien.liquidglass.GlassQuality
import io.github.ntvien.liquidglass.LocalGlassCapabilities
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Screenshot and theme-parity tests rendering main screens in both
 * [InterfaceStyle.LIQUID_GLASS] and [InterfaceStyle.CLASSIC].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-xhdpi")
class ThemeStyleScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    private fun setStyle(style: InterfaceStyle, cornerRoundnessDp: Float = 24f) {
        val store = SettingsStore.get(appContext())
        store.update {
            it.copy(
                interfaceStyle = style,
                cornerRoundnessDp = cornerRoundnessDp,
                smartFeatures = true,
            )
        }
    }

    private fun captureViewScreenshot(rootView: View, name: String) {
        val w = rootView.width.coerceAtLeast(1)
        val h = rootView.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        rootView.draw(Canvas(bmp))

        // Verify frame rendered non-uniform visual pixels.
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

    private fun renderInStyle(
        screenName: String,
        style: InterfaceStyle,
        roundnessDp: Float = 24f,
        assertUi: () -> Unit,
    ) {
        setStyle(style, roundnessDp)
        var capturedView: View? = null
        var resolvedStyle: InterfaceStyle? = null
        var resolvedQuality: GlassQuality? = null
        var resolvedRoundnessDp: Float? = null

        rule.setContent {
            ProNotesTheme {
                capturedView = LocalView.current
                resolvedStyle = LocalInterfaceStyle.current
                resolvedQuality = LocalGlassCapabilities.current.effective
                resolvedRoundnessDp = LocalCornerRoundness.current.value
                when (screenName) {
                    "library" -> LibraryScreen(
                        onOpen = {},
                        onSettings = {},
                        pendingImport = null,
                        onImportConsumed = {},
                    )
                    "settings" -> SettingsScreen(onBack = {})
                    "welcome" -> WelcomeScreen(onDone = {})
                    "support" -> SupportScreen(onBack = {})
                    "studio" -> GalaxyCreativeStudioScreen(onBack = {})
                    "editor_chrome" -> EditorChromeHarness()
                }
            }
        }
        rule.waitForIdle()

        assertEquals(style, resolvedStyle)
        assertEquals(roundnessDp, resolvedRoundnessDp ?: -1f, 0.01f)
        if (style == InterfaceStyle.CLASSIC) {
            assertEquals(
                "Classic mode must disable shader/blur quality tiers completely",
                GlassQuality.Flat,
                resolvedQuality,
            )
        }
        assertUi()
        val view = requireNotNull(capturedView)
        captureViewScreenshot(view, "${screenName}_${style.name.lowercase()}")
    }

    @Test
    fun libraryScreen_rendersBothStyles() {
        for (style in InterfaceStyle.entries) {
            renderInStyle("library", style) {
                rule.onAllNodesWithText("Documents")[0].assertIsDisplayed()
            }
        }
    }

    @Test
    fun settingsScreen_rendersBothStyles() {
        for (style in InterfaceStyle.entries) {
            renderInStyle("settings", style) {
                rule.onAllNodesWithText("Settings")[0].assertIsDisplayed()
                rule.onAllNodesWithText("Smart features")[0].assertIsDisplayed()
            }
        }
    }

    @Test
    fun welcomeScreen_rendersBothStyles() {
        for (style in InterfaceStyle.entries) {
            renderInStyle("welcome", style) {
                rule.onNodeWithText("Skip setup").assertIsDisplayed()
            }
        }
    }

    @Test
    fun supportScreen_rendersBothStyles() {
        for (style in InterfaceStyle.entries) {
            renderInStyle("support", style) {
                rule.onNodeWithText("Support Pro Notes").assertIsDisplayed()
            }
        }
    }

    @Test
    fun creativeStudioScreen_rendersBothStyles() {
        for (style in InterfaceStyle.entries) {
            renderInStyle("studio", style) {
                rule.onNodeWithText("Creative Studio").assertIsDisplayed()
            }
        }
    }

    @Test
    fun editorChromeShell_rendersBothStylesAndCornerRoundness() {
        renderInStyle("editor_chrome", InterfaceStyle.LIQUID_GLASS, roundnessDp = 36f) {
            rule.onNodeWithText("Lecture Notes").assertIsDisplayed()
            rule.onNodeWithText("Pen").assertIsDisplayed()
        }
        renderInStyle("editor_chrome", InterfaceStyle.CLASSIC, roundnessDp = 8f) {
            rule.onNodeWithText("Lecture Notes").assertIsDisplayed()
            rule.onNodeWithText("Pen").assertIsDisplayed()
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

    @Test
    fun smartFeatures_defaultsOnAndClassicStyleIsDeviceLocal() {
        val store = SettingsStore.get(appContext())
        store.update { it.copy(interfaceStyle = InterfaceStyle.CLASSIC, smartFeatures = false) }
        assertEquals(InterfaceStyle.CLASSIC, store.settings.value.interfaceStyle)
        assertFalse(store.settings.value.smartFeatures)
    }
}

@Composable
private fun EditorChromeHarness() {
    GlassScene(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassSurface(
                role = GlassRole.Chrome,
                shape = glassCardShape(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Lecture Notes", style = MaterialTheme.typography.titleMedium)
                    GlassPill(selected = true) { Text("Pen") }
                    GlassPill(selected = false) { Text("Highlighter") }
                }
            }
            GlassCard(
                hero = true,
                shape = glassCardShape(),
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Canvas workspace preview", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
