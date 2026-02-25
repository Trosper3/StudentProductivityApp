package com.example.studentproductivityapp.features.pdf_scanner.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedPdf::class], version = 1)
abstract class PdfDatabase : RoomDatabase() {

    abstract fun pdfDao(): PdfDao

    companion object {
        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getDatabase(context: Context): PdfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "pdf_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
