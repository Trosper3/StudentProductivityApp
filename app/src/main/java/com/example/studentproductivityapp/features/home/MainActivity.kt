package com.example.studentproductivityapp.features.home

import android.Manifest
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.studentproductivityapp.database.Assignment
import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.studentproductivityapp.database.Assignment
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.AssignmentAdapter
import com.example.studentproductivityapp.CanvasRetrofitClient
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.database.AppDatabase
import com.example.studentproductivityapp.database.AssignmentRepository
import com.example.studentproductivityapp.ScheduleActivity
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.example.studentproductivityapp.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.viewmodel.AssignmentViewModelFactory
import com.example.studentproductivityapp.features.pdf_scanner.SavedPdfsAdapter
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDatabase
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.core.content.edit
import com.example.studentproductivityapp.features.notifications.NotificationHelper


class  MainActivity : AppCompatActivity() {

    private lateinit var viewModel: AssignmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Set LIGHT/DARK mode theme on startup
        val themePref = getSharedPreferences("user_prefs",Context.MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themePref)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        //Request NOTIFICATION PERMISSIONS
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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

        //Set current date
        val textDateView = findViewById<TextView>(R.id.textDateView)
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val currentDate = sdf.format(Date())
        textDateView.text = currentDate

        //settings button
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        //------------Assignments "Quick View" -----------------------------------
        val database = AppDatabase.getDatabase(this)
        val repo = AssignmentRepository(database.assignmentDao())
        val factory = AssignmentViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[AssignmentViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.rvQuickView)
        val adapter = AssignmentAdapter { assignment, isChecked ->
            viewModel.update(assignment.copy(isCompleted = isChecked))
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allAssignments.observe(this) { assignments ->
            val incompleteAssignments = assignments.filter { !it.isCompleted }
            val sortedAssignments = incompleteAssignments.sortedBy { it.dueDateMillis }
            val top3Assignments = sortedAssignments.take(3)
            adapter.submitList(top3Assignments)
        }


        //-------------------Recent Scans View------------------------------------
        val pdfDatabase = PdfDatabase.getDatabase(this)
        val pdfDao = pdfDatabase.pdfDao()
        val pdfRecyclerView = findViewById<RecyclerView>(R.id.rvRecentScans)
        val pdfAdapter = SavedPdfsAdapter(
            onClick = { pdf ->
                val intent = Intent(this@MainActivity, PdfHubActivity::class.java)
                startActivity(intent)
            },
            onDelete = { pdf ->
                lifecycleScope.launch {
                    pdfDao.delete(pdf)
                    Toast.makeText(this@MainActivity, "PDF deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )

        pdfRecyclerView.adapter = pdfAdapter
        pdfRecyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            pdfDao.getAllPdfs().collect { pdfList ->
                val recentPdfs = pdfList.take(3)
                pdfAdapter.submitList(recentPdfs)
            }
        }

        // Bottom navigation bar setup
        val navigationBarView = findViewById<NavigationBarView>(R.id.bottomNavigationView)
        navigationBarView.selectedItemId = R.id.nav_home
        navigationBarView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_assignment_trackr -> {
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_campus_map -> {
                    if (isInternetAvailable()) {
                        startActivity(Intent(this, CampusMapActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Internet connection required for map.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }

                R.id.nav_pdf_scanner -> {
                    startActivity(Intent(this, PdfHubActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_video_lectures -> {
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

                    if (!isLoggedIn) {
                        Toast.makeText(
                            this,
                            "Please login to access Video Lectures",
                            Toast.LENGTH_LONG
                        ).show()
                        false
                    } else if (isInternetAvailable()) {
                        startActivity(Intent(this, VideoLectureActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                        true
                    } else {
                        Toast.makeText(
                            this,
                            "Internet connection required for video lectures.",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                }

                else -> false
            }
        }
    }

    //-------------------Calculate Time of Day and Greet User------------------------------------
    private fun updateGreetingUI(textView: TextView, button: ImageButton) {
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
        }
        else {
            textView.text = "$timeOfDayGreeting, Student"
            button.setImageResource(android.R.drawable.ic_menu_myplaces)
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

    private fun showCanvasSyncDialog() {
        val input = EditText(this)
        input.hint = "Paste Personal Access Token here"
        input.setPadding(48, 48, 48, 48)

        AlertDialog.Builder(this)
            .setTitle("Sync with Canvas")
            .setMessage(
                "Generate a token un your Canvas Account Settings and paste it " +
                        "below to pull your upcoming ISU assignments!"
            )
            .setView(input)
            .setPositiveButton("Sync") { _, _ ->
                val token = input.text.toString().trim()
                if (token.isNotEmpty()) {
                    fetchCanvasAssignments(token)
                } else {
                    Toast.makeText(this, "Please enter a token", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchCanvasAssignments(token: String) {
        //ensure that the Canvas token has 'Bearer' prefix for enterprise APIs
        val authHeader = if (token.startsWith("Bearer")) token else "Bearer $token"
        val assignmentDao = AppDatabase.getDatabase(this@MainActivity).assignmentDao()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todoItems = CanvasRetrofitClient.api.getTodoItems(authHeader)

                //Conver Canvas API models to local Room Database model
                val newAssignments = todoItems.mapNotNull { item ->
                    item.assignment?.let { canvasAssign ->
                        var parsedMillis = 0L
                        if (!canvasAssign.due_at.isNullOrEmpty()) {
                            try{
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                parsedMillis = sdf.parse(canvasAssign.due_at).time

                            }
                            catch (ignored: Exception){ }
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
                    newAssignments.forEach { assignmentDao.insert(it) }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Canvas Sync Successful",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
                else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "ISU Todo list is completely clear now!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Canvas Sync Failed", Toast.LENGTH_SHORT)
                        .show()
                }
            }

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
            setText(sharedPref.getString("user_name", "User"))
        }
        layout.addView(nameInput)

        //Theme selection UI
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


        //Canvas Token Input
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
                    //refresh greeting UI
                    sharedPref.edit().putString("user_name", newName).apply()
                    updateGreetingUI(findViewById(R.id.textTodayView), findViewById(R.id.btnLogin))
                }

                //Save Theme and Recreate the Activity to apply it immediately
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
        //ensure that the Canvas token has 'Bearer' prefix for enterprise APIs
        val authHeader = if (token.startsWith("Bearer", ignoreCase = true)) token else "Bearer $token"
        val assignmentDao = AppDatabase.getDatabase(this@MainActivity).assignmentDao()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todoItems = CanvasRetrofitClient.api.getTodoItems(authHeader)

                //Convert Canvas API models to local Room Database model
                val newAssignments = todoItems.mapNotNull { item ->
                    item.assignment?.let { canvasAssign ->
                        var parsedMillis = 0L
                        if (!canvasAssign.due_at.isNullOrEmpty()) {
                            try{
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                parsedMillis = sdf.parse(canvasAssign.due_at)?.time ?: 0L

                            }
                            catch (_: Exception){ }
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
                    newAssignments.forEach { assignmentDao.insert(it)

                        //Schedule the alarm for each new assignment
                        NotificationHelper.scheduleNotification(this@MainActivity, it)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Canvas Sync Successful",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
                else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "ISU Todo list is completely clear now!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Canvas Sync Failed", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}