package com.pronotes.app.editor

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

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
}

/**
 * On-device maths evaluator for handwritten expressions. Supports arithmetic, powers,
 * implicit multiplication, percentages, factorials, common scientific functions/constants,
 * and one-variable linear equations. Invalid or ambiguous expressions return null rather
 * than inventing an answer.
 */
object MathSolver {
    fun normalize(raw: String): String = raw.trim()
        .replace("√", "sqrt")
        .replace("²", "^2").replace("³", "^3").replace("**", "^")
        .replace('×', '*').replace('·', '*').replace('⋅', '*')
        .replace('÷', '/').replace(':', '/')
        .replace('−', '-').replace('—', '-').replace('–', '-')
        .replace(',', '.')
        .replace(Regex("\\s+"), "")
        .replace(Regex("(?<=[0-9.)])(?:x|X)(?=[0-9.(])"), "*")
        .replace('X', 'x')
        // Correct common handwriting/OCR confusions only when they occur between digits.
        .replace(Regex("(?<=\\d)[oO](?=\\d)"), "0")
        .replace(Regex("(?<=\\d)[lI](?=\\d)"), "1")
        .replace(Regex("(?<=\\d)[sS](?=\\d)"), "5")
        .replace("π", "pi")
        .replace(Regex("\\s+"), "")
        .replace(Regex("sqrt(\\d+(?:\\.\\d+)?)"), "sqrt($1)")

    /** Choose the first highest-confidence parseable recognition candidate; ties retain model order. */
    fun bestCandidate(candidates: List<String>): String? {
        data class Ranked(val raw: String, val score: Int, val index: Int)
        return candidates.mapIndexedNotNull { index, raw ->
            val answer = solve(raw) ?: return@mapIndexedNotNull null
            val normalized = normalize(raw)
            val balanced = normalized.count { it == '(' } == normalized.count { it == ')' }
            val hasEquals = raw.trimEnd().endsWith("=")
            val score = 100 + (if (balanced) 4 else 0) + (if (hasEquals) 3 else 0) + (if (answer.isNotBlank()) 1 else 0)
            Ranked(raw, score, index)
        }.maxWithOrNull(compareBy<Ranked> { it.score }.thenBy { -it.index })?.raw
    }

    fun solve(raw: String): String? {
        val source = raw.trim().removeSuffix("?").trim()
        if (source.isEmpty() || source.length > 180) return null

        // A complete equation is handled separately so an incorrect equality is never treated as a sum.
        val equalsAt = source.indexOf('=')
        if (equalsAt >= 0 && equalsAt < source.lastIndex && source.substring(equalsAt + 1).isNotBlank()) {
            if (source.indexOf('=', equalsAt + 1) >= 0) return null
            val left = source.substring(0, equalsAt)
            val right = source.substring(equalsAt + 1)
            if (left.isBlank() || right.isBlank()) return null
            solveLinearEquation(left, right)?.let { return it }
            val leftValue = evaluate(left) ?: return null
            val rightValue = evaluate(right) ?: return null
            return if (abs(leftValue - rightValue) <= 1e-8 * maxOf(1.0, abs(leftValue), abs(rightValue))) format(rightValue) else null
        }

        val expression = normalize(source).trimEnd('=', '?')
        if (expression.isEmpty() || expression.none { it.isDigit() }) return null
        if (expression.none { it in "+-*/^%!()" || it.isLetter() }) return null
        return evaluate(expression)?.let(::format)
    }

    private fun evaluate(expression: String): Double? = runCatching {
        val value = NumericParser(expression).parse()
        require(value.isFinite())
        require(abs(value) < 1.0e100)
        value
    }.getOrNull()

    private fun solveLinearEquation(left: String, right: String): String? = runCatching {
        val a = LinearParser(normalize(left)).parse()
        val b = LinearParser(normalize(right)).parse()
        val coefficient = a.x - b.x
        val constant = b.constant - a.constant
        require(abs(coefficient) > 1e-12)
        val answer = constant / coefficient
        require(answer.isFinite() && abs(answer) < 1.0e100)
        "x = ${format(answer)}"
    }.getOrNull()

    private fun format(value: Double): String {
        require(value.isFinite())
        if (abs(value - kotlin.math.round(value)) < 1e-9 && abs(value) < 9.0e15) return kotlin.math.round(value).toLong().toString()
        return BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    private class NumericParser(private val s: String) {
        private var i = 0
        fun parse(): Double {
            val result = expression()
            require(i == s.length)
            return result
        }

        private fun expression(): Double {
            var value = term()
            while (peek() == '+' || peek() == '-') {
                val op = s[i++]
                val rhs = term()
                value = if (op == '+') value + rhs else value - rhs
            }
            return value
        }

        private fun term(): Double {
            var value = unary()
            while (i < s.length) {
                when (peek()) {
                    '*' -> { i++; value *= unary() }
                    '/' -> { i++; value /= unary() }
                    '%' -> {
                        if (startsPrimary(i + 1)) { i++; value %= unary() } else break
                    }
                    else -> if (startsPrimary(i)) value *= unary() else break // e.g. 2(3+4), 3pi
                }
            }
            return value
        }

        private fun unary(): Double = when (peek()) {
            '+' -> { i++; unary() }
            '-' -> { i++; -unary() }
            else -> power()
        }

        private fun power(): Double {
            val base = postfix()
            return if (peek() == '^') { i++; base.pow(unary()) } else base
        }

        private fun postfix(): Double {
            var value = primary()
            while (i < s.length) {
                when (peek()) {
                    '%' -> { if (startsPrimary(i + 1)) return value else { i++; value /= 100.0 } }
                    '!' -> { i++; value = factorial(value) }
                    '°' -> { i++; value = Math.toRadians(value) }
                    else -> return value
                }
            }
            return value
        }

        private fun primary(): Double {
            require(i < s.length)
            val c = peek()
            if (c == '(') {
                i++
                val value = expression()
                require(peek() == ')')
                i++
                return value
            }
            if (c.isDigit() || c == '.') return number()
            if (c.isLetter()) {
                val start = i
                while (i < s.length && (s[i].isLetter() || s[i].isDigit())) i++
                val name = s.substring(start, i).lowercase(Locale.US)
                return when (name) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    else -> function(name)
                }
            }
            error("Unexpected symbol")
        }

        private fun number(): Double {
            val start = i
            var digits = 0
            while (i < s.length && s[i].isDigit()) { i++; digits++ }
            if (peek() == '.') {
                i++
                while (i < s.length && s[i].isDigit()) { i++; digits++ }
            }
            require(digits > 0)
            if (peek() == 'e' || peek() == 'E') {
                val exponentStart = i++
                if (peek() == '+' || peek() == '-') i++
                val expDigits = i
                while (i < s.length && s[i].isDigit()) i++
                if (i == expDigits) i = exponentStart
            }
            return s.substring(start, i).toDouble()
        }

        private fun function(name: String): Double {
            require(peek() == '(')
            i++
            val args = ArrayList<Double>(2)
            if (peek() != ')') {
                args += expression()
                while (peek() == ',') { i++; args += expression() }
            }
            require(peek() == ')')
            i++
            require(args.isNotEmpty())
            val x = args[0]
            return when (name) {
                "sqrt" -> { require(args.size == 1 && x >= 0.0); kotlin.math.sqrt(x) }
                "cbrt" -> { require(args.size == 1); Math.cbrt(x) }
                "abs" -> { require(args.size == 1); abs(x) }
                "sin" -> { require(args.size == 1); kotlin.math.sin(x) }
                "cos" -> { require(args.size == 1); kotlin.math.cos(x) }
                "tan" -> { require(args.size == 1); kotlin.math.tan(x) }
                "sind" -> { require(args.size == 1); kotlin.math.sin(Math.toRadians(x)) }
                "cosd" -> { require(args.size == 1); kotlin.math.cos(Math.toRadians(x)) }
                "tand" -> { require(args.size == 1); kotlin.math.tan(Math.toRadians(x)) }
                "asin" -> { require(args.size == 1); Math.asin(x) }
                "acos" -> { require(args.size == 1); Math.acos(x) }
                "atan" -> { require(args.size == 1); Math.atan(x) }
                "ln" -> { require(args.size == 1 && x > 0); kotlin.math.ln(x) }
                "log", "log10" -> {
                    require(args.size in 1..2 && x > 0)
                    if (args.size == 2) { require(args[1] > 0 && args[1] != 1.0); kotlin.math.ln(x) / kotlin.math.ln(args[1]) }
                    else kotlin.math.log10(x)
                }
                "floor" -> { require(args.size == 1); kotlin.math.floor(x) }
                "ceil" -> { require(args.size == 1); kotlin.math.ceil(x) }
                "round" -> { require(args.size == 1); kotlin.math.round(x) }
                "exp" -> { require(args.size == 1); kotlin.math.exp(x) }
                "deg" -> { require(args.size == 1); Math.toDegrees(x) }
                "rad" -> { require(args.size == 1); Math.toRadians(x) }
                else -> error("Unknown function")
            }
        }

        private fun factorial(value: Double): Double {
            require(value >= 0 && value <= 170 && abs(value - kotlin.math.round(value)) < 1e-10)
            var result = 1.0
            for (n in 2..value.toInt()) result *= n
            return result
        }

        private fun startsPrimary(at: Int): Boolean {
            if (at !in s.indices) return false
            val c = s[at]
            return c.isDigit() || c == '.' || c == '(' || c.isLetter()
        }

        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'
    }

    /** Minimal linear-polynomial evaluator: each expression is represented as a*x + b. */
    private data class Linear(val x: Double, val constant: Double) {
        operator fun plus(other: Linear) = Linear(x + other.x, constant + other.constant)
        operator fun minus(other: Linear) = Linear(x - other.x, constant - other.constant)
        fun times(other: Linear): Linear {
            require(abs(x) < 1e-12 || abs(other.x) < 1e-12) // refuse quadratic/non-linear products
            return Linear(x * other.constant + other.x * constant, constant * other.constant)
        }
        fun dividedBy(other: Linear): Linear {
            require(abs(other.x) < 1e-12 && abs(other.constant) > 1e-12)
            return Linear(x / other.constant, constant / other.constant)
        }
    }

    private class LinearParser(private val s: String) {
        private var i = 0
        fun parse(): Linear { val result = expression(); require(i == s.length); return result }
        private fun expression(): Linear {
            var value = term()
            while (peek() == '+' || peek() == '-') {
                val op = s[i++]
                val rhs = term()
                value = if (op == '+') value + rhs else value - rhs
            }
            return value
        }
        private fun term(): Linear {
            var value = unary()
            while (i < s.length) {
                when (peek()) {
                    '*' -> { i++; value = value.times(unary()) }
                    '/' -> { i++; value = value.dividedBy(unary()) }
                    else -> if (startsPrimary()) value = value.times(unary()) else break
                }
            }
            return value
        }
        private fun unary(): Linear = when (peek()) {
            '+' -> { i++; unary() }
            '-' -> { i++; val v = unary(); Linear(-v.x, -v.constant) }
            else -> power()
        }
        private fun power(): Linear {
            val base = primary()
            if (peek() == '^') {
                i++
                val exponent = unary()
                require(abs(exponent.x) < 1e-12)
                val n = exponent.constant
                require(n == 0.0 || n == 1.0 || abs(base.x) < 1e-12)
                return when (n) {
                    0.0 -> Linear(0.0, 1.0)
                    1.0 -> base
                    else -> Linear(0.0, base.constant.pow(n))
                }
            }
            return base
        }
        private fun primary(): Linear {
            require(i < s.length)
            when (peek()) {
                '(' -> { i++; val v = expression(); require(peek() == ')'); i++; return v }
                'x' -> { i++; return Linear(1.0, 0.0) }
                else -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    require(i > start)
                    return Linear(0.0, s.substring(start, i).toDouble())
                }
            }
        }
        private fun startsPrimary(): Boolean = i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == '(' || s[i] == 'x')
        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'
    }
}
