package com.example.meddocsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PatientFile entity.
 * Provides database operations for patient file records.
 */
@Dao
interface PatientFileDao {

    /** Insert a new patient file record */
    @Insert
    suspend fun insert(patientFile: PatientFile)

    /** Update an existing patient file record */
    @Update
    suspend fun update(patientFile: PatientFile)

    /** Get all files for a specific patient */
    @Query("SELECT * FROM patient_files WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>

    /** Get only image files for a specific patient (mimeType starts with 'image/') */
    @Query("SELECT * FROM patient_files WHERE patientId = :patientId AND mimeType LIKE 'image/%' ORDER BY createdAt DESC")
    fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>>

    /** Delete a patient file record */
    @Delete
    suspend fun delete(patientFile: PatientFile)
}