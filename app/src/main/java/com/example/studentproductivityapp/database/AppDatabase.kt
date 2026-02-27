package com.example.studentproductivityapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Assignment::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assignmentDao(): AssignmentDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        //create a single instance of database
        fun getDatabase(context: Context): AppDatabase {
            //return INSTANCE ?:
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "student_productivity_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}