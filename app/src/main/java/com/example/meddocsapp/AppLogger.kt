package com.example.meddocsapp

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * AppLogger - Centralized logging utility with developer mode support.
 *
 * Features:
 * - Standard Android logcat logging
 * - Optional file-based logging for developer mode
 * - Automatic cleanup of logs older than 2 days
 * - Export functionality for selective time ranges
 *
 * Usage:
 *   AppLogger.d("Tag", "Debug message")
 *   AppLogger.e("Tag", "Error message", throwable)
 */
object AppLogger {

    private const val PREFS_NAME = "meddocs_developer_prefs"
    private const val KEY_DEVELOPER_MODE = "developer_mode_enabled"
    private const val LOG_FILE_PREFIX = "meddocs_log_"
    private const val LOG_RETENTION_DAYS = 2

    private val executor = Executors.newSingleThreadExecutor()
    private var context: Context? = null
    private var prefs: SharedPreferences? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Initialize the logger with application context.
     * Call this in Application.onCreate()
     */
    fun init(appContext: Context) {
        context = appContext.applicationContext
        prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cleanupOldLogs()
    }

    /**
     * Check if developer mode is enabled
     */
    fun isDeveloperModeEnabled(): Boolean {
        return prefs?.getBoolean(KEY_DEVELOPER_MODE, false) ?: false
    }

    /**
     * Enable or disable developer mode
     */
    fun setDeveloperModeEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_DEVELOPER_MODE, enabled)?.apply()
        if (enabled) {
            i("AppLogger", "Developer mode enabled - file logging started")
        } else {
            i("AppLogger", "Developer mode disabled - file logging stopped")
        }
    }

    /**
     * Log debug message
     */
    fun d(tag: String, message: String) {
        try {
            android.util.Log.d(tag, message)
        } catch (e: Exception) {
            // Running in test environment without Android framework
            println("D/$tag: $message")
        }
        writeToFile("D", tag, message)
    }

    /**
     * Log info message
     */
    fun i(tag: String, message: String) {
        try {
            android.util.Log.i(tag, message)
        } catch (e: Exception) {
            println("I/$tag: $message")
        }
        writeToFile("I", tag, message)
    }

    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                android.util.Log.w(tag, message, throwable)
            } else {
                android.util.Log.w(tag, message)
            }
        } catch (e: Exception) {
            println("W/$tag: $message")
            throwable?.printStackTrace()
        }
        if (throwable != null) {
            writeToFile("W", tag, "$message\n${throwable.stackTraceToString()}")
        } else {
            writeToFile("W", tag, message)
        }
    }

    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                android.util.Log.e(tag, message, throwable)
            } else {
                android.util.Log.e(tag, message)
            }
        } catch (e: Exception) {
            println("E/$tag: $message")
            throwable?.printStackTrace()
        }
        if (throwable != null) {
            writeToFile("E", tag, "$message\n${throwable.stackTraceToString()}")
        } else {
            writeToFile("E", tag, message)
        }
    }

    /**
     * Write log entry to file if developer mode is enabled
     */
    private fun writeToFile(level: String, tag: String, message: String) {
        if (!isDeveloperModeEnabled()) return
        if (context == null) return  // Not initialized, likely in test environment

        executor.execute {
            try {
                val logDir = getLogDirectory() ?: return@execute
                val today = fileDateFormat.format(Date())
                val logFile = File(logDir, "${LOG_FILE_PREFIX}$today.txt")

                val timestamp = dateFormat.format(Date())
                val logEntry = "$timestamp [$level/$tag]: $message\n"

                FileWriter(logFile, true).use { writer ->
                    writer.append(logEntry)
                }
            } catch (e: Exception) {
                // Silently ignore in case of test environment
                try {
                    android.util.Log.e("AppLogger", "Failed to write log to file", e)
                } catch (ignored: Exception) {
                    // Running in test environment
                }
            }
        }
    }

    /**
     * Get the log directory, creating it if necessary
     */
    private fun getLogDirectory(): File? {
        val ctx = context ?: return null
        val logDir = File(ctx.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return logDir
    }

    /**
     * Clean up logs older than retention period
     */
    private fun cleanupOldLogs() {
        if (context == null) return  // Not initialized

        executor.execute {
            try {
                val logDir = getLogDirectory() ?: return@execute
                val cutoffTime = System.currentTimeMillis() - (LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L)

                logDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(LOG_FILE_PREFIX) && file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // Silently ignore cleanup failures
            }
        }
    }

    /**
     * Get all available log files
     */
    fun getLogFiles(): List<File> {
        val logDir = getLogDirectory() ?: return emptyList()
        return logDir.listFiles()
            ?.filter { it.name.startsWith(LOG_FILE_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Read logs from a specific file
     */
    fun readLogFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }

    /**
     * Export logs from a time range to a single file
     * @param startTime Start of time range in milliseconds
     * @param endTime End of time range in milliseconds
     * @return File containing exported logs, or null if failed
     */
    fun exportLogs(startTime: Long, endTime: Long): File? {
        try {
            val ctx = context ?: return null
            val logDir = getLogDirectory() ?: return null

            val exportFile = File(ctx.getExternalFilesDir(null),
                "meddocs_logs_export_${System.currentTimeMillis()}.txt")

            val startDate = dateFormat.format(Date(startTime))
            val endDate = dateFormat.format(Date(endTime))

            FileWriter(exportFile).use { writer ->
                writer.append("=== MedDocs Log Export ===\n")
                writer.append("Time Range: $startDate to $endDate\n")
                writer.append("Export Time: ${dateFormat.format(Date())}\n")
                writer.append("================================\n\n")

                logDir.listFiles()
                    ?.filter { it.name.startsWith(LOG_FILE_PREFIX) }
                    ?.sortedBy { it.lastModified() }
                    ?.forEach { file ->
                        // Read and filter entries within time range
                        file.readLines().forEach { line ->
                            // Parse timestamp from log line and check if in range
                            try {
                                val timestampStr = line.substringBefore(" [")
                                if (timestampStr.isNotEmpty()) {
                                    val entryTime = dateFormat.parse(timestampStr)?.time ?: 0
                                    if (entryTime in startTime..endTime) {
                                        writer.append(line).append("\n")
                                    }
                                }
                            } catch (e: Exception) {
                                // If we can't parse, include the line anyway
                                writer.append(line).append("\n")
                            }
                        }
                    }
            }

            return exportFile
        } catch (e: Exception) {
            // Silently fail if we can't export
            return null
        }
    }

    /**
     * Clear all log files
     */
    fun clearAllLogs() {
        executor.execute {
            try {
                val logDir = getLogDirectory() ?: return@execute
                logDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(LOG_FILE_PREFIX)) {
                        file.delete()
                    }
                }
                i("AppLogger", "All logs cleared")
            } catch (e: Exception) {
                // Silently ignore clear failures
            }
        }
    }
}

