package com.example.studentproductivityapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfExportService(private val context: Context) {

    fun exportToPdf(pages: List<ScanPage>, onFinished: (success: Boolean, fileUri: Uri?) -> Unit) {
        val document = PdfDocument()

        try {
            for ((index, page) in pages.withIndex()) {
                val bitmap = uriToBitmap(page.uri) ?: continue

                // Create a page with the same dimensions as the bitmap.
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val pdfPage = document.startPage(pageInfo)

                // Draw the bitmap onto the page.
                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(pdfPage)
                bitmap.recycle()
            }

            // Write the document to a file in the Downloads folder.
            val fileName = "scan_${System.currentTimeMillis()}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        document.writeTo(outputStream)
                        onFinished(true, uri)
                    }
                } else {
                    onFinished(false, null)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                document.writeTo(FileOutputStream(file))
                onFinished(true, Uri.fromFile(file))
            }

        } catch (e: IOException) {
            e.printStackTrace()
            onFinished(false, null)
        } finally {
            document.close()
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
