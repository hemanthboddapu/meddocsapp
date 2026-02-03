package com.example.meddocsapp

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
    }

    private var progressOverlay: FrameLayout? = null
    private var progressText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard, R.id.navigation_patients
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun showProgress(message: String) {
        // Create progress overlay dynamically if not in layout
        if (progressOverlay == null) {
            progressOverlay = FrameLayout(this).apply {
                setBackgroundColor(0x80000000.toInt())
                isClickable = true
                isFocusable = true

                val container = android.widget.LinearLayout(this@HomeActivity).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(64, 64, 64, 64)
                    setBackgroundResource(R.drawable.dialog_background)
                }

                val progress = android.widget.ProgressBar(this@HomeActivity)
                container.addView(progress, android.widget.LinearLayout.LayoutParams(96, 96).apply {
                    bottomMargin = 32
                })

                progressText = TextView(this@HomeActivity).apply {
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                    textSize = 16f
                }
                container.addView(progressText)

                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                addView(container, params)
            }

            val rootView = findViewById<View>(android.R.id.content) as android.view.ViewGroup
            rootView.addView(progressOverlay, android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

        progressText?.text = message
        progressOverlay?.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progressOverlay?.visibility = View.GONE
    }
}