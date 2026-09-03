package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PullUpStrategy : RepStrategy {
    override val exerciseType = ExerciseType.PULL_UPS

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rShoulder = landmarks[12]
        val rElbow = landmarks[14]
        val rWrist = landmarks[16]
        val lShoulder = landmarks[11]
        val lElbow = landmarks[13]
        val lWrist = landmarks[15]
        
        val rElbowAngle = PoseMathUtils.calculateAngle(rShoulder, rElbow, rWrist)
        val lElbowAngle = PoseMathUtils.calculateAngle(lShoulder, lElbow, lWrist)
        
        val rConf = rShoulder.visibility().orElse(0f) + rElbow.visibility().orElse(0f)
        val lConf = lShoulder.visibility().orElse(0f) + lElbow.visibility().orElse(0f)
        
        val elbowAngle = if (rConf > lConf) rElbowAngle else lElbowAngle
        
        var newState = currentState
        var feedback = "Pull yourself up!"
        val incorrect = mutableSetOf<Int>()

        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (elbowAngle > 150) {
                    newState = ExerciseState.START_VERIFIED
                    feedback = "Ready! Pull."
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (elbowAngle < 60) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Excellent pull! Lower yourself."
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (elbowAngle > 140) {
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                    feedback = "Rep complete!"
                }
            }
            else -> {}
        }

        return AnalysisResult(
            newState = newState, 
            feedback = feedback, 
            currentAngle = elbowAngle,
            incorrectJoints = incorrect
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var y = landmark.y()
            if (index in 0..12 && currentState == ExerciseState.START_VERIFIED) {
                y -= 0.2f
            }
            NormalizedLandmark.create(landmark.x(), y, landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return 88
    }
}
