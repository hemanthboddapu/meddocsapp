package com.example.meddocsapp

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * PatientFile entity representing a file attached to a patient.
 *
 * Files are stored in app-private storage and referenced by URI.
 * The foreign key ensures files are deleted when the parent patient is deleted.
 *
 * @property id Auto-generated primary key
 * @property patientId ID of the parent patient (foreign key)
 * @property uri File URI as string (typically file:// pointing to internal storage)
 * @property mimeType MIME type of the file (e.g., "image/jpeg", "application/pdf")
 * @property fileName Display name of the file
 * @property size File size in bytes
 * @property createdAt Timestamp when the file was added (milliseconds)
 */
@Parcelize
@Entity(
    tableName = "patient_files",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PatientFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val uri: String,
    val mimeType: String,
    val fileName: String,
    val size: Long,
    val createdAt: Long
) : Parcelable