package com.example.meddocsapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker

/**
 * Periodic background worker to run automatic backups respecting user settings.
 * Schedules backups at night or when constraints are met.
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val app = applicationContext as MedDocsApplication
        val backupManager = app.backupManager
        val repository = app.repository

        return try {
            // Only run if auto backup is enabled and due
            if (!backupManager.isAutoBackupEnabled()) {
                return ListenableWorker.Result.success()
            }
            if (!backupManager.isBackupDue()) {
                return ListenableWorker.Result.success()
            }

            val result = backupManager.createBackup(repository)
            when (result) {
                is BackupManager.BackupResult.Success -> {
                    AppLogger.i("BackupWorker", "Background backup done: ${result.patientCount} patients, ${result.fileCount} files")
                    ListenableWorker.Result.success()
                }
                is BackupManager.BackupResult.Error -> {
                    AppLogger.e("BackupWorker", "Background backup failed: ${result.message}")
                    ListenableWorker.Result.retry()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("BackupWorker", "Exception in backup worker", e)
            ListenableWorker.Result.retry()
        }
    }
}

