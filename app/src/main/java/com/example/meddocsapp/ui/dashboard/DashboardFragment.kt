package com.example.meddocsapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.meddocsapp.AppLogger
import com.example.meddocsapp.DeveloperSettingsActivity
import com.example.meddocsapp.MedDocsApplication
import com.example.meddocsapp.Patient
import com.example.meddocsapp.PatientDetailActivity
import com.example.meddocsapp.PatientViewModel
import com.example.meddocsapp.PatientViewModelFactory
import com.example.meddocsapp.R
import com.example.meddocsapp.RecycleBinActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard fragment showing overview statistics and recent patients.
 */
class DashboardFragment : Fragment() {

    companion object {
        private const val TAG = "DashboardFragment"
    }

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as MedDocsApplication).repository)
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        setHasOptionsMenu(true)

        AppLogger.d(TAG, "onCreateView")

        val totalPatientsTextView: TextView = root.findViewById(R.id.total_patients_text_view)
        val activePatientsTextView: TextView = root.findViewById(R.id.active_patients_text_view)
        val dischargedPatientsTextView: TextView = root.findViewById(R.id.discharged_patients_text_view)
        val totalFilesTextView: TextView = root.findViewById(R.id.total_files_text_view)
        val recentPatientsContainer: LinearLayout? = root.findViewById(R.id.recent_patients_container)

        patientViewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            totalPatientsTextView.text = patients.size.toString()
            activePatientsTextView.text = patients.count { it.status == "Active" }.toString()
            dischargedPatientsTextView.text = patients.count { it.status == "Discharged" }.toString()

            // Show recent patients with admission date
            recentPatientsContainer?.let { container ->
                updateRecentPatients(container, patients)
            }
        }

        patientViewModel.fileCount.observe(viewLifecycleOwner) { fileCount ->
            totalFilesTextView.text = fileCount.toString()
        }

        return root
    }

    private fun updateRecentPatients(container: LinearLayout, patients: List<Patient>) {
        container.removeAllViews()

        // Get recent patients (last 5 created)
        val recentPatients = patients
            .sortedByDescending { it.createdAt }
            .take(5)

        if (recentPatients.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "No patients yet"
                setTextColor(resources.getColor(R.color.textSecondary, null))
                setPadding(0, 16, 0, 16)
            }
            container.addView(emptyText)
            return
        }

        recentPatients.forEach { patient ->
            val patientView = layoutInflater.inflate(R.layout.item_recent_patient, container, false)

            val nameText = patientView.findViewById<TextView>(R.id.recent_patient_name)
            val detailsText = patientView.findViewById<TextView>(R.id.recent_patient_details)
            val statusText = patientView.findViewById<TextView>(R.id.recent_patient_status)

            nameText.text = patient.name

            // Build details with admission date and creation date
            val details = StringBuilder()
            details.append("Bed: ${patient.bedNumber}")

            patient.admissionDate?.let {
                details.append(" • Admitted: ${dateFormat.format(Date(it))}")
            }

            details.append("\nAdded: ${dateFormat.format(Date(patient.createdAt))}")

            detailsText.text = details.toString()

            statusText.text = patient.status
            if (patient.status == "Active") {
                statusText.setBackgroundResource(R.drawable.status_badge_active)
            } else {
                statusText.setBackgroundResource(R.drawable.status_badge_discharged)
            }

            container.addView(patientView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_recycle_bin -> {
                startActivity(Intent(requireContext(), RecycleBinActivity::class.java))
                true
            }
            R.id.action_developer_settings -> {
                startActivity(Intent(requireContext(), DeveloperSettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}