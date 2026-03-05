package com.example.studentproductivityapp.features.video_lectures

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.ScheduleActivity
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.google.android.material.navigation.NavigationBarView

class VideoLectureActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_video_lecture)

        val etVideoUrl = findViewById<EditText>(R.id.etVideoUrl)
        val btnLoadVideo = findViewById<Button>(R.id.btnLoadVideo)
        val webView = findViewById<WebView>(R.id.webView)
        val etSearchPhrase = findViewById<EditText>(R.id.etSearchPhrase)
        val btnSearch = findViewById<Button>(R.id.btnSearch)

        // --- Performance Optimizations for Video ---
        // Enable Hardware acceleration for the WebView
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.settings.apply {
            javaScriptEnabled = true
            // Improve caching behavior
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            // Allow videos to autoplay and buffer without direct user interaction on the video element
            mediaPlaybackRequiresUserGesture = false
            // Enable other storage mechanisms that modern web players might use
            domStorageEnabled = true
            databaseEnabled = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        btnLoadVideo.setOnClickListener {
            val url = etVideoUrl.text.toString()
            if (url.isNotEmpty()) {
                webView.loadUrl(url)
            }
        }

        btnSearch.setOnClickListener {
            val phrase = etSearchPhrase.text.toString()
            if (phrase.isNotEmpty()) {
                // TODO: Implement Zoom API call and transcript parsing
                Toast.makeText(this, "Search functionality coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        //---------------Navigation Bar Logic ----------------

        // Use NavigationBarView to support both BottomNavigationView and NavigationRailView
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottomNavigationView)

        // Set the highlighted tab in nav bar
        bottomNavigationView.selectedItemId = R.id.nav_video_lectures

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_assignment_trackr -> {
                    startActivity(Intent(this, ScheduleActivity::class.java))
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

                R.id.nav_pdf_scanner -> {
                    startActivity(Intent(this, PdfHubActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }

                R.id.nav_video_lectures -> true

                else -> false
            }
        }
    }
}