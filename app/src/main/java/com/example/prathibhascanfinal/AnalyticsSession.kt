package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_sessions")
data class AnalyticsSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val userEmail: String,
    val sportName: String,
    val exerciseType: String,
    val accuracy: Int,
    val techScore: Int,
    val repCount: Int,
    val durationSeconds: Long,
    val calories: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val integrityScore: Int = 100, // 0-100 base
    val verificationStatus: String = "Verified", // Verified, Flagged
    val aiFeedback: String? = null
)
