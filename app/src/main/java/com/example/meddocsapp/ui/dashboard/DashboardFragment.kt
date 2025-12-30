package com.example.meddocsapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.meddocsapp.MedDocsApplication
import com.example.meddocsapp.PatientViewModel
import com.example.meddocsapp.PatientViewModelFactory
import com.example.meddocsapp.R

class DashboardFragment : Fragment() {

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as MedDocsApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val totalPatientsTextView: TextView = root.findViewById(R.id.total_patients_text_view)
        val activePatientsTextView: TextView = root.findViewById(R.id.active_patients_text_view)
        val dischargedPatientsTextView: TextView = root.findViewById(R.id.discharged_patients_text_view)
        val totalFilesTextView: TextView = root.findViewById(R.id.total_files_text_view)

        patientViewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            totalPatientsTextView.text = patients.size.toString()
            activePatientsTextView.text = patients.count { it.status == "Active" }.toString()
            dischargedPatientsTextView.text = patients.count { it.status == "Discharged" }.toString()
        }

        patientViewModel.fileCount.observe(viewLifecycleOwner) { fileCount ->
            totalFilesTextView.text = fileCount.toString()
        }

        return root
    }
}