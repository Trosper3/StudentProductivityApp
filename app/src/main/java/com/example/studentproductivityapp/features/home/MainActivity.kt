package com.example.studentproductivityapp.features.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.google.android.material.navigation.NavigationBarView
import java.util.Calendar
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Set LIGHT/DARK mode theme on startup
        val themePref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themePref)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        //Request NOTIFICATION PERMISSIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val textTodayView = findViewById<TextView>(R.id.textTodayView)
        val btnLogin = findViewById<ImageButton>(R.id.btnLogin)

        // Check login status and update UI
        updateGreetingUI(textTodayView, btnLogin)

        btnLogin.setOnClickListener {
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

            if (isLoggedIn) {
                // Logout
                sharedPref.edit { putBoolean("is_logged_in", false) }
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Login (Simulated)
                sharedPref.edit { putBoolean("is_logged_in", true) }
                Toast.makeText(this, "Logged in as Jerry", Toast.LENGTH_SHORT).show()
            }
            updateGreetingUI(textTodayView, btnLogin)
        }

        // Bottom navigation bar setup
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
                    val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

                    if (!isLoggedIn) {
                        Toast.makeText(this, "Please login to access Video Lectures", Toast.LENGTH_LONG).show()
                        false
                    } else if (isInternetAvailable()) {
                        startActivity(Intent(this, VideoLectureActivity::class.java))
                        true
                    } else {
                        Toast.makeText(this, "Internet connection required for video lectures.", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
                else -> false
            }
        }

        // Initial fragment
        if (savedInstanceState == null) {
            val showSchedule = intent.getBooleanExtra("show_schedule", false)
            if (showSchedule) {
                navigationBarView.selectedItemId = R.id.nav_assignment_trackr
            } else {
                navigationBarView.selectedItemId = R.id.nav_home
            }
        }
    }

    //-------------------Calculate Time of Day and Greet User------------------------------------
    fun updateGreetingUI(textView: TextView, button: ImageButton) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        //Default to "User" if not logged in
        val savedName = sharedPref.getString("user_name", "User")

        //fetch current hour of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDayGreeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }

        if (isLoggedIn) {
            textView.text = "$timeOfDayGreeting, $savedName"
            button.setImageResource(android.R.drawable.ic_lock_power_off)
        } else {
            textView.text = "$timeOfDayGreeting, Student"
            button.setImageResource(android.R.drawable.ic_menu_myplaces)
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
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
