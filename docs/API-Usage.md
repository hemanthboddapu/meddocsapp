# API Usage

This document describes key public classes and how to use them in UI code.

## Models
### Patient
- Fields: `id: Long`, `name: String`, `bedNumber: String`, `status: String`, plus any additional metadata.
- Created without an id (0L) to auto-generate upon insert.

### PatientFile
- Fields: `id: Long`, `patientId: Long`, `uri: String`, `mimeType: String`, `fileName: String`, `size: Long`, `createdAt: Long`.
- Associate with a patient by `patientId`.

## DAOs
### PatientDao
- `suspend fun insert(patient: Patient)`
- `suspend fun update(patient: Patient)`
- `suspend fun delete(patient: Patient)`
- `fun getAllPatients(): Flow<List<Patient>>`
- `fun searchPatients(query: String): Flow<List<Patient>>`
- `fun getFileCount(): Flow<Int>` (example count)

### PatientFileDao
- `suspend fun insert(patientFile: PatientFile)`
- `suspend fun update(patientFile: PatientFile)`
- `suspend fun delete(patientFile: PatientFile)`
- `fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>`
- `fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>>`

## Repository
### PatientRepository
Responsibilities:
- Coordinates Patient and PatientFile DAOs
- Provides high-level operations for UI and view models

Typical methods:
- `fun getAllPatients(): Flow<List<Patient>>`
- `suspend fun insert(patient: Patient)`
- `suspend fun update(patient: Patient)`
- `suspend fun delete(patient: Patient)`
- `suspend fun insertFile(file: PatientFile)`
- `suspend fun updateFile(file: PatientFile)`
- `suspend fun delete(file: PatientFile)`
- `fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>`
- `fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>>`

## ViewModel
### PatientViewModel
- Exposes patient and file data as `Flow`/`LiveData`/`StateFlow` suitable for UI.
- Delegates writes to the repository on `viewModelScope`.

Usage in a Fragment (example):
```kotlin
class PatientsFragment : Fragment() {
    private val viewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as MedDocsApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            viewModel.patients.collect { list ->
                // update RecyclerView adapter
            }
        }
    }

    private fun addPatient() {
        lifecycleScope.launch {
            viewModel.insert(Patient(name = "John Doe", bedNumber = "101", status = "Active"))
        }
    }
}
```

## Notes
- Prefer collecting Flows using lifecycle-aware scopes.
- Handle errors and loading states in the ViewModel, expose to UI.
- Avoid long-running work on the main thread.

