package com.example.meddocsapp

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Patient entity representing a patient record in the database.
 *
 * @property id Auto-generated primary key
 * @property patientIdNumber Hospital/clinic patient ID (optional)
 * @property name Patient's full name (required)
 * @property bedNumber Bed/room number (required)
 * @property status Current status: "Active" or "Discharged"
 * @property gender Patient's gender (optional)
 * @property dob Date of birth as string (optional)
 * @property problem Medical problem/diagnosis description (optional)
 * @property admissionDate Admission date in milliseconds (optional)
 * @property dischargeDate Discharge date in milliseconds (optional)
 * @property createdAt Record creation timestamp in milliseconds
 */
@Parcelize
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientIdNumber: String = "",
    val name: String,
    val bedNumber: String,
    val status: String,
    val gender: String? = null,
    val dob: String? = null,
    val problem: String? = null,
    val admissionDate: Long? = null,
    val dischargeDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable