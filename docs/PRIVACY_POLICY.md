# MedDocs App Privacy Policy

**Last Updated: February 3, 2026**

---

## 1. Introduction

MedDocs ("we", "our", or "the app") is a local patient record management application designed for healthcare professionals. This Privacy Policy explains how we handle your data and is designed to comply with privacy regulations worldwide.

## 2. Data Controller

This app operates as a local data management tool. There is no central data controller as all data remains on the user's device under their direct control.

## 3. Data Collection and Storage

### 3.1 Local Storage Only

- **All patient data, medical files, and app settings are stored LOCALLY on your device only.**
- We do NOT collect, transmit, or store any data on external servers.
- No data is sent over the internet unless you explicitly choose to share files.
- No user accounts or registration required.

### 3.2 Types of Data Stored

| Data Category | Examples | Purpose |
|--------------|----------|---------|
| Patient Information | Names, bed numbers, patient IDs, DOB, gender, medical problems | Patient record management |
| Medical Files | Images, documents, videos, audio recordings, text notes | Attach supporting files to patient records |
| App Settings | User preferences, developer mode, recycle bin settings | Personalize app behavior |
| Diagnostic Logs | App events and errors (if developer mode enabled) | Debugging and troubleshooting |

### 3.3 Data Not Collected

We do NOT collect:
- Personal information about app users
- Location data
- Device identifiers
- Usage analytics
- Advertising identifiers
- Browsing history
- Contact lists
- Any data from other apps

## 4. Data Security

### 4.1 Device-Level Protection

- Data is stored in the app's private internal storage
- Protected by Android's app sandboxing security model
- Only MedDocs can access its stored data
- Recommended: Use device-level security (PIN, fingerprint, face unlock)

### 4.2 No Network Transmission

- No cloud backup or synchronization
- No internet connection required for core functionality
- Data exists only on the device where it was created

### 4.3 Automatic Local Backups

- Backups are created automatically on a user-defined schedule via WorkManager
- Backups run in the background when device is charging and battery is not low
- Backups are stored locally in public Documents/MedDocsBackups directory
- Restore is manual from the Backup & Restore screen; auto-restore on fresh install is not performed
- On first launch, backups are deferred until the user chooses to restore or proceed
- No passwords or encryption keys required (user-friendly design)
- Backup files are ZIP archives with Base64-encoded metadata
- Users have full control over backup creation, frequency, sharing, and deletion
- Only the latest backup is kept; older backups are automatically deleted

### 4.4 Data Isolation

- Each installation is completely independent
- No data sharing between devices
- Uninstalling the app permanently deletes all stored data

## 5. Data Sharing

### 5.1 User-Initiated Sharing

You may choose to share patient files via:
- Android's built-in share functionality
- Email attachments
- Messaging apps
- Cloud storage apps

**Important:** Sharing is entirely at your discretion. Once data is shared externally, it is no longer under the app's control.

### 5.2 No Third-Party Access

- We do not share data with any third parties
- No analytics services (Google Analytics, Firebase, etc.)
- No advertising networks
- No crash reporting services that transmit data
- No social media integrations

## 6. Data Retention

### 6.1 Active Data

- Patient records and files remain until you delete them
- You have full control over all data lifecycle
- No automatic data expiration for active records

### 6.2 Recycle Bin

- Deleted items move to recycle bin for 7 days (configurable)
- After retention period, items are permanently deleted
- Physical files are removed from storage upon permanent deletion
- You can restore items from recycle bin before expiration

### 6.3 Diagnostic Logs

- Developer mode logs are automatically deleted after 2 days
- You can manually clear logs at any time
- Logs are stored locally and never transmitted

## 7. Your Rights

As the data controller of your own data, you have the right to:

| Right | How to Exercise |
|-------|-----------------|
| **Access** | View all data directly in the app |
| **Rectification** | Edit any patient information |
| **Erasure** | Delete any or all data |
| **Data Portability** | Export/share patient data |
| **Restriction** | Control what data is stored |
| **Object** | Simply stop using the app |

### 7.1 Complete Data Deletion

To delete all app data:
1. Go to Android Settings → Apps → MedDocs → Storage
2. Tap "Clear Data" or "Clear Storage"
3. All data will be permanently removed

Or simply uninstall the app.

## 8. Children's Privacy

- This app is intended for use by healthcare professionals
- It is not designed for use by children under 18 years of age
- We do not knowingly collect data from children

## 9. Medical Disclaimer

**Important Notice:**

- MedDocs is a record-keeping tool only
- It does not provide medical advice, diagnosis, or treatment
- It is not a substitute for professional medical judgment
- Always consult qualified healthcare professionals for medical decisions
- We are not responsible for clinical decisions made using this app
- The app does not validate medical information entered

## 10. Permissions

The app may request the following Android permissions:

| Permission | Purpose | Required? |
|------------|---------|-----------|
| Camera | Capture photos directly in the app | Optional |
| Microphone | Record audio notes | Optional |
| Storage | Access shared files from other apps | Optional |

- All permissions are used solely for the stated purposes
- Data captured remains local to your device
- You can deny any permission and still use other features

## 11. Changes to This Policy

- We may update this Privacy Policy periodically
- Changes will be reflected in the "Last Updated" date
- Significant changes will be noted in app release notes
- Continued use of the app after changes constitutes acceptance

## 12. Open Source & Transparency

- The app's functionality is transparent and predictable
- No hidden data collection or transmission
- Privacy-by-design principles are followed throughout

## 13. Compliance

This app is designed with privacy-by-design principles aligned with:

- **GDPR** (General Data Protection Regulation) - EU
- **HIPAA** considerations (local storage, user-controlled data)
- **CCPA** (California Consumer Privacy Act)
- **LGPD** (Lei Geral de Proteção de Dados) - Brazil
- **POPIA** (Protection of Personal Information Act) - South Africa

**Note:** While the app follows privacy best practices, healthcare professionals are responsible for ensuring their use of the app complies with applicable regulations in their jurisdiction.

## 14. Contact

For questions about this Privacy Policy or data practices:

- Review this document in the app (Menu → Privacy Policy)
- Contact the app developer through appropriate channels
- Check the app's repository for updates

## 15. Summary

| Aspect | Status |
|--------|--------|
| Data stored locally | ✅ Yes |
| Data sent to servers | ❌ No |
| User accounts required | ❌ No |
| Analytics collected | ❌ No |
| Ads displayed | ❌ No |
| Third-party sharing | ❌ No |
| User controls data | ✅ Yes |
| Data deletion available | ✅ Yes |

---

**By using MedDocs, you acknowledge that you have read and understood this Privacy Policy and agree to its terms.**

---

*This privacy policy is also accessible within the app via Menu → Privacy Policy.*
