package com.example.studentproductivityapp.features.pdf_scanner

import android.content.Intent
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDatabase
import com.example.studentproductivityapp.features.pdf_scanner.db.SavedPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

class ReviewActivity : AppCompatActivity() {

    private val TAG = "ReviewActivity"
    private val DEBUG_VISIBLE_TEXT = false // Set to true to see OCR text in Red

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val rvPages = findViewById<RecyclerView>(R.id.rvPages)
        rvPages.layoutManager = LinearLayoutManager(this)

        val adapter = PageAdapter(ScanSession.pages) { position ->
            ScanSession.pages.removeAt(position)
            rvPages.adapter?.notifyItemRemoved(position)
        }
        rvPages.adapter = adapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                Collections.swap(ScanSession.pages, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(rvPages)

        findViewById<Button>(R.id.btnCreatePdf).setOnClickListener {
            val pdfName = findViewById<EditText>(R.id.etPdfName).text.toString()
            createPdf(pdfName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun createPdf(pdfName: String) {
        if (ScanSession.pages.isEmpty()) {
            Toast.makeText(this, "Add at least one page", Toast.LENGTH_SHORT).show()
            return
        }

        if (pdfName.isBlank()) {
            Toast.makeText(this, "Please enter a name for the PDF", Toast.LENGTH_SHORT).show()
            return
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val pdfDocument = PrintedPdfDocument(this, printAttributes)
        
        val textPaint = Paint().apply {
            color = if (DEBUG_VISIBLE_TEXT) Color.RED else Color.argb(1, 0, 0, 0) 
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        var totalLinesDrawn = 0

        for ((index, scanPage) in ScanSession.pages.withIndex()) {
            val page = pdfDocument.startPage(index)
            val canvas = page.canvas

            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, scanPage.picture)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, scanPage.picture)
                }

                val pageWidth = canvas.width.toFloat()
                val pageHeight = canvas.height.toFloat()
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()

                val pageRatio = pageWidth / pageHeight
                val bitmapRatio = bitmapWidth / bitmapHeight

                val destRect: Rect
                val scaleFactor: Float

                if (bitmapRatio > pageRatio) { // bitmap is wider, fit to width
                    val h = (pageWidth / bitmapRatio).toInt()
                    val top = ((pageHeight - h) / 2).toInt()
                    destRect = Rect(0, top, pageWidth.toInt(), top + h)
                    scaleFactor = pageWidth / bitmapWidth
                } else { // bitmap is taller, fit to height
                    val w = (pageHeight * bitmapRatio).toInt()
                    val left = ((pageWidth - w) / 2).toInt()
                    destRect = Rect(left, 0, left + w, pageHeight.toInt())
                    scaleFactor = pageHeight / bitmapHeight
                }

                canvas.drawBitmap(bitmap, null, destRect, null)

                // Overlay the OCR text line by line
                for (line in scanPage.textLines) {
                    val box = line.boundingBox
                    
                    val x = (box.left * scaleFactor) + destRect.left
                    // Adjust baseline: drawing at box.bottom is usually correct for the baseline
                    val y = (box.bottom * scaleFactor) + destRect.top 
                    
                    textPaint.textSize = box.height() * scaleFactor
                    canvas.drawText(line.text, x, y, textPaint)
                    totalLinesDrawn++
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error drawing page $index", e)
                Toast.makeText(this, "Error drawing page: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            pdfDocument.finishPage(page)
        }

        Toast.makeText(this, "PDF created with $totalLinesDrawn lines detected", Toast.LENGTH_SHORT).show()

        val fileName = if (pdfName.endsWith(".pdf")) pdfName else "$pdfName.pdf"
        val pdfDir = getExternalFilesDir("pdfs")

        if (pdfDir == null) {
            Toast.makeText(this, "Cannot access storage directory", Toast.LENGTH_SHORT).show()
            pdfDocument.close()
            return
        }

        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val file = File(pdfDir, fileName)

        try {
            FileOutputStream(file).use {
                pdfDocument.writeTo(it)
            }
        } catch (e: IOException) {
            pdfDocument.close()
            Toast.makeText(this, "Error saving PDF file: ${e.message}", Toast.LENGTH_LONG).show()
            return 
        }

        pdfDocument.close() 

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfDao = PdfDatabase.getDatabase(applicationContext).pdfDao()
                val savedPdf = SavedPdf(displayName = pdfName, filePath = file.absolutePath)
                pdfDao.insert(savedPdf)

                withContext(Dispatchers.Main) {
                    ScanSession.pages.clear()
                    val intent = Intent(this@ReviewActivity, PdfHubActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving PDF to database", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReviewActivity, "DB Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
