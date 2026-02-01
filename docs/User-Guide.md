# MedDocs User Guide

A complete guide to using the MedDocs app for managing patient records and medical files.

---

## Table of Contents
- [Getting Started](#getting-started)
- [Dashboard](#dashboard)
- [Managing Patients](#managing-patients)
- [Managing Files](#managing-files)
- [Image Comparison](#image-comparison)
- [Sharing Files](#sharing-files)
- [Recycle Bin](#recycle-bin)
- [Search & Filter](#search--filter)
- [Developer Mode](#developer-mode)
- [Tips & Tricks](#tips--tricks)
- [Troubleshooting](#troubleshooting)

---

## Getting Started

### First Launch
When you first open MedDocs, you'll see the Dashboard with empty statistics. The app is ready to use immediately with no setup required.

### Navigation
The app uses bottom navigation with two main sections:
- **Dashboard**: Overview and statistics
- **Patients**: Patient list and management

---

## Dashboard

### Overview
The Dashboard shows:
- **Total Patients**: All patients in the system
- **Active**: Currently admitted patients
- **Discharged**: Patients who have been discharged
- **Total Files**: All files attached to patients

### Recent Patients
Below the statistics, you'll see recently added patients. Tap any patient to view their details.

### Menu Options
Tap the menu icon (⋮) to access:
- **Developer Settings**: Enable debugging mode
- **Recycle Bin**: View and restore deleted items
- **Help**: Get usage tips

---

## Managing Patients

### Adding a New Patient

1. Go to **Patients** tab
2. Tap the **+** button (floating action button)
3. Fill in the patient information:
   - **Name** (required): Patient's full name
   - **Bed Number** (required): Room/bed identifier
   - **Patient ID**: Hospital/clinic ID number
   - **Gender**: Male or Female
   - **Date of Birth**: For avatar selection
   - **Problem**: Medical condition/diagnosis
   - **Admission Date**: When patient was admitted
4. Tap **Save**

### Editing a Patient

1. Tap on a patient in the list
2. Tap the **Edit** button (pencil icon)
3. Modify the information
4. Tap **Save**

### Quick Discharge

To quickly discharge a patient:
1. In the patient list, tap the **discharge icon** (exit icon) on the patient card
2. Optionally select a discharge date
3. Confirm the discharge

The patient status changes to "Discharged" and they move lower in the list.

### Deleting a Patient

1. Open the patient's detail page
2. Tap the **Delete** button
3. Confirm deletion

**Note**: Deleted patients go to the Recycle Bin and can be restored within 7 days.

### Patient Avatars

Avatars are automatically selected based on:
- **Gender**: Male or Female
- **Age** (from date of birth):
  - Infant: 0-2 years
  - Child: 3-12 years
  - Teen: 13-19 years
  - Adult: 20-59 years
  - Senior: 60+ years

---

## Managing Files

### Adding Files to a Patient

1. Open a patient's detail page
2. Tap the **Add File** button (+)
3. Select file source:
   - **Camera**: Take a new photo
   - **Gallery**: Select from device photos
   - **Files**: Select documents (PDF, Word, etc.)
4. The file is added to the patient's folder

### Viewing Files

- **Images**: Tap to view full screen with zoom
- **PDFs**: Tap to open in PDF viewer app
- **Videos**: Tap to open in video player app
- **Documents**: Tap to open in appropriate app

**Note**: You need compatible apps installed for non-image files.

### File Type Icons

Files display icons based on type:
- 🖼️ Images: Thumbnail preview
- 📄 PDF: Red PDF icon
- 🎬 Video: Red video icon
- 📝 Word: Blue document icon
- 📊 PowerPoint: Orange presentation icon

### Deleting Files

1. In the patient's file list, tap the delete icon on a file
2. Confirm deletion

Files go to the Recycle Bin and can be restored within 7 days.

### Sharing Files

1. Tap the **share icon** on any file
2. Select the app to share with (WhatsApp, Email, etc.)
3. Patient information is included with the share

---

## Image Comparison

Compare two medical images side by side:

### Starting a Comparison

1. Open a patient with multiple images
2. Tap **Compare Images** from the menu
3. Select the first image
4. Select the second image

### Comparison Controls

- **Rotate Left/Right**: Rotate individual images in 90° increments
- **Swap**: Switch left and right images
- **Landscape**: Toggle landscape orientation for wider view
- **Save**: Save the comparison as a new image

### Saving Comparisons

When you save a comparison:
- A new image is created combining both images
- The image is saved to the patient's folder
- You can share or view it like any other file

---

## Sharing Files

### Receiving Files from Other Apps

When you share a file (image, PDF, etc.) from another app to MedDocs:

1. The Share screen opens
2. **Search** for an existing patient, or
3. **Add New Patient** using the + button
4. The file is attached to the selected patient

### Sharing Files Out

1. Open a patient's detail page
2. Tap the **share icon** on any file
3. Choose the destination app
4. The file is shared with patient context:
   - Patient Name
   - Bed Number
   - Patient ID (if set)

---

## Recycle Bin

### Accessing the Recycle Bin

1. Go to Dashboard
2. Tap the menu (⋮)
3. Select **Recycle Bin**

### Viewing Deleted Items

The recycle bin shows:
- Item type (Patient or File)
- Item name
- Time until permanent deletion

### Restoring Items

1. Find the item to restore
2. Tap the **Restore** button
3. The item returns to its original location

### Permanent Deletion

Items are automatically deleted after **7 days**.

To delete immediately:
1. Tap the **Delete** button on an item
2. Confirm permanent deletion

To empty entire recycle bin:
1. Tap the menu (⋮)
2. Select **Empty Recycle Bin**
3. Confirm

⚠️ **Warning**: Permanent deletion cannot be undone!

---

## Search & Filter

### Searching Patients

1. Tap the **search icon** in the Patients view
2. Type your search query
3. Results update as you type

Search matches:
- Patient name
- Bed number
- Patient ID number

### Filtering by Status

1. Tap the **filter icon** in the toolbar
2. Select a filter:
   - **All**: Show all patients
   - **Active Only**: Show only active patients
   - **Discharged Only**: Show only discharged patients

### Sorting

Patients are always sorted with:
- Active patients first
- Then alphabetically by name

---

## Developer Mode

For troubleshooting and debugging:

### Enabling Developer Mode

1. Go to Dashboard → Menu → **Developer Settings**
2. Toggle **Enable Developer Mode** ON

### What Developer Mode Does

- Saves detailed logs to files
- Logs are kept for 2 days
- Helps diagnose issues

### Viewing Logs

1. In Developer Settings, tap **View Logs**
2. Select a log file by date
3. Scroll through log entries

### Exporting Logs

1. Tap **Export Logs**
2. Select date range
3. Share or save the log file

### Clearing Logs

Tap **Clear Logs** to delete all log files.

---

## Tips & Tricks

### Quick Actions

- **Long press** on a patient for quick options
- **Swipe** on files for quick delete (if enabled)
- **Double tap** images to zoom

### Efficient Workflows

1. **Batch file uploads**: Add multiple files in one session
2. **Quick discharge**: Use the discharge button instead of editing
3. **Search shortcuts**: Search by bed number for fast lookup

### Storage Management

- Regularly review the Recycle Bin
- Delete old discharged patient data
- Use the file count to monitor storage

### Best Practices

1. **Use Patient IDs**: Helps with hospital system integration
2. **Add admission dates**: Track length of stay
3. **Use descriptive filenames**: Easier to find later
4. **Regular backups**: Export important data

---

## Troubleshooting

### Common Issues

#### App crashes when opening files
- Ensure you have appropriate viewer apps installed
- Try a different file of the same type
- Enable Developer Mode and check logs

#### Files not showing
- Refresh the patient detail page
- Check if files are in Recycle Bin
- Verify file was successfully added

#### Search not finding patients
- Check spelling
- Try searching by bed number or patient ID
- Clear filter if one is active

#### Avatar not displaying correctly
- Ensure gender is set
- Add date of birth for age-based avatar
- Check if drawable resources exist

### Getting Help

1. Enable **Developer Mode**
2. Reproduce the issue
3. **Export Logs**
4. Share logs with support

### Resetting the App

To start fresh (⚠️ deletes all data):
1. Go to Android Settings → Apps → MedDocs
2. Tap **Clear Data**
3. Confirm

---

## Keyboard Shortcuts

When using with external keyboard:

| Action | Shortcut |
|--------|----------|
| Search | Ctrl + F |
| Save | Ctrl + S |
| Back | Escape |

---

## Accessibility

MedDocs supports:
- **TalkBack**: Screen reader compatibility
- **Large text**: Respects system font size
- **High contrast**: Works with high contrast mode

---

## Feedback

We welcome your feedback to improve MedDocs!

- Report bugs with Developer Mode logs
- Suggest features via issue tracker
- Share your workflow improvements

Thank you for using MedDocs! 🏥

