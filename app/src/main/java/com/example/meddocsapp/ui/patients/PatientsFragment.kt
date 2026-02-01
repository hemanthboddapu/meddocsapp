package com.example.meddocsapp.ui.patients

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.meddocsapp.AppLogger
import com.example.meddocsapp.AvatarHelper
import com.example.meddocsapp.MedDocsApplication
import com.example.meddocsapp.Patient
import com.example.meddocsapp.PatientDetailActivity
import com.example.meddocsapp.PatientViewModel
import com.example.meddocsapp.PatientViewModelFactory
import com.example.meddocsapp.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragment displaying the list of patients with search, filter, and quick actions.
 */
class PatientsFragment : Fragment() {

    companion object {
        private const val TAG = "PatientsFragment"
    }

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as MedDocsApplication).repository)
    }

    private lateinit var patientRecyclerView: RecyclerView
    private lateinit var addPatientFab: ExtendedFloatingActionButton
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var patientAdapter: PatientAdapter

    private var currentFilterStatus: String? = null // null = all, "Active", "Discharged"

    private val addPatientLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val patient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(AddEditPatientActivity.EXTRA_PATIENT, Patient::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(AddEditPatientActivity.EXTRA_PATIENT)
            }
            patient?.let {
                patientViewModel.insert(it)
                AppLogger.d(TAG, "Patient added: ${it.name}")
            }
        }
    }

    private val editPatientLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val patient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(AddEditPatientActivity.EXTRA_PATIENT, Patient::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(AddEditPatientActivity.EXTRA_PATIENT)
            }
            patient?.let {
                patientViewModel.update(it)
                AppLogger.d(TAG, "Patient updated: ${it.name}")
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

        AppLogger.d(TAG, "onCreateView")

        patientRecyclerView = root.findViewById(R.id.patient_recycler_view)
        addPatientFab = root.findViewById(R.id.add_patient_fab)
        emptyStateLayout = root.findViewById(R.id.empty_state_layout)

        patientAdapter = PatientAdapter(
            onPatientClicked = { patient ->
                val intent = Intent(requireActivity(), PatientDetailActivity::class.java)
                intent.putExtra(PatientDetailActivity.EXTRA_PATIENT, patient)
                startActivity(intent)
            },
            onPatientLongClicked = { patient, view ->
                showPatientContextMenu(patient, view)
            },
            onQuickDischargeClicked = { patient ->
                showDischargeDialog(patient)
            }
        )
        patientRecyclerView.adapter = patientAdapter
        patientRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Collapse FAB on scroll
        patientRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && addPatientFab.isExtended) {
                    addPatientFab.shrink()
                } else if (dy < 0 && !addPatientFab.isExtended) {
                    addPatientFab.extend()
                }
            }
        })

        addPatientFab.setOnClickListener {
            val intent = Intent(requireActivity(), AddEditPatientActivity::class.java)
            addPatientLauncher.launch(intent)
        }

        patientViewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            patients?.let {
                val filteredList = applyFilter(it)
                patientAdapter.submitList(filteredList)
                updateEmptyState(filteredList.isEmpty())
            }
        }

        return root
    }

    private fun applyFilter(patients: List<Patient>): List<Patient> {
        return when (currentFilterStatus) {
            "Active" -> patients.filter { it.status == "Active" }
            "Discharged" -> patients.filter { it.status == "Discharged" }
            else -> patients
        }
    }

    private fun showPatientContextMenu(patient: Patient, view: View) {
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
                R.id.action_discharge_patient -> {
                    showDischargeDialog(patient)
                    true
                }
                R.id.action_delete_patient -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Patient")
                        .setMessage("Move this patient and all their files to recycle bin?")
                        .setPositiveButton("Delete") { _, _ ->
                            patientViewModel.movePatientToRecycleBin(patient)
                            AppLogger.d(TAG, "Patient moved to recycle bin: ${patient.name}")
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

    private fun showDischargeDialog(patient: Patient) {
        if (patient.status == "Discharged") {
            AlertDialog.Builder(requireContext())
                .setTitle("Already Discharged")
                .setMessage("This patient is already discharged.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        AlertDialog.Builder(requireContext())
            .setTitle("Discharge Patient")
            .setMessage("Mark ${patient.name} as discharged?\n\nYou can optionally set a discharge date.")
            .setPositiveButton("Discharge Today") { _, _ ->
                patientViewModel.dischargePatient(patient, System.currentTimeMillis())
                AppLogger.d(TAG, "Patient discharged: ${patient.name}")
            }
            .setNeutralButton("Select Date") { _, _ ->
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        patientViewModel.dischargePatient(patient, calendar.timeInMillis)
                        AppLogger.d(TAG, "Patient discharged with date: ${patient.name}")
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("How to Use MedDocs")
            .setMessage("""
                📋 PATIENT MANAGEMENT
                • Tap '+' to add a new patient
                • Tap a patient to view details & files
                • Long-press for edit/delete/discharge options
                
                🔍 SEARCH & FILTER
                • Use the search icon to find patients by name, bed number, or patient ID
                • Use the filter icon to show only Active or Discharged patients
                
                ⚡ QUICK ACTIONS
                • Swipe or long-press a patient to quickly discharge them
                • Quick discharge icon appears on active patients
                
                📁 FILES & IMAGES
                • Open patient details to add/view files
                • Use Compare feature to view images side-by-side
                • Export patient data as ZIP file
                
                📤 SHARING
                • Share files from other apps directly to MedDocs
                • Select a patient to attach the shared file
            """.trimIndent())
            .setPositiveButton("Got it!", null)
            .show()
    }

    private fun showFilterDialog() {
        val options = arrayOf("All Patients", "Active Only", "Discharged Only")
        val currentSelection = when (currentFilterStatus) {
            "Active" -> 1
            "Discharged" -> 2
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Patients")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                currentFilterStatus = when (which) {
                    1 -> "Active"
                    2 -> "Discharged"
                    else -> null
                }
                // Re-apply filter
                patientViewModel.allPatients.value?.let { patients ->
                    val filteredList = applyFilter(patients)
                    patientAdapter.submitList(filteredList)
                    updateEmptyState(filteredList.isEmpty())
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            patientRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            patientRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
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
                    if (it.isEmpty()) {
                        patientViewModel.allPatients.observe(viewLifecycleOwner) { patients ->
                            patients?.let { list ->
                                val filtered = applyFilter(list)
                                patientAdapter.submitList(filtered)
                            }
                        }
                    } else {
                        patientViewModel.searchPatients(it).observe(viewLifecycleOwner) { patients ->
                            patients?.let { list ->
                                val filtered = applyFilter(list)
                                patientAdapter.submitList(filtered)
                            }
                        }
                    }
                }
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Adapter for displaying patients in a RecyclerView with quick actions.
     */
    private class PatientAdapter(
        private val onPatientClicked: (Patient) -> Unit,
        private val onPatientLongClicked: (Patient, View) -> Unit,
        private val onQuickDischargeClicked: (Patient) -> Unit
    ) : ListAdapter<Patient, PatientAdapter.PatientViewHolder>(PatientsDiffCallback()) {

        class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.patient_name_text_view)
            private val detailsTextView: TextView = itemView.findViewById(R.id.patient_details_text_view)
            private val avatarImage: ImageView = itemView.findViewById(R.id.avatar_image)
            private val statusBadge: TextView = itemView.findViewById(R.id.status_badge)
            private val quickDischargeButton: ImageView? = itemView.findViewById(R.id.quick_discharge_button)

            fun bind(
                patient: Patient,
                onPatientClicked: (Patient) -> Unit,
                onPatientLongClicked: (Patient, View) -> Unit,
                onQuickDischargeClicked: (Patient) -> Unit
            ) {
                nameTextView.text = patient.name

                // Build details string with admission date if available
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                var details = "Bed: ${patient.bedNumber}"
                patient.admissionDate?.let {
                    details += " • Admitted: ${dateFormat.format(Date(it))}"
                }
                detailsTextView.text = details

                // Set avatar based on gender and age
                avatarImage.setImageResource(AvatarHelper.getAvatarResource(patient.gender, patient.dob))

                // Set status badge
                statusBadge.text = patient.status
                if (patient.status == "Active") {
                    statusBadge.setBackgroundResource(R.drawable.status_badge_active)
                    quickDischargeButton?.visibility = View.VISIBLE
                } else {
                    statusBadge.setBackgroundResource(R.drawable.status_badge_discharged)
                    quickDischargeButton?.visibility = View.GONE
                }

                itemView.setOnClickListener { onPatientClicked(patient) }
                itemView.setOnLongClickListener {
                    onPatientLongClicked(patient, itemView)
                    true
                }
                quickDischargeButton?.setOnClickListener { onQuickDischargeClicked(patient) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.patient_list_item, parent, false)
            return PatientViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
            val currentPatient = getItem(position)
            holder.bind(currentPatient, onPatientClicked, onPatientLongClicked, onQuickDischargeClicked)
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