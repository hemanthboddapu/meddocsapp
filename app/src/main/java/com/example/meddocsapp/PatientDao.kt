package com.example.meddocsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Patient entity.
 * Provides database operations for patient records.
 */
@Dao
interface PatientDao {

    /** Insert a new patient record */
    @Insert
    suspend fun insert(patient: Patient)

    /** Update an existing patient record */
    @Update
    suspend fun update(patient: Patient)

    /** Get all patients ordered by status (Active first) then by name */
    @Query("SELECT * FROM patients ORDER BY CASE WHEN status = 'Active' THEN 0 ELSE 1 END, name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    /** Get only active patients */
    @Query("SELECT * FROM patients WHERE status = 'Active' ORDER BY name ASC")
    fun getActivePatients(): Flow<List<Patient>>

    /** Get only discharged patients */
    @Query("SELECT * FROM patients WHERE status = 'Discharged' ORDER BY name ASC")
    fun getDischargedPatients(): Flow<List<Patient>>

    /**
     * Search patients by name, bed number, or patient ID number.
     * Results are ordered with active patients first.
     */
    @Query("""
        SELECT * FROM patients 
        WHERE name LIKE '%' || :query || '%' 
           OR bedNumber LIKE '%' || :query || '%'
           OR patientIdNumber LIKE '%' || :query || '%'
        ORDER BY CASE WHEN status = 'Active' THEN 0 ELSE 1 END, name ASC
    """)
    fun searchPatients(query: String): Flow<List<Patient>>

    /** Delete a patient record (cascades to patient_files) */
    @Delete
    suspend fun delete(patient: Patient)

    /** Get total count of files across all patients */
    @Query("SELECT COUNT(*) FROM patient_files")
    fun getFileCount(): Flow<Int>

    /** Get count of active patients */
    @Query("SELECT COUNT(*) FROM patients WHERE status = 'Active'")
    fun getActivePatientCount(): Flow<Int>

    /** Get count of discharged patients */
    @Query("SELECT COUNT(*) FROM patients WHERE status = 'Discharged'")
    fun getDischargedPatientCount(): Flow<Int>
}