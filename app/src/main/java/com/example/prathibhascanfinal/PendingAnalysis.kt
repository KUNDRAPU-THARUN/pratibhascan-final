package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_analyses")
data class PendingAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoUri: String,
    val exerciseType: String,
    val status: String = "PENDING", // PENDING, PROCESSING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null
)
