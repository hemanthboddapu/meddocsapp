package com.example.meddocsapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for adding a new patient or editing an existing patient.
 *
 * Receives an optional EXTRA_PATIENT parcelable. If present, the activity
 * operates in edit mode; otherwise, it's in add mode.
 */
class AddEditPatientActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATIENT = "com.example.meddocsapp.EXTRA_PATIENT"
        private const val TAG = "AddEditPatientActivity"
    }

    private lateinit var patientIdNumberEditText: EditText
    private lateinit var patientNameEditText: EditText
    private lateinit var bedNumberEditText: EditText
    private lateinit var patientStatusSpinner: Spinner
    private lateinit var genderEditText: EditText
    private lateinit var dobEditText: EditText
    private lateinit var problemEditText: EditText
    private lateinit var admissionDateEditText: EditText
    private lateinit var savePatientButton: Button

    private var existingPatient: Patient? = null
    private var selectedAdmissionDate: Long? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_patient)

        AppLogger.d(TAG, "onCreate")

        initViews()
        setupStatusSpinner()
        setupDatePicker()
        loadExistingPatient()
        setupSaveButton()
    }

    private fun initViews() {
        patientIdNumberEditText = findViewById(R.id.patient_id_number_edit_text)
        patientNameEditText = findViewById(R.id.patient_name_edit_text)
        bedNumberEditText = findViewById(R.id.bed_number_edit_text)
        patientStatusSpinner = findViewById(R.id.patient_status_spinner)
        genderEditText = findViewById(R.id.gender_edit_text)
        dobEditText = findViewById(R.id.dob_edit_text)
        problemEditText = findViewById(R.id.problem_edit_text)
        admissionDateEditText = findViewById(R.id.admission_date_edit_text)
        savePatientButton = findViewById(R.id.save_patient_button)
    }

    private fun setupStatusSpinner() {
        val statusOptions = arrayOf("Active", "Discharged")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        patientStatusSpinner.adapter = adapter
    }

    private fun setupDatePicker() {
        admissionDateEditText.setOnClickListener {
            showDatePicker()
        }
        admissionDateEditText.isFocusable = false
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedAdmissionDate?.let { calendar.timeInMillis = it }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedAdmissionDate = calendar.timeInMillis
                admissionDateEditText.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadExistingPatient() {
        existingPatient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PATIENT, Patient::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PATIENT)
        }

        if (existingPatient != null) {
            title = "Edit Patient"
            populateFields(existingPatient!!)
            AppLogger.d(TAG, "Editing patient: ${existingPatient?.id}")
        } else {
            title = "Add Patient"
            // Set default admission date to today for new patients
            selectedAdmissionDate = System.currentTimeMillis()
            admissionDateEditText.setText(dateFormat.format(Date()))
            AppLogger.d(TAG, "Adding new patient")
        }
    }

    private fun populateFields(patient: Patient) {
        patientIdNumberEditText.setText(patient.patientIdNumber)
        patientNameEditText.setText(patient.name)
        bedNumberEditText.setText(patient.bedNumber)

        val statusOptions = arrayOf("Active", "Discharged")
        val statusPosition = statusOptions.indexOf(patient.status)
        if (statusPosition >= 0) {
            patientStatusSpinner.setSelection(statusPosition)
        }

        genderEditText.setText(patient.gender ?: "")
        dobEditText.setText(patient.dob ?: "")
        problemEditText.setText(patient.problem ?: "")

        patient.admissionDate?.let {
            selectedAdmissionDate = it
            admissionDateEditText.setText(dateFormat.format(Date(it)))
        }
    }

    private fun setupSaveButton() {
        savePatientButton.setOnClickListener {
            savePatient()
        }
    }

    private fun savePatient() {
        val patientIdNumber = patientIdNumberEditText.text.toString().trim()
        val name = patientNameEditText.text.toString().trim()
        val bedNumber = bedNumberEditText.text.toString().trim()
        val status = patientStatusSpinner.selectedItem.toString()
        val gender = genderEditText.text.toString().trim()
        val dob = dobEditText.text.toString().trim()
        val problem = problemEditText.text.toString().trim()

        // Validate required fields
        if (name.isBlank()) {
            patientNameEditText.error = "Name is required"
            patientNameEditText.requestFocus()
            return
        }
        if (bedNumber.isBlank()) {
            bedNumberEditText.error = "Bed number is required"
            bedNumberEditText.requestFocus()
            return
        }

        val resultIntent = Intent()

        try {
            if (existingPatient != null) {
                val updatedPatient = existingPatient!!.copy(
                    patientIdNumber = patientIdNumber,
                    name = name,
                    bedNumber = bedNumber,
                    status = status,
                    gender = gender.ifEmpty { null },
                    dob = dob.ifEmpty { null },
                    problem = problem.ifEmpty { null },
                    admissionDate = selectedAdmissionDate
                )
                resultIntent.putExtra(EXTRA_PATIENT, updatedPatient)
                AppLogger.d(TAG, "Patient updated: ${updatedPatient.id}")
            } else {
                val newPatient = Patient(
                    patientIdNumber = patientIdNumber,
                    name = name,
                    bedNumber = bedNumber,
                    status = status,
                    gender = gender.ifEmpty { null },
                    dob = dob.ifEmpty { null },
                    problem = problem.ifEmpty { null },
                    admissionDate = selectedAdmissionDate,
                    createdAt = System.currentTimeMillis()
                )
                resultIntent.putExtra(EXTRA_PATIENT, newPatient)
                AppLogger.d(TAG, "New patient created: $name")
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error saving patient", e)
        }
    }
}