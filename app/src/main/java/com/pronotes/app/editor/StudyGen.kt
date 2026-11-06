package com.pronotes.app.editor

import kotlin.random.Random

/**
 * Builds study material from page text, fully on the device and without a language model:
 * it looks for definitions ("X is ...", "X: ...", "X refers to ...") and key sentences, then
 * turns them into flashcards and multiple-choice questions whose wrong answers come from
 * other terms of the same text.
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
            // Fill-in-the-blank cards from key sentences.
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
        // "Which term matches this meaning?"
        for (f in facts) {
            if (out.size >= max) break
            // Wrong answers must not be words that appear in the meaning itself (that would give it away).
            val wrong = terms.filter { !it.equals(f.term, true) && !f.meaning.contains(it, ignoreCase = true) }.shuffled(rnd).take(3)
            if (wrong.size < 3) continue
            val opts = (wrong + f.term).shuffled(rnd)
            out += Mcq("Which term means: \"${f.meaning.take(160)}\"?", opts, opts.indexOf(f.term), f.sentence)
        }
        // Fill the gap in a sentence.
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
}

/** Evaluates handwritten maths such as "12 x (3+4) =" or "2^3 - 1". Returns null when it is not maths. */
object MathSolver {
    fun normalize(raw: String): String = raw
        .replace("²", "^2").replace("³", "^3").replace("**", "^")
        .replace('×', '*').replace('x', '*').replace('X', '*').replace('÷', '/').replace(':', '/')
        .replace('−', '-').replace('—', '-').replace('–', '-').replace(',', '.')
        .replace(" ", "").trimEnd('=', '?')

    fun solve(raw: String): String? {
        val s = normalize(raw)
        if (s.isEmpty() || s.none { it.isDigit() } || s.any { !(it.isDigit() || it in "+-*/^().%") }) return null
        if (s.none { it in "+-*/^%" }) return null
        val v = runCatching { Parser(s).parse() }.getOrNull() ?: return null
        if (v.isNaN() || v.isInfinite()) return null
        return if (kotlin.math.abs(v - Math.rint(v)) < 1e-9) Math.rint(v).toLong().toString()
        else "%.6f".format(v).trimEnd('0').trimEnd('.')
    }

    private class Parser(val s: String) {
        var i = 0
        fun parse(): Double { val v = expr(); require(i == s.length); return v }
        fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) { val op = s[i++]; val r = term(); v = if (op == '+') v + r else v - r }
            return v
        }
        fun term(): Double {
            var v = power()
            while (i < s.length && (s[i] == '*' || s[i] == '/' || s[i] == '%')) {
                val op = s[i++]; val r = power()
                v = when (op) { '*' -> v * r; '/' -> v / r; else -> v % r }
            }
            return v
        }
        fun power(): Double { val b = unary(); return if (i < s.length && s[i] == '^') { i++; Math.pow(b, power()) } else b }
        fun unary(): Double = if (i < s.length && s[i] == '-') { i++; -unary() } else atom()
        fun atom(): Double {
            if (s[i] == '(') { i++; val v = expr(); require(s[i] == ')'); i++; return v }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start)
            return s.substring(start, i).toDouble()
        }
    }
}
