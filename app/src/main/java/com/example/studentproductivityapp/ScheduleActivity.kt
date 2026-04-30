package com.example.studentproductivityapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.AssignmentRepository
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationBarView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.studentproductivityapp.features.assignments.AddAssignmentActivity
import com.example.studentproductivityapp.features.assignments.AssignmentAdapter

class ScheduleActivity : AppCompatActivity() {

    private lateinit var viewModel: AssignmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        val database = AppDatabase.getDatabase(this)
        val repo = AssignmentRepository(database.assignmentDao())
        val factory = AssignmentViewModelFactory(repo)
        
        viewModel = ViewModelProvider(this, factory)[AssignmentViewModel::class.java]

        val rvPending = findViewById<RecyclerView>(R.id.rvPending)
        val rvCompleted = findViewById<RecyclerView>(R.id.rvCompleted)

        val updateAssignment: (com.example.studentproductivityapp.features.assignments.database.Assignment, Boolean) -> Unit = { assignment, isChecked ->
            viewModel.update(assignment.copy(isCompleted = isChecked))
        }

        val pendingAdapter = AssignmentAdapter(updateAssignment)
        val completedAdapter = AssignmentAdapter(updateAssignment)

        rvPending.adapter = pendingAdapter
        rvPending.layoutManager = LinearLayoutManager(this)

        rvCompleted.adapter = completedAdapter
        rvCompleted.layoutManager = LinearLayoutManager(this)

        val swipeToDeleteCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val currentAdapter = viewHolder.bindingAdapter as? AssignmentAdapter
                val assignmentToDelete = currentAdapter?.currentList?.getOrNull(position)

                assignmentToDelete?.let {
                    viewModel.delete(it)
                }
            }
        }

        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(rvPending)
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(rvCompleted)

        viewModel.allAssignments.observe(this) { assignments ->
            val pending = assignments.filter { !it.isCompleted }.sortedBy { it.dueDateMillis }
            val completed = assignments.filter { it.isCompleted }.sortedBy { it.dueDateMillis }

            pendingAdapter.submitList(pending)
            completedAdapter.submitList(completed)
        }

        findViewById<FloatingActionButton>(R.id.fabAddAssignment).setOnClickListener {
            val intent = Intent(this, AddAssignmentActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tvDeleteAssignment).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete All Assignments")
                .setMessage("Are you sure you want to delete all assignments?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.deleteAll()
                }
                .setNegativeButton("No", null)
                .show()
        }

        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_assignment_trackr

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_assignment_trackr -> true

                R.id.nav_campus_map -> {
                    startActivity(Intent(this, CampusMapActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_pdf_scanner -> {
                    startActivity(Intent(this, PdfHubActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }

                R.id.nav_video_lectures -> {
                    startActivity(Intent(this, VideoLectureActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}
