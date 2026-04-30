package com.example.studentproductivityapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.database.AppDatabase
import com.example.studentproductivityapp.database.AssignmentRepository
import com.example.studentproductivityapp.features.campus_map.CampusMapActivity
import com.example.studentproductivityapp.features.home.MainActivity
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.video_lectures.VideoLectureActivity
import com.example.studentproductivityapp.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.viewmodel.AssignmentViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationBarView

import androidx.recyclerview.widget.ItemTouchHelper

class ScheduleActivity  : AppCompatActivity() {

    private lateinit var viewModel: AssignmentViewModel

    //open another activity when the user clicks on the add assignment button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_schedule)

        val database = AppDatabase.getDatabase(this)
        val repo = AssignmentRepository(database.assignmentDao())
        val factory = AssignmentViewModelFactory(repo)
        
        viewModel = ViewModelProvider(this, factory)[AssignmentViewModel::class.java]

        //Hook up both RecyclerViews in activity_schedule.xml
        val rvPending = findViewById<RecyclerView>(R.id.rvPending)
        val rvCompleted = findViewById<RecyclerView>(R.id.rvCompleted)

        //function for when user checks a box
        val updateAssignment: (com.example.studentproductivityapp.database.Assignment, Boolean) -> Unit = { assignment, isChecked ->
            viewModel.update(assignment.copy(isCompleted = isChecked))
        }

        //set up two separate adapters for the two sections
        val pendingAdapter = AssignmentAdapter(updateAssignment)
        val completedAdapter = AssignmentAdapter(updateAssignment)


        rvPending.adapter = pendingAdapter
        rvPending.layoutManager = LinearLayoutManager(this)

        rvCompleted.adapter = completedAdapter
        rvCompleted.layoutManager = LinearLayoutManager(this)

        //Swipe-to-delete functionality
// Swipe-to-delete functionality with Red Background and Icon
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

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#EF5350")) // Material Red

                // You can add a trash can icon if you have one in your drawable folder!
                // val icon = androidx.core.content.ContextCompat.getDrawable(this@ScheduleActivity, android.R.drawable.ic_menu_delete)

                if (dX > 0) { // Swiping to the right
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                } else if (dX < 0) { // Swiping to the left
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                } else { // view is unSwiped
                    background.setBounds(0, 0, 0, 0)
                }
                background.draw(c)
            }
        }

        // Attach the swipe gesture to both lists
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(rvPending)
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(rvCompleted)

        // Observe the database and filter into respective sections
        viewModel.allAssignments.observe(this) { assignments ->
            // Filter pending and sort by date
            val pending = assignments.filter { !it.isCompleted }.sortedBy { it.dueDateMillis }
            // Filter completed and sort by date
            val completed = assignments.filter { it.isCompleted }.sortedBy { it.dueDateMillis }

            // Submit to their respective sections
            pendingAdapter.submitList(pending)
            completedAdapter.submitList(completed)
        }

        //______________________end of mods ^----------

        val fabAddAssignment = findViewById<FloatingActionButton>(R.id.fabAddAssignment)
        fabAddAssignment.setOnClickListener {

            val intent = Intent(this, AddAssignmentActivity::class.java)
            startActivity(intent)
        }

        val tvDeleteAssignment = findViewById<TextView>(R.id.tvDeleteAssignment)
        tvDeleteAssignment.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete All Assignments")
                .setMessage("Are you sure you want to delete all assignments?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.deleteAll()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Bottom navigation bar existence
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottomNavigationView)

        // Set the highlighted tab in nav bar
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
