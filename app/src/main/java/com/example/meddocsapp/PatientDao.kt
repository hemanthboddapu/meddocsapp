package com.example.meddocsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert
    suspend fun insert(patient: Patient)

    @Update
    suspend fun update(patient: Patient)

    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR bedNumber LIKE '%' || :query || '%'")
    fun searchPatients(query: String): Flow<List<Patient>>

    @Delete
    suspend fun delete(patient: Patient)

    @Query("SELECT COUNT(*) FROM patient_files")
    fun getFileCount(): Flow<Int>
}