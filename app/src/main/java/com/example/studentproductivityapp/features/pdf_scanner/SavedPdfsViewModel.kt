package com.example.studentproductivityapp.features.pdf_scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentproductivityapp.features.pdf_scanner.db.PdfDao
import com.example.studentproductivityapp.features.pdf_scanner.db.SavedPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SavedPdfsViewModel(private val pdfDao: PdfDao) : ViewModel() {
    val allPdfs: Flow<List<SavedPdf>> = pdfDao.getAllPdfs()

    fun delete(pdf: SavedPdf) {
        viewModelScope.launch(Dispatchers.IO) {
            pdfDao.delete(pdf)
        }
    }
}

class SavedPdfsViewModelFactory(private val pdfDao: PdfDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedPdfsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedPdfsViewModel(pdfDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
