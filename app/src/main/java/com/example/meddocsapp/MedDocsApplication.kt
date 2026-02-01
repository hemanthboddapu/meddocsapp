package com.example.meddocsapp

import android.app.Application

/**
 * Application class for MedDocs app.
 * Initializes database, repository, and logging infrastructure.
 */
class MedDocsApplication : Application() {

    /** Lazy-initialized Room database instance */
    val database by lazy { AppDatabase.getDatabase(this) }

    /** Lazy-initialized repository for data operations */
    val repository by lazy {
        PatientRepository(
            database.patientDao(),
            database.patientFileDao(),
            database.recycleBinDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize the logging system
        AppLogger.init(this)
        AppLogger.i("MedDocsApplication", "Application started")
    }
}