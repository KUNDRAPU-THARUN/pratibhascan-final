package com.example.prathibhascanfinal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class, Academy::class, Coach::class, Institution::class, Student::class, 
        Achievement::class, SportEnrollment::class, ScoutReport::class,
        AcademyAthlete::class, Team::class, Equipment::class, TrainingSlot::class, 
        Attendance::class, Tournament::class, Match::class, MedicalRecord::class, 
        DietPlan::class, Payroll::class, Facility::class, AcademySport::class,
        InstitutionTeam::class, InstitutionTeamMember::class, InstitutionEquipment::class, GroundBooking::class,
        PracticalExam::class, ExamMark::class, InstitutionTournament::class, InstitutionSport::class,
        InstitutionTeacher::class, InstitutionMedicalRecord::class, InstitutionTrainingSlot::class,
        CricketProfile::class, FootballProfile::class, BasketballProfile::class,
        RacketSportProfile::class, ChessProfile::class, AthleticsProfile::class,
        SwimmingProfile::class, CombatSportProfile::class, SkillSportProfile::class,
        TeamSportProfile::class, PrecisionSportProfile::class, AnalyticsSession::class,
        ChatMessageEntity::class, ChatConversation::class, TournamentRegistration::class,
        PendingAnalysis::class, AcademyInvitation::class
    ],
    version = 39,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun academyDao(): AcademyDao
    abstract fun institutionDao(): InstitutionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun sportEnrollmentDao(): SportEnrollmentDao
    abstract fun scoutReportDao(): ScoutReportDao
    abstract fun academyManagementDao(): AcademyManagementDao
    abstract fun institutionManagementDao(): InstitutionManagementDao
    abstract fun sportProfileDao(): SportProfileDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun chatDao(): ChatDao
    abstract fun pendingAnalysisDao(): PendingAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pratibha_global_master",
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
