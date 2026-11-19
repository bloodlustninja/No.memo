package com.example.nomemo.ledger

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Monetary entry parsed from a note line.
 */
data class ExpenseEntry(
    val rawLine: String,
    val description: String,
    val amount: Double,
    val isNegative: Boolean = false,
    val isCredit: Boolean = false,
    val tags: List<String> = emptyList(),
) {
    val signedAmount: Double
        get() = if (isNegative) -amount else amount
}

/**
 * Line analysis result with formatted badge for editor right-alignment.
 */
data class ParsedLine(
    val lineText: String,
    val isDirective: Boolean = false,
    val directiveType: String? = null, // "cap" or "split"
    val entry: ExpenseEntry? = null,
    val formattedBadge: String? = null,
)

/**
 * Overall Ledger state for a note.
 */
data class LedgerAnalysis(
    val isLedgerMode: Boolean,
    val entries: List<ExpenseEntry>,
    val totalExpense: Double,
    val capAmount: Double? = null,
    val splitCount: Int? = null,
    val currencySymbol: String = "₹",
) {
    val perPersonSplit: Double?
        get() = if (splitCount != null && splitCount > 0 && totalExpense != 0.0) totalExpense / splitCount else null

    val remainingBudget: Double?
        get() = if (capAmount != null) capAmount - totalExpense else null

    val capProgressFraction: Float?
        get() = if (capAmount != null && capAmount > 0) (totalExpense / capAmount).toFloat().coerceIn(0f, 1f) else null

    val isCapExceededOrNearLimit: Boolean
        get() = if (capAmount != null && capAmount > 0) (totalExpense / capAmount) >= 0.85 else false
}

/**
 * Pure Kotlin Auto-Ledger Parser Engine for Nothing OS 5.0 Bento Notes.
 *
 * Scans note lines for monetary entries ("Fuel 450", "Coffee -180", "₹1,200 groceries", "+5000 split", "100 - 50", "#tags"),
 * properly handles negative amounts / deductions (-180, -₹180, Discount -50),
 * supports optional inline directives "/cap <amount>" and "/split <count>", and activates strictly when at least
 * 2 valid expense lines or explicit directives are present.
 */
object ExpenseParser {

    private val STOP_WORDS = setOf(
        "at", "on", "in", "by", "to", "for", "room", "ch", "chapter", "page", "pg", "step",
        "v", "ver", "version", "no", "num", "number", "nov", "dec", "jan", "feb", "mar",
        "apr", "may", "jun", "jul", "aug", "sep", "oct",
    )

    private val DEDUCTION_KEYWORDS = setOf(
        "discount", "refund", "cashback", "coupon", "rebate", "deduct", "deduction", "return", "less", "minus", "off", "saving", "savings",
    )

    private val CAP_REGEX = "(?:^|\\s)[#/]cap\\s+([+-]?\\s*[₹$€£Rs\\.]*\\s*[\\d,]+(?:\\.\\d{1,2})?)".toRegex(RegexOption.IGNORE_CASE)
    private val SPLIT_REGEX = "(?:^|\\s)[#/]split\\s+(\\d+)".toRegex(RegexOption.IGNORE_CASE)
    private val TAG_REGEX = "#([a-zA-Z0-9_]+)".toRegex()
    private val CURRENCY_SYMBOL_REGEX = "([₹$€£]|Rs\\.?|INR)".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Parses the note text into a complete LedgerAnalysis.
     */
    fun parse(text: String): LedgerAnalysis {
        val lines = text.split("\n")
        var detectedCurrency = "₹"
        var capAmount: Double? = null
        var splitCount: Int? = null
        val entries = mutableListOf<ExpenseEntry>()
        var hasExpenseTag = false

        // Detect currency symbol from text
        val currencyMatch = CURRENCY_SYMBOL_REGEX.find(text)
        if (currencyMatch != null) {
            val symbol = currencyMatch.value.trim()
            detectedCurrency = when {
                symbol.startsWith("Rs", ignoreCase = true) -> "Rs."
                symbol.equals("INR", ignoreCase = true) -> "₹"
                else -> symbol
            }
        }

        // Check for directives and tags
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // /cap directive
            val capMatch = CAP_REGEX.find(trimmed)
            if (capMatch != null) {
                val rawNum = capMatch.groupValues[1].replace("[^0-9.]".toRegex(), "")
                rawNum.toDoubleOrNull()?.let { capAmount = it }
            }

            // /split directive
            val splitMatch = SPLIT_REGEX.find(trimmed)
            if (splitMatch != null) {
                splitMatch.groupValues[1].toIntOrNull()?.let { if (it > 0) splitCount = it }
            }

            // Tags
            TAG_REGEX.findAll(trimmed).forEach { match ->
                val tag = match.groupValues[1]
                if (tag.equals("expense", ignoreCase = true) || tag.equals("ledger", ignoreCase = true)) {
                    hasExpenseTag = true
                }
            }

            val parsed = parseLine(trimmed, detectedCurrency)
            if (parsed?.entry != null) {
                entries.add(parsed.entry)
            }
        }

        // Ledger Mode triggers only if >= 2 valid lines OR explicit /cap or #expense tag
        val isLedgerMode = entries.size >= 2 || capAmount != null || hasExpenseTag

        val positiveEntries = entries.filter { !it.isNegative }
        val negativeEntries = entries.filter { it.isNegative }

        val positiveSum = positiveEntries.sumOf { it.amount }
        val negativeSum = negativeEntries.sumOf { it.amount }

        val totalExpense = when {
            // Both positive and negative entries: subtract negative deductions from positive expenses
            positiveEntries.isNotEmpty() && negativeEntries.isNotEmpty() -> positiveSum - negativeSum
            // Only positive entries: sum of positive entries
            positiveEntries.isNotEmpty() -> positiveSum
            // Only negative entries: if user logged expenses using minus signs, total magnitude of expenses
            negativeEntries.isNotEmpty() -> negativeSum
            else -> 0.0
        }

        return LedgerAnalysis(
            isLedgerMode = isLedgerMode,
            entries = entries,
            totalExpense = totalExpense,
            capAmount = capAmount,
            splitCount = splitCount,
            currencySymbol = detectedCurrency,
        )
    }

    /**
     * Parses a single line of text for monetary entries or directives.
     */
    fun parseLine(line: String, currencySymbol: String = "₹"): ParsedLine? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        // Check for directives
        val capMatch = CAP_REGEX.find(trimmed)
        if (capMatch != null) {
            val rawNum = capMatch.groupValues[1].replace("[^0-9.]".toRegex(), "")
            val capVal = rawNum.toDoubleOrNull()
            return ParsedLine(
                lineText = trimmed,
                isDirective = true,
                directiveType = "cap",
                formattedBadge = if (capVal != null) "CAP: $currencySymbol${formatAmountWithCommas(capVal)}" else "CAP",
            )
        }

        val splitMatch = SPLIT_REGEX.find(trimmed)
        if (splitMatch != null) {
            val count = splitMatch.groupValues[1].toIntOrNull() ?: 1
            return ParsedLine(
                lineText = trimmed,
                isDirective = true,
                directiveType = "split",
                formattedBadge = "$count WAYS",
            )
        }

        // Extract inline tags
        val tags = TAG_REGEX.findAll(trimmed).map { it.groupValues[1] }.toList()
        val textWithoutTags = trimmed.replace(TAG_REGEX, "").trim()
        if (textWithoutTags.isEmpty()) return null

        // Pattern 1: Inline Arithmetic Expression on a single line (e.g. "500 - 180", "100 - 50", "Dinner 500 - 100", "1000 - 200 + 50")
        val mathRegex = "^(.*?)(?:^|\\s)([\\d,]+(?:\\.\\d{1,2})?\\s*(?:[+-]\\s*[\\d,]+(?:\\.\\d{1,2})?)+)(?:\\s+(.*?))?$".toRegex()
        val mathMatch = mathRegex.find(textWithoutTags)
        if (mathMatch != null) {
            val expr = mathMatch.groupValues[2].trim()
            val termRegex = "([+-]?)\\s*([\\d,]+(?:\\.\\d{1,2})?)".toRegex()
            var netEval = 0.0
            var termCount = 0
            for (term in termRegex.findAll(expr)) {
                val sign = term.groupValues[1].trim()
                val num = term.groupValues[2].replace(",", "").toDoubleOrNull()
                if (num != null) {
                    termCount++
                    if (sign == "-") {
                        netEval -= num
                    } else {
                        netEval += num
                    }
                }
            }
            if (termCount >= 2) {
                val prefixDesc = mathMatch.groupValues[1].trim()
                val suffixDesc = mathMatch.groupValues[3].trim()
                val desc = listOf(prefixDesc, suffixDesc).filter { it.isNotBlank() }.joinToString(" ")
                    .replace(":", "").trim().ifBlank { "Calculation" }
                val isNeg = netEval < 0
                val absVal = abs(netEval)
                val badge = (if (isNeg) "-" else "") + "$currencySymbol${formatAmountWithCommas(absVal)}"
                return ParsedLine(
                    lineText = trimmed,
                    entry = ExpenseEntry(
                        rawLine = trimmed,
                        description = desc,
                        amount = absVal,
                        isNegative = isNeg,
                        isCredit = !isNeg,
                        tags = tags,
                    ),
                    formattedBadge = badge,
                )
            }
        }

        // Pattern 2: Explicit Currency Symbol with optional minus/plus before OR after symbol
        // (e.g., "-₹180", "- ₹180", "₹-180", "-$50", "+₹5000", "₹1,200 groceries", "Coffee -₹180")
        val currRegex = "(?:^|\\s)([+-]?)\\s*([₹$€£]|Rs\\.?|INR)\\s*([+-]?)\\s*([\\d,]+(?:\\.\\d{1,2})?)(?:\\s|$)".toRegex(RegexOption.IGNORE_CASE)
        val currMatch = currRegex.find(textWithoutTags)
        if (currMatch != null) {
            val preSign = currMatch.groupValues[1].trim()
            val sym = currMatch.groupValues[2].trim()
            val postSign = currMatch.groupValues[3].trim()
            val numStr = currMatch.groupValues[4].replace(",", "").trim()
            val amountVal = numStr.toDoubleOrNull()
            if (amountVal != null) {
                val isNegative = preSign == "-" || postSign == "-"
                val isCredit = preSign == "+" || postSign == "+"
                val desc = textWithoutTags.replace(currMatch.value, "").replace(":", "").trim()
                    .ifBlank { if (isNegative) "Deduction" else "Expense" }
                val absAmount = abs(amountVal)
                val badge = (if (isNegative) "-" else if (isCredit) "+" else "") +
                    "$currencySymbol${formatAmountWithCommas(absAmount)}"
                return ParsedLine(
                    lineText = trimmed,
                    entry = ExpenseEntry(
                        rawLine = trimmed,
                        description = desc,
                        amount = absAmount,
                        isNegative = isNegative,
                        isCredit = isCredit,
                        tags = tags,
                    ),
                    formattedBadge = badge,
                )
            }
        }

        // Pattern 3: Trailing Currency Symbol (e.g. "180₹", "-180₹", "50$")
        val trailingCurrRegex = "(?:^|\\s)([+-]?)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*([₹$€£]|Rs\\.?|INR)(?:\\s|$)".toRegex(RegexOption.IGNORE_CASE)
        val trailingCurrMatch = trailingCurrRegex.find(textWithoutTags)
        if (trailingCurrMatch != null) {
            val sign = trailingCurrMatch.groupValues[1].trim()
            val numStr = trailingCurrMatch.groupValues[2].replace(",", "").trim()
            val amountVal = numStr.toDoubleOrNull()
            if (amountVal != null) {
                val isNegative = sign == "-"
                val isCredit = sign == "+"
                val desc = textWithoutTags.replace(trailingCurrMatch.value, "").replace(":", "").trim()
                    .ifBlank { if (isNegative) "Deduction" else "Expense" }
                val absAmount = abs(amountVal)
                val badge = (if (isNegative) "-" else if (isCredit) "+" else "") +
                    "$currencySymbol${formatAmountWithCommas(absAmount)}"
                return ParsedLine(
                    lineText = trimmed,
                    entry = ExpenseEntry(
                        rawLine = trimmed,
                        description = desc,
                        amount = absAmount,
                        isNegative = isNegative,
                        isCredit = isCredit,
                        tags = tags,
                    ),
                    formattedBadge = badge,
                )
            }
        }

        // Pattern 4: Explicit signed number without currency symbol
        // (e.g., "Coffee -180", "Coffee - 180", "Coffee-180", "+5000 split", "-180", "- 180", "+500")
        val signedRegex = "(?:^|\\s|[a-zA-Z:])([+-])\\s*([\\d,]+(?:\\.\\d{1,2})?)(?:\\s|$)".toRegex()
        val signedMatch = signedRegex.find(textWithoutTags)
        if (signedMatch != null) {
            val sign = signedMatch.groupValues[1].trim()
            val numStr = signedMatch.groupValues[2].replace(",", "").trim()
            val amountVal = numStr.toDoubleOrNull()
            if (amountVal != null) {
                val isNegative = sign == "-"
                val isCredit = sign == "+"
                val matchedFull = signedMatch.value
                val desc = textWithoutTags.replace(matchedFull, "").replace(":", "").trim()
                    .ifBlank { if (isNegative) "Deduction" else "Expense" }
                val absAmount = abs(amountVal)
                val badge = (if (isNegative) "-" else "+") + "$currencySymbol${formatAmountWithCommas(absAmount)}"
                return ParsedLine(
                    lineText = trimmed,
                    entry = ExpenseEntry(
                        rawLine = trimmed,
                        description = desc,
                        amount = absAmount,
                        isNegative = isNegative,
                        isCredit = isCredit,
                        tags = tags,
                    ),
                    formattedBadge = badge,
                )
            }
        }

        // Pattern 5: Trailing number without symbol (e.g., "Fuel 450", "Coffee: 180", "Groceries 1,200", "Discount 50")
        val trailingRegex = "^(.+?)(?:\\s+|:\\s*)([\\d,]+(?:\\.\\d{1,2})?)$".toRegex()
        val trailingMatch = trailingRegex.find(textWithoutTags)
        if (trailingMatch != null) {
            val desc = trailingMatch.groupValues[1].replace(":", "").trim()
            val numStr = trailingMatch.groupValues[2].replace(",", "")
            val amountVal = numStr.toDoubleOrNull()
            if (amountVal != null && amountVal >= 1.0) {
                val words = desc.lowercase().split("\\s+".toRegex())
                val lastWord = words.lastOrNull()
                if (lastWord == null || !STOP_WORDS.contains(lastWord)) {
                    val isNegative = words.any { DEDUCTION_KEYWORDS.contains(it) }
                    val badge = (if (isNegative) "-" else "") + "$currencySymbol${formatAmountWithCommas(amountVal)}"
                    return ParsedLine(
                        lineText = trimmed,
                        entry = ExpenseEntry(
                            rawLine = trimmed,
                            description = desc,
                            amount = amountVal,
                            isNegative = isNegative,
                            isCredit = false,
                            tags = tags,
                        ),
                        formattedBadge = badge,
                    )
                }
            }
        }

        // Pattern 6: Leading number without symbol (e.g., "450 fuel", "1200 groceries", "50 discount")
        val leadingRegex = "^([\\d,]+(?:\\.\\d{1,2})?)\\s+([a-zA-Z].*)$".toRegex()
        val leadingMatch = leadingRegex.find(textWithoutTags)
        if (leadingMatch != null) {
            val numStr = leadingMatch.groupValues[1].replace(",", "")
            val desc = leadingMatch.groupValues[2].replace(":", "").trim()
            val amountVal = numStr.toDoubleOrNull()
            if (amountVal != null && amountVal >= 1.0) {
                val words = desc.lowercase().split("\\s+".toRegex())
                val isNegative = words.any { DEDUCTION_KEYWORDS.contains(it) }
                val badge = (if (isNegative) "-" else "") + "$currencySymbol${formatAmountWithCommas(amountVal)}"
                return ParsedLine(
                    lineText = trimmed,
                    entry = ExpenseEntry(
                        rawLine = trimmed,
                        description = desc,
                        amount = amountVal,
                        isNegative = isNegative,
                        isCredit = false,
                        tags = tags,
                    ),
                    formattedBadge = badge,
                )
            }
        }

        // Pattern 7: Standalone positive number on its own line (e.g., "450", "1,200", "500.50")
        val standaloneRegex = "^([\\d,]+(?:\\.\\d{1,2})?)$".toRegex()
        val standaloneMatch = standaloneRegex.find(textWithoutTags)
        if (standaloneMatch != null) {
            val numStr = standaloneMatch.groupValues[1].replace(",", "")
            val amountVal = numStr.toDoubleOrNull()
            if (amountVal != null && amountVal >= 1.0) {
                val badge = "$currencySymbol${formatAmountWithCommas(amountVal)}"
                return ParsedLine(
                    lineText = trimmed,
                    entry = ExpenseEntry(
                        rawLine = trimmed,
                        description = "Expense",
                        amount = amountVal,
                        isNegative = false,
                        isCredit = false,
                        tags = tags,
                    ),
                    formattedBadge = badge,
                )
            }
        }

        return null
    }

    /**
     * Formats an amount with thousand commas (e.g. 1450.0 -> "1,450", 1450.5 -> "1,450.50").
     */
    fun formatAmountWithCommas(amount: Double): String {
        val isWhole = (amount % 1.0) == 0.0
        val absAmount = abs(amount)
        val formatter = DecimalFormat(if (isWhole) "#,##0" else "#,##0.00")
        return formatter.format(absAmount)
    }

    /**
     * Formats a signed monetary amount with currency symbol (e.g. 1450.0 -> "₹1,450", -180.0 -> "-₹180").
     */
    fun formatCurrency(amount: Double, currencySymbol: String = "₹"): String {
        val formatted = formatAmountWithCommas(abs(amount))
        return if (amount < 0) "-$currencySymbol$formatted" else "$currencySymbol$formatted"
    }

    /**
     * Resets/clears expense amounts and directives from the note body while keeping other text.
     */
    fun resetLedger(body: String): String {
        val lines = body.split("\n")
        val remainingLines = lines.filter { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("/cap", ignoreCase = true) || trimmed.startsWith("/split", ignoreCase = true)) {
                false
            } else {
                val parsed = parseLine(trimmed)
                parsed?.entry == null // keep lines that are NOT pure expense entries
            }
        }
        return remainingLines.joinToString("\n").trim()
    }

    /**
     * Exports the note expenses to CSV format and launches the system Share intent.
     */
    fun exportCsv(context: Context, title: String, body: String) {
        val analysis = parse(body)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("Date,Description,Amount (${analysis.currencySymbol}),Type,Tags")
        for (entry in analysis.entries) {
            val typeStr = when {
                entry.isNegative -> "Deduction"
                entry.isCredit -> "Income/Credit"
                else -> "Expense"
            }
            val signedAmount = if (entry.isNegative) "-${entry.amount}" else if (entry.isCredit) "+${entry.amount}" else "${entry.amount}"
            val tagsStr = entry.tags.joinToString(" ") { "#$it" }
            sb.appendLine("\"$today\",\"${entry.description.replace("\"", "\"\"")}\",$signedAmount,$typeStr,\"$tagsStr\"")
        }
        sb.appendLine()
        sb.appendLine("Total,,${analysis.totalExpense},,")
        if (analysis.splitCount != null && analysis.perPersonSplit != null) {
            sb.appendLine("Split (${analysis.splitCount} people),,${analysis.perPersonSplit},,")
        }
        if (analysis.capAmount != null && analysis.remainingBudget != null) {
            sb.appendLine("Budget Cap,,${analysis.capAmount},,")
            sb.appendLine("Remaining Budget,,${analysis.remainingBudget},,")
        }

        val csvContent = sb.toString()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "${title.ifBlank { "Note" }} - Expense Ledger CSV")
            putExtra(Intent.EXTRA_TEXT, csvContent)
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Ledger CSV")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(shareIntent)
            Toast.makeText(context, "LEDGER CSV EXPORT READY", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "NO APP AVAILABLE TO SHARE CSV", Toast.LENGTH_SHORT).show()
        }
    }
}
