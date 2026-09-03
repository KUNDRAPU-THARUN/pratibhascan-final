package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class LungeStrategy : RepStrategy {
    override val exerciseType = ExerciseType.LUNGES

    private val STANDING_THRESHOLD = 150.0 // Relaxed from 160
    private val DOWN_THRESHOLD = 135.0
    private val TARGET_DEPTH_THRESHOLD = 100.0
    private val UP_PHASE_THRESHOLD = 120.0

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        // Bilateral Analysis: Check both legs
        val rHip = landmarks[24]
        val rKnee = landmarks[26]
        val rAnkle = landmarks[28]
        val lHip = landmarks[23]
        val lKnee = landmarks[25]
        val lAnkle = landmarks[27]
        
        val rKneeAngle = PoseMathUtils.calculateAngle(rHip, rKnee, rAnkle)
        val lKneeAngle = PoseMathUtils.calculateAngle(lHip, lKnee, lAnkle)
        
        val rConf = (rHip.visibility().orElse(0f) + rKnee.visibility().orElse(0f) + rAnkle.visibility().orElse(0f)) / 3f
        val lConf = (lHip.visibility().orElse(0f) + lKnee.visibility().orElse(0f) + lAnkle.visibility().orElse(0f)) / 3f
        
        // Track the knee that is bending most (likely the back knee in a lunge)
        val kneeAngle = Math.min(rKneeAngle, lKneeAngle)
        
        var newState = currentState
        var feedback = "Step forward!"
        val incorrect = mutableSetOf<Int>()
        val warnings = mutableSetOf<Int>()

        // Form check: Front knee stability (simplified check for lateral movement)
        if (Math.abs(rKnee.x() - rAnkle.x()) > 0.08f && rKneeAngle > lKneeAngle) {
            feedback = "Keep your front knee stable"
            warnings.add(26)
        }

        // Standardized State Machine with Hysteresis
        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (kneeAngle > STANDING_THRESHOLD) {
                    newState = ExerciseState.START_VERIFIED
                    feedback = "Ready! Lunge down."
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (kneeAngle < DOWN_THRESHOLD) {
                    newState = ExerciseState.MOVING_DOWN
                }
            }
            ExerciseState.MOVING_DOWN -> {
                if (kneeAngle < TARGET_DEPTH_THRESHOLD) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Perfect depth! Push back up."
                } else if (kneeAngle > DOWN_THRESHOLD + 10) {
                    newState = ExerciseState.START_VERIFIED // Reset if they stand back up too early
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (kneeAngle > UP_PHASE_THRESHOLD) {
                    newState = ExerciseState.MOVING_UP
                }
            }
            ExerciseState.MOVING_UP -> {
                if (kneeAngle > STANDING_THRESHOLD - 5) {
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
            currentAngle = kneeAngle,
            currentAngles = mapOf("Knee Angle" to kneeAngle),
            incorrectJoints = incorrect,
            warningJoints = warnings,
            confidence = Math.max(rConf, lConf)
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var y = landmark.y()
            if (index in 24..28 && (currentState == ExerciseState.MOVING_DOWN || currentState == ExerciseState.START_VERIFIED)) {
                y += 0.1f
            }
            NormalizedLandmark.create(landmark.x(), y, landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        // Compare angles instead of absolute Y positions for distance-invariance
        val userKnee = PoseMathUtils.calculateAngle(userLandmarks[24], userLandmarks[26], userLandmarks[28])
        val ghostKnee = PoseMathUtils.calculateAngle(ghostLandmarks[24], ghostLandmarks[26], ghostLandmarks[28])
        val diff = Math.abs(userKnee - ghostKnee)
        return (100 - (diff * 1.5)).toInt().coerceIn(0, 100)
    }
}
