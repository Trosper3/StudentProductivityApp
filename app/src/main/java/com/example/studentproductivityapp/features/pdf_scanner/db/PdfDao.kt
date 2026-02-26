package com.example.studentproductivityapp.features.pdf_scanner.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {

    @Insert
    suspend fun insert(pdf: SavedPdf)

    @Query("SELECT * FROM saved_pdfs ORDER BY creationTimestamp DESC")
    fun getAllPdfs(): Flow<List<SavedPdf>>

    @Delete
    suspend fun delete(pdf: SavedPdf)
}
