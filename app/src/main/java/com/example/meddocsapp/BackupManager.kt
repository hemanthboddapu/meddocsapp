package com.example.meddocsapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages automatic backup creation and restoration for MedDocs app.
 *
 * Backups are created as ZIP files with Base64-encoded metadata containing:
 * - Database export (patients and files metadata as JSON)
 * - All patient files (images, documents, audio, etc.)
 *
 * Features:
 * - Automatic scheduled backups (daily, weekly, or custom)
 * - No password required - simple and user-friendly
 * - Auto-restore on fresh install when backup is found
 * - Keeps last 5 backups to save space
 */
class BackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_DIR_NAME = "MedDocsBackups"
        private const val METADATA_FILE = "backup_metadata.json"
        private const val PATIENTS_FILE = "patients.json"
        private const val FILES_METADATA_FILE = "files_metadata.json"
        private const val FILES_DIR = "files"
        private const val BACKUP_VERSION = 2
        private const val MAX_BACKUPS_TO_KEEP = 5

        // Preference keys
        private const val PREFS_NAME = "meddocs_backup_prefs"
        private const val PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val PREF_BACKUP_FREQUENCY = "backup_frequency"
        private const val PREF_LAST_BACKUP_TIME = "last_backup_time"
        private const val PREF_FIRST_LAUNCH_DONE = "first_launch_done"
        private const val PREF_BACKUP_TREE_URI = "backup_tree_uri"

        // Backup frequencies in milliseconds
        const val FREQUENCY_DAILY = 24 * 60 * 60 * 1000L      // 24 hours
        const val FREQUENCY_WEEKLY = 7 * 24 * 60 * 60 * 1000L  // 7 days
        const val FREQUENCY_EVERY_2_DAYS = 2 * 24 * 60 * 60 * 1000L // 2 days
        const val FREQUENCY_EVERY_3_DAYS = 3 * 24 * 60 * 60 * 1000L // 3 days
    }

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Backup metadata containing version and creation info
     */
    data class BackupMetadata(
        val version: Int,
        val appVersion: String,
        val createdAt: Long,
        val patientCount: Int,
        val fileCount: Int,
        val deviceName: String = android.os.Build.MODEL
    )

    /**
     * Result of a backup operation
     */
    sealed class BackupResult {
        data class Success(val backupFile: File, val patientCount: Int, val fileCount: Int) : BackupResult()
        data class Error(val message: String, val exception: Exception? = null) : BackupResult()
    }

    /**
     * Result of a restore operation
     */
    sealed class RestoreResult {
        data class Success(val patientCount: Int, val fileCount: Int) : RestoreResult()
        data class Error(val message: String, val exception: Exception? = null) : RestoreResult()
    }

    // ==================== Settings ====================

    /**
     * Check if auto backup is enabled
     */
    fun isAutoBackupEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_BACKUP_ENABLED, true) // Enabled by default
    }

    /**
     * Enable or disable auto backup
     */
    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_BACKUP_ENABLED, enabled).apply()
        AppLogger.d(TAG, "Auto backup ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get backup frequency in milliseconds
     */
    fun getBackupFrequency(): Long {
        return prefs.getLong(PREF_BACKUP_FREQUENCY, FREQUENCY_DAILY)
    }

    /**
     * Set backup frequency
     * @param frequency Frequency in milliseconds (use FREQUENCY_* constants)
     */
    fun setBackupFrequency(frequency: Long) {
        prefs.edit().putLong(PREF_BACKUP_FREQUENCY, frequency).apply()
        AppLogger.d(TAG, "Backup frequency set to ${frequency / (60 * 60 * 1000)} hours")
    }

    /**
     * Get last backup timestamp
     */
    fun getLastBackupTime(): Long {
        return prefs.getLong(PREF_LAST_BACKUP_TIME, 0)
    }

    /**
     * Check if this is the first app launch (for auto-restore)
     */
    fun isFirstLaunch(): Boolean {
        return !prefs.getBoolean(PREF_FIRST_LAUNCH_DONE, false)
    }

    /**
     * Mark first launch as done
     */
    fun markFirstLaunchDone() {
        prefs.edit().putBoolean(PREF_FIRST_LAUNCH_DONE, true).apply()
    }

    /**
     * Check if backup is due based on frequency setting
     */
    fun isBackupDue(): Boolean {
        if (!isAutoBackupEnabled()) return false

        val lastBackup = getLastBackupTime()
        val frequency = getBackupFrequency()
        val now = System.currentTimeMillis()

        return (now - lastBackup) >= frequency
    }

    /**
     * Get human-readable frequency string
     */
    fun getFrequencyDisplayString(): String {
        return when (getBackupFrequency()) {
            FREQUENCY_DAILY -> "Daily"
            FREQUENCY_EVERY_2_DAYS -> "Every 2 days"
            FREQUENCY_EVERY_3_DAYS -> "Every 3 days"
            FREQUENCY_WEEKLY -> "Weekly"
            else -> "Custom"
        }
    }

    /**
     * Get human-readable last backup string
     */
    fun getLastBackupDisplayString(): String {
        val lastBackup = getLastBackupTime()
        if (lastBackup == 0L) return "Never"

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(lastBackup))
    }

    // ==================== Backup Directory ====================

    /**
     * Get candidate backup directories (public Documents, Downloads, and app external as fallback)
     */
    private fun getCandidateBackupDirectories(): List<File> {
        val dirs = mutableListOf<File>()
        try {
            // Public Documents
            val docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            dirs.add(File(docsDir, BACKUP_DIR_NAME))
        } catch (_: Exception) {}
        try {
            // Public Downloads
            val dlDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            dirs.add(File(dlDir, BACKUP_DIR_NAME))
        } catch (_: Exception) {}
        // App-specific external (fallback)
        context.getExternalFilesDir(null)?.let { ext ->
            dirs.add(File(ext, BACKUP_DIR_NAME))
        }
        return dirs
    }

    /**
     * Get the primary backup directory (prefer public Documents), create if needed
     */
    fun getBackupDirectory(): File {
        val candidates = getCandidateBackupDirectories()
        // Prefer public Documents
        val primary = candidates.firstOrNull()
        val dir = primary ?: File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Save SAF tree URI for backup directory */
    fun saveBackupTreeUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit().putString(PREF_BACKUP_TREE_URI, uri.toString()).apply()
        AppLogger.i(TAG, "Persisted backup tree URI")
    }

    /** Get SAF tree URI if set */
    fun getBackupTreeUri(): Uri? {
        val s = prefs.getString(PREF_BACKUP_TREE_URI, null) ?: return null
        return Uri.parse(s)
    }

    /** Use SAF to list backups if permission granted (via DocumentsContract) */
    private fun listBackupsViaSaf(): List<BackupInfo> {
        val tree = getBackupTreeUri() ?: return emptyList()
        return try {
            val cr = context.contentResolver
            val treeDocId = DocumentsContract.getTreeDocumentId(tree)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            val result = mutableListOf<BackupInfo>()
            cr.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val lastModIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else null
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val lastModified = if (lastModIdx >= 0) cursor.getLong(lastModIdx) else 0L
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null

                    val isZip = (mime == "application/zip") || (name?.endsWith(".zip", ignoreCase = true) == true)
                    val matchesPrefix = name?.startsWith("MedDocs_Backup_") == true
                    if (docId != null && name != null && isZip && matchesPrefix) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                        result.add(
                            BackupInfo(
                                file = File("/dev/null"),
                                name = name,
                                size = size,
                                lastModified = lastModified,
                                metadata = null,
                                uri = docUri
                            )
                        )
                    }
                }
            }
            result.sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            AppLogger.e(TAG, "SAF listing failed", e)
            emptyList()
        }
    }

    // ==================== Create Backup ====================

    /**
     * Create a backup of all patient data and files (Base64 metadata, ZIP of files)
     *
     * @param repository PatientRepository to access data
     * @return BackupResult indicating success or failure
     */
    suspend fun createBackup(repository: PatientRepository): BackupResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Starting backup creation")

            // Get all patients and files
            val patients = repository.allPatients.first()
            val allFiles = mutableListOf<PatientFile>()

            for (patient in patients) {
                val files = repository.getFilesForPatient(patient.id).first()
                allFiles.addAll(files)
            }

            AppLogger.d(TAG, "Backing up ${patients.size} patients and ${allFiles.size} files")

            // Create temp directory for staging backup files
            val tempDir = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // Create files subdirectory
                val filesSubDir = File(tempDir, FILES_DIR)
                filesSubDir.mkdirs()

                // Copy all patient files to temp directory and build file mapping
                val fileMapping = mutableMapOf<Long, String>() // originalId -> newFileName
                var copiedFileCount = 0

                for (patientFile in allFiles) {
                    try {
                        val sourceUri = Uri.parse(patientFile.uri)
                        val sourcePath = sourceUri.path
                        if (sourcePath != null) {
                            val sourceFile = File(sourcePath)
                            if (sourceFile.exists()) {
                                // Use a unique filename based on ID to avoid conflicts
                                val newFileName = "${patientFile.id}_${patientFile.fileName}"
                                val destFile = File(filesSubDir, newFileName)
                                sourceFile.copyTo(destFile, overwrite = true)
                                fileMapping[patientFile.id] = newFileName
                                copiedFileCount++
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Failed to copy file: ${patientFile.fileName}", e)
                        // Continue with other files
                    }
                }

                // Create backup metadata
                val metadata = BackupMetadata(
                    version = BACKUP_VERSION,
                    appVersion = getAppVersion(),
                    createdAt = System.currentTimeMillis(),
                    patientCount = patients.size,
                    fileCount = copiedFileCount
                )

                // If there are 0 files, do not create a backup (per requirement)
                if (metadata.fileCount == 0) {
                    AppLogger.i(TAG, "Skipping backup: 0 files to backup")
                    return@withContext BackupResult.Error("No files to backup")
                }

                // Encode metadata as Base64 for simple obfuscation
                val metadataJson = gson.toJson(metadata)
                val encodedMetadata = Base64.encodeToString(metadataJson.toByteArray(), Base64.DEFAULT)
                File(tempDir, METADATA_FILE).writeText(encodedMetadata)

                // Export patients data (Base64 encoded)
                val patientsJson = gson.toJson(patients)
                val encodedPatients = Base64.encodeToString(patientsJson.toByteArray(), Base64.DEFAULT)
                File(tempDir, PATIENTS_FILE).writeText(encodedPatients)

                // Export files metadata with mapping (Base64 encoded)
                val filesWithMapping = allFiles.map { file ->
                    mapOf(
                        "file" to file,
                        "backupFileName" to (fileMapping[file.id] ?: "")
                    )
                }
                val filesJson = gson.toJson(filesWithMapping)
                val encodedFiles = Base64.encodeToString(filesJson.toByteArray(), Base64.DEFAULT)
                File(tempDir, FILES_METADATA_FILE).writeText(encodedFiles)

                // Create ZIP file
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val backupFileName = "MedDocs_Backup_$timestamp.zip"
                val backupFile = File(getBackupDirectory(), backupFileName)

                // If writing to primary fails, try fallbacks
                try {
                    createZipFile(tempDir, backupFile)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Primary backup path failed, trying fallback", e)
                    val fallbackDir = getCandidateBackupDirectories().lastOrNull() ?: getBackupDirectory()
                    val fallbackFile = File(fallbackDir, backupFileName)
                    createZipFile(tempDir, fallbackFile)
                    return@withContext BackupResult.Success(fallbackFile, patients.size, copiedFileCount)
                }

                // Update last backup time
                prefs.edit().putLong(PREF_LAST_BACKUP_TIME, System.currentTimeMillis()).apply()

                // Cleanup old backups
                cleanupOldBackups()

                AppLogger.d(TAG, "Backup created successfully: ${backupFile.absolutePath}")
                return@withContext BackupResult.Success(backupFile, patients.size, copiedFileCount)

            } finally {
                // Clean up temp directory
                tempDir.deleteRecursively()
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Backup creation failed", e)
            return@withContext BackupResult.Error("Backup failed: ${e.message}", e)
        }
    }

    /**
     * Create a ZIP file from a directory
     */
    private fun createZipFile(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(sourceDir).path.replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    /** Extract a ZIP file (from File) to a directory */
    private fun extractZipFile(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(destDir, entry.name)
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    // ==================== Restore Backup ====================

    /**
     * Restore data from a backup file
     *
     * @param backupFile The backup file to restore from
     * @param repository PatientRepository to restore data
     * @param clearExisting If true, clears existing data before restore
     * @return RestoreResult indicating success or failure
     */
    suspend fun restoreBackup(
        backupFile: File,
        repository: PatientRepository,
        clearExisting: Boolean = false
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Starting backup restoration from: ${backupFile.name}")

            if (!backupFile.exists()) {
                return@withContext RestoreResult.Error("Backup file not found")
            }

            // Create temp directory for extraction
            val extractDir = File(context.cacheDir, "restore_extract_${System.currentTimeMillis()}")
            extractDir.mkdirs()

            try {
                // Extract ZIP
                extractZipFile(backupFile, extractDir)

                // Restore using a prepared extraction directory (shared by file and SAF flows)
                return@withContext restoreFromExtractedDir(extractDir, repository, clearExisting)

            } finally {
                // Clean up temp files
                extractDir.deleteRecursively()
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Backup restoration failed", e)
            return@withContext RestoreResult.Error("Restore failed: ${e.message}", e)
        }
    }

    /**
     * Restore using a prepared extraction directory (shared by file and SAF flows)
     */
    private suspend fun restoreFromExtractedDir(
        extractDir: File,
        repository: PatientRepository,
        clearExisting: Boolean
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            // Read and decode metadata
            val metadataFile = File(extractDir, METADATA_FILE)
            if (!metadataFile.exists()) {
                return@withContext RestoreResult.Error("Invalid backup: missing metadata")
            }
            val encodedMetadata = metadataFile.readText()
            val metadataJson = String(Base64.decode(encodedMetadata, Base64.DEFAULT))
            val metadata = gson.fromJson(metadataJson, BackupMetadata::class.java)
            AppLogger.d(TAG, "Backup version: ${metadata.version}, created: ${Date(metadata.createdAt)}")

            // Read and decode patients data
            val patientsFile = File(extractDir, PATIENTS_FILE)
            if (!patientsFile.exists()) {
                return@withContext RestoreResult.Error("Invalid backup: missing patients data")
            }
            val encodedPatients = patientsFile.readText()
            val patientsJson = String(Base64.decode(encodedPatients, Base64.DEFAULT))
            val patientsType = object : TypeToken<List<Patient>>() {}.type
            val patients: List<Patient> = gson.fromJson(patientsJson, patientsType)

            // Read and decode files metadata
            val filesMetadataFile = File(extractDir, FILES_METADATA_FILE)
            val filesWithMapping: List<Map<String, Any>> = if (filesMetadataFile.exists()) {
                val encodedFiles = filesMetadataFile.readText()
                val filesJson = String(Base64.decode(encodedFiles, Base64.DEFAULT))
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                gson.fromJson(filesJson, type)
            } else {
                emptyList()
            }

            // Clear existing data if requested
            if (clearExisting) {
                val existingPatients = repository.allPatients.first()
                for (patient in existingPatients) {
                    repository.delete(patient)
                }
                AppLogger.d(TAG, "Cleared ${existingPatients.size} existing patients")
            }

            // Restore patients and track ID mapping
            val patientIdMapping = mutableMapOf<Long, Long>()
            var restoredPatientCount = 0
            for (patient in patients) {
                val oldId = patient.id
                val newPatient = patient.copy(id = 0)
                repository.insert(newPatient)
                val allPatients = repository.allPatients.first()
                val insertedPatient = allPatients.find {
                    it.name == newPatient.name &&
                    it.bedNumber == newPatient.bedNumber &&
                    it.createdAt == newPatient.createdAt
                }
                if (insertedPatient != null) {
                    patientIdMapping[oldId] = insertedPatient.id
                    restoredPatientCount++
                }
            }

            // Restore files
            val backupFilesDir = File(extractDir, FILES_DIR)
            var restoredFileCount = 0
            for (entry in filesWithMapping) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val fileData = entry["file"] as? Map<String, Any> ?: continue
                    val backupFileName = entry["backupFileName"] as? String ?: continue
                    if (backupFileName.isEmpty()) continue
                    val oldPatientId = (fileData["patientId"] as? Double)?.toLong() ?: continue
                    val newPatientId = patientIdMapping[oldPatientId] ?: continue
                    val originalFileName = fileData["fileName"] as? String ?: "file"
                    val mimeType = fileData["mimeType"] as? String ?: "application/octet-stream"
                    val createdAt = (fileData["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis()

                    val sourceBackupFile = File(backupFilesDir, backupFileName)
                    if (sourceBackupFile.exists()) {
                        val newFileName = "${System.currentTimeMillis()}_${originalFileName}"
                        val destFile = File(context.filesDir, newFileName)
                        sourceBackupFile.copyTo(destFile, overwrite = true)
                        val patientFile = PatientFile(
                            id = 0,
                            patientId = newPatientId,
                            uri = Uri.fromFile(destFile).toString(),
                            mimeType = mimeType,
                            fileName = originalFileName,
                            size = destFile.length(),
                            createdAt = createdAt
                        )
                        repository.insertFile(patientFile)
                        restoredFileCount++
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to restore file", e)
                }
            }

            // Mark first launch done since we've restored data
            markFirstLaunchDone()

            AppLogger.d(TAG, "Restore completed: $restoredPatientCount patients, $restoredFileCount files")
            return@withContext RestoreResult.Success(restoredPatientCount, restoredFileCount)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Restore from extracted dir failed", e)
            return@withContext RestoreResult.Error("Restore failed: ${e.message}", e)
        }
    }

    /** Restore from SAF DocumentFile */
    suspend fun restoreBackupFromSaf(
        backupUri: Uri,
        repository: PatientRepository,
        clearExisting: Boolean = false
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Starting SAF backup restoration from: $backupUri")

            val resolver = context.contentResolver
            resolver.openInputStream(backupUri)?.use { inputStream ->
                val extractDir = File(context.cacheDir, "restore_extract_${System.currentTimeMillis()}")
                extractDir.mkdirs()
                try {
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val outFile = File(extractDir, entry.name)
                            if (entry.isDirectory) outFile.mkdirs() else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                    return@withContext restoreFromExtractedDir(extractDir, repository, clearExisting)
                } finally {
                    extractDir.deleteRecursively()
                }
            }
            return@withContext RestoreResult.Error("Unable to open backup URI")
        } catch (e: Exception) {
            AppLogger.e(TAG, "SAF restoration failed", e)
            return@withContext RestoreResult.Error("Restore failed: ${e.message}", e)
        }
    }

    fun loadBackupMetadataFromUri(uri: Uri): BackupMetadata? {
        return try {
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == METADATA_FILE) {
                            val baos = java.io.ByteArrayOutputStream()
                            zis.copyTo(baos)
                            val encodedMetadata = baos.toString()
                            val metadataJson = String(Base64.decode(encodedMetadata, Base64.DEFAULT))
                            return gson.fromJson(metadataJson, BackupMetadata::class.java)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read metadata from SAF URI", e)
            null
        }
    }

    // ==================== Auto Restore ====================

    /**
     * Check for available backups and return the most recent one
     * Used for auto-restore on first launch
     */
    fun findLatestBackup(): File? {
        return listBackups().firstOrNull()?.file
    }

    /**
     * Check if there's data to restore (for first launch)
     */
    fun hasBackupAvailable(): Boolean {
        return listBackups().isNotEmpty()
    }

    // ==================== Backup Management ====================

    /**
     * List backups across all directories, light-weight without parsing metadata
     */
    fun listBackups(): List<BackupInfo> {
        // Prefer SAF if available
        val saf = listBackupsViaSaf()
        if (saf.isNotEmpty()) return saf

        val all = mutableListOf<BackupInfo>()
        getCandidateBackupDirectories().forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.filter { it.isFile && it.extension == "zip" && it.name.startsWith("MedDocs_Backup_") }?.forEach { file ->
                    all.add(BackupInfo(
                        file = file,
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        metadata = null // defer heavy read until needed
                    ))
                }
            }
        }
        return all.sortedByDescending { it.lastModified }
    }

    /**
     * Read metadata for a specific backup on demand
     */
    fun loadBackupMetadata(backupFile: File): BackupMetadata? {
        return tryReadBackupMetadata(backupFile)
    }

    /**
     * Try to read metadata from a backup file
     */
    private fun tryReadBackupMetadata(backupFile: File): BackupMetadata? {
        return try {
            val tempDir = File(context.cacheDir, "meta_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            try {
                // Extract only metadata file
                ZipInputStream(FileInputStream(backupFile)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == METADATA_FILE) {
                            val metadataFile = File(tempDir, METADATA_FILE)
                            FileOutputStream(metadataFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            val encodedMetadata = metadataFile.readText()
                            val metadataJson = String(Base64.decode(encodedMetadata, Base64.DEFAULT))
                            return gson.fromJson(metadataJson, BackupMetadata::class.java)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                null
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read backup metadata", e)
            null
        }
    }

    /**
     * Delete a backup file
     */
    fun deleteBackup(backupFile: File): Boolean {
        return try {
            backupFile.delete()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete backup", e)
            false
        }
    }

    /**
     * Cleanup old backups in the primary directory only (keep last 5)
     */
    private fun cleanupOldBackups() {
        val backups = listBackups().filter { it.file.parentFile == getBackupDirectory() }
        if (backups.size > 1) {
            backups.sortedByDescending { it.lastModified }
                .drop(1)
                .forEach { backup ->
                    deleteBackup(backup.file)
                    AppLogger.d(TAG, "Deleted old backup: ${backup.name}")
                }
        }
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Information about a backup file
     */
    data class BackupInfo(
        val file: File,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val metadata: BackupMetadata? = null,
        val uri: Uri? = null
    )
}
