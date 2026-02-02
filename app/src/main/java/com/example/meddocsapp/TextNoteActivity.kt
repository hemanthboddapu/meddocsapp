package com.example.meddocsapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for creating text notes for patients.
 * Supports adding consulting logs, observations, and other text-based notes.
 */
class TextNoteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TextNoteActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var discardButton: MaterialButton

    private var patientId: Long = 0
    private var patientName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_note)

        patientId = intent.getLongExtra("PATIENT_ID", 0)
        patientName = intent.getStringExtra("PATIENT_NAME") ?: "Unknown"

        initViews()
        setupToolbar()
        setupButtons()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        titleEditText = findViewById(R.id.title_edit_text)
        contentEditText = findViewById(R.id.content_edit_text)
        saveButton = findViewById(R.id.save_button)
        discardButton = findViewById(R.id.discard_button)

        // Set default title with timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        titleEditText.setText("Note - ${dateFormat.format(Date())}")
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Note"
        supportActionBar?.subtitle = patientName
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupButtons() {
        saveButton.setOnClickListener { saveNote() }
        discardButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun saveNote() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter note content", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\-_]"), "_").take(30)
            val fileName = "NOTE_${sanitizedTitle}_${timeStamp}.txt"
            val file = File(filesDir, fileName)

            FileWriter(file).use { writer ->
                writer.write("Title: $title\n")
                writer.write("Patient: $patientName\n")
                writer.write("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("---\n\n")
                writer.write(content)
            }

            AppLogger.d(TAG, "Note saved: ${file.absolutePath}")

            val resultIntent = Intent().apply {
                putExtra("NOTE_FILE_PATH", file.absolutePath)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save note", e)
            Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show()
        }
    }
}

