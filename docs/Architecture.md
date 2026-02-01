# Architecture

MedDocs uses a layered MVVM architecture that cleanly separates UI from data and domain logic.

## Layers
- UI: Activities and Fragments handle user interaction and rendering.
- ViewModel: `PatientViewModel` exposes app state and operations. It collects Flows from the repository and performs writes in viewModelScope.
- Repository: `PatientRepository` coordinates data access across DAOs and applies business rules.
- Persistence: Room `AppDatabase` with DAOs `PatientDao`, `PatientFileDao` stores `Patient` and `PatientFile` entities.

## Data Flow
- DAOs return `Flow<List<T>>` for reactive streams.
- Repository forwards/transforms Flows; write operations are `suspend` and dispatched on IO.
- ViewModel exposes state (e.g., patients list) to UI via LiveData/StateFlow.
- UI observes and renders, and delegates user actions back to ViewModel.

## Components
- Models: `Patient(id: Long, name: String, bedNumber: String, status: String, ...)`
- Models: `PatientFile(id: Long, patientId: Long, uri: String, mimeType: String, fileName: String, size: Long, createdAt: Long)`
- DAOs:
  - `PatientDao`: insert/update/delete, `getAllPatients(): Flow<List<Patient>>`, `searchPatients(query): Flow<List<Patient>>`, `getFileCount(): Flow<Int>` (example count)
  - `PatientFileDao`: insert/update/delete, `getFilesForPatient(patientId): Flow<List<PatientFile>>`, `getImageFilesForPatient(patientId): Flow<List<PatientFile>>`
- Repository: `PatientRepository` wraps DAOs and provides high-level operations.
- ViewModel: `PatientViewModel` orchestrates reads/writes and exposes UI-friendly APIs.

## Dependency Graph
- `AppDatabase` creates `PatientDao` and `PatientFileDao`.
- `PatientRepository` depends on both DAOs.
- `PatientViewModel` depends on `PatientRepository` (via factory or DI).
- UI depends on `PatientViewModel`.

## Navigation
- Single-activity pattern (e.g., `HomeActivity`) with `BottomNavigationView` hosting fragments.
- Arguments are passed to details screens (patient detail, image compare) via intents or SafeArgs.

## Error Handling and State
- Repository catching IO/DB exceptions and surfacing error states.
- ViewModel maps to UI state: loading, error, empty.

## Extensibility
To add a new feature/entity:
1. Create the entity data class and DAO.
2. Add to `AppDatabase`.
3. Inject into `PatientRepository`.
4. Expose via `PatientViewModel`.
5. Build UI screens and navigation.

