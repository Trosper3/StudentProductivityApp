package com.example.studentproductivityapp.features.pdf_scanner

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.google.android.material.navigation.NavigationBarView

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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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

        //---------------Navigation Bar Logic ----------------

        // Use NavigationBarView to support both BottomNavigationView and NavigationRailView
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottomNavigationView)

        // Set the highlighted tab in nav bar
        bottomNavigationView.selectedItemId = R.id.nav_pdf_scanner

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_assignment_trackr -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("show_schedule", true)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }


                R.id.nav_campus_map -> {
                    startActivity(Intent(this, CampusMapActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_pdf_scanner -> true

                R.id.nav_video_lectures -> {
                    startActivity(Intent(this, VideoLectureActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}