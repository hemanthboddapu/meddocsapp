package com.example.meddocsapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for managing automatic backups.
 *
 * Features:
 * - Enable/disable automatic backups
 * - Choose backup frequency (daily, every 2 days, every 3 days, weekly)
 * - Create backup now
 * - View and manage existing backups
 * - Restore from backup
 * - Share backups
 *
 * User-friendly design - no passwords or encryption keys needed!
 */
class BackupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BackupActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var autoBackupSwitch: Switch
    private lateinit var frequencyRadioGroup: RadioGroup
    private lateinit var lastBackupText: TextView
    private lateinit var nextBackupText: TextView
    private lateinit var backupNowButton: MaterialButton
    private lateinit var backupsRecyclerView: RecyclerView
    private lateinit var noBackupsText: TextView
    private lateinit var backupLocationText: TextView
    private lateinit var progressOverlay: View
    private lateinit var progressText: TextView
    private lateinit var manualRestoreButton: MaterialButton

    private lateinit var backupManager: BackupManager
    private lateinit var repository: PatientRepository
    private lateinit var backupAdapter: BackupAdapter

    private val REQUEST_CODE_PICK_BACKUP_ZIP = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        toolbar = findViewById(R.id.toolbar)
        autoBackupSwitch = findViewById(R.id.auto_backup_switch)
        frequencyRadioGroup = findViewById(R.id.frequency_radio_group)
        lastBackupText = findViewById(R.id.last_backup_text)
        nextBackupText = findViewById(R.id.next_backup_text)
        backupNowButton = findViewById(R.id.backup_now_button)
        backupsRecyclerView = findViewById(R.id.backups_recycler_view)
        noBackupsText = findViewById(R.id.no_backups_text)
        backupLocationText = findViewById(R.id.backup_location_text)
        progressOverlay = findViewById(R.id.progress_overlay)
        progressText = findViewById(R.id.progress_text)
        manualRestoreButton = findViewById(R.id.manual_restore_button)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        backupManager = BackupManager(this)
        repository = (application as MedDocsApplication).repository

        setupUI()
        loadSettings()
        refreshBackupsList()

        AppLogger.d(TAG, "BackupActivity created")
    }

    private fun setupUI() {
        // Show backup location
        backupLocationText.text = "Backups are stored in:\n${backupManager.getBackupDirectory().absolutePath}"

        // Setup auto backup switch
        autoBackupSwitch.setOnCheckedChangeListener { _, isChecked ->
            backupManager.setAutoBackupEnabled(isChecked)
            updateFrequencyGroupState()
            updateBackupInfo()
        }

        // Setup frequency radio group
        frequencyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val frequency = when (checkedId) {
                R.id.radio_daily -> BackupManager.FREQUENCY_DAILY
                R.id.radio_every_2_days -> BackupManager.FREQUENCY_EVERY_2_DAYS
                R.id.radio_every_3_days -> BackupManager.FREQUENCY_EVERY_3_DAYS
                R.id.radio_weekly -> BackupManager.FREQUENCY_WEEKLY
                else -> BackupManager.FREQUENCY_DAILY
            }
            backupManager.setBackupFrequency(frequency)
            updateBackupInfo()
        }

        // Backup now button
        backupNowButton.setOnClickListener {
            createBackupNow()
        }

        // Manual restore button
        manualRestoreButton.setOnClickListener { pickBackupZipForRestore() }

        // Setup backup adapter
        backupAdapter = BackupAdapter(
            onRestoreClicked = { backup -> confirmRestore(backup) },
            onShareClicked = { backup -> shareBackup(backup) },
            onDeleteClicked = { backup -> confirmDeleteBackup(backup) }
        )
        backupsRecyclerView.layoutManager = LinearLayoutManager(this)
        backupsRecyclerView.adapter = backupAdapter
    }

    private fun loadSettings() {
        // Load auto backup setting
        autoBackupSwitch.isChecked = backupManager.isAutoBackupEnabled()
        updateFrequencyGroupState()

        // Load frequency setting
        val frequency = backupManager.getBackupFrequency()
        val radioId = when (frequency) {
            BackupManager.FREQUENCY_DAILY -> R.id.radio_daily
            BackupManager.FREQUENCY_EVERY_2_DAYS -> R.id.radio_every_2_days
            BackupManager.FREQUENCY_EVERY_3_DAYS -> R.id.radio_every_3_days
            BackupManager.FREQUENCY_WEEKLY -> R.id.radio_weekly
            else -> R.id.radio_daily
        }
        frequencyRadioGroup.check(radioId)

        updateBackupInfo()
    }

    private fun updateFrequencyGroupState() {
        val isEnabled = autoBackupSwitch.isChecked
        for (i in 0 until frequencyRadioGroup.childCount) {
            frequencyRadioGroup.getChildAt(i).isEnabled = isEnabled
        }
        frequencyRadioGroup.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun updateBackupInfo() {
        // Last backup
        lastBackupText.text = "Last backup: ${backupManager.getLastBackupDisplayString()}"

        // Next backup
        if (backupManager.isAutoBackupEnabled()) {
            val lastBackup = backupManager.getLastBackupTime()
            val frequency = backupManager.getBackupFrequency()
            if (lastBackup == 0L) {
                nextBackupText.text = "Next backup: On next app launch"
            } else {
                val nextBackupTime = lastBackup + frequency
                val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                nextBackupText.text = "Next backup: ${dateFormat.format(Date(nextBackupTime))}"
            }
            nextBackupText.visibility = View.VISIBLE
        } else {
            nextBackupText.visibility = View.GONE
        }
    }

    private fun createBackupNow() {
        showProgress("Creating backup...")

        lifecycleScope.launch {
            val result = backupManager.createBackup(repository)

            hideProgress()

            when (result) {
                is BackupManager.BackupResult.Success -> {
                    AppLogger.d(TAG, "Backup created: ${result.backupFile.absolutePath}")

                    refreshBackupsList()
                    updateBackupInfo()

                    AlertDialog.Builder(this@BackupActivity)
                        .setTitle("✓ Backup Created")
                        .setMessage(
                            "Your data has been backed up successfully!\n\n" +
                            "• Patients: ${result.patientCount}\n" +
                            "• Files: ${result.fileCount}\n\n" +
                            "Would you like to share this backup?"
                        )
                        .setPositiveButton("Share") { _, _ ->
                            shareBackupFile(result.backupFile)
                        }
                        .setNegativeButton("Done", null)
                        .show()
                }
                is BackupManager.BackupResult.Error -> {
                    AppLogger.e(TAG, "Backup failed: ${result.message}")
                    Toast.makeText(this@BackupActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmRestore(backup: BackupManager.BackupInfo) {
        val metadata = backup.metadata
        val infoText = if (metadata != null) {
            "This backup contains:\n" +
            "• ${metadata.patientCount} patients\n" +
            "• ${metadata.fileCount} files\n" +
            "• Created: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(metadata.createdAt))}\n\n"
        } else {
            ""
        }

        AlertDialog.Builder(this)
            .setTitle("Restore from Backup")
            .setMessage(
                "${infoText}Choose how to restore:\n\n" +
                "• MERGE: Add backup data to existing data\n" +
                "• REPLACE: Clear all data and restore from backup"
            )
            .setPositiveButton("Merge") { _, _ ->
                restoreBackup(backup.file, clearExisting = false)
            }
            .setNegativeButton("Replace All") { _, _ ->
                // Second confirmation for destructive action
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Confirm Replace")
                    .setMessage("This will DELETE all current data and restore from the backup. This cannot be undone.\n\nContinue?")
                    .setPositiveButton("Replace All") { _, _ ->
                        restoreBackup(backup.file, clearExisting = true)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun restoreBackup(backupFile: File, clearExisting: Boolean) {
        showProgress("Restoring data...")

        lifecycleScope.launch {
            val result = backupManager.restoreBackup(backupFile, repository, clearExisting)

            hideProgress()

            when (result) {
                is BackupManager.RestoreResult.Success -> {
                    AppLogger.d(TAG, "Restore completed: ${result.patientCount} patients, ${result.fileCount} files")

                    AlertDialog.Builder(this@BackupActivity)
                        .setTitle("✓ Restore Complete")
                        .setMessage(
                            "Data restored successfully!\n\n" +
                            "• Patients: ${result.patientCount}\n" +
                            "• Files: ${result.fileCount}"
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
                is BackupManager.RestoreResult.Error -> {
                    AppLogger.e(TAG, "Restore failed: ${result.message}")
                    Toast.makeText(this@BackupActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareBackup(backup: BackupManager.BackupInfo) {
        shareBackupFile(backup.file)
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MedDocs Backup - ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "MedDocs backup file. To restore, copy this file to the MedDocsBackups folder on your device.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share Backup"))
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to share backup", e)
            Toast.makeText(this, "Failed to share backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteBackup(backup: BackupManager.BackupInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete Backup")
            .setMessage("Delete this backup?\n\n${backup.name}\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                if (backupManager.deleteBackup(backup.file)) {
                    Toast.makeText(this, "Backup deleted", Toast.LENGTH_SHORT).show()
                    refreshBackupsList()
                } else {
                    Toast.makeText(this, "Failed to delete backup", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshBackupsList() {
        val backups = backupManager.listBackups()
        backupAdapter.submitList(backups)

        if (backups.isEmpty()) {
            noBackupsText.visibility = View.VISIBLE
            backupsRecyclerView.visibility = View.GONE
        } else {
            noBackupsText.visibility = View.GONE
            backupsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showProgress(message: String) {
        progressText.text = message
        progressOverlay.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progressOverlay.visibility = View.GONE
    }

    private fun pickBackupZipForRestore() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                // Hint picker to start in Documents
                putExtra("android.provider.extra.INITIAL_URI",
                    android.provider.DocumentsContract.buildDocumentUriUsingTree(
                        android.provider.DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:"),
                        "primary:Documents"
                    )
                )
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP_ZIP)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to launch file picker", e)
            Toast.makeText(this, "Unable to open file picker", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_BACKUP_ZIP && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                AlertDialog.Builder(this)
                    .setTitle("Restore from Selected Backup")
                    .setMessage("Do you want to restore from the selected backup? This will replace current data.")
                    .setPositiveButton("Restore") { _, _ ->
                        showProgress("Restoring data...")
                        lifecycleScope.launch {
                            val result = backupManager.restoreBackupFromSaf(uri, repository, clearExisting = true)
                            hideProgress()
                            when (result) {
                                is BackupManager.RestoreResult.Success -> {
                                    Toast.makeText(this@BackupActivity, "Restore complete", Toast.LENGTH_LONG).show()
                                }
                                is BackupManager.RestoreResult.Error -> {
                                    Toast.makeText(this@BackupActivity, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    /**
     * Adapter for displaying backup files in RecyclerView
     */
    private inner class BackupAdapter(
        private val onRestoreClicked: (BackupManager.BackupInfo) -> Unit,
        private val onShareClicked: (BackupManager.BackupInfo) -> Unit,
        private val onDeleteClicked: (BackupManager.BackupInfo) -> Unit
    ) : ListAdapter<BackupManager.BackupInfo, BackupAdapter.BackupViewHolder>(BackupDiffCallback()) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_backup, parent, false)
            return BackupViewHolder(view)
        }

        override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.backup_name_text)
            private val detailsText: TextView = itemView.findViewById(R.id.backup_details_text)
            private val shareButton: ImageButton = itemView.findViewById(R.id.share_backup_button)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_backup_button)

            fun bind(backup: BackupManager.BackupInfo) {
                // Load metadata lazily to avoid slowness
                var shownTitle = false
                backup.metadata?.let {
                    nameText.text = "${it.patientCount} patients, ${it.fileCount} files"
                    shownTitle = true
                }
                if (!shownTitle) {
                    // Try to load on demand in background
                    lifecycleScope.launch {
                        val meta = backupManager.loadBackupMetadata(backup.file)
                        if (meta != null && adapterPosition != RecyclerView.NO_POSITION) {
                            nameText.post {
                                nameText.text = "${meta.patientCount} patients, ${meta.fileCount} files"
                            }
                        } else {
                            nameText.post { nameText.text = backup.name }
                        }
                    }
                }

                val sizeStr = formatFileSize(backup.size)
                val dateStr = dateFormat.format(Date(backup.lastModified))
                detailsText.text = "$sizeStr • $dateStr"

                itemView.setOnClickListener { onRestoreClicked(backup) }
                shareButton.setOnClickListener { onShareClicked(backup) }
                deleteButton.setOnClickListener { onDeleteClicked(backup) }
            }

            private fun formatFileSize(size: Long): String {
                return when {
                    size < 1024 -> "$size B"
                    size < 1024 * 1024 -> "${size / 1024} KB"
                    else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                }
            }
        }
    }

    private class BackupDiffCallback : DiffUtil.ItemCallback<BackupManager.BackupInfo>() {
        override fun areItemsTheSame(oldItem: BackupManager.BackupInfo, newItem: BackupManager.BackupInfo): Boolean {
            return oldItem.file.absolutePath == newItem.file.absolutePath
        }

        override fun areContentsTheSame(oldItem: BackupManager.BackupInfo, newItem: BackupManager.BackupInfo): Boolean {
            return oldItem == newItem
        }
    }
}
