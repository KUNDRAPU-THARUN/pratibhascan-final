package com.example.prathibhascanfinal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

@Dao
interface SportEnrollmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enrollInSport(enrollment: SportEnrollment)

    @Query("SELECT * FROM sport_enrollments WHERE userEmail = :email")
    fun getEnrollmentsForUser(email: String): Flow<List<SportEnrollment>>

    @Query("SELECT * FROM sport_enrollments WHERE userEmail = :email AND sportName = :sportName LIMIT 1")
    suspend fun getEnrollmentForSport(email: String, sportName: String): SportEnrollment?

    @Query("SELECT * FROM sport_enrollments WHERE sportName = :sportName")
    suspend fun getAthletesBySport(sportName: String): List<SportEnrollment>
}
