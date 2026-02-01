package com.example.meddocsapp

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class ImageViewActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var zoomHintText: TextView
    private var isSystemUiVisible = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        // Enable edge-to-edge display
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Image Viewer"
        }

        photoView = findViewById(R.id.photo_view)
        zoomHintText = findViewById(R.id.zoom_hint_text)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString != null) {
            Glide.with(this)
                .load(Uri.parse(imageUriString))
                .into(photoView)
        }

        // Toggle system UI visibility on tap
        photoView.setOnPhotoTapListener { _, _, _ ->
            toggleSystemUi()
        }

        // Set zoom levels
        photoView.minimumScale = 1.0f
        photoView.mediumScale = 2.5f
        photoView.maximumScale = 5.0f

        // Auto-hide zoom hint after 3 seconds
        handler.postDelayed({
            fadeOutView(zoomHintText)
        }, 3000)
    }

    private fun fadeOutView(view: View) {
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 500
            fillAfter = true
        }
        view.startAnimation(fadeOut)
        handler.postDelayed({
            view.visibility = View.GONE
        }, 500)
    }

    private fun toggleSystemUi() {
        if (isSystemUiVisible) {
            hideSystemUi()
        } else {
            showSystemUi()
        }
        isSystemUiVisible = !isSystemUiVisible
    }

    private fun hideSystemUi() {
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
    }

    private fun showSystemUi() {
        supportActionBar?.show()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "com.example.meddocsapp.EXTRA_IMAGE_URI"
    }
}