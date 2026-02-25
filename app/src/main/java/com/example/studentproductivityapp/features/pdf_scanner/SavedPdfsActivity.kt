package com.example.studentproductivityapp.features.pdf_scanner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDatabase
import com.example.studentproductivityapp.features.pdf_scanner.db.SavedPdf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class SavedPdfsActivity : AppCompatActivity() {

    private val viewModel: SavedPdfsViewModel by viewModels {
        SavedPdfsViewModelFactory(PdfDatabase.getDatabase(applicationContext).pdfDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_pdfs)

        val rvSavedPdfs = findViewById<RecyclerView>(R.id.rvSavedPdfs)
        rvSavedPdfs.layoutManager = LinearLayoutManager(this)

        val adapter = SavedPdfsAdapter(
            onClick = { pdf -> openPdf(pdf) },
            onDelete = { pdf -> deletePdf(pdf) }
        )
        rvSavedPdfs.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allPdfs.collect { pdfs ->
                    adapter.submitList(pdfs)
                }
            }
        }
    }

    private fun openPdf(pdf: SavedPdf) {
        val file = File(pdf.filePath)
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePdf(pdf: SavedPdf) {
        val file = File(pdf.filePath)
        if (file.exists()) {
            file.delete()
        }
        viewModel.delete(pdf)
    }
}
