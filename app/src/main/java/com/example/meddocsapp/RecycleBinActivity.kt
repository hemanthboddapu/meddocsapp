package com.example.meddocsapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Activity for managing items in the recycle bin.
 * Users can restore or permanently delete items from here.
 */
class RecycleBinActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var adapter: RecycleBinAdapter

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((application as MedDocsApplication).repository)
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycle_bin)

        initViews()
        setupToolbar()
        setupRecyclerView()
        observeData()

        // Clean up expired items on launch
        patientViewModel.cleanupExpiredRecycleBinItems()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recycle_bin_recycler_view)
        emptyStateLayout = findViewById(R.id.empty_state_layout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recycle Bin"
    }

    private fun setupRecyclerView() {
        adapter = RecycleBinAdapter(
            onRestoreClicked = { item ->
                AlertDialog.Builder(this)
                    .setTitle("Restore Item")
                    .setMessage("Restore this ${item.itemType}?")
                    .setPositiveButton("Restore") { _, _ ->
                        patientViewModel.restoreFromRecycleBin(item)
                        Toast.makeText(this, "Item restored", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onDeleteClicked = { item ->
                AlertDialog.Builder(this)
                    .setTitle("Permanent Delete")
                    .setMessage("This will permanently delete the item. This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        patientViewModel.permanentlyDelete(item)
                        Toast.makeText(this, "Item permanently deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        patientViewModel.recycleBinItems?.observe(this) { items ->
            items?.let {
                adapter.submitList(it)
                updateEmptyState(it.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.recycle_bin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_empty_recycle_bin -> {
                showEmptyRecycleBinDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEmptyRecycleBinDialog() {
        AlertDialog.Builder(this)
            .setTitle("Empty Recycle Bin")
            .setMessage("This will permanently delete all items in the recycle bin. This cannot be undone.")
            .setPositiveButton("Empty") { _, _ ->
                patientViewModel.clearRecycleBin()
                Toast.makeText(this, "Recycle bin emptied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Adapter for recycle bin items.
     */
    private inner class RecycleBinAdapter(
        private val onRestoreClicked: (RecycleBinItem) -> Unit,
        private val onDeleteClicked: (RecycleBinItem) -> Unit
    ) : ListAdapter<RecycleBinItem, RecycleBinAdapter.ViewHolder>(
        // Inline DiffUtil callback to avoid nested class compilation issues
        object : DiffUtil.ItemCallback<RecycleBinItem>() {
            override fun areItemsTheSame(oldItem: RecycleBinItem, newItem: RecycleBinItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: RecycleBinItem, newItem: RecycleBinItem) = oldItem == newItem
        }
    ) {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.item_title)
            private val typeText: TextView = itemView.findViewById(R.id.item_type)
            private val expiresText: TextView = itemView.findViewById(R.id.item_expires)
            private val restoreButton: View = itemView.findViewById(R.id.restore_button)
            private val deleteButton: View = itemView.findViewById(R.id.delete_button)

            fun bind(item: RecycleBinItem) {
                // Set title based on item type
                titleText.text = if (item.itemType == "patient") {
                    item.patientName.ifEmpty { "Unknown Patient" }
                } else {
                    item.fileName.ifEmpty { "Unknown File" }
                }

                typeText.text = if (item.itemType == "patient") "Patient" else "File"

                // Calculate days until expiration
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(item.expiresAt - System.currentTimeMillis())
                expiresText.text = when {
                    daysRemaining > 1 -> "Expires in $daysRemaining days"
                    daysRemaining == 1L -> "Expires tomorrow"
                    daysRemaining == 0L -> "Expires today"
                    else -> "Expired"
                }

                restoreButton.setOnClickListener { onRestoreClicked(item) }
                deleteButton.setOnClickListener { onDeleteClicked(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_recycle_bin, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }
}
