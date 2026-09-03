package com.example.prathibhascanfinal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface InstitutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitution(institution: Institution): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Transaction
    suspend fun enrollRoster(institutionId: Int, roster: List<Student>) {
        roster.forEach { student ->
            insertStudent(student.copy(institutionId = institutionId))
        }
    }

    @Query("SELECT * FROM institutions ORDER BY createdAt DESC")
    suspend fun getAllInstitutions(): List<Institution>

    @Query("SELECT * FROM institutions WHERE contactEmail = :email")
    fun getInstitutionFlowByEmail(email: String): kotlinx.coroutines.flow.Flow<Institution?>

    @Query("SELECT * FROM institutions WHERE contactEmail = :email")
    suspend fun getInstitutionByEmail(email: String): Institution?

    @Query("SELECT * FROM institution_students WHERE institutionId = :institutionId")
    suspend fun getStudentsByInstitution(institutionId: Int): List<Student>

    @Query("SELECT * FROM institution_students WHERE parentPhone = :parentPhone")
    suspend fun getStudentByParentPhone(parentPhone: String): Student?
}
