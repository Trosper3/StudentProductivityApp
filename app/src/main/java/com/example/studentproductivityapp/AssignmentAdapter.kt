package com.example.studentproductivityapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.database.Assignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter class for the RecyclerView that displays the list of assignments. Connects the data to the views
//An adapter class binds data from database of assignments to the views in the RecyclerView
class AssignmentAdapter(
    private val onAssignmentChecked: (Assignment, Boolean) -> Unit) :
    ListAdapter<Assignment, AssignmentAdapter.AssignmentViewHolder>(AssignmentsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_assignment, parent, false)
        return AssignmentViewHolder(view)
}
    override fun onBindViewHolder(holder: AssignmentViewHolder, position: Int) {
        val currentAssignment = getItem(position)
        holder.bind(currentAssignment, onAssignmentChecked)
    }

    // ViewHolder class for each item (assignment) in the list
    class AssignmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleItemView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val courseNameItemView: TextView = itemView.findViewById(R.id.textCourseName)
        private val dueDateItemView: TextView = itemView.findViewById(R.id.textViewDueDate)
        private val checkBoxCompleted: CheckBox = itemView.findViewById(R.id.checkBoxCompleted)

        // Binds the data of an assignment to the views in the item layout
        //displays important information (from the Assignment object) such as due date, title, and course name
        fun bind(assignment: Assignment, onAssignmentChecked: (Assignment, Boolean) -> Unit) {
            titleItemView.text = assignment.title
            courseNameItemView.text = assignment.courseName

            val simpleDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateString = simpleDateFormat.format(Date(assignment.dueDateMillis))
            dueDateItemView.text = "Due: $dateString"

            checkBoxCompleted.setOnCheckedChangeListener(null)
            checkBoxCompleted.isChecked = assignment.isCompleted

            checkBoxCompleted.setOnCheckedChangeListener { _, isChecked ->
                onAssignmentChecked(assignment, isChecked)
            }
        }
    }

    // Helper class to compare items in the list, so that no two items are the same
    class AssignmentsComparator : DiffUtil.ItemCallback<Assignment>() {
        // Checks if the items are the same
        //if so, the item is redrawn
        override fun areItemsTheSame(oldItem: Assignment, newItem: Assignment): Boolean {
            return oldItem.id == newItem.id
        }

        // Checks if the contents of the items are the same
        //if so, the item is not redrawn
        override fun areContentsTheSame(oldItem: Assignment, newItem: Assignment): Boolean {
            return oldItem == newItem
        }
    }
}


