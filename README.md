# MedDocs App

A comprehensive Android application for managing patient records and associated medical files locally on a device. Built with modern Android development practices using Kotlin, MVVM architecture, Room database, and Coroutines/Flow for reactive data handling.

## Overview

- **Platform**: Android (Kotlin)
- **Min SDK**: 28 (Android 9.0)
- **Target SDK**: 36
- **Architecture**: MVVM (UI → ViewModel → Repository → Room/DAOs)
- **Key Libraries**: AndroidX, Room, Kotlin Coroutines/Flow, ViewModel, Navigation, ViewBinding, Glide, PhotoView, Gson

## Goals

- Provide a simple, offline-first way to manage patient records on a device
- Keep data organization clear: Patients and their associated medical files
- Offer fast search and filtering to find the right patient or file
- Enable safe sharing and comparison of medical images
- Support soft-delete with recycle bin for data recovery
- Maintain a clean, testable architecture (MVVM + Repository + Room)
- Provide developer tools for debugging and log analysis

## Features

### Patient Management
- **CRUD Operations**: Create, read, update, delete patient records
- **Quick Discharge**: Mark patients as discharged with optional discharge date
- **Patient ID Number**: Hospital/clinic ID field for external reference
- **Admission Date Tracking**: Capture and display admission dates
- **Age-Based Avatars**: Auto-populated avatars based on age and gender (10 avatar variants)
- **Active First Sorting**: Active patients always displayed before discharged patients
- **Status Management**: Track "Active" or "Discharged" status

### File Management
- **Multi-Type Support**: Attach images, PDFs, videos, Word documents, PowerPoints
- **External App Integration**: Open files with appropriate external apps (PDF viewers, video players, etc.)
- **File Type Icons**: Visual indicators for different file types (video, PDF, doc, ppt)
- **Image-Only Filter**: Filter to show only image files
- **File Operations**: Rename, delete, share files
- **Preview Icons**: Thumbnails for images, type icons for other formats

### Search & Filter
- **Multi-Field Search**: Search by patient name, bed number, or patient ID
- **Status Filter**: Filter by All, Active Only, or Discharged Only
- **Smart Sorting**: Active patients always appear first in results

### Dashboard
- **Statistics Overview**: Total patients, active count, discharged count, total files
- **Recent Patients**: Quick access to recently added patients
- **Date Display**: Shows admission dates and creation timestamps
- **Quick Navigation**: Access Developer Settings, Recycle Bin, and Help

### Image Comparison
- **Side-by-Side View**: Compare two images simultaneously
- **Independent Rotation**: Rotate left and right images independently (90° increments)
- **Landscape Mode**: Toggle between portrait and landscape orientation
- **Save Comparison**: Save the comparison view as a new image to patient folder
- **Fullscreen Mode**: Hide controls for maximum viewing area
- **Image Picker**: Grid-based image selection from patient's files

### File Sharing
- **Share Intent Receiver**: Accept shared files from external apps
- **Patient Search**: Search for patients with quick suggestions when receiving shares
- **Inline Patient Creation**: Add new patient directly from share screen if not found
- **Multi-File Export**: Share multiple files with patient info (name, bed number, patient ID)
- **ZIP Export**: Export patient data as ZIP archive

### Recycle Bin
- **Soft Delete**: Deleted items move to recycle bin instead of permanent deletion
- **7-Day Retention**: Items automatically permanently deleted after 7 days
- **Restore Function**: Restore patients or files from recycle bin
- **Manual Cleanup**: Empty recycle bin manually or delete individual items
- **Auto Cleanup**: Expired items cleaned up on app launch

### Developer Mode
- **File-Based Logging**: Enable detailed logging for debugging
- **2-Day Log Retention**: Logs automatically cleaned up after 2 days
- **Log Viewer**: View logs directly in the app
- **Log Export**: Export logs for specific time ranges
- **Clear Logs**: Delete all log files
- **Toggle Control**: Enable/disable developer mode from settings

## How It Works

### Data Flow
```
UI (Activities/Fragments)
    ↓ observes
ViewModel (LiveData/StateFlow)
    ↓ collects
Repository (Business Logic)
    ↓ queries
DAOs (Room Database)
    ↓ persists
SQLite Database
```

### Patient Management
- Patient list backed by Room DAO returning `Flow<List<Patient>>`
- Adding/updating/deleting are suspend operations on IO dispatcher
- ViewModel exposes patients to UI via LiveData
- Changes automatically propagate through the reactive chain

### File Attachments
- Files represented by `PatientFile` entity linked via `patientId` foreign key
- Scoped storage APIs used for file operations
- URIs stored in database, files in app-private storage
- Foreign key cascade ensures files deleted with parent patient

### Recycle Bin
- Deleted items serialized to JSON and stored in `recycle_bin` table
- Original data preserved for restoration
- Expiration timestamp calculated at deletion time
- Cleanup runs on app launch and can be triggered manually

### Avatar Selection
- `AvatarHelper` utility calculates age from date of birth
- Gender and age determine avatar from 10 variants
- Categories: Infant (0-2), Child (3-12), Teen (13-19), Adult (20-59), Senior (60+)
- Fallback to default avatar if gender/age unknown

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/meddocsapp/
│   │   ├── AddEditPatientActivity.kt    # Patient form
│   │   ├── AppDatabase.kt               # Room database with migrations
│   │   ├── AppLogger.kt                 # Centralized logging utility
│   │   ├── AvatarHelper.kt              # Avatar selection by age/gender
│   │   ├── DashboardActivity.kt         # Dashboard screen
│   │   ├── DeveloperSettingsActivity.kt # Developer mode settings
│   │   ├── HomeActivity.kt              # Main entry with navigation
│   │   ├── ImageCompareActivity.kt      # Side-by-side image comparison
│   │   ├── ImageViewActivity.kt         # Full-screen image viewer
│   │   ├── MainActivity.kt              # Splash/launcher
│   │   ├── MedDocsApplication.kt        # Application class
│   │   ├── Patient.kt                   # Patient entity
│   │   ├── PatientDao.kt                # Patient database operations
│   │   ├── PatientDetailActivity.kt     # Patient detail with files
│   │   ├── PatientFile.kt               # File attachment entity
│   │   ├── PatientFileDao.kt            # File database operations
│   │   ├── PatientRepository.kt         # Data repository
│   │   ├── PatientViewModel.kt          # ViewModel for UI
│   │   ├── RecycleBinActivity.kt        # Recycle bin management
│   │   ├── RecycleBinDao.kt             # Recycle bin operations
│   │   ├── RecycleBinItem.kt            # Recycle bin entity
│   │   ├── ShareActivity.kt             # Receive shared files
│   │   └── ui/
│   │       ├── dashboard/
│   │       │   └── DashboardFragment.kt
│   │       └── patients/
│   │           └── PatientsFragment.kt
│   └── res/
│       ├── drawable/                    # Icons and avatars
│       ├── layout/                      # Activity/fragment layouts
│       ├── menu/                        # Menu resources
│       ├── values/                      # Colors, strings, themes
│       └── xml/                         # File provider paths
├── src/test/                            # Unit tests
└── src/androidTest/                     # Instrumented tests
```

## Database Schema

### Version 7 (Current)

**patients**
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-generated) |
| patientIdNumber | TEXT | Hospital/clinic ID |
| name | TEXT | Patient full name |
| bedNumber | TEXT | Bed/room number |
| status | TEXT | "Active" or "Discharged" |
| gender | TEXT | Patient gender |
| dob | TEXT | Date of birth (dd/MM/yyyy) |
| problem | TEXT | Medical problem description |
| admissionDate | INTEGER | Admission timestamp (ms) |
| dischargeDate | INTEGER | Discharge timestamp (ms) |
| createdAt | INTEGER | Record creation timestamp (ms) |

**patient_files**
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-generated) |
| patientId | INTEGER | Foreign key to patients |
| uri | TEXT | File URI |
| mimeType | TEXT | MIME type |
| fileName | TEXT | Display name |
| size | INTEGER | File size in bytes |
| createdAt | INTEGER | Timestamp (ms) |

**recycle_bin**
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-generated) |
| itemType | TEXT | "patient" or "file" |
| originalId | INTEGER | Original item ID |
| itemData | TEXT | JSON serialized data |
| patientName | TEXT | For display (patients) |
| fileName | TEXT | For display (files) |
| deletedAt | INTEGER | Deletion timestamp (ms) |
| expiresAt | INTEGER | Expiration timestamp (ms) |

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or 21
- Android SDK with API level 28+
- Android device/emulator (API 28+)

### Setup
1. Clone or open the project in Android Studio
2. Ensure `local.properties` points to valid Android SDK:
   ```properties
   sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```
3. Set JAVA_HOME to JDK 17 or 21
4. Sync Gradle (File → Sync Project with Gradle Files)

### Build & Run

**Using Android Studio:**
- Click Run ▶️ to deploy to connected device/emulator

**Using Command Line (Windows PowerShell):**
```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Install on connected device
.\gradlew.bat installDebug

# Run tests
.\gradlew.bat test
```

**APK Location:**
```
app/build/outputs/apk/debug/app-debug.apk
```

## Testing

### Unit Tests
Located in `app/src/test/java/com/example/meddocsapp/`
- `PatientViewModelTest.kt` - ViewModel tests with fake DAOs
- `PatientRepositoryTest.kt` - Repository tests

```powershell
.\gradlew.bat test
```

### Instrumented Tests
Located in `app/src/androidTest/java/`

```powershell
.\gradlew.bat connectedAndroidTest
```

## Troubleshooting

### Build Errors
- **Gradle sync fails**: Invalidate caches (File → Invalidate Caches) and restart
- **Room/KSP errors**: Clean and rebuild (Build → Clean Project, then Rebuild)
- **Resource not found**: Check for malformed XML in drawable/layout files

### Runtime Issues
- **File opening fails**: Ensure external apps installed for file type; check FileProvider paths
- **Crash on patient add/edit**: Check for null values in required fields
- **Database migration errors**: Clear app data or uninstall/reinstall

### Common Fixes
```powershell
# Clean build
.\gradlew.bat clean

# Full rebuild
.\gradlew.bat clean assembleDebug

# Clear Gradle cache
Remove-Item -Recurse -Force .gradle
Remove-Item -Recurse -Force app\build
.\gradlew.bat assembleDebug
```

## Configuration

### File Provider Paths
Located in `app/src/main/res/xml/file_paths.xml`:
- `files-path`: Internal app files (filesDir)
- `cache-path`: Cache files
- `external-files-path`: App-specific external storage

### ProGuard Rules
Located in `app/proguard-rules.pro` for release builds with minification enabled.

## License

See `LICENSE` file for details.

## Acknowledgments

- [AndroidX Libraries](https://developer.android.com/jetpack/androidx)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [PhotoView](https://github.com/Baseflow/PhotoView) - Image zoom support
- [Glide](https://github.com/bumptech/glide) - Image loading
- [Gson](https://github.com/google/gson) - JSON serialization
