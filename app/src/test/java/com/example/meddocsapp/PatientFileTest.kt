package com.example.meddocsapp

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the PatientFile data class
 */
class PatientFileTest {

    @Test
    fun `patientFile creation with all fields`() {
        val patientFile = PatientFile(
            id = 1,
            patientId = 100,
            uri = "file:///path/to/file.jpg",
            mimeType = "image/jpeg",
            fileName = "file.jpg",
            size = 1024,
            createdAt = 1699999999999
        )

        assertEquals(1L, patientFile.id)
        assertEquals(100L, patientFile.patientId)
        assertEquals("file:///path/to/file.jpg", patientFile.uri)
        assertEquals("image/jpeg", patientFile.mimeType)
        assertEquals("file.jpg", patientFile.fileName)
        assertEquals(1024L, patientFile.size)
        assertEquals(1699999999999, patientFile.createdAt)
    }

    @Test
    fun `patientFile with default id`() {
        val patientFile = PatientFile(
            patientId = 100,
            uri = "file:///path/to/file.pdf",
            mimeType = "application/pdf",
            fileName = "document.pdf",
            size = 2048,
            createdAt = System.currentTimeMillis()
        )

        assertEquals(0L, patientFile.id)
        assertEquals(100L, patientFile.patientId)
    }

    @Test
    fun `patientFile copy creates correct copy`() {
        val original = PatientFile(
            id = 1,
            patientId = 100,
            uri = "file:///path/to/file.jpg",
            mimeType = "image/jpeg",
            fileName = "file.jpg",
            size = 1024,
            createdAt = 1699999999999
        )

        val modified = original.copy(fileName = "renamed.jpg", size = 2048)

        assertEquals(original.id, modified.id)
        assertEquals(original.patientId, modified.patientId)
        assertEquals(original.uri, modified.uri)
        assertEquals("renamed.jpg", modified.fileName)
        assertEquals(2048L, modified.size)
    }

    @Test
    fun `patientFile equality works correctly`() {
        val file1 = PatientFile(
            id = 1,
            patientId = 100,
            uri = "file:///path/to/file.jpg",
            mimeType = "image/jpeg",
            fileName = "file.jpg",
            size = 1024,
            createdAt = 1699999999999
        )
        val file2 = PatientFile(
            id = 1,
            patientId = 100,
            uri = "file:///path/to/file.jpg",
            mimeType = "image/jpeg",
            fileName = "file.jpg",
            size = 1024,
            createdAt = 1699999999999
        )
        val file3 = PatientFile(
            id = 2,
            patientId = 100,
            uri = "file:///path/to/file.jpg",
            mimeType = "image/jpeg",
            fileName = "file.jpg",
            size = 1024,
            createdAt = 1699999999999
        )

        assertEquals(file1, file2)
        assertNotEquals(file1, file3)
    }

    @Test
    fun `mimeType correctly identifies image types`() {
        val imageFile = PatientFile(
            patientId = 1,
            uri = "file:///test.jpg",
            mimeType = "image/jpeg",
            fileName = "test.jpg",
            size = 100,
            createdAt = 0
        )

        val pdfFile = PatientFile(
            patientId = 1,
            uri = "file:///test.pdf",
            mimeType = "application/pdf",
            fileName = "test.pdf",
            size = 100,
            createdAt = 0
        )

        assertTrue(imageFile.mimeType.startsWith("image/"))
        assertFalse(pdfFile.mimeType.startsWith("image/"))
    }
}

