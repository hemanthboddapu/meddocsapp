package com.example.meddocsapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Patient::class, PatientFile::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun patientFileDao(): PatientFileDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    }
}