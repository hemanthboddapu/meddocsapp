package com.example.meddocsapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

/**
 * Activity displaying the app's privacy policy.
 * Explains data collection, storage, and user rights.
 */
class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        setupToolbar()
        loadPrivacyPolicy()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Privacy Policy"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadPrivacyPolicy() {
        val policyText = findViewById<TextView>(R.id.privacy_policy_text)
        policyText.text = getPrivacyPolicyText()
    }

    private fun getPrivacyPolicyText(): String {
        return """
MEDDOCS APP PRIVACY POLICY

Last Updated: February 3, 2026

1. INTRODUCTION

MedDocs ("we", "our", or "the app") is a local patient record management application designed for healthcare professionals. This Privacy Policy explains how we handle your data.

2. DATA COLLECTION AND STORAGE

2.1 Local Storage Only
• All patient data, medical files, and app settings are stored LOCALLY on your device only.
• We do NOT collect, transmit, or store any data on external servers.
• No data is sent over the internet unless you explicitly choose to share files.

2.2 Types of Data Stored
• Patient Information: Names, bed numbers, patient IDs, dates of birth, gender, medical problems, admission/discharge dates
• Medical Files: Images, documents, videos, audio recordings, and text notes attached to patient records
• App Settings: User preferences, developer mode settings, recycle bin retention settings
• Logs: If developer mode is enabled, diagnostic logs are stored locally for up to 2 days

3. DATA SECURITY

3.1 Device Security
• Data is stored in the app's private internal storage, protected by Android's app sandboxing
• Only MedDocs can access its stored data
• We recommend using device-level security (PIN, fingerprint, face unlock) for additional protection

3.2 No Cloud Sync
• There is no cloud backup or synchronization feature
• Data exists only on the device where it was created
• Uninstalling the app will permanently delete all stored data

4. DATA SHARING

4.1 User-Initiated Sharing
• You may choose to share patient files via Android's share functionality
• Sharing is entirely at your discretion and control
• We are not responsible for data once shared externally

4.2 No Third-Party Access
• We do not share data with any third parties
• No analytics, advertising, or tracking services are integrated
• No user behavior data is collected

5. DATA RETENTION

5.1 Active Data
• Patient records and files remain until you delete them
• You have full control over all data lifecycle

5.2 Recycle Bin
• Deleted items move to recycle bin for 7 days (configurable)
• After retention period, items are permanently deleted
• Physical files are removed from storage upon permanent deletion

5.3 Logs
• Developer mode logs are automatically deleted after 2 days
• You can manually clear logs at any time

6. YOUR RIGHTS

You have the right to:
• Access all data stored in the app
• Modify or update any patient information
• Delete any or all data at any time
• Export patient data for backup purposes
• Clear all app data via Android Settings

7. CHILDREN'S PRIVACY

This app is intended for use by healthcare professionals. It is not designed for use by children under 18 years of age.

8. MEDICAL DISCLAIMER

• MedDocs is a record-keeping tool only
• It does not provide medical advice, diagnosis, or treatment
• Always consult qualified healthcare professionals for medical decisions
• We are not responsible for clinical decisions made using this app

9. PERMISSIONS

The app may request the following permissions:
• Camera: To capture photos directly in the app
• Microphone: To record audio notes
• Storage: To access shared files from other apps

All permissions are used solely for the stated purposes and data remains local.

10. CHANGES TO THIS POLICY

We may update this Privacy Policy periodically. Changes will be reflected in the "Last Updated" date. Continued use of the app after changes constitutes acceptance.

11. CONTACT

For questions about this Privacy Policy or data practices, please contact the app developer through the appropriate channels.

12. COMPLIANCE

This app is designed with privacy-by-design principles:
• Data minimization: Only necessary data is collected
• Purpose limitation: Data used only for stated purposes
• Storage limitation: Clear retention and deletion policies
• User control: Full data ownership and control

---

By using MedDocs, you acknowledge that you have read and understood this Privacy Policy and agree to its terms.
        """.trimIndent()
    }
}

