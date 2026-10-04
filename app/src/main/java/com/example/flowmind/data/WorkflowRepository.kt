package com.example.flowmind.data

import com.example.flowmind.data.db.WorkflowDao
import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowRepository @Inject constructor(private val dao: WorkflowDao) {
    fun allWorkflows(): Flow<List<Workflow>> = dao.getAllWorkflows()
    suspend fun save(workflow: Workflow) = dao.insertWorkflow(workflow)
    suspend fun delete(workflow: Workflow) = dao.deleteWorkflow(workflow)
}
