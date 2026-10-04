package com.example.flowmind.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flowmind.domain.models.ModelCandidate
import com.example.flowmind.domain.models.RunRecord
import com.example.flowmind.domain.models.Workflow

@Database(
    entities = [Workflow::class, ModelCandidate::class, RunRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun modelDao(): ModelDao
    abstract fun runRecordDao(): RunRecordDao
}
