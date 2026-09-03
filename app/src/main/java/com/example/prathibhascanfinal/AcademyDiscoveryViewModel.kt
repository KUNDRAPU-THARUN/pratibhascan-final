package com.example.prathibhascanfinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class AcademyDiscoveryState(
    val academies: List<Academy> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSport: String? = null,
    val searchQuery: String = ""
)

class AcademyDiscoveryViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val firestoreRepo = FirestoreRepository()
    private val academyRepo = AcademyRepository(application)
    private val db = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(AcademyDiscoveryState())
    val uiState: StateFlow<AcademyDiscoveryState> = _uiState.asStateFlow()

    private val _sportFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        startDiscovery()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startDiscovery() {
        viewModelScope.launch {
            combine(
                _sportFilter,
                _searchQuery
            ) { sport, query ->
                Pair(sport, query)
            }.flatMapLatest { (sport, query) ->
                _uiState.update { it.copy(isLoading = true, selectedSport = sport, searchQuery = query) }
                getAcademiesFlow(sport)
                    .catch { emit(emptyList()) }
                    .map { list ->
                        if (query.isEmpty()) list
                        else list.filter { it.academyName.contains(query, ignoreCase = true) }
                    }
            }.collect { filteredList ->
                _uiState.update { it.copy(academies = filteredList, isLoading = false) }
            }
        }
    }

    private fun getAcademiesFlow(sport: String?): Flow<List<Academy>> = callbackFlow {
        val query = db.collection("academies")
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(Academy::class.java) ?: emptyList()
            val filtered = if (sport == null) list else list.filter { it.specializedDomains.contains(sport, ignoreCase = true) }
            trySend(filtered)
        }
        awaitClose { listener.remove() }
    }

    fun setSportFilter(sport: String?) {
        _sportFilter.value = sport
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun applyToAcademy(academy: Academy, message: String) {
        val userEmail = SessionManager(getApplication()).getEmail() ?: return
        val userName = SessionManager(getApplication()).getName() ?: "Athlete"
        
        viewModelScope.launch {
            val invitation = AcademyInvitation(
                academyId = academy.id,
                academyName = academy.academyName,
                athleteEmail = userEmail,
                athleteName = userName,
                sport = academy.specializedDomains.split(",").firstOrNull() ?: "General",
                message = message,
                status = "PENDING"
            )
            // Reuse sendInvitation logic from repository (it handles Firestore save)
            academyRepo.sendInvitation(invitation)
            
            // Notify Academy
            val notificationRepo = com.example.prathibhascanfinal.data.repository.NotificationRepository(db)
            notificationRepo.sendNotification(
                userEmail = academy.contactEmail,
                title = "New Application",
                message = "$userName has applied to your academy.",
                category = com.example.prathibhascanfinal.data.NotificationCategories.ACADEMY,
                action = "ATHLETE_DETAILS"
            )
        }
    }
}
