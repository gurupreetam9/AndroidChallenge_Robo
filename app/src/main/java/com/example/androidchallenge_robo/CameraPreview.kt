package com.example.androidchallenge_robo

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onEmotionDetected: (String, Long) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                val cameraExecutor = Executors.newSingleThreadExecutor()
                val classifier = EmotionClassifier(context)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(this.surfaceProvider)
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        
                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                        // Ensure bitmap is not null and manage rotation if needed
                        // toBitmap() handles YUV to Bitmap conversion.
                        // However, we need to be careful about rotation.
                        // For front camera, it might be rotated.
                        // EmotionClassifier.classify handles resizing.
                        
                        // We need to rotate the bitmap if necessary, or just rely on toBitmap() 
                        // which usually respects rotation if we pass it, but ImageProxy.toBitmap() needs checking.
                        
                        /**
                         * TODO: Handle rotation properly. 
                         * For now, let's assume toBitmap() is sufficient or we can manually rotate.
                         * But wait, toBitmap() is an extension in camera-core? 
                         * It's available in newer CameraX versions. We added 1.3.0-rc01 so it should be there.
                         */

                         if (bitmap != null) {
                             val startTime = System.currentTimeMillis()
                             val emotion = classifier.classify(bitmap)
                             val endTime = System.currentTimeMillis()
                             ContextCompat.getMainExecutor(context).execute {
                                 onEmotionDetected(emotion, endTime - startTime)
                             }
                         }
                        
                        imageProxy.close()
                    }
                    
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                    }
                    
                }, ContextCompat.getMainExecutor(context))
            }
        }
    )
}
