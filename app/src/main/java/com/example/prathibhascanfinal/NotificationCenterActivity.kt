package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.data.AppNotification
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.example.prathibhascanfinal.ui.notification.NotificationAdapter
import com.example.prathibhascanfinal.ui.notification.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationCenterActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_center)
        
        setupUI()
        observeNotifications()
        
        val session = SessionManager(this)
        notificationViewModel.init(session.getEmail() ?: "")
    }

    private fun setupUI() {
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()
        
        findViewById<ImageView>(R.id.iv_back_notifications)?.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_notifications)
        adapter = NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            notificationViewModel.notifications.collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    private fun handleNotificationClick(notification: AppNotification) {
        notificationViewModel.markAsRead(notification)
        
        val intent = when (notification.actionType) {
            "ACADEMY_DETAILS" -> Intent(this, AcademyRegistrationActivity::class.java)
            "TOURNAMENT_DETAILS" -> Intent(this, AcademyTournamentActivity::class.java)
            "AI_COACH_REPORT" -> Intent(this, AICoachActivity::class.java)
            "TRAINING_CALENDAR" -> Intent(this, AcademyTrainingCalendarActivity::class.java)
            "CERTIFICATES", "ACHIEVEMENTS" -> Intent(this, AchievementVaultActivity::class.java)
            "PERFORMANCE_DASHBOARD", "ATHLETE_RANKING" -> {
                val i = Intent(this, DashboardActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                i.putExtra("OPEN_TAB", if (notification.actionType == "ATHLETE_RANKING") 3 else 2)
                i
            }
            "HEALTH_DASHBOARD" -> Intent(this, HealthUpdateActivity::class.java)
            else -> null
        }

        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Notification: ${notification.title}", Toast.LENGTH_SHORT).show()
        }
    }
}

