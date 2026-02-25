package com.example.studentproductivityapp.features.campus_map

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.example.studentproductivityapp.R

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
    }
}
