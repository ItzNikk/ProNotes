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

data class MathDetailedResult(
    val normalized: String,
    val answer: String,
    val steps: List<String>,
    val fractionHint: String? = null,
)

object MathSolver {
    private val superDigits = mapOf('⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4', '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9')
    private val reservedNames = setOf(
        "pi", "tau", "phi", "e", "sqrt", "cbrt", "abs", "sin", "cos", "tan", "sind", "cosd", "tand",
        "asin", "acos", "atan", "ln", "log", "log2", "log10", "floor", "ceil", "round", "exp", "deg", "rad", "ncr", "npr",
    )

    private fun replaceUnicodeSuperscripts(raw: String): String {
        val out = StringBuilder(raw.length + 8)
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            val mapped = superDigits[ch]
            if (mapped != null) {
                val run = StringBuilder()
                while (i < raw.length && superDigits[raw[i]] != null) {
                    run.append(superDigits[raw[i]])
                    i++
                }
                out.append("^(").append(run).append(')')
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    private fun preserveFunctionArgCommas(input: String): String {
        val sb = StringBuilder(input)
        val multiArg = Regex("""(?i)\b(?:ncr|npr|log10|log)\s*\(""")
        for (m in multiArg.findAll(input)) {
            var depth = 1
            var i = m.range.last + 1
            while (i < sb.length && depth > 0) {
                when (sb[i]) {
                    '(' -> depth++
                    ')' -> depth--
                    ',' -> if (depth == 1) sb.setCharAt(i, ';')
                }
                i++
            }
        }
        return sb.toString()
    }

    fun normalize(raw: String): String {
        var s = replaceUnicodeSuperscripts(preserveFunctionArgCommas(raw.trim()))
            .replace('[', '(').replace(']', ')')
            .replace('{', '(').replace('}', ')')
            .replace("√", "sqrt")
            .replace("**", "^")
            .replace('×', '*').replace('·', '*').replace('⋅', '*')
            .replace('÷', '/').replace(':', '/')
            .replace('−', '-').replace('—', '-').replace('–', '-')
            .replace("π", "pi")
            .replace("τ", "tau")
            .replace("φ", "phi")
            .replace(Regex("(?<=\\d),(?=\\d{3}\\b)"), "")
            .replace(',', '.')
            .replace(Regex("\\s+"), "")
            // Convert 'x' or 'X' into '*' ONLY when standing between two numbers: e.g. 12x4 -> 12*4, NOT 2x(x+1)
            .replace(Regex("(?<=\\d)[xX](?=\\d)"), "*")
            // Fix handwritten OCR digit slips only when surrounded by digits
            .replace(Regex("(?<=\\d)[oO](?=\\d)"), "0")
            .replace(Regex("(?<=\\d)[lI](?=\\d)"), "1")
            .replace(Regex("(?<=\\d)[sS](?=\\d)"), "5")
            .replace(Regex("sqrt(\\d+(?:\\.\\d+)?|[a-zA-Z])"), "sqrt($1)")

        // Convert |expr| absolute-value notation into abs(expr)
        var absGuard = 0
        val absRegex = Regex("""\|([^|]+)\|""")
        while (s.contains('|') && absGuard < 8) {
            val next = absRegex.replace(s, "abs($1)")
            if (next == s) break
            s = next
            absGuard++
        }

        // Handle powers wrapped simply like x^(2) -> x^2 when safe
        s = s.replace(Regex("""\^\((\d+)\)"""), "^$1")
        return s
    }

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

    fun solve(raw: String): String? = solveDetailed(raw)?.answer

    fun solveDetailed(raw: String): MathDetailedResult? {
        val source = raw.trim().removeSuffix("?").trim()
        if (source.isEmpty() || source.length > 400) return null

        // Multi-line or semicolon-separated statements (e.g., "x = 3; y = 2x + 1; x^2 + y")
        val lines = source.split('\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size > 1 && source.none { it.equals('d') } ) {
            solveMultiStatement(lines)?.let { return it }
        } else if (lines.size > 1) {
            solveMultiStatement(lines)?.let { return it }
        }

        solveDerivative(source)?.let { return it }
        solvePercentPhrase(source)?.let { return it }
        solveUnitConversion(source)?.let { return it }

        val equalsAt = source.indexOf('=')
        if (equalsAt >= 0 && equalsAt < source.lastIndex && source.substring(equalsAt + 1).isNotBlank()) {
            if (source.indexOf('=', equalsAt + 1) >= 0) return null
            val left = source.substring(0, equalsAt)
            val right = source.substring(equalsAt + 1)
            if (left.isBlank() || right.isBlank()) return null

            solvePolynomialEquationDetailed(left, right)?.let { return it }
            solveExponentialEquationDetailed(left, right)?.let { return it }
            solveNumericEquationDetailed(left, right)?.let { return it }

            val lv = evaluate(normalize(left)) ?: return null
            val rv = evaluate(normalize(right)) ?: return null
            if (abs(lv - rv) <= 1e-8 * maxOf(1.0, abs(lv), abs(rv))) {
                val formatted = format(rv)
                return MathDetailedResult(
                    normalized = "${normalize(left)} = ${normalize(right)}",
                    answer = formatted,
                    steps = listOf("Both sides evaluate to $formatted (identity holds)"),
                    fractionHint = toFractionHint(rv),
                )
            }
            return null
        }

        val expr = normalize(source).trimEnd('=', '?')
        if (expr.isEmpty() || expr.none { it.isDigit() || it in "piPi" }) return null
        if (expr.none { it in "+-*/^%!()" || it.isLetter() }) return null
        val value = evaluate(expr) ?: return null
        val formatted = format(value)
        return MathDetailedResult(
            normalized = expr,
            answer = formatted,
            steps = listOf("Evaluated $expr = $formatted"),
            fractionHint = toFractionHint(value),
        )
    }

    private fun solveMultiStatement(lines: List<String>): MathDetailedResult? {
        val vars = HashMap<String, Double>()
        val steps = ArrayList<String>()
        for (idx in lines.indices) {
            val line = lines[idx]
            val eqIdx = line.indexOf('=')
            val isLast = idx == lines.lastIndex
            if (eqIdx > 0 && eqIdx < line.lastIndex) {
                val lhs = line.substring(0, eqIdx).trim()
                val rhs = line.substring(eqIdx + 1).trim()
                if (lhs.matches(Regex("[a-zA-Z]")) && lhs.lowercase(Locale.US) !in reservedNames) {
                    val valRhs = evaluate(normalize(rhs), vars) ?: return null
                    vars[lhs] = valRhs
                    steps += "$lhs = ${format(valRhs)}"
                    if (isLast) {
                        val ans = format(valRhs)
                        return MathDetailedResult(normalize(line), "$lhs = $ans", steps, toFractionHint(valRhs))
                    }
                    continue
                }
            }
            if (isLast) {
                val expr = normalize(line).trimEnd('=', '?')
                val v = evaluate(expr, vars) ?: return null
                val ans = format(v)
                steps += "$expr = $ans"
                return MathDetailedResult(expr, ans, steps, toFractionHint(v))
            }
        }
        return null
    }

    private fun solveDerivative(raw: String): MathDetailedResult? {
        val m = Regex("""^d/d([a-zA-Z])\s*\((.+)\)(?:\s*at\s*\1\s*=\s*([+-]?\d+(?:\.\d+)?))?=?$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw.trim().trimEnd('=', '?').trim()) ?: return null
        val vName = m.groupValues[1]
        val body = normalize(m.groupValues[2])
        val atVal = m.groupValues[3].toDoubleOrNull()
        if (atVal != null) {
            val h = 1e-6
            val f1 = evaluate(body, mapOf(vName to (atVal + h))) ?: return null
            val f0 = evaluate(body, mapOf(vName to (atVal - h))) ?: return null
            val deriv = (f1 - f0) / (2.0 * h)
            val ans = format(deriv)
            return MathDetailedResult(
                normalized = "d/d$vName($body) at $vName=$atVal",
                answer = ans,
                steps = listOf("Central difference around $vName = ${format(atVal)}", "f'(${format(atVal)}) ≈ $ans"),
                fractionHint = toFractionHint(deriv),
            )
        }
        // Symbolic derivative via PolyParser when body is polynomial up to degree 5 in vName
        val poly = runCatching { PolyParser(body, vName[0]).parse() }.getOrNull() ?: return null
        val terms = ArrayList<Pair<Int, Double>>()
        for (p in poly.degree() downTo 1) {
            val dc = poly.coeff(p) * p
            if (abs(dc) > 1e-11) terms += (p - 1) to dc
        }
        val sb = StringBuilder()
        terms.forEachIndexed { idx, (pow, coeff) ->
            val neg = coeff < 0
            val a = abs(coeff)
            if (idx > 0) sb.append(if (neg) " - " else " + ")
            else if (neg) sb.append("-")
            val cStr = format(a)
            when (pow) {
                0 -> sb.append(cStr)
                1 -> sb.append(if (abs(a - 1.0) < 1e-11) vName else "$cStr$vName")
                else -> sb.append(if (abs(a - 1.0) < 1e-11) "$vName^$pow" else "$cStr$vName^$pow")
            }
        }
        val ans = if (sb.isEmpty()) "0" else sb.toString()
        return MathDetailedResult(
            normalized = "d/d$vName($body)",
            answer = ans,
            steps = listOf("Differentiate each term with respect to $vName", "Result: $ans"),
        )
    }

    private fun solvePercentPhrase(raw: String): MathDetailedResult? {
        val clean = raw.trim().trimEnd('=', '?').trim()
        val ofM = Regex("""^([+-]?\d+(?:\.\d+)?)\s*%\s*of\s*([+-]?\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE).matchEntire(clean)
        if (ofM != null) {
            val pct = ofM.groupValues[1].toDoubleOrNull() ?: return null
            val base = ofM.groupValues[2].toDoubleOrNull() ?: return null
            val res = base * pct / 100.0
            val ans = format(res)
            return MathDetailedResult(clean, ans, listOf("$pct% × $base = ($pct / 100) × $base = $ans"), toFractionHint(res))
        }
        val offM = Regex("""^([+-]?\d+(?:\.\d+)?)\s*%\s*off\s*([+-]?\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE).matchEntire(clean)
        if (offM != null) {
            val pct = offM.groupValues[1].toDoubleOrNull() ?: return null
            val base = offM.groupValues[2].toDoubleOrNull() ?: return null
            val res = base * (1.0 - pct / 100.0)
            val ans = format(res)
            return MathDetailedResult(clean, ans, listOf("$base − ($pct% of $base) = $ans"), toFractionHint(res))
        }
        return null
    }

    private fun solveUnitConversion(raw: String): MathDetailedResult? {
        val clean = raw.trim().trimEnd('=', '?').trim()
        val m = Regex("""^([+-]?\d+(?:\.\d+)?)\s*([a-zA-Z]+)\s+(?:to|in)\s+([a-zA-Z]+)$""", RegexOption.IGNORE_CASE).matchEntire(clean) ?: return null
        val v = m.groupValues[1].toDoubleOrNull() ?: return null
        val from = m.groupValues[2].lowercase(Locale.US)
        val to = m.groupValues[3].lowercase(Locale.US)
        val out = when {
            (from == "c" || from == "celsius") && (to == "f" || to == "fahrenheit") -> v * 9.0 / 5.0 + 32.0
            (from == "f" || from == "fahrenheit") && (to == "c" || to == "celsius") -> (v - 32.0) * 5.0 / 9.0
            (from == "c" || from == "celsius") && (to == "k" || to == "kelvin") -> v + 273.15
            (from == "k" || from == "kelvin") && (to == "c" || to == "celsius") -> v - 273.15
            else -> {
                val len = mapOf("mm" to 0.001, "cm" to 0.01, "m" to 1.0, "km" to 1000.0, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.344)
                val mass = mapOf("mg" to 0.001, "g" to 1.0, "kg" to 1000.0, "oz" to 28.349523125, "lb" to 453.59237, "lbs" to 453.59237)
                val time = mapOf("s" to 1.0, "sec" to 1.0, "min" to 60.0, "h" to 3600.0, "hr" to 3600.0, "day" to 86400.0, "days" to 86400.0)
                val data = mapOf("b" to 1.0, "kb" to 1024.0, "mb" to 1048576.0, "gb" to 1073741824.0)
                val group = listOf(len, mass, time, data).firstOrNull { from in it && to in it } ?: return null
                v * group.getValue(from) / group.getValue(to)
            }
        }
        val ans = "${format(out)} $to"
        return MathDetailedResult(clean, ans, listOf("Convert ${format(v)} $from → $ans"))
    }

    private fun detectVariable(expr: String): Char? {
        val masked = Regex("""\b(?:pi|tau|phi|sqrt|cbrt|abs|sind|cosd|tand|sin|cos|tan|asin|acos|atan|ln|log10|log2|log|floor|ceil|round|exp|deg|rad|ncr|npr)\b""", RegexOption.IGNORE_CASE)
            .replace(expr, "")
        val vars = masked.filter { it.isLetter() }.toSet()
        return if (vars.size == 1) vars.first() else null
    }

    private fun evaluate(expression: String, vars: Map<String, Double> = emptyMap()): Double? = runCatching {
        val value = NumericParser(expression, vars).parse()
        require(value.isFinite())
        require(abs(value) < 1.0e100)
        value
    }.getOrNull()

    private fun solvePolynomialEquationDetailed(left: String, right: String): MathDetailedResult? = runCatching {
        val nl = normalize(left)
        val nr = normalize(right)
        val v = detectVariable("$nl=$nr") ?: 'x'
        val a = PolyParser(nl, v).parse()
        val b = PolyParser(nr, v).parse()
        val d = a - b
        val deg = d.degree()
        require(deg >= 1)
        if (deg == 1) {
            val c1 = d.coeff(1); val c0 = d.coeff(0)
            val ans = -c0 / c1
            require(ans.isFinite() && abs(ans) < 1.0e100)
            val f = format(ans)
            return@runCatching MathDetailedResult(
                normalized = "$nl = $nr",
                answer = "$v = $f",
                steps = listOf(
                    "Collect terms: ${format(c1)}$v + (${format(c0)}) = 0",
                    "$v = ${format(-c0)} / ${format(c1)} = $f",
                ),
                fractionHint = toFractionHint(ans),
            )
        }
        if (deg == 2) {
            val c2 = d.coeff(2); val c1 = d.coeff(1); val c0 = d.coeff(0)
            val disc = c1 * c1 - 4.0 * c2 * c0
            if (disc < -1e-9) {
                val real = -c1 / (2.0 * c2)
                val imag = abs(kotlin.math.sqrt(-disc) / (2.0 * c2))
                val rPart = if (abs(real) < 1e-10) "" else "${format(real)} ± "
                val iPart = if (abs(imag - 1.0) < 1e-9) "i" else "${format(imag)}i"
                val ansStr = if (abs(real) < 1e-10) "±$iPart" else "$rPart$iPart"
                return@runCatching MathDetailedResult(
                    normalized = "$nl = $nr",
                    answer = "$v = $ansStr",
                    steps = listOf(
                        "Standard form: ${format(c2)}$v² + (${format(c1)})$v + (${format(c0)}) = 0",
                        "Discriminant Δ = ${format(disc)} < 0 → complex conjugate roots",
                        "$v = $ansStr",
                    ),
                )
            }
            if (disc <= 1e-9) {
                val r = -c1 / (2.0 * c2)
                val f = format(r)
                return@runCatching MathDetailedResult(
                    normalized = "$nl = $nr",
                    answer = "$v = $f",
                    steps = listOf(
                        "Standard form: ${format(c2)}$v² + (${format(c1)})$v + (${format(c0)}) = 0",
                        "Discriminant Δ = 0 → double root $v = $f",
                    ),
                    fractionHint = toFractionHint(r),
                )
            }
            val root = kotlin.math.sqrt(disc)
            val r1 = (-c1 - root) / (2.0 * c2)
            val r2 = (-c1 + root) / (2.0 * c2)
            val lo = minOf(r1, r2)
            val hi = maxOf(r1, r2)
            return@runCatching MathDetailedResult(
                normalized = "$nl = $nr",
                answer = "$v = ${format(lo)}, ${format(hi)}",
                steps = listOf(
                    "Standard form: ${format(c2)}$v² + (${format(c1)})$v + (${format(c0)}) = 0",
                    "Discriminant Δ = ${format(disc)}",
                    "Roots: $v₁ = ${format(lo)}, $v₂ = ${format(hi)}",
                ),
            )
        }
        // Degree 3..5: numerical root scan + bisection + deduplication
        val roots = findPolynomialRoots(d)
        require(roots.isNotEmpty())
        val formatted = roots.joinToString(", ") { format(it) }
        MathDetailedResult(
            normalized = "$nl = $nr",
            answer = "$v = $formatted",
            steps = listOf("Degree-$deg polynomial reduced to standard form = 0", "Real root(s): $v = $formatted"),
            fractionHint = if (roots.size == 1) toFractionHint(roots[0]) else null,
        )
    }.getOrNull()

    private fun findPolynomialRoots(p: Poly): List<Double> {
        fun eval(x: Double): Double {
            var acc = 0.0
            for (k in Poly.MAX_DEG downTo 0) acc = acc * x + p.c[k]
            return acc
        }
        val roots = ArrayList<Double>()
        fun addRoot(r: Double) {
            if (!r.isFinite()) return
            if (roots.none { abs(it - r) < 1e-4 }) roots += r
        }
        var prevX = -150.0
        var prevY = eval(prevX)
        val step = 0.25
        var x = prevX + step
        while (x <= 150.0 + 1e-9) {
            val y = eval(x)
            if (abs(y) < 1e-10) addRoot(x)
            else if (prevY * y < 0.0) {
                var lo = prevX; var hi = x; var flo = prevY
                repeat(55) {
                    val mid = 0.5 * (lo + hi)
                    val fmid = eval(mid)
                    if (flo * fmid <= 0.0) { hi = mid } else { lo = mid; flo = fmid }
                }
                addRoot(0.5 * (lo + hi))
            }
            prevX = x; prevY = y; x += step
        }
        return roots.sorted()
    }

    private fun solveNumericEquationDetailed(left: String, right: String): MathDetailedResult? {
        val nl = normalize(left)
        val nr = normalize(right)
        val v = detectVariable("$nl=$nr") ?: return null
        val vs = v.toString()
        fun diff(x: Double): Double? {
            val l = evaluate(nl, mapOf(vs to x)) ?: return null
            val r = evaluate(nr, mapOf(vs to x)) ?: return null
            return l - r
        }
        val roots = ArrayList<Double>()
        fun addRoot(r: Double) {
            if (r.isFinite() && roots.none { abs(it - r) < 1e-4 }) roots += r
        }
        var prevX = -60.0
        var prevY = diff(prevX)
        val step = 0.25
        var x = prevX + step
        while (x <= 60.0 + 1e-9 && roots.size < 4) {
            val y = diff(x)
            val py = prevY
            if (y != null && py != null) {
                if (abs(y) < 1e-9) addRoot(x)
                else if (py * y < 0.0) {
                    var lo = prevX; var hi = x; var flo: Double = py
                    var valid = true
                    repeat(52) {
                        val mid = 0.5 * (lo + hi)
                        val fmid = diff(mid) ?: run { valid = false; return@repeat }
                        if (flo * fmid <= 0.0) { hi = mid } else { lo = mid; flo = fmid }
                    }
                    val root = 0.5 * (lo + hi)
                    val res = diff(root)
                    if (valid && res != null && abs(res) < 1e-4) addRoot(root)
                }
            }
            prevX = x; prevY = y; x += step
        }
        if (roots.isEmpty()) return null
        val formatted = roots.sorted().joinToString(", ") { format(it) }
        return MathDetailedResult(
            normalized = "$nl = $nr",
            answer = "$v = $formatted",
            steps = listOf(
                "Solve f($v) = $nl − ($nr) = 0 numerically",
                "Solution(s): $v = $formatted",
            ),
            fractionHint = if (roots.size == 1) toFractionHint(roots[0]) else null,
        )
    }

    private fun solveExponentialEquationDetailed(left: String, right: String): MathDetailedResult? {
        val nl = normalize(left)
        val nr = normalize(right)
        val m = Regex("""^(\d+(?:\.\d+)?)\^([a-zA-Z])$""").matchEntire(nl) ?: return null
        val base = m.groupValues[1].toDoubleOrNull() ?: return null
        val v = m.groupValues[2]
        val target = evaluate(nr) ?: return null
        if (base <= 0.0 || base == 1.0 || target <= 0.0) return null
        val ans = kotlin.math.ln(target) / kotlin.math.ln(base)
        val f = format(ans)
        return MathDetailedResult(
            normalized = "$nl = $nr",
            answer = "$v = $f",
            steps = listOf(
                "Take log base ${format(base)} of both sides",
                "$v = ln(${format(target)}) / ln(${format(base)}) = $f",
            ),
            fractionHint = toFractionHint(ans),
        )
    }

    private fun toFractionHint(value: Double): String? {
        if (!value.isFinite() || abs(value - kotlin.math.round(value)) < 1e-8) return null
        for (den in 2..64) {
            val num = kotlin.math.round(value * den)
            if (abs(value - num / den.toDouble()) < 1e-6) {
                fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
                val n = kotlin.math.round(num).toLong()
                val g = gcd(abs(n), den.toLong())
                return "${n / g}/${den / g}"
            }
        }
        return null
    }

    private fun format(value: Double): String {
        require(value.isFinite())
        val clean = if (abs(value) < 1e-11) 0.0 else value
        if (abs(clean - kotlin.math.round(clean)) < 1e-9 && abs(clean) < 9.0e15) return kotlin.math.round(clean).toLong().toString()
        return BigDecimal.valueOf(clean).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    private class NumericParser(private val s: String, private val vars: Map<String, Double> = emptyMap()) {
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
                    '%' -> { if (startsPrimary(i + 1)) { i++; value %= unary() } else break }
                    else -> if (startsPrimary(i)) value *= unary() else break
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
                val rawName = s.substring(start, i)
                val name = rawName.lowercase(Locale.US)
                vars[rawName]?.let { return it }
                vars.entries.firstOrNull { it.key.equals(rawName, ignoreCase = true) }?.let { return it.value }
                if (rawName.length == 1 && peek() != '(' && name !in setOf("e")) {
                    // Single letter unknown -> not evaluable numerically unless bound
                    error("Unbound variable")
                }
                return when (name) {
                    "pi" -> Math.PI
                    "tau" -> Math.PI * 2.0
                    "phi" -> (1.0 + kotlin.math.sqrt(5.0)) / 2.0
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
                while (peek() == ';') { i++; args += expression() }
                while (peek() == '.') { break }
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
                "log2" -> { require(args.size == 1 && x > 0); kotlin.math.ln(x) / kotlin.math.ln(2.0) }
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
                "ncr" -> {
                    require(args.size == 2)
                    val n = args[0].toInt(); val r = args[1].toInt()
                    require(n >= 0 && r in 0..n && n <= 30)
                    factorial(n.toDouble()) / (factorial(r.toDouble()) * factorial((n - r).toDouble()))
                }
                "npr" -> {
                    require(args.size == 2)
                    val n = args[0].toInt(); val r = args[1].toInt()
                    require(n >= 0 && r in 0..n && n <= 30)
                    factorial(n.toDouble()) / factorial((n - r).toDouble())
                }
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

    private class Poly(val c: DoubleArray = DoubleArray(MAX_DEG + 1)) {
        companion object {
            const val MAX_DEG = 5
            fun const(v: Double) = Poly().also { it.c[0] = v }
            fun linear(v: Double = 1.0) = Poly().also { it.c[1] = v }
        }
        fun coeff(p: Int): Double = c[p]
        fun degree(): Int {
            for (k in MAX_DEG downTo 1) if (abs(c[k]) > 1e-12) return k
            return 0
        }
        operator fun plus(other: Poly) = Poly(DoubleArray(MAX_DEG + 1) { c[it] + other.c[it] })
        operator fun minus(other: Poly) = Poly(DoubleArray(MAX_DEG + 1) { c[it] - other.c[it] })
        operator fun unaryMinus() = Poly(DoubleArray(MAX_DEG + 1) { -c[it] })
        fun times(other: Poly): Poly {
            val d1 = degree(); val d2 = other.degree()
            require(d1 + d2 <= MAX_DEG)
            val out = DoubleArray(MAX_DEG + 1)
            for (i in 0..d1) for (j in 0..d2) out[i + j] += c[i] * other.c[j]
            return Poly(out)
        }
        fun dividedBy(other: Poly): Poly {
            require(other.degree() == 0 && abs(other.c[0]) > 1e-12)
            return Poly(DoubleArray(MAX_DEG + 1) { c[it] / other.c[0] })
        }
    }

    private class PolyParser(private val s: String, private val variable: Char = 'x') {
        private var i = 0
        fun parse(): Poly { val result = expression(); require(i == s.length); return result }
        private fun expression(): Poly {
            var value = term()
            while (peek() == '+' || peek() == '-') {
                val op = s[i++]
                val rhs = term()
                value = if (op == '+') value + rhs else value - rhs
            }
            return value
        }
        private fun term(): Poly {
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
        private fun unary(): Poly = when (peek()) {
            '+' -> { i++; unary() }
            '-' -> { i++; -unary() }
            else -> power()
        }
        private fun power(): Poly {
            val base = primary()
            if (peek() == '^') {
                i++
                val exponent = unary()
                require(exponent.degree() == 0)
                val n = exponent.c[0]
                if (base.degree() == 0) return Poly.const(base.c[0].pow(n))
                val k = kotlin.math.round(n).toInt()
                require(abs(n - k) < 1e-11 && k in 0..Poly.MAX_DEG && base.degree() * k <= Poly.MAX_DEG)
                var acc = Poly.const(1.0)
                repeat(k) { acc = acc.times(base) }
                return acc
            }
            return base
        }
        private fun primary(): Poly {
            require(i < s.length)
            val p = peek()
            if (p == '(') { i++; val v = expression(); require(peek() == ')'); i++; return v }
            if (p.isLetter()) {
                val start = i
                while (i < s.length && (s[i].isLetter() || s[i].isDigit())) i++
                val raw = s.substring(start, i)
                val low = raw.lowercase(Locale.US)
                if (raw.length == 1 && raw[0].equals(variable, ignoreCase = true) && peek() != '(') return Poly.linear(1.0)
                when (low) {
                    "pi" -> return Poly.const(Math.PI)
                    "tau" -> return Poly.const(Math.PI * 2.0)
                    "phi" -> return Poly.const((1.0 + kotlin.math.sqrt(5.0)) / 2.0)
                    "e" -> return Poly.const(Math.E)
                }
                if (peek() == '(') {
                    val argStart = i
                    var depth = 0
                    while (i < s.length) {
                        when (s[i]) {
                            '(' -> depth++
                            ')' -> { depth--; if (depth == 0) { i++; break } }
                        }
                        i++
                    }
                    require(depth == 0)
                    val fullCall = raw + s.substring(argStart, i)
                    val numVal = evaluate(fullCall) ?: error("Non-constant function")
                    return Poly.const(numVal)
                }
                error("Unknown variable $raw")
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start)
            return Poly.const(s.substring(start, i).toDouble())
        }
        private fun startsPrimary(): Boolean = i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == '(' || s[i].isLetter())
        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'
    }
}
