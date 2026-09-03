package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class SquatStrategy : RepStrategy {
    override val exerciseType = ExerciseType.SQUATS

    private val STANDING_THRESHOLD = 150.0 // Relaxed from 160
    private val DOWN_THRESHOLD = 135.0 // Adjusted for better sensitivity
    private val TARGET_DEPTH_THRESHOLD = 100.0
    private val UP_PHASE_THRESHOLD = 120.0
    
    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        // Bilateral analysis: Check both sides and use the most reliable or average
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
        
        // Use side with better confidence
        val kneeAngle = if (rConf > lConf) rKneeAngle else lKneeAngle
        
        // Body alignment (Back straight)
        val rShoulder = landmarks[12]
        val lShoulder = landmarks[11]
        val backAngle = if (rConf > lConf) {
            PoseMathUtils.calculateAngle(rShoulder, rHip, rKnee)
        } else {
            PoseMathUtils.calculateAngle(lShoulder, lHip, lKnee)
        }
        
        var newState = currentState
        var feedback = when(currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> "Stand straight to start"
            ExerciseState.START_VERIFIED -> "Lower your hips"
            ExerciseState.MOVING_DOWN -> "Keep going down"
            ExerciseState.MID_VERIFIED -> "Good depth! Push up"
            ExerciseState.MOVING_UP -> "Almost there"
            else -> "Keep going!"
        }
        
        val incorrect = mutableSetOf<Int>()
        val warnings = mutableSetOf<Int>()

        // Form checks
        if (backAngle < 150) {
            feedback = "Straighten your back"
            incorrect.addAll(listOf(11, 12, 23, 24))
        }
        
        // Knee tracking check (Avoid valgus)
        val kneeDistance = Math.abs(rKnee.x() - lKnee.x())
        val hipDistance = Math.abs(rHip.x() - lHip.x())
        if (kneeDistance < hipDistance * 0.8) {
            feedback = "Push your knees out"
            warnings.addAll(listOf(25, 26))
        }

        // State Machine with Hysteresis
        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (kneeAngle > STANDING_THRESHOLD) {
                    newState = ExerciseState.START_VERIFIED
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
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (kneeAngle > UP_PHASE_THRESHOLD) {
                    newState = ExerciseState.MOVING_UP
                }
            }
            ExerciseState.MOVING_UP -> {
                if (kneeAngle > STANDING_THRESHOLD - 5) { // Small buffer
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                }
            }
            else -> {}
        }

        return AnalysisResult(
            newState = newState,
            feedback = feedback,
            currentAngle = kneeAngle,
            currentAngles = mapOf("Knee" to kneeAngle, "Back" to backAngle),
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
            // Ghost shows "Ideal Depth" when user is moving down or ready
            val isTargetJoint = index in 23..32 // Hips to toes
            if (isTargetJoint) {
                when (currentState) {
                    ExerciseState.START_VERIFIED, ExerciseState.MOVING_DOWN -> {
                        // Slowly move ghost to perfect depth (0.15 lower than standing)
                        y += 0.15f 
                    }
                    ExerciseState.MID_VERIFIED -> {
                        y += 0.15f // Stay at max depth
                    }
                    else -> {} // Standing
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
        return PoseMathUtils.calculatePoseSimilarity(userLandmarks, ghostLandmarks, ExerciseType.SQUATS)
    }
}
