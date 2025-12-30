package com.example.meddocsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientFileDao {

    @Insert
    suspend fun insert(patientFile: PatientFile)

    @Query("SELECT * FROM patient_files WHERE patientId = :patientId")
    fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>

    @Delete
    suspend fun delete(patientFile: PatientFile)
}