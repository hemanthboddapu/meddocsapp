package com.example.meddocsapp

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Patient data class
 */
class PatientTest {

    @Test
    fun `patient creation with required fields only`() {
        val patient = Patient(
            name = "John Doe",
            bedNumber = "101",
            status = "Active"
        )

        assertEquals(0L, patient.id)
        assertEquals("John Doe", patient.name)
        assertEquals("101", patient.bedNumber)
        assertEquals("Active", patient.status)
        assertNull(patient.gender)
        assertNull(patient.dob)
        assertNull(patient.problem)
    }

    @Test
    fun `patient creation with all fields`() {
        val patient = Patient(
            id = 1,
            name = "Jane Doe",
            bedNumber = "102",
            status = "Discharged",
            gender = "Female",
            dob = "1990-01-15",
            problem = "Fever"
        )

        assertEquals(1L, patient.id)
        assertEquals("Jane Doe", patient.name)
        assertEquals("102", patient.bedNumber)
        assertEquals("Discharged", patient.status)
        assertEquals("Female", patient.gender)
        assertEquals("1990-01-15", patient.dob)
        assertEquals("Fever", patient.problem)
    }

    @Test
    fun `patient copy creates correct copy with modified fields`() {
        val original = Patient(
            id = 1,
            name = "John Doe",
            bedNumber = "101",
            status = "Active"
        )

        val modified = original.copy(status = "Discharged", problem = "Recovered")

        assertEquals(original.id, modified.id)
        assertEquals(original.name, modified.name)
        assertEquals(original.bedNumber, modified.bedNumber)
        assertEquals("Discharged", modified.status)
        assertEquals("Recovered", modified.problem)
    }

    @Test
    fun `patient equality works correctly`() {
        val patient1 = Patient(id = 1, name = "John", bedNumber = "101", status = "Active")
        val patient2 = Patient(id = 1, name = "John", bedNumber = "101", status = "Active")
        val patient3 = Patient(id = 2, name = "John", bedNumber = "101", status = "Active")

        assertEquals(patient1, patient2)
        assertNotEquals(patient1, patient3)
    }

    @Test
    fun `patient hashCode is consistent`() {
        val patient1 = Patient(id = 1, name = "John", bedNumber = "101", status = "Active")
        val patient2 = Patient(id = 1, name = "John", bedNumber = "101", status = "Active")

        assertEquals(patient1.hashCode(), patient2.hashCode())
    }
}

