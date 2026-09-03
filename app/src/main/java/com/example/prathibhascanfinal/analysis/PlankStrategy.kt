package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PlankStrategy : RepStrategy {
    override val exerciseType = ExerciseType.PLANKS

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val shoulder = landmarks[12]
        val hip = landmarks[24]
        val ankle = landmarks[28]
        
        val bodyAngle = PoseMathUtils.calculateAngle(shoulder, hip, ankle)
        
        var feedback = "Hold it!"
        val incorrect = mutableSetOf<Int>()

        if (Math.abs(bodyAngle - 180) > 20) {
            feedback = "Keep your back straight"
            incorrect.addAll(listOf(12, 24, 28))
        }

        return AnalysisResult(
            newState = currentState,
            feedback = feedback,
            currentAngle = bodyAngle,
            incorrectJoints = incorrect
        )
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
        val angle = PoseMathUtils.calculateAngle(userLandmarks[12], userLandmarks[24], userLandmarks[28])
        return (100 - Math.abs(angle - 180)).toInt().coerceIn(0, 100)
    }
}
