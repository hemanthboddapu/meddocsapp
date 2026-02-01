package com.example.meddocsapp

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientIdNumber: String = "",  // Hospital/clinic patient ID
    val name: String,
    val bedNumber: String,
    val status: String,
    val gender: String? = null,
    val dob: String? = null,
    val problem: String? = null
) : Parcelable