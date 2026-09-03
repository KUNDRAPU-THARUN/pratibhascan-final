package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String = "",
    val password: String = "",
    val role: String = "Athlete", // Athlete, Academy, Institution
    val fullName: String = "",
    val uniqueId: String = "",
    val gender: String = "Male",
    val nickname: String? = null,
    val bloodGroup: String? = null,
    val position: String? = null,
    val academy: String? = null,
    val institution: String? = null,
    val coachName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val emergencyContact: String? = null,
    val preferredLanguage: String = "en",
    val theme: String = "Dark",
    val units: String = "Metric",
    val privacy: String = "Public",
    val bio: String? = null,
    val age: String? = null,
    
    // --- Section A: Identity & Trust ---
    val aadhaarMasked: String? = null,
    val schoolEnrollmentId: String? = null,
    val parentMobile: String? = null,
    val dob: String? = null,
    val location: String? = null,
    val state: String? = null,
    val profilePicture: String? = null,
    
    // --- Section B: Biomechanics (AI Calibration) ---
    val height: String? = null,
    val weight: String? = null,
    val wingSpan: String? = null,
    val seatingHeight: String? = null,
    val dominantSide: String? = "Right",
    val experienceLevel: String? = "Beginner",
    
    // --- Section C: Skill Preference Matrix ---
    val primaryDiscipline: String? = null,
    val specificGames: String? = null, 
    val currentTier: String? = "School Level",
    
    // --- Section D: Performance & Gamification ---
    val totalXP: Int = 0,
    val streakCount: Int = 0,
    val podiumStreak: Int = 0,
    val evolutionTier: String = "Bronze",
    val nationalRank: Int = 0,
    val globalRank: Int = 0,
    val districtRank: Int = 0,
    val technicalImpactScore: Double = 0.0,
    val speedScore: Int = 70,
    val agilityScore: Int = 70,
    val staminaScore: Int = 70,
    val strengthScore: Int = 70,
    val topSpeed: String? = null,
    
    // --- Section E: Health Ecosystem ---
    val heartRate: Int = 72,
    val sleepHours: Double = 8.0,
    val hydrationLevel: Double = 2.5,
    val injuryStatus: String = "Fit",
    val healthCertificateUri: String? = null,
    val aiProteinReq: Double = 0.0,
    val aiCarbReq: Double = 0.0,
    
    // Media & Portfolio
    val highlightReel: String? = null,
    val achievements: String? = null,
    val certificates: String? = null
)
