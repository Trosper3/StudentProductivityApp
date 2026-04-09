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
import androidx.lifecycle.lifecycleScope
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.ScheduleActivity
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VideoLectureActivity : ComponentActivity() {

    data class TranscriptEntry(val text: String, val timestampSeconds: Int)

    private var transcript = mutableListOf<TranscriptEntry>()
    private var searchResults = listOf<TranscriptEntry>()
    private var currentResultIndex = 0

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

        // Load initial mock data for testing
        loadDefaultTranscript()

        btnLoad.setOnClickListener {
            val url = etVideoUrl.text.toString()
            if (url.isNotEmpty()) {
                val embedUrl = if (url.contains("youtube.com/watch?v=")) 
                    url.replace("watch?v=", "embed/") else url
                webView.loadUrl(embedUrl)
                fetchTranscriptFromZoom(url)
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

    private fun loadDefaultTranscript() {
        transcript.clear()
        transcript.addAll(listOf(
            TranscriptEntry("Welcome to the lecture.", 0),
            TranscriptEntry("Make Zoom your default application.", 10),
            TranscriptEntry("Today we cover search APIs.", 45),
            TranscriptEntry("Android development is fun.", 120)
        ))
    }

    /**
     * Simulates a call to the Zoom Cloud Recording API.
     * In a production app, you would use Retrofit or OkHttp to call 
     * GET https://api.zoom.us/v2/meetings/{meetingId}/recordings
     */
    private fun fetchTranscriptFromZoom(videoUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Mocking network delay
                delay(800)

                // This simulates the JSON structure returned by Zoom's Recording API
                val mockApiResponse = """
                {
                    "recording_files": [
                        {
                            "file_type": "TRANSCRIPT",
                            "transcript_data": [
                                {"text": "Zoom recording started.", "start_time": 0},
                                {"text": "In this session, we analyze student productivity.", "start_time": 30},
                                {"text": "Let's look at the search feature implementation.", "start_time": 90},
                                {"text": "The API returns a JSON with timestamps.", "start_time": 150},
                                {"text": "Make Zoom your default app for all meetings.", "start_time": 210},
                                {"text": "Conclusion of the recording.", "start_time": 300}
                            ]
                        }
                    ]
                }
                """.trimIndent()

                val jsonObject = JSONObject(mockApiResponse)
                val files = jsonObject.getJSONArray("recording_files")
                val apiTranscript = mutableListOf<TranscriptEntry>()

                for (i in 0 until files.length()) {
                    val file = files.getJSONObject(i)
                    if (file.getString("file_type") == "TRANSCRIPT") {
                        val dataArray = file.getJSONArray("transcript_data")
                        for (j in 0 until dataArray.length()) {
                            val entry = dataArray.getJSONObject(j)
                            apiTranscript.add(TranscriptEntry(
                                entry.getString("text"),
                                entry.getInt("start_time")
                            ))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    transcript.clear()
                    transcript.addAll(apiTranscript)
                    // Reset search results for the new video
                    searchResults = emptyList()
                    currentResultIndex = 0
                    Toast.makeText(this@VideoLectureActivity, "Transcript Synced via Zoom API", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VideoLectureActivity, "API Sync Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSearch(phrase: String) {
        searchResults = transcript.filter { it.text.contains(phrase, ignoreCase = true) }
        if (searchResults.isEmpty()) {
            Toast.makeText(this, "Phrase not found in current recording", Toast.LENGTH_SHORT).show()
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
        val js = "javascript:(function() { var v = document.querySelector('video'); if(v){ v.currentTime = $seconds; v.play(); } })()"
        webView.evaluateJavascript(js, null)
    }

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
