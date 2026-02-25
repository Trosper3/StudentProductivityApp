package com.example.studentproductivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.studentproductivityapp.database.Assignment
import com.example.studentproductivityapp.database.AssignmentRepository
import kotlinx.coroutines.launch

class AssignmentViewModel(private val repository: AssignmentRepository) : ViewModel() {

    //Convert Flow to LiveData
    val allAssignments = repository.allAssignments.asLiveData()

    //write to database in background, so app doesn't freeze during save
    fun insert(assignment: Assignment) = viewModelScope.launch {
        repository.insert(assignment)
    }

    fun update(assignment: Assignment) = viewModelScope.launch {
        repository.update(assignment)
    }

    fun delete(assignment: Assignment) = viewModelScope.launch {
        repository.delete(assignment)
    }
}

// Use Factory to create ViewModels with parameters
class AssignmentViewModelFactory(private val repository: AssignmentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssignmentViewModel::class.java)) {
            //
            @Suppress("UNCHECKED_CAST")
            return AssignmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



