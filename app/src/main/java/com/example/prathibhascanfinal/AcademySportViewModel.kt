package com.example.prathibhascanfinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AcademySportState(
    val athletes: List<AcademyAthlete> = emptyList(),
    val coaches: List<Coach> = emptyList(),
    val facilities: List<Facility> = emptyList(),
    val tournaments: List<Tournament> = emptyList(),
    val registrations: List<TournamentRegistration> = emptyList(),
    val isLoading: Boolean = true
)

class AcademySportViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val db = AppDatabase.getDatabase(application)
    private val mgmtDao = db.academyManagementDao()
    private val academyRepository = com.example.prathibhascanfinal.data.repository.AcademyRepository(application)
    private val notificationRepository = com.example.prathibhascanfinal.data.repository.NotificationRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
    private val firestoreRepo = com.example.prathibhascanfinal.data.repository.FirestoreRepository()

    private val _uiState = MutableStateFlow(AcademySportState())
    val uiState: StateFlow<AcademySportState> = _uiState.asStateFlow()

    fun loadSportData(academyId: Int, sport: String) {
        viewModelScope.launch {
            combine(
                firestoreRepo.getAthletesFlow(academyId).map { list -> list.filter { it.sportDomain == sport } },
                firestoreRepo.getCoachesFlow(academyId).map { list -> list.filter { it.specialization == sport } },
                firestoreRepo.getFacilitiesFlow(academyId).map { list -> list.filter { it.sport == sport } },
                firestoreRepo.getTournamentsFlow(academyId).map { list -> list.filter { it.sport == sport } },
                firestoreRepo.getRegistrationsForOrganizerFlow(academyId)
            ) { a, c, f, t, r ->
                val tournamentIds = t.map { it.tournamentId }.toSet()
                AcademySportState(
                    athletes = a,
                    coaches = c,
                    facilities = f,
                    tournaments = t,
                    registrations = r.filter { reg -> tournamentIds.contains(reg.tournamentId) },
                    isLoading = false
                )
            }.flowOn(Dispatchers.Default)
             .collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateRegistration(registration: TournamentRegistration, status: String) {
        viewModelScope.launch {
            val success = academyRepository.updateRegistrationStatus(registration, status)
            if (success) {
                val title = if (status == "ACCEPTED") "🏆 Registration Accepted" else "❌ Registration Rejected"
                val message = if (status == "ACCEPTED") 
                    "Your application for ${registration.tournamentTitle} has been approved!" 
                else 
                    "Your application for ${registration.tournamentTitle} was not successful."
                
                notificationRepository.sendNotification(
                    userEmail = registration.athleteEmail,
                    title = title,
                    message = message,
                    category = com.example.prathibhascanfinal.data.NotificationCategories.TOURNAMENT,
                    action = "TOURNAMENT_DETAILS"
                )
            }
        }
    }
}
