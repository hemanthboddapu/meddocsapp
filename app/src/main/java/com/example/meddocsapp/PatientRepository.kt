package com.example.meddocsapp

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import com.google.gson.Gson
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Repository for patient data operations.
 *
 * This class serves as the single source of truth for patient data,
 * coordinating between the UI layer and the data access layer (DAOs).
 *
 * All database operations are performed on background threads via
 * Kotlin coroutines and Flow.
 */
open class PatientRepository(
    private val patientDao: PatientDao,
    private val patientFileDao: PatientFileDao,
    private val appContext: Context,
    private val recycleBinDao: RecycleBinDao? = null
) {
    companion object {
        private const val TAG = "PatientRepository"
        private const val DEFAULT_RETENTION_DAYS = 7
    }

    /** Flow of all patients, sorted with active patients first */
    open val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()

    /** Flow of total file count across all patients */
    open val fileCount: Flow<Int> = patientDao.getFileCount()

    /** Flow of active patients only */
    open val activePatients: Flow<List<Patient>> = patientDao.getActivePatients()

    /** Flow of discharged patients only */
    open val dischargedPatients: Flow<List<Patient>> = patientDao.getDischargedPatients()

    /** Flow of recycle bin items */
    open val recycleBinItems: Flow<List<RecycleBinItem>>? = recycleBinDao?.getAllItems()

    /** Flow of recycle bin item count */
    open val recycleBinCount: Flow<Int>? = recycleBinDao?.getItemCount()

    /**
     * Insert a new patient record
     * @param patient Patient to insert
     */
    open suspend fun insert(patient: Patient) {
        AppLogger.d(TAG, "Inserting patient: ${patient.name}")
        patientDao.insert(patient)
    }

    /**
     * Update an existing patient record
     * @param patient Patient with updated data
     */
    open suspend fun update(patient: Patient) {
        AppLogger.d(TAG, "Updating patient: ${patient.id} - ${patient.name}")
        patientDao.update(patient)
    }

    /**
     * Get all files for a specific patient
     * @param patientId ID of the patient
     * @return Flow of patient files
     */
    open fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
        return patientFileDao.getFilesForPatient(patientId)
    }

    /**
     * Get only image files for a specific patient
     * @param patientId ID of the patient
     * @return Flow of image files
     */
    open fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
        return patientFileDao.getImageFilesForPatient(patientId)
    }

    /**
     * Insert a new patient file
     * @param patientFile File to insert
     */
    open suspend fun insertFile(patientFile: PatientFile) {
        AppLogger.d(TAG, "Inserting file: ${patientFile.fileName} for patient: ${patientFile.patientId}")
        patientFileDao.insert(patientFile)
    }

    /**
     * Update an existing patient file
     * @param patientFile File with updated data
     */
    open suspend fun updateFile(patientFile: PatientFile) {
        AppLogger.d(TAG, "Updating file: ${patientFile.id} - ${patientFile.fileName}")
        patientFileDao.update(patientFile)
    }

    /**
     * Search patients by name, bed number, or patient ID number
     * @param query Search query string
     * @return Flow of matching patients
     */
    open fun searchPatients(query: String): Flow<List<Patient>> {
        AppLogger.d(TAG, "Searching patients with query: $query")
        return patientDao.searchPatients(query)
    }

    /**
     * Delete a patient and all associated files (cascade)
     * @param patient Patient to delete
     */
    open suspend fun delete(patient: Patient) {
        AppLogger.d(TAG, "Deleting patient: ${patient.id} - ${patient.name}")
        patientDao.delete(patient)
    }

    /**
     * Mark a patient as discharged with optional discharge date
     * @param patient Patient to discharge
     * @param dischargeDate Optional discharge date in milliseconds
     */
    open suspend fun dischargePatient(patient: Patient, dischargeDate: Long? = null) {
        val updatedPatient = patient.copy(
            status = "Discharged",
            dischargeDate = dischargeDate ?: System.currentTimeMillis()
        )
        AppLogger.d(TAG, "Discharging patient: ${patient.id} - ${patient.name}")
        patientDao.update(updatedPatient)
    }

    // ==================== Recycle Bin Operations ====================

    /**
     * Move a patient to recycle bin instead of permanent delete
     * @param patient Patient to move to recycle bin
     * @param retentionDays Number of days to keep in recycle bin (default 7)
     */
    open suspend fun movePatientToRecycleBin(patient: Patient, retentionDays: Int = DEFAULT_RETENTION_DAYS) {
        if (recycleBinDao == null) {
            // Fallback to permanent delete if recycle bin not available
            delete(patient)
            return
        }

        val itemData = Gson().toJson(patient)
        val expiresAt = System.currentTimeMillis() + (retentionDays * 24 * 60 * 60 * 1000L)

        val recycleBinItem = RecycleBinItem(
            itemType = "patient",
            originalId = patient.id,
            itemData = itemData,
            patientName = patient.name,
            expiresAt = expiresAt
        )

        recycleBinDao.insert(recycleBinItem)
        patientDao.delete(patient)
        AppLogger.d(TAG, "Moved patient to recycle bin: ${patient.name}")
    }

    /**
     * Move a file to recycle bin instead of permanent delete
     * @param patientFile File to move to recycle bin
     * @param retentionDays Number of days to keep in recycle bin (default 7)
     */
    open suspend fun moveFileToRecycleBin(patientFile: PatientFile, retentionDays: Int = DEFAULT_RETENTION_DAYS) {
        if (recycleBinDao == null) {
            // Fallback to permanent delete if recycle bin not available
            delete(patientFile)
            return
        }

        val itemData = Gson().toJson(patientFile)
        val expiresAt = System.currentTimeMillis() + (retentionDays * 24 * 60 * 60 * 1000L)

        val recycleBinItem = RecycleBinItem(
            itemType = "file",
            originalId = patientFile.id,
            itemData = itemData,
            fileName = patientFile.fileName,
            expiresAt = expiresAt
        )

        recycleBinDao.insert(recycleBinItem)
        patientFileDao.delete(patientFile)
        AppLogger.d(TAG, "Moved file to recycle bin: ${patientFile.fileName}")
    }

    /**
     * Restore a patient from recycle bin
     * @param recycleBinItem The recycle bin item to restore
     */
    open suspend fun restorePatient(recycleBinItem: RecycleBinItem) {
        if (recycleBinItem.itemType != "patient" || recycleBinDao == null) return

        val patient = Gson().fromJson(recycleBinItem.itemData, Patient::class.java)
        // Insert with a new ID to avoid conflicts
        val newPatient = patient.copy(id = 0)
        patientDao.insert(newPatient)
        recycleBinDao.delete(recycleBinItem)
        AppLogger.d(TAG, "Restored patient from recycle bin: ${patient.name}")
    }

    /**
     * Restore a file from recycle bin
     * @param recycleBinItem The recycle bin item to restore
     */
    open suspend fun restoreFile(recycleBinItem: RecycleBinItem) {
        if (recycleBinItem.itemType != "file" || recycleBinDao == null) return

        val patientFile = Gson().fromJson(recycleBinItem.itemData, PatientFile::class.java)
        // Insert with a new ID to avoid conflicts
        val newFile = patientFile.copy(id = 0)
        patientFileDao.insert(newFile)
        recycleBinDao.delete(recycleBinItem)
        AppLogger.d(TAG, "Restored file from recycle bin: ${patientFile.fileName}")
    }

    /**
     * Delete a physical file on disk if it exists.
     */
    private fun deletePhysicalFile(fileUriString: String?) {
        if (fileUriString.isNullOrBlank()) return
        try {
            val uri = Uri.parse(fileUriString)
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) {
                    val deleted = file.delete()
                    AppLogger.d(TAG, "Deleted physical file $path: $deleted")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete physical file: $fileUriString", e)
        }
    }

    /**
     * Permanently delete an item from recycle bin
     * @param recycleBinItem Item to permanently delete
     */
    open suspend fun permanentlyDelete(recycleBinItem: RecycleBinItem) {
        // If this is a file, also remove the underlying physical file from internal storage
        if (recycleBinItem.itemType == "file") {
            try {
                val patientFile = Gson().fromJson(recycleBinItem.itemData, PatientFile::class.java)
                deletePhysicalFile(patientFile.uri)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to parse PatientFile from recycle bin for physical delete", e)
            }
        }

        recycleBinDao?.delete(recycleBinItem)
        AppLogger.d(TAG, "Permanently deleted from recycle bin: ${recycleBinItem.id}")
    }

    /**
     * Clear all items from recycle bin
     */
    open suspend fun clearRecycleBin() {
        // Also clean up any underlying files for file-type entries
        recycleBinDao?.getAllOnce()?.forEach { item ->
            if (item.itemType == "file") {
                try {
                    val patientFile = Gson().fromJson(item.itemData, PatientFile::class.java)
                    deletePhysicalFile(patientFile.uri)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to parse PatientFile from recycle bin on clear", e)
                }
            }
        }

        recycleBinDao?.deleteAll()
        AppLogger.d(TAG, "Cleared recycle bin")
    }

    /**
     * Delete a single file (permanent, bypassing recycle bin)
     */
    open suspend fun delete(patientFile: PatientFile) {
        // Remove DB record
        patientFileDao.delete(patientFile)
        // Remove physical file
        deletePhysicalFile(patientFile.uri)
        AppLogger.d(TAG, "Deleted file permanently: ${patientFile.fileName}")
    }

    /**
     * Clean up expired items from recycle bin
     */
    open suspend fun cleanupExpiredRecycleBinItems() {
        recycleBinDao?.deleteExpiredItems(System.currentTimeMillis())
        AppLogger.d(TAG, "Cleaned up expired recycle bin items")
    }
}