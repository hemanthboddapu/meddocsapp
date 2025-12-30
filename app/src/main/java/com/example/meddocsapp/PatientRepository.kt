package com.example.meddocsapp

import kotlinx.coroutines.flow.Flow

class PatientRepository(private val patientDao: PatientDao, private val patientFileDao: PatientFileDao) {

    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()
    val fileCount: Flow<Int> = patientDao.getFileCount()

    suspend fun insert(patient: Patient) {
        patientDao.insert(patient)
    }

    suspend fun update(patient: Patient) {
        patientDao.update(patient)
    }

    fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
        return patientFileDao.getFilesForPatient(patientId)
    }

    suspend fun insertFile(patientFile: PatientFile) {
        patientFileDao.insert(patientFile)
    }

    fun searchPatients(query: String): Flow<List<Patient>> {
        return patientDao.searchPatients(query)
    }

    suspend fun delete(patientFile: PatientFile) {
        patientFileDao.delete(patientFile)
    }

    suspend fun delete(patient: Patient) {
        patientDao.delete(patient)
    }
}