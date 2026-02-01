package com.example.meddocsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for RecycleBinItem entity.
 * Provides database operations for recycle bin items.
 */
@Dao
interface RecycleBinDao {

    /** Insert a new recycle bin item */
    @Insert
    suspend fun insert(item: RecycleBinItem)

    /** Delete a recycle bin item (permanent deletion) */
    @Delete
    suspend fun delete(item: RecycleBinItem)

    /** Get all recycle bin items ordered by deletion date (newest first) */
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllItems(): Flow<List<RecycleBinItem>>

    /** Get expired items that should be permanently deleted */
    @Query("SELECT * FROM recycle_bin WHERE expiresAt < :currentTime")
    suspend fun getExpiredItems(currentTime: Long): List<RecycleBinItem>

    /** Delete all expired items */
    @Query("DELETE FROM recycle_bin WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredItems(currentTime: Long)

    /** Delete all items in recycle bin */
    @Query("DELETE FROM recycle_bin")
    suspend fun deleteAll()

    /** Get count of items in recycle bin */
    @Query("SELECT COUNT(*) FROM recycle_bin")
    fun getItemCount(): Flow<Int>

    /** Get recycle bin item by original ID and type */
    @Query("SELECT * FROM recycle_bin WHERE originalId = :originalId AND itemType = :itemType LIMIT 1")
    suspend fun getItemByOriginalId(originalId: Long, itemType: String): RecycleBinItem?
}

