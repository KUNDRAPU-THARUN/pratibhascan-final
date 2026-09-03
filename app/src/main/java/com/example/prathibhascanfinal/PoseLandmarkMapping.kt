package com.example.prathibhascanfinal

/**
 * Centralized mapping for MediaPipe Pose 33 Landmarks.
 * This ensures consistency across Detector, Smoother, Renderer and AI Feedback.
 */
object PoseLandmarkMapping {
    
    data class LandmarkInfo(
        val index: Int,
        val name: String,
        val parentIndex: Int? = null,
        val drawingPriority: Int = 1
    )

    val LANDMARKS = mapOf(
        0 to LandmarkInfo(0, "Nose", null, 2),
        1 to LandmarkInfo(1, "Left Eye Inner", 0),
        2 to LandmarkInfo(2, "Left Eye", 1),
        3 to LandmarkInfo(3, "Left Eye Outer", 2),
        4 to LandmarkInfo(4, "Right Eye Inner", 0),
        5 to LandmarkInfo(5, "Right Eye", 4),
        6 to LandmarkInfo(6, "Right Eye Outer", 5),
        7 to LandmarkInfo(7, "Left Ear", 3),
        8 to LandmarkInfo(8, "Right Ear", 6),
        9 to LandmarkInfo(9, "Mouth Left", 0),
        10 to LandmarkInfo(10, "Mouth Right", 0),
        11 to LandmarkInfo(11, "Left Shoulder", 12, 3),
        12 to LandmarkInfo(12, "Right Shoulder", 11, 3),
        13 to LandmarkInfo(13, "Left Elbow", 11, 3),
        14 to LandmarkInfo(14, "Right Elbow", 12, 3),
        15 to LandmarkInfo(15, "Left Wrist", 13, 3),
        16 to LandmarkInfo(16, "Right Wrist", 14, 3),
        17 to LandmarkInfo(17, "Left Pinky", 15),
        18 to LandmarkInfo(18, "Right Pinky", 16),
        19 to LandmarkInfo(19, "Left Index", 15),
        20 to LandmarkInfo(20, "Right Index", 16),
        21 to LandmarkInfo(21, "Left Thumb", 15),
        22 to LandmarkInfo(22, "Right Thumb", 16),
        23 to LandmarkInfo(23, "Left Hip", 24, 3),
        24 to LandmarkInfo(24, "Right Hip", 23, 3),
        25 to LandmarkInfo(25, "Left Knee", 23, 3),
        26 to LandmarkInfo(26, "Right Knee", 24, 3),
        27 to LandmarkInfo(27, "Left Ankle", 25, 3),
        28 to LandmarkInfo(28, "Right Ankle", 26, 3),
        29 to LandmarkInfo(29, "Left Heel", 27),
        30 to LandmarkInfo(30, "Right Heel", 28),
        31 to LandmarkInfo(31, "Left Foot Index", 27),
        32 to LandmarkInfo(32, "Right Foot Index", 28)
    )

    fun getRequiredJoints(type: ExerciseType): List<Int> {
        return when (type) {
            ExerciseType.SQUATS -> listOf(24, 26, 28, 23, 25, 27) // Hips, Knees, Ankles
            ExerciseType.PUSH_UPS -> listOf(12, 14, 16, 11, 13, 15, 24, 28) // Arms + Body alignment
            ExerciseType.LUNGES -> listOf(23, 25, 27, 24, 26, 28)
            ExerciseType.PLANKS -> listOf(12, 24, 28, 11, 23, 27)
            else -> listOf(11, 12, 23, 24, 25, 26) // Default major joints
        }
    }

    /**
     * Anatomically correct skeleton connections for all 33 landmarks
     */
    val SKELETON_CONNECTIONS = listOf(
        // Face (Eyes, Nose, Mouth)
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 7), // Left eye and ear
        Pair(0, 4), Pair(4, 5), Pair(5, 6), Pair(6, 8), // Right eye and ear
        Pair(9, 10), // Mouth
        
        // Torso
        Pair(11, 12), // Shoulders
        Pair(11, 23), // Left shoulder to hip
        Pair(12, 24), // Right shoulder to hip
        Pair(23, 24), // Hips
        
        // Left Arm
        Pair(11, 13), // Shoulder to elbow
        Pair(13, 15), // Elbow to wrist
        Pair(15, 17), Pair(15, 19), Pair(15, 21), // Wrist to hand points
        Pair(17, 19), // Pinky to index
        
        // Right Arm
        Pair(12, 14), // Shoulder to elbow
        Pair(14, 16), // Elbow to wrist
        Pair(16, 18), Pair(16, 20), Pair(16, 22), // Wrist to hand points
        Pair(18, 20), // Pinky to index
        
        // Left Leg
        Pair(23, 25), // Hip to knee
        Pair(25, 27), // Knee to ankle
        Pair(27, 29), // Ankle to heel
        Pair(27, 31), // Ankle to foot index
        Pair(29, 31), // Heel to foot index
        
        // Right Leg
        Pair(24, 26), // Hip to knee
        Pair(26, 28), // Knee to ankle
        Pair(28, 30), // Ankle to heel
        Pair(28, 32), // Ankle to foot index
        Pair(30, 32)  // Heel to foot index
    )

    fun getName(index: Int): String = LANDMARKS[index]?.name ?: "Unknown"
    
    fun getJointGroup(index: Int): String {
        return when (index) {
            in 0..10 -> "Face"
            in 11..16 -> "Arms"
            in 17..22 -> "Hands"
            in 23..24 -> "Hips"
            in 25..32 -> "Legs"
            else -> "Other"
        }
    }
}
