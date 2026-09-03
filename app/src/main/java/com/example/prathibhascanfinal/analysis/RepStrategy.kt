package com.example.prathibhascanfinal.analysis

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.example.prathibhascanfinal.ExerciseType
import com.example.prathibhascanfinal.ExerciseState

interface RepStrategy {
    val exerciseType: ExerciseType
    
    /**
     * Processes landmarks and returns the new state and feedback
     */
    fun process(
        landmarks: List<NormalizedLandmark>,
        currentState: ExerciseState,
        onRepComplete: () -> Unit
    ): AnalysisResult

    /**
     * Generates a "Perfect Form" ghost pose based on the current user position and state
     */
    fun generateGhost(
        userLandmarks: List<NormalizedLandmark>,
        currentState: ExerciseState
    ): List<NormalizedLandmark>
    
    /**
     * Calculates similarity score (0-100)
     */
    fun calculateSimilarity(
        userLandmarks: List<NormalizedLandmark>,
        ghostLandmarks: List<NormalizedLandmark>
    ): Int
}

data class AnalysisResult(
    val newState: ExerciseState,
    val feedback: String,
    val currentAngle: Double = 0.0,
    val currentAngles: Map<String, Double> = emptyMap(),
    val incorrectJoints: Set<Int> = emptySet(),
    val warningJoints: Set<Int> = emptySet(),
    val alignmentScore: Int = 0,
    val isIdealForm: Boolean = false,
    val confidence: Float = 0f
)
