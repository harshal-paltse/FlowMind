package com.example.flowmind.di

import android.content.Context
import androidx.room.Room
import com.example.flowmind.data.db.AppDatabase
import com.example.flowmind.data.db.ModelDao
import com.example.flowmind.data.db.RunRecordDao
import com.example.flowmind.data.db.WorkflowDao
import com.example.flowmind.data.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager =
        SecurityManager(context)

    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): AppDatabase {
        val passphrase = securityManager.getDatabasePassphrase()
        val supportFactory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, "flowmind_db")
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigrationFrom(1)
            .build()
    }

    @Provides @Singleton
    fun provideWorkflowDao(db: AppDatabase): WorkflowDao = db.workflowDao()

    @Provides @Singleton
    fun provideModelDao(db: AppDatabase): ModelDao = db.modelDao()

    @Provides @Singleton
    fun provideRunRecordDao(db: AppDatabase): RunRecordDao = db.runRecordDao()
}

