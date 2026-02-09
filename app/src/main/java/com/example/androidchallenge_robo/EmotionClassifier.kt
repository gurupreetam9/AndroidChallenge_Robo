package com.example.androidchallenge_robo

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

class EmotionClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    private val modelName = "Face_Expression.tflite"

    private val inputSize = 48
    private val inputChannels = 1
    private val outputSize = 7

    // Reusable buffers (IMPORTANT for performance)
    private val inputBuffer =
        ByteBuffer.allocateDirect(1 * inputSize * inputSize * inputChannels * 4)
            .order(ByteOrder.nativeOrder())

    private val outputBuffer =
        ByteBuffer.allocateDirect(1 * outputSize * 4)
            .order(ByteOrder.nativeOrder())

    init {
        initialize()
    }

    private fun initialize() {
        try {
            val model = loadModelFile(modelName)

            val options = Interpreter.Options()
            options.setNumThreads(4)

            // GPU Delegate (optional)
            try {
                val compatList = CompatibilityList()

                if (compatList.isDelegateSupportedOnThisDevice) {
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    gpuDelegate = GpuDelegate(delegateOptions)
                    options.addDelegate(gpuDelegate)
                    Log.d("EmotionClassifier", "GPU enabled")
                } else {
                    Log.d("EmotionClassifier", "GPU not supported")
                }

            } catch (e: Exception) {
                Log.e("EmotionClassifier", "GPU init failed", e)
            }

            interpreter = Interpreter(model, options)

        } catch (e: Exception) {
            Log.e("EmotionClassifier", "Initialization error", e)
        }
    }

    private fun loadModelFile(filename: String): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun classify(bitmap: Bitmap): String {

        val interpreter = interpreter ?: return "Error"

        // Resize image
        val resizedBitmap =
            bitmap.scale(inputSize, inputSize)

        inputBuffer.rewind()

        // Convert to grayscale + normalize
        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {

            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // FER-2013 grayscale conversion (standard luminance)
            val gray = (0.299f * r + 0.587f * g + 0.114f * b)

            // normalize to [0,1]
            inputBuffer.putFloat(gray / 255f)
        }

        outputBuffer.rewind()

        interpreter.run(inputBuffer, outputBuffer)

        // Read output
        outputBuffer.rewind() // Rewind after writing
        val results = FloatArray(outputSize)
        outputBuffer.asFloatBuffer().get(results)

        val maxIndex = results.indices.maxByOrNull { results[it] } ?: -1

        return getEmotionLabel(maxIndex)
    }

    private fun getEmotionLabel(index: Int): String {
        val emotions = listOf(
            "Angry",
            "Disgust",
            "Fear",
            "Happy",
            "Neutral",
            "Sad",
            "Surprise"
        )
        return if (index in emotions.indices) emotions[index] else "Unknown"
    }

    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
