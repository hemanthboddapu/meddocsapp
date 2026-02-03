package com.example.meddocsapp

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for MedDocs app.
 * Initializes database, repository, logging, and automatic backup system.
 */
class MedDocsApplication : Application() {

    companion object {
        private const val TAG = "MedDocsApplication"
    }

    /** Application-level coroutine scope for background operations */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Lazy-initialized Room database instance */
    val database by lazy { AppDatabase.getDatabase(this) }

    /** Lazy-initialized repository for data operations */
    val repository by lazy {
        PatientRepository(
            database.patientDao(),
            database.patientFileDao(),
            this,
            database.recycleBinDao()
        )
    }

    /** Lazy-initialized backup manager */
    val backupManager by lazy { BackupManager(this) }

    /** Flag to track if restore prompt was shown */
    var restorePromptShown = false

    /** Flag to track if backup was found on first launch */
    var hasBackupOnFirstLaunch = false

    override fun onCreate() {
        super.onCreate()
        // Initialize the logging system
        AppLogger.init(this)
        AppLogger.i(TAG, "Application started")

        if (backupManager.isFirstLaunch()) {
            // We no longer auto-restore; just mark as done if no backup found
            if (!backupManager.hasBackupAvailable()) {
                backupManager.markFirstLaunchDone()
            } else {
                // First launch but auto-restore removed; defer backups until user manually restores or starts using the app
                AppLogger.i(TAG, "First launch with backup available. Manual restore available in Backup & Restore.")
            }
        }

        // Only run auto-backup if first launch is already marked done
        if (!backupManager.isFirstLaunch()) {
            performAutoBackupIfNeeded()
            // Schedule periodic backups after first-launch is handled
            schedulePeriodicBackups()
        } else {
            AppLogger.i(TAG, "Deferring auto-backup and scheduling until first-launch restore flow completes")
        }

        // Cleanup expired recycle bin items
        cleanupExpiredRecycleBinItems()
    }

    /**
     * Perform automatic backup if enabled and due
     */
    private fun performAutoBackupIfNeeded() {
        if (backupManager.isBackupDue()) {
            AppLogger.i(TAG, "Automatic backup is due, starting backup...")
            applicationScope.launch {
                try {
                    val result = backupManager.createBackup(repository)
                    when (result) {
                        is BackupManager.BackupResult.Success -> {
                            AppLogger.i(TAG, "Automatic backup completed: ${result.patientCount} patients, ${result.fileCount} files")
                        }
                        is BackupManager.BackupResult.Error -> {
                            AppLogger.e(TAG, "Automatic backup failed: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Automatic backup error", e)
                }
            }
        }
    }

    /**
     * Cleanup expired recycle bin items
     */
    private fun cleanupExpiredRecycleBinItems() {
        applicationScope.launch {
            try {
                repository.cleanupExpiredRecycleBinItems()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to cleanup expired recycle bin items", e)
            }
        }
    }

    /**
     * Schedule periodic backups using WorkManager
     */
    private fun schedulePeriodicBackups() {
        val backupManager = backupManager
        if (!backupManager.isAutoBackupEnabled()) return

        // Choose repeat interval based on user frequency (min 15 minutes per WorkManager)
        val freqMs = backupManager.getBackupFrequency()
        val repeatIntervalHours = (freqMs / (60 * 60 * 1000)).coerceAtLeast(24) // default daily

        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true) // Prefer while charging
            .build()

        val request = androidx.work.PeriodicWorkRequestBuilder<BackupWorker>(
            java.time.Duration.ofHours(repeatIntervalHours)
        )
            .setConstraints(constraints)
            .addTag("meddocs_backup_periodic")
            .build()

        val wm = androidx.work.WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            "meddocs_backup_periodic",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        AppLogger.i(TAG, "Scheduled periodic backups every ${repeatIntervalHours}h")
    }
}