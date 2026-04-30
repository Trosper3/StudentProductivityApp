package com.example.studentproductivityapp.features.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.AssignmentAdapter
import com.example.studentproductivityapp.features.assignments.CanvasRetrofitClient
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.Assignment
import com.example.studentproductivityapp.features.assignments.database.AssignmentRepository
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModelFactory
import com.example.studentproductivityapp.features.notifications.NotificationHelper
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.pdf_scanner.SavedPdfsAdapter
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var viewModel: AssignmentViewModel
    private var _view: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _view = inflater.inflate(R.layout.fragment_home, container, false)
        return _view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request NOTIFICATION PERMISSIONS (Your Feature)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireActivity().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requireActivity().requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val textTodayView = view.findViewById<TextView>(R.id.textTodayView)
        val btnLogin = view.findViewById<ImageButton>(R.id.btnLogin)
        val textDateView = view.findViewById<TextView>(R.id.textDateView)

        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        textDateView.text = sdf.format(Date())

        updateGreetingUI(textTodayView, btnLogin)

        btnLogin.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
            val currentName = sharedPref.getString("user_name", "Student")

            if (isLoggedIn) {
                sharedPref.edit { putBoolean("is_logged_in", false) }
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            } else {
                sharedPref.edit { putBoolean("is_logged_in", true) }
                Toast.makeText(requireContext(), "Welcome back, $currentName", Toast.LENGTH_SHORT).show()
            }
            updateGreetingUI(textTodayView, btnLogin)
        }

        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        // Assignments "Quick View" Setup
        val database = AppDatabase.getDatabase(requireContext())
        val repo = AssignmentRepository(database.assignmentDao())
        val factory = AssignmentViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[AssignmentViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvQuickView)
        val adapter = AssignmentAdapter { assignment, isChecked ->
            viewModel.update(assignment.copy(isCompleted = isChecked))
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.allAssignments.observe(viewLifecycleOwner) { assignments ->
            val incompleteAssignments = assignments.filter { !it.isCompleted }
            val sortedAssignments = incompleteAssignments.sortedBy { it.dueDateMillis }
            adapter.submitList(sortedAssignments.take(3))
        }

        // Recent Scans Setup
        val pdfDatabase = PdfDatabase.getDatabase(requireContext())
        val pdfDao = pdfDatabase.pdfDao()
        val pdfRecyclerView = view.findViewById<RecyclerView>(R.id.rvRecentScans)
        val pdfAdapter = SavedPdfsAdapter(
            onClick = { startActivity(Intent(requireContext(), PdfHubActivity::class.java)) },
            onDelete = { pdf ->
                lifecycleScope.launch {
                    pdfDao.delete(pdf)
                    Toast.makeText(requireContext(), "PDF deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )

        pdfRecyclerView.adapter = pdfAdapter
        pdfRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            pdfDao.getAllPdfs().collect { pdfList ->
                pdfAdapter.submitList(pdfList.take(3))
            }
        }
    }

    private fun updateGreetingUI(textView: TextView, button: ImageButton) {
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val savedName = sharedPref.getString("user_name", "Student")

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

    private fun showSettingsDialog() {
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val paddingPx = (24 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        val nameInput = EditText(requireContext()).apply {
            hint = "Change Display Name"
            setText(sharedPref.getString("user_name", "Student"))
        }
        layout.addView(nameInput)

        // Theme selection UI
        layout.addView(TextView(requireContext()).apply {
            text = "\nApp Theme:"; textSize = 16f; setPadding(0, 16, 0, 8)
        })
        val themeGroup = RadioGroup(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val btnSystem = RadioButton(requireContext()).apply { text = "System "; id = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
        val btnLight = RadioButton(requireContext()).apply { text = "Light "; id = AppCompatDelegate.MODE_NIGHT_NO }
        val btnDark = RadioButton(requireContext()).apply { text = "Dark "; id = AppCompatDelegate.MODE_NIGHT_YES }
        themeGroup.addView(btnSystem)
        themeGroup.addView(btnLight)
        themeGroup.addView(btnDark)
        themeGroup.check(sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        layout.addView(themeGroup)

        // Canvas Token Input
        val tokenInput = EditText(requireContext()).apply {
            hint = "\nPaste Personal Access Token Here"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }
        layout.addView(tokenInput)

        AlertDialog.Builder(requireContext())
            .setTitle("App Settings")
            .setView(layout)
            .setPositiveButton("Save & Sync") { _, _ ->
                val newName = nameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    sharedPref.edit().putString("user_name", newName).apply()
                    _view?.let { v ->
                        updateGreetingUI(v.findViewById(R.id.textTodayView), v.findViewById(R.id.btnLogin))
                    }
                }

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
        val assignmentDao = AppDatabase.getDatabase(requireContext()).assignmentDao()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todoItems = CanvasRetrofitClient.api.getTodoItems(authHeader)

                val newAssignments = todoItems.mapNotNull { item ->
                    item.assignment?.let { canvasAssign ->
                        var parsedMillis = 0L
                        if (!canvasAssign.due_at.isNullOrEmpty()) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                parsedMillis = sdf.parse(canvasAssign.due_at)?.time ?: 0L
                            } catch (_: Exception) {}
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
                        NotificationHelper.scheduleNotification(requireContext(), it)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Canvas Sync Successful", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "ISU Todo list is completely clear now!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Canvas Sync Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}