package com.example.meddocsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class ShareActivity : AppCompatActivity() {

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((application as MedDocsApplication).repository)
    }

    private lateinit var patientListView: ListView
    private var sharedUris: ArrayList<Uri>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        patientListView = findViewById(R.id.patient_list_view)

        sharedUris = when (intent.action) {
            Intent.ACTION_SEND -> intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { arrayListOf(it) }
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        if (sharedUris == null) {
            finish()
            return
        }

        patientViewModel.allPatients.observe(this) { patients ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, patients.map { it.name })
            patientListView.adapter = adapter
            patientListView.setOnItemClickListener { _, _, position, _ ->
                val selectedPatient = patients[position]
                sharedUris?.forEach { uri ->
                    val mimeType = contentResolver.getType(uri)
                    if (mimeType != null) {
                        val cursor = contentResolver.query(uri, null, null, null, null)
                        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
                        cursor?.moveToFirst()
                        val fileName = nameIndex?.let { cursor.getString(it) } ?: "shared_file"
                        val size = sizeIndex?.let { cursor.getLong(it) } ?: 0
                        cursor?.close()

                        val file = File(filesDir, fileName)
                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                FileOutputStream(file).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            val patientFile = PatientFile(
                                patientId = selectedPatient.id,
                                uri = Uri.fromFile(file).toString(),
                                mimeType = mimeType,
                                fileName = fileName,
                                size = size,
                                createdAt = System.currentTimeMillis()
                            )
                            patientViewModel.insertFile(patientFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                Toast.makeText(this, "File(s) saved to ${selectedPatient.name}", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, PatientDetailActivity::class.java)
                intent.putExtra(PatientDetailActivity.EXTRA_PATIENT, selectedPatient)
                startActivity(intent)
                finish()
            }
        }
    }
}