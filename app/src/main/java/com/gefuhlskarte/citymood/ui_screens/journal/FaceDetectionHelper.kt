package com.gefuhlskarte.citymood.ui_screens.journal

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Returns true when at least one face is detected, false when no face is found,
 * or null when the image could not be analyzed.
 */
object FaceDetectionHelper {

    suspend fun containsFace(
        context: Context,
        photoUri: String
    ): Boolean? = withContext(Dispatchers.IO) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()

        val detector = FaceDetection.getClient(options)

        try {
            val image = InputImage.fromFilePath(
                context,
                Uri.parse(photoUri)
            )
            Tasks.await(detector.process(image)).isNotEmpty()
        } catch (_: Exception) {
            // Salvarea jurnalului continuă chiar dacă fotografia nu poate fi analizată.
            null
        } finally {
            detector.close()
        }
    }
}