package com.example.nomemo.voice

/**
 * Strict Voice Command Parser for NO.MEMO.
 *
 * All speech goes 100% into the Body by default. Title is ONLY populated when the user
 * explicitly speaks the keyword "title" or "tittle".
 */
data class ParsedVoiceNote(
    val title: String,
    val body: String,
)

object VoiceParser {
    fun parse(rawSpeech: String): ParsedVoiceNote {
        val trimmed = rawSpeech.trim()
        if (trimmed.isEmpty()) return ParsedVoiceNote("", "")

        val lower = trimmed.lowercase()

        val hasTitle = lower.contains("title") || lower.contains("tittle")
        val hasBody = lower.contains("body")

        // Strict Rule: If neither "title" nor "body" is explicitly spoken, put 100% of speech into body
        if (!hasTitle && !hasBody) {
            return ParsedVoiceNote(title = "", body = trimmed)
        }

        var extractedTitle = ""
        var extractedBody = ""

        // Scenario 1: Starts with "title " or "tittle "
        if (lower.startsWith("title ") || lower.startsWith("tittle ")) {
            val prefixLength = if (lower.startsWith("title ")) 6 else 7
            val afterTitle = trimmed.substring(prefixLength).trim()
            val lowerAfter = afterTitle.lowercase()

            if (lowerAfter.contains(" body ")) {
                val splitIndex = lowerAfter.indexOf(" body ")
                extractedTitle = afterTitle.substring(0, splitIndex).trim()
                extractedBody = afterTitle.substring(splitIndex + 6).trim()
            } else {
                extractedTitle = afterTitle
                extractedBody = ""
            }
            return ParsedVoiceNote(title = extractedTitle, body = extractedBody)
        }

        // Scenario 2: Starts with "body "
        if (lower.startsWith("body ")) {
            val afterBody = trimmed.substring(5).trim()
            return ParsedVoiceNote(title = "", body = afterBody)
        }

        // Scenario 3: Explicit "title" keyword inside sentence
        if (lower.contains(" title ") || lower.contains(" tittle ")) {
            val keyword = if (lower.contains(" title ")) " title " else " tittle "
            val splitIndex = lower.indexOf(keyword)
            val beforeTitle = trimmed.substring(0, splitIndex).trim()
            val afterTitle = trimmed.substring(splitIndex + keyword.length).trim()
            val lowerAfter = afterTitle.lowercase()

            if (lowerAfter.contains(" body ")) {
                val bodyIndex = lowerAfter.indexOf(" body ")
                extractedTitle = afterTitle.substring(0, bodyIndex).trim()
                extractedBody = (beforeTitle + " " + afterTitle.substring(bodyIndex + 6)).trim()
            } else {
                extractedTitle = afterTitle
                extractedBody = beforeTitle
            }
            return ParsedVoiceNote(title = extractedTitle, body = extractedBody)
        }

        return ParsedVoiceNote(title = "", body = trimmed)
    }
}