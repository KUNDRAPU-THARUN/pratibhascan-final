package com.example.prathibhascanfinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DiscoveryState(
    val athletes: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSport: String? = null,
    val searchQuery: String = ""
)

class AthleteDiscoveryViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val firestoreRepo = FirestoreRepository()
    private val academyRepo = AcademyRepository(application)
    
    private val _uiState = MutableStateFlow(DiscoveryState())
    val uiState: StateFlow<DiscoveryState> = _uiState.asStateFlow()

    private val _sportFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        startDiscovery()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun startDiscovery() {
        viewModelScope.launch {
            combine(
                _sportFilter,
                _searchQuery
            ) { sport, query ->
                Pair(sport, query)
            }.flatMapLatest { (sport, query) ->
                _uiState.update { it.copy(isLoading = true, selectedSport = sport, searchQuery = query) }
                firestoreRepo.getDiscoveryAthletesFlow(sport)
                    .catch { emit(emptyList()) }
                    .map { list ->
                        if (query.isEmpty()) list
                        else list.filter { it.fullName.contains(query, ignoreCase = true) }
                    }
            }.collect { filteredList ->
                _uiState.update { it.copy(athletes = filteredList, isLoading = false) }
            }
        }
    }

    fun setSportFilter(sport: String?) {
        _sportFilter.value = sport
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun inviteAthlete(academy: Academy, athlete: User, message: String) {
        viewModelScope.launch {
            val invitation = AcademyInvitation(
                academyId = academy.id,
                academyName = academy.academyName,
                athleteEmail = athlete.email,
                athleteName = athlete.fullName,
                sport = athlete.primaryDiscipline ?: "General",
                message = message
            )
            academyRepo.sendInvitation(invitation)
            
            // Send Notification
            val notificationRepo = com.example.prathibhascanfinal.data.repository.NotificationRepository(
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
            notificationRepo.sendNotification(
                userEmail = athlete.email,
                title = "🏀 Academy Invitation",
                message = "${academy.academyName} has invited you to join their program!",
                category = com.example.prathibhascanfinal.data.NotificationCategories.ACADEMY,
                action = "ACADEMY_INVITATION"
            )
        }
    }
    
    fun calculateTalentScore(user: User): Int {
        val base = user.technicalImpactScore.toInt().coerceIn(0, 100)
        val metricsAvg = (user.speedScore + user.agilityScore + user.staminaScore + user.strengthScore) / 4
        return (base * 0.4 + metricsAvg * 0.6).toInt().coerceIn(0, 100)
    }
}
