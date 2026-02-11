package com.example.studentproductivityapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

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
        setContentView(R.layout.activity_camera)

        // Button: open CameraX screen
        findViewById<Button>(R.id.btnOpenCamera).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // Optional: open review screen (if you have it)
        val reviewBtn = findViewById<Button?>(R.id.btnReview)
        reviewBtn?.setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }
    }
}
