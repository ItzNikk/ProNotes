package com.pronotes.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ============================================================================
// Data Contracts & Enums
// ============================================================================

enum class Difficulty { EASY, MEDIUM, HARD }

data class Flashcard(
    val front: String,
    val back: String,
    val difficulty: Difficulty = Difficulty.MEDIUM,
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class SummaryResult(
    val headline: String,
    val bulletPoints: List<String>,
    val keyTerms: List<String>,
    val wordCount: Int,
)

data class ChatMessage(
    val role: String,
    val content: String,
)

data class ChatResponse(
    val answer: String,
    val citations: List<String> = emptyList(),
    val engineLabel: String = "ProNotes On-Device Engine",
)

data class MathSolution(
    val input: String,
    val result: String,
    val steps: List<String>,
    val isEquation: Boolean,
    val variables: Map<String, Double> = emptyMap(),
)

enum class StickerStyle(val label: String, val badge: String) {
    CARTOON_3D("3D Cartoon", "🧸"),
    WATERCOLOR("Watercolor", "🎨"),
    POP_ART("Pop Art", "⚡"),
    ANIME("Anime Studio", "✨"),
    SKETCH("Sketch Art", "✏️"),
    MINIMAL("Minimal Vector", "📐"),
}

enum class CoverTheme(val label: String) {
    GALAXY("Cosmic Galaxy"),
    BOTANICAL("Botanical Garden"),
    MINIMAL("Minimal Studio"),
    VINTAGE("Vintage Leather"),
    GRADIENT("Aura Gradient"),
    GEOMETRIC("Geometric Bauhaus"),
}

enum class TemplateType(val label: String) {
    CORNELL("Cornell Notes"),
    WEEKLY_PLANNER("Weekly Planner"),
    HABIT_TRACKER("Habit Tracker"),
    GRID_DOT("Dot Grid Matrix"),
    MEETING_MINUTES("Meeting Minutes"),
    EISENHOWER_MATRIX("Eisenhower Matrix"),
}

// ============================================================================
// ProNotes Complete AI Engine
// ============================================================================

class ProNotesAiEngine(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // ------------------------------------------------------------------------
    // 1. Symbolic Math Solver (Pure Kotlin Computer Algebra / AST)
    // ------------------------------------------------------------------------

    sealed class Token {
        data class Number(val value: Double, val raw: String) : Token()
        data class Variable(val name: String) : Token()
        data class Operator(val op: Char) : Token()
        data class Function(val name: String) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object Equals : Token()
        data object Eof : Token()
    }

    private fun tokenizeMath(input: String): List<Token> {
        val clean = input
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("√", "sqrt ")
            .trim()

        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            when {
                c.isWhitespace() -> i++
                c in "+-*/^" -> {
                    tokens.add(Token.Operator(c))
                    i++
                }
                c == '=' -> {
                    tokens.add(Token.Equals)
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.RParen)
                    i++
                }
                c.isDigit() || (c == '.' && i + 1 < clean.length && clean[i + 1].isDigit()) -> {
                    val start = i
                    while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) i++
                    val numStr = clean.substring(start, i)
                    tokens.add(Token.Number(numStr.toDoubleOrNull() ?: 0.0, numStr))
                }
                c.isLetter() -> {
                    val start = i
                    while (i < clean.length && clean[i].isLetter()) i++
                    val word = clean.substring(start, i).lowercase(Locale.ROOT)
                    if (word in listOf("sqrt", "sin", "cos", "tan", "abs", "log", "ln")) {
                        tokens.add(Token.Function(word))
                    } else {
                        for (ch in word) {
                            tokens.add(Token.Variable(ch.toString()))
                        }
                    }
                }
                else -> i++
            }
        }
        tokens.add(Token.Eof)
        return tokens
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) abs(a) else gcd(b, a % b)

    private fun simplifyFraction(numerator: Long, denominator: Long): Pair<Long, Long> {
        if (denominator == 0L) return numerator to 0L
        val g = gcd(numerator, denominator)
        var n = numerator / g
        var d = denominator / g
        if (d < 0) {
            n = -n
            d = -d
        }
        return n to d
    }

    private fun formatNumber(v: Double): String {
        return if (abs(v - v.roundToInt()) < 1e-9) {
            v.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }

    fun solveMath(expression: String): MathSolution {
        val raw = expression.trim()
        val steps = mutableListOf<String>()

        if (raw.isEmpty()) {
            return MathSolution(raw, "0", listOf("Empty expression provided."), false)
        }

        // Percentage calculations e.g. "20% of 150" or "15% off 80"
        val percentRegex = Regex("""(?i)^\s*(\d+(?:\.\d+)?)\s*%\s*(?:of|off|\*)\s*(\d+(?:\.\d+)?)\s*$""")
        val percentMatch = percentRegex.matchEntire(raw)
        if (percentMatch != null) {
            val pVal = percentMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val baseVal = percentMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val isDiscount = raw.contains("off", ignoreCase = true)
            val computed = (pVal / 100.0) * baseVal
            val finalRes = if (isDiscount) baseVal - computed else computed
            steps.add("Convert percentage to fraction: $pVal% = ${pVal / 100.0}")
            if (isDiscount) {
                steps.add("Calculate discount amount: ${pVal / 100.0} * $baseVal = ${formatNumber(computed)}")
                steps.add("Subtract from base: $baseVal - ${formatNumber(computed)} = ${formatNumber(finalRes)}")
            } else {
                steps.add("Multiply fraction by base: ${pVal / 100.0} * $baseVal = ${formatNumber(computed)}")
            }
            return MathSolution(raw, formatNumber(finalRes), steps, false)
        }

        // Unit conversion e.g. "5 km to miles" or "100 c to f"
        val unitSol = trySolveUnitConversion(raw)
        if (unitSol != null) return unitSol

        // System of two linear equations e.g. "2x + y = 10, x - y = 1" or "2x + y = 10 and x - y = 1"
        if ((raw.contains(",") || raw.contains(" and ", ignoreCase = true)) && raw.count { it == '=' } == 2) {
            val eqParts = raw.split(Regex("[,]|(?i)\\s+and\\s+")).map { it.trim() }.filter { it.contains("=") }
            if (eqParts.size == 2) {
                val sysSol = trySolveSystem2(eqParts[0], eqParts[1], raw)
                if (sysSol != null) return sysSol
            }
        }

        // Equation checking
        if (raw.contains("=")) {
            val parts = raw.split("=").map { it.trim() }
            if (parts.size == 2) {
                return solveEquation(parts[0], parts[1], raw)
            }
        }

        // Arithmetic evaluation
        steps.add("Standardize input: $raw")
        val tokens = tokenizeMath(raw)

        // Check if fraction simplification applies e.g. "12/18"
        val fractionRegex = Regex("""^\s*(-?\d+)\s*/\s*(-?\d+)\s*$""")
        val fracMatch = fractionRegex.matchEntire(raw)
        if (fracMatch != null) {
            val n = fracMatch.groupValues[1].toLongOrNull()
            val d = fracMatch.groupValues[2].toLongOrNull()
            if (n != null && d != null && d != 0L) {
                val (sn, sd) = simplifyFraction(n, d)
                val g = gcd(n, d)
                steps.add("Compute greatest common divisor: GCD($n, $d) = $g")
                steps.add("Divide numerator and denominator by $g: $sn / $sd")
                val decimal = n.toDouble() / d.toDouble()
                steps.add("Decimal approximation: ${formatNumber(decimal)}")
                return MathSolution(
                    input = raw,
                    result = if (sd == 1L) "$sn" else "$sn/$sd (approx ${formatNumber(decimal)})",
                    steps = steps,
                    isEquation = false,
                )
            }
        }

        // Parse and evaluate arithmetic expression
        return try {
            val parser = MathExprParser(tokens)
            val ast = parser.parse()
            steps.add("Construct Abstract Syntax Tree: ${ast.repr()}")
            val (result, evalSteps) = ast.evaluateWithSteps()
            steps.addAll(evalSteps)
            steps.add("Final evaluated result: ${formatNumber(result)}")

            MathSolution(
                input = raw,
                result = formatNumber(result),
                steps = steps,
                isEquation = false,
            )
        } catch (e: Exception) {
            steps.add("Parsing standard arithmetic failed: ${e.message}")
            MathSolution(raw, "Error", steps, false)
        }
    }

    private fun trySolveUnitConversion(raw: String): MathSolution? {
        val convRegex = Regex("""(?i)^\s*(\d+(?:\.\d+)?)\s*([a-zA-Z°]+)\s*(?:to|in|into)\s*([a-zA-Z°]+)\s*$""")
        val m = convRegex.matchEntire(raw) ?: return null
        val value = m.groupValues[1].toDoubleOrNull() ?: return null
        val fromUnit = m.groupValues[2].lowercase(Locale.ROOT)
        val toUnit = m.groupValues[3].lowercase(Locale.ROOT)

        val steps = mutableListOf<String>()
        steps.add("Unit conversion query: $value $fromUnit → $toUnit")

        val toMeters = mapOf(
            "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
            "m" to 1.0, "meter" to 1.0, "meters" to 1.0,
            "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
            "mm" to 0.001, "millimeter" to 0.001,
            "mi" to 1609.344, "mile" to 1609.344, "miles" to 1609.344,
            "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
            "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
            "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144,
        )

        val toGrams = mapOf(
            "kg" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0,
            "g" to 1.0, "gram" to 1.0, "grams" to 1.0,
            "mg" to 0.001, "milligram" to 0.001,
            "lb" to 453.592, "lbs" to 453.592, "pound" to 453.592, "pounds" to 453.592,
            "oz" to 28.3495, "ounce" to 28.3495, "ounces" to 28.3495,
        )

        val toSeconds = mapOf(
            "s" to 1.0, "sec" to 1.0, "second" to 1.0, "seconds" to 1.0,
            "min" to 60.0, "minute" to 60.0, "minutes" to 60.0,
            "h" to 3600.0, "hr" to 3600.0, "hour" to 3600.0, "hours" to 3600.0,
            "d" to 86400.0, "day" to 86400.0, "days" to 86400.0,
        )

        if ((fromUnit in listOf("c", "celsius", "°c")) && (toUnit in listOf("f", "fahrenheit", "°f"))) {
            val f = value * 9.0 / 5.0 + 32.0
            steps.add("Apply formula: °F = (°C × 9/5) + 32")
            steps.add("Compute: ($value × 1.8) + 32 = ${formatNumber(f)}°F")
            return MathSolution(raw, "${formatNumber(f)} °F", steps, false)
        }
        if ((fromUnit in listOf("f", "fahrenheit", "°f")) && (toUnit in listOf("c", "celsius", "°c"))) {
            val c = (value - 32.0) * 5.0 / 9.0
            steps.add("Apply formula: °C = (°F - 32) × 5/9")
            steps.add("Compute: ($value - 32) × 0.5556 = ${formatNumber(c)}°C")
            return MathSolution(raw, "${formatNumber(c)} °C", steps, false)
        }

        if (fromUnit in toMeters && toUnit in toMeters) {
            val meters = value * toMeters[fromUnit]!!
            val result = meters / toMeters[toUnit]!!
            steps.add("Convert $value $fromUnit to standard base (meters): $meters m")
            steps.add("Convert meters to $toUnit: $meters / ${toMeters[toUnit]!!} = ${formatNumber(result)}")
            return MathSolution(raw, "${formatNumber(result)} $toUnit", steps, false)
        }

        if (fromUnit in toGrams && toUnit in toGrams) {
            val grams = value * toGrams[fromUnit]!!
            val result = grams / toGrams[toUnit]!!
            steps.add("Convert $value $fromUnit to standard base (grams): $grams g")
            steps.add("Convert grams to $toUnit: $grams / ${toGrams[toUnit]!!} = ${formatNumber(result)}")
            return MathSolution(raw, "${formatNumber(result)} $toUnit", steps, false)
        }

        if (fromUnit in toSeconds && toUnit in toSeconds) {
            val secs = value * toSeconds[fromUnit]!!
            val result = secs / toSeconds[toUnit]!!
            steps.add("Convert $value $fromUnit to standard base (seconds): $secs s")
            steps.add("Convert seconds to $toUnit: $secs / ${toSeconds[toUnit]!!} = ${formatNumber(result)}")
            return MathSolution(raw, "${formatNumber(result)} $toUnit", steps, false)
        }

        return null
    }

    private fun trySolveSystem2(eq1: String, eq2: String, fullInput: String): MathSolution? {
        val steps = mutableListOf<String>()
        steps.add("System of linear equations detected:\n  (1) $eq1\n  (2) $eq2")

        fun parse2Var(eq: String): Triple<Double, Double, Double>? {
            val parts = eq.split("=")
            if (parts.size != 2) return null
            val left = parts[0].trim()
            val right = parts[1].trim()
            val rightConst = right.toDoubleOrNull() ?: 0.0

            val (a, leftConst1) = parseLinearCoeffs(left, "x")
            val (b, _) = parseLinearCoeffs(left, "y")
            val c = rightConst - leftConst1
            return Triple(a, b, c)
        }

        val eqA = parse2Var(eq1) ?: return null
        val eqB = parse2Var(eq2) ?: return null

        val (a1, b1, c1) = eqA
        val (a2, b2, c2) = eqB

        steps.add("Standard form:\n  ${a1}x + ${b1}y = $c1\n  ${a2}x + ${b2}y = $c2")

        val det = a1 * b2 - a2 * b1
        steps.add("Calculate system determinant D = (${a1})(${b2}) - (${a2})(${b1}) = $det")

        if (abs(det) < 1e-9) {
            steps.add("Determinant is zero: The lines are either parallel (no solution) or coincident (infinite solutions).")
            return MathSolution(fullInput, "No unique solution", steps, true)
        }

        val dx = c1 * b2 - c2 * b1
        val dy = a1 * c2 - a2 * c1
        val x = dx / det
        val y = dy / det

        steps.add("Dx = (${c1})(${b2}) - (${c2})(${b1}) = $dx")
        steps.add("Dy = (${a1})(${c2}) - (${a2})(${c1}) = $dy")
        steps.add("Solve by Cramer's rule: x = Dx / D = $dx / $det = ${formatNumber(x)}")
        steps.add("Solve by Cramer's rule: y = Dy / D = $dy / $det = ${formatNumber(y)}")

        return MathSolution(
            input = fullInput,
            result = "x = ${formatNumber(x)}, y = ${formatNumber(y)}",
            steps = steps,
            isEquation = true,
            variables = mapOf("x" to x, "y" to y),
        )
    }

    private fun solveEquation(leftSide: String, rightSide: String, fullInput: String): MathSolution {
        val steps = mutableListOf<String>()
        steps.add("Equation detected: $leftSide = $rightSide")

        val quadraticRegex = Regex("""(-?\d*(?:\.\d+)?)?\s*([a-zA-Z])\s*(?:\^2|²)\s*([+-]\s*\d*(?:\.\d+)?\s*[a-zA-Z])?\s*([+-]\s*\d+(?:\.\d+)?)?\s*=\s*0""")
        val quadMatch = quadraticRegex.matchEntire("$leftSide = $rightSide")
        if (quadMatch != null) {
            val varName = quadMatch.groupValues[2]
            var aStr = quadMatch.groupValues[1].replace(" ", "")
            if (aStr.isEmpty() || aStr == "+") aStr = "1"
            if (aStr == "-") aStr = "-1"
            val a = aStr.toDoubleOrNull() ?: 1.0

            var bStr = quadMatch.groupValues[3].replace(" ", "").replace(varName, "")
            if (bStr.isEmpty()) bStr = "0"
            else if (bStr == "+") bStr = "1"
            else if (bStr == "-") bStr = "-1"
            val b = bStr.toDoubleOrNull() ?: 0.0

            val cStr = quadMatch.groupValues[4].replace(" ", "")
            val c = cStr.toDoubleOrNull() ?: 0.0

            steps.add("Recognized standard quadratic form: ${a}${varName}² + (${b})${varName} + (${c}) = 0")
            val discriminant = b * b - 4 * a * c
            steps.add("Calculate discriminant Δ = b² - 4ac = (${b})² - 4*(${a})*(${c}) = $discriminant")

            if (discriminant > 0) {
                val root1 = (-b + sqrt(discriminant)) / (2 * a)
                val root2 = (-b - sqrt(discriminant)) / (2 * a)
                steps.add("Δ > 0: Two distinct real roots exist.")
                steps.add("$varName₁ = (-b + √Δ) / 2a = (-($b) + ${formatNumber(sqrt(discriminant))}) / ${2 * a} = ${formatNumber(root1)}")
                steps.add("$varName₂ = (-b - √Δ) / 2a = (-($b) - ${formatNumber(sqrt(discriminant))}) / ${2 * a} = ${formatNumber(root2)}")
                return MathSolution(
                    input = fullInput,
                    result = "$varName = ${formatNumber(root1)}, $varName = ${formatNumber(root2)}",
                    steps = steps,
                    isEquation = true,
                    variables = mapOf("${varName}1" to root1, "${varName}2" to root2),
                )
            } else if (abs(discriminant) < 1e-9) {
                val root = -b / (2 * a)
                steps.add("Δ = 0: One repeated real root.")
                steps.add("$varName = -b / 2a = -($b) / ${2 * a} = ${formatNumber(root)}")
                return MathSolution(
                    input = fullInput,
                    result = "$varName = ${formatNumber(root)}",
                    steps = steps,
                    isEquation = true,
                    variables = mapOf(varName to root),
                )
            } else {
                val real = -b / (2 * a)
                val imag = sqrt(-discriminant) / (2 * a)
                steps.add("Δ < 0: Complex conjugate roots exist.")
                val res = "$varName = ${formatNumber(real)} ± ${formatNumber(abs(imag))}i"
                steps.add(res)
                return MathSolution(input = fullInput, result = res, steps = steps, isEquation = true)
            }
        }

        val varRegex = Regex("""[a-zA-Z]""")
        val detectedVar = varRegex.find(fullInput)?.value ?: "x"

        val (leftCoeff, leftConst) = parseLinearCoeffs(leftSide, detectedVar)
        val (rightCoeff, rightConst) = parseLinearCoeffs(rightSide, detectedVar)

        steps.add("Group terms: (${leftCoeff})$detectedVar + (${leftConst}) = (${rightCoeff})$detectedVar + (${rightConst})")
        val netCoeff = leftCoeff - rightCoeff
        val netConst = rightConst - leftConst

        steps.add("Move variable terms to left side: (${leftCoeff} - ${rightCoeff})$detectedVar = ${netCoeff}$detectedVar")
        steps.add("Move constants to right side: ${rightConst} - (${leftConst}) = $netConst")
        steps.add("Simplified linear form: ${netCoeff}$detectedVar = $netConst")

        if (abs(netCoeff) < 1e-9) {
            if (abs(netConst) < 1e-9) {
                steps.add("Identity 0 = 0 holds: Infinitely many solutions.")
                return MathSolution(fullInput, "Infinite solutions (Identity)", steps, true)
            } else {
                steps.add("Contradiction: 0 = $netConst (No solution).")
                return MathSolution(fullInput, "No solution", steps, true)
            }
        }

        val sol = netConst / netCoeff
        steps.add("Divide both sides by ${formatNumber(netCoeff)}: $detectedVar = ${formatNumber(netConst)} / ${formatNumber(netCoeff)}")
        steps.add("Solution: $detectedVar = ${formatNumber(sol)}")

        return MathSolution(
            input = fullInput,
            result = "$detectedVar = ${formatNumber(sol)}",
            steps = steps,
            isEquation = true,
            variables = mapOf(detectedVar to sol),
        )
    }

    private fun parseLinearCoeffs(side: String, variable: String): Pair<Double, Double> {
        var coeff = 0.0
        var constVal = 0.0
        val clean = side.replace("-", "+-").trim()
        val parts = clean.split("+").map { it.trim() }.filter { it.isNotEmpty() }

        for (part in parts) {
            if (part.contains(variable, ignoreCase = true)) {
                val cleanVar = part.replace(Regex("(?i)$variable"), "").trim()
                val c = when {
                    cleanVar.isEmpty() -> 1.0
                    cleanVar == "-" -> -1.0
                    cleanVar == "+" -> 1.0
                    else -> cleanVar.toDoubleOrNull() ?: 1.0
                }
                coeff += c
            } else {
                constVal += (part.toDoubleOrNull() ?: 0.0)
            }
        }
        return coeff to constVal
    }

    private sealed class Expr {
        abstract fun repr(): String
        abstract fun evaluateWithSteps(): Pair<Double, List<String>>

        data class Number(val v: Double) : Expr() {
            override fun repr() = if (v == v.roundToInt().toDouble()) "${v.roundToInt()}" else "$v"
            override fun evaluateWithSteps(): Pair<Double, List<String>> = v to emptyList<String>()
        }

        data class Binary(val left: Expr, val op: Char, val right: Expr) : Expr() {
            override fun repr() = "(${left.repr()} $op ${right.repr()})"
            override fun evaluateWithSteps(): Pair<Double, List<String>> {
                val (lVal, lSteps) = left.evaluateWithSteps()
                val (rVal, rSteps) = right.evaluateWithSteps()
                val steps = mutableListOf<String>()
                steps.addAll(lSteps)
                steps.addAll(rSteps)

                val res = when (op) {
                    '+' -> lVal + rVal
                    '-' -> lVal - rVal
                    '*' -> lVal * rVal
                    '/' -> if (rVal != 0.0) lVal / rVal else Double.NaN
                    '^' -> lVal.pow(rVal)
                    else -> 0.0
                }
                steps.add("Evaluate: $lVal $op $rVal = $res")
                return res to steps
            }
        }

        data class FunctionCall(val fn: String, val arg: Expr) : Expr() {
            override fun repr() = "$fn(${arg.repr()})"
            override fun evaluateWithSteps(): Pair<Double, List<String>> {
                val (argVal, argSteps) = arg.evaluateWithSteps()
                val steps = mutableListOf<String>()
                steps.addAll(argSteps)
                val res = when (fn) {
                    "sqrt" -> sqrt(argVal)
                    "abs" -> abs(argVal)
                    "sin" -> sin(argVal)
                    "cos" -> cos(argVal)
                    "tan" -> kotlin.math.tan(argVal)
                    "ln" -> kotlin.math.ln(argVal)
                    "log" -> kotlin.math.log10(argVal)
                    else -> argVal
                }
                steps.add("Function $fn($argVal) = $res")
                return res to steps
            }
        }
    }

    private class MathExprParser(private val tokens: List<Token>) {
        private var idx = 0

        private fun peek(): Token = if (idx < tokens.size) tokens[idx] else Token.Eof
        private fun consume(): Token = tokens[idx++]

        fun parse(): Expr = parseAdditive()

        private fun parseAdditive(): Expr {
            var left = parseMultiplicative()
            while (true) {
                val t = peek()
                if (t is Token.Operator && (t.op == '+' || t.op == '-')) {
                    consume()
                    val right = parseMultiplicative()
                    left = Expr.Binary(left, t.op, right)
                } else break
            }
            return left
        }

        private fun parseMultiplicative(): Expr {
            var left = parseExponential()
            while (true) {
                val t = peek()
                if (t is Token.Operator && (t.op == '*' || t.op == '/')) {
                    consume()
                    val right = parseExponential()
                    left = Expr.Binary(left, t.op, right)
                } else break
            }
            return left
        }

        private fun parseExponential(): Expr {
            val base = parsePrimary()
            if (peek() is Token.Operator && (peek() as Token.Operator).op == '^') {
                consume()
                val exponent = parseExponential()
                return Expr.Binary(base, '^', exponent)
            }
            return base
        }

        private fun parsePrimary(): Expr {
            val t = consume()
            return when (t) {
                is Token.Number -> Expr.Number(t.value)
                is Token.Function -> {
                    if (peek() is Token.LParen) consume()
                    val arg = parseAdditive()
                    if (peek() is Token.RParen) consume()
                    Expr.FunctionCall(t.name, arg)
                }
                is Token.LParen -> {
                    val inside = parseAdditive()
                    if (peek() is Token.RParen) consume()
                    inside
                }
                is Token.Operator -> {
                    if (t.op == '-') {
                        val expr = parsePrimary()
                        Expr.Binary(Expr.Number(0.0), '-', expr)
                    } else parsePrimary()
                }
                else -> Expr.Number(0.0)
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. Flashcard Generator (Heuristic + Semantic Extraction)
    // ------------------------------------------------------------------------

    fun generateFlashcards(notesText: String, count: Int = 15): List<Flashcard> {
        val clean = notesText.trim()
        if (clean.isEmpty()) return emptyList()

        val cards = mutableListOf<Flashcard>()
        val seenQuestions = mutableSetOf<String>()

        val defPatterns = listOf(
            Regex("""^(.{2,50}?)\s+(?:is|are|was|were)\s+(?:defined as|called|known as|termed)\s+(.{8,260})$""", RegexOption.IGNORE_CASE),
            Regex("""^(.{2,50}?)\s+(?:refers to|means|denotes|is defined as|is the|are the|is an?|are)\s+(.{8,260})$""", RegexOption.IGNORE_CASE),
            Regex("""^([A-Z][\w\s\-()]{1,45}):\s+(.{8,260})$"""),
            Regex("""^([A-Z][\w\s\-()]{1,45})\s+[-–—]\s+(.{8,260})$"""),
        )

        val sentences = clean.replace('\n', ' ')
            .split(Regex("(?<=[.!?])\\s+(?=[A-Z0-9])"))
            .map { it.trim() }
            .filter { it.length in 20..320 }

        for (s in sentences) {
            for (pattern in defPatterns) {
                val m = pattern.find(s) ?: continue
                val term = m.groupValues[1].trim().trim(',', ';').removePrefix("The ").removePrefix("A ")
                val def = m.groupValues[2].trim().replaceFirstChar { it.uppercase() }

                if (term.split(" ").size <= 5 && term.length >= 2) {
                    val q = "What is $term?"
                    if (seenQuestions.add(q.lowercase())) {
                        val diff = when {
                            term.length > 15 || def.length > 150 -> Difficulty.HARD
                            term.length < 8 -> Difficulty.EASY
                            else -> Difficulty.MEDIUM
                        }
                        cards.add(Flashcard(q, def, diff))
                        break
                    }
                }
            }
            if (cards.size >= count) break
        }

        if (cards.size < count) {
            val keyWords = extractKeyTerms(clean, 40)
            for (s in sentences) {
                if (cards.size >= count) break
                val term = keyWords.firstOrNull { kw -> s.contains(Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)) } ?: continue
                val q = "Fill in the blank:\n" + s.replace(Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE), "_____")
                if (seenQuestions.add(q.lowercase())) {
                    cards.add(Flashcard(q, term, Difficulty.MEDIUM))
                }
            }
        }

        return cards.take(count)
    }

    // ------------------------------------------------------------------------
    // 3. Quiz Generator (MCQ with Smart Distractor Construction)
    // ------------------------------------------------------------------------

    fun generateQuiz(notesText: String, count: Int = 10): List<QuizQuestion> {
        val clean = notesText.trim()
        if (clean.isEmpty()) return emptyList()

        val cards = generateFlashcards(clean, count * 2)
        val allFronts = cards.map { it.front.removePrefix("What is ").removeSuffix("?") }.distinct()
        val questions = mutableListOf<QuizQuestion>()
        val rnd = Random(clean.hashCode())

        for (card in cards) {
            if (questions.size >= count) break
            val term = card.front.removePrefix("What is ").removeSuffix("?")

            val distractors = allFronts.filter { !it.equals(term, true) }.shuffled(rnd).take(3)
            if (distractors.size >= 2) {
                val opts = (distractors + term).shuffled(rnd)
                val correctIdx = opts.indexOf(term)
                questions.add(
                    QuizQuestion(
                        question = "Which concept is described as:\n\"${card.back.take(180)}\"?",
                        options = opts,
                        correctIndex = correctIdx,
                        explanation = "$term is defined as ${card.back}.",
                    ),
                )
            }
        }

        val sentences = clean.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length in 35..240 }
        val terms = extractKeyTerms(clean, 30)

        for (s in sentences) {
            if (questions.size >= count) break
            val t = terms.firstOrNull { kw -> s.contains(Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)) } ?: continue
            val distractors = terms.filter { !it.equals(t, true) }.shuffled(rnd).take(3)
            if (distractors.size >= 3) {
                val opts = (distractors + t).shuffled(rnd)
                val correctIdx = opts.indexOf(t)
                questions.add(
                    QuizQuestion(
                        question = "Complete the statement:\n\"" + s.replace(Regex("\\b${Regex.escape(t)}\\b", RegexOption.IGNORE_CASE), "_____") + "\"",
                        options = opts,
                        correctIndex = correctIdx,
                        explanation = "The correct term is '$t'. Full sentence: $s",
                    ),
                )
            }
        }

        return questions.take(count)
    }

    // ------------------------------------------------------------------------
    // 4. Note Summarizer
    // ------------------------------------------------------------------------

    fun summarize(text: String, maxPoints: Int = 6): SummaryResult {
        val clean = text.trim()
        if (clean.isEmpty()) {
            return SummaryResult("Empty notes", emptyList(), emptyList(), 0)
        }

        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = clean.replace('\n', ' ')
            .split(Regex("(?<=[.!?])\\s+(?=[A-Z0-9])"))
            .map { it.trim() }
            .filter { it.length in 25..350 }

        val keyTerms = extractKeyTerms(clean, 12)
        val termFrequencies = HashMap<String, Int>()
        for (w in words) {
            val lw = w.lowercase(Locale.ROOT)
            if (lw.length > 3) termFrequencies[lw] = (termFrequencies[lw] ?: 0) + 1
        }

        val scoredSentences = sentences.mapIndexed { idx, s ->
            val sWords = s.lowercase(Locale.ROOT).split(Regex("\\W+"))
            val score = sWords.sumOf { termFrequencies[it] ?: 0 }.toDouble() / (sWords.size + 4) +
                (if (idx < 2) 1.2 else 0.0) +
                (if (s.contains("is") || s.contains("defined") || s.contains("key")) 1.5 else 0.0)
            idx to score
        }

        val topSentences = scoredSentences
            .sortedByDescending { it.second }
            .take(maxPoints)
            .sortedBy { it.first }
            .map { sentences[it.first] }

        val headline = if (topSentences.isNotEmpty()) {
            topSentences.first().take(80).trimEnd('.') + "…"
        } else "Summary of ${words.size} words."

        return SummaryResult(
            headline = headline,
            bulletPoints = topSentences,
            keyTerms = keyTerms,
            wordCount = words.size,
        )
    }

    private fun extractKeyTerms(text: String, limit: Int = 20): List<String> {
        val stop = setOf(
            "the", "this", "that", "these", "those", "it", "they", "there", "here",
            "which", "what", "who", "a", "an", "and", "or", "in", "on", "of", "to",
            "for", "with", "by", "as", "at", "from", "but", "if", "so", "we", "he",
            "she", "his", "her", "their", "its", "our", "you", "your", "also", "is",
            "are", "was", "were", "been", "have", "has", "had", "will", "would",
        )
        val counts = HashMap<String, Int>()
        Regex("[A-Za-z][A-Za-z\\-]{3,}").findAll(text).forEach { m ->
            val w = m.value
            val lw = w.lowercase(Locale.ROOT)
            if (lw !in stop) {
                counts[w] = (counts[w] ?: 0) + 1
            }
        }
        return counts.entries
            .sortedByDescending { it.value * (if (it.key[0].isUpperCase()) 2 else 1) + it.key.length / 4 }
            .map { it.key }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(limit)
    }

    // ------------------------------------------------------------------------
    // 5. PDF & Notes Chat (Sliding-window RAG + Conversational Context)
    // ------------------------------------------------------------------------

    suspend fun chat(query: String, documentContext: String, history: List<ChatMessage> = emptyList()): ChatResponse =
        withContext(Dispatchers.Default) {
            val q = query.trim()
            if (q.isEmpty()) {
                return@withContext ChatResponse("Please ask a question about your study notes or any concept!")
            }

            val mathPats = Regex("""^(\s*solve\s+|calculate\s+|evaluate\s+)?(-?\d.*[+\-*/=^√].*)$""", RegexOption.IGNORE_CASE)
            if (mathPats.matches(q) || (q.contains("=") && q.any { it.isDigit() })) {
                val mathExpr = q.replace(Regex("(?i)^(solve|calculate|evaluate)\\s+"), "").trim()
                val sol = solveMath(mathExpr)
                val formattedAnswer = buildString {
                    appendLine("🔢 **Math Solution: ${sol.input}**")
                    appendLine("• **Result:** `${sol.result}`")
                    appendLine()
                    appendLine("**Step-by-step reasoning:**")
                    sol.steps.forEachIndexed { i, step -> appendLine("${i + 1}. $step") }
                }.trim()
                return@withContext ChatResponse(formattedAnswer, emptyList(), "ProNotes Symbolic Math Engine")
            }

            if (q.contains("quiz", ignoreCase = true) || q.contains("test me", ignoreCase = true)) {
                val questions = generateQuiz(documentContext, 3)
                if (questions.isNotEmpty()) {
                    val qText = buildString {
                        appendLine("🎯 **Here is a quick concept quiz from your notes:**\n")
                        questions.forEachIndexed { i, item ->
                            appendLine("**Q${i + 1}: ${item.question}**")
                            item.options.forEachIndexed { oi, opt ->
                                val letter = ('A' + oi)
                                appendLine("  $letter) $opt")
                            }
                            appendLine("  *Answer:* ${'A' + item.correctIndex} (${item.explanation})\n")
                        }
                    }.trim()
                    return@withContext ChatResponse(qText, listOf("Extracted from current note context"), "ProNotes Quiz Engine")
                }
            }

            val onlineReply = callFreeChatAi(query = q, context = documentContext, history = history)
            if (!onlineReply.isNullOrBlank()) {
                return@withContext ChatResponse(onlineReply, listOf("Verified with ProNotes AI Assistant"), "ProNotes Cloud Tutor")
            }

            val qTokens = q.lowercase(Locale.ROOT).split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }.toSet()
            val passages = documentContext.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }

            val rankedPassages = passages.map { passage ->
                val pLow = passage.lowercase(Locale.ROOT)
                val score = qTokens.count { pLow.contains(it) } + if (pLow.contains(q.lowercase(Locale.ROOT))) 4 else 0
                passage to score
            }.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)

            if (rankedPassages.isNotEmpty()) {
                val topAnswer = rankedPassages.first().first
                val citations = rankedPassages.mapIndexed { idx, _ -> "Section #${idx + 1}" }
                val reply = buildString {
                    appendLine("✨ **Relevant note context:**")
                    appendLine()
                    appendLine(topAnswer)
                    if (rankedPassages.size > 1) {
                        appendLine()
                        appendLine("**Additional reference:**")
                        appendLine(rankedPassages[1].first.take(200) + "…")
                    }
                }.trim()
                return@withContext ChatResponse(reply, citations, "ProNotes Local RAG")
            }

            ChatResponse(
                answer = "I've reviewed your notes for \"$q\". While there wasn't an exact match, try asking about specific terms, definitions, or equations in your note!",
                citations = emptyList(),
                engineLabel = "ProNotes Semantic Assistant",
            )
        }

    private suspend fun callFreeChatAi(query: String, context: String, history: List<ChatMessage>): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val sysPrompt = "You are the smart, helpful study tutor inside the ProNotes app. Answer concisely and accurately based on the user's study notes. If formulas or math are needed, provide clear steps."
                val userContent = "Context Notes:\n${context.take(3500)}\n\nQuestion: $query"

                val messages = buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", sysPrompt) })
                    history.takeLast(4).forEach { msg ->
                        add(buildJsonObject { put("role", msg.role); put("content", msg.content.take(500)) })
                    }
                    add(buildJsonObject { put("role", "user"); put("content", userContent) })
                }

                val payload = buildJsonObject {
                    put("model", "openai")
                    put("messages", messages)
                }.toString()

                val conn = (URL("https://text.pollinations.ai/openai").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 7_000
                    readTimeout = 14_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "ProNotes-Android/10.0")
                }

                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (raw.startsWith("{")) {
                        val root = json.parseToJsonElement(raw).jsonObject
                        root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content?.trim()
                    } else raw.takeIf { it.isNotBlank() }
                } else null
            }.getOrNull()
        }

    // ------------------------------------------------------------------------
    // 6. Sticker Generator (FLUX.1 + Die-Cut Vinyl Alpha Contouring)
    // ------------------------------------------------------------------------

    suspend fun generateSticker(
        prompt: String,
        style: StickerStyle = StickerStyle.CARTOON_3D,
        width: Int = 512,
        height: Int = 512,
        seedNonce: Int = 0,
        forceRefresh: Boolean = false,
    ): Bitmap = withContext(Dispatchers.Default) {
        val cleanPrompt = prompt.trim().ifEmpty { "Cute Study Mascot" }
        val seed = if (seedNonce != 0) seedNonce else abs((cleanPrompt + style.name).hashCode())

        val cacheDir = File(context.cacheDir, "pronotes_ai_cache").apply { mkdirs() }
        val cacheKey = "sticker_${cleanPrompt.take(20).replace(Regex("[^a-zA-Z0-9]"), "_")}_${style.name}_${width}x${height}_s$seed.png"
        val cacheFile = File(cacheDir, cacheKey)
        if (!forceRefresh && seedNonce == 0 && cacheFile.exists() && cacheFile.length() > 2048L) {
            val cachedBmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (cachedBmp != null) return@withContext cachedBmp
        }

        val onlineBmp = withContext(Dispatchers.IO) {
            runCatching {
                val styleKeywords = when (style) {
                    StickerStyle.CARTOON_3D -> "3D Pixar digital render, volumetric clay character, vibrant glossy studio lighting, cute"
                    StickerStyle.WATERCOLOR -> "pastel watercolor painting, clean soft ink contours, paper wash texture"
                    StickerStyle.POP_ART -> "pop art halftone style, vibrant retro colors, bold black contours"
                    StickerStyle.ANIME -> "vibrant anime sticker, clean cel shading, cute studio anime aesthetic"
                    StickerStyle.SKETCH -> "hand-drawn graphite and colored pencil sketch, clean fine lines"
                    StickerStyle.MINIMAL -> "flat minimal vector badge, clean geometry, pastel palette"
                }

                val enhancedPrompt = "$cleanPrompt, cute die-cut vinyl sticker, thick solid white border contour outline around character, isolated completely on pure solid white background, $styleKeywords, vector art, centered composition, high resolution, no text artifacts, no blur"
                val encoded = URLEncoder.encode(enhancedPrompt, "UTF-8")
                val urlString = "https://image.pollinations.ai/prompt/$encoded?width=1024&height=1024&nologo=true&seed=$seed&model=flux"

                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 25_000
                    setRequestProperty("User-Agent", "ProNotes-Android/11.0")
                }

                if (conn.responseCode in 200..299) {
                    val rawBitmap = BitmapFactory.decodeStream(conn.inputStream)
                    if (rawBitmap != null && rawBitmap.width > 30 && rawBitmap.height > 30) {
                        val processed = processStickerTransparency(rawBitmap)
                        val scaled = Bitmap.createScaledBitmap(processed, width, height, true)
                        runCatching {
                            cacheFile.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        }
                        scaled
                    } else null
                } else null
            }.getOrNull()
        }

        if (onlineBmp != null) return@withContext onlineBmp

        generateProceduralSticker(cleanPrompt, style, width, height)
    }

    /**
     * Connected-Component BFS Flood-Fill sticker segmentation:
     * - Only erases pixels connected to the image perimeter that match the background color.
     * - Internal white details (eyes, teeth, highlights) are strictly preserved.
     * - Draws a crisp die-cut white vinyl contour border and soft drop shadow behind the subject.
     */
    fun processStickerTransparency(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val samplePoints = listOf(
            0,
            w - 1,
            (h - 1) * w,
            (h - 1) * w + (w - 1),
            w / 2,
            (h - 1) * w + (w / 2),
            (h / 2) * w,
            (h / 2) * w + (w - 1),
        )

        var rSum = 0; var gSum = 0; var bSum = 0
        for (idx in samplePoints) {
            val p = pixels[idx]
            rSum += AndroidColor.red(p)
            gSum += AndroidColor.green(p)
            bSum += AndroidColor.blue(p)
        }
        val rRef = rSum / samplePoints.size
        val gRef = gSum / samplePoints.size
        val bRef = bSum / samplePoints.size

        if (rRef > 205 && gRef > 205 && bRef > 205) {
            val isBg = BooleanArray(w * h)
            val visited = BooleanArray(w * h)
            val queue = java.util.ArrayDeque<Int>(w * 4)

            fun isSimilarToBg(p: Int): Boolean {
                val dr = AndroidColor.red(p) - rRef
                val dg = AndroidColor.green(p) - gRef
                val db = AndroidColor.blue(p) - bRef
                return sqrt((dr * dr + dg * dg + db * db).toDouble()) < 42.0
            }

            for (x in 0 until w) {
                val topIdx = x
                val bottomIdx = (h - 1) * w + x
                if (isSimilarToBg(pixels[topIdx])) { queue.add(topIdx); visited[topIdx] = true }
                if (isSimilarToBg(pixels[bottomIdx])) { queue.add(bottomIdx); visited[bottomIdx] = true }
            }
            for (y in 0 until h) {
                val leftIdx = y * w
                val rightIdx = y * w + (w - 1)
                if (isSimilarToBg(pixels[leftIdx]) && !visited[leftIdx]) { queue.add(leftIdx); visited[leftIdx] = true }
                if (isSimilarToBg(pixels[rightIdx]) && !visited[rightIdx]) { queue.add(rightIdx); visited[rightIdx] = true }
            }

            val dx = intArrayOf(1, -1, 0, 0)
            val dy = intArrayOf(0, 0, 1, -1)

            while (!queue.isEmpty()) {
                val curr = queue.poll()
                isBg[curr] = true
                val cx = curr % w
                val cy = curr / w

                for (d in 0..3) {
                    val nx = cx + dx[d]
                    val ny = cy + dy[d]
                    if (nx in 0 until w && ny in 0 until h) {
                        val nIdx = ny * w + nx
                        if (!visited[nIdx]) {
                            visited[nIdx] = true
                            if (isSimilarToBg(pixels[nIdx])) {
                                queue.add(nIdx)
                            }
                        }
                    }
                }
            }

            for (i in pixels.indices) {
                if (isBg[i]) {
                    pixels[i] = 0
                }
            }
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)

        val bordered = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bordered)

        // Drop shadow for die-cut effect
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(0x26000000, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(out, 0f, 8f, shadowPaint)

        // Crisp white die-cut vinyl contour outline
        val whiteContourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(AndroidColor.WHITE, PorterDuff.Mode.SRC_IN)
        }
        val contourOffsets = floatArrayOf(-5f, 0f, 5f)
        for (ox in contourOffsets) {
            for (oy in contourOffsets) {
                if (ox != 0f || oy != 0f) {
                    canvas.drawBitmap(out, ox, oy, whiteContourPaint)
                }
            }
        }

        canvas.drawBitmap(out, 0f, 0f, null)

        return bordered
    }

    private fun generateProceduralSticker(prompt: String, style: StickerStyle, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = width / 2f
        val cy = height / 2f
        val radius = width * 0.40f

        val accentColor = when (style) {
            StickerStyle.CARTOON_3D -> 0xFFFF7043.toInt()
            StickerStyle.WATERCOLOR -> 0xFF42A5F5.toInt()
            StickerStyle.POP_ART -> 0xFFFFD600.toInt()
            StickerStyle.ANIME -> 0xFFEC407A.toInt()
            StickerStyle.SKETCH -> 0xFF78909C.toInt()
            StickerStyle.MINIMAL -> 0xFF26A69A.toInt()
        }

        val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22000000
        }

        canvas.drawCircle(cx, cy + 8f, radius + 12f, shadowPaint)
        canvas.drawCircle(cx, cy, radius + 10f, whiteBorderPaint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx - radius * 0.3f, cy - radius * 0.3f, radius * 1.2f,
                accentColor, darkenColor(accentColor, 0.35f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x38FFFFFF
        }
        canvas.drawOval(RectF(cx - radius * 0.6f, cy - radius * 0.8f, cx + radius * 0.4f, cy - radius * 0.1f), glossPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = (width * 0.088f).coerceIn(24f, 44f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val label = prompt.take(14).uppercase(Locale.ROOT)
        canvas.drawText("${style.badge} $label", cx, cy + textPaint.textSize * 0.35f, textPaint)

        return bmp
    }

    // ------------------------------------------------------------------------
    // 7. Notebook Cover Generator
    // ------------------------------------------------------------------------

    suspend fun generateCover(
        title: String,
        theme: CoverTheme = CoverTheme.GALAXY,
        width: Int = 900,
        height: Int = 1200,
        seedNonce: Int = 0,
        forceRefresh: Boolean = false,
    ): Bitmap = withContext(Dispatchers.Default) {
        val cleanTitle = title.trim().ifEmpty { "ProNotes Journal" }
        val seed = if (seedNonce != 0) seedNonce else abs((cleanTitle + theme.name).hashCode())

        val onlineBmp = withContext(Dispatchers.IO) {
            runCatching {
                val themeKeywords = when (theme) {
                    CoverTheme.GALAXY -> "vibrant deep cosmic nebula, starry night sky, purple cyan space dust, high detail artwork"
                    CoverTheme.BOTANICAL -> "aesthetic botanical illustration, lush monstera leaves, soft pastel flowers, gold foil accents"
                    CoverTheme.MINIMAL -> "modern minimalist geometric graphic design, bauhaus art style, elegant balance"
                    CoverTheme.VINTAGE -> "vintage leather bound book cover texture, embossed ornate frame, gold foil embossing"
                    CoverTheme.GRADIENT -> "smooth ethereal aura mesh gradient, liquid fluid glass aesthetic, vibrant pastel glow"
                    CoverTheme.GEOMETRIC -> "abstract geometric architecture pattern, contemporary vibrant shapes, structured elegance"
                }

                val prompt = "Notebook journal book cover art: $themeKeywords, clean aesthetic composition, masterpiece, 8k resolution, suitable for printing"
                val encoded = URLEncoder.encode(prompt, "UTF-8")
                val urlString = "https://image.pollinations.ai/prompt/$encoded?width=$width&height=$height&nologo=true&seed=$seed&model=flux"

                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 14_000
                    readTimeout = 28_000
                    setRequestProperty("User-Agent", "ProNotes-Android/11.0")
                }

                if (conn.responseCode in 200..299) {
                    BitmapFactory.decodeStream(conn.inputStream)
                } else null
            }.getOrNull()
        }

        if (onlineBmp != null) {
            return@withContext renderCoverTypography(onlineBmp, cleanTitle, theme)
        }

        generateProceduralCover(cleanTitle, theme, width, height)
    }

    private fun renderCoverTypography(base: Bitmap, title: String, theme: CoverTheme): Bitmap {
        val w = base.width
        val h = base.height
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        canvas.drawRect(0f, 0f, w * 0.05f, h.toFloat(), Paint().apply { color = 0x33000000 })

        val plate = RectF(w * 0.12f, h * 0.38f, w * 0.88f, h * 0.62f)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAA000000.toInt()
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x77FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(plate, 32f, 32f, platePaint)
        canvas.drawRoundRect(plate, 32f, 32f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = (w * 0.068f).coerceIn(36f, 60f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title.take(24).uppercase(Locale.ROOT), plate.centerX(), plate.centerY() - 6f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = (w * 0.032f).coerceIn(18f, 26f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(theme.label, plate.centerX(), plate.centerY() + 42f, subPaint)

        return out
    }

    private fun generateProceduralCover(title: String, theme: CoverTheme, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rng = Random(title.hashCode())

        val (c1, c2) = when (theme) {
            CoverTheme.GALAXY -> 0xFF0D0826.toInt() to 0xFF4A148C.toInt()
            CoverTheme.BOTANICAL -> 0xFF1B5E20.toInt() to 0xFF81C784.toInt()
            CoverTheme.MINIMAL -> 0xFF212121.toInt() to 0xFF424242.toInt()
            CoverTheme.VINTAGE -> 0xFF3E2723.toInt() to 0xFF795548.toInt()
            CoverTheme.GRADIENT -> 0xFF38BDF8.toInt() to 0xFF8C65FF.toInt()
            CoverTheme.GEOMETRIC -> 0xFF0D47A1.toInt() to 0xFFFF6E53.toInt()
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), c1, c2, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18FFFFFF }
        for (i in 0 until 8) {
            val bx = rng.nextFloat() * width
            val by = rng.nextFloat() * height
            val br = width * (0.15f + rng.nextFloat() * 0.35f)
            canvas.drawCircle(bx, by, br, blobPaint)
        }

        return renderCoverTypography(bmp, title, theme)
    }

    // ------------------------------------------------------------------------
    // 8. Study Template Generator (Vector Canvas Layouts)
    // ------------------------------------------------------------------------

    fun generateTemplate(
        type: TemplateType = TemplateType.CORNELL,
        width: Int = 1240,
        height: Int = 1754,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        canvas.drawColor(0xFFFAFAFA.toInt())

        val pad = 64f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD1D5DB.toInt()
            strokeWidth = 2f
        }
        val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF6366F1.toInt()
            strokeWidth = 3.5f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF374151.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111827.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("ProNotes: ${type.label}", pad, 96f, headerPaint)
        canvas.drawText("Date: ___________  |  Topic: ________________________", pad, 142f, textPaint)
        canvas.drawLine(pad, 170f, width - pad, 170f, accentLinePaint)

        when (type) {
            TemplateType.CORNELL -> {
                val cueX = width * 0.32f
                val sumY = height * 0.80f

                canvas.drawLine(cueX, 170f, cueX, sumY, accentLinePaint)
                canvas.drawLine(pad, sumY, width - pad, sumY, accentLinePaint)

                canvas.drawText("CUES & QUESTIONS", pad, 210f, textPaint)
                canvas.drawText("MAIN NOTES", cueX + 24f, 210f, textPaint)
                canvas.drawText("SUMMARY & ACTION ITEMS", pad, sumY + 40f, textPaint)

                var y = 250f
                while (y < sumY - 20f) {
                    canvas.drawLine(pad, y, width - pad, y, linePaint)
                    y += 48f
                }
                var sy = sumY + 80f
                while (sy < height - pad) {
                    canvas.drawLine(pad, sy, width - pad, sy, linePaint)
                    sy += 48f
                }
            }

            TemplateType.WEEKLY_PLANNER -> {
                val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "WEEKEND")
                val cols = 2
                val rows = 3
                val colW = (width - pad * 2 - 24f) / cols
                val rowH = (height - 220f - 48f) / rows

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val idx = r * cols + c
                        val left = pad + c * (colW + 24f)
                        val top = 200f + r * (rowH + 24f)
                        val rect = RectF(left, top, left + colW, top + rowH)

                        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFFE5E7EB.toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                        }
                        canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
                        canvas.drawText(days[idx], left + 18f, top + 36f, textPaint)

                        var ly = top + 70f
                        while (ly < rect.bottom - 16f) {
                            canvas.drawLine(left + 14f, ly, rect.right - 14f, ly, linePaint)
                            ly += 42f
                        }
                    }
                }
            }

            TemplateType.HABIT_TRACKER -> {
                var y = 230f
                canvas.drawText("HABIT / GOAL", pad, y - 16f, textPaint)
                for (day in 1..28) {
                    val cx = width * 0.40f + (day - 1) * 26f
                    canvas.drawText("$day", cx - 6f, y - 16f, Paint(textPaint).apply { textSize = 16f })
                }
                for (hIdx in 0 until 18) {
                    canvas.drawLine(pad, y, width - pad, y, linePaint)
                    for (day in 1..28) {
                        val cx = width * 0.40f + (day - 1) * 26f
                        canvas.drawCircle(cx, y - 16f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF9CA3AF.toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = 1.5f
                        })
                    }
                    y += 76f
                }
            }

            TemplateType.GRID_DOT -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF9CA3AF.toInt()
                }
                val spacing = 36f
                var x = pad + 16f
                while (x < width - pad) {
                    var y = 210f
                    while (y < height - pad) {
                        canvas.drawCircle(x, y, 2.5f, dotPaint)
                        y += spacing
                    }
                    x += spacing
                }
            }

            TemplateType.MEETING_MINUTES -> {
                canvas.drawText("Attendees: _______________________________", pad, 220f, textPaint)
                canvas.drawText("Agenda:", pad, 270f, textPaint)
                canvas.drawLine(pad, 300f, width - pad, 300f, linePaint)
                canvas.drawLine(pad, 350f, width - pad, 350f, linePaint)

                canvas.drawText("Discussion & Decisions:", pad, 420f, textPaint)
                var y = 460f
                while (y < height * 0.70f) {
                    canvas.drawLine(pad, y, width - pad, y, linePaint)
                    y += 48f
                }

                val actionY = height * 0.72f
                canvas.drawLine(pad, actionY, width - pad, actionY, accentLinePaint)
                canvas.drawText("ACTION ITEMS / ASSIGNMENTS", pad, actionY + 40f, textPaint)
                var ay = actionY + 80f
                while (ay < height - pad) {
                    canvas.drawRect(pad, ay - 18f, pad + 18f, ay, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFF9CA3AF.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    })
                    canvas.drawLine(pad + 32f, ay, width - pad, ay, linePaint)
                    ay += 52f
                }
            }

            TemplateType.EISENHOWER_MATRIX -> {
                val midX = width / 2f
                val midY = (height + 170f) / 2f

                canvas.drawLine(midX, 170f, midX, height - pad, accentLinePaint)
                canvas.drawLine(pad, midY, width - pad, midY, accentLinePaint)

                canvas.drawText("1. DO FIRST (Urgent & Important)", pad + 20f, 210f, textPaint)
                canvas.drawText("2. SCHEDULE (Not Urgent & Important)", midX + 20f, 210f, textPaint)
                canvas.drawText("3. DELEGATE (Urgent & Not Important)", pad + 20f, midY + 40f, textPaint)
                canvas.drawText("4. DON'T DO (Not Urgent & Not Important)", midX + 20f, midY + 40f, textPaint)
            }
        }

        return bmp
    }

    private fun darkenColor(argb: Int, factor: Float): Int {
        val a = AndroidColor.alpha(argb)
        val r = (AndroidColor.red(argb) * (1f - factor)).roundToInt().coerceIn(0, 255)
        val g = (AndroidColor.green(argb) * (1f - factor)).roundToInt().coerceIn(0, 255)
        val b = (AndroidColor.blue(argb) * (1f - factor)).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
