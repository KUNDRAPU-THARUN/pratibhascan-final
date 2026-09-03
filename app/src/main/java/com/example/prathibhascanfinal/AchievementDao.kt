package com.example.prathibhascanfinal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Query("SELECT * FROM achievements WHERE userEmail = :email ORDER BY createdAt DESC")
    suspend fun getAchievementsForUser(email: String): List<Achievement>

    @Query("SELECT COUNT(*) FROM achievements WHERE userEmail = :email AND isVerified = 1")
    suspend fun getVerifiedCount(email: String): Int
}
