package com.example.meddocsapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var patientNameTextView: TextView
    private lateinit var patientDetailsTextView: TextView
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var addFileFab: FloatingActionButton
    private lateinit var fileAdapter: FileAdapter
    private var patient: Patient? = null
    private var currentPhotoUri: Uri? = null

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((application as MedDocsApplication).repository)
    }

    private val addFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    copyFileToInternalStorage(clipData.getItemAt(i).uri)
                }
            } ?: result.data?.data?.also { uri ->
                copyFileToInternalStorage(uri)
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                copyFileToInternalStorage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)

        toolbar = findViewById(R.id.toolbar)
        patientNameTextView = findViewById(R.id.patient_name_text_view)
        patientDetailsTextView = findViewById(R.id.patient_details_text_view)
        filesRecyclerView = findViewById(R.id.files_recycler_view)
        addFileFab = findViewById(R.id.add_file_fab)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        patient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PATIENT, Patient::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_PATIENT)
        }

        if (patient == null) {
            finish()
            return
        }

        patientNameTextView.text = patient!!.name
        var details = "Bed: ${patient!!.bedNumber} - ${patient!!.status}"
        patient!!.gender?.let { details += "\nGender: $it" }
        patient!!.dob?.let { details += "\nDOB: $it" }
        patient!!.problem?.let { details += "\nProblem: $it" }
        patientDetailsTextView.text = details

        fileAdapter = FileAdapter(
            onFileClicked = { patientFile ->
                val fileUri = Uri.parse(patientFile.uri)
                if (patientFile.mimeType.startsWith("image/")) {
                    val intent = Intent(this, ImageViewActivity::class.java)
                    intent.putExtra(ImageViewActivity.EXTRA_IMAGE_URI, fileUri.toString())
                    startActivity(intent)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, patientFile.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Open file"))
                }
            },
            onDeleteClicked = { patientFile ->
                AlertDialog.Builder(this)
                    .setTitle("Delete File")
                    .setMessage("Are you sure you want to delete this file?")
                    .setPositiveButton("Delete") { _, _ ->
                        patientViewModel.delete(patientFile)
                        val file = File(Uri.parse(patientFile.uri).path!!)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        filesRecyclerView.adapter = fileAdapter
        filesRecyclerView.layoutManager = LinearLayoutManager(this)

        patientViewModel.getFilesForPatient(patient!!.id).observe(this) { files ->
            files?.let { fileAdapter.submitList(it) }
        }

        addFileFab.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            addFileLauncher.launch(intent)
        }
    }

    private fun copyFileToInternalStorage(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        if (patient != null && mimeType != null) {
            val cursor = contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
            cursor?.moveToFirst()
            val fileName = nameIndex?.let { cursor.getString(it) } ?: "shared_file"
            val size = sizeIndex?.let { cursor.getLong(it) } ?: 0
            cursor?.close()

            val file = File(filesDir, fileName)
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val patientFile = PatientFile(
                    patientId = patient!!.id,
                    uri = Uri.fromFile(file).toString(),
                    mimeType = mimeType,
                    fileName = fileName,
                    size = size,
                    createdAt = System.currentTimeMillis()
                )
                patientViewModel.insertFile(patientFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.patient_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_take_picture -> {
                dispatchTakePictureIntent()
                true
            }
            R.id.action_export -> {
                exportPatientData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.meddocsapp.provider",
                        it
                    )
                    currentPhotoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun exportPatientData() {
        patient?.let { patient ->
            patientViewModel.getFilesForPatient(patient.id).observe(this) { files ->
                if (files.isNullOrEmpty()) {
                    Toast.makeText(this, "No files to export", Toast.LENGTH_SHORT).show()
                    return@observe
                }

                val zipFile = File(getExternalFilesDir(null), "${patient.name}_${patient.id}.zip")
                try {
                    ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
                        // Add patient details to a text file
                        zipOutputStream.putNextEntry(ZipEntry("patient_details.txt"))
                        zipOutputStream.write("Name: ${patient.name}\n".toByteArray())
                        zipOutputStream.write("Bed Number: ${patient.bedNumber}\n".toByteArray())
                        zipOutputStream.write("Status: ${patient.status}\n".toByteArray())
                        patient.gender?.let { zipOutputStream.write("Gender: $it\n".toByteArray()) }
                        patient.dob?.let { zipOutputStream.write("DOB: $it\n".toByteArray()) }
                        patient.problem?.let { zipOutputStream.write("Problem: $it\n".toByteArray()) }
                        zipOutputStream.closeEntry()

                        // Add files
                        files.forEach { patientFile ->
                            val uri = Uri.parse(patientFile.uri)
                            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "unknown_file"
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                zipOutputStream.putNextEntry(ZipEntry(fileName))
                                inputStream.copyTo(zipOutputStream)
                                zipOutputStream.closeEntry()
                            }
                        }
                    }

                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@PatientDetailActivity, "com.example.meddocsapp.provider", zipFile))
                        type = "application/zip"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Export Patient Data"))

                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error exporting data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private class FileAdapter(
        private val onFileClicked: (PatientFile) -> Unit,
        private val onDeleteClicked: (PatientFile) -> Unit
    ) :
        ListAdapter<PatientFile, FileAdapter.FileViewHolder>(FilesDiffCallback()) {

        class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val fileNameTextView: TextView = itemView.findViewById(R.id.file_name_text_view)
            private val fileDetailsTextView: TextView = itemView.findViewById(R.id.file_details_text_view)
            private val fileThumbnail: ImageView = itemView.findViewById(R.id.file_thumbnail)
            private val deleteFileButton: ImageButton = itemView.findViewById(R.id.delete_file_button)

            fun bind(file: PatientFile, onFileClicked: (PatientFile) -> Unit, onDeleteClicked: (PatientFile) -> Unit) {
                fileNameTextView.text = file.fileName
                val formattedDate = SimpleDateFormat.getDateInstance().format(Date(file.createdAt))
                fileDetailsTextView.text = "${android.text.format.Formatter.formatShortFileSize(itemView.context, file.size)} - $formattedDate"
                if (file.mimeType.startsWith("image/")) {
                    Glide.with(itemView.context)
                        .load(Uri.parse(file.uri))
                        .into(fileThumbnail)
                } else {
                    // Set a generic file icon for non-image files
                    fileThumbnail.setImageResource(R.drawable.ic_file)
                }
                itemView.setOnClickListener { onFileClicked(file) }
                deleteFileButton.setOnClickListener { onDeleteClicked(file) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.file_list_item, parent, false)
            return FileViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            holder.bind(getItem(position), onFileClicked, onDeleteClicked)
        }

        private class FilesDiffCallback : DiffUtil.ItemCallback<PatientFile>() {
            override fun areItemsTheSame(oldItem: PatientFile, newItem: PatientFile): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PatientFile, newItem: PatientFile): Boolean {
                return oldItem == newItem
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT = "com.example.meddocsapp.EXTRA_PATIENT"
    }
}