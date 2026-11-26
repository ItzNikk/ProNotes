package com.pronotes.app.editor

/**
 * Word-level diff engine for note revision history.
 *
 * Computes what was added and removed between two versions of a note using
 * a longest-common-subsequence (LCS) approach on word tokens.
 *
 * Returns a list of [DiffSpan]s that can be rendered as coloured inline text
 * (green = added, red = removed, neutral = unchanged).
 */
object NoteRevisionDiff {

    enum class DiffType { ADDED, REMOVED, UNCHANGED }

    data class DiffSpan(
        val text: String,
        val type: DiffType,
    )

    data class DiffSummary(
        val spans: List<DiffSpan>,
        val addedWordCount: Int,
        val removedWordCount: Int,
        val changedWordCount: Int,
        val similarityPercent: Int,
    )

    private val TOKENIZE = Regex("""(\s+|[^\s]+)""")

    fun diff(before: String, after: String): DiffSummary {
        val beforeTokens = tokenize(before)
        val afterTokens = tokenize(after)

        val lcs = computeLCS(beforeTokens, afterTokens)
        val spans = buildSpans(beforeTokens, afterTokens, lcs)

        val added = spans.count { it.type == DiffType.ADDED && it.text.isNotBlank() }
        val removed = spans.count { it.type == DiffType.REMOVED && it.text.isNotBlank() }
        val unchanged = spans.count { it.type == DiffType.UNCHANGED && it.text.isNotBlank() }
        val beforeWords = before.split(Regex("""\s+""")).count { it.isNotBlank() }
        val afterWords = after.split(Regex("""\s+""")).count { it.isNotBlank() }
        val similarity = when {
            added == 0 && removed == 0 -> 100
            beforeWords + afterWords == 0 -> 100
            else -> ((unchanged * 2 * 100) / (beforeWords + afterWords)).coerceIn(0, 100)
        }

        return DiffSummary(
            spans = spans,
            addedWordCount = added,
            removedWordCount = removed,
            changedWordCount = added + removed,
            similarityPercent = similarity,
        )
    }

    private fun tokenize(text: String): List<String> =
        TOKENIZE.findAll(text).map { it.value }.toList()

    private fun computeLCS(a: List<String>, b: List<String>): Array<IntArray> {
        val m = a.size; val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1
                       else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
        return dp
    }

    private fun buildSpans(
        a: List<String>,
        b: List<String>,
        dp: Array<IntArray>,
    ): List<DiffSpan> {
        val result = mutableListOf<DiffSpan>()
        var i = a.size; var j = b.size
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && a[i - 1] == b[j - 1] -> {
                    result.add(0, DiffSpan(a[i - 1], DiffType.UNCHANGED)); i--; j--
                }
                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                    result.add(0, DiffSpan(b[j - 1], DiffType.ADDED)); j--
                }
                else -> {
                    result.add(0, DiffSpan(a[i - 1], DiffType.REMOVED)); i--
                }
            }
        }
        return result
    }
}
