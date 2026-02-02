package com.example.meddocsapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream

/**
 * Activity for receiving shared files from other apps and attaching them to patients.
 * Supports patient search and quick add of new patients.
 */
class ShareActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ShareActivity"
    }

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((application as MedDocsApplication).repository)
    }

    private lateinit var searchEditText: EditText
    private lateinit var patientListView: ListView
    private lateinit var addPatientFab: FloatingActionButton
    private lateinit var noResultsText: TextView

    private var sharedUris: ArrayList<Uri>? = null
    private var allPatients: List<Patient> = emptyList()
    private var filteredPatients: List<Patient> = emptyList()

    private val addPatientLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val patient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(AddEditPatientActivity.EXTRA_PATIENT, Patient::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(AddEditPatientActivity.EXTRA_PATIENT)
            }
            patient?.let { newPatient ->
                patientViewModel.insert(newPatient)
                Toast.makeText(this, "Patient added: ${newPatient.name}", Toast.LENGTH_SHORT).show()
                AppLogger.d(TAG, "New patient added from share: ${newPatient.name}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        AppLogger.d(TAG, "onCreate - receiving shared content")

        initViews()
        parseSharedContent()
        setupPatientList()
        setupSearch()
        setupAddPatientButton()
    }

    private fun initViews() {
        searchEditText = findViewById(R.id.search_edit_text)
        patientListView = findViewById(R.id.patient_list_view)
        addPatientFab = findViewById(R.id.add_patient_fab)
        noResultsText = findViewById(R.id.no_results_text)
    }

    private fun parseSharedContent() {
        sharedUris = when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { arrayListOf(it) }
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { arrayListOf(it) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }

        if (sharedUris == null) {
            AppLogger.e(TAG, "No shared URIs received")
            Toast.makeText(this, "No files to share", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        AppLogger.d(TAG, "Received ${sharedUris?.size} file(s) to share")
    }

    private fun setupPatientList() {
        patientViewModel.allPatients.observe(this) { patients ->
            allPatients = patients ?: emptyList()
            filteredPatients = allPatients
            updatePatientListView()
        }

        patientListView.setOnItemClickListener { _, _, position, _ ->
            val selectedPatient = filteredPatients[position]
            saveFilesToPatient(selectedPatient)
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                filterPatients(query)
            }
        })
    }

    private fun filterPatients(query: String) {
        filteredPatients = if (query.isEmpty()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.name.contains(query, ignoreCase = true) ||
                patient.bedNumber.contains(query, ignoreCase = true) ||
                patient.patientIdNumber.contains(query, ignoreCase = true) ||
                (patient.tags?.contains(query, ignoreCase = true) == true)
            }
        }
        updatePatientListView()
    }

    private fun updatePatientListView() {
        if (filteredPatients.isEmpty()) {
            noResultsText.visibility = View.VISIBLE
            patientListView.visibility = View.GONE
        } else {
            noResultsText.visibility = View.GONE
            patientListView.visibility = View.VISIBLE

            val displayList = filteredPatients.map { patient ->
                val status = if (patient.status == "Active") "🟢" else "🟠"
                "$status ${patient.name} (Bed: ${patient.bedNumber})"
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
            patientListView.adapter = adapter
        }
    }

    private fun setupAddPatientButton() {
        addPatientFab.setOnClickListener {
            val intent = Intent(this, AddEditPatientActivity::class.java)
            addPatientLauncher.launch(intent)
        }
    }

    private fun saveFilesToPatient(selectedPatient: Patient) {
        var successCount = 0

        sharedUris?.forEach { uri ->
            try {
                val mimeType = contentResolver.getType(uri)
                if (mimeType != null) {
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
                    cursor?.moveToFirst()
                    val fileName = nameIndex?.let { cursor.getString(it) } ?: "shared_file_${System.currentTimeMillis()}"
                    val size = sizeIndex?.let { cursor.getLong(it) } ?: 0
                    cursor?.close()

                    val file = File(filesDir, fileName)
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
                    successCount++
                    AppLogger.d(TAG, "File saved: $fileName to patient: ${selectedPatient.name}")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error saving file", e)
            }
        }

        val message = if (successCount > 1) {
            "$successCount files saved to ${selectedPatient.name}"
        } else {
            "File saved to ${selectedPatient.name}"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Navigate to patient detail
        val intent = Intent(this, PatientDetailActivity::class.java)
        intent.putExtra(PatientDetailActivity.EXTRA_PATIENT, selectedPatient)
        startActivity(intent)
        finish()
    }
}