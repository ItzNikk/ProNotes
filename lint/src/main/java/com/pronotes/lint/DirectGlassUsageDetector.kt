package com.pronotes.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement

/**
 * Fails the build if any screen or component outside GlassSurface.kt (or Theme.kt for LiquidGlassTheme/GlassColors)
 * directly imports or calls raw liquidglass primitives (LiquidSurface, LiquidGlassScene, OptionalLiquidGlassBox,
 * LiquidGlassPill, LiquidGlassCard, LiquidGlassChip, LiquidGlassDialog, LiquidGlassDialogSurface, LiquidGlassSheet,
 * LiquidToggle, LiquidSlider, LiquidSliderRow, LiquidChoiceRow, LiquidContextMenu, LiquidMenuButton,
 * LiquidSegmentedControl, LiquidTabBar, LiquidBottomTabs, LiquidGradientBackground, liquidGlass, droplet).
 */
class DirectGlassUsageDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UImportStatement::class.java,
        UCallExpression::class.java,
    )

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        val filePath = context.file.path.replace('\\', '/')
        // Allow inside the liquidglass implementation itself, the GlassSurface bridge, and Theme setup.
        if (filePath.contains("/io/github/ntvien/liquidglass/")) return null
        if (filePath.endsWith("/com/pronotes/app/ui/GlassSurface.kt")) return null
        if (filePath.endsWith("/com/pronotes/app/ui/Theme.kt")) return null
        if (filePath.contains("/src/test/") || filePath.contains("/src/androidTest/")) return null

        return object : UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                val fqName = node.importReference?.asSourceString() ?: return
                if (fqName.startsWith("io.github.ntvien.liquidglass") ||
                    fqName.startsWith("com.kyant.backdrop")
                ) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Do not import raw glass library '$fqName' directly. Use wrappers from com.pronotes.app.ui.GlassSurface instead.",
                    )
                }
            }

            override fun visitCallExpression(node: UCallExpression) {
                val name = node.methodName ?: return
                if (name in FORBIDDEN_DIRECT_CALLS) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Direct call to '$name' is disallowed outside GlassSurface.kt. Use the GlassSurface wrapper (GlassScene, GlassSurface, GlassCard, GlassPill, etc.) so Classic mode works everywhere.",
                    )
                }
            }
        }
    }

    companion object {
        private val FORBIDDEN_DIRECT_CALLS = setOf(
            "LiquidSurface",
            "LiquidGlassScene",
            "OptionalLiquidGlassBox",
            "LiquidGlassPill",
            "LiquidGlassCard",
            "LiquidGlassChip",
            "LiquidGlassDialog",
            "LiquidGlassDialogSurface",
            "LiquidGlassSheet",
            "LiquidGlassSlider",
            "LiquidGlassToggle",
            "LiquidToggle",
            "LiquidSlider",
            "LiquidSliderRow",
            "LiquidChoiceRow",
            "LiquidContextMenu",
            "LiquidMenuButton",
            "LiquidSegmentedControl",
            "LiquidTabBar",
            "LiquidBottomTabs",
            "LiquidGradientBackground",
            "LiquidButton",
            "DisableGlassSceneBackdrop",
            "liquidGlass",
            "droplet",
        )

        val ISSUE: Issue = Issue.create(
            id = "DirectLiquidGlassUsage",
            briefDescription = "Direct glass component used outside GlassSurface wrapper",
            explanation = "All UI must go through GlassSurface.kt wrappers so switching to Classic mode removes every glass effect and backdrop allocation.",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                DirectGlassUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
