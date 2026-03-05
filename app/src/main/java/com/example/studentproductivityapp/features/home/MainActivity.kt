package com.example.studentproductivityapp.features.home

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.AssignmentAdapter
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch



class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: AssignmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false)
        setContentView(R.layout.activity_main)

        //Set current date
        val textDateView = findViewById<TextView>(R.id.textDateView)
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val currentDate = sdf.format(Date())
        textDateView.text = currentDate

        //settings button
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        //------------Assignments \"Quick View\" -----------------------------------
        //set up Database connection for \"quick view\"
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

        // Observe the LiveData from the ViewModel. Pick Top 3 assignments
        //to show on quick view
        viewModel.allAssignments.observe(this) { assignments ->
            //Ignore assignments when completed
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
                // Handle item click
                Toast.makeText(this@MainActivity, "PDF clicked", Toast.LENGTH_SHORT).show()
                TODO("Not yet implemented")

                // You can navigate to a new activity or perform any other action here
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

        // Set the highlighted tab in nav bar
        navigationBarView.selectedItemId = R.id.nav_home

        navigationBarView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true //Home is highlighted
                R.id.nav_assignment_trackr -> {
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_campus_map -> {
                    if (isInternetAvailable()) {
                        startActivity(Intent(this, CampusMapActivity::class.java))
                        overridePendingTransition(0,0)
                        finish()
                    }
                    else {
                        Toast.makeText(this, "Internet connection required for map.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }


                R.id.nav_pdf_scanner -> {
                    startActivity(Intent(this, PdfHubActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }

                R.id.nav_video_lectures -> {

                    if (isInternetAvailable()) {
                        startActivity(Intent(this, VideoLectureActivity::class.java))
                        overridePendingTransition(0,0)
                        finish()

                        //Toast.makeText(this, \"Video Lectures coming soon!\", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Internet connection required for video lectures.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                else -> false
            }
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
}
