package com.example.meddocsapp

import kotlinx.coroutines.flow.Flow

open class PatientRepository(private val patientDao: PatientDao, private val patientFileDao: PatientFileDao) {

    open val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()
    open val fileCount: Flow<Int> = patientDao.getFileCount()

    open suspend fun insert(patient: Patient) {
        patientDao.insert(patient)
    }

    open suspend fun update(patient: Patient) {
        patientDao.update(patient)
    }

    open fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
        return patientFileDao.getFilesForPatient(patientId)
    }

    open fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
        return patientFileDao.getImageFilesForPatient(patientId)
    }

    open suspend fun insertFile(patientFile: PatientFile) {
        patientFileDao.insert(patientFile)
    }

    open suspend fun updateFile(patientFile: PatientFile) {
        patientFileDao.update(patientFile)
    }

    open fun searchPatients(query: String): Flow<List<Patient>> {
        return patientDao.searchPatients(query)
    }

    open suspend fun delete(patientFile: PatientFile) {
        patientFileDao.delete(patientFile)
    }

    open suspend fun delete(patient: Patient) {
        patientDao.delete(patient)
    }
}