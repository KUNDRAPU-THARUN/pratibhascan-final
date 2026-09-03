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

data class InstitutionProfileState(
    val institution: Institution? = null,
    val totalStudents: Int = 0,
    val sportsStudents: Int = 0,
    val peTeachers: Int = 0,
    val totalTeams: Int = 0,
    val sportsOfferedCount: Int = 0,
    val totalGrounds: Int = 0,
    val totalEquipment: Int = 0,
    val time: String = "--:--",
    val dayDate: String = "----, -- ---",
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Never"
)

class InstitutionProfileViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val repository = InstitutionRepository(application)
    private val _uiState = MutableStateFlow(InstitutionProfileState())
    val uiState: StateFlow<InstitutionProfileState> = _uiState.asStateFlow()

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
    fun loadProfile(email: String) {
        viewModelScope.launch {
            repository.getInstitutionFlowByEmail(email)
                .flatMapLatest { inst ->
                    if (inst == null) {
                        flowOf(InstitutionProfileState(isLoading = false, institution = null))
                    } else {
                        val instId = inst.id
                        val now = System.currentTimeMillis()
                        val syncFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        
                        combine(
                            repository.getTotalStudents(instId),
                            repository.getTotalTeams(instId),
                            repository.getTotalEquipmentCount(instId),
                            repository.getInstitutionSports(instId),
                            repository.getTotalTeacherCount(instId),
                            repository.getStudentCountBySport(instId)
                        ) { flows ->
                            val students = flows[0] as Int
                            val teams = flows[1] as Int
                            val equipment = flows[2] as Int
                            val instSports = flows[3] as List<InstitutionSport>
                            val teacherCount = flows[4] as Int
                            val sBySport = flows[5] as List<SportCount>

                            InstitutionProfileState(
                                institution = inst,
                                totalStudents = students,
                                sportsStudents = sBySport.sumOf { it.count },
                                peTeachers = teacherCount,
                                totalTeams = teams,
                                sportsOfferedCount = instSports.size,
                                totalGrounds = instSports.count { it.groundAvailable },
                                totalEquipment = equipment,
                                time = _uiState.value.time,
                                dayDate = _uiState.value.dayDate,
                                isLoading = false,
                                lastSyncTime = syncFormat.format(Date(inst.lastSync))
                            )
                        }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun updateProfile(institution: Institution) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            repository.updateInstitution(institution.copy(lastSync = System.currentTimeMillis()))
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}
