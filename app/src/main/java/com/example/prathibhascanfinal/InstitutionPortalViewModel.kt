package com.example.prathibhascanfinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.data.repository.InstitutionRepository
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class InstitutionDashboardState(
    val institution: Institution? = null,
    val totalStudents: Int = 0,
    val sportsStudents: Int = 0,
    val peTeachers: Int = 0,
    val totalTeams: Int = 0,
    val sportsOfferedCount: Int = 0,
    val totalGrounds: Int = 0,
    val totalEquipment: Int = 0,
    val todayAttendance: String = "--%",
    val upcomingEvents: Int = 0,
    val activeTournaments: Int = 0,
    val pendingApprovals: Int = 0,
    val medicalCases: Int = 0,
    val time: String = "--:--",
    val dayDate: String = "----, -- ---",
    val institutionSports: List<InstitutionSport> = emptyList(),
    val sportSummaries: List<InstitutionSportSummary> = emptyList(),
    val isLoading: Boolean = true
)

data class InstitutionSportSummary(
    val sportName: String,
    val studentCount: Int = 0,
    val teamCount: Int = 0,
    val teacherCount: Int = 0
)

class InstitutionPortalViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val repository = InstitutionRepository(application)
    private val _uiState = MutableStateFlow(InstitutionDashboardState())
    val uiState: StateFlow<InstitutionDashboardState> = _uiState.asStateFlow()

    private var timeJob: Job? = null

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
        viewModelScope.launch {
            repository.getInstitutionFlowByEmail(email)
                .flatMapLatest { inst ->
                    if (inst == null) {
                        flowOf(InstitutionDashboardState(isLoading = false, institution = null))
                    } else {
                        val instId = inst.id
                        repository.startSync(instId)
                        val now = System.currentTimeMillis()
                        
                        combine(
                            repository.getTotalStudents(instId),
                            repository.getTotalTeams(instId),
                            repository.getTotalEquipmentCount(instId),
                            repository.getActiveBookingCount(instId, now),
                            repository.getTotalTournamentCount(instId),
                            repository.getInstitutionSports(instId),
                            repository.getStudentCountBySport(instId),
                            repository.getTeamCountBySport(instId),
                            repository.getTotalTeacherCount(instId),
                            repository.getPendingMedicalCaseCount(instId),
                            repository.getScholarshipCandidateCount(instId),
                            repository.getAverageAttendance(instId)
                        ) { flows ->
                            val students = flows[0] as Int
                            val teams = flows[1] as Int
                            val equipment = flows[2] as Int
                            val bookings = flows[3] as Int
                            val tournaments = flows[4] as Int
                            @Suppress("UNCHECKED_CAST")
                            val instSports = flows[5] as List<InstitutionSport>
                            @Suppress("UNCHECKED_CAST")
                            val sBySport = flows[6] as List<SportCount>
                            @Suppress("UNCHECKED_CAST")
                            val tBySport = flows[7] as List<SportCount>
                            val teacherCount = flows[8] as Int
                            val medicalCases = flows[9] as Int
                            val scholarshipCandidates = flows[10] as Int
                            val avgAttendance = flows[11] as Double?
                            
                            val sportsNames = (sBySport.map { it.sport } + tBySport.map { it.sport } + instSports.map { it.sportName }).distinct()
                            
                            val summaries = sportsNames.map { sport ->
                                val sCount = sBySport.find { it.sport == sport }?.count ?: 0
                                val tCount = tBySport.find { it.sport == sport }?.count ?: 0
                                InstitutionSportSummary(
                                    sportName = sport,
                                    studentCount = sCount,
                                    teamCount = tCount,
                                    teacherCount = if (teacherCount > 0) (teacherCount / sportsNames.size.coerceAtLeast(1)).coerceAtLeast(1) else 0
                                )
                            }

                            InstitutionDashboardState(
                                institution = inst,
                                totalStudents = students,
                                sportsStudents = summaries.sumOf { it.studentCount },
                                peTeachers = teacherCount,
                                totalTeams = teams,
                                sportsOfferedCount = sportsNames.size,
                                totalGrounds = instSports.count { it.groundAvailable },
                                totalEquipment = equipment,
                                todayAttendance = String.format(Locale.getDefault(), "%.1f%%", avgAttendance ?: 0.0),
                                upcomingEvents = tournaments + bookings,
                                activeTournaments = tournaments,
                                medicalCases = medicalCases,
                                pendingApprovals = scholarshipCandidates, // Using scholarship candidates as a proxy for approvals for now
                                time = _uiState.value.time,
                                dayDate = _uiState.value.dayDate,
                                institutionSports = instSports,
                                sportSummaries = summaries,
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
