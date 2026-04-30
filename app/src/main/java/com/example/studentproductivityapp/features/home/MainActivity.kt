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
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.CanvasRetrofitClient
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.Assignment
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.notifications.NotificationHelper
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set LIGHT/DARK mode theme on startup (Your Feature)
        val themePref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themePref)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Request NOTIFICATION PERMISSIONS (Your Feature)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val textTodayView = findViewById<TextView>(R.id.textTodayView)
        val btnLogin = findViewById<ImageButton>(R.id.btnLogin)
        val textDateView = findViewById<TextView>(R.id.textDateView)

        // Set current date
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        textDateView.text = sdf.format(Date())

        // Check login status and update UI
        updateGreetingUI(textTodayView, btnLogin)

        btnLogin.setOnClickListener {
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
            val currentName = sharedPref.getString("user_name", "Student")

            if (isLoggedIn) {
                // Logout
                sharedPref.edit { putBoolean("is_logged_in", false) }
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Login
                sharedPref.edit { putBoolean("is_logged_in", true) }
                Toast.makeText(this, "Welcome back, $currentName", Toast.LENGTH_SHORT).show()
            }
            updateGreetingUI(textTodayView, btnLogin)
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        // Bottom navigation bar setup (Partner's Fragment Logic)
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

    //-------------------Calculate Time of Day and Greet User------------------------------------
    fun updateGreetingUI(textView: TextView, button: ImageButton) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        val savedName = sharedPref.getString("user_name", "Student")

        // Fetch current hour of day
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

    //-------------------Unified Settings Dialog for Changing name, theme, and Syncing Canvas------------------------------------
    private fun showSettingsDialog() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val paddingPx = (24 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        val nameInput = EditText(this).apply {
            hint = "Change Display Name"
            setText(sharedPref.getString("user_name", "Student"))
        }
        layout.addView(nameInput)

        // Theme selection UI
        layout.addView(TextView(this).apply{
            text = "\nApp Theme:"; textSize = 16f; setPadding(0,16,0,8)
        })
        val themeGroup = RadioGroup(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnSystem = RadioButton(this).apply { text = "System "; id = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
        val btnLight = RadioButton(this).apply { text = "Light "; id = AppCompatDelegate.MODE_NIGHT_NO }
        val btnDark = RadioButton(this).apply { text = "Dark "; id = AppCompatDelegate.MODE_NIGHT_YES }
        themeGroup.addView(btnSystem)
        themeGroup.addView(btnLight)
        themeGroup.addView(btnDark)
        themeGroup.check(sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        layout.addView(themeGroup)

        // Canvas Token Input
        val tokenInput = EditText(this).apply{
            hint = "\nPaste Personal Access Token Here"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }
        layout.addView(tokenInput)

        AlertDialog.Builder(this)
            .setTitle("App Settings")
            .setView(layout)
            .setPositiveButton("Save & Sync") { _, _ ->
                val newName = nameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    sharedPref.edit().putString("user_name", newName).apply()
                    updateGreetingUI(findViewById(R.id.textTodayView), findViewById(R.id.btnLogin))
                }

                // Save Theme and Recreate the Activity to apply it immediately
                val selectedTheme = themeGroup.checkedRadioButtonId
                if (selectedTheme != sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
                    sharedPref.edit().putInt("theme_mode", selectedTheme).apply()
                    AppCompatDelegate.setDefaultNightMode(selectedTheme)
                }

                val token = tokenInput.text.toString().trim()
                if (token.isNotEmpty()) {
                    fetchCanvasAssignments(token)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchCanvasAssignments(token: String) {
        val authHeader = if (token.startsWith("Bearer", ignoreCase = true)) token else "Bearer $token"
        val assignmentDao = AppDatabase.getDatabase(this@MainActivity).assignmentDao()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todoItems = CanvasRetrofitClient.api.getTodoItems(authHeader)

                val newAssignments = todoItems.mapNotNull { item ->
                    item.assignment?.let { canvasAssign ->
                        var parsedMillis = 0L
                        if (!canvasAssign.due_at.isNullOrEmpty()) {
                            try{
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                parsedMillis = sdf.parse(canvasAssign.due_at)?.time ?: 0L
                            } catch (_: Exception){ }
                        }
                        Assignment(
                            title = canvasAssign.name,
                            courseName = "Course ID: ${canvasAssign.course_id}",
                            dueDateMillis = parsedMillis,
                            isCompleted = false
                        )
                    }
                }

                if (newAssignments.isNotEmpty()) {
                    newAssignments.forEach {
                        assignmentDao.insert(it)
                        // Schedule the alarm for each new assignment (Your Feature)
                        NotificationHelper.scheduleNotification(this@MainActivity, it)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Canvas Sync Successful", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "ISU Todo list is completely clear now!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Canvas Sync Failed", Toast.LENGTH_SHORT).show()
                }
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
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}