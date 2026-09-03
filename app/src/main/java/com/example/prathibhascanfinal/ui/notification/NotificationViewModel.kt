package com.example.prathibhascanfinal.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.data.AppNotification
import com.example.prathibhascanfinal.data.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repository = NotificationRepository(FirebaseFirestore.getInstance())

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private var userEmail: String = ""

    fun init(email: String) {
        this.userEmail = email
        viewModelScope.launch {
            repository.getNotifications(email).collectLatest { list ->
                _notifications.value = list
            }
        }
    }

    fun markAsRead(notification: AppNotification) {
        if (notification.isRead) return
        viewModelScope.launch {
            repository.markAsRead(userEmail, notification.id)
        }
    }
}
