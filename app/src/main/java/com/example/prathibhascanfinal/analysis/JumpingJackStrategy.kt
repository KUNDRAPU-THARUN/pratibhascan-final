package com.example.prathibhascanfinal.analysis

import com.example.prathibhascanfinal.ExerciseState
import com.example.prathibhascanfinal.ExerciseType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class JumpingJackStrategy : RepStrategy {
    override val exerciseType = ExerciseType.JUMPING_JACKS

    override fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult {
        val lWrist = landmarks[15]
        val rWrist = landmarks[16]
        val lAnkle = landmarks[27]
        val rAnkle = landmarks[28]
        val lShoulder = landmarks[11]
        val rShoulder = landmarks[12]
        
        // Bilateral check for arms raised above shoulders
        val armsRaised = lWrist.y() < lShoulder.y() && rWrist.y() < rShoulder.y()
        
        // Dynamic leg separation (based on shoulder width for scaling)
        val shoulderWidth = Math.abs(lShoulder.x() - rShoulder.x())
        val legSeparation = Math.abs(lAnkle.x() - rAnkle.x())
        val legsApart = legSeparation > shoulderWidth * 1.5f
        
        var newState = currentState
        var feedback = "Keep jumping!"
        val warnings = mutableSetOf<Int>()

        if (!armsRaised && legsApart) {
            feedback = "Sync your arms with your legs"
            warnings.addAll(listOf(15, 16))
        }

        when (currentState) {
            ExerciseState.READY, ExerciseState.COMPLETED -> {
                if (!armsRaised && !legsApart) {
                    newState = ExerciseState.START_VERIFIED
                }
            }
            ExerciseState.START_VERIFIED -> {
                if (armsRaised && legsApart) {
                    newState = ExerciseState.MID_VERIFIED
                    feedback = "Great! Now jump back."
                }
            }
            ExerciseState.MID_VERIFIED -> {
                if (!armsRaised && !legsApart) {
                    newState = ExerciseState.COMPLETED
                    onRepComplete()
                    feedback = "Excellent rep!"
                }
            }
            else -> {}
        }

        val accuracy = if (armsRaised && legsApart) 100 else 70

        return AnalysisResult(
            newState = newState, 
            feedback = feedback,
            warningJoints = warnings,
            alignmentScore = accuracy
        )
    }

    override fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark> {
        return userLandmarks.mapIndexed { index, landmark ->
            var x = landmark.x()
            var y = landmark.y()
            if (currentState == ExerciseState.START_VERIFIED) {
                if (index == 15) { y -= 0.4f; x -= 0.2f }
                if (index == 16) { y -= 0.4f; x += 0.2f }
                if (index == 27) x -= 0.1f
                if (index == 28) x += 0.1f
            }
            NormalizedLandmark.create(x, y, landmark.z(), java.util.Optional.of(landmark.visibility().orElse(0f)), java.util.Optional.of(landmark.presence().orElse(0f)))
        }
    }

    override fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int {
        return 85 // Simplified
    }
}
