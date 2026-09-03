package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "institutions")
data class Institution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val institutionName: String = "",
    val institutionType: String = "School", // School / College / University
    val ownershipType: String = "Private", // Government / Private
    val logoUri: String? = null,
    val coverImageUri: String? = null,
    val boardAffiliation: String = "", // e.g. CBSE, ICSE
    val affiliationCode: String = "",
    val affiliationNumber: String = "",
    val registrationNumber: String = "",
    val establishedYear: String = "",
    val academicYear: String = "2026-27",
    val campusAddress: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = "India",
    val postalCode: String = "",
    val contactEmail: String = "",
    val officialPhone: String = "",
    val alternatePhone: String = "",
    val website: String = "",
    val principalName: String = "",
    val sportsCoordinatorName: String = "",
    val description: String = "",
    val socialLinks: String = "", // JSON or comma-separated
    
    // Detailed Info
    val regCertNumber: String = "",
    val recognitionStatus: String = "Recognized",
    val deptApproval: String = "",
    val accreditation: String = "",
    val campusArea: String = "",
    val buildingCount: Int = 0,
    
    val studentCountTier: String = "",
    val sportsFacilities: String = "", // Comma separated
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "institution_students")
data class Student(
    @PrimaryKey(autoGenerate = true) val studentId: Int = 0,
    val institutionId: Int = 0,
    val fullName: String = "",
    val photoUri: String? = null,
    val rollNumber: String = "",
    val grade: String = "", // Class
    val section: String = "",
    val department: String = "", // For Colleges
    val academicYear: String = "",
    val dob: String = "",
    val age: Int = 0,
    val gender: String = "",
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val bloodGroup: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val studentIdentityId: String = "", // Aadhaar / Student ID
    val selectedSport: String = "",
    val selectedGame: String = "",
    val medicalHistory: String = "",
    
    // --- Health & Fit India ---
    val healthStatus: String = "Good",
    val healthCertificateUri: String? = null,
    val lastCheckup: Long = System.currentTimeMillis(),
    val bmi: Double = 0.0,
    val sprintScore: Double = 0.0,
    val balanceScore: Double = 0.0,
    val flexibilityScore: Double = 0.0,
    val strengthScore: Double = 0.0,
    val enduranceScore: Double = 0.0,
    
    // Performance Scores (Aggregated)
    val boardExamReadiness: Int = 0, 
    val talentScoutingFlag: Boolean = false,
    val aiTechniqueScore: Int = 0,
    val aiFitnessScore: Int = 0,
    val aiPostureScore: Int = 0,
    val attendancePercentage: Double = 0.0,
    val scholarshipEligible: Boolean = false
)

@Entity(tableName = "institution_teams")
data class InstitutionTeam(
    @PrimaryKey(autoGenerate = true) val teamId: Int = 0,
    val institutionId: Int = 0,
    val teamName: String = "",
    val teamType: String = "School Team", // School Team, House, Class, Practice Group, College
    val sport: String = "",
    val category: String = "", // e.g. Under-19, Under-17
    val ageGroup: String = "", // e.g. Under-19
    val gender: String = "Male", // Male, Female, Mixed
    val coachId: Int? = null,
    val coachName: String? = null,
    val captainId: Int? = null,
    val viceCaptainId: Int? = null,
    val teacherInCharge: String? = null,
    val description: String? = null,
    val status: String = "Active", // Active / Inactive
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "institution_team_members")
data class InstitutionTeamMember(
    @PrimaryKey(autoGenerate = true) val memberId: Int = 0,
    val teamId: Int = 0,
    val studentId: Int = 0,
    val institutionId: Int = 0,
    val jerseyNumber: String = "",
    val position: String = "",
    val role: String = "Player", // "Captain", "Vice-Captain", "Player"
    val performanceStatus: String = "Good", // Excellent, Good, Average
    val notes: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "institution_equipment")
data class InstitutionEquipment(
    @PrimaryKey(autoGenerate = true) val equipmentId: Int = 0,
    val institutionId: Int = 0,
    val name: String = "",
    val category: String = "Equipment", // Equipment, Uniform, Shoe, Training Kit
    val totalQuantity: Int = 0,
    val issuedQuantity: Int = 0,
    val purchaseDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "institution_bookings")
data class GroundBooking(
    @PrimaryKey(autoGenerate = true) val bookingId: Int = 0,
    val institutionId: Int = 0,
    val facilityName: String = "", // Playground, Indoor Court, Pool, Gym
    val teamId: Int? = null,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val purpose: String = "Practice"
)

@Entity(tableName = "practical_exams")
data class PracticalExam(
    @PrimaryKey(autoGenerate = true) val examId: Int = 0,
    val institutionId: Int = 0,
    val examTitle: String = "",
    val grade: String = "",
    val date: Long = System.currentTimeMillis(),
    val maxMarks: Int = 50
)

@Entity(tableName = "student_exam_marks")
data class ExamMark(
    @PrimaryKey(autoGenerate = true) val markId: Int = 0,
    val examId: Int = 0,
    val studentId: Int = 0,
    val marksObtained: Double = 0.0,
    val aiReportUri: String? = null,
    val videoProofUri: String? = null,
    val teacherRemarks: String = ""
)

@Entity(tableName = "institution_tournaments")
data class InstitutionTournament(
    @PrimaryKey(autoGenerate = true) val tournamentId: Int = 0,
    val institutionId: Int = 0,
    val title: String = "",
    val level: String = "School", // School, Inter-school, District, State, National
    val sport: String = "",
    val startDate: Long = 0,
    val venue: String = ""
)

@Entity(tableName = "institution_sports")
data class InstitutionSport(
    @PrimaryKey(autoGenerate = true) val sportId: Int = 0,
    val institutionId: Int = 0,
    val sportName: String = "",
    val category: String = "", // Indoor / Outdoor / Athletics / Other
    val targetAge: String = "",
    val genderType: String = "Mixed", // Boys / Girls / Mixed
    val groundAvailable: Boolean = true,
    val equipmentReady: Boolean = true,
    val status: String = "Active", // Active / Inactive
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "institution_teachers")
data class InstitutionTeacher(
    @PrimaryKey(autoGenerate = true) val teacherId: Int = 0,
    val institutionId: Int = 0,
    val fullName: String = "",
    val specialization: String = "",
    val email: String = "",
    val phone: String = "",
    val qualification: String = "",
    val experienceYears: Int = 0,
    val joiningDate: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false
)

@Entity(tableName = "institution_medical_records")
data class InstitutionMedicalRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val institutionId: Int = 0,
    val studentId: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val injuryType: String = "",
    val severity: String = "Minor", // Minor, Moderate, Severe
    val physiotherapyNotes: String = "",
    val clearanceStatus: String = "Pending" // Pending, Cleared, Rest
)

@Entity(tableName = "institution_training_slots")
data class InstitutionTrainingSlot(
    @PrimaryKey(autoGenerate = true) val slotId: Int = 0,
    val institutionId: Int = 0,
    val sportName: String = "",
    val teamId: Int? = null,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val dayOfWeek: String = "", // Monday, Tuesday...
    val venue: String = ""
)
