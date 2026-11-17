package com.example.nomemo.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.nomemo.data.entity.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Nothing OS 5.0 Industrial 1-Bit Thermal Receipt & Dot-Matrix Tape Exporter.
 *
 * Renders any Note into a high-contrast dot-matrix receipt image with
 * perforated edges, cryptographic checksum, and Nothing Crimson stamp.
 */
object NothingReceiptExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Formats the note as an industrial ASCII dot-matrix receipt.
     */
    fun formatAsText(note: Note): String {
        val dateStr = dateFormat.format(Date(note.timestamp))
        val checksum = String.format("%06X", abs((note.title + note.body + note.timestamp).hashCode()) % 0xFFFFFF)

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("  N O T H I N G   (R)   N O . M E M O")
        sb.appendLine("     INDUSTRIAL BENTO RECEIPT   ")
        sb.appendLine("================================")
        sb.appendLine("DATE     : $dateStr")
        sb.appendLine("TYPE     : ${note.type}")
        sb.appendLine("CHECKSUM : #$checksum")
        if (note.tags.isNotEmpty()) {
            sb.appendLine("TAGS     : ${note.tags.joinToString(" ") { "#$it" }}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("TITLE    : ${note.title.ifBlank { "UNTITLED" }.uppercase()}")
        sb.appendLine("--------------------------------")
        sb.appendLine(note.body.ifBlank { "[NO TEXT CONTENT]" })
        if (note.checklistItems.isNotEmpty()) {
            sb.appendLine("--------------------------------")
            sb.appendLine("ITEMS:")
            note.checklistItems.forEach { item ->
                val mark = if (item.isChecked) "[X]" else "[ ]"
                sb.appendLine("$mark ${item.text}")
            }
        }
        sb.appendLine("================================")
        sb.appendLine("   ZERO-FRICTION PERSISTENCE    ")
        sb.appendLine("================================")
        return sb.toString()
    }

    /**
     * Renders a high-resolution 1080px wide monochrome receipt bitmap.
     */
    fun generateReceiptBitmap(context: Context, note: Note): Bitmap {
        val width = 1080
        val padding = 64
        val contentWidth = width - (padding * 2)

        val bgPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            typeface = Typeface.MONOSPACE
            textSize = 34f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 42f
            isAntiAlias = true
        }

        val crimsonPaint = Paint().apply {
            color = Color.parseColor("#D71921")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 30f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#33333A")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val dotPerforationPaint = Paint().apply {
            color = Color.parseColor("#44444F")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Split text lines for layout measurement
        val bodyLines = mutableListOf<String>()
        val words = note.body.split("\\s+".toRegex())
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) <= contentWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) bodyLines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) bodyLines.add(currentLine.toString())

        val lineHeight = 50f
        val checklistLinesCount = note.checklistItems.size * 54f
        val calculatedHeight = (800 + (bodyLines.size * lineHeight) + checklistLinesCount).toInt()

        val bitmap = Bitmap.createBitmap(width, calculatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw Black OLED background
        canvas.drawRect(0f, 0f, width.toFloat(), calculatedHeight.toFloat(), bgPaint)

        // Top & Bottom Perforations
        val perforationStep = 32f
        var x = 20f
        while (x < width) {
            canvas.drawCircle(x, 16f, 6f, dotPerforationPaint)
            canvas.drawCircle(x, calculatedHeight - 16f, 6f, dotPerforationPaint)
            x += perforationStep
        }

        var y = 90f

        // Nothing Header
        canvas.drawText("NOTHING (R) NO.MEMO", padding.toFloat(), y, headerPaint)
        y += 46f
        canvas.drawText("INDUSTRIAL DOT-MATRIX RECEIPT // V5.0", padding.toFloat(), y, crimsonPaint)
        y += 40f

        // Separator
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, linePaint)
        y += 50f

        // Metadata
        val dateStr = dateFormat.format(Date(note.timestamp))
        val checksum = String.format("%06X", abs((note.title + note.body + note.timestamp).hashCode()) % 0xFFFFFF)

        textPaint.textSize = 28f
        canvas.drawText("DATE     : $dateStr", padding.toFloat(), y, textPaint)
        y += 40f
        canvas.drawText("SERIAL   : #NM-$checksum", padding.toFloat(), y, textPaint)
        y += 40f
        canvas.drawText("TYPE     : ${note.type}", padding.toFloat(), y, textPaint)
        y += 40f

        if (note.tags.isNotEmpty()) {
            canvas.drawText("TAGS     : ${note.tags.joinToString(" ") { "#$it" }}", padding.toFloat(), y, textPaint)
            y += 40f
        }

        y += 10f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, linePaint)
        y += 60f

        // Title
        headerPaint.textSize = 48f
        canvas.drawText(note.title.ifBlank { "UNTITLED NOTE" }.uppercase(), padding.toFloat(), y, headerPaint)
        y += 60f

        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, linePaint)
        y += 50f

        // Body Lines
        textPaint.textSize = 34f
        for (line in bodyLines) {
            canvas.drawText(line, padding.toFloat(), y, textPaint)
            y += lineHeight
        }

        // Checklist Items
        if (note.checklistItems.isNotEmpty()) {
            y += 20f
            canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, linePaint)
            y += 50f
            crimsonPaint.textSize = 30f
            canvas.drawText("CHECKLIST ITEMS:", padding.toFloat(), y, crimsonPaint)
            y += 46f

            note.checklistItems.forEach { item ->
                val checkSymbol = if (item.isChecked) "[X]" else "[ ]"
                canvas.drawText("$checkSymbol ${item.text}", padding.toFloat(), y, textPaint)
                y += 50f
            }
        }

        y += 40f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, linePaint)
        y += 50f

        // Footer Stamp
        textPaint.textSize = 26f
        canvas.drawText("ZERO-FRICTION PERSISTENCE // END OF TAPE", padding.toFloat(), y, textPaint)

        return bitmap
    }

    /**
     * Shares the note as a graphic receipt or text to Android share sheet.
     */
    fun shareReceipt(context: Context, note: Note) {
        try {
            val bitmap = generateReceiptBitmap(context, note)
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            val file = File(cachePath, "no_memo_receipt_${note.id}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "NO.MEMO // ${note.title}")
                putExtra(Intent.EXTRA_TEXT, formatAsText(note))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "SHARE NOTHING RECEIPT"))
        } catch (_: Exception) {
            // Fallback to text share
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formatAsText(note))
            }
            context.startActivity(Intent.createChooser(textIntent, "SHARE NO.MEMO TAPE"))
        }
    }
}
