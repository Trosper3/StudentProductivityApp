package com.example.studentproductivityapp.features.home

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.features.assignments.AssignmentAdapter
import com.example.studentproductivityapp.features.assignments.CanvasRetrofitClient
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.Assignment
import com.example.studentproductivityapp.features.assignments.database.AssignmentRepository
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.pdf_scanner.SavedPdfsAdapter
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDatabase
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var viewModel: AssignmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textTodayView = view.findViewById<TextView>(R.id.textTodayView)
        val btnLogin = view.findViewById<ImageButton>(R.id.btnLogin)

        updateLoginUI(textTodayView, btnLogin)

        btnLogin.setOnClickListener {
            val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

            if (isLoggedIn) {
                sharedPref.edit().putBoolean("is_logged_in", false).apply()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            } else {
                sharedPref.edit().putBoolean("is_logged_in", true).apply()
                Toast.makeText(requireContext(), "Logged in as Jerry", Toast.LENGTH_SHORT).show()
            }
            updateLoginUI(textTodayView, btnLogin)
        }

        val textDateView = view.findViewById<TextView>(R.id.textDateView)
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val currentDate = sdf.format(Date())
        textDateView.text = currentDate

        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showCanvasSyncDialog()
        }

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
            val top3Assignments = sortedAssignments.take(3)
            adapter.submitList(top3Assignments)
        }

        val pdfDatabase = PdfDatabase.getDatabase(requireContext())
        val pdfDao = pdfDatabase.pdfDao()
        val pdfRecyclerView = view.findViewById<RecyclerView>(R.id.rvRecentScans)
        val pdfAdapter = SavedPdfsAdapter(
            onClick = { pdf ->
                val intent = Intent(requireContext(), PdfHubActivity::class.java)
                startActivity(intent)
            },
            onDelete = { pdf ->
                viewLifecycleOwner.lifecycleScope.launch {
                    pdfDao.delete(pdf)
                    Toast.makeText(requireContext(), "PDF deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )

        pdfRecyclerView.adapter = pdfAdapter
        pdfRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            pdfDao.getAllPdfs().collect { pdfList ->
                val recentPdfs = pdfList.take(3)
                pdfAdapter.submitList(recentPdfs)
            }
        }
    }

    private fun updateLoginUI(textView: TextView, button: ImageButton) {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            textView.text = "Good Afternoon, Jerry"
            button.setImageResource(android.R.drawable.ic_lock_power_off)
        } else {
            textView.text = "Good Afternoon, Student"
            button.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    private fun showCanvasSyncDialog() {
        val input = EditText(requireContext())
        input.hint = "Paste Personal Access Token here"
        input.setPadding(48, 48, 48, 48)

        AlertDialog.Builder(requireContext())
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
                    Toast.makeText(requireContext(), "Please enter a token", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchCanvasAssignments(token: String) {
        val authHeader = if (token.startsWith("Bearer")) token else "Bearer $token"
        val assignmentDao = AppDatabase.getDatabase(requireContext()).assignmentDao()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todoItems = CanvasRetrofitClient.api.getTodoItems(authHeader)
                val newAssignments = todoItems.mapNotNull { item ->
                    item.assignment?.let { canvasAssign ->
                        var parsedMillis = 0L
                        if (!canvasAssign.due_at.isNullOrEmpty()) {
                            try{
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                parsedMillis = sdf.parse(canvasAssign.due_at).time
                            } catch (ignored: Exception){ }
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
                        Toast.makeText(requireContext(), "Canvas Sync Successful", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "ISU Todo list is completely clear now!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Canvas Sync Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
