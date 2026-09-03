package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val tournamentName: String,
    val eventYear: String,
    val position: String, // e.g. "Winner", "Participant"
    val discipline: String,
    val isVerified: Boolean = false,
    val certificateUri: String? = null,
    val proofVideoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
