package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class MountainClimberStrategy : RepStrategy {
    override val exerciseType = ExerciseType.MOUNTAIN_CLIMBERS

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rHip = landmarks[24]
        val rKnee = landmarks[26]
        val rAnkle = landmarks[28]
        val lHip = landmarks[23]
        val lKnee = landmarks[25]
        val lAnkle = landmarks[27]
        
        val rKneeAngle = PoseMathUtils.calculateAngle(rHip, rKnee, rAnkle)
        val lKneeAngle = PoseMathUtils.calculateAngle(lHip, lKnee, lAnkle)
        
        // In mountain climbers, one knee is driving forward (angle decreases) 
        // while the other is extended.
        val activeKneeAngle = Math.min(rKneeAngle, lKneeAngle)
        
        var newState = currentState
        var feedback = "Drive those knees!"
        val warnings = mutableSetOf<Int>()

        // Check for "Hips too high" common error
        val shouldersY = (landmarks[11].y() + landmarks[12].y()) / 2f
        if (rHip.y() < shouldersY - 0.05f) {
            feedback = "Keep your hips down"
            warnings.addAll(listOf(23, 24))
        }

        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (activeKneeAngle > 150) {
                    newState = ExerciseState.START_VERIFIED
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (activeKneeAngle < 110) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Good drive!"
                }
            }
            ExerciseState.MID_VERIFIED -> {
                // Return to extended position or switch legs
                if (activeKneeAngle > 140) {
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                }
            }
            else -> {}
        }

        return AnalysisResult(
            newState = newState, 
            feedback = feedback, 
            currentAngle = activeKneeAngle,
            warningJoints = warnings
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var x = landmark.x()
            if (index == 26 && currentState == ExerciseState.START_VERIFIED) {
                x -= 0.15f
            }
            NormalizedLandmark.create(x, landmark.y(), landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return 90
    }
}
