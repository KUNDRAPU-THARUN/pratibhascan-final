package com.example.prathibhascanfinal

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MessageSender {
    USER, AI
}

enum class MessageStatus {
    SENDING, SENT, ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val messageType: String = "TEXT"
) {
    val isUser: Boolean get() = sender == MessageSender.USER
}

sealed interface ChatUiState {
    object Initial : ChatUiState
    object Loading : ChatUiState
    data class Success(val messages: List<ChatMessage>) : ChatUiState
    object Typing : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val text: String,
    val sender: String, // "USER" or "AI"
    val timestamp: Long,
    val status: String,
    val messageType: String
)

@Entity(tableName = "chat_conversations")
data class ChatConversation(
    @PrimaryKey val id: String,
    val userEmail: String,
    val sportName: String,
    val lastMessage: String,
    val lastTimestamp: Long
)
