package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PushUpStrategy : RepStrategy {
    override val exerciseType = ExerciseType.PUSH_UPS

    private val READY_THRESHOLD = 150.0
    private val DOWN_THRESHOLD = 130.0
    private val DEPTH_THRESHOLD = 85.0
    private val UP_PHASE_THRESHOLD = 110.0

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rShoulder = landmarks[12]
        val rElbow = landmarks[14]
        val rWrist = landmarks[16]
        val rHip = landmarks[24]
        val rAnkle = landmarks[28]

        val lShoulder = landmarks[11]
        val lElbow = landmarks[13]
        val lWrist = landmarks[15]
        val lHip = landmarks[23]
        val lAnkle = landmarks[27]
        
        val rElbowAngle = PoseMathUtils.calculateAngle(rShoulder, rElbow, rWrist)
        val lElbowAngle = PoseMathUtils.calculateAngle(lShoulder, lElbow, lWrist)
        
        val rConf = (rShoulder.visibility().orElse(0f) + rElbow.visibility().orElse(0f) + rWrist.visibility().orElse(0f)) / 3f
        val lConf = (lShoulder.visibility().orElse(0f) + lElbow.visibility().orElse(0f) + lWrist.visibility().orElse(0f)) / 3f

        val elbowAngle = if (rConf > lConf) rElbowAngle else lElbowAngle
        
        val rBodyAngle = PoseMathUtils.calculateAngle(rShoulder, rHip, rAnkle)
        val lBodyAngle = PoseMathUtils.calculateAngle(lShoulder, lHip, lAnkle)
        val bodyAngle = if (rConf > lConf) rBodyAngle else lBodyAngle
        
        var newState = currentState
        var feedback = when(currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> "Get into plank position"
            ExerciseState.START_VERIFIED -> "Lower your chest"
            ExerciseState.MID_VERIFIED -> "Push up!"
            else -> "Good posture"
        }
        
        val incorrect = mutableSetOf<Int>()

        if (bodyAngle < 160) {
            feedback = "Keep your hips level"
            incorrect.addAll(listOf(11, 12, 23, 24, 27, 28))
        }

        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (elbowAngle > READY_THRESHOLD) {
                    newState = ExerciseState.START_VERIFIED
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (elbowAngle < DOWN_THRESHOLD) {
                    newState = ExerciseState.MOVING_DOWN
                }
            }
            ExerciseState.MOVING_DOWN -> {
                if (elbowAngle < DEPTH_THRESHOLD) {
                    newState = ExerciseState.MID_VERIFIED
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (elbowAngle > UP_PHASE_THRESHOLD) {
                    newState = ExerciseState.MOVING_UP
                }
            }
            ExerciseState.MOVING_UP -> {
                if (elbowAngle > READY_THRESHOLD - 5) {
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                }
            }
            else -> {}
        }

        return AnalysisResult(
            newState = newState,
            feedback = feedback,
            currentAngle = elbowAngle,
            currentAngles = mapOf("Elbow" to elbowAngle, "Body" to bodyAngle),
            incorrectJoints = incorrect,
            confidence = Math.max(rConf, lConf)
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var y = landmark.y()
            // Ghost shows ideal push-up depth
            if (index in 0..22) { // Upper body
                when (currentState) {
                    ExerciseState.START_VERIFIED, ExerciseState.MOVING_DOWN -> {
                        y += 0.12f // Target depth
                    }
                    ExerciseState.MID_VERIFIED -> {
                        y += 0.12f
                    }
                    else -> {}
                }
            }
            // Position ghost directly on user for AR overlay guidance
            NormalizedLandmark.create(
                landmark.x(), 
                y, 
                landmark.z(), 
                java.util.Optional.of(landmark.visibility().orElse(0f)), 
                java.util.Optional.of(landmark.presence().orElse(0f))
            )
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return PoseMathUtils.calculatePoseSimilarity(userLandmarks, ghostLandmarks, ExerciseType.PUSH_UPS)
    }
}
