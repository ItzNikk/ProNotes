package com.pronotes.benchmark

import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.pronotes.app"

@RunWith(AndroidJUnit4::class)
class ProNotesMacrobenchmark {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun collectStartupProfile() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        outputFilePrefix = "pronotes-startup",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        check(device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)) {
            "Pro Notes did not open during profile capture"
        }
    }

    @Test
    fun coldStart() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }

    @Test
    fun libraryScroll() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        },
    ) {
        val scrollable = device.findObject(By.scrollable(true))
        if (scrollable != null) {
            scrollable.setGestureMargin(device.displayWidth / 6)
            scrollable.fling(Direction.DOWN)
            device.waitForIdle()
            scrollable.fling(Direction.UP)
            device.waitForIdle()
        } else {
            val w = device.displayWidth
            val h = device.displayHeight
            device.swipe(w / 2, (h * 0.72f).toInt(), w / 2, (h * 0.28f).toInt(), 18)
            device.waitForIdle()
        }
    }

    @Test
    fun openNotebook() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        },
    ) {
        openOrCreateNotebook()
        device.waitForIdle()
    }

    @Test
    fun pageFlip() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            openOrCreateNotebook()
        },
    ) {
        val next = device.findObject(By.desc("Next page"))
        if (next != null && next.isEnabled) {
            next.click()
        } else {
            val w = device.displayWidth
            val h = device.displayHeight
            device.swipe((w * 0.8f).toInt(), h / 2, (w * 0.2f).toInt(), h / 2, 16)
        }
        device.waitForIdle()
    }

    @Test
    fun drawStroke() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            openOrCreateNotebook()
        },
    ) {
        val w = device.displayWidth
        val h = device.displayHeight
        val y = (h * 0.52f).toInt()
        device.swipe((w * 0.28f).toInt(), y, (w * 0.72f).toInt(), y + 120, 24)
        SystemClock.sleep(150)
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openOrCreateNotebook() {
        val noteCanvas = device.findObject(By.clazz("com.pronotes.app.editor.NoteCanvasView"))
        if (noteCanvas != null) return

        val createBtn = device.findObject(By.desc("Create new"))
        if (createBtn != null) {
            createBtn.click()
            device.wait(Until.hasObject(By.text("Quick note")), 2_000)
            device.findObject(By.text("Quick note"))?.click()
        } else {
            val w = device.displayWidth
            val h = device.displayHeight
            device.click(w / 2, (h * 0.36f).toInt())
        }
        device.wait(Until.hasObject(By.clazz("com.pronotes.app.editor.NoteCanvasView")), 4_000)
    }
}
