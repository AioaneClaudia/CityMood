package com.gefuhlskarte.citymood.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onMoodDetected: (moodLabel: String, emoji: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    // Variabile pentru stabilizare (smoothing)
    private var candidateMood: String = ""
    private var candidateCount: Int = 0
    private val requiredConsistentFrames = 4

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        val smile = face.smilingProbability ?: -1f
                        val leftEye = face.leftEyeOpenProbability ?: -1f
                        val rightEye = face.rightEyeOpenProbability ?: -1f
                        val avgEyeOpen = if (leftEye >= 0f && rightEye >= 0f) {
                            (leftEye + rightEye) / 2f
                        } else {
                            -1f
                        }

                        // Clasificare pe intervale distincte și echilibrate
                        val (detectedMood, emoji) = when {
                            // 1. Zâmbet foarte evident -> Great
                            smile >= 0.75f -> Pair("Great", "🤩")

                            // 2. Zâmbet mediu -> Happy
                            smile in 0.28f..0.74f -> Pair("Happy", "😊")

                            // 3. Ochii tensionați și fără zâmbet -> Stressed
                            smile < 0.15f && avgEyeOpen >= 0.92f -> Pair("Stressed", "😰")

                            // 4. Lipsă totală de zâmbet și privire căzută/obosită -> Sad
                            smile <= 0.06f && avgEyeOpen in 0.0f..0.75f -> Pair("Sad", "😔")

                            // 5. Față neutra, relaxată -> Calm
                            else -> Pair("Calm", "😌")
                        }

                        // Filtru de stabilizare
                        if (detectedMood == candidateMood) {
                            candidateCount++
                            if (candidateCount >= requiredConsistentFrames) {
                                onMoodDetected(detectedMood, emoji)
                            }
                        } else {
                            candidateMood = detectedMood
                            candidateCount = 1
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}