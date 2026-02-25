package com.example.studentproductivityapp.database

import kotlinx.coroutines.flow.Flow


class AssignmentRepository(private val assignmentDao: AssignmentDao) {

    //Fetch list of assignments as Flow from database
    val allAssignments: Flow<List<Assignment>> = assignmentDao.getAllAssignments()

    suspend fun insert(assignment: Assignment) {
        assignmentDao.insert(assignment)
    }

    suspend fun update(assignment: Assignment) {
        assignmentDao.update(assignment)
    }

    suspend fun delete(assignment: Assignment) {
        assignmentDao.delete(assignment)
    }
}