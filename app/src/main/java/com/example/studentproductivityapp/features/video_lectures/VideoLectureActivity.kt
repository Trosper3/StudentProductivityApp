package com.example.studentproductivityapp.features.video_lectures

import android.annotation.SuppressLint
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

class VideoLectureActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}
