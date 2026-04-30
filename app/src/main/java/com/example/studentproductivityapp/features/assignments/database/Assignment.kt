package com.example.studentproductivityapp.features.assignments.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val courseName: String,
    val dueDateMillis: Long, // Unix timestamp in milliseconds
    val isCompleted: Boolean = false
)
