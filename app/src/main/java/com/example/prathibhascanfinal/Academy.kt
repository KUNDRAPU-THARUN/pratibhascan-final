package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academies")
data class Academy(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val academyName: String = "",
    val logoUri: String? = null,
    val academyType: String = "Private", // Government / Private
    val registrationNumber: String = "",
    val contactEmail: String = "",
    val phoneNumber: String = "",
    val website: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val pinCode: String = "",
    val establishmentYear: Int = 2024,
    val description: String = "",
    val socialMediaLinks: String = "", // JSON string for multiple links
    val directorName: String = "",
    val membershipPlan: String = "Basic",
    val district: String = "",
    val profileCompletion: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Infrastructure & Domain
    val specializedDomains: String = "", // Comma-separated sports list
    val infrastructureTier: String = "",
    val profilePhotoUri: String? = null,
    val workingHours: String = "",
    val websiteUrl: String = "",
    val facebookUrl: String = "",
    val twitterUrl: String = "",
    val instagramUrl: String = "",
    
    // Stats & Tracking (Cached for performance, but usually recalculated)
    val athleteCount: String = "0",
    val coachCount: String = "0",
    
    // Step 2: AI Readiness
    val biomechanicalTools: String = "",
    val primaryMetrics: String = "",
    
    // Step 4: Demographics
    val ageGroups: String = "",
    val trainingObjectives: String = "",
    
    // Step 5: Compliance
    val dpoContact: String = "",
    val hasParentalConsentSystem: Boolean = false,
    
    // Attachments
    val licenseUri: String? = null,
    
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class SportCount(
    val sport: String,
    val count: Int
)

@Entity(tableName = "academy_sports")
data class AcademySport(
    @PrimaryKey(autoGenerate = true) val sportId: Int = 0,
    val academyId: Int = 0,
    val sportName: String = "",
    val category: String = "", // Indoor / Outdoor / Athletics / Other
    val trainingLevel: String = "Beginner",
    val ageGroups: String = "",
    val genderType: String = "Mixed", // Boys / Girls / Mixed
    val groundCount: Int = 0,
    val schedule: String = "",
    val maxCapacity: Int = 0,
    val status: String = "Active", // Active / Inactive
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "academy_facilities")
data class Facility(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val academyId: Int = 0,
    val name: String = "",
    val type: String = "", // Cricket Ground, Football Ground, etc.
    val sport: String = "",
    val capacity: Int = 0,
    val isIndoor: Boolean = false,
    val availability: String = "Available",
    val imageUrl: String? = null,
    val status: String = "Active"
)

@Entity(tableName = "academy_athletes")
data class AcademyAthlete(
    @PrimaryKey(autoGenerate = true) val athleteId: Int = 0,
    val academyId: Int = 0,
    val fullName: String = "",
    val photoUri: String? = null,
    val dob: String = "", // Date of Birth
    val age: Int = 0,
    val gender: String = "",
    val nationality: String = "Indian",
    val bloodGroup: String = "",
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val bmi: Double = 0.0,
    
    // Contact & Emergency
    val parentName: String = "",
    val parentPhone: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val address: String = "",
    val identityId: String = "", // Aadhaar / Student ID
    
    // Academy & Sport
    val admissionNumber: String = "",
    val joiningDate: Long = System.currentTimeMillis(),
    val sportDomain: String = "", // Primary Sport
    val secondarySport: String? = null,
    val selectedGame: String = "",
    val position: String = "",
    val skillLevel: String = "Beginner",
    val experienceYears: Int = 0,
    val dominantSide: String = "Right", // Right/Left Hand/Foot
    val currentRanking: String = "N/A",
    val personalBest: String = "",
    val coachId: Int? = null,
    
    // Assessment & Medical
    val medicalHistory: String = "",
    val currentInjury: String? = null,
    val documentUris: String = "", // JSON String for docs
    
    // AI Performance Metrics
    val trainingHours: Double = 0.0,
    val attendancePercentage: Double = 0.0,
    val techniqueScore: Int = 0,
    val fitnessScore: Int = 0,
    val speed: Double = 0.0,
    val strength: Double = 0.0,
    val endurance: Double = 0.0,
    val flexibility: Double = 0.0,
    val aiScore: Double = 0.0,
    val consistency: Double = 0.0,
    
    // Status & Verification
    val membershipStatus: String = "Active", // Active, Inactive
    val verificationStatus: String = "Pending", // Pending, Verified, Rejected
    val isActive: Boolean = true,
    val teamId: Int? = null,
    val jerseyNumber: String = "",
    val role: String = "Player", // "Captain", "Vice-Captain", "Player"
    val performanceLevel: String = "Intermediate" // "Beginner", "Intermediate", "Advanced", "Elite"
)

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true) val teamId: Int = 0,
    val academyId: Int = 0,
    val teamName: String = "",
    val ageGroup: String = "",
    val sport: String = "",
    val category: String = "",
    val gender: String = "Mixed",
    val coachId: Int = 0,
    val coachName: String? = null,
    val captainId: Int? = null,
    val viceCaptainId: Int? = null,
    val description: String? = null,
    val status: String = "Active",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "academy_coaches")
data class Coach(
    @PrimaryKey(autoGenerate = true) val coachId: Int = 0,
    val academyId: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val specialization: String = "",
    val qualification: String = "",
    val experienceYears: Int = 0,
    val certificationUri: String? = null,
    val status: String = "Active", // Active, Inactive
    val salary: Double = 0.0,
    val attendancePercentage: Double = 0.0,
    
    // Recruitment Portfolio Data
    val isLookingForWork: Boolean = false,
    val techSkills: String = "", // Comma separated tags
    val portfolioVideoUri: String? = null
)

@Entity(tableName = "equipment_inventory")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val equipmentId: Int = 0,
    val academyId: Int = 0,
    val name: String = "",
    val category: String = "", // Sport category
    val totalQuantity: Int = 0,
    val availableStock: Int = 0,
    val condition: String = "New",
    val purchaseDate: Long = System.currentTimeMillis(),
    val maintenanceDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "training_slots")
data class TrainingSlot(
    @PrimaryKey(autoGenerate = true) val slotId: Int = 0,
    val academyId: Int = 0,
    val groundName: String = "",
    val coachId: Int = 0,
    val teamId: Int = 0,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val sessionType: String = "Practice",
    val status: String = "Scheduled" // Scheduled, Completed, Cancelled
)

@Entity(tableName = "attendance_records")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val attendanceId: Int = 0,
    val academyId: Int = 0,
    val entityType: String = "Athlete", // Athlete, Coach
    val entityId: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val isPresent: Boolean = true,
    val sessionType: String = "Daily", // Daily, Training, Competition
    val qrCodeUsed: Boolean = false
)

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true) val tournamentId: Int = 0,
    val academyId: Int = 0,
    val title: String = "",
    val level: String = "Internal", // Internal, District, State, National
    val sport: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val venue: String = "",
    val description: String = "",
    val entryFee: String = "Free",
    val registrationDeadline: Long = 0,
    val contactInfo: String = ""
)

@Entity(tableName = "tournament_registrations")
data class TournamentRegistration(
    @PrimaryKey(autoGenerate = true) val registrationId: Int = 0,
    val tournamentId: Int = 0,
    val tournamentTitle: String = "",
    val athleteEmail: String = "",
    val athleteName: String = "",
    val organizerAcademyId: Int = 0,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val appliedAt: Long = System.currentTimeMillis(),
    val rejectionReason: String? = null
)

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true) val matchId: Int = 0,
    val tournamentId: Int = 0,
    val team1Id: Int = 0,
    val team2Id: Int = 0,
    val team1Score: String = "0",
    val team2Score: String = "0",
    val winnerId: Int? = null,
    val matchTime: Long = 0,
    val status: String = "Upcoming" // Upcoming, Live, Finished
)

@Entity(tableName = "academy_invitations")
data class AcademyInvitation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val academyId: Int,
    val academyName: String,
    val athleteEmail: String,
    val athleteName: String,
    val sport: String,
    val message: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val athleteId: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val injuryType: String = "",
    val recoveryPlan: String = "",
    val doctorReportUri: String? = null,
    val fitnessClearance: Boolean = false,
    val physiotherapyNotes: String = ""
)

@Entity(tableName = "diet_plans")
data class DietPlan(
    @PrimaryKey(autoGenerate = true) val planId: Int = 0,
    val athleteId: Int = 0,
    val caloriesTarget: Int = 2500,
    val hydrationTarget: Double = 3.0,
    val mealPlanDetails: String = "",
    val supplements: String = "",
    val goals: String = ""
)

@Entity(tableName = "payroll_records")
data class Payroll(
    @PrimaryKey(autoGenerate = true) val payrollId: Int = 0,
    val academyId: Int = 0,
    val coachId: Int = 0,
    val monthYear: String = "",
    val baseSalary: Double = 0.0,
    val bonus: Double = 0.0,
    val totalPaid: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis(),
    val status: String = "Paid"
)
