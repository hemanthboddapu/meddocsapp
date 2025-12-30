package com.example.meddocsapp

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ImageViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val imageView: ImageView = findViewById(R.id.image_view)
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString != null) {
            imageView.setImageURI(Uri.parse(imageUriString))
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "com.example.meddocsapp.EXTRA_IMAGE_URI"
    }
}