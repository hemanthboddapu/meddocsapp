package com.example.meddocsapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for the MedDocs app.
 *
 * Entities:
 * - Patient: Patient records with personal/medical info
 * - PatientFile: Files attached to patients
 * - RecycleBinItem: Deleted items awaiting permanent deletion
 *
 * Version History:
 * - v1: Initial schema with patients table
 * - v2: Added patient_files table
 * - v3: Added gender, dob, problem to patients
 * - v4: Added fileName, size, createdAt to patient_files
 * - v5: Added patientIdNumber to patients
 * - v6: Added admissionDate, dischargeDate, createdAt to patients
 * - v7: Added recycle_bin table
 */
@Database(entities = [Patient::class, PatientFile::class, RecycleBinItem::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun patientFileDao(): PatientFileDao
    abstract fun recycleBinDao(): RecycleBinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meddocs_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `patient_files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `uri` TEXT NOT NULL, `mimeType` TEXT NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE patients ADD COLUMN gender TEXT")
                database.execSQL("ALTER TABLE patients ADD COLUMN dob TEXT")
                database.execSQL("ALTER TABLE patients ADD COLUMN problem TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE patient_files ADD COLUMN fileName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE patient_files ADD COLUMN size INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE patient_files ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE patients ADD COLUMN patientIdNumber TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE patients ADD COLUMN admissionDate INTEGER")
                database.execSQL("ALTER TABLE patients ADD COLUMN dischargeDate INTEGER")
                database.execSQL("ALTER TABLE patients ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recycle_bin` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `originalId` INTEGER NOT NULL,
                        `itemData` TEXT NOT NULL,
                        `patientName` TEXT NOT NULL DEFAULT '',
                        `fileName` TEXT NOT NULL DEFAULT '',
                        `deletedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}