package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "sport_enrollments")
data class SportEnrollment(
    @PrimaryKey(autoGenerate = true) val enrollmentId: Int = 0,
    val userEmail: String,
    val sportName: String,
    val sportCategory: String = "", 
    val athleteSportId: String = "", // PR-SPORT-XXXX
    val blockchainSportId: String = "", // BC-SPORT-XXXX
    val registrationStatus: String = "Registered", // Registered, Pending, Verified
    val positionStyle: String = "",
    val yearsExperience: String = "",
    val bestPerformance: String = "",
    val specializedData: String? = null,
    val achievementUri: String? = null,
    val proofVideoUri: String? = null,
    val scoutAccessGranted: Boolean = true,
    val syncStatus: String = "Pending", // Synced, Pending
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable
