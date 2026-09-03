package com.example.prathibhascanfinal.data

import android.graphics.Color
import com.example.prathibhascanfinal.R
import java.util.Date
import java.util.UUID

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Reminder",
    val isRead: Boolean = false,
    val actionType: String = "", // e.g., "ACADEMY", "TOURNAMENT", "COACH_FEEDBACK"
    val relatedId: String? = null
)

object NotificationCategories {
    const val ACADEMY = "Academy"
    const val TOURNAMENT = "Tournament"
    const val AI_COACH = "AI Coach"
    const val COACH = "Coach"
    const val CERTIFICATE = "Certificate"
    const val ACHIEVEMENT = "Achievement"
    const val PERFORMANCE = "Performance"
    const val HEALTH = "Health"
    const val REMINDER = "Reminder"

    fun getColor(category: String): Int = when (category) {
        ACADEMY -> Color.parseColor("#3B82F6") // Blue
        TOURNAMENT -> Color.parseColor("#F97316") // Orange
        AI_COACH -> Color.parseColor("#A855F7") // Purple
        COACH -> Color.parseColor("#22C55E") // Green
        CERTIFICATE -> Color.parseColor("#06B6D4") // Cyan
        ACHIEVEMENT -> Color.parseColor("#EAB308") // Gold
        PERFORMANCE -> Color.parseColor("#6366F1") // Indigo
        HEALTH -> Color.parseColor("#EF4444") // Red
        REMINDER -> Color.parseColor("#14B8A6") // Teal
        else -> Color.parseColor("#94A3B8") // Gray
    }

    fun getIcon(category: String): Int = when (category) {
        ACADEMY -> R.drawable.ic_academy_emblem
        TOURNAMENT -> R.drawable.ic_tournament_trophy
        AI_COACH -> R.drawable.ic_ai_badge
        COACH -> R.drawable.ic_coach_voice
        CERTIFICATE -> R.drawable.ic_precision_emblem
        ACHIEVEMENT -> R.drawable.ic_tournament_trophy
        PERFORMANCE -> R.drawable.ic_analytics_chart
        HEALTH -> R.drawable.ic_medical_health
        REMINDER -> R.drawable.ic_calendar_schedule
        else -> R.drawable.ic_ai_badge
    }
}

object DemoNotifications {
    fun getList(): List<AppNotification> {
        val now = System.currentTimeMillis()
        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        return listOf(
            AppNotification(
                title = "🏫 Academy Invitation",
                description = "Elite Sports Academy has invited you to join their Basketball Development Program.",
                category = NotificationCategories.ACADEMY,
                timestamp = now - 5 * minute,
                actionType = "ACADEMY_DETAILS"
            ),
            AppNotification(
                title = "🏆 Tournament Registration Accepted",
                description = "Congratulations! Your registration for the City Basketball Championship has been approved.",
                category = NotificationCategories.TOURNAMENT,
                timestamp = now - 2 * hour,
                actionType = "TOURNAMENT_DETAILS"
            ),
            AppNotification(
                title = "👨‍🏫 Coach Feedback Available",
                description = "Coach Arjun has reviewed your latest training video. Tap to view personalized feedback.",
                category = NotificationCategories.COACH,
                timestamp = now - 5 * hour,
                actionType = "AI_COACH_REPORT"
            ),
            AppNotification(
                title = "🤖 AI Coach Analysis Completed",
                description = "Your basketball shooting technique has been analyzed. New improvement suggestions are ready.",
                category = NotificationCategories.AI_COACH,
                timestamp = now - 1 * day,
                actionType = "AI_COACH_REPORT"
            ),
            AppNotification(
                title = "🎯 Training Reminder",
                description = "Today's basketball training session starts at 5:00 PM. Don't miss your practice.",
                category = NotificationCategories.REMINDER,
                timestamp = now - 4 * hour,
                actionType = "TRAINING_CALENDAR"
            ),
            AppNotification(
                title = "📈 Weekly Performance Report",
                description = "Great progress! Your shooting accuracy improved by 14% this week.",
                category = NotificationCategories.PERFORMANCE,
                timestamp = now - 1 * day,
                actionType = "PERFORMANCE_DASHBOARD"
            ),
            AppNotification(
                title = "🥇 Achievement Unlocked",
                description = "Congratulations! You earned the 'Consistent Performer' badge.",
                category = NotificationCategories.ACHIEVEMENT,
                timestamp = now - 2 * day,
                actionType = "ACHIEVEMENTS"
            ),
            AppNotification(
                title = "📜 Certificate Verified",
                description = "Your Basketball Participation Certificate has been successfully verified.",
                category = NotificationCategories.CERTIFICATE,
                timestamp = now - 3 * day,
                actionType = "CERTIFICATES"
            ),
            AppNotification(
                title = "🏀 New Tournament Nearby",
                description = "A new Basketball Tournament is available within 10 km of your location.",
                category = NotificationCategories.TOURNAMENT,
                timestamp = now - 4 * day,
                actionType = "TOURNAMENT_DETAILS"
            ),
            AppNotification(
                title = "🎓 Academy Profile Viewed",
                description = "An academy has viewed your athlete profile.",
                category = NotificationCategories.ACADEMY,
                timestamp = now - 5 * day,
                actionType = "ACADEMY_DETAILS"
            ),
            AppNotification(
                title = "👥 Team Invitation",
                description = "You have been invited to join the 'Rising Stars Basketball Team'.",
                category = NotificationCategories.ACADEMY,
                timestamp = now - 6 * day,
                actionType = "ACADEMY_DETAILS"
            ),
            AppNotification(
                title = "📅 Upcoming Match Reminder",
                description = "Reminder: Your Basketball League Match is tomorrow at 10:00 AM.",
                category = NotificationCategories.REMINDER,
                timestamp = now - 12 * hour,
                actionType = "TRAINING_CALENDAR"
            ),
            AppNotification(
                title = "❤️ Health Reminder",
                description = "Remember to complete today's nutrition and hydration plan.",
                category = NotificationCategories.HEALTH,
                timestamp = now - 8 * hour,
                actionType = "HEALTH_DASHBOARD"
            ),
            AppNotification(
                title = "🔥 Daily Motivation",
                description = "Champions are built one practice at a time. Complete today's training and keep improving!",
                category = NotificationCategories.PERFORMANCE,
                timestamp = now - 2 * hour,
                actionType = "PERFORMANCE_DASHBOARD"
            ),
            AppNotification(
                title = "🏅 Rank Improved",
                description = "Congratulations! Your Athlete Ranking has increased from #24 to #18.",
                category = NotificationCategories.PERFORMANCE,
                timestamp = now - 1 * day,
                actionType = "ATHLETE_RANKING"
            )
        )
    }
}
