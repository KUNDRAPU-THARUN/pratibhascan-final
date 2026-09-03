package com.example.prathibhascanfinal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChatHistoryRepository(private val chatDao: ChatDao) {

    private val firestore = FirebaseFirestore.getInstance()

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getConversationsForUser(email: String): Flow<List<ChatConversation>> {
        return chatDao.getConversationsForUser(email)
    }

    suspend fun saveMessage(conversationId: String, message: ChatMessage) = withContext(Dispatchers.IO) {
        // Save to Room
        chatDao.insertMessage(message.toEntity(conversationId))
        
        // Update conversation last message
        val conversation = chatDao.getConversationById(conversationId)
        conversation?.let {
            chatDao.insertConversation(it.copy(
                lastMessage = message.text,
                lastTimestamp = message.timestamp
            ))
        }

        // Sync to Firebase (optional/background)
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(message.id)
                .set(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createConversation(userEmail: String, sportName: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val conversation = ChatConversation(
            id = id,
            userEmail = userEmail,
            sportName = sportName,
            lastMessage = "Start coaching",
            lastTimestamp = System.currentTimeMillis()
        )
        chatDao.insertConversation(conversation)
        return id
    }

    private fun ChatMessageEntity.toDomainModel() = ChatMessage(
        id = id,
        text = text,
        sender = MessageSender.valueOf(sender),
        timestamp = timestamp,
        status = MessageStatus.valueOf(status),
        messageType = messageType
    )

    private fun ChatMessage.toEntity(conversationId: String) = ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        text = text,
        sender = sender.name,
        timestamp = timestamp,
        status = status.name,
        messageType = messageType
    )
}
