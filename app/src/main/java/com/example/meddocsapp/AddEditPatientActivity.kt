package com.example.meddocsapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for adding a new patient or editing an existing patient.
 * Compact UI with inline fields for better user experience.
 */
class AddEditPatientActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATIENT = "com.example.meddocsapp.EXTRA_PATIENT"
        private const val TAG = "AddEditPatientActivity"
    }

    private lateinit var patientIdNumberEditText: EditText
    private lateinit var patientNameEditText: EditText
    private lateinit var bedNumberEditText: EditText
    private lateinit var statusDropdown: AutoCompleteTextView
    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var ageEditText: EditText
    private lateinit var dobEditText: EditText
    private lateinit var problemEditText: EditText
    private lateinit var admissionDateEditText: EditText
    private lateinit var tagsEditText: EditText
    private lateinit var savePatientButton: Button
    private lateinit var bedNumberInputLayout: com.google.android.material.textfield.TextInputLayout

    private var existingPatient: Patient? = null
    private var selectedAdmissionDate: Long? = null
    private var selectedDob: Long? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_patient)

        AppLogger.d(TAG, "onCreate")

        setupToolbar()
        initViews()
        setupBedNumberToggle()
        setupDropdowns()
        setupDatePickers()
        setupAgeCalculation()
        loadExistingPatient()
        setupSaveButton()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initViews() {
        patientIdNumberEditText = findViewById(R.id.patient_id_number_edit_text)
        patientNameEditText = findViewById(R.id.patient_name_edit_text)
        bedNumberEditText = findViewById(R.id.bed_number_edit_text)
        statusDropdown = findViewById(R.id.status_dropdown)
        genderDropdown = findViewById(R.id.gender_dropdown)
        ageEditText = findViewById(R.id.age_edit_text)
        dobEditText = findViewById(R.id.dob_edit_text)
        problemEditText = findViewById(R.id.problem_edit_text)
        admissionDateEditText = findViewById(R.id.admission_date_edit_text)
        tagsEditText = findViewById(R.id.tags_edit_text)
        savePatientButton = findViewById(R.id.save_patient_button)
        bedNumberInputLayout = findViewById(R.id.bed_number_input_layout)
    }

    private fun setupBedNumberToggle() {
        // Toggle between numeric and text input types via end icon
        bedNumberInputLayout.setEndIconOnClickListener {
            val isNumeric = bedNumberEditText.inputType and android.text.InputType.TYPE_CLASS_NUMBER == android.text.InputType.TYPE_CLASS_NUMBER
            if (isNumeric) {
                bedNumberEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
                bedNumberEditText.setSelection(bedNumberEditText.text?.length ?: 0)
                bedNumberInputLayout.helperText = "Text mode"
            } else {
                bedNumberEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                bedNumberEditText.setSelection(bedNumberEditText.text?.length ?: 0)
                bedNumberInputLayout.helperText = "Numeric mode"
            }
        }
    }

    private fun setupDropdowns() {
        // Status dropdown
        val statusOptions = arrayOf("Active", "Discharged")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusOptions)
        statusDropdown.setAdapter(statusAdapter)
        statusDropdown.setText("Active", false)

        // Gender dropdown
        val genderOptions = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        genderDropdown.setAdapter(genderAdapter)
    }

    private fun setupDatePickers() {
        // Admission Date picker
        admissionDateEditText.setOnClickListener {
            showDatePicker(selectedAdmissionDate) { date ->
                selectedAdmissionDate = date
                admissionDateEditText.setText(dateFormat.format(Date(date)))
            }
        }

        // DOB picker
        dobEditText.setOnClickListener {
            showDatePicker(selectedDob, maxDate = System.currentTimeMillis()) { date ->
                selectedDob = date
                dobEditText.setText(dateFormat.format(Date(date)))
                // Auto-calculate age from DOB
                calculateAndSetAge(date)
            }
        }
    }

    private fun setupAgeCalculation() {
        // When age is manually entered, clear DOB
        ageEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && ageEditText.text.isNotEmpty()) {
                // User is editing age manually
            }
        }
    }

    private fun calculateAndSetAge(dobMillis: Long) {
        val dob = Calendar.getInstance().apply { timeInMillis = dobMillis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        if (age >= 0) {
            ageEditText.setText(age.toString())
        }
    }

    private fun showDatePicker(initialDate: Long?, maxDate: Long? = null, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        initialDate?.let { calendar.timeInMillis = it }

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        maxDate?.let { dialog.datePicker.maxDate = it }
        dialog.show()
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
        statusDropdown.setText(patient.status, false)
        genderDropdown.setText(patient.gender ?: "", false)
        problemEditText.setText(patient.problem ?: "")
        tagsEditText.setText(patient.tags ?: "")

        // Handle DOB - could be stored as age string or date
        patient.dob?.let { dob ->
            if (dob.all { it.isDigit() }) {
                // It's an age
                ageEditText.setText(dob)
            } else {
                // It's a date string
                dobEditText.setText(dob)
                try {
                    dateFormat.parse(dob)?.let { date ->
                        selectedDob = date.time
                        calculateAndSetAge(date.time)
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }

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
        val name = toTitleCase(patientNameEditText.text.toString().trim())
        val bedNumber = bedNumberEditText.text.toString().trim()
        val status = statusDropdown.text.toString().ifEmpty { "Active" }
        val gender = genderDropdown.text.toString().trim()
        val age = ageEditText.text.toString().trim()
        val dobText = dobEditText.text.toString().trim()
        val problem = problemEditText.text.toString().trim()
        val tags = tagsEditText.text.toString().trim()

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

        // Determine DOB value - prefer DOB date if set, otherwise use age
        val dobValue = when {
            dobText.isNotEmpty() -> dobText
            age.isNotEmpty() -> age
            else -> null
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
                    dob = dobValue,
                    problem = problem.ifEmpty { null },
                    admissionDate = selectedAdmissionDate,
                    tags = tags.ifEmpty { null }
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
                    dob = dobValue,
                    problem = problem.ifEmpty { null },
                    admissionDate = selectedAdmissionDate,
                    tags = tags.ifEmpty { null },
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

    /**
     * Converts a string to Title Case (first letter of each word capitalized)
     */
    private fun toTitleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }
}