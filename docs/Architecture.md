# Architecture

MedDocs uses a layered MVVM architecture that cleanly separates UI from data and domain logic. This document describes the architectural decisions and component interactions.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Activities  │  │  Fragments  │  │      Adapters       │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                     │             │
│         └────────────────┼─────────────────────┘             │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    ViewModels                            ││
│  │              (PatientViewModel)                          ││
│  └──────────────────────┬──────────────────────────────────┘│
└─────────────────────────┼───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                 PatientRepository                        ││
│  │   (Business logic, data coordination, recycle bin)       ││
│  └──────────────────────┬──────────────────────────────────┘│
└─────────────────────────┼───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌───────────┐  ┌───────────────┐  ┌─────────────────────┐  │
│  │PatientDao │  │PatientFileDao │  │   RecycleBinDao     │  │
│  └─────┬─────┘  └───────┬───────┘  └──────────┬──────────┘  │
│        │                │                      │             │
│        └────────────────┼──────────────────────┘             │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              AppDatabase (Room)                          ││
│  │         SQLite - meddocs_database                        ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Layers

### UI Layer
- **Activities**: Handle screen-level lifecycle and navigation
- **Fragments**: Manage UI state and user interactions
- **Adapters**: Bind data to RecyclerViews with DiffUtil for efficient updates
- **ViewBinding**: Type-safe view access without findViewById

### ViewModel Layer
- `PatientViewModel`: Exposes app state via LiveData, orchestrates UI operations
- Uses `viewModelScope` for coroutine lifecycle management
- Survives configuration changes

### Repository Layer
- `PatientRepository`: Single source of truth for patient data
- Coordinates between multiple DAOs
- Implements business logic (discharge, recycle bin operations)
- Transforms data as needed for UI consumption

### Data Layer
- Room DAOs provide type-safe database operations
- Entities map directly to SQLite tables
- Migrations handle schema evolution
- Flow-based reactive queries for real-time updates

### Cross-Cutting Concerns
- `AppLogger`: Centralized logging with developer mode support
- `AvatarHelper`: Avatar selection utility based on patient demographics

## Data Flow

### Read Operations (Reactive)
```
DAO.getAllPatients()  →  Flow<List<Patient>>
        ↓
Repository.allPatients  →  Flow<List<Patient>>
        ↓
ViewModel.allPatients  →  LiveData<List<Patient>>
        ↓
Fragment.observe()  →  UI Update
```

### Write Operations (Imperative)
```
UI Action (click)
        ↓
ViewModel.insert(patient)  →  viewModelScope.launch
        ↓
Repository.insert(patient)  →  suspend function
        ↓
DAO.insert(patient)  →  Room @Insert
        ↓
SQLite INSERT
        ↓
Flow emits new list  →  UI updates automatically
```

## Components

### Entities (Models)

**Patient**
```kotlin
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientIdNumber: String = "",
    val name: String,
    val bedNumber: String,
    val status: String,              // "Active" | "Discharged"
    val gender: String? = null,
    val dob: String? = null,         // dd/MM/yyyy format
    val problem: String? = null,
    val admissionDate: Long? = null, // milliseconds
    val dischargeDate: Long? = null, // milliseconds
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
```

**PatientFile**
```kotlin
@Entity(tableName = "patient_files",
    foreignKeys = [ForeignKey(
        entity = Patient::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = CASCADE
    )])
data class PatientFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val uri: String,
    val mimeType: String,
    val fileName: String,
    val size: Long,
    val createdAt: Long
) : Parcelable
```

**RecycleBinItem**
```kotlin
@Entity(tableName = "recycle_bin")
data class RecycleBinItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String,        // "patient" | "file"
    val originalId: Long,
    val itemData: String,        // JSON serialized
    val patientName: String = "",
    val fileName: String = "",
    val deletedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long          // 7 days from deletion
) : Parcelable
```

### DAOs

**PatientDao**
- `insert(patient)`, `update(patient)`, `delete(patient)`
- `getAllPatients()`: Active first, then alphabetical
- `getActivePatients()`, `getDischargedPatients()`
- `searchPatients(query)`: Searches name, bedNumber, patientIdNumber
- `getFileCount()`, `getActivePatientCount()`, `getDischargedPatientCount()`

**PatientFileDao**
- `insert(file)`, `update(file)`, `delete(file)`
- `getFilesForPatient(patientId)`
- `getImageFilesForPatient(patientId)`: Filters by image/* MIME type

**RecycleBinDao**
- `insert(item)`, `delete(item)`, `deleteAll()`
- `getAllItems()`: Ordered by deletedAt DESC
- `getExpiredItems(currentTime)`, `deleteExpiredItems(currentTime)`
- `getItemCount()`

### Repository

**PatientRepository**
- Wraps all three DAOs
- Exposes Flow properties: `allPatients`, `activePatients`, `dischargedPatients`, `recycleBinItems`
- Business methods:
  - `dischargePatient(patient, dischargeDate?)`: Updates status and date
  - `movePatientToRecycleBin(patient)`: Serializes and soft-deletes
  - `moveFileToRecycleBin(file)`: Serializes and soft-deletes
  - `restorePatient(item)`, `restoreFile(item)`: Deserializes and restores
  - `cleanupExpiredRecycleBinItems()`: Removes expired items

### ViewModel

**PatientViewModel**
- Converts Flow to LiveData for lifecycle-aware observation
- Launches coroutines in `viewModelScope`
- Delegates all operations to repository
- Created via `PatientViewModelFactory` with repository injection

### Application

**MedDocsApplication**
- Initializes `AppLogger`
- Creates `AppDatabase` instance
- Creates `PatientRepository` with all DAOs
- Provides repository access for ViewModelFactory

## Key Activities

| Activity | Purpose |
|----------|---------|
| `MainActivity` | Splash/launcher screen |
| `HomeActivity` | Main container with BottomNavigationView |
| `AddEditPatientActivity` | Patient form with date pickers |
| `PatientDetailActivity` | View patient info and attached files |
| `ImageCompareActivity` | Side-by-side image comparison |
| `ImageViewActivity` | Fullscreen image viewer with PhotoView |
| `ShareActivity` | Receive shared files from other apps |
| `RecycleBinActivity` | Manage deleted items |
| `DeveloperSettingsActivity` | Developer mode and log management |

## Navigation

- Single-activity pattern with `HomeActivity` as container
- `BottomNavigationView` hosts Dashboard and Patients fragments
- Detail screens launched as separate activities via explicit Intents
- Patient data passed via Parcelable extras

## Database Migrations

| Version | Changes |
|---------|---------|
| 1 | Initial patients table |
| 2 | Added patient_files table with foreign key |
| 3 | Added gender, dob, problem to patients |
| 4 | Added fileName, size, createdAt to patient_files |
| 5 | Added patientIdNumber to patients |
| 6 | Added admissionDate, dischargeDate, createdAt to patients |
| 7 | Added recycle_bin table |

## Error Handling

### Repository Level
- Try-catch around database operations
- Logging via AppLogger for debugging
- Graceful fallbacks (e.g., permanent delete if recycle bin unavailable)

### ViewModel Level
- Coroutine exception handling
- State exposure for loading/error/success

### UI Level
- Toast messages for user feedback
- AlertDialogs for confirmations
- Crash prevention with null safety

## Logging Strategy

**AppLogger** provides:
- Standard logcat output in all modes
- File-based logging when developer mode enabled
- Automatic cleanup of logs older than 2 days
- Export functionality for debugging

Usage:
```kotlin
AppLogger.d("Tag", "Debug message")
AppLogger.e("Tag", "Error message", throwable)
```

## Extensibility

To add a new feature/entity:

1. **Create Entity**: Define `@Entity` data class
2. **Create DAO**: Define `@Dao` interface with queries
3. **Update Database**: Add entity to `@Database`, create migration
4. **Update Repository**: Add DAO reference and business methods
5. **Update ViewModel**: Expose new data/operations
6. **Build UI**: Create Activity/Fragment with layouts
7. **Add Navigation**: Wire up intents/navigation
8. **Add Logging**: Use AppLogger for debugging

## Dependencies

```kotlin
// Core
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.material)

// Room
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

// Lifecycle
implementation(libs.androidx.lifecycle.viewmodel.ktx)
implementation(libs.androidx.lifecycle.livedata.ktx)

// Navigation
implementation(libs.androidx.navigation.fragment.ktx)
implementation(libs.androidx.navigation.ui.ktx)

// Image handling
implementation(libs.glide)
implementation(libs.photoview)

// Serialization
implementation("com.google.code.gson:gson:2.10.1")
```
