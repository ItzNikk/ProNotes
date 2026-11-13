package com.pronotes.app.editor

import kotlin.random.Random

/**
 * Builds study material from page text, fully on the device and without a language model:
 * it looks for definitions ("X is ...", "X: ...", "X refers to ..."), then turns them
 * into flashcards and multiple-choice questions.
 */
object StudyGen {
    data class Fact(val term: String, val meaning: String, val sentence: String)

    private val defPatterns = listOf(
        Regex("""^(.{2,60}?)\s+(?:is|are|was|were)\s+(?:called|known as|defined as|termed)\s+(.{8,240})$""", RegexOption.IGNORE_CASE),
        Regex("""^(.{2,60}?)\s+(?:refers to|means|denotes|is defined as|is the|are the|is a|is an|are a|are)\s+(.{8,240})$""", RegexOption.IGNORE_CASE),
        Regex("""^([A-Z][\w\s\-()]{1,50}):\s+(.{8,240})$"""),
        Regex("""^([A-Z][\w\s\-()]{1,50})\s+[-–—]\s+(.{8,240})$"""),
    )

    private val stop = setOf(
        "the", "this", "that", "these", "those", "it", "they", "there", "here", "which", "what", "who", "a", "an", "and", "or",
        "in", "on", "of", "to", "for", "with", "by", "as", "at", "from", "but", "if", "so", "we", "he", "she", "his", "her",
        "their", "its", "our", "you", "your", "i", "also", "however", "thus", "therefore", "such", "one", "two", "all",
    )

    fun sentences(text: String): List<String> =
        text.replace('\n', ' ').replace(Regex("\\s+"), " ")
            .split(Regex("(?<=[.!?])\\s+(?=[A-Z0-9])"))
            .map { it.trim() }
            .filter { it.length in 25..320 && it.count { c -> c.isLetter() } > it.length / 2 }

    fun facts(text: String): List<Fact> {
        val out = ArrayList<Fact>()
        val seen = HashSet<String>()
        for (s in sentences(text)) {
            val body = s.trimEnd('.', ' ')
            for (p in defPatterns) {
                val m = p.find(body) ?: continue
                var term = m.groupValues[1].trim().trim(',', ';')
                val meaning = m.groupValues[2].trim()
                term = term.removePrefix("The ").removePrefix("the ").removePrefix("A ").removePrefix("An ")
                val words = term.split(' ')
                if (words.size > 6 || words.first().lowercase() in stop) continue
                if (!seen.add(term.lowercase())) break
                out += Fact(term, meaning.replaceFirstChar { it.uppercase() }, s)
                break
            }
        }
        return out
    }

    /** Important single words or short phrases: capitalised or repeated words, longest first. */
    fun keyTerms(text: String, limit: Int = 60): List<String> {
        val counts = HashMap<String, Int>()
        Regex("[A-Za-z][A-Za-z\\-]{3,}").findAll(text).forEach {
            val w = it.value
            if (w.lowercase() in stop) return@forEach
            counts[w] = (counts[w] ?: 0) + 1
        }
        return counts.entries.sortedByDescending { it.value * (if (it.key[0].isUpperCase()) 2 else 1) + it.key.length / 4 }
            .map { it.key }.distinctBy { it.lowercase() }.take(limit)
    }

    /** Key points: the sentences that share the most important words, in reading order. */
    fun summary(text: String, max: Int = 6): List<String> {
        val ss = sentences(text)
        if (ss.size <= max) return ss
        val freq = HashMap<String, Int>()
        ss.forEach { s -> Regex("[A-Za-z]{4,}").findAll(s.lowercase()).forEach { w -> if (w.value !in stop) freq[w.value] = (freq[w.value] ?: 0) + 1 } }
        val scored = ss.mapIndexed { i, s ->
            val words = Regex("[A-Za-z]{4,}").findAll(s.lowercase()).map { it.value }.filter { it !in stop }.toList()
            val score = words.sumOf { freq[it] ?: 0 }.toDouble() / (words.size + 4) + (if (facts(s).isNotEmpty()) 2.0 else 0.0) + (if (i < 3) 0.5 else 0.0)
            i to score
        }
        return scored.sortedByDescending { it.second }.take(max).map { it.first }.sorted().map { ss[it] }
    }

    data class Card(val front: String, val back: String)

    fun flashcards(text: String, max: Int = 25): List<Card> {
        val out = ArrayList<Card>()
        facts(text).forEach { f -> if (f.meaning.length <= 260) out += Card("What is ${f.term}?", f.meaning) }
        if (out.size < max) {
            val terms = keyTerms(text, 40)
            for (s in sentences(text)) {
                if (out.size >= max) break
                val term = terms.firstOrNull { t -> Regex("\\b${Regex.escape(t)}\\b").containsMatchIn(s) } ?: continue
                if (out.any { it.back.equals(term, true) }) continue
                out += Card(s.replace(Regex("\\b${Regex.escape(term)}\\b"), "_____"), term)
            }
        }
        return out.take(max)
    }

    data class Mcq(val question: String, val options: List<String>, val answer: Int, val explanation: String)

    fun quiz(text: String, max: Int = 15, rnd: Random = Random(text.hashCode())): List<Mcq> {
        val out = ArrayList<Mcq>()
        val facts = facts(text)
        val terms = (facts.map { it.term } + keyTerms(text, 50)).distinctBy { it.lowercase() }
        for (f in facts) {
            if (out.size >= max) break
            val wrong = terms.filter { !it.equals(f.term, true) && !f.meaning.contains(it, ignoreCase = true) }.shuffled(rnd).take(3)
            if (wrong.size < 3) continue
            val opts = (wrong + f.term).shuffled(rnd)
            out += Mcq("Which term means: \"${f.meaning.take(160)}\"?", opts, opts.indexOf(f.term), f.sentence)
        }
        for (s in sentences(text)) {
            if (out.size >= max) break
            val term = terms.firstOrNull { t -> t.length > 3 && Regex("\\b${Regex.escape(t)}\\b").containsMatchIn(s) } ?: continue
            if (out.any { it.explanation == s }) continue
            val wrong = terms.filter { !it.equals(term, true) && it[0].isUpperCase() == term[0].isUpperCase() }.shuffled(rnd).take(3)
            if (wrong.size < 3) continue
            val opts = (wrong + term).shuffled(rnd)
            out += Mcq("Fill the gap: " + s.replace(Regex("\\b${Regex.escape(term)}\\b"), "_____"), opts, opts.indexOf(term), s)
        }
        return out
    }

    fun slideDeck(title: String, text: String, maxSlides: Int = 8): List<com.pronotes.app.data.SlideCard> {
        val clean = text.trim()
        val themes = com.pronotes.app.data.SlideTheme.entries
        val deck = ArrayList<com.pronotes.app.data.SlideCard>()
        val terms = keyTerms(clean, 14)
        val facts = facts(clean)
        val pts = summary(clean, 8)
        val formulas = Regex("""(?:[A-Za-z_][\w]*\s*=[^.\n;]{1,36}|\d+\s*[+\-*/^×÷]\s*[\w\d()+\-*/^×÷.\s]{1,30}=?[^.\n]{0,20})""")
            .findAll(clean).map { it.value.trim() }.filter { it.length >= 4 }.distinct().take(4).toList()

        deck += com.pronotes.app.data.SlideCard(
            id = "sl-${System.nanoTime()}",
            kind = com.pronotes.app.data.SlideKind.TITLE,
            theme = themes[0],
            title = title.ifBlank { "Chapter Quick Revision" },
            subtitle = if (terms.isNotEmpty()) "Key themes: ${terms.take(4).joinToString(" · ")}" else "Interactive Chapter Deck",
            bullets = pts.take(3).map { it.take(96) },
            callout = "${maxOf(pts.size, facts.size, 1)} takeaways ready",
            badge = "CHAPTER DECK",
        )

        var themeIdx = 1
        for (f in facts.take(4)) {
            if (deck.size >= maxSlides - 2) break
            val detailBullets = f.meaning.split(Regex("[,;]")).map { it.trim() }.filter { it.length >= 5 }.take(4)
                .ifEmpty { listOf(f.meaning.take(140)) }
            deck += com.pronotes.app.data.SlideCard(
                id = "sl-${System.nanoTime()}-${deck.size}",
                kind = com.pronotes.app.data.SlideKind.CONCEPT,
                theme = themes[themeIdx++ % themes.size],
                title = f.term,
                subtitle = "Core Definition & Concept",
                bullets = detailBullets,
                callout = f.sentence.take(150),
                badge = "CONCEPT ${deck.size}",
            )
        }

        if (deck.size < maxSlides - 2 && pts.isNotEmpty()) {
            pts.chunked(3).forEachIndexed { idx, chunk ->
                if (deck.size >= maxSlides - 2) return@forEachIndexed
                deck += com.pronotes.app.data.SlideCard(
                    id = "sl-${System.nanoTime()}-p$idx",
                    kind = com.pronotes.app.data.SlideKind.CONCEPT,
                    theme = themes[themeIdx++ % themes.size],
                    title = terms.getOrNull(idx * 2) ?: "Key Takeaways #${idx + 1}",
                    subtitle = "Essential Notes",
                    bullets = chunk.map { it.take(130) },
                    badge = "HIGHLIGHT",
                )
            }
        }

        if (formulas.isNotEmpty() && deck.size < maxSlides - 1) {
            deck += com.pronotes.app.data.SlideCard(
                id = "sl-${System.nanoTime()}-fm",
                kind = com.pronotes.app.data.SlideKind.FORMULA,
                theme = themes[themeIdx++ % themes.size],
                title = "Formulas & Equations",
                subtitle = "High-yield relationships to memorize",
                bullets = formulas,
                callout = formulas.first(),
                badge = "FORMULA",
            )
        }

        val qaFact = facts.firstOrNull()
        val qaCard = flashcards(clean, 2).firstOrNull()
        if (qaFact != null || qaCard != null) {
            deck += com.pronotes.app.data.SlideCard(
                id = "sl-${System.nanoTime()}-qa",
                kind = com.pronotes.app.data.SlideKind.CHECK_QA,
                theme = com.pronotes.app.data.SlideTheme.ROSE_VELVET,
                title = "Quick Active Recall",
                subtitle = "Tap reveal to test yourself before moving on",
                question = qaCard?.front ?: "Define ${qaFact?.term.orEmpty()}",
                answer = qaCard?.back ?: qaFact?.meaning.orEmpty(),
                badge = "SELF CHECK",
            )
        }

        deck += com.pronotes.app.data.SlideCard(
            id = "sl-${System.nanoTime()}-sum",
            kind = com.pronotes.app.data.SlideKind.SUMMARY,
            theme = com.pronotes.app.data.SlideTheme.EMERALD_MINT,
            title = "Chapter Recap",
            subtitle = "Everything distilled into bite-size anchors",
            bullets = (pts.take(4).ifEmpty { terms.take(5) }).map { it.take(120) },
            callout = if (terms.isNotEmpty()) "Must-know terms: ${terms.take(5).joinToString(", ")}" else "",
            badge = "SUMMARY",
        )

        return deck.take(maxSlides)
    }

    /** Local semantic query expansion: finds related words co-occurring with [query] inside [corpus]. */
    fun expandQueryTerms(query: String, corpus: String): List<String> {
        val tokens = query.lowercase().split(Regex("\\s+")).map { it.trim(',', '.', '?', '!') }.filter { it.length >= 2 }
        if (tokens.isEmpty() || corpus.isBlank()) return emptyList()
        val related = LinkedHashMap<String, Int>()
        for (s in sentences(corpus)) {
            val low = s.lowercase()
            if (tokens.any { low.contains(it) }) {
                Regex("[A-Za-z][A-Za-z\\-]{3,}").findAll(s).forEach { m ->
                    val w = m.value
                    val wl = w.lowercase()
                    if (wl !in stop && tokens.none { wl.contains(it) }) {
                        related[w] = (related[w] ?: 0) + 1
                    }
                }
            }
        }
        return related.entries.sortedByDescending { it.value }.map { it.key }.take(5)
    }
}
