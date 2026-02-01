# API Usage

This document describes the public APIs for MedDocs components. Use this as a reference when building new features or extending existing functionality.

## Table of Contents
- [Models](#models)
- [DAOs](#daos)
- [Repository](#repository)
- [ViewModel](#viewmodel)
- [Utilities](#utilities)
- [Usage Examples](#usage-examples)

---

## Models

### Patient

Represents a patient record in the database.

```kotlin
@Parcelize
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientIdNumber: String = "",    // Hospital/clinic ID
    val name: String,                     // Required: Patient full name
    val bedNumber: String,                // Required: Bed/room number
    val status: String,                   // "Active" or "Discharged"
    val gender: String? = null,           // "Male", "Female", or null
    val dob: String? = null,              // Date of birth (dd/MM/yyyy)
    val problem: String? = null,          // Medical problem description
    val admissionDate: Long? = null,      // Admission timestamp (milliseconds)
    val dischargeDate: Long? = null,      // Discharge timestamp (milliseconds)
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
```

**Usage:**
```kotlin
// Create new patient
val patient = Patient(
    name = "John Doe",
    bedNumber = "101",
    status = "Active",
    gender = "Male",
    dob = "15/03/1985",
    admissionDate = System.currentTimeMillis()
)

// Update patient
val updated = patient.copy(status = "Discharged", dischargeDate = System.currentTimeMillis())
```

---

### PatientFile

Represents a file attached to a patient.

```kotlin
@Parcelize
@Entity(
    tableName = "patient_files",
    foreignKeys = [ForeignKey(
        entity = Patient::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PatientFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,                  // Foreign key to Patient
    val uri: String,                      // File URI (content:// or file://)
    val mimeType: String,                 // MIME type (e.g., "image/jpeg")
    val fileName: String,                 // Display name
    val size: Long,                       // File size in bytes
    val createdAt: Long                   // Creation timestamp (milliseconds)
) : Parcelable
```

**Usage:**
```kotlin
val file = PatientFile(
    patientId = patient.id,
    uri = "content://...",
    mimeType = "image/jpeg",
    fileName = "xray_001.jpg",
    size = 1024000,
    createdAt = System.currentTimeMillis()
)
```

---

### RecycleBinItem

Represents a deleted item awaiting permanent deletion.

```kotlin
@Parcelize
@Entity(tableName = "recycle_bin")
data class RecycleBinItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String,                 // "patient" or "file"
    val originalId: Long,                 // Original item ID before deletion
    val itemData: String,                 // JSON serialized original data
    val patientName: String = "",         // For display (patients only)
    val fileName: String = "",            // For display (files only)
    val deletedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long                   // When item will be permanently deleted
) : Parcelable
```

---

## DAOs

### PatientDao

```kotlin
@Dao
interface PatientDao {
    /** Insert a new patient */
    @Insert
    suspend fun insert(patient: Patient)

    /** Update existing patient */
    @Update
    suspend fun update(patient: Patient)

    /** Delete patient (cascades to files) */
    @Delete
    suspend fun delete(patient: Patient)

    /** Get all patients, active first, then alphabetical */
    @Query("SELECT * FROM patients ORDER BY CASE WHEN status = 'Active' THEN 0 ELSE 1 END, name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    /** Get only active patients */
    @Query("SELECT * FROM patients WHERE status = 'Active' ORDER BY name ASC")
    fun getActivePatients(): Flow<List<Patient>>

    /** Get only discharged patients */
    @Query("SELECT * FROM patients WHERE status = 'Discharged' ORDER BY name ASC")
    fun getDischargedPatients(): Flow<List<Patient>>

    /** Search by name, bed number, or patient ID */
    @Query("""
        SELECT * FROM patients 
        WHERE name LIKE '%' || :query || '%' 
           OR bedNumber LIKE '%' || :query || '%'
           OR patientIdNumber LIKE '%' || :query || '%'
        ORDER BY CASE WHEN status = 'Active' THEN 0 ELSE 1 END, name ASC
    """)
    fun searchPatients(query: String): Flow<List<Patient>>

    /** Get total file count */
    @Query("SELECT COUNT(*) FROM patient_files")
    fun getFileCount(): Flow<Int>

    /** Get active patient count */
    @Query("SELECT COUNT(*) FROM patients WHERE status = 'Active'")
    fun getActivePatientCount(): Flow<Int>

    /** Get discharged patient count */
    @Query("SELECT COUNT(*) FROM patients WHERE status = 'Discharged'")
    fun getDischargedPatientCount(): Flow<Int>
}
```

---

### PatientFileDao

```kotlin
@Dao
interface PatientFileDao {
    @Insert
    suspend fun insert(patientFile: PatientFile)

    @Update
    suspend fun update(patientFile: PatientFile)

    @Delete
    suspend fun delete(patientFile: PatientFile)

    /** Get all files for a patient */
    @Query("SELECT * FROM patient_files WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>

    /** Get only image files for a patient */
    @Query("SELECT * FROM patient_files WHERE patientId = :patientId AND mimeType LIKE 'image/%' ORDER BY createdAt DESC")
    fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>>
}
```

---

### RecycleBinDao

```kotlin
@Dao
interface RecycleBinDao {
    @Insert
    suspend fun insert(item: RecycleBinItem)

    @Delete
    suspend fun delete(item: RecycleBinItem)

    /** Get all items, newest first */
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllItems(): Flow<List<RecycleBinItem>>

    /** Delete expired items */
    @Query("DELETE FROM recycle_bin WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredItems(currentTime: Long)

    /** Delete all items */
    @Query("DELETE FROM recycle_bin")
    suspend fun deleteAll()

    /** Get item count */
    @Query("SELECT COUNT(*) FROM recycle_bin")
    fun getItemCount(): Flow<Int>
}
```

---

## Repository

### PatientRepository

The repository is the single source of truth for all patient-related data.

```kotlin
class PatientRepository(
    patientDao: PatientDao,
    patientFileDao: PatientFileDao,
    recycleBinDao: RecycleBinDao? = null
) {
    // ============ Properties (Reactive Flows) ============
    
    /** All patients, active first */
    val allPatients: Flow<List<Patient>>
    
    /** Active patients only */
    val activePatients: Flow<List<Patient>>
    
    /** Discharged patients only */
    val dischargedPatients: Flow<List<Patient>>
    
    /** Total file count */
    val fileCount: Flow<Int>
    
    /** Recycle bin items */
    val recycleBinItems: Flow<List<RecycleBinItem>>?
    
    /** Recycle bin count */
    val recycleBinCount: Flow<Int>?

    // ============ Patient Operations ============
    
    suspend fun insert(patient: Patient)
    suspend fun update(patient: Patient)
    suspend fun delete(patient: Patient)
    suspend fun dischargePatient(patient: Patient, dischargeDate: Long? = null)
    fun searchPatients(query: String): Flow<List<Patient>>

    // ============ File Operations ============
    
    fun getFilesForPatient(patientId: Long): Flow<List<PatientFile>>
    fun getImageFilesForPatient(patientId: Long): Flow<List<PatientFile>>
    suspend fun insertFile(patientFile: PatientFile)
    suspend fun updateFile(patientFile: PatientFile)
    suspend fun delete(patientFile: PatientFile)

    // ============ Recycle Bin Operations ============
    
    suspend fun movePatientToRecycleBin(patient: Patient, retentionDays: Int = 7)
    suspend fun moveFileToRecycleBin(patientFile: PatientFile, retentionDays: Int = 7)
    suspend fun restorePatient(recycleBinItem: RecycleBinItem)
    suspend fun restoreFile(recycleBinItem: RecycleBinItem)
    suspend fun permanentlyDelete(recycleBinItem: RecycleBinItem)
    suspend fun clearRecycleBin()
    suspend fun cleanupExpiredRecycleBinItems()
}
```

---

## ViewModel

### PatientViewModel

```kotlin
class PatientViewModel(repository: PatientRepository) : ViewModel() {
    
    // ============ LiveData Properties ============
    
    val allPatients: LiveData<List<Patient>>
    val fileCount: LiveData<Int>
    val recycleBinItems: LiveData<List<RecycleBinItem>>?
    val recycleBinCount: LiveData<Int>?

    // ============ Patient Operations ============
    
    fun insert(patient: Patient)              // Launches in viewModelScope
    fun update(patient: Patient)
    fun delete(patient: Patient)
    fun dischargePatient(patient: Patient, dischargeDate: Long? = null)
    fun searchPatients(query: String): LiveData<List<Patient>>

    // ============ File Operations ============
    
    fun getFilesForPatient(patientId: Long): LiveData<List<PatientFile>>
    fun getImageFilesForPatient(patientId: Long): LiveData<List<PatientFile>>
    fun insertFile(patientFile: PatientFile)
    fun updateFile(patientFile: PatientFile)
    fun delete(patientFile: PatientFile)

    // ============ Recycle Bin Operations ============
    
    fun movePatientToRecycleBin(patient: Patient)
    fun moveFileToRecycleBin(patientFile: PatientFile)
    fun restoreFromRecycleBin(recycleBinItem: RecycleBinItem)
    fun permanentlyDelete(recycleBinItem: RecycleBinItem)
    fun clearRecycleBin()
    fun cleanupExpiredRecycleBinItems()
}
```

### PatientViewModelFactory

```kotlin
class PatientViewModelFactory(
    private val repository: PatientRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T
}
```

---

## Utilities

### AppLogger

Centralized logging with developer mode support.

```kotlin
object AppLogger {
    /** Initialize in Application.onCreate() */
    fun init(appContext: Context)
    
    /** Check if developer mode is enabled */
    fun isDeveloperModeEnabled(): Boolean
    
    /** Enable/disable developer mode */
    fun setDeveloperModeEnabled(enabled: Boolean)
    
    /** Log debug message */
    fun d(tag: String, message: String)
    
    /** Log info message */
    fun i(tag: String, message: String)
    
    /** Log warning */
    fun w(tag: String, message: String, throwable: Throwable? = null)
    
    /** Log error */
    fun e(tag: String, message: String, throwable: Throwable? = null)
    
    /** Get list of log files */
    fun getLogFiles(): List<File>
    
    /** Read log file content */
    fun readLogFile(file: File): String
    
    /** Export logs for time range */
    fun exportLogs(startTime: Long, endTime: Long): String
    
    /** Clear all log files */
    fun clearAllLogs()
}
```

---

### AvatarHelper

Avatar selection based on patient demographics.

```kotlin
object AvatarHelper {
    /**
     * Get avatar drawable resource based on gender and age.
     * 
     * Age categories:
     * - Infant: 0-2 years
     * - Child: 3-12 years
     * - Teen: 13-19 years
     * - Adult: 20-59 years
     * - Senior: 60+ years
     *
     * @param gender "Male", "Female", "M", "F", etc.
     * @param dob Date of birth in dd/MM/yyyy format
     * @return Drawable resource ID
     */
    fun getAvatarResource(gender: String?, dob: String?): Int
}
```

**Available avatars:**
- `R.drawable.avatar_male_infant`, `R.drawable.avatar_female_infant`
- `R.drawable.avatar_male_child`, `R.drawable.avatar_female_child`
- `R.drawable.avatar_male_teen`, `R.drawable.avatar_female_teen`
- `R.drawable.avatar_male_adult`, `R.drawable.avatar_female_adult`
- `R.drawable.avatar_male_senior`, `R.drawable.avatar_female_senior`
- `R.drawable.avatar_default`

---

## Usage Examples

### Fragment with ViewModel

```kotlin
class PatientsFragment : Fragment() {
    
    private val viewModel: PatientViewModel by viewModels {
        PatientViewModelFactory(
            (requireActivity().application as MedDocsApplication).repository
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observe patients
        viewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            adapter.submitList(patients)
        }
        
        // Search
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                viewModel.searchPatients(query).observe(viewLifecycleOwner) { results ->
                    adapter.submitList(results)
                }
                return true
            }
        })
    }

    private fun addPatient() {
        val patient = Patient(
            name = "New Patient",
            bedNumber = "101",
            status = "Active",
            admissionDate = System.currentTimeMillis()
        )
        viewModel.insert(patient)
    }

    private fun dischargePatient(patient: Patient) {
        viewModel.dischargePatient(patient, System.currentTimeMillis())
    }

    private fun deletePatient(patient: Patient) {
        // Moves to recycle bin instead of permanent delete
        viewModel.movePatientToRecycleBin(patient)
    }
}
```

### Activity with File Operations

```kotlin
class PatientDetailActivity : AppCompatActivity() {
    
    private val viewModel: PatientViewModel by viewModels {
        PatientViewModelFactory(
            (application as MedDocsApplication).repository
        )
    }
    
    private lateinit var patient: Patient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        patient = intent.getParcelableExtra("patient")!!
        
        // Observe files
        viewModel.getFilesForPatient(patient.id).observe(this) { files ->
            fileAdapter.submitList(files)
        }
        
        // Set avatar
        val avatarRes = AvatarHelper.getAvatarResource(patient.gender, patient.dob)
        binding.avatarImage.setImageResource(avatarRes)
    }

    private fun addFile(uri: Uri, mimeType: String, fileName: String, size: Long) {
        val file = PatientFile(
            patientId = patient.id,
            uri = uri.toString(),
            mimeType = mimeType,
            fileName = fileName,
            size = size,
            createdAt = System.currentTimeMillis()
        )
        viewModel.insertFile(file)
    }

    private fun deleteFile(file: PatientFile) {
        viewModel.moveFileToRecycleBin(file)
    }
}
```

### Developer Mode Logging

```kotlin
class SomeActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d(TAG, "Activity created")
        
        try {
            // Some operation
            performOperation()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun enableDeveloperMode() {
        AppLogger.setDeveloperModeEnabled(true)
        AppLogger.i(TAG, "Developer mode enabled by user")
    }
}
```

### Recycle Bin Management

```kotlin
class RecycleBinActivity : AppCompatActivity() {
    
    private val viewModel: PatientViewModel by viewModels { ... }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Cleanup expired on launch
        viewModel.cleanupExpiredRecycleBinItems()
        
        // Observe items
        viewModel.recycleBinItems?.observe(this) { items ->
            adapter.submitList(items)
        }
    }

    private fun restoreItem(item: RecycleBinItem) {
        viewModel.restoreFromRecycleBin(item)
    }

    private fun permanentlyDeleteItem(item: RecycleBinItem) {
        viewModel.permanentlyDelete(item)
    }

    private fun emptyRecycleBin() {
        viewModel.clearRecycleBin()
    }
}
```

---

## Notes

- **Lifecycle Awareness**: Always observe LiveData with a lifecycle owner to prevent leaks
- **Coroutines**: ViewModel operations launch in `viewModelScope` - no manual scope management needed
- **Null Safety**: Handle nullable fields appropriately, especially for optional patient data
- **Error Handling**: Wrap risky operations in try-catch and log errors with AppLogger
- **Testing**: Use fake DAOs in unit tests (see `PatientRepositoryTest.kt`)
