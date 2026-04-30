package com.example.studentproductivityapp.features.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.features.assignments.AddAssignmentActivity
import com.example.studentproductivityapp.features.assignments.AssignmentAdapter
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.AssignmentRepository
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ScheduleFragment : Fragment() {

    private lateinit var viewModel: AssignmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repo = AssignmentRepository(database.assignmentDao())
        val factory = AssignmentViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[AssignmentViewModel::class.java]

        val rvPending = view.findViewById<RecyclerView>(R.id.rvPending)
        val rvCompleted = view.findViewById<RecyclerView>(R.id.rvCompleted)

        val updateAssignment: (com.example.studentproductivityapp.features.assignments.database.Assignment, Boolean) -> Unit = { assignment, isChecked ->
            viewModel.update(assignment.copy(isCompleted = isChecked))
        }

        val pendingAdapter = AssignmentAdapter(updateAssignment)
        val completedAdapter = AssignmentAdapter(updateAssignment)

        rvPending.adapter = pendingAdapter
        rvPending.layoutManager = LinearLayoutManager(requireContext())

        rvCompleted.adapter = completedAdapter
        rvCompleted.layoutManager = LinearLayoutManager(requireContext())

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

        viewModel.allAssignments.observe(viewLifecycleOwner) { assignments ->
            val pending = assignments.filter { !it.isCompleted }.sortedBy { it.dueDateMillis }
            val completed = assignments.filter { it.isCompleted }.sortedBy { it.dueDateMillis }
            pendingAdapter.submitList(pending)
            completedAdapter.submitList(completed)
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddAssignment).setOnClickListener {
            val intent = Intent(requireContext(), AddAssignmentActivity::class.java)
            startActivity(intent)
        }
    }
}
