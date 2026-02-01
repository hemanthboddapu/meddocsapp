# Security & Privacy

MedDocs stores patient-related data locally on the device. This document outlines security considerations, privacy practices, and recommendations for secure usage.

---

## Table of Contents
- [Data Privacy](#data-privacy)
- [Data Storage](#data-storage)
- [Access Control](#access-control)
- [Permissions](#permissions)
- [Data Sharing](#data-sharing)
- [Logging](#logging)
- [Backups](#backups)
- [Threat Model](#threat-model)
- [Recommendations](#recommendations)
- [Compliance Considerations](#compliance-considerations)

---

## Data Privacy

### Sensitive Data Handling
MedDocs handles Protected Health Information (PHI) including:
- Patient names and identifiers
- Bed/room numbers
- Medical problems/diagnoses
- Medical images and documents
- Admission/discharge dates

### Privacy Principles
1. **Data Minimization**: Collect only necessary information
2. **Local Storage**: All data stays on device by default
3. **No Analytics**: No telemetry or usage tracking
4. **No Network**: App works completely offline
5. **User Control**: Users decide what to share

---

## Data Storage

### Database
- **Location**: App-private internal storage (`/data/data/com.example.meddocsapp/`)
- **Format**: SQLite database via Room
- **Encryption**: Not enabled by default (see recommendations)

### Files
- **Location**: App-private storage (`filesDir`, `cacheDir`, `externalFilesDir`)
- **Access**: Only accessible by the app (or via FileProvider for sharing)

### Recycle Bin
- Deleted items stored temporarily (7 days default)
- JSON serialized in database
- Automatic cleanup on app launch

### Developer Logs
- Stored in app-private cache directory
- 2-day automatic retention
- Contains debug information (no PHI by default)

---

## Access Control

### Current State
- **No built-in authentication**: App relies on device-level security
- **No role-based access**: Single-user model

### Device-Level Protection
- Users should enable device PIN/password/biometric
- Enable full-disk encryption (default on modern Android)
- Use secure lock screen

### Recommendations for Enhanced Security
```kotlin
// Consider implementing:
// 1. App-level PIN/password
// 2. Biometric authentication (fingerprint/face)
// 3. Session timeout with re-authentication
// 4. Failed attempt lockout
```

---

## Permissions

### Requested Permissions
| Permission | Purpose | Required |
|------------|---------|----------|
| `READ_EXTERNAL_STORAGE` | Access shared files (legacy) | API < 33 |
| `READ_MEDIA_IMAGES` | Access images | API 33+ |
| `READ_MEDIA_VIDEO` | Access videos | API 33+ |
| `CAMERA` | Capture photos | Optional |

### Permission Best Practices
- Request at runtime with explanation
- Handle denial gracefully
- Provide alternatives when permission denied
- Never request unnecessary permissions

### Storage Access
- Use Scoped Storage (MediaStore/SAF) for external files
- Store app files in private directories
- Use FileProvider for sharing

---

## Data Sharing

### Share Intent (Outgoing)
- Requires explicit user action
- Uses `content://` URIs via FileProvider
- Includes patient context (name, bed number, ID)

### Share Intent (Incoming)
- Accept files from other apps
- User must select/create patient
- Files copied to app-private storage

### Export Features
- ZIP export of patient data
- Comparison image save
- Log export for debugging

### Warnings
- Display confirmation before sharing sensitive data
- Warn about PHI when sharing externally
- Log share actions in developer mode

---

## Logging

### AppLogger Behavior

| Mode | Logcat | File Log | Retention |
|------|--------|----------|-----------|
| Normal | ✅ | ❌ | N/A |
| Developer | ✅ | ✅ | 2 days |

### What Gets Logged
```kotlin
// Safe to log (examples):
AppLogger.d(TAG, "Patient inserted: ID=123")
AppLogger.i(TAG, "File count: 5")

// Never log:
// - Patient names
// - Medical information
// - File contents
// - Passwords/tokens
```

### Log Security
- Logs stored in app-private cache
- Not accessible without root/ADB
- Auto-deleted after 2 days
- Can be cleared manually

### Recommendations
```kotlin
// In production, sanitize logged data:
AppLogger.d(TAG, "Processing patient: ${patient.id}")  // OK
// NOT: AppLogger.d(TAG, "Patient: ${patient.name}")   // Bad
```

---

## Backups

### Android Backup
Current `AndroidManifest.xml` settings:
```xml
android:allowBackup="true"
android:fullBackupContent="@xml/backup_rules"
```

### Recommendations
For sensitive deployments:
```xml
<!-- Disable cloud backup -->
android:allowBackup="false"

<!-- Or use backup rules to exclude sensitive data -->
<full-backup-content>
    <exclude domain="database" path="." />
    <exclude domain="file" path="." />
</full-backup-content>
```

---

## Threat Model

### Assets
- Patient records (PHI)
- Medical files (images, documents)
- Application database

### Threat Actors
1. **Unauthorized device access**: Lost/stolen device
2. **Malicious apps**: Apps with storage access
3. **Physical access**: Someone with device access
4. **Network attackers**: N/A (offline app)

### Vulnerabilities & Mitigations

| Threat | Vulnerability | Mitigation |
|--------|--------------|------------|
| Lost device | Unencrypted data | Device encryption, app lock |
| Screenshot | Sensitive screens | `FLAG_SECURE` on screens |
| Logs | PHI in logs | Avoid logging PHI |
| Backup | Cloud backup | Disable or encrypt |
| Root access | DB readable | SQLCipher encryption |
| Screen recording | PHI visible | `FLAG_SECURE` |

---

## Recommendations

### For Users
1. ✅ Enable device PIN/password/biometric
2. ✅ Keep device OS updated
3. ✅ Don't share device with untrusted users
4. ✅ Regularly review recycle bin
5. ✅ Export important data periodically
6. ❌ Don't take screenshots of PHI
7. ❌ Don't share login credentials

### For Developers

#### Immediate (Low Effort)
```kotlin
// Add FLAG_SECURE to sensitive activities
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)

// Disable logging PHI
AppLogger.d(TAG, "Patient ID: ${patient.id}") // ID only, not name
```

#### Medium Term
- [ ] Implement app-level PIN lock
- [ ] Add biometric authentication option
- [ ] Encrypt database with SQLCipher
- [ ] Add session timeout

#### Long Term
- [ ] Add audit logging
- [ ] Implement role-based access
- [ ] Add data export encryption
- [ ] Security penetration testing

### SQLCipher Integration (Example)
```kotlin
// build.gradle.kts
implementation("net.zetetic:android-database-sqlcipher:4.5.4")
implementation("androidx.sqlite:sqlite-ktx:2.4.0")

// AppDatabase.kt
val factory = SupportFactory(passphrase)
Room.databaseBuilder(context, AppDatabase::class.java, "meddocs_database")
    .openHelperFactory(factory)
    .build()
```

---

## Compliance Considerations

### HIPAA (US Healthcare)
MedDocs as-is may not meet HIPAA requirements. Consider:
- Access controls and authentication
- Audit logs for PHI access
- Encryption at rest
- Backup and disaster recovery
- Business Associate Agreements

### GDPR (EU)
For EU users, ensure:
- Data subject rights (access, deletion, portability)
- Consent for data processing
- Data minimization
- Privacy by design

### Local Regulations
Consult legal counsel for specific jurisdictions and use cases.

---

## Reporting Security Issues

If you discover a security vulnerability:

1. **Do not** open a public issue
2. Email security concerns to maintainers privately
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (optional)

We will respond within 48 hours and work to address the issue promptly.

---

## Security Checklist

### Before Release
- [ ] PHI not logged in production
- [ ] FLAG_SECURE on sensitive screens
- [ ] Backup settings reviewed
- [ ] Permissions minimized
- [ ] Error messages don't leak data
- [ ] ProGuard/R8 enabled for release

### Regular Audits
- [ ] Review logging statements
- [ ] Check for hardcoded secrets
- [ ] Update dependencies
- [ ] Test permission handling
- [ ] Verify file access patterns
