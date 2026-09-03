package com.example.prathibhascanfinal

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PoseMathUtils {

    /**
     * Calculates the angle between three landmarks in 3D.
     * Most reliable for fitness tracking.
     */
    fun calculateAngle(
        first: NormalizedLandmark,
        mid: NormalizedLandmark,
        last: NormalizedLandmark
    ): Double {
        return calculateAngle3D(first, mid, last)
    }

    private fun calculateAngle2D(
        first: NormalizedLandmark,
        mid: NormalizedLandmark,
        last: NormalizedLandmark
    ): Double {
        var angle = Math.toDegrees(
            (atan2(last.y() - mid.y(), last.x() - mid.x()) -
                    atan2(first.y() - mid.y(), first.x() - mid.x())).toDouble()
        )
        angle = Math.abs(angle)
        if (angle > 180) {
            angle = 360 - angle
        }
        return angle
    }

    private fun calculateAngle3D(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Double {
        // Vectors BA and BC
        val v1x = a.x() - b.x()
        val v1y = a.y() - b.y()
        val v1z = a.z() - b.z()

        val v2x = c.x() - b.x()
        val v2y = c.y() - b.y()
        val v2z = c.z() - b.z()

        val dotProduct = v1x * v2x + v1y * v2y + v1z * v2z
        val mag1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
        val mag2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)

        if (mag1 * mag2 == 0f) return 0.0

        val cosTheta = dotProduct / (mag1 * mag2)
        // Clamp to avoid NaN from precision issues
        val clampedCos = cosTheta.coerceIn(-1.0f, 1.0f)
        return Math.toDegrees(acos(clampedCos.toDouble()))
    }

    fun calculateDistance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        val dz = a.z() - b.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculates a similarity score between user and ghost landmarks.
     * Compares key joint angles instead of absolute coordinates to be scale-invariant.
     */
    fun calculatePoseSimilarity(
        user: List<NormalizedLandmark>,
        ghost: List<NormalizedLandmark>,
        exerciseType: ExerciseType
    ): Int {
        if (user.isEmpty() || ghost.isEmpty()) return 0

        val anglesToCompare = when (exerciseType) {
            ExerciseType.SQUATS -> listOf(
                Triple(24, 26, 28), // Knee
                Triple(12, 24, 26)  // Hip
            )
            ExerciseType.PUSH_UPS -> listOf(
                Triple(12, 14, 16), // Elbow
                Triple(12, 24, 28)  // Body alignment
            )
            ExerciseType.BASKETBALL_SHOOT -> listOf(
                Triple(12, 14, 16), // Shooting Elbow
                Triple(24, 26, 28), // Knee bend
                Triple(12, 24, 26)  // Hip posture
            )
            ExerciseType.BASKETBALL_DRIBBLING -> listOf(
                Triple(24, 26, 28), // Knee (low stance)
                Triple(12, 24, 26), // Hip (low stance)
                Triple(11, 13, 15)  // Support arm
            )
            ExerciseType.BASKETBALL_DEFENSE -> listOf(
                Triple(24, 26, 28), // Base width / Knee bend
                Triple(11, 12, 14)  // Arm reach
            )
            ExerciseType.SPORT_SKILL -> listOf( // Default Skill
                Triple(12, 14, 16),
                Triple(24, 26, 28),
                Triple(12, 24, 26)
            )
            else -> listOf(Triple(12, 24, 26), Triple(11, 23, 25))
        }

        var totalDiff = 0.0
        for (triple in anglesToCompare) {
            val userAngle = calculateAngle(user[triple.first], user[triple.second], user[triple.third])
            val ghostAngle = calculateAngle(ghost[triple.first], ghost[triple.second], ghost[triple.third])
            totalDiff += Math.abs(userAngle - ghostAngle)
        }

        val avgDiff = totalDiff / anglesToCompare.size
        // Map 0-45 degree avg diff to 100-0 score
        return (100 - (avgDiff * 2.22)).toInt().coerceIn(0, 100)
    }

    /**
     * Calculates a score (0-100) for a single joint based on target range.
     */
    fun calculateJointScore(current: Double, targetMin: Double, targetMax: Double): Int {
        return when {
            current in targetMin..targetMax -> 100
            current < targetMin -> (100 - (targetMin - current) * 2).toInt().coerceIn(0, 100)
            else -> (100 - (current - targetMax) * 2).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Maps similarity score to descriptive feedback label.
     */
    fun getSimilarityLabel(score: Int): String {
        return when {
            score >= 90 -> "Elite Form"
            score >= 75 -> "Great Alignment"
            score >= 50 -> "Good - Adjust Joint"
            score >= 30 -> "Needs Correction"
            else -> "Align Pose"
        }
    }

    /**
     * Simple exponential smoothing for landmarks
     */
    fun smoothLandmark(current: Float, previous: Float, alpha: Float = 0.7f): Float {
        return alpha * current + (1 - alpha) * previous
    }

    /**
     * Creates a transformation matrix to map normalized (0-1) coordinates to view coordinates.
     * Handles rotation, mirroring and aspect ratio (FILL_CENTER / CENTER_CROP).
     */
    fun getTransformationMatrix(
        viewWidth: Int,
        viewHeight: Int,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ): android.graphics.Matrix {
        val matrix = android.graphics.Matrix()
        
        // 1. Move to origin (-0.5 to 0.5 range) for rotation and mirroring
        matrix.postTranslate(-0.5f, -0.5f)

        // 2. Mirror if front camera (X-axis flip)
        // MediaPipe landmarks are in image coordinate space. 
        // For front camera, we flip X to match the mirrored preview.
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f)
        }

        // 3. Rotate to match image orientation
        matrix.postRotate(rotationDegrees.toFloat())

        // 4. Calculate rotated image dimensions
        val rotatedWidth = if (rotationDegrees % 180 == 0) imageWidth else imageHeight
        val rotatedHeight = if (rotationDegrees % 180 == 0) imageHeight else imageWidth
        
        // 5. Scale to match view size using FILL_CENTER logic (Center Crop)
        // This ensures the skeleton matches the part of the image visible in the preview.
        val scale = Math.max(
            viewWidth.toFloat() / rotatedWidth,
            viewHeight.toFloat() / rotatedHeight
        )
        
        matrix.postScale(rotatedWidth * scale, rotatedHeight * scale)

        // 6. Center the result in the view
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f)

        return matrix
    }

    /**
     * Checks if a point is within a "Good Zone" relative to a target.
     */
    fun isAlignmentCorrect(current: Float, target: Float, threshold: Float = 0.05f): Boolean {
        return Math.abs(current - target) <= threshold
    }
}
