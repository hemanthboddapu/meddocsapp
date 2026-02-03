# Changelog

All notable changes to MedDocs App will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.1.1] - 2026-02-03

### Changed
- Removed auto-restore on fresh install for predictability; restore is manual from Backup & Restore
- Manual restore file picker now defaults to Documents (Documents/MedDocsBackups)
- Backups are deferred on first launch until user decides to restore or continue
- Skip creating backups when there are 0 attached files
- SAF manual restore reliability improved (stream to temp, unified restore path)

### Fixed
- Avatar selection from numeric age strings (e.g., "2", "2y") now maps to correct age bucket
- Duplicate drawable resources for avatars (PNG vs XML) removed to fix build

---

## [1.1.0] - 2026-02-03

### Added

#### Automatic Backup & Restore
- **WorkManager Scheduled Backups**: Backups run automatically in the background via WorkManager
- **Smart Scheduling**: Backups occur when device is charging and battery is not low
- **Adjustable Frequency**: Daily, every 2 days, every 3 days, or weekly options
- **SAF Integration**: Uses Storage Access Framework for persistent folder access across reinstalls
- **Manual Restore from Any File**: Pick any backup ZIP from anywhere on your device to restore
- **Public Storage**: Backups stored in Documents/MedDocsBackups for persistence
- **No Password Required**: Simple, user-friendly backup without encryption complexity
- **Full Data Backup**: Includes all patients, metadata, and attached files
- **Backup Management**: View, share, and delete existing backups from the app
- **Optimized Retention**: Keeps only the latest backup; older ones deleted automatically
- **SAF Streaming Restore**: Restores backups via content resolver without raw file access
- **Progress Indicator**: Visual feedback during backup/restore operations
- **Merge or Replace**: Choose to add backup data to existing or replace all data
- **Lazy Metadata Loading**: Backup list loads instantly; metadata fetched on-demand

#### UI Improvements
- **Bed Number Field**: Defaults to numeric keyboard with toggle icon to switch to text mode
- **Privacy Note**: Subtle one-line reminder on dashboard that data is stored locally
- **Manual Restore Button**: Dedicated button in Backup screen to pick and restore any ZIP file

---

## [1.0.0] - 2026-02-02

### Added

#### Patient Management
- **CRUD Operations**: Full create, read, update, delete functionality for patient records
- **Quick Discharge**: Mark patients as discharged with optional discharge date picker
- **Patient ID Number**: Hospital/clinic patient ID field for external reference
- **Admission Date**: Capture and display admission dates with date picker
- **Age-Based Avatars**: 10 avatar variants based on age (infant, child, teen, adult, senior) and gender
- **Active First Sorting**: Active patients always displayed before discharged patients
- **Status Tracking**: "Active" or "Discharged" status with visual indicators

#### File Management
- **Multi-Format Support**: Attach images, PDFs, videos, Word documents, PowerPoints
- **External App Integration**: Open files with appropriate viewer apps via FileProvider
- **File Type Icons**: Visual icons for different file types (video, PDF, doc, ppt)
- **Image-Only Filter**: Filter to show only image files in patient detail view
- **File Operations**: Rename, delete, and share files
- **File Sharing**: Share files via WhatsApp, email, or other apps with patient info

#### Search & Filter
- **Multi-Field Search**: Search by name, bed number, or patient ID number
- **Status Filter**: Filter patients by All, Active Only, or Discharged Only
- **Quick Search**: Real-time search results as user types

#### Dashboard
- **Statistics Cards**: Total patients, active count, discharged count, total files
- **Recent Patients**: List of recently added patients with quick access
- **Date Display**: Shows admission dates and record creation timestamps
- **Menu Access**: Developer Settings, Recycle Bin, and Help accessible from dashboard

#### Image Comparison
- **Side-by-Side View**: Compare two patient images simultaneously
- **Independent Rotation**: Rotate left and right images in 90° increments
- **Landscape Mode**: Toggle between portrait and landscape orientation
- **Save Comparison**: Save comparison view as new image to patient folder
- **Image Picker**: Grid-based image selection from patient's files

#### File Sharing
- **Share Intent Receiver**: Accept shared files from external apps
- **Patient Search**: Search and select patient when receiving shares
- **Inline Patient Creation**: Add new patient directly from share screen
- **Multi-File Export**: Share multiple files with patient context
- **ZIP Export**: Export complete patient data as ZIP archive

#### Recycle Bin
- **Soft Delete**: Deleted items move to recycle bin instead of permanent deletion
- **7-Day Retention**: Items automatically permanently deleted after 7 days
- **Restore Function**: Restore patients or files from recycle bin
- **Manual Cleanup**: Empty recycle bin or delete individual items
- **Auto Cleanup**: Expired items cleaned up on app launch

#### Developer Mode
- **File-Based Logging**: Enable detailed logging for debugging
- **2-Day Log Retention**: Logs automatically cleaned up after 2 days
- **Log Viewer**: View logs directly in the app
- **Log Export**: Export logs for specific time ranges
- **Clear Logs**: Delete all log files manually
- **Toggle Control**: Enable/disable developer mode from settings

#### Help System
- **Help Button**: Access help dialog from patients view toolbar
- **Feature Guide**: Instructions on how to use all major features
- **Contextual Help**: Tips for search, filtering, and patient management

### Technical

#### Architecture
- **MVVM Pattern**: Clean separation of UI, ViewModel, Repository, and Data layers
- **Room Database**: SQLite persistence with type-safe DAOs
- **Kotlin Coroutines**: Async operations with structured concurrency
- **Flow/LiveData**: Reactive data streams for UI updates
- **ViewBinding**: Type-safe view access

#### Database
- **Version 7 Schema**: Full schema with patients, patient_files, and recycle_bin tables
- **Migrations 1-7**: Incremental schema migrations for upgrades
- **Foreign Keys**: Cascade delete for patient files

#### Dependencies
- AndroidX Core, AppCompat, Material Design
- Room with KSP annotation processing
- Lifecycle ViewModel and LiveData
- Navigation Component
- Glide for image loading
- PhotoView for image zoom
- Gson for JSON serialization

### Fixed
- **PDF/Video/Doc Viewing**: Files now open correctly in external apps using FileProvider
- **Add Patient Crash**: Proper error handling and null safety in patient form
- **Image Compare Crash**: Fixed infinite loop in zoom sync; replaced with rotation controls
- **TextInputLayout Inflation**: Fixed Material Components style references
- **Button Visibility**: Fixed swap button and control visibility in compare view

---

## [Unreleased]

### Planned Features
- **Data Cleanup Activity**: UI for bulk cleaning discharged patient data
- **Dashboard File Size**: Display total file size on dashboard
- **Customizable Retention**: Allow users to configure recycle bin retention period
- **Database Encryption**: Optional SQLCipher encryption for sensitive data
- **Biometric Lock**: Optional app lock with fingerprint/face unlock
- **Cloud Backup**: Optional integration with Google Drive or other cloud storage

### Known Issues
- Large file imports may be slow on older devices
- Some file types may not have compatible viewer apps installed

---

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 1.1.1 | 2026-02-03 | Removed auto-restore, improved manual restore, various fixes |
| 1.1.0 | 2026-02-03 | Added encrypted backup & restore feature |
| 1.0.0 | 2026-02-02 | Initial release with full feature set |

---

## Migration Notes

### Upgrading from Development Builds
If upgrading from a development build:
1. Export any important patient data first
2. Uninstall the old version
3. Install the new version
4. Database migrations should handle schema updates automatically

### Database Migrations
The app includes migrations for all schema versions (1-7). Upgrading should preserve all existing data. If you encounter issues:
1. Enable Developer Mode
2. Check logs for migration errors
3. Report issues with log export
