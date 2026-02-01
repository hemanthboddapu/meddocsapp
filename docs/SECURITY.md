# Security & Privacy

MedDocs stores patient-related data locally on the device. Follow these guidelines to reduce risk.

## Data Privacy
- Treat patient data (names, bed numbers, statuses, files) as PII. Do not log sensitive data.
- Consider encrypting Room database (e.g., SQLCipher) if threat model requires it.

## Storage & Files
- Store attachments (`PatientFile`) in app-private storage where possible.
- Use scoped storage APIs (MediaStore or SAF) for external files.
- Validate MIME types and sanitize filenames.

## Access Control
- App has no built-in auth. Rely on device security or add optional app lock (PIN/biometric).

## Permissions
- Request minimal runtime permissions and explain the purpose.
- Handle denial gracefully; provide alternatives.

## Backups
- Configure manifest to control backups (e.g., `android:allowBackup`). Disable if data should not be backed up to cloud.

## Data Export & Sharing
- Ensure explicit user action for exporting or sharing files.
- Warn users when sharing potentially sensitive data.

## Threats & Mitigations
- Data leakage via screenshots/logs → avoid logging PII; add `FLAG_SECURE` on sensitive screens if needed.
- Unauthorized access → use app-private storage; recommend device-level security.

