package com.example.studentproductivityapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.database.AppDatabase
import com.example.studentproductivityapp.database.AssignmentRepository
import com.example.studentproductivityapp.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.viewmodel.AssignmentViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ScheduleActivity : AppCompatActivity() {

    private lateinit var viewModel: AssignmentViewModel

    //open another activity when the user clicks on the add assignment button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_schedule)

        val database = AppDatabase.getDatabase(this)
        val repo = AssignmentRepository(database.assignmentDao())
        val factory = AssignmentViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(AssignmentViewModel::class.java)

        //set up the recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAssignments)
        val adapter = AssignmentAdapter {
             assignment, isChecked ->

            //update the room database when the user checks/unchecks an assignment
            val updatedAssignment = assignment.copy(isCompleted = isChecked)
            viewModel.update(updatedAssignment)

        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allAssignments.observe(this) { assignments ->
            //this will sort the list of assignments by incomplete ones first, then by date
            val sortedAssignments = assignments.sortedWith(compareBy({ !it.isCompleted }, { it.dueDateMillis }))

            adapter.submitList(sortedAssignments)
        }

        val fabAddAssignment = findViewById<FloatingActionButton>(R.id.fabAddAssignment)
        fabAddAssignment.setOnClickListener {

            val intent = Intent(this, AddAssignmentActivity::class.java)
            startActivity(intent)
        }

        //val fabDeleteAssignment = findViewById<FloatingActionButton>(R.id.fabDeleteAssignment)

    }
}