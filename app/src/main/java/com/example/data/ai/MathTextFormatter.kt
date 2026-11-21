package com.example.data.ai

/**
 * Converts stray LaTeX math tokens into clean, human-readable Unicode mathematical notation
 * so formulas always render legibly in plain text without raw LaTeX clutter.
 */
object MathTextFormatter {

    private val fracRegex = Regex("""\\(?:d?frac)\{([^{}]+)\}\{([^{}]+)\}""")
    private val sqrtRegex = Regex("""\\sqrt\{([^{}]+)\}""")
    private val textCmdRegex = Regex("""\\(?:text|mathrm|mathbf|mathbb)\{([^{}]+)\}""")
    private val simplePowerRegex = Regex("""\^\{([^{}]+)\}""")
    private val simpleSubRegex = Regex("""_\{([^{}]+)\}""")

    fun formatReadableMath(raw: String): String {
        if (raw.isBlank()) return raw
        var text = raw

        // Remove display/inline LaTeX delimiters
        text = text.replace("$$", "")
            .replace("\\[", "")
            .replace("\\]", "")
            .replace("\\(", "")
            .replace("\\)", "")

        // Iteratively unwrap nested \frac{a}{b} and \sqrt{x}
        repeat(3) {
            text = fracRegex.replace(text) { m -> "(${m.groupValues[1]}) / (${m.groupValues[2]})" }
            text = sqrtRegex.replace(text) { m -> "√(${m.groupValues[1]})" }
            text = textCmdRegex.replace(text) { m -> m.groupValues[1] }
        }

        // Replace common LaTeX math symbols with crisp Unicode symbols
        val replacements = listOf(
            "\\times" to "×",
            "\\cdot" to "·",
            "\\div" to "÷",
            "\\pm" to "±",
            "\\leq" to "≤",
            "\\le" to "≤",
            "\\geq" to "≥",
            "\\ge" to "≥",
            "\\neq" to "≠",
            "\\ne" to "≠",
            "\\approx" to "≈",
            "\\equiv" to "≡",
            "\\infty" to "∞",
            "\\Rightarrow" to "⇒",
            "\\Leftrightarrow" to "⇔",
            "\\implies" to "⇒",
            "\\iff" to "⇔",
            "\\rightarrow" to "→",
            "\\to" to "→",
            "\\in" to "∈",
            "\\notin" to "∉",
            "\\subset" to "⊂",
            "\\cup" to "∪",
            "\\cap" to "∩",
            "\\emptyset" to "∅",
            "\\forall" to "∀",
            "\\exists" to "∃",
            "\\Delta" to "Δ",
            "\\Omega" to "Ω",
            "\\pi" to "π",
            "\\tau" to "τ",
            "\\alpha" to "α",
            "\\beta" to "β",
            "\\theta" to "θ",
            "\\lambda" to "λ",
            "\\mu" to "μ",
            "\\sigma" to "σ",
            "\\sum" to "Σ",
            "\\int" to "∫",
            "\\perp" to "⊥",
            "\\parallel" to "//",
            "\\quad" to "  ",
            "\\;" to " ",
            "\\," to " ",
            "\\left" to "",
            "\\right" to ""
        )
        for ((latex, unicode) in replacements) {
            text = text.replace(latex, unicode)
        }

        // Convert common exponents and subscripts
        text = simplePowerRegex.replace(text) { m ->
            when (m.groupValues[1]) {
                "2" -> "²"
                "3" -> "³"
                "n" -> "ⁿ"
                "-1" -> "⁻¹"
                else -> "^(${m.groupValues[1]})"
            }
        }
        text = simpleSubRegex.replace(text) { m -> "_${m.groupValues[1]}" }

        // Remove isolated inline $ markers without eating currency amounts
        text = text.replace(Regex("""\$([^$\n]+)\$""")) { it.groupValues[1] }

        // Clean leftover markdown bold asterisks if desired? Keep clean readability by replacing **x** with x
        text = text.replace("**", "")

        return text.trim()
    }
}
