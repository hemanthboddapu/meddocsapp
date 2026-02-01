package com.example.meddocsapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class AddEditPatientActivity : AppCompatActivity() {

    private lateinit var patientIdNumberEditText: EditText
    private lateinit var patientNameEditText: EditText
    private lateinit var bedNumberEditText: EditText
    private lateinit var patientStatusSpinner: Spinner
    private lateinit var genderEditText: EditText
    private lateinit var dobEditText: EditText
    private lateinit var problemEditText: EditText
    private lateinit var savePatientButton: Button
    private var existingPatient: Patient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_patient)

        patientIdNumberEditText = findViewById(R.id.patient_id_number_edit_text)
        patientNameEditText = findViewById(R.id.patient_name_edit_text)
        bedNumberEditText = findViewById(R.id.bed_number_edit_text)
        patientStatusSpinner = findViewById(R.id.patient_status_spinner)
        genderEditText = findViewById(R.id.gender_edit_text)
        dobEditText = findViewById(R.id.dob_edit_text)
        problemEditText = findViewById(R.id.problem_edit_text)
        savePatientButton = findViewById(R.id.save_patient_button)

        val statusOptions = arrayOf("Active", "Discharged")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        patientStatusSpinner.adapter = adapter

        existingPatient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PATIENT, Patient::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_PATIENT)
        }

        if (existingPatient != null) {
            title = "Edit Patient"
            patientIdNumberEditText.setText(existingPatient?.patientIdNumber)
            patientNameEditText.setText(existingPatient?.name)
            bedNumberEditText.setText(existingPatient?.bedNumber)
            val statusPosition = statusOptions.indexOf(existingPatient?.status)
            if (statusPosition >= 0) {
                patientStatusSpinner.setSelection(statusPosition)
            }
            genderEditText.setText(existingPatient?.gender)
            dobEditText.setText(existingPatient?.dob)
            problemEditText.setText(existingPatient?.problem)
        } else {
            title = "Add Patient"
        }

        savePatientButton.setOnClickListener {
            savePatient()
        }
    }

    private fun savePatient() {
        val patientIdNumber = patientIdNumberEditText.text.toString()
        val name = patientNameEditText.text.toString()
        val bedNumber = bedNumberEditText.text.toString()
        val status = patientStatusSpinner.selectedItem.toString()
        val gender = genderEditText.text.toString()
        val dob = dobEditText.text.toString()
        val problem = problemEditText.text.toString()

        if (name.isBlank() || bedNumber.isBlank()) {
            if (name.isBlank()) {
                patientNameEditText.error = "Name is required"
            }
            if (bedNumber.isBlank()) {
                bedNumberEditText.error = "Bed number is required"
            }
            return
        }

        val resultIntent = Intent()
        if (existingPatient != null) {
            val updatedPatient = existingPatient!!.copy(
                patientIdNumber = patientIdNumber,
                name = name,
                bedNumber = bedNumber,
                status = status,
                gender = gender,
                dob = dob,
                problem = problem
            )
            resultIntent.putExtra(EXTRA_PATIENT, updatedPatient)
        } else {
            val newPatient = Patient(
                patientIdNumber = patientIdNumber,
                name = name,
                bedNumber = bedNumber,
                status = status,
                gender = gender,
                dob = dob,
                problem = problem
            )
            resultIntent.putExtra(EXTRA_PATIENT, newPatient)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_PATIENT = "com.example.meddocsapp.EXTRA_PATIENT"
    }
}