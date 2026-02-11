package com.example.studentproductivityapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ReviewActivity : ComponentActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var textCount: TextView
    private lateinit var adapter: PageAdapter
    private val pdfExportService by lazy { PdfExportService(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            exportPdf()
        } else {
            Toast.makeText(this, "Permission denied. Cannot export PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        recycler = findViewById(R.id.recyclerPages)
        textCount = findViewById(R.id.textCount)

        adapter = PageAdapter(ScanSession.pages) { index ->
            if (index < 0 || index >= ScanSession.pages.size) return@PageAdapter
            ScanSession.pages.removeAt(index)
            adapter.notifyItemRemoved(index)
            adapter.notifyItemRangeChanged(index, ScanSession.pages.size - index)
            updateCount()
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnAddMore).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            if (ScanSession.pages.isEmpty()) {
                Toast.makeText(this, "No pages to export.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            exportPdf()
        }

        updateCount()
    }

    private fun exportPdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        pdfExportService.exportToPdf(ScanSession.pages) { success, fileUri ->
            if (success && fileUri != null) {
                sharePdf(fileUri)
            } else {
                Toast.makeText(this, "Failed to export PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sharePdf(fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            val uriToShare = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileUri
            } else {
                val authority = "${applicationContext.packageName}.provider"
                val file = File(fileUri.path!!)
                FileProvider.getUriForFile(this@ReviewActivity, authority, file)
            }
            putExtra(Intent.EXTRA_STREAM, uriToShare)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share PDF"))
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateCount()
    }

    private fun updateCount() {
        textCount.text = "Pages: ${ScanSession.pages.size}"
    }
}
