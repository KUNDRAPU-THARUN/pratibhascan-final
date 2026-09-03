package com.example.prathibhascanfinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class AcademyDashboardState(
    val academy: Academy? = null,
    val totalAthletes: Int = 0,
    val verifiedAthletes: Int = 0,
    val pendingAthletes: Int = 0,
    val activeAthletes: Int = 0,
    val totalCoaches: Int = 0,
    val totalFacilities: Int = 0,
    val totalSports: Int = 0,
    val totalActiveTeams: Int = 0,
    val totalTournaments: Int = 0,
    val totalInventory: Int = 0,
    val activeBookings: Int = 0,
    val totalRevenue: Double = 0.0,
    val pendingRequests: Int = 0,
    val time: String = "--:--",
    val dayDate: String = "----, -- ---",
    val academySports: List<AcademySport> = emptyList(),
    val sportSummaries: List<SportSummary> = emptyList(),
    val genderCounts: List<SportCount> = emptyList(),
    val ageGroupCounts: List<SportCount> = emptyList(),
    val recentAthletes: List<AcademyAthlete> = emptyList(),
    val isLoading: Boolean = true
)

data class SportSummary(
    val sportName: String,
    val athleteCount: Int = 0,
    val coachCount: Int = 0,
    val facilityCount: Int = 0
)

class AcademyPortalViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val db = AppDatabase.getDatabase(application)
    private val academyDao = db.academyDao()
    private val mgmtDao = db.academyManagementDao()
    private val repository = com.example.prathibhascanfinal.data.repository.AcademyRepository(application)
    private val firestoreRepo = com.example.prathibhascanfinal.data.repository.FirestoreRepository()

    private val _uiState = MutableStateFlow(AcademyDashboardState())
    val uiState: StateFlow<AcademyDashboardState> = _uiState.asStateFlow()

    private var timeJob: Job? = null
    private var dashboardJob: Job? = null

    init {
        startTimeUpdates()
    }

    private fun startTimeUpdates() {
        timeJob?.cancel()
        timeJob = viewModelScope.launch {
            while (true) {
                val calendar = Calendar.getInstance()
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
                
                _uiState.update { it.copy(
                    time = timeFormat.format(calendar.time),
                    dayDate = dateFormat.format(calendar.time)
                )}
                delay(60000)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadDashboard(email: String) {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            val lowerEmail = email.lowercase().trim()
            
            // 1. Initial attempt from Room Flow
            repository.getAcademyFlowByEmail(lowerEmail)
                .flatMapLatest { localAcademy ->
                    if (localAcademy == null) {
                        // 2. Fallback: Check cloud if Room is empty
                        flow<Academy?> {
                            val cloudAcademy = FirebaseManager.getAcademy(lowerEmail)
                            if (cloudAcademy != null) {
                                // Save to Room to trigger the original flow
                                withContext(Dispatchers.IO) { db.academyDao().insertAcademy(cloudAcademy) }
                                emit(cloudAcademy)
                            } else {
                                emit(null)
                            }
                        }
                    } else {
                        flowOf(localAcademy)
                    }
                }
                .flatMapLatest { academy ->
                    if (academy == null) {
                        flowOf(AcademyDashboardState(isLoading = false, academy = null))
                    } else {
                        val academyId = academy.id
                        repository.startSync(academyId)
                        val now = System.currentTimeMillis()
                        
                        combine(
                            mgmtDao.getAthletesFlow(academyId),
                            mgmtDao.getCoachesFlow(academyId),
                            mgmtDao.getTotalTeams(academyId),
                            mgmtDao.getTotalTournaments(academyId),
                            mgmtDao.getFacilitiesFlow(academyId),
                            mgmtDao.getTotalInventoryCount(academyId),
                            mgmtDao.getActiveSlotBookingCount(academyId, now),
                            mgmtDao.getAcademySports(academyId),
                            mgmtDao.getRegistrationsForOrganizerFlow(academyId),
                            mgmtDao.getInvitationsForAcademyFlow(academyId)
                        ) { flows ->
                            val athletes = flows[0] as List<AcademyAthlete>
                            val coaches = flows[1] as List<Coach>
                            val teamsCount = flows[2] as Int
                            val tournamentsCount = flows[3] as Int
                            val facilities = flows[4] as List<Facility>
                            val inventory = flows[5] as Int
                            val bookings = flows[6] as Int
                            val academySports = flows[7] as List<AcademySport>
                            val registrations = flows[8] as List<TournamentRegistration>
                            val invitations = flows[9] as List<AcademyInvitation>
                            
                            val sports = (athletes.map { it.sportDomain } + coaches.map { it.specialization } + facilities.map { it.sport }).distinct()
                            
                            val summaries = sports.map { sport ->
                                SportSummary(
                                    sportName = sport,
                                    athleteCount = athletes.count { it.sportDomain == sport },
                                    coachCount = coaches.count { it.specialization == sport },
                                    facilityCount = facilities.count { it.sport == sport }
                                )
                            }

                            val revenue = athletes.count { it.verificationStatus == "Verified" } * 1000.0

                            AcademyDashboardState(
                                academy = academy,
                                totalAthletes = athletes.size,
                                verifiedAthletes = athletes.count { it.verificationStatus == "Verified" },
                                pendingAthletes = athletes.count { it.verificationStatus == "Pending" },
                                activeAthletes = athletes.count { it.isActive },
                                totalCoaches = coaches.size,
                                totalFacilities = facilities.size,
                                totalActiveTeams = teamsCount,
                                totalTournaments = tournamentsCount,
                                totalInventory = inventory,
                                activeBookings = bookings,
                                totalRevenue = revenue,
                                pendingRequests = athletes.count { it.verificationStatus == "Pending" } + registrations.count { it.status == "PENDING" } + invitations.count { it.status == "PENDING" },
                                totalSports = sports.size,
                                academySports = academySports,
                                sportSummaries = summaries,
                                recentAthletes = athletes.sortedByDescending { it.joiningDate }.take(5),
                                time = _uiState.value.time,
                                dayDate = _uiState.value.dayDate,
                                isLoading = false
                            )
                        }
                        .flowOn(Dispatchers.Default)
                        .distinctUntilChanged()
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}
