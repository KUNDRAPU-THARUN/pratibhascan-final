package com.example.prathibhascanfinal

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Query("SELECT * FROM chat_conversations WHERE userEmail = :email ORDER BY lastTimestamp DESC")
    fun getConversationsForUser(email: String): Flow<List<ChatConversation>>

    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ChatConversation?

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}
