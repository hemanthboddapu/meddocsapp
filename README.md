# MedDocs App

MedDocs is an Android app for managing patient records and associated medical files locally on a device. It’s built with Kotlin, MVVM, Room for local persistence, and Coroutines/Flow for reactive updates.

- Platform: Android (Kotlin)
- Architecture: MVVM (UI → ViewModel → Repository → Room/DAOs)
- Key Libraries: AndroidX, Room, Kotlin Coroutines/Flow, ViewModel, Navigation, ViewBinding/DataBinding

## Goals
- Provide a simple, offline-first way to manage patient records on a device.
- Keep data organization clear: Patients and their associated medical files.
- Offer fast search and filtering to find the right patient or file.
- Enable safe sharing and comparison of medical images where appropriate.
- Maintain a clean, testable architecture (MVVM + Repository + Room).

## Features
- Patient CRUD: create, read, update, delete patient records.
- Attach files to patients (images and other MIME types).
- View and filter files (e.g., image files only).
- Search patients by name or bed number.
- Dashboard with basic counts/metrics.
- Compare images side-by-side.
- Share selected files via Android sharing intents.
- Offline-first local persistence with Room.

## How Features Work
- Patient Management
  - The patient list is backed by a Room DAO returning Flow<List<Patient>>.
  - Adding/updating/deleting patients are suspend operations dispatched on IO.
  - The ViewModel exposes patients to the UI via StateFlow/LiveData.
- File Attachments
  - Files (e.g., images) are represented by `PatientFile` and linked to a patient via `patientId`.
  - Use scoped storage APIs for picking and saving files; URIs are stored in the database.
  - The UI fetches files for a selected patient by collecting a Flow from the DAO/repository.
- Search & Filter
  - Search queries are passed to the DAO (e.g., `searchPatients(query)`) which returns a filtered Flow.
  - Image-only filters for `PatientFile` are implemented via DAO queries on MIME type.
- Dashboard & Metrics
  - Simple counts (e.g., total patients, total files) are exposed via DAO queries and collected in the ViewModel.
- Image Comparison
  - Select two images associated with a patient and render them side-by-side in the comparison screen.
  - Large images are handled via content URIs; ensure Glide/Coil (if used) decodes efficiently.
- Sharing
  - Sharing uses Android’s Share intents with content URIs and appropriate MIME types.
  - Confirm user intent and warn about sensitive data as needed.

## Project Structure
- `app/src/main` — App source (activities, fragments, view models, repository, database, daos, models)
- `app/src/test` — Unit tests (e.g., `PatientViewModelTest.kt` with Fake DAOs)
- `app/src/androidTest` — Instrumented tests (if present)
- `app/build.gradle.kts` — Module build config
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/` — Root gradle config and wrapper

Core components you’ll typically find:
- Models: `Patient`, `PatientFile`
- Persistence: `AppDatabase` (Room), `PatientDao`, `PatientFileDao`
- Domain: `PatientRepository`
- Presentation: `PatientViewModel` (+ factory)
- UI: Activities/Fragments using Navigation and ViewBinding/DataBinding

## Getting Started
### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17 (or the version configured in `gradle.properties`)
- An Android device/emulator (API level per module config)

### Setup
1. Clone or open the project in Android Studio.
2. Ensure `local.properties` points to a valid Android SDK, e.g. `sdk.dir=C:\\Android\\Sdk`.
3. Sync Gradle. Android Studio will download dependencies.

### Build & Run
- Use Android Studio’s Run to deploy the `app` module to an emulator or device.
- Entry point activity (typical): `HomeActivity` hosting navigation.
- If you prefer CLI (Windows PowerShell):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Testing
- Unit tests live in `app/src/test`. Example: `PatientViewModelTest.kt` uses fake DAOs and verifies repository/view model interactions.
- Run tests in Android Studio or via CLI (Windows PowerShell):

```powershell
.\gradlew.bat test
```

- Instrumented tests (if present) reside in `app/src/androidTest` and run on a device/emulator:

```powershell
.\gradlew.bat connectedAndroidTest
```

## Troubleshooting
- Room/KSP errors: Clean and rebuild; ensure KSP/annotation processors are configured.
- ViewModel factory not found: Provide `PatientRepository` via an application or DI container.
- File access: Use scoped storage; ensure runtime permissions if accessing non-app-private files.
- Navigation issues: Check nav graph and fragment IDs; verify resource generation.

## License
See `LICENSE`. If absent, this repository currently has no explicit license.

## Acknowledgments
- Android Jetpack libraries
- Kotlin Coroutines
- Room persistence
