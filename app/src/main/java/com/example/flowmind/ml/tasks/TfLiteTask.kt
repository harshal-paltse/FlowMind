package com.example.flowmind.ml.tasks

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TfLiteTask @Inject constructor(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    fun loadModel(modelFile: File, useGpu: Boolean = false) {
        val options = Interpreter.Options()
        
        if (useGpu && CompatibilityList().isDelegateSupportedOnThisDevice) {
            gpuDelegate = GpuDelegate(CompatibilityList().bestOptionsForThisDevice)
            options.addDelegate(gpuDelegate)
        } else {
            options.setNumThreads(4)
        }

        interpreter = Interpreter(modelFile, options)
    }

    fun runInference(inputData: Any, outputData: Any) {
        interpreter?.run(inputData, outputData)
    }

    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
    }
}
