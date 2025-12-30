package com.example.meddocsapp

import android.app.Application

class MedDocsApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { PatientRepository(database.patientDao(), database.patientFileDao()) }
}