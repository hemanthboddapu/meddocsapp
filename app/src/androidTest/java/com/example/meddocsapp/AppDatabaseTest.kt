package com.example.meddocsapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for Room database operations
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var patientDao: PatientDao
    private lateinit var patientFileDao: PatientFileDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        patientDao = db.patientDao()
        patientFileDao = db.patientFileDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // PatientDao Tests

    @Test
    fun insertAndGetPatient() = runTest {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        patientDao.insert(patient)

        val patients = patientDao.getAllPatients().first()

        assertEquals(1, patients.size)
        assertEquals("John Doe", patients[0].name)
        assertEquals("101", patients[0].bedNumber)
        assertEquals("Active", patients[0].status)
    }

    @Test
    fun insertMultiplePatientsAndGetAll() = runTest {
        val patient1 = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        val patient2 = Patient(name = "Jane Smith", bedNumber = "102", status = "Discharged")
        val patient3 = Patient(name = "Bob Wilson", bedNumber = "103", status = "Active")

        patientDao.insert(patient1)
        patientDao.insert(patient2)
        patientDao.insert(patient3)

        val patients = patientDao.getAllPatients().first()

        assertEquals(3, patients.size)
        // Should be sorted by name ASC
        assertEquals("Bob Wilson", patients[0].name)
        assertEquals("Jane Smith", patients[1].name)
        assertEquals("John Doe", patients[2].name)
    }

    @Test
    fun updatePatient() = runTest {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        patientDao.insert(patient)

        val insertedPatient = patientDao.getAllPatients().first()[0]
        val updatedPatient = insertedPatient.copy(status = "Discharged", problem = "Recovered")
        patientDao.update(updatedPatient)

        val patients = patientDao.getAllPatients().first()

        assertEquals(1, patients.size)
        assertEquals("Discharged", patients[0].status)
        assertEquals("Recovered", patients[0].problem)
    }

    @Test
    fun deletePatient() = runTest {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        patientDao.insert(patient)

        val insertedPatient = patientDao.getAllPatients().first()[0]
        patientDao.delete(insertedPatient)

        val patients = patientDao.getAllPatients().first()

        assertTrue(patients.isEmpty())
    }

    @Test
    fun searchPatientsByName() = runTest {
        patientDao.insert(Patient(name = "John Doe", bedNumber = "101", status = "Active"))
        patientDao.insert(Patient(name = "Jane Smith", bedNumber = "102", status = "Active"))
        patientDao.insert(Patient(name = "Johnny Appleseed", bedNumber = "103", status = "Discharged"))

        val results = patientDao.searchPatients("John").first()

        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "John Doe" })
        assertTrue(results.any { it.name == "Johnny Appleseed" })
    }

    @Test
    fun searchPatientsByBedNumber() = runTest {
        patientDao.insert(Patient(name = "John Doe", bedNumber = "101", status = "Active"))
        patientDao.insert(Patient(name = "Jane Smith", bedNumber = "102", status = "Active"))
        patientDao.insert(Patient(name = "Bob Wilson", bedNumber = "201", status = "Discharged"))

        val results = patientDao.searchPatients("10").first()

        assertEquals(2, results.size)
        assertTrue(results.any { it.bedNumber == "101" })
        assertTrue(results.any { it.bedNumber == "102" })
    }

    // PatientFileDao Tests

    @Test
    fun insertAndGetPatientFile() = runTest {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        patientDao.insert(patient)
        val insertedPatient = patientDao.getAllPatients().first()[0]

        val patientFile = PatientFile(
            patientId = insertedPatient.id,
            uri = "file:///test.jpg",
            mimeType = "image/jpeg",
            fileName = "test.jpg",
            size = 1024,
            createdAt = System.currentTimeMillis()
        )
        patientFileDao.insert(patientFile)

        val files = patientFileDao.getFilesForPatient(insertedPatient.id).first()

        assertEquals(1, files.size)
        assertEquals("test.jpg", files[0].fileName)
        assertEquals("image/jpeg", files[0].mimeType)
    }

    @Test
    fun getFilesForSpecificPatient() = runTest {
        val patient1 = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        val patient2 = Patient(name = "Jane Smith", bedNumber = "102", status = "Active")
        patientDao.insert(patient1)
        patientDao.insert(patient2)
        val patients = patientDao.getAllPatients().first()
        val johnId = patients.find { it.name == "John Doe" }!!.id
        val janeId = patients.find { it.name == "Jane Smith" }!!.id

        patientFileDao.insert(PatientFile(patientId = johnId, uri = "file1", mimeType = "image/jpeg", fileName = "john_file1.jpg", size = 100, createdAt = 0))
        patientFileDao.insert(PatientFile(patientId = johnId, uri = "file2", mimeType = "image/png", fileName = "john_file2.png", size = 200, createdAt = 0))
        patientFileDao.insert(PatientFile(patientId = janeId, uri = "file3", mimeType = "application/pdf", fileName = "jane_file.pdf", size = 300, createdAt = 0))

        val johnFiles = patientFileDao.getFilesForPatient(johnId).first()
        val janeFiles = patientFileDao.getFilesForPatient(janeId).first()

        assertEquals(2, johnFiles.size)
        assertEquals(1, janeFiles.size)
        assertTrue(johnFiles.all { it.patientId == johnId })
        assertTrue(janeFiles.all { it.patientId == janeId })
    }

    @Test
    fun deletePatientFile() = runTest {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        patientDao.insert(patient)
        val insertedPatient = patientDao.getAllPatients().first()[0]

        val patientFile = PatientFile(
            patientId = insertedPatient.id,
            uri = "file:///test.jpg",
            mimeType = "image/jpeg",
            fileName = "test.jpg",
            size = 1024,
            createdAt = System.currentTimeMillis()
        )
        patientFileDao.insert(patientFile)
        val insertedFile = patientFileDao.getFilesForPatient(insertedPatient.id).first()[0]

        patientFileDao.delete(insertedFile)

        val files = patientFileDao.getFilesForPatient(insertedPatient.id).first()
        assertTrue(files.isEmpty())
    }

    @Test
    fun cascadeDeleteFilesWhenPatientDeleted() = runTest {
        val patient = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        patientDao.insert(patient)
        val insertedPatient = patientDao.getAllPatients().first()[0]

        patientFileDao.insert(PatientFile(patientId = insertedPatient.id, uri = "file1", mimeType = "image/jpeg", fileName = "file1.jpg", size = 100, createdAt = 0))
        patientFileDao.insert(PatientFile(patientId = insertedPatient.id, uri = "file2", mimeType = "image/png", fileName = "file2.png", size = 200, createdAt = 0))

        val filesBefore = patientFileDao.getFilesForPatient(insertedPatient.id).first()
        assertEquals(2, filesBefore.size)

        patientDao.delete(insertedPatient)

        val filesAfter = patientFileDao.getFilesForPatient(insertedPatient.id).first()
        assertTrue(filesAfter.isEmpty())
    }

    @Test
    fun getFileCount() = runTest {
        val patient1 = Patient(name = "John Doe", bedNumber = "101", status = "Active")
        val patient2 = Patient(name = "Jane Smith", bedNumber = "102", status = "Active")
        patientDao.insert(patient1)
        patientDao.insert(patient2)
        val patients = patientDao.getAllPatients().first()

        patientFileDao.insert(PatientFile(patientId = patients[0].id, uri = "file1", mimeType = "image/jpeg", fileName = "file1.jpg", size = 100, createdAt = 0))
        patientFileDao.insert(PatientFile(patientId = patients[0].id, uri = "file2", mimeType = "image/png", fileName = "file2.png", size = 200, createdAt = 0))
        patientFileDao.insert(PatientFile(patientId = patients[1].id, uri = "file3", mimeType = "application/pdf", fileName = "file3.pdf", size = 300, createdAt = 0))

        val count = patientDao.getFileCount().first()

        assertEquals(3, count)
    }

    @Test
    fun patientWithAllOptionalFields() = runTest {
        val patient = Patient(
            name = "John Doe",
            bedNumber = "101",
            status = "Active",
            gender = "Male",
            dob = "1985-05-15",
            problem = "Pneumonia"
        )
        patientDao.insert(patient)

        val patients = patientDao.getAllPatients().first()

        assertEquals(1, patients.size)
        assertEquals("Male", patients[0].gender)
        assertEquals("1985-05-15", patients[0].dob)
        assertEquals("Pneumonia", patients[0].problem)
    }
}

