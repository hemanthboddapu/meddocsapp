package com.example.meddocsapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for patient-related UI operations.
 *
 * Exposes patient data as LiveData for UI observation and provides
 * methods for CRUD operations that execute in viewModelScope.
 */
class PatientViewModel(private val repository: PatientRepository) : ViewModel() {

    companion object {
        private const val TAG = "PatientViewModel"
    }

    /** All patients as LiveData, sorted with active patients first */
    val allPatients: LiveData<List<Patient>> = repository.allPatients.asLiveData()

    /** Total file count across all patients */
    val fileCount: LiveData<Int> = repository.fileCount.asLiveData()

    /**
     * Insert a new patient
     * @param patient Patient to insert
     */
    fun insert(patient: Patient) = viewModelScope.launch {
        AppLogger.d(TAG, "Insert patient: ${patient.name}")
        repository.insert(patient)
    }

    /**
     * Update an existing patient
     * @param patient Patient with updated data
     */
    fun update(patient: Patient) = viewModelScope.launch {
        AppLogger.d(TAG, "Update patient: ${patient.id}")
        repository.update(patient)
    }

    /**
     * Get files for a specific patient
     * @param patientId ID of the patient
     * @return LiveData of patient files
     */
    fun getFilesForPatient(patientId: Long): LiveData<List<PatientFile>> {
        return repository.getFilesForPatient(patientId).asLiveData()
    }

    /**
     * Get only image files for a specific patient
     * @param patientId ID of the patient
     * @return LiveData of image files
     */
    fun getImageFilesForPatient(patientId: Long): LiveData<List<PatientFile>> {
        return repository.getImageFilesForPatient(patientId).asLiveData()
    }

    /**
     * Insert a new patient file
     * @param patientFile File to insert
     */
    fun insertFile(patientFile: PatientFile) = viewModelScope.launch {
        AppLogger.d(TAG, "Insert file: ${patientFile.fileName}")
        repository.insertFile(patientFile)
    }

    /**
     * Update an existing patient file
     * @param patientFile File with updated data
     */
    fun updateFile(patientFile: PatientFile) = viewModelScope.launch {
        AppLogger.d(TAG, "Update file: ${patientFile.id}")
        repository.updateFile(patientFile)
    }

    /**
     * Search patients by name, bed number, or patient ID
     * @param query Search query string
     * @return LiveData of matching patients
     */
    fun searchPatients(query: String): LiveData<List<Patient>> {
        return repository.searchPatients(query).asLiveData()
    }

    /**
     * Delete a patient file
     * @param patientFile File to delete
     */
    fun delete(patientFile: PatientFile) = viewModelScope.launch {
        AppLogger.d(TAG, "Delete file: ${patientFile.id}")
        repository.delete(patientFile)
    }

    /**
     * Delete a patient and all associated files
     * @param patient Patient to delete
     */
    fun delete(patient: Patient) = viewModelScope.launch {
        AppLogger.d(TAG, "Delete patient: ${patient.id}")
        repository.delete(patient)
    }

    /**
     * Mark a patient as discharged
     * @param patient Patient to discharge
     * @param dischargeDate Optional discharge date in milliseconds (defaults to current time)
     */
    fun dischargePatient(patient: Patient, dischargeDate: Long? = null) = viewModelScope.launch {
        AppLogger.d(TAG, "Discharge patient: ${patient.id}")
        repository.dischargePatient(patient, dischargeDate)
    }

    // ==================== Recycle Bin Operations ====================

    /** Recycle bin items as LiveData */
    val recycleBinItems: LiveData<List<RecycleBinItem>>? = repository.recycleBinItems?.asLiveData()

    /** Recycle bin item count as LiveData */
    val recycleBinCount: LiveData<Int>? = repository.recycleBinCount?.asLiveData()

    /**
     * Move a patient to recycle bin
     * @param patient Patient to move to recycle bin
     */
    fun movePatientToRecycleBin(patient: Patient) = viewModelScope.launch {
        AppLogger.d(TAG, "Move patient to recycle bin: ${patient.id}")
        repository.movePatientToRecycleBin(patient)
    }

    /**
     * Move a file to recycle bin
     * @param patientFile File to move to recycle bin
     */
    fun moveFileToRecycleBin(patientFile: PatientFile) = viewModelScope.launch {
        AppLogger.d(TAG, "Move file to recycle bin: ${patientFile.id}")
        repository.moveFileToRecycleBin(patientFile)
    }

    /**
     * Restore a patient from recycle bin
     * @param recycleBinItem The recycle bin item to restore
     */
    fun restoreFromRecycleBin(recycleBinItem: RecycleBinItem) = viewModelScope.launch {
        AppLogger.d(TAG, "Restore from recycle bin: ${recycleBinItem.id}")
        if (recycleBinItem.itemType == "patient") {
            repository.restorePatient(recycleBinItem)
        } else {
            repository.restoreFile(recycleBinItem)
        }
    }

    /**
     * Permanently delete an item from recycle bin
     * @param recycleBinItem Item to permanently delete
     */
    fun permanentlyDelete(recycleBinItem: RecycleBinItem) = viewModelScope.launch {
        AppLogger.d(TAG, "Permanently delete: ${recycleBinItem.id}")
        repository.permanentlyDelete(recycleBinItem)
    }

    /**
     * Clear all items from recycle bin
     */
    fun clearRecycleBin() = viewModelScope.launch {
        AppLogger.d(TAG, "Clear recycle bin")
        repository.clearRecycleBin()
    }

    /**
     * Clean up expired items from recycle bin
     */
    fun cleanupExpiredRecycleBinItems() = viewModelScope.launch {
        AppLogger.d(TAG, "Cleanup expired recycle bin items")
        repository.cleanupExpiredRecycleBinItems()
    }
}

/**
 * Factory for creating PatientViewModel instances with required dependencies.
 */
class PatientViewModelFactory(private val repository: PatientRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}