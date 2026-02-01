package com.example.meddocsapp

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * RecycleBinItem entity representing a deleted item in the recycle bin.
 *
 * Items are automatically permanently deleted after the retention period.
 *
 * @property id Auto-generated primary key
 * @property itemType Type of item: "patient" or "file"
 * @property originalId Original ID of the item
 * @property itemData JSON serialized data of the original item
 * @property deletedAt Timestamp when the item was deleted (milliseconds)
 * @property expiresAt Timestamp when the item will be permanently deleted (milliseconds)
 */
@Parcelize
@Entity(tableName = "recycle_bin")
data class RecycleBinItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String,  // "patient" or "file"
    val originalId: Long,
    val itemData: String,  // JSON serialized data
    val patientName: String = "",  // For display purposes
    val fileName: String = "",     // For display purposes
    val deletedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)  // 7 days default
) : Parcelable

