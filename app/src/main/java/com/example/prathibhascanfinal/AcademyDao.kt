package com.example.prathibhascanfinal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AcademyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademy(academy: Academy): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoach(coach: Coach)

    @Transaction
    suspend fun registerAcademyWithCoaches(academy: Academy, coaches: List<Coach>): Long {
        val academyId = insertAcademy(academy)
        coaches.forEach { coach ->
            insertCoach(coach.copy(academyId = academyId.toInt()))
        }
        return academyId
    }

    @Query("SELECT * FROM academies ORDER BY createdAt DESC")
    suspend fun getAllAcademies(): List<Academy>

    @Query("SELECT * FROM academies WHERE contactEmail = :email LIMIT 1")
    suspend fun getAcademyByEmail(email: String): Academy?

    @Query("SELECT * FROM academies WHERE contactEmail = :email LIMIT 1")
    fun getAcademyFlowByEmail(email: String): kotlinx.coroutines.flow.Flow<Academy?>

    @Query("SELECT * FROM academy_coaches WHERE academyId = :academyId")
    suspend fun getCoachesForAcademy(academyId: Int): List<Coach>
}
