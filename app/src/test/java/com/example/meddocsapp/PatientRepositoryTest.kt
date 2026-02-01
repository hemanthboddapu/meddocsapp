package com.example.meddocsapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PatientRepository
 */
class PatientRepositoryTest {

    private lateinit var fakePatientDao: FakePatientDao
    private lateinit var fakePatientFileDao: FakePatientFileDao
    private lateinit var repository: PatientRepository

    @Before
    fun setup() {
        fakePatientDao = FakePatientDao()
        fakePatientFileDao = FakePatientFileDao()
        repository = PatientRepository(fakePatientDao, fakePatientFileDao)
    }

    @Test
    fun `insert patient adds to dao`() = runBlocking {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")

        repository.insert(patient)

        val patients = repository.allPatients.first()
        assertEquals(1, patients.size)
        assertEquals("John Doe", patients[0].name)
    }

    @Test
    fun `update patient modifies existing patient`() = runBlocking {
        val patient = Patient(id = 1, name = "John Doe", bedNumber = "101", status = "Active")
        fakePatientDao.insertedPatients.add(patient)
        fakePatientDao.emitPatients()

        val updatedPatient = patient.copy(status = "Discharged")
        repository.update(updatedPatient)

        val patients = repository.allPatients.first()
        assertEquals("Discharged", patients[0].status)
    }

    @Test
    fun `delete patient removes from dao`() = runBlocking {
        val patient = Patient(id = 1, name = "John Doe", bedNumber = "101", status = "Active")
        fakePatientDao.insertedPatients.add(patient)
        fakePatientDao.emitPatients()

        repository.delete(patient)

        val patients = repository.allPatients.first()
        assertTrue(patients.isEmpty())
    }

    @Test
    fun `searchPatients returns matching patients`() = runBlocking {
        fakePatientDao.insertedPatients.addAll(listOf(
            Patient(id = 1, name = "John Doe", bedNumber = "101", status = "Active"),
            Patient(id = 2, name = "Jane Smith", bedNumber = "102", status = "Active"),
            Patient(id = 3, name = "Johnny Appleseed", bedNumber = "103", status = "Discharged")
        ))
        fakePatientDao.emitPatients()

        val results = repository.searchPatients("John").first()

        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "John Doe" })
        assertTrue(results.any { it.name == "Johnny Appleseed" })
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

        val files = repository.getFilesForPatient(1).first()
        assertEquals(1, files.size)
        assertEquals("test.jpg", files[0].fileName)
    }

    @Test
    fun `getFilesForPatient returns only files for specific patient`() = runBlocking {
        fakePatientFileDao.insertedFiles.addAll(listOf(
            PatientFile(id = 1, patientId = 1, uri = "file1", mimeType = "image/jpeg", fileName = "file1.jpg", size = 100, createdAt = 0),
            PatientFile(id = 2, patientId = 2, uri = "file2", mimeType = "image/png", fileName = "file2.png", size = 200, createdAt = 0),
            PatientFile(id = 3, patientId = 1, uri = "file3", mimeType = "application/pdf", fileName = "file3.pdf", size = 300, createdAt = 0)
        ))
        fakePatientFileDao.emitFiles()

        val files = repository.getFilesForPatient(1).first()

        assertEquals(2, files.size)
        assertTrue(files.all { it.patientId == 1L })
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

        val files = repository.getFilesForPatient(1).first()
        assertTrue(files.isEmpty())
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

        override fun getFileCount(): Flow<Int> = flowOf(0)

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

