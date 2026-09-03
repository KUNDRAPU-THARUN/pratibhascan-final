package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.PoseMathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class BasketballStrategy(override val exerciseType: ExerciseType) : RepStrategy {

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        return when (exerciseType) {
            ExerciseType.BASKETBALL_SHOOT -> analyzeShooting(landmarks, currentState, onRepComplete)
            ExerciseType.BASKETBALL_DRIBBLING -> analyzeDribbling(landmarks, currentState, onRepComplete)
            ExerciseType.BASKETBALL_DEFENSE -> analyzeDefense(landmarks, currentState, onRepComplete)
            else -> AnalysisResult(currentState, "Select Basketball Mode")
        }
    }

    private fun analyzeShooting(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        // Shooting is usually dominant-hand led, but we track both for balance
        val rShoulder = landmarks[12]
        val rElbow = landmarks[14]
        val rWrist = landmarks[16]
        val lShoulder = landmarks[11]
        val lElbow = landmarks[13]
        val lWrist = landmarks[15]
        val rHip = landmarks[24]
        val rKnee = landmarks[26]
        val rAnkle = landmarks[28]

        val rElbowAngle = PoseMathUtils.calculateAngle(rShoulder, rElbow, rWrist)
        val lElbowAngle = PoseMathUtils.calculateAngle(lShoulder, lElbow, lWrist)
        
        // Use the arm that is higher (shooting arm)
        val isRightHanded = rWrist.y() < lWrist.y()
        val elbowAngle = if (isRightHanded) rElbowAngle else lElbowAngle
        val shootingElbowIdx = if (isRightHanded) 14 else 13
        
        val kneeAngle = PoseMathUtils.calculateAngle(rHip, rKnee, rAnkle)
        val bodyAlignment = PoseMathUtils.calculateAngle(rShoulder, rHip, rAnkle)
        
        var newState = currentState
        var feedback = "Hold your form..."
        val incorrect = mutableSetOf<Int>()
        val warnings = mutableSetOf<Int>()

        // Biomechanical Analysis
        if (elbowAngle < 85) {
            feedback = "Tuck your shooting elbow in"
            incorrect.add(shootingElbowIdx)
        }
        
        if (bodyAlignment < 165) {
            feedback = "Keep your core stable"
            warnings.add(24)
        }

        // Motion Flow
        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (kneeAngle < 135) {
                    newState = ExerciseState.START_VERIFIED
                    feedback = "Powering up - Dip low!"
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (elbowAngle > 150) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Release point reached!"
                }
            }
            ExerciseState.MID_VERIFIED -> {
                val wristY = if (isRightHanded) rWrist.y() else lWrist.y()
                val shoulderY = if (isRightHanded) rShoulder.y() else lShoulder.y()
                
                if (elbowAngle > 165 && wristY < shoulderY - 0.1f) {
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                    feedback = "Elite Release! Nice Follow-through."
                }
            }
            else -> {}
        }

        return AnalysisResult(
            newState = newState,
            feedback = feedback,
            currentAngle = elbowAngle,
            currentAngles = mapOf("Shooting Elbow" to elbowAngle, "Knee Power" to kneeAngle),
            incorrectJoints = incorrect,
            warningJoints = warnings,
            alignmentScore = (bodyAlignment / 1.8).toInt().coerceIn(0, 100)
        )
    }

    private fun analyzeDribbling(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val hip = landmarks[24]
        val wrist = landmarks[16]
        
        val hipHeight = hip.y()
        val wristY = wrist.y()
        
        val feedback: String
        val incorrect = mutableSetOf<Int>()

        // Ideal dribbling hip height is around 0.6-0.7 of image height (lower is better)
        if (hipHeight < 0.65f) {
            feedback = "Great low stance! High control."
        } else {
            feedback = "Get lower to the ground"
            incorrect.add(24)
        }

        // Detect dribble motion (wrist moving up and down)
        // This is a simplified proxy for the hackathon
        if (wristY > hipHeight + 0.05f) {
            onRepComplete()
        }

        return AnalysisResult(
            newState = currentState,
            feedback = feedback,
            currentAngle = (1.0 - hipHeight) * 180,
            currentAngles = mapOf("Stance Depth" to (1.0 - hipHeight) * 100),
            incorrectJoints = incorrect
        )
    }

    private fun analyzeDefense(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        @Suppress("UNUSED_PARAMETER") onRepComplete: () -> Unit
    ): AnalysisResult {
        val lKnee = landmarks[25]
        val rKnee = landmarks[26]
        val lAnkle = landmarks[27]
        val rAnkle = landmarks[28]
        
        val baseWidth = kotlin.math.abs(lAnkle.x() - rAnkle.x())
        val avgKneeY = (lKnee.y() + rKnee.y()) / 2f
        
        var feedback = "Stay wide and low"
        val incorrect = mutableSetOf<Int>()

        if (baseWidth < 0.4f) {
            feedback = "Widen your stance"
            incorrect.addAll(listOf(27, 28))
        } else if (avgKneeY < 0.7f) {
            feedback = "Perfect defensive stance"
        }

        return AnalysisResult(
            newState = currentState,
            feedback = feedback,
            currentAngle = (baseWidth * 100).toDouble(),
            currentAngles = mapOf("RKnee" to (rKnee.y() * 100).toDouble(), "LKnee" to (lKnee.y() * 100).toDouble()),
            incorrectJoints = incorrect
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var y = landmark.y()
            var x = landmark.x()
            
            when (exerciseType) {
                ExerciseType.BASKETBALL_SHOOT -> {
                    if (index == 14) y -= 0.1f // Ideal high elbow
                    if (index == 16) y -= 0.2f // Ideal high release
                }
                ExerciseType.BASKETBALL_DEFENSE -> {
                    if (index == 25 || index == 26) y += 0.1f // Ideal low knees
                    if (index == 27) x -= 0.1f // Ideal wide base
                    if (index == 28) x += 0.1f
                }
                else -> {}
            }
            NormalizedLandmark.create(x, y, landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return PoseMathUtils.calculatePoseSimilarity(userLandmarks, ghostLandmarks, ExerciseType.SPORT_SKILL)
    }
}
