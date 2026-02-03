package com.example.meddocsapp

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Unit tests for PatientViewModel
 */
class PatientViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var fakePatientDao: FakePatientDao
    private lateinit var fakePatientFileDao: FakePatientFileDao
    private lateinit var mockContext: Context
    private lateinit var repository: PatientRepository
    private lateinit var viewModel: PatientViewModel

    @Before
    fun setup() {
        fakePatientDao = FakePatientDao()
        fakePatientFileDao = FakePatientFileDao()
        mockContext = mock()
        repository = PatientRepository(fakePatientDao, fakePatientFileDao, mockContext)
        viewModel = PatientViewModel(repository)
    }

    @Test
    fun `insert patient adds to dao`() = runBlocking {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")

        repository.insert(patient)

        // Give coroutine time to execute
        Thread.sleep(100)

        assertEquals(1, fakePatientDao.insertedPatients.size)
        assertEquals("John Doe", fakePatientDao.insertedPatients[0].name)
    }

    @Test
    fun `update patient modifies existing patient`() = runBlocking {
        val patient = Patient(id = 1, name = "John Doe", bedNumber = "101", status = "Active")
        fakePatientDao.insertedPatients.add(patient)

        val updated = patient.copy(status = "Discharged")
        repository.update(updated)

        Thread.sleep(100)

        assertEquals("Discharged", fakePatientDao.insertedPatients[0].status)
    }

    @Test
    fun `delete patient removes from dao`() = runBlocking {
        val patient = Patient(id = 1, name = "John Doe", bedNumber = "101", status = "Active")
        fakePatientDao.insertedPatients.add(patient)
        fakePatientDao.emitPatients()

        repository.delete(patient)

        Thread.sleep(100)

        assertTrue(fakePatientDao.insertedPatients.isEmpty())
    }

    @Test
    fun `insertFile adds file to dao`() = runBlocking {
        val patientFile = PatientFile(
            patientId = 1,
            uri = "file:///test.jpg",
            mimeType = "image/jpeg",
            fileName = "test.jpg",
            size = 1024,
            createdAt = System.currentTimeMillis()
        )

        repository.insertFile(patientFile)

        Thread.sleep(100)

        assertEquals(1, fakePatientFileDao.insertedFiles.size)
        assertEquals("test.jpg", fakePatientFileDao.insertedFiles[0].fileName)
    }

    @Test
    fun `delete patientFile removes from dao`() = runBlocking {
        val patientFile = PatientFile(
            id = 1,
            patientId = 1,
            uri = "file:///test.jpg",
            mimeType = "image/jpeg",
            fileName = "test.jpg",
            size = 1024,
            createdAt = System.currentTimeMillis()
        )
        fakePatientFileDao.insertedFiles.add(patientFile)
        fakePatientFileDao.emitFiles()

        repository.delete(patientFile)

        Thread.sleep(100)

        assertTrue(fakePatientFileDao.insertedFiles.isEmpty())
    }

    // Fake implementations for testing
    class FakePatientDao : PatientDao {
        val insertedPatients = mutableListOf<Patient>()
        private val patientsFlow = MutableStateFlow<List<Patient>>(emptyList())

        fun emitPatients() {
            patientsFlow.value = insertedPatients.toList()
        }

        override suspend fun insert(patient: Patient) {
            val newPatient = if (patient.id == 0L) {
                patient.copy(id = (insertedPatients.maxOfOrNull { it.id } ?: 0) + 1)
            } else patient
            insertedPatients.add(newPatient)
            patientsFlow.value = insertedPatients.toList()
        }

        override suspend fun update(patient: Patient) {
            val index = insertedPatients.indexOfFirst { it.id == patient.id }
            if (index >= 0) {
                insertedPatients[index] = patient
                patientsFlow.value = insertedPatients.toList()
            }
        }

        override fun getAllPatients(): Flow<List<Patient>> = patientsFlow

        override fun getActivePatients(): Flow<List<Patient>> {
            return patientsFlow.map { patients ->
                patients.filter { it.status == "Active" }
            }
        }

        override fun getDischargedPatients(): Flow<List<Patient>> {
            return patientsFlow.map { patients ->
                patients.filter { it.status == "Discharged" }
            }
        }

        override fun searchPatients(query: String): Flow<List<Patient>> {
            return patientsFlow.map { patients ->
                patients.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.bedNumber.contains(query, ignoreCase = true) ||
                    it.patientIdNumber.contains(query, ignoreCase = true)
                }
            }
        }

        override suspend fun delete(patient: Patient) {
            insertedPatients.removeIf { it.id == patient.id }
            patientsFlow.value = insertedPatients.toList()
        }

        override fun getFileCount(): Flow<Int> = flowOf(insertedPatients.size)

        override fun getActivePatientCount(): Flow<Int> {
            return patientsFlow.map { patients ->
                patients.count { it.status == "Active" }
            }
        }

        override fun getDischargedPatientCount(): Flow<Int> {
            return patientsFlow.map { patients ->
                patients.count { it.status == "Discharged" }
            }
        }
    }

    class FakePatientFileDao : PatientFileDao {
        val insertedFiles = mutableListOf<PatientFile>()
        private val filesFlow = MutableStateFlow<List<PatientFile>>(emptyList())

        fun emitFiles() {
            filesFlow.value = insertedFiles.toList()
        }

        override suspend fun insert(patientFile: PatientFile) {
            val newFile = if (patientFile.id == 0L) {
                patientFile.copy(id = (insertedFiles.maxOfOrNull { it.id } ?: 0) + 1)
            } else patientFile
            insertedFiles.add(newFile)
            filesFlow.value = insertedFiles.toList()
        }

        override suspend fun update(patientFile: PatientFile) {
            val index = insertedFiles.indexOfFirst { it.id == patientFile.id }
            if (index >= 0) {
                insertedFiles[index] = patientFile
                filesFlow.value = insertedFiles.toList()
            }
        }

        override fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
            return filesFlow.map { files -> files.filter { it.patientId == patientId } }
        }

        override fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>> {
            return filesFlow.map { files ->
                files.filter { it.patientId == patientId && it.mimeType.startsWith("image/") }
            }
        }

        override suspend fun delete(patientFile: PatientFile) {
            insertedFiles.removeIf { it.id == patientFile.id }
            filesFlow.value = insertedFiles.toList()
        }
    }
}

