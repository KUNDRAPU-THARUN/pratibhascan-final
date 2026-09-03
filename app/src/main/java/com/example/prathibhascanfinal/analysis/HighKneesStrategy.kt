package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class HighKneesStrategy : RepStrategy {
    override val exerciseType = ExerciseType.HIGH_KNEES

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rHip = landmarks[24]
        val rKnee = landmarks[26]
        val lHip = landmarks[23]
        val lKnee = landmarks[25]
        
        // Check if either knee is above its respective hip
        val rKneeHigh = rKnee.y() < rHip.y() - 0.05f
        val lKneeHigh = lKnee.y() < lHip.y() - 0.05f
        
        var newState = currentState
        var feedback = "Knees up higher!"
        
        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (rKneeHigh || lKneeHigh) {
                    newState = ExerciseState.MID_VERIFIED
                    onRepComplete()
                    feedback = "Good drive!"
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (!rKneeHigh && !lKneeHigh) {
                    newState = ExerciseState.READY
                }
            }
            else -> {}
        }

        return AnalysisResult(newState, feedback)
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var y = landmark.y()
            if (index == 26) y -= 0.15f
            NormalizedLandmark.create(landmark.x(), y, landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return 92
    }
}
