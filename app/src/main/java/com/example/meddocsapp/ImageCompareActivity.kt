package com.example.meddocsapp

import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for comparing two patient images side-by-side.
 * Features: Image rotation, landscape mode toggle, save comparison.
 */
class ImageCompareActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATIENT = "com.example.meddocsapp.EXTRA_PATIENT"
        const val EXTRA_LEFT_IMAGE_URI = "com.example.meddocsapp.EXTRA_LEFT_IMAGE_URI"
        const val EXTRA_RIGHT_IMAGE_URI = "com.example.meddocsapp.EXTRA_RIGHT_IMAGE_URI"
        private const val TAG = "ImageCompareActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var leftImageView: PhotoView
    private lateinit var rightImageView: PhotoView
    private lateinit var leftImageLabel: TextView
    private lateinit var rightImageLabel: TextView
    private lateinit var leftImageCard: MaterialCardView
    private lateinit var rightImageCard: MaterialCardView
    private lateinit var instructionsLayout: View
    private lateinit var comparisonContainer: View
    private lateinit var expandedTools: View
    private lateinit var toggleToolsFab: View

    // FAB buttons
    private lateinit var selectLeftButton: View
    private lateinit var selectRightButton: View
    private lateinit var swapButton: View
    private lateinit var rotateLeftButton: View
    private lateinit var rotateRightButton: View
    private lateinit var landscapeButton: View
    private lateinit var saveComparisonButton: View
    private lateinit var quickSaveButton: View
    private var dismissInstructionsButton: View? = null

    private var patient: Patient? = null
    private var leftImage: PatientFile? = null
    private var rightImage: PatientFile? = null
    private var imageFiles: List<PatientFile> = emptyList()

    private var leftRotation = 0f
    private var rightRotation = 0f
    private var isLandscape = false
    private var toolsExpanded = false

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((application as MedDocsApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_compare)

        AppLogger.d(TAG, "onCreate")

        initViews()
        setupToolbar()
        loadPatientData()
        setupClickListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        leftImageView = findViewById(R.id.left_image_view)
        rightImageView = findViewById(R.id.right_image_view)
        leftImageLabel = findViewById(R.id.left_image_label)
        rightImageLabel = findViewById(R.id.right_image_label)
        leftImageCard = findViewById(R.id.left_image_card)
        rightImageCard = findViewById(R.id.right_image_card)
        instructionsLayout = findViewById(R.id.instructions_layout)
        comparisonContainer = findViewById(R.id.comparison_container)

        // FAB controls
        expandedTools = findViewById(R.id.expanded_tools)
        toggleToolsFab = findViewById(R.id.toggle_tools_fab)
        selectLeftButton = findViewById(R.id.select_left_button)
        selectRightButton = findViewById(R.id.select_right_button)
        swapButton = findViewById(R.id.swap_button)
        rotateLeftButton = findViewById(R.id.rotate_left_button)
        rotateRightButton = findViewById(R.id.rotate_right_button)
        landscapeButton = findViewById(R.id.landscape_button)
        saveComparisonButton = findViewById(R.id.save_comparison_button)
        dismissInstructionsButton = findViewById(R.id.dismiss_instructions_button)

        // Quick save button
        quickSaveButton = findViewById(R.id.quick_save_button)
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
            AppLogger.d(TAG, "Loading images for patient: ${p.name}")

            patientViewModel.getImageFilesForPatient(p.id).observe(this) { files ->
                imageFiles = files ?: emptyList()
                AppLogger.d(TAG, "Found ${imageFiles.size} image files")
                if (imageFiles.size < 2) {
                    Toast.makeText(this, "Need at least 2 images to compare", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            AppLogger.e(TAG, "No patient data received")
            Toast.makeText(this, "Error: No patient data", Toast.LENGTH_SHORT).show()
            finish()
            return
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

        rotateLeftButton.setOnClickListener { rotateLeftImage() }
        rotateRightButton.setOnClickListener { rotateRightImage() }
        landscapeButton.setOnClickListener { toggleLandscape() }
        saveComparisonButton.setOnClickListener { saveComparison() }
        quickSaveButton.setOnClickListener { saveComparison() }

        // Toggle FAB for expanding/collapsing tools
        toggleToolsFab.setOnClickListener { toggleTools() }

        // Dismiss instructions button
        dismissInstructionsButton?.setOnClickListener {
            instructionsLayout.visibility = View.GONE
        }
    }

    private fun updateQuickSaveButtonVisibility() {
        quickSaveButton.visibility = if (leftImage != null && rightImage != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun toggleTools() {
        toolsExpanded = !toolsExpanded
        expandedTools.visibility = if (toolsExpanded) View.VISIBLE else View.GONE
    }

    private fun rotateLeftImage() {
        leftRotation = (leftRotation + 90f) % 360f
        leftImageView.rotation = leftRotation
    }

    private fun rotateRightImage() {
        rightRotation = (rightRotation + 90f) % 360f
        rightImageView.rotation = rightRotation
    }

    private fun toggleLandscape() {
        isLandscape = !isLandscape
        requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        // Close the expanded tools when switching orientation
        toolsExpanded = false
        expandedTools.visibility = View.GONE
    }

    private fun saveComparison() {
        if (leftImage == null || rightImage == null) {
            Toast.makeText(this, "Select both images first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Use a scale factor for higher resolution output
            val scaleFactor = 2.0f

            // Get the comparison container dimensions
            val containerWidth = comparisonContainer.width
            val containerHeight = comparisonContainer.height

            if (containerWidth <= 0 || containerHeight <= 0) {
                Toast.makeText(this, "Please wait for images to load", Toast.LENGTH_SHORT).show()
                return
            }

            // Create a high-resolution bitmap
            val outputWidth = (containerWidth * scaleFactor).toInt()
            val outputHeight = (containerHeight * scaleFactor).toInt()

            val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Scale the canvas to render at higher resolution
            canvas.scale(scaleFactor, scaleFactor)

            // Draw the comparison container (captures zoom/pan state)
            comparisonContainer.draw(canvas)

            // Save to patient's folder
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "comparison_${timestamp}.jpg"

            val file = File(filesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()

            // Add to patient's files
            val patientFile = PatientFile(
                patientId = patient!!.id,
                uri = Uri.fromFile(file).toString(),
                mimeType = "image/jpeg",
                fileName = fileName,
                size = file.length(),
                createdAt = System.currentTimeMillis()
            )
            patientViewModel.insertFile(patientFile)

            Toast.makeText(this, "Comparison saved", Toast.LENGTH_SHORT).show()
            AppLogger.d(TAG, "Comparison saved: $fileName, size: ${file.length() / 1024}KB")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error saving comparison", e)
            Toast.makeText(this, "Error saving comparison: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showImagePicker(isLeft: Boolean) {
        if (imageFiles.isEmpty()) {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_image_picker, null)

            val recyclerView = view.findViewById<RecyclerView>(R.id.images_recycler_view)
            val titleText = view.findViewById<TextView>(R.id.picker_title)
            titleText.text = if (isLeft) "Select Before Image" else "Select After Image"

            val adapter = ImagePickerAdapter { selectedFile ->
                try {
                    if (isLeft) {
                        leftImage = selectedFile
                        leftRotation = 0f
                        leftImageView.rotation = 0f
                        loadLeftImage(selectedFile.uri)
                        leftImageLabel.text = selectedFile.fileName
                    } else {
                        rightImage = selectedFile
                        rightRotation = 0f
                        rightImageView.rotation = 0f
                        loadRightImage(selectedFile.uri)
                        rightImageLabel.text = selectedFile.fileName
                    }
                    updateInstructionsVisibility()
                    updateQuickSaveButtonVisibility()
                    AppLogger.d(TAG, "Selected image: ${selectedFile.fileName}")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error loading selected image", e)
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            recyclerView.layoutManager = GridLayoutManager(this, 3)
            recyclerView.adapter = adapter
            adapter.submitList(imageFiles.toList())

            dialog.setContentView(view)
            dialog.show()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error showing image picker", e)
            Toast.makeText(this, "Error opening image picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLeftImage(uri: String) {
        try {
            Glide.with(this)
                .load(Uri.parse(uri))
                .error(R.drawable.ic_file)
                .into(leftImageView)
            instructionsLayout.visibility = View.GONE
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error loading left image", e)
        }
    }

    private fun loadRightImage(uri: String) {
        try {
            Glide.with(this)
                .load(Uri.parse(uri))
                .error(R.drawable.ic_file)
                .into(rightImageView)
            instructionsLayout.visibility = View.GONE
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error loading right image", e)
        }
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
        val tempRotation = leftRotation

        leftImage = rightImage
        rightImage = tempImage

        leftImageLabel.text = rightImageLabel.text
        rightImageLabel.text = tempLabel

        leftRotation = rightRotation
        rightRotation = tempRotation

        leftImageView.rotation = leftRotation
        rightImageView.rotation = rightRotation

        // Swap the actual images
        try {
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
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error swapping images", e)
        }
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
            R.id.action_canvas_size -> {
                showCanvasSizeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCanvasSizeDialog() {
        val options = arrayOf(
            "1:1 Square",
            "4:3 Standard",
            "16:9 Widescreen",
            "3:4 Portrait",
            "9:16 Mobile Portrait"
        )

        AlertDialog.Builder(this)
            .setTitle("Canvas Size")
            .setItems(options) { _, which ->
                applyCanvasSize(which)
            }
            .show()
    }

    private fun applyCanvasSize(sizeOption: Int) {
        val comparisonLayout = comparisonContainer as? android.widget.LinearLayout ?: return

        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels - 200 // Account for toolbar and bottom nav

        val (targetWidth, targetHeight) = when (sizeOption) {
            0 -> { // 1:1 Square
                val size = minOf(screenWidth, screenHeight)
                size to size
            }
            1 -> { // 4:3 Standard
                val height = (screenWidth * 3) / 4
                screenWidth to minOf(height, screenHeight)
            }
            2 -> { // 16:9 Widescreen
                val height = (screenWidth * 9) / 16
                screenWidth to minOf(height, screenHeight)
            }
            3 -> { // 3:4 Portrait
                val width = (screenHeight * 3) / 4
                minOf(width, screenWidth) to screenHeight
            }
            4 -> { // 9:16 Mobile Portrait
                val width = (screenHeight * 9) / 16
                minOf(width, screenWidth) to screenHeight
            }
            else -> screenWidth to screenHeight
        }

        comparisonLayout.layoutParams = comparisonLayout.layoutParams.apply {
            width = targetWidth
            height = targetHeight
        }
        comparisonLayout.requestLayout()

        Toast.makeText(this, "Canvas size applied", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFullscreen() {
        val fabContainer = findViewById<View>(R.id.fab_container)
        if (fabContainer.visibility == View.VISIBLE) {
            fabContainer.visibility = View.GONE
            findViewById<View>(R.id.app_bar_layout)?.visibility = View.GONE
        } else {
            fabContainer.visibility = View.VISIBLE
            findViewById<View>(R.id.app_bar_layout)?.visibility = View.VISIBLE
        }
    }

    /**
     * Adapter for image picker grid in bottom sheet dialog.
     */
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
            try {
                Glide.with(holder.itemView.context)
                    .load(Uri.parse(file.uri))
                    .centerCrop()
                    .error(R.drawable.ic_file)
                    .into(holder.imageView)
                holder.nameText.text = file.fileName
                holder.itemView.setOnClickListener { onImageSelected(file) }
            } catch (e: Exception) {
                holder.imageView.setImageResource(R.drawable.ic_file)
            }
        }

        private class ImageDiffCallback : DiffUtil.ItemCallback<PatientFile>() {
            override fun areItemsTheSame(oldItem: PatientFile, newItem: PatientFile) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PatientFile, newItem: PatientFile) = oldItem == newItem
        }
    }
}

