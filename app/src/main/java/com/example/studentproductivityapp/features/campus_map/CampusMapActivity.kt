package com.example.studentproductivityapp.features.campus_map

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.google.android.material.navigation.NavigationBarView

class CampusMapActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_map)

        // Enable remote debugging for the WebView.
        WebView.setWebContentsDebuggingEnabled(true)

        val webView = findViewById<WebView>(R.id.webView)

        // Configure WebView settings for complex interactive web pages
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.loadUrl("https://maps.isu.edu/?id=2108#!ct/71803,74197,76334,94335,94337,94338?s/")


        //---------------Navigation Bar Logic ----------------

        // Use NavigationBarView to support both BottomNavigationView and NavigationRailView
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottomNavigationView)

        // Set the highlighted tab in nav bar if it exists (might be missing in landscape if not handled)
        bottomNavigationView?.let { navView ->
            navView.selectedItemId = R.id.nav_campus_map

            navView.setOnItemSelectedListener { item ->
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


                    R.id.nav_campus_map -> true

                    R.id.nav_pdf_scanner -> {
                        startActivity(Intent(this, PdfHubActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                        true
                    }

                    R.id.nav_video_lectures -> {
                        startActivity(Intent(this, VideoLectureActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                        true
                    }

                    else -> false
                }
            }
        }
    }
}
