package com.example.flowmind.di

import android.content.Context
import com.example.flowmind.worker.WorkflowScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkflowScheduler(@ApplicationContext ctx: Context): WorkflowScheduler =
        WorkflowScheduler(ctx)
}
