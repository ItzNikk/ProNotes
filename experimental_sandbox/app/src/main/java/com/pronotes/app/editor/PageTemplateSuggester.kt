package com.pronotes.app.editor

/**
 * Suggests the best page template / paper style for a note based on its content.
 *
 * Content signals:
 * - Heavy bullet/list structure → Lined or Cornell
 * - Mathematical expressions → Grid or Graph
 * - Many dates/numbers → Grid
 * - Long paragraphs → Ruled (Seyès)
 * - Short, creative, visual → Blank
 * - Lists with sub-indentation → Cornell
 */
object PageTemplateSuggester {

    enum class PaperTemplate {
        BLANK,
        LINED,
        GRID,
        DOTTED,
        CORNELL,
        SEYES,
        GRAPH,
    }

    data class TemplateSuggestion(
        val recommended: PaperTemplate,
        val reason: String,
        val alternatives: List<PaperTemplate>,
    )

    private val MATH_REGEX = Regex("""[+*/=^√∫∑∏±≤≥≠∞×÷]|\d\s*-\s*\d|\bsqrt\b|\bsin\b|\bcos\b|\btan\b|\blog\b|\blim\b|\bdx\b|\bdy\b""", RegexOption.IGNORE_CASE)
    private val DATE_REGEX = Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b""", RegexOption.IGNORE_CASE)
    private val BULLET_REGEX = Regex("""^\s*[-*•]\s""", RegexOption.MULTILINE)
    private val INDENT_REGEX = Regex("""^\s{4,}[-*•]\s""", RegexOption.MULTILINE)
    private val PARAGRAPH_REGEX = Regex("""[^.!?]+[.!?]""")

    fun suggest(text: String): TemplateSuggestion {
        if (text.isBlank()) {
            return TemplateSuggestion(
                recommended = PaperTemplate.BLANK,
                reason = "Empty note — start fresh on a blank canvas.",
                alternatives = listOf(PaperTemplate.DOTTED, PaperTemplate.LINED),
            )
        }

        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val wordCount = words.size
        val lineCount = text.lines().filter { it.isNotBlank() }.size
        val bulletCount = BULLET_REGEX.findAll(text).count()
        val hasDeepIndent = INDENT_REGEX.containsMatchIn(text)
        val mathCount = MATH_REGEX.findAll(text).count()
        val dateCount = DATE_REGEX.findAll(text).count()
        val sentences = PARAGRAPH_REGEX.findAll(text).toList()
        val avgSentenceWords = if (sentences.isEmpty()) wordCount.toDouble()
            else sentences.map { it.value.split(Regex("""\s+""")).size }.average()
        val bulletRatio = if (lineCount > 0) bulletCount.toDouble() / lineCount else 0.0

        return when {
            mathCount >= 5 || (mathCount >= 2 && wordCount < 50) ->
                TemplateSuggestion(
                    recommended = PaperTemplate.GRAPH,
                    reason = "Mathematical content detected — a graph-paper grid helps align equations and diagrams.",
                    alternatives = listOf(PaperTemplate.GRID, PaperTemplate.DOTTED),
                )

            hasDeepIndent || (bulletRatio > 0.5 && bulletCount > 4) ->
                TemplateSuggestion(
                    recommended = PaperTemplate.CORNELL,
                    reason = "Hierarchical bullet structure — Cornell layout separates cues from notes and adds a summary strip.",
                    alternatives = listOf(PaperTemplate.LINED, PaperTemplate.DOTTED),
                )

            bulletRatio > 0.35 && bulletCount > 2 ->
                TemplateSuggestion(
                    recommended = PaperTemplate.LINED,
                    reason = "List-heavy note — lined paper keeps bullet points visually ordered.",
                    alternatives = listOf(PaperTemplate.DOTTED, PaperTemplate.CORNELL),
                )

            dateCount > 3 || (dateCount > 1 && wordCount < 80) ->
                TemplateSuggestion(
                    recommended = PaperTemplate.GRID,
                    reason = "Date-heavy content — grid paper is great for schedules and planners.",
                    alternatives = listOf(PaperTemplate.DOTTED, PaperTemplate.LINED),
                )

            avgSentenceWords > 15 && wordCount > 80 ->
                TemplateSuggestion(
                    recommended = PaperTemplate.SEYES,
                    reason = "Long academic prose — Seyès ruling provides extra space between lines for dense writing.",
                    alternatives = listOf(PaperTemplate.LINED, PaperTemplate.CORNELL),
                )

            wordCount < 40 ->
                TemplateSuggestion(
                    recommended = PaperTemplate.DOTTED,
                    reason = "Short, creative note — dotted paper gives layout freedom without distracting lines.",
                    alternatives = listOf(PaperTemplate.BLANK, PaperTemplate.GRID),
                )

            else ->
                TemplateSuggestion(
                    recommended = PaperTemplate.LINED,
                    reason = "General note — lined paper is the balanced default for mixed content.",
                    alternatives = listOf(PaperTemplate.DOTTED, PaperTemplate.CORNELL),
                )
        }
    }
}
