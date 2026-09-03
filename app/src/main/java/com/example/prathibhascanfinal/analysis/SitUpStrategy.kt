package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class SitUpStrategy : RepStrategy {
    override val exerciseType = ExerciseType.SIT_UPS

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rShoulder = landmarks[12]
        val rHip = landmarks[24]
        val rKnee = landmarks[26]
        val lShoulder = landmarks[11]
        val lHip = landmarks[23]
        val lKnee = landmarks[25]
        
        val rSitUpAngle = PoseMathUtils.calculateAngle(rShoulder, rHip, rKnee)
        val lSitUpAngle = PoseMathUtils.calculateAngle(lShoulder, lHip, lKnee)
        
        val rConf = rShoulder.visibility().orElse(0f) + rHip.visibility().orElse(0f)
        val lConf = lShoulder.visibility().orElse(0f) + lHip.visibility().orElse(0f)
        
        val sitUpAngle = if (rConf > lConf) rSitUpAngle else lSitUpAngle
        
        var newState = currentState
        var feedback = "Engage your core!"
        val incorrect = mutableSetOf<Int>()

        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (sitUpAngle > 140) {
                    newState = ExerciseState.START_VERIFIED
                    feedback = "Ready! Sit up."
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (sitUpAngle < 70) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Great reach! Now go back down."
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (sitUpAngle > 130) {
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
            currentAngle = sitUpAngle,
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
        return 85
    }
}
