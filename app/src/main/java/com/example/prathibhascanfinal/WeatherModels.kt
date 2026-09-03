package com.example.prathibhascanfinal

data class WeatherData(
    val temp: Double,
    val condition: String,
    val iconUrl: String? = null,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val rainChance: Int = 0,
    val aqi: Int = 0,
    val uv: Double = 0.0,
    val city: String = "Unknown",
    val sunrise: String = "--:--",
    val sunset: String = "--:--"
)

data class DashboardUIState(
    val greeting: String = "Welcome!",
    val time: String = "--:--",
    val dayDate: String = "----, -- ---",
    val weather: WeatherData? = null,
    val isLoadingWeather: Boolean = false,
    val weatherError: String? = null,
    val unreadNotifications: Int = 0,
    val enrolledSports: List<SportEnrollment> = emptyList(),
    val latestSession: AnalyticsSession? = null,
    val userProfile: User? = null,
    val accuracyTrend: List<Float> = emptyList(),
    val availableTournaments: List<Tournament> = emptyList(),
    val pendingInvitations: List<AcademyInvitation> = emptyList()
)
