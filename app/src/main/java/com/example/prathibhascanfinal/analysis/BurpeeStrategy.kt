package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class BurpeeStrategy : RepStrategy {
    override val exerciseType = ExerciseType.BURPEES

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val shoulder = landmarks[12]
        val hip = landmarks[24]
        val wrist = landmarks[16]
        
        var newState = currentState
        var feedback = "Drop down!"
        
        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                // Standing: shoulders well above hips
                if (shoulder.y() < hip.y() - 0.1f) {
                    newState = ExerciseState.START_VERIFIED
                }
            }
            ExerciseState.START_VERIFIED -> {
                // Moving down: wrists reaching ground
                if (wrist.y() > hip.y() - 0.1f) {
                    newState = ExerciseState.MOVING_DOWN
                    feedback = "Plank reached. Push up!"
                }
            }
            ExerciseState.MOVING_DOWN -> {
                // Chest to floor (simplified by shoulder/hip proximity to ground)
                if (shoulder.y() > 0.6f && Math.abs(shoulder.y() - hip.y()) < 0.2f) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Jump up!"
                }
            }
            ExerciseState.MID_VERIFIED -> {
                // Jumping back up
                if (shoulder.y() < hip.y() - 0.2f) {
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                    feedback = "Rep complete!"
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
