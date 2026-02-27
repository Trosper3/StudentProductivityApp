package com.example.studentproductivityapp.features.home

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.ScheduleActivity
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPdfManager).setOnClickListener {
            startActivity(Intent(this, PdfHubActivity::class.java))
            //Toast.makeText(this, "PDF Scanner coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnHomeworkCalendar).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
            //Toast.makeText(this, "Homework Calendar coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCampusMap).setOnClickListener {

            if (isInternetAvailable()) {
                startActivity(Intent(this, CampusMapActivity::class.java))
            } else {
                Toast.makeText(this, "Internet connection required for map.", Toast.LENGTH_SHORT).show()
            }

            //Toast.makeText(this, "Map coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnVideoLectures).setOnClickListener {

            if (isInternetAvailable()) {
                startActivity(Intent(this, VideoLectureActivity::class.java))
            } else {
                Toast.makeText(this, "Internet connection required for video lectures.", Toast.LENGTH_SHORT).show()
            }

            //Toast.makeText(this, "Video Lectures coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}