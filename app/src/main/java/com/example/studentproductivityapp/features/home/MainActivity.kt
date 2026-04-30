package com.example.studentproductivityapp.features.home

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set LIGHT/DARK mode theme on startup
        val themePref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themePref)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Force status bar icons to be dark during Light Mode
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode

        val navigationBarView = findViewById<NavigationBarView>(R.id.bottomNavigationView)

        navigationBarView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_assignment_trackr -> {
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.nav_campus_map -> {
                    if (isInternetAvailable()) {
                        startActivity(Intent(this, CampusMapActivity::class.java))
                    } else {
                        Toast.makeText(this, "Internet connection required for map.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_pdf_scanner -> {
                    startActivity(Intent(this, PdfHubActivity::class.java))
                    true
                }
                R.id.nav_video_lectures -> {
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    if (!sharedPref.getBoolean("is_logged_in", false)) {
                        Toast.makeText(this, "Please login to access Video Lectures", Toast.LENGTH_LONG).show()
                        false
                    } else if (isInternetAvailable()) {
                        startActivity(Intent(this, VideoLectureActivity::class.java))
                        true
                    } else {
                        Toast.makeText(this, "Internet connection required.", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
                else -> false
            }
        }

        // Initial fragment load
        if (savedInstanceState == null) {
            val showSchedule = intent.getBooleanExtra("show_schedule", false)
            if (showSchedule) {
                navigationBarView.selectedItemId = R.id.nav_assignment_trackr
            } else {
                navigationBarView.selectedItemId = R.id.nav_home
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}