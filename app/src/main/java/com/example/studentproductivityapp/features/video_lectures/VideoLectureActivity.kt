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
import androidx.appcompat.app.AppCompatActivity
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.ScheduleActivity
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.google.android.material.navigation.NavigationBarView
import java.io.BufferedReader
import java.io.InputStreamReader


class VideoLectureActivity : AppCompatActivity() {

    data class TranscriptEntry(val text: String, val timestampSeconds: Int)

    private var transcript = mutableListOf<TranscriptEntry>()
    private var searchResults = listOf<TranscriptEntry>()
    private var currentResultIndex = 0

    private var currentVideoId = ""

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_video_lecture)

        val etVideoUrl = findViewById<EditText>(R.id.etVideoUrl)
        val etSearch = findViewById<EditText>(R.id.etSearchPhrase)
        val btnLoad = findViewById<Button>(R.id.btnLoadVideo)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnPrev = findViewById<Button>(R.id.btnPrev)
        webView = findViewById(R.id.webView)

        setupWebView()
        setupNavigation()

//        // Load initial mock data for testing
//        loadDefaultTranscript()

        btnLoad.setOnClickListener {
            val url = etVideoUrl.text.toString()

            currentVideoId = when {
                url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
                else -> ""

            }
            if (currentVideoId.isNotEmpty()){
                loadVideoAtTime(0)
                loadLocalTranscript()
            }
            else {
                Toast.makeText(this, "Please enter a Youtube video URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnSearch.setOnClickListener {
            val phrase = etSearch.text.toString().trim()
            if (phrase.isEmpty()) {
                Toast.makeText(this, "Enter search phrase", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSearch(phrase)
        }

        btnNext.setOnClickListener {
            if (searchResults.isNotEmpty()) {
                currentResultIndex = (currentResultIndex + 1) % searchResults.size
                jumpToResult()
            }
        }

        btnPrev.setOnClickListener {
            if (searchResults.isNotEmpty()) {
                currentResultIndex = if (currentResultIndex > 0) currentResultIndex - 1 else searchResults.size - 1
                jumpToResult()
            }
        }
    }

    private fun setupWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun loadLocalTranscript() {
        transcript.clear()
        try {
            val inputStream = assets.open("NoteGPT_TRANSCRIPT_Lecture 1 Introduction to CS and Programming Using Python.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))

            var currentTimestampSeconds = 0

            //Time for REGEX time format
            val timeRegex = Regex("\\b(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b")
            reader.forEachLine { line ->
                val match = timeRegex.find(line)

                if (match != null) {
                    val parts = match.value.split(":")
                    currentTimestampSeconds = if (parts.size == 3) {
                        parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                    } else if (parts.size == 2) {
                        parts[0].toInt() * 60 + parts[1].toInt()
                    }
                    else {
                        0
                    }
                }

                val cleanText = line.replace(timeRegex, "").replace(Regex("\\[.*?]"), "").trim()

                if(cleanText.isNotEmpty()) {
                    transcript.add(TranscriptEntry(cleanText, currentTimestampSeconds))
                }

            }
            Toast.makeText(this, "Transcript Loaded", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Toast.makeText(this, "Transcript Load Failed", Toast.LENGTH_SHORT).show()
        }

    }

//    /**
//     * Simulates a call to the Zoom Cloud Recording API.
//     * In a production app, you would use Retrofit or OkHttp to call
//     * GET https://api.zoom.us/v2/meetings/{meetingId}/recordings
//     */
//    private fun fetchTranscriptFromZoom(videoUrl: String) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                // Mocking network delay
//                delay(800)
//
//                // This simulates the JSON structure returned by Zoom's Recording API
//                val mockApiResponse = """
//                {
//                    "recording_files": [
//                        {
//                            "file_type": "TRANSCRIPT",
//                            "transcript_data": [
//                                {"text": "Zoom recording started.", "start_time": 0},
//                                {"text": "In this session, we analyze student productivity.", "start_time": 30},
//                                {"text": "Let's look at the search feature implementation.", "start_time": 90},
//                                {"text": "The API returns a JSON with timestamps.", "start_time": 150},
//                                {"text": "Make Zoom your default app for all meetings.", "start_time": 210},
//                                {"text": "Conclusion of the recording.", "start_time": 300}
//                            ]
//                        }
//                    ]
//                }
//                """.trimIndent()
//
//                val jsonObject = JSONObject(mockApiResponse)
//                val files = jsonObject.getJSONArray("recording_files")
//                val apiTranscript = mutableListOf<TranscriptEntry>()
//
//                for (i in 0 until files.length()) {
//                    val file = files.getJSONObject(i)
//                    if (file.getString("file_type") == "TRANSCRIPT") {
//                        val dataArray = file.getJSONArray("transcript_data")
//                        for (j in 0 until dataArray.length()) {
//                            val entry = dataArray.getJSONObject(j)
//                            apiTranscript.add(TranscriptEntry(
//                                entry.getString("text"),
//                                entry.getInt("start_time")
//                            ))
//                        }
//                    }
//                }
//
//                withContext(Dispatchers.Main) {
//                    transcript.clear()
//                    transcript.addAll(apiTranscript)
//                    // Reset search results for the new video
//                    searchResults = emptyList()
//                    currentResultIndex = 0
//                    Toast.makeText(this@VideoLectureActivity, "Transcript Synced via Zoom API", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@VideoLectureActivity, "API Sync Failed", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    private fun performSearch(phrase: String) {
        if (transcript.isEmpty()) {
            Toast.makeText(this, "Load a video first to process transcript", Toast.LENGTH_SHORT).show()
            return
        }

        searchResults = transcript.filter { it.text.contains(phrase, ignoreCase = true) }

        if(searchResults.isEmpty()) {
            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show()
            return
        }

        currentResultIndex = 0
        jumpToResult()
    }

    private fun jumpToResult() {
        val entry = searchResults[currentResultIndex]
        Toast.makeText(this, "Match ${currentResultIndex + 1}/${searchResults.size} at ${entry.timestampSeconds}s", Toast.LENGTH_SHORT).show()
        seekTo(entry.timestampSeconds)
    }

    private fun seekTo(seconds: Int) {
        loadVideoAtTime(seconds)
    }

    private fun loadVideoAtTime(seconds: Int) {
        if (currentVideoId.isEmpty()) return

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background-color:#000;">
            <iframe width="100%" height="100%" 
            src="https://www.youtube.com/embed/$currentVideoId?start=$seconds&autoplay=1&playsinline=1&enablejsapi=1&origin=https://localhost" 
            frameborder="0" 
            allow="autoplay; encrypted-media; picture-in-picture"
            allowfullscreen>
            </iframe>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://localhost", html, "text/html", "utf-8",null)
    }

    //---Bottom navigation logic---
    private fun setupNavigation() {
        val bottomNav = findViewById<NavigationBarView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_video_lectures
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_assignment_trackr -> { startActivity(Intent(this, ScheduleActivity::class.java)); finish(); true }
                R.id.nav_campus_map -> { startActivity(Intent(this, CampusMapActivity::class.java)); finish(); true }
                R.id.nav_pdf_scanner -> { startActivity(Intent(this, PdfHubActivity::class.java)); finish(); true }
                R.id.nav_video_lectures -> true
                else -> false
            }
        }
    }
}
