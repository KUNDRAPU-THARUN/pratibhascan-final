package com.example.prathibhascanfinal

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: PendingAnalysis)

    @Query("SELECT * FROM pending_analyses ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<PendingAnalysis>>

    @Query("SELECT * FROM pending_analyses WHERE status = 'PENDING' LIMIT 1")
    suspend fun getNextPending(): PendingAnalysis?

    @Update
    suspend fun update(analysis: PendingAnalysis)

    @Delete
    suspend fun delete(analysis: PendingAnalysis)
}
