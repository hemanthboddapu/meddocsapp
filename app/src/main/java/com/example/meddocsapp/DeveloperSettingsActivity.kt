package com.example.meddocsapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Developer settings activity for enabling developer mode,
 * viewing logs, and exporting logs for debugging.
 */
class DeveloperSettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeveloperSettings"
    }

    private lateinit var developerModeSwitch: SwitchMaterial
    private lateinit var viewLogsButton: Button
    private lateinit var exportLogsButton: Button
    private lateinit var clearLogsButton: Button
    private lateinit var logStatusText: TextView

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Developer Settings"

        initViews()
        setupDeveloperModeSwitch()
        setupButtons()
        updateLogStatus()
    }

    private fun initViews() {
        developerModeSwitch = findViewById(R.id.developer_mode_switch)
        viewLogsButton = findViewById(R.id.view_logs_button)
        exportLogsButton = findViewById(R.id.export_logs_button)
        clearLogsButton = findViewById(R.id.clear_logs_button)
        logStatusText = findViewById(R.id.log_status_text)
    }

    private fun setupDeveloperModeSwitch() {
        developerModeSwitch.isChecked = AppLogger.isDeveloperModeEnabled()
        developerModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppLogger.setDeveloperModeEnabled(isChecked)
            updateLogStatus()
            val message = if (isChecked) "Developer mode enabled" else "Developer mode disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        viewLogsButton.setOnClickListener { showLogViewer() }
        exportLogsButton.setOnClickListener { showExportDialog() }
        clearLogsButton.setOnClickListener { showClearConfirmation() }
    }

    private fun updateLogStatus() {
        val logFiles = AppLogger.getLogFiles()
        if (logFiles.isEmpty()) {
            logStatusText.text = "No log files available"
        } else {
            val totalSize = logFiles.sumOf { it.length() }
            val formattedSize = android.text.format.Formatter.formatShortFileSize(this, totalSize)
            logStatusText.text = "${logFiles.size} log file(s), $formattedSize total"
        }
    }

    private fun showLogViewer() {
        val logFiles = AppLogger.getLogFiles()
        if (logFiles.isEmpty()) {
            Toast.makeText(this, "No logs available", Toast.LENGTH_SHORT).show()
            return
        }

        val fileNames = logFiles.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Log File")
            .setItems(fileNames) { _, which ->
                val selectedFile = logFiles[which]
                val content = AppLogger.readLogFile(selectedFile)
                showLogContent(selectedFile.name, content)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogContent(title: String, content: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_log_viewer, null)
        val logContentText = dialogView.findViewById<TextView>(R.id.log_content_text)
        logContentText.text = content

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Share") { _, _ ->
                shareLogContent(title, content)
            }
            .show()
    }

    private fun shareLogContent(title: String, content: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MedDocs Log: $title")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Log"))
    }

    private fun showExportDialog() {
        val calendar = Calendar.getInstance()
        var startTime = calendar.timeInMillis - (24 * 60 * 60 * 1000) // Default: last 24 hours
        var endTime = calendar.timeInMillis

        AlertDialog.Builder(this)
            .setTitle("Export Logs")
            .setMessage("Export logs from the last 24 hours, or select a custom time range.")
            .setPositiveButton("Last 24 Hours") { _, _ ->
                exportLogs(startTime, endTime)
            }
            .setNeutralButton("Last 2 Days") { _, _ ->
                startTime = calendar.timeInMillis - (2 * 24 * 60 * 60 * 1000)
                exportLogs(startTime, endTime)
            }
            .setNegativeButton("Custom Range") { _, _ ->
                showCustomRangePicker()
            }
            .show()
    }

    private fun showCustomRangePicker() {
        val startCalendar = Calendar.getInstance()
        startCalendar.add(Calendar.DAY_OF_MONTH, -1)

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                startCalendar.set(year, month, dayOfMonth)
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        startCalendar.set(Calendar.MINUTE, minute)

                        // Now pick end date/time
                        pickEndDateTime(startCalendar.timeInMillis)
                    },
                    startCalendar.get(Calendar.HOUR_OF_DAY),
                    startCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Start Date")
            show()
        }
    }

    private fun pickEndDateTime(startTime: Long) {
        val endCalendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                endCalendar.set(year, month, dayOfMonth)
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        endCalendar.set(Calendar.MINUTE, minute)
                        exportLogs(startTime, endCalendar.timeInMillis)
                    },
                    endCalendar.get(Calendar.HOUR_OF_DAY),
                    endCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select End Date")
            show()
        }
    }

    private fun exportLogs(startTime: Long, endTime: Long) {
        val exportFile = AppLogger.exportLogs(startTime, endTime)
        if (exportFile != null && exportFile.exists()) {
            val contentUri = FileProvider.getUriForFile(
                this,
                "com.example.meddocsapp.provider",
                exportFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export Logs"))
            AppLogger.d(TAG, "Logs exported: ${exportFile.name}")
        } else {
            Toast.makeText(this, "No logs found in selected range", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Logs")
            .setMessage("Are you sure you want to delete all log files? This cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                AppLogger.clearAllLogs()
                updateLogStatus()
                Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

