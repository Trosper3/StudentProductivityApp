package com.example.studentproductivityapp.features.pdf_scanner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pdfs")
data class SavedPdf(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val filePath: String,
    val creationTimestamp: Long = System.currentTimeMillis()
)
