package com.example.studentproductivityapp.features.pdf_scanner

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class NoteParser(private val context: Context) {

    private val TAG = "NoteParser"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Regex for some common date formats (MM/DD/YYYY, MM-DD-YYYY, MMM DD)
    private val dateRegex = Regex("""(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})|([A-Z][a-z]{2,8}\s\d{1,2}(?:st|nd|rd|th)?(?:,?\s\d{2,4})?)""")

    fun tryParseDate(text: String): Long? {
        val match = dateRegex.find(text) ?: return null
        val dateStr = match.value
        
        val formats = listOf(
            "MM/dd/yyyy", "MM-dd-yyyy", "MM/dd/yy", "MM-dd-yy",
            "MMM dd, yyyy", "MMM dd yyyy", "MMMM dd, yyyy", "MMMM dd yyyy",
            "MMM dd", "MMMM dd"
        )

        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                val date = sdf.parse(dateStr)
                if (date != null) {
                    // If no year was provided, assume current year
                    if (!format.contains("y")) {
                        val cal = java.util.Calendar.getInstance()
                        val year = cal.get(java.util.Calendar.YEAR)
                        cal.time = date
                        cal.set(java.util.Calendar.YEAR, year)
                        return cal.timeInMillis
                    }
                    return date.time
                }
            } catch (e: Exception) {
                // Ignore and try next format
            }
        }
        return null
    }

    fun tryParseTitle(text: String): String? {
        val lines = text.split("\n")
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("assignment") || lower.contains("homework") || lower.contains("exam") || lower.contains("project")) {
                // Return the line but remove the prefix
                return line.replace(Regex("(?i)assignment|homework|exam|project|[:\\-]"), "").trim()
            }
        }
        return null
    }

    fun extractTextLinesFromImage(
        uri: Uri,
        onSuccess: (List<TextLine>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = mutableListOf<TextLine>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            line.boundingBox?.let { box ->
                                lines.add(TextLine(line.text, box))
                            }
                        }
                    }
                    onSuccess(lines)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
