package com.example.nomemo.data

import com.example.nomemo.data.model.ChecklistItem
import java.util.regex.Pattern

/**
 * Lightweight Zero-Latency Local Intelligence Engine for NO.MEMO.
 *
 * Automatically extracts:
 * 1. Actionable checklist items from natural prose or voice transcripts
 * 2. Smart categorization hashtags (#ideas, #urgent, #meeting, #grocery)
 * 3. Urgency detection (flashes Nothing Crimson accent)
 */
object SmartNoteIntelligence {

    private val URGENT_KEYWORDS = listOf("urgent", "asap", "critical", "important", "deadline", "emergency")
    private val CHECKLIST_TRIGGER_PHRASES = listOf(
        "todo:", "tasks:", "checklist:", "remember to", "need to", "buy", "pick up",
        "- [ ]", "1.", "2.", "first,", "second,", "finally,"
    )

    data class AnalysisResult(
        val detectedTags: List<String>,
        val isUrgent: Boolean,
        val suggestedChecklist: List<ChecklistItem>,
        val cleanBody: String,
    )

    fun analyze(title: String, body: String): AnalysisResult {
        val combined = "$title\n$body"
        val lower = combined.lowercase()

        // 1. Tag extraction
        val tagRegex = "#([a-zA-Z0-9_]+)".toRegex()
        val tags = tagRegex.findAll(combined)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toMutableList()

        // 2. Urgency detection
        val isUrgent = URGENT_KEYWORDS.any { lower.contains(it) }
        if (isUrgent && !tags.contains("urgent")) {
            tags.add(0, "urgent")
        }

        // 3. Checklist extraction from sentences or lines
        val extractedItems = mutableListOf<ChecklistItem>()
        val lines = body.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val lowerLine = trimmed.lowercase()

            when {
                trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") -> {
                    val isChecked = trimmed.startsWith("- [x]")
                    val text = trimmed.substring(5).trim()
                    if (text.isNotBlank()) extractedItems.add(ChecklistItem(text = text, isChecked = isChecked))
                }
                trimmed.startsWith("•") || trimmed.startsWith("*") || trimmed.startsWith("-") -> {
                    val text = trimmed.substring(1).trim()
                    if (text.isNotBlank()) extractedItems.add(ChecklistItem(text = text))
                }
                trimmed.matches("^\\d+\\.\\s+.*".toRegex()) -> {
                    val text = trimmed.replaceFirst("^\\d+\\.\\s+".toRegex(), "").trim()
                    if (text.isNotBlank()) extractedItems.add(ChecklistItem(text = text))
                }
                CHECKLIST_TRIGGER_PHRASES.any { lowerLine.startsWith(it) } -> {
                    val phrase = CHECKLIST_TRIGGER_PHRASES.first { lowerLine.startsWith(it) }
                    val text = trimmed.substring(phrase.length).trim().removePrefix(":").trim()
                    if (text.isNotBlank()) extractedItems.add(ChecklistItem(text = text))
                }
            }
        }

        return AnalysisResult(
            detectedTags = tags.toList(),
            isUrgent = isUrgent,
            suggestedChecklist = extractedItems,
            cleanBody = body,
        )
    }
}
