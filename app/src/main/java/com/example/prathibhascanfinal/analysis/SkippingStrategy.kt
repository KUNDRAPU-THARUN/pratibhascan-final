package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class SkippingStrategy : RepStrategy {
    override val exerciseType = ExerciseType.SKIPPING

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rAnkle = landmarks[28]
        val lAnkle = landmarks[27]
        
        // Detect a jump when both ankles are high off the ground relative to normal standing.
        // Assuming the image scale is normalized 0-1, standing ankles are around 0.9.
        val jumpThreshold = 0.85f
        val isJumping = rAnkle.y() < jumpThreshold && lAnkle.y() < jumpThreshold
        
        var newState = currentState
        var feedback = "Jump higher!"
        
        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (isJumping) {
                    newState = ExerciseState.MID_VERIFIED
                    onRepComplete()
                    feedback = "Perfect skip!"
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (!isJumping) {
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
            NormalizedLandmark.create(landmark.x(), landmark.y(), landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return 80
    }
}
