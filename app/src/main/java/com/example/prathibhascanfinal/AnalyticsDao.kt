package com.example.prathibhascanfinal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: AnalyticsSession)

    @Query("SELECT * FROM analytics_sessions WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getSessionsForUser(email: String): Flow<List<AnalyticsSession>>

    @Query("SELECT * FROM analytics_sessions WHERE userEmail = :email ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSession(email: String): Flow<AnalyticsSession?>

    @Query("SELECT * FROM analytics_sessions WHERE userEmail = :email ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSessionSync(email: String): AnalyticsSession?

    @Query("SELECT AVG(accuracy) FROM analytics_sessions WHERE userEmail = :email")
    suspend fun getAverageAccuracy(email: String): Double

    @Query("SELECT accuracy FROM analytics_sessions WHERE userEmail = :email ORDER BY timestamp ASC LIMIT 10")
    fun getAccuracyHistory(email: String): Flow<List<Int>>

    @Query("SELECT accuracy FROM analytics_sessions WHERE userEmail = :email ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentAccuracySync(email: String): List<Int>

    @Query("SELECT SUM(durationSeconds) FROM analytics_sessions WHERE userEmail = :email")
    suspend fun getTotalPracticeTime(email: String): Long
}
