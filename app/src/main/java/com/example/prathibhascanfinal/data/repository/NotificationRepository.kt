package com.example.prathibhascanfinal.data.repository

import com.example.prathibhascanfinal.data.AppNotification
import com.example.prathibhascanfinal.data.DemoNotifications
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(private val db: FirebaseFirestore) {

    fun getNotifications(userEmail: String): Flow<List<AppNotification>> = callbackFlow {
        if (userEmail.isEmpty()) {
            trySend(DemoNotifications.getList())
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userEmail)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(DemoNotifications.getList())
                } else {
                    val notifications = snapshot.toObjects(AppNotification::class.java)
                    trySend(notifications)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(userEmail: String, notificationId: String) {
        if (userEmail.isEmpty()) return
        try {
            db.collection("users")
                .document(userEmail)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendNotification(userEmail: String, title: String, message: String, category: String, action: String) {
        if (userEmail.isEmpty()) return
        try {
            val notification = AppNotification(
                title = title,
                description = message,
                category = category,
                actionType = action
            )
            db.collection("users")
                .document(userEmail)
                .collection("notifications")
                .document(notification.id)
                .set(notification)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
