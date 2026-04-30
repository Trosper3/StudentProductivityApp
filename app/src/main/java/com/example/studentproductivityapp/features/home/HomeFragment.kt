package com.example.studentproductivityapp.features.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.features.assignments.AssignmentAdapter
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.database.AppDatabase
import com.example.studentproductivityapp.features.assignments.database.AssignmentRepository
import com.example.studentproductivityapp.features.pdf_scanner.PdfHubActivity
import com.example.studentproductivityapp.features.pdf_scanner.SavedPdfsAdapter
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDatabase
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModel
import com.example.studentproductivityapp.features.assignments.viewmodel.AssignmentViewModelFactory
import kotlinx.coroutines.launch

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
}
