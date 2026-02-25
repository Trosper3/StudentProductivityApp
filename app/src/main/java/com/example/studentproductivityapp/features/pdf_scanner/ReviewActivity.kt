package com.example.studentproductivityapp.features.pdf_scanner

import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

                val pageRatio = page.canvas.width.toFloat() / page.canvas.height.toFloat()
                val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

                val destRect = if (bitmapRatio > pageRatio) { // bitmap is wider, fit to width
                    val h = (page.canvas.width / bitmapRatio).toInt()
                    val top = (page.canvas.height - h) / 2
                    Rect(0, top, page.canvas.width, top + h)
                } else { // bitmap is taller, fit to height
                    val w = (page.canvas.height * bitmapRatio).toInt()
                    val left = (page.canvas.width - w) / 2
                    Rect(left, 0, left + w, page.canvas.height)
                }

                canvas.drawBitmap(bitmap, null, destRect, null)

            } catch (e: Exception) {
                // Let the user know an error occurred and continue to the next page
                Toast.makeText(this, "Error drawing page: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            pdfDocument.finishPage(page)
        }

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
            return // Stop if file saving fails
        }

        pdfDocument.close() // Close the document AFTER writing it

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfDao = PdfDatabase.getDatabase(applicationContext).pdfDao()
                val savedPdf = SavedPdf(displayName = pdfName, filePath = file.absolutePath)
                pdfDao.insert(savedPdf)

                // Switch back to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    ScanSession.pages.clear()
                    Toast.makeText(this@ReviewActivity, "PDF saved successfully", Toast.LENGTH_LONG).show()

                    // Navigate back to the Hub
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
