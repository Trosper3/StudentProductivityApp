package com.example.studentproductivityapp.features.pdf_scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.studentproductivityapp.R

class PdfHubActivity : AppCompatActivity() {

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted) {
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_hub)

        findViewById<Button>(R.id.btnScanNew).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        findViewById<Button>(R.id.btnViewSaved).setOnClickListener {
             startActivity(Intent(this, SavedPdfsActivity::class.java))
        }
    }
}