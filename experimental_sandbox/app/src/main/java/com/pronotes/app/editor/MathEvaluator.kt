package com.pronotes.app.editor

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Result of evaluating a mathematical expression or solving a simple linear equation.
 */
sealed class MathResult {
    data class Value(val number: Double, val formatted: String) : MathResult()
    data class EquationSolution(val variable: String, val number: Double, val formatted: String) : MathResult()
    data class Error(val message: String) : MathResult()
}

/**
 * Clean-room recursive-descent mathematical expression evaluator and simple linear equation solver.
 *
 * Supports:
 * - Arithmetic: `+`, `-`, `*`, `/`, `%`, `^` / `**`, parentheses `(...)`, unary `+`/`-`
 * - Unicode operators: `×` (`*`), `÷` (`/`), `−` (`-`), `²` (`^2`), `³` (`^3`), `√` (`sqrt`)
 * - Constants: `pi` (`π`), `e`, `tau`
 * - Functions: `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sqrt`, `cbrt`, `abs`, `ln`, `log`, `log2`, `exp`, `floor`, `ceil`, `round`
 * - Percentage phrases: `"15% of 200"`, `"200 + 15%"`, `"200 - 15%"`
 * - Simple 1-variable linear equations: `"2x + 5 = 15"`, `"3(x - 2) = 9"`
 */
object MathEvaluator {

    private val CONSTANTS = mapOf(
        "pi" to Math.PI,
        "π" to Math.PI,
        "e" to Math.E,
        "tau" to (2.0 * Math.PI)
    )

    private val FUNCTIONS = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sqrt", "cbrt", "abs", "ln", "log", "log10", "log2",
        "exp", "floor", "ceil", "round"
    )

    /**
     * Evaluates an expression with a single named variable (e.g. `x`) bound to [value].
     * Returns `Double.NaN` if the expression is invalid or undefined at [value].
     */
    fun evaluateWithVariable(rawInput: String, variable: String, value: Double): Double {
        val normalized = normalizeInput(rawInput)
            .removePrefix("y=")
            .removePrefix("y =")
            .removePrefix("f(x)=")
            .removePrefix("f(x) =")
            .trim()
        if (normalized.isEmpty()) return Double.NaN
        return evalWithVar(normalized, variable, value)
    }

    /**
     * Evaluates an expression if it ends with `=` (e.g. `"24 * 15 ="` -> `"360"`),
     * or returns `null` if the input does not look like a trailing-equals math prompt.
     */
    fun evaluateTrailingEquals(input: String): String? {
        val trimmed = input.trimEnd()
        if (!trimmed.endsWith("=")) return null
        val expr = trimmed.dropLast(1).trim()
        if (expr.isEmpty()) return null
        return when (val res = evaluate(expr)) {
            is MathResult.Value -> res.formatted
            is MathResult.EquationSolution -> "${res.variable} = ${res.formatted}"
            is MathResult.Error -> null
        }
    }

    /**
     * Evaluates a mathematical expression or solves a single-variable linear equation.
     */
    fun evaluate(rawInput: String): MathResult {
        val normalized = normalizeInput(rawInput)
        if (normalized.isEmpty()) {
            return MathResult.Error("Empty expression")
        }

        // Check for equation with '='
        if ('=' in normalized) {
            val parts = normalized.split('=')
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return MathResult.Error("Invalid equation")
            }
            return solveLinearEquation(parts[0].trim(), parts[1].trim())
        }

        // Check for "X% of Y"
        val percentOfMatch = Regex("""^(.+?)%\s+of\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(normalized)
        if (percentOfMatch != null) {
            val pctRes = evalExpr(percentOfMatch.groupValues[1])
            val baseRes = evalExpr(percentOfMatch.groupValues[2])
            return if (pctRes is MathResult.Value && baseRes is MathResult.Value) {
                val value = (pctRes.number / 100.0) * baseRes.number
                valueResult(value)
            } else {
                MathResult.Error("Invalid percentage expression")
            }
        }

        return evalExpr(normalized)
    }

    private fun normalizeInput(raw: String): String {
        return raw.trim()
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("√(", "sqrt(")
            .replace(Regex("""√(\d+(?:\.\d+)?)"""), "sqrt($1)")
            .replace("**", "^")
    }

    private fun evalExpr(expr: String): MathResult {
        return try {
            val parser = Parser(expr)
            val value = parser.parseExpression()
            if (parser.hasMore()) {
                MathResult.Error("Unexpected character '${parser.peek()}' at position ${parser.pos}")
            } else if (value.isNaN() || value.isInfinite()) {
                MathResult.Error("Undefined result")
            } else {
                valueResult(value)
            }
        } catch (e: IllegalArgumentException) {
            MathResult.Error(e.message ?: "Invalid expression")
        }
    }

    private fun solveLinearEquation(lhs: String, rhs: String): MathResult {
        val varMatch = Regex("""[a-zA-Z]""").findAll("$lhs $rhs")
            .map { it.value }
            .filter { token ->
                // Exclude function names / constants
                val lower = token.lowercase()
                lower != "e"
            }
            .distinct()
            .toList()

        // Find single-letter variable not part of a known function/constant
        val stripped = "$lhs $rhs"
            .replace(Regex("""\b(sin|cos|tan|asin|acos|atan|sqrt|cbrt|abs|ln|log|log10|log2|exp|floor|ceil|round|pi|tau)\b""", RegexOption.IGNORE_CASE), "")
        val vars = Regex("""[a-zA-Z]""").findAll(stripped).map { it.value }.distinct().toList()
        if (vars.size != 1) {
            return MathResult.Error("Expected a single variable")
        }
        val variable = vars.first()

        // Evaluate f(x) = lhs - rhs at x = 0 and x = 1 (assuming linear)
        val f0 = evalWithVar(lhs, variable, 0.0) - evalWithVar(rhs, variable, 0.0)
        val f1 = evalWithVar(lhs, variable, 1.0) - evalWithVar(rhs, variable, 1.0)
        if (f0.isNaN() || f1.isNaN()) {
            return MathResult.Error("Could not solve equation")
        }
        val slope = f1 - f0
        if (abs(slope) < 1e-12) {
            return MathResult.Error("No unique solution")
        }
        val solution = -f0 / slope
        val formatted = formatNumber(solution)
        return MathResult.EquationSolution(variable, solution, formatted)
    }

    private fun evalWithVar(expr: String, variable: String, value: Double): Double {
        return try {
            val parser = Parser(expr, mapOf(variable to value))
            val res = parser.parseExpression()
            if (parser.hasMore()) Double.NaN else res
        } catch (_: IllegalArgumentException) {
            Double.NaN
        }
    }

    private fun valueResult(value: Double): MathResult.Value {
        return MathResult.Value(value, formatNumber(value))
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Undefined"
        val cleaned = if (abs(value) < 1e-12) 0.0 else value
        val bd = BigDecimal.valueOf(cleaned)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val plain = bd.toPlainString()
        return if (plain == "-0") "0" else plain
    }

    private class Parser(
        private val input: String,
        private val variables: Map<String, Double> = emptyMap()
    ) {
        var pos: Int = 0
            private set

        fun hasMore(): Boolean {
            skipWhitespace()
            return pos < input.length
        }

        fun peek(): Char = input[pos]

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                skipWhitespace()
                if (pos >= input.length) break
                when (input[pos]) {
                    '+' -> {
                        pos++
                        val right = parseTermWithPercent(x, isAdd = true)
                        x += right
                    }
                    '-' -> {
                        pos++
                        val right = parseTermWithPercent(x, isAdd = false)
                        x -= right
                    }
                    else -> break
                }
            }
            return x
        }

        private fun parseTermWithPercent(base: Double, isAdd: Boolean): Double {
            val savedPos = pos
            val term = parseTerm()
            skipWhitespace()
            // Support "200 + 15%" -> 200 + (200 * 0.15)
            if (pos < input.length && input[pos] == '%' &&
                (pos + 1 >= input.length || !input[pos + 1].isDigit())
            ) {
                pos++
                return base * (term / 100.0)
            }
            // Otherwise if '%' was consumed inside parseTerm as modulo, that's already handled
            val _unused = savedPos
            val _unused2 = isAdd
            return term
        }

        private fun parseTerm(): Double {
            var x = parsePower()
            while (true) {
                skipWhitespace()
                if (pos >= input.length) break
                when (input[pos]) {
                    '*' -> {
                        pos++
                        x *= parsePower()
                    }
                    '/' -> {
                        pos++
                        val denom = parsePower()
                        if (denom == 0.0) throw IllegalArgumentException("Division by zero")
                        x /= denom
                    }
                    '%' -> {
                        // Modulo only if followed by another operand
                        val nextPos = pos + 1
                        var look = nextPos
                        while (look < input.length && input[look].isWhitespace()) look++
                        if (look < input.length && (input[look].isDigit() || input[look] == '(' || input[look].isLetter() || input[look] == '.')) {
                            pos++
                            val denom = parsePower()
                            if (denom == 0.0) throw IllegalArgumentException("Division by zero")
                            x %= denom
                        } else {
                            break
                        }
                    }
                    else -> {
                        // Implicit multiplication: e.g. 2(x + 1) or 2x
                        if (input[pos] == '(' || input[pos].isLetter()) {
                            x *= parsePower()
                        } else {
                            break
                        }
                    }
                }
            }
            return x
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            skipWhitespace()
            if (pos < input.length && input[pos] == '^') {
                pos++
                val exponent = parsePower() // Right-associative
                return base.pow(exponent)
            }
            return base
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            if (pos < input.length) {
                when (input[pos]) {
                    '+' -> {
                        pos++
                        return parseUnary()
                    }
                    '-' -> {
                        pos++
                        return -parseUnary()
                    }
                }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (pos >= input.length) throw IllegalArgumentException("Unexpected end of expression")

            val ch = input[pos]
            if (ch == '(') {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (pos >= input.length || input[pos] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                pos++
                return value
            }

            if (ch.isDigit() || ch == '.') {
                val start = pos
                while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                    pos++
                }
                return input.substring(start, pos).toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid number")
            }

            if (ch.isLetter() || ch == 'π') {
                val start = pos
                while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == 'π')) {
                    pos++
                }
                val ident = input.substring(start, pos)
                val lower = ident.lowercase()

                variables[ident]?.let { return it }
                CONSTANTS[lower]?.let { return it }

                if (lower in FUNCTIONS) {
                    skipWhitespace()
                    if (pos >= input.length || input[pos] != '(') {
                        throw IllegalArgumentException("Expected '(' after function $ident")
                    }
                    pos++
                    val arg = parseExpression()
                    skipWhitespace()
                    if (pos >= input.length || input[pos] != ')') {
                        throw IllegalArgumentException("Missing ')' after $ident argument")
                    }
                    pos++
                    return applyFunction(lower, arg)
                }

                throw IllegalArgumentException("Unknown identifier '$ident'")
            }

            throw IllegalArgumentException("Unexpected character '$ch'")
        }

        private fun applyFunction(name: String, arg: Double): Double = when (name) {
            "sin" -> sin(arg)
            "cos" -> cos(arg)
            "tan" -> tan(arg)
            "asin" -> asin(arg)
            "acos" -> acos(arg)
            "atan" -> atan(arg)
            "sqrt" -> {
                if (arg < 0) throw IllegalArgumentException("Square root of negative number")
                sqrt(arg)
            }
            "cbrt" -> cbrt(arg)
            "abs" -> abs(arg)
            "ln" -> {
                if (arg <= 0) throw IllegalArgumentException("Logarithm of non-positive number")
                ln(arg)
            }
            "log", "log10" -> {
                if (arg <= 0) throw IllegalArgumentException("Logarithm of non-positive number")
                log10(arg)
            }
            "log2" -> {
                if (arg <= 0) throw IllegalArgumentException("Logarithm of non-positive number")
                log2(arg)
            }
            "exp" -> exp(arg)
            "floor" -> floor(arg)
            "ceil" -> ceil(arg)
            "round" -> round(arg)
            else -> throw IllegalArgumentException("Unknown function '$name'")
        }
    }
}
