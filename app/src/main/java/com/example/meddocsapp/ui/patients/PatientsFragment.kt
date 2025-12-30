package com.example.meddocsapp.ui.patients

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meddocsapp.AddEditPatientActivity
import com.example.meddocsapp.MedDocsApplication
import com.example.meddocsapp.Patient
import com.example.meddocsapp.PatientDetailActivity
import com.example.meddocsapp.PatientViewModel
import com.example.meddocsapp.PatientViewModelFactory
import com.example.meddocsapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PatientsFragment : Fragment() {

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as MedDocsApplication).repository)
    }

    private lateinit var patientRecyclerView: RecyclerView
    private lateinit var addPatientFab: FloatingActionButton
    private lateinit var patientAdapter: PatientAdapter

    private val addPatientLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Patient>(AddEditPatientActivity.EXTRA_PATIENT)?.let {
                patientViewModel.insert(it)
            }
        }
    }

    private val editPatientLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Patient>(AddEditPatientActivity.EXTRA_PATIENT)?.let {
                patientViewModel.update(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_patients, container, false)
        setHasOptionsMenu(true)

        patientRecyclerView = root.findViewById(R.id.patient_recycler_view)
        addPatientFab = root.findViewById(R.id.add_patient_fab)

        patientAdapter = PatientAdapter(
            onPatientClicked = { patient ->
                val intent = Intent(requireActivity(), PatientDetailActivity::class.java)
                intent.putExtra(PatientDetailActivity.EXTRA_PATIENT, patient)
                startActivity(intent)
            },
            onPatientLongClicked = { patient, view ->
                val popup = PopupMenu(requireContext(), view)
                popup.menuInflater.inflate(R.menu.patient_list_item_menu, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit_patient -> {
                            val intent = Intent(requireActivity(), AddEditPatientActivity::class.java)
                            intent.putExtra(AddEditPatientActivity.EXTRA_PATIENT, patient)
                            editPatientLauncher.launch(intent)
                            true
                        }
                        R.id.action_delete_patient -> {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Delete Patient")
                                .setMessage("Are you sure you want to delete this patient and all their files?")
                                .setPositiveButton("Delete") { _, _ ->
                                    patientViewModel.delete(patient)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        )
        patientRecyclerView.adapter = patientAdapter
        patientRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        addPatientFab.setOnClickListener {
            val intent = Intent(requireActivity(), AddEditPatientActivity::class.java)
            addPatientLauncher.launch(intent)
        }

        patientViewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            patients?.let { patientAdapter.submitList(it) }
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    val searchQuery = "%${it}%"
                    patientViewModel.searchPatients(searchQuery).observe(viewLifecycleOwner) { patients ->
                        patients?.let { patientAdapter.submitList(it) }
                    }
                }
                return true
            }
        })
    }

    private class PatientAdapter(
        private val onPatientClicked: (Patient) -> Unit,
        private val onPatientLongClicked: (Patient, View) -> Unit
    ) :
        ListAdapter<Patient, PatientAdapter.PatientViewHolder>(PatientsDiffCallback()) {

        class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.patient_name_text_view)
            private val detailsTextView: TextView = itemView.findViewById(R.id.patient_details_text_view)

            fun bind(patient: Patient, onPatientClicked: (Patient) -> Unit, onPatientLongClicked: (Patient, View) -> Unit) {
                nameTextView.text = patient.name
                detailsTextView.text = "Bed: ${patient.bedNumber} - ${patient.status}"
                itemView.setOnClickListener { onPatientClicked(patient) }
                itemView.setOnLongClickListener {
                    onPatientLongClicked(patient, itemView)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.patient_list_item, parent, false)
            return PatientViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
            val currentPatient = getItem(position)
            holder.bind(currentPatient, onPatientClicked, onPatientLongClicked)
        }

        private class PatientsDiffCallback : DiffUtil.ItemCallback<Patient>() {
            override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
                return oldItem == newItem
            }
        }
    }
}