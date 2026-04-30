package com.example.studentproductivityapp.features.assignments

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.Assignment
import com.example.studentproductivityapp.features.assignments.database.AssignmentRepository
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModelFactory
import com.example.studentproductivityapp.features.notifications.NotificationHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddAssignmentActivity : AppCompatActivity() {

    private lateinit var viewModel: AssignmentViewModel
    private var selectedDueDateMillis: Long = System.currentTimeMillis()

    //This function is called when the activity is created. (onCreate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_assignment)

        val database = AppDatabase.getDatabase(this)
        //repo, or repository. Same thing.
        val repo = AssignmentRepository(database.assignmentDao())
        //The Chocolate Factory! :) JK, this is a "factory" that "outputs" assignments into our ViewModel. Right?
        val factory = AssignmentViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(AssignmentViewModel::class.java)

        val titleInput = findViewById<TextInputEditText>(R.id.etAssignmentName)
        val courseInput = findViewById<TextInputEditText>(R.id.etCourseName)
        val dueDateButton = findViewById<Button>(R.id.btnPickDueDate)
        val saveButton = findViewById<Button>(R.id.btnSaveAssignment)

        // Pre-populate if intent has extras (e.g., from OCR)
        intent.getStringExtra("EXTRA_TITLE")?.let { titleInput.setText(it) }
        val initialDate = intent.getLongExtra("EXTRA_DUE_DATE", -1L)
        if (initialDate != -1L) {
            selectedDueDateMillis = initialDate
            val dateString = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            dueDateButton.text = "Due: ${dateString.format(Date(selectedDueDateMillis))}"
        }

        dueDateButton.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a Due Date")
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDueDateMillis = selection
                val dateString = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                dueDateButton.text = "Due: ${dateString.format(Date(selectedDueDateMillis))}"


            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        saveButton.setOnClickListener {
            val title = titleInput.text.toString()
            val courseName = courseInput.text.toString()

            if (title.isEmpty() || courseName.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newAssignment = Assignment(
                title = title,
                courseName = courseName,
                dueDateMillis = selectedDueDateMillis,
                isCompleted = false
            )

            viewModel.insert(newAssignment)
            
            // Schedule notification for the new assignment
            NotificationHelper.scheduleNotification(this, newAssignment)

            Toast.makeText(this, "Assignment saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}