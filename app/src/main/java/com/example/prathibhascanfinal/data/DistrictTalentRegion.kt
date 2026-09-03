package com.example.prathibhascanfinal.data

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng

data class DistrictTalentRegion(
    val id: String,
    val name: String,
    val center: LatLng,
    val athleteCount: Int = 0,
    val verifiedCount: Int = 0,
    val activeCount: Int = 0,
    val avgPerformanceScore: Double = 0.0,
    val avgAiAssessmentScore: Double = 0.0,
    val tournamentParticipation: Int = 0,
    val trend: String = "Stable" // Improving, Stable, Declining
) {
    /**
     * Calculates a normalized talent score (0-100) based on weighted metrics.
     */
    fun calculateScore(): Int {
        if (athleteCount == 0) return 0
        
        // Weighting factors
        val verificationWeight = 0.25
        val performanceWeight = 0.30
        val aiWeight = 0.20
        val participationWeight = 0.15
        val activityWeight = 0.10
        
        // Normalized sub-scores
        val vScore = (verifiedCount.toDouble() / athleteCount.coerceAtLeast(1) * 100).coerceIn(0.0, 100.0)
        val pScore = avgPerformanceScore.coerceIn(0.0, 100.0)
        val aScore = avgAiAssessmentScore.coerceIn(0.0, 100.0)
        val partScore = (tournamentParticipation * 10.0).coerceIn(0.0, 100.0) // Heuristic
        val actScore = (activeCount.toDouble() / athleteCount.coerceAtLeast(1) * 100).coerceIn(0.0, 100.0)
        
        return (vScore * verificationWeight +
                pScore * performanceWeight +
                aScore * aiWeight +
                partScore * participationWeight +
                actScore * activityWeight).toInt().coerceIn(0, 100)
    }

    fun getTalentLevel(): TalentLevel {
        val score = calculateScore()
        return when {
            score < 40 -> TalentLevel.DEVELOPING
            score < 70 -> TalentLevel.ACTIVE
            else -> TalentLevel.STRONG
        }
    }

    enum class TalentLevel(val label: String, val color: Int) {
        DEVELOPING("Developing", Color.parseColor("#3B82F6")), // Blue
        ACTIVE("Active", Color.parseColor("#10B981")),     // Green
        STRONG("Strong", Color.parseColor("#FBBF24"))      // Yellow
    }
}
