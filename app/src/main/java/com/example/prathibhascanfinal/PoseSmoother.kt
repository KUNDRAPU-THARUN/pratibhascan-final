package com.example.prathibhascanfinal

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Temporal smoothing for 33-point Pose Landmarks.
 * Uses Exponential Moving Average (EMA) to reduce jitter while maintaining responsiveness.
 */
class PoseSmoother(private val alpha: Float = 0.65f) {

    private var previousLandmarks: List<List<NormalizedLandmark>>? = null

    /**
     * Smooths the raw detector results.
     */
    fun smooth(result: PoseLandmarkerResult): PoseLandmarkerResult {
        val rawLandmarksList = result.landmarks()
        if (rawLandmarksList.isEmpty()) {
            previousLandmarks = null
            return result
        }

        val smoothedLandmarksList = mutableListOf<List<NormalizedLandmark>>()

        for (personIndex in rawLandmarksList.indices) {
            val personRaw = rawLandmarksList[personIndex]
            val personPrev = previousLandmarks?.getOrNull(personIndex)

            val smoothedPerson = if (personPrev == null || personPrev.size != personRaw.size) {
                personRaw // First frame or size mismatch, no smoothing
            } else {
                personRaw.mapIndexed { i, current ->
                    val prev = personPrev[i]
                    NormalizedLandmark.create(
                        lerp(current.x(), prev.x(), alpha),
                        lerp(current.y(), prev.y(), alpha),
                        lerp(current.z(), prev.z(), alpha),
                        java.util.Optional.of(lerp(current.visibility().orElse(0f), prev.visibility().orElse(0f), alpha)),
                        java.util.Optional.of(lerp(current.presence().orElse(0f), prev.presence().orElse(0f), alpha))
                    )
                }
            }
            smoothedLandmarksList.add(smoothedPerson)
        }

        previousLandmarks = smoothedLandmarksList
        
        // Return a new result with smoothed landmarks (MediaPipe results are immutable but we can't easily recreate the native object with custom timestamps, so we just return the smoothed list for internal use if needed, but for simplicity we'll just expose the smoothed landmarks in the result bundle in the ViewModel)
        return result // Note: In MediaPipe 0.10.x, we might need a custom wrapper if we can't recreate PoseLandmarkerResult easily.
    }

    /**
     * Expose smoothed landmarks directly
     */
    fun getSmoothedLandmarks(result: PoseLandmarkerResult): List<List<NormalizedLandmark>> {
        val rawLandmarksList = result.landmarks()
        if (rawLandmarksList.isEmpty()) {
            previousLandmarks = null
            return emptyList()
        }

        val smoothedLandmarksList = mutableListOf<List<NormalizedLandmark>>()

        for (personIndex in rawLandmarksList.indices) {
            val personRaw = rawLandmarksList[personIndex]
            val personPrev = previousLandmarks?.getOrNull(personIndex)

            val smoothedPerson = if (personPrev == null || personPrev.size != personRaw.size) {
                personRaw 
            } else {
                personRaw.mapIndexed { i, current ->
                    val prev = personPrev[i]
                    NormalizedLandmark.create(
                        lerp(current.x(), prev.x(), alpha),
                        lerp(current.y(), prev.y(), alpha),
                        lerp(current.z(), prev.z(), alpha),
                        java.util.Optional.of(lerp(current.visibility().orElse(0f), prev.visibility().orElse(0f), alpha)),
                        java.util.Optional.of(lerp(current.presence().orElse(0f), prev.presence().orElse(0f), alpha))
                    )
                }
            }
            smoothedLandmarksList.add(smoothedPerson)
        }

        previousLandmarks = smoothedLandmarksList
        return smoothedLandmarksList
    }

    private fun lerp(current: Float, previous: Float, alpha: Float): Float {
        return alpha * current + (1 - alpha) * previous
    }

    fun reset() {
        previousLandmarks = null
    }
}
