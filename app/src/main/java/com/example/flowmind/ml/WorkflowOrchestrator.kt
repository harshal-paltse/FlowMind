package com.example.flowmind.ml

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowOrchestrator @Inject constructor(
    private val modelSelector: ModelSelector
) {
    private val loadedModels = mutableMapOf<String, Any>()
    private val MAX_MODELS_LOADED = 2

    fun loadModel(modelId: String, modelInstance: Any) {
        if (loadedModels.size >= MAX_MODELS_LOADED) {
            val firstKey = loadedModels.keys.first()
            unloadModel(firstKey)
        }
        loadedModels[modelId] = modelInstance
    }

    private fun unloadModel(modelId: String) {
        val model = loadedModels.remove(modelId)
        (model as? java.lang.AutoCloseable)?.close()
    }
    
    fun executeStep(taskType: String, inputData: Any): Any {
        return "Step Executed"
    }
}
