package com.example.meddocsapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PatientViewModel(private val repository: PatientRepository) : ViewModel() {

    val allPatients: LiveData<List<Patient>> = repository.allPatients.asLiveData()
    val fileCount: LiveData<Int> = repository.fileCount.asLiveData()

    fun insert(patient: Patient) = viewModelScope.launch {
        repository.insert(patient)
    }

    fun update(patient: Patient) = viewModelScope.launch {
        repository.update(patient)
    }

    fun getFilesForPatient(patientId: Long): LiveData<List<PatientFile>> {
        return repository.getFilesForPatient(patientId).asLiveData()
    }

    fun insertFile(patientFile: PatientFile) = viewModelScope.launch {
        repository.insertFile(patientFile)
    }

    fun searchPatients(query: String): LiveData<List<Patient>> {
        return repository.searchPatients(query).asLiveData()
    }

    fun delete(patientFile: PatientFile) = viewModelScope.launch {
        repository.delete(patientFile)
    }

    fun delete(patient: Patient) = viewModelScope.launch {
        repository.delete(patient)
    }
}

class PatientViewModelFactory(private val repository: PatientRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}