package com.example.flowmind.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flowmind.domain.models.ModelCandidate
import com.example.flowmind.domain.models.RunRecord
import com.example.flowmind.domain.models.Workflow

@Database(
    entities = [Workflow::class, ModelCandidate::class, RunRecord::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun modelDao(): ModelDao
    abstract fun runRecordDao(): RunRecordDao

    companion object {
        /**
         * Adds the `isArchived` column introduced in v3.
         *
         * Default value `0` (false) keeps all existing rows visible in the
         * Dashboard — no data is lost on upgrade.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE workflows ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
