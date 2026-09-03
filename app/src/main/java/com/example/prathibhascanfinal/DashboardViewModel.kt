package com.example.prathibhascanfinal

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

import com.example.prathibhascanfinal.data.repository.NotificationRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest

class DashboardViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val _uiState = MutableStateFlow(DashboardUIState())
    val uiState: StateFlow<DashboardUIState> = _uiState.asStateFlow()

    private val notificationRepository = NotificationRepository(FirebaseFirestore.getInstance())
    private val firestoreRepository = FirestoreRepository()
    private val academyRepository by lazy { com.example.prathibhascanfinal.data.repository.AcademyRepository(getApplication()) }

    private var timeJob: Job? = null
    private var enrollmentJob: Job? = null
    private var profileJob: Job? = null
    private var analyticsJob: Job? = null
    private var notificationJob: Job? = null
    private var invitationJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var discoveryJob: Job? = null

    init {
        startTimeUpdates()
        startDiscoveryUpdates()
    }

    private fun startDiscoveryUpdates() {
        academyRepository.startDiscoverySync()
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            academyRepository.getAllTournamentsFlow().collectLatest { list ->
                _uiState.update { it.copy(availableTournaments = list) }
            }
        }
    }

    fun applyForTournament(tournament: Tournament) {
        val user = uiState.value.userProfile ?: return
        viewModelScope.launch {
            val registration = TournamentRegistration(
                tournamentId = tournament.tournamentId,
                tournamentTitle = tournament.title,
                athleteEmail = user.email,
                athleteName = user.fullName,
                organizerAcademyId = tournament.academyId
            )
            val success = academyRepository.registerForTournament(registration)
            if (success) {
                // Trigger notification in Phase 10
            }
        }
    }

    fun respondToInvitation(invitation: AcademyInvitation, accepted: Boolean) {
        viewModelScope.launch {
            val status = if (accepted) "ACCEPTED" else "DECLINED"
            val success = academyRepository.updateInvitationStatus(invitation, status)
            if (success && accepted) {
                // Trigger Relationship Creation if needed
                notificationRepository.sendNotification(
                    userEmail = invitation.athleteEmail, // Should notify academy actually
                    title = "🤝 Invitation Accepted",
                    message = "You have joined ${invitation.academyName}!",
                    category = com.example.prathibhascanfinal.data.NotificationCategories.ACADEMY,
                    action = "ACADEMY_DETAILS"
                )
            }
        }
    }

    fun calculateTalentScore(user: User): Int {
        val base = user.technicalImpactScore.toInt().coerceIn(0, 100)
        val metricsAvg = (user.speedScore + user.agilityScore + user.staminaScore + user.strengthScore) / 4
        return (base * 0.4 + metricsAvg * 0.6).toInt().coerceIn(0, 100)
    }

    fun startEnrollmentUpdates(context: android.content.Context, email: String) {
        if (email.isEmpty()) return
        
        // Sync user profile from cloud to local Room
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch(Dispatchers.IO) {
            firestoreRepository.getUserFlow(email).collectLatest { cloudUser ->
                cloudUser?.let {
                    AppDatabase.getDatabase(context.applicationContext).userDao().insertUser(it)
                }
            }
        }

        academyRepository.startAthleteInvitationSync(email)
        invitationJob?.cancel()
        invitationJob = viewModelScope.launch {
            val db = AppDatabase.getDatabase(context.applicationContext)
            db.academyManagementDao().getInvitationsForAthleteFlow(email).collectLatest { list ->
                _uiState.update { it.copy(pendingInvitations = list) }
            }
        }

        enrollmentJob?.cancel()
        enrollmentJob = viewModelScope.launch {
            val db = AppDatabase.getDatabase(context.applicationContext)
            db.sportEnrollmentDao().getEnrollmentsForUser(email).collectLatest { list ->
                _uiState.update { it.copy(enrolledSports = list) }
            }
        }
        
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val db = AppDatabase.getDatabase(context.applicationContext)
            db.userDao().getUserFlow(email).collectLatest { user ->
                user?.let { u ->
                    // Move heavy ranking calculation to background thread
                    val updatedProfile = withContext(Dispatchers.Default) {
                        val athletes = db.userDao().getAllAthletes()
                        val sorted = athletes.sortedByDescending { it.totalXP }
                        val myRank = sorted.indexOfFirst { it.email == u.email } + 1
                        
                        u.copy(
                            nationalRank = myRank,
                            globalRank = myRank + 1200,
                            districtRank = (myRank / 2).coerceAtLeast(1)
                        )
                    }
                    _uiState.update { it.copy(userProfile = updatedProfile) }
                } ?: run {
                    _uiState.update { it.copy(userProfile = null) }
                }
            }
        }

        analyticsJob?.cancel()
        analyticsJob = viewModelScope.launch {
            val db = AppDatabase.getDatabase(context.applicationContext)
            launch {
                db.analyticsDao().getLatestSession(email).collectLatest { session ->
                    _uiState.update { it.copy(latestSession = session) }
                }
            }
            launch {
                db.analyticsDao().getAccuracyHistory(email).collectLatest { history ->
                    val trend = history.map { it / 100f }
                    _uiState.update { it.copy(accuracyTrend = trend) }
                }
            }
        }

        notificationJob?.cancel()
        notificationJob = viewModelScope.launch {
            notificationRepository.getNotifications(email).collectLatest { list ->
                val unreadCount = list.count { !it.isRead }
                _uiState.update { it.copy(unreadNotifications = unreadCount) }
            }
        }
    }

    private fun startTimeUpdates() {
        timeJob?.cancel()
        // Initial update immediately
        updateTimeAndGreeting()
        timeJob = viewModelScope.launch {
            while (true) {
                delay(60000) // Update every minute
                updateTimeAndGreeting()
            }
        }
    }

    fun updateTimeAndGreeting(userName: String = "User") {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // String resources are used via context in Activity usually, 
        // here we just provide the time strings for the UI to consume.
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        
        _uiState.update { it.copy(
            time = timeFormat.format(calendar.time),
            dayDate = dateFormat.format(calendar.time)
        )}
    }

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, weatherError = null) }
            try {
                // Simulate weather fetch for now - Replace with real API call
                delay(1500)
                val mockWeather = WeatherData(
                    temp = 28.5,
                    condition = "Sunny",
                    humidity = 63,
                    windSpeed = 12.4,
                    aqi = 42,
                    uv = 3.5,
                    city = "Visakhapatnam",
                    sunrise = "05:40 AM",
                    sunset = "06:25 PM"
                )
                _uiState.update { it.copy(weather = mockWeather, isLoadingWeather = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingWeather = false, weatherError = "Failed to load weather") }
            }
        }
    }
}
