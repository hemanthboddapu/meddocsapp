package com.example.meddocsapp

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class ImageCompareActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var leftImageView: PhotoView
    private lateinit var rightImageView: PhotoView
    private lateinit var leftImageLabel: TextView
    private lateinit var rightImageLabel: TextView
    private lateinit var leftImageCard: MaterialCardView
    private lateinit var rightImageCard: MaterialCardView
    private lateinit var selectLeftButton: MaterialButton
    private lateinit var selectRightButton: MaterialButton
    private lateinit var swapButton: MaterialButton
    private lateinit var resetZoomButton: MaterialButton
    private lateinit var syncZoomSwitch: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var instructionsLayout: View

    private var patient: Patient? = null
    private var leftImage: PatientFile? = null
    private var rightImage: PatientFile? = null
    private var imageFiles: List<PatientFile> = emptyList()
    private var syncZoom = true

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((application as MedDocsApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_compare)

        initViews()
        setupToolbar()
        loadPatientData()
        setupClickListeners()
        setupZoomSync()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        leftImageView = findViewById(R.id.left_image_view)
        rightImageView = findViewById(R.id.right_image_view)
        leftImageLabel = findViewById(R.id.left_image_label)
        rightImageLabel = findViewById(R.id.right_image_label)
        leftImageCard = findViewById(R.id.left_image_card)
        rightImageCard = findViewById(R.id.right_image_card)
        selectLeftButton = findViewById(R.id.select_left_button)
        selectRightButton = findViewById(R.id.select_right_button)
        swapButton = findViewById(R.id.swap_button)
        resetZoomButton = findViewById(R.id.reset_zoom_button)
        syncZoomSwitch = findViewById(R.id.sync_zoom_switch)
        instructionsLayout = findViewById(R.id.instructions_layout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Compare Images"
    }

    private fun loadPatientData() {
        patient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PATIENT, Patient::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PATIENT)
        }

        patient?.let { p ->
            supportActionBar?.subtitle = p.name
            patientViewModel.getImageFilesForPatient(p.id).observe(this) { files ->
                imageFiles = files ?: emptyList()
                if (imageFiles.size < 2) {
                    Toast.makeText(this, "Need at least 2 images to compare", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Check if images were passed directly
        val leftUri = intent.getStringExtra(EXTRA_LEFT_IMAGE_URI)
        val rightUri = intent.getStringExtra(EXTRA_RIGHT_IMAGE_URI)

        if (leftUri != null) {
            loadLeftImage(leftUri)
        }
        if (rightUri != null) {
            loadRightImage(rightUri)
        }
    }

    private fun setupClickListeners() {
        selectLeftButton.setOnClickListener { showImagePicker(true) }
        selectRightButton.setOnClickListener { showImagePicker(false) }
        leftImageCard.setOnClickListener { if (leftImage == null) showImagePicker(true) }
        rightImageCard.setOnClickListener { if (rightImage == null) showImagePicker(false) }

        swapButton.setOnClickListener { swapImages() }
        resetZoomButton.setOnClickListener { resetZoom() }

        syncZoomSwitch.setOnCheckedChangeListener { _, isChecked ->
            syncZoom = isChecked
        }
    }

    private fun setupZoomSync() {
        leftImageView.setOnMatrixChangeListener { rect ->
            if (syncZoom && leftImage != null && rightImage != null) {
                rightImageView.setScale(leftImageView.scale, false)
            }
        }

        rightImageView.setOnMatrixChangeListener { rect ->
            if (syncZoom && leftImage != null && rightImage != null) {
                leftImageView.setScale(rightImageView.scale, false)
            }
        }
    }

    private fun showImagePicker(isLeft: Boolean) {
        if (imageFiles.isEmpty()) {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_image_picker, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.images_recycler_view)
        val titleText = view.findViewById<TextView>(R.id.picker_title)
        titleText.text = if (isLeft) "Select Before Image" else "Select After Image"

        val adapter = ImagePickerAdapter { selectedFile ->
            if (isLeft) {
                leftImage = selectedFile
                loadLeftImage(selectedFile.uri)
                leftImageLabel.text = selectedFile.fileName
            } else {
                rightImage = selectedFile
                loadRightImage(selectedFile.uri)
                rightImageLabel.text = selectedFile.fileName
            }
            updateInstructionsVisibility()
            dialog.dismiss()
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter
        adapter.submitList(imageFiles)

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loadLeftImage(uri: String) {
        Glide.with(this)
            .load(Uri.parse(uri))
            .into(leftImageView)
        instructionsLayout.visibility = View.GONE
    }

    private fun loadRightImage(uri: String) {
        Glide.with(this)
            .load(Uri.parse(uri))
            .into(rightImageView)
        instructionsLayout.visibility = View.GONE
    }

    private fun updateInstructionsVisibility() {
        instructionsLayout.visibility = if (leftImage == null && rightImage == null) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun swapImages() {
        val tempImage = leftImage
        val tempLabel = leftImageLabel.text

        leftImage = rightImage
        rightImage = tempImage

        leftImageLabel.text = rightImageLabel.text
        rightImageLabel.text = tempLabel

        // Swap the actual images
        if (leftImage != null) {
            Glide.with(this).load(Uri.parse(leftImage!!.uri)).into(leftImageView)
        } else {
            leftImageView.setImageResource(R.drawable.ic_add_photo)
        }

        if (rightImage != null) {
            Glide.with(this).load(Uri.parse(rightImage!!.uri)).into(rightImageView)
        } else {
            rightImageView.setImageResource(R.drawable.ic_add_photo)
        }
    }

    private fun resetZoom() {
        leftImageView.setScale(1f, true)
        rightImageView.setScale(1f, true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.compare_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_fullscreen -> {
                toggleFullscreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFullscreen() {
        val controlsLayout = findViewById<View>(R.id.controls_layout)
        if (controlsLayout.visibility == View.VISIBLE) {
            controlsLayout.visibility = View.GONE
            supportActionBar?.hide()
        } else {
            controlsLayout.visibility = View.VISIBLE
            supportActionBar?.show()
        }
    }

    // Image Picker Adapter
    private class ImagePickerAdapter(
        private val onImageSelected: (PatientFile) -> Unit
    ) : ListAdapter<PatientFile, ImagePickerAdapter.ImageViewHolder>(ImageDiffCallback()) {

        class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.picker_image)
            val nameText: TextView = itemView.findViewById(R.id.picker_image_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_picker, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val file = getItem(position)
            Glide.with(holder.itemView.context)
                .load(Uri.parse(file.uri))
                .centerCrop()
                .into(holder.imageView)
            holder.nameText.text = file.fileName
            holder.itemView.setOnClickListener { onImageSelected(file) }
        }

        private class ImageDiffCallback : DiffUtil.ItemCallback<PatientFile>() {
            override fun areItemsTheSame(oldItem: PatientFile, newItem: PatientFile) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PatientFile, newItem: PatientFile) = oldItem == newItem
        }
    }

    companion object {
        const val EXTRA_PATIENT = "com.example.meddocsapp.EXTRA_PATIENT"
        const val EXTRA_LEFT_IMAGE_URI = "com.example.meddocsapp.EXTRA_LEFT_IMAGE_URI"
        const val EXTRA_RIGHT_IMAGE_URI = "com.example.meddocsapp.EXTRA_RIGHT_IMAGE_URI"
    }
}

