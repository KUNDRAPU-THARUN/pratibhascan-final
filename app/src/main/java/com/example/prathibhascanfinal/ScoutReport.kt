package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "scout_reports")
data class ScoutReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentEmail: String,
    val studentName: String,
    val teacherEmail: String,
    val institutionName: String,
    val targetAcademyId: Int,
    val academyName: String,
    val sportCategory: String,
    val recommendationNote: String,
    val aiScore: Double,
    val status: String = "Pending", // Pending, Reviewed, TrialInvited, Recruited, Rejected
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ScoutReportDao {
    @Insert
    suspend fun insertReport(report: ScoutReport)

    @Query("SELECT * FROM scout_reports WHERE targetAcademyId = :academyId ORDER BY timestamp DESC")
    suspend fun getReportsForAcademy(academyId: Int): List<ScoutReport>

    @Query("SELECT * FROM scout_reports WHERE teacherEmail = :teacherEmail ORDER BY timestamp DESC")
    suspend fun getReportsByTeacher(teacherEmail: String): List<ScoutReport>

    @Query("SELECT * FROM scout_reports WHERE studentEmail = :studentEmail ORDER BY timestamp DESC")
    suspend fun getReportsForStudent(studentEmail: String): List<ScoutReport>

    @Update
    suspend fun updateReport(report: ScoutReport)
}
