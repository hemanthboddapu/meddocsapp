package com.example.meddocsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientFileDao {

    @Insert
    suspend fun insert(patientFile: PatientFile)

    @Update
    suspend fun update(patientFile: PatientFile)

    @Query("SELECT * FROM patient_files WHERE patientId = :patientId")
    fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>

    @Query("SELECT * FROM patient_files WHERE patientId = :patientId AND mimeType LIKE 'image/%'")
    fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>>

    @Delete
    suspend fun delete(patientFile: PatientFile)
}