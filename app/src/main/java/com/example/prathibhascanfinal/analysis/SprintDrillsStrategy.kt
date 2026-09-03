package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class SprintDrillsStrategy : RepStrategy {
    override val exerciseType = ExerciseType.SPRINT_DRILLS

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val rShoulder = landmarks[12]
        val rHip = landmarks[24]
        val lShoulder = landmarks[11]
        val lHip = landmarks[23]
        
        // Forward lean check: hip should be slightly ahead of shoulder in horizontal space 
        // (Assuming side profile for sprint drills)
        val rLean = (rHip.x() - rShoulder.x())
        val lLean = (lHip.x() - lShoulder.x())
        val leanValue = Math.max(rLean, lLean)
        
        val isLeaning = leanValue > 0.04f
        var feedback = if (isLeaning) "Perfect lean!" else "Lean forward slightly"
        
        // Detection of running movement: alternating knee drive
        val rKnee = landmarks[26]
        val lKnee = landmarks[25]
        val kneeDelta = Math.abs(rKnee.y() - lKnee.y())
        
        var newState = currentState
        if (kneeDelta > 0.12f) {
            if (currentState != ExerciseState.MID_VERIFIED) {
                newState = ExerciseState.MID_VERIFIED
                onRepComplete()
            }
        } else {
            newState = ExerciseState.READY
        }

        return AnalysisResult(
            newState = newState, 
            feedback = feedback,
            currentAngle = leanValue.toDouble() * 100
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var x = landmark.x()
            if (index in 0..12) x += 0.05f // Add a lean to ghost
            NormalizedLandmark.create(x, landmark.y(), landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return 85
    }
}
