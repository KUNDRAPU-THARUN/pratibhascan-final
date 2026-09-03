package com.example.prathibhascanfinal.data.repository

import android.content.Context
import com.example.prathibhascanfinal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AcademyRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val academyDao = db.academyDao()
    private val mgmtDao = db.academyManagementDao()
    private val firestore = FirestoreRepository()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getAcademyFlowByEmail(email: String): Flow<Academy?> = academyDao.getAcademyFlowByEmail(email)

    suspend fun getAcademyByEmail(email: String): Academy? = academyDao.getAcademyByEmail(email)

    suspend fun registerAcademy(academy: Academy, coaches: List<Coach>): Long {
        return withContext(Dispatchers.IO) {
            val id = academyDao.registerAcademyWithCoaches(academy, coaches)
            val savedAcademy = academy.copy(id = id.toInt())
            firestore.saveAcademyProfile(savedAcademy)
            id
        }
    }

    // --- Mediator Sync Logic ---

    fun startSync(academyId: Int) {
        if (academyId <= 0) return
        
        repositoryScope.launch {
            // Athletes Sync
            firestore.getAthletesFlow(academyId).collectLatest { list ->
                list.forEach { mgmtDao.insertAthlete(it) }
            }
        }
        
        repositoryScope.launch {
            // Coaches Sync
            firestore.getCoachesFlow(academyId).collectLatest { list ->
                list.forEach { academyDao.insertCoach(it) }
            }
        }

        repositoryScope.launch {
            // Teams Sync
            firestore.getTeamsFlow(academyId).collectLatest { list ->
                list.forEach { mgmtDao.insertTeam(it) }
            }
        }

        repositoryScope.launch {
            // Tournaments Sync
            firestore.getTournamentsFlow(academyId).collectLatest { list ->
                list.forEach { mgmtDao.insertTournament(it) }
            }
        }

        repositoryScope.launch {
            // Facilities Sync
            firestore.getFacilitiesFlow(academyId).collectLatest { list ->
                list.forEach { mgmtDao.insertFacility(it) }
            }
        }

        repositoryScope.launch {
            // Tournament Registrations Sync
            firestore.getRegistrationsForOrganizerFlow(academyId).collectLatest { list ->
                list.forEach { mgmtDao.insertRegistration(it) }
            }
        }

        repositoryScope.launch {
            // Invitations Sync
            firestore.getInvitationsForAcademyFlow(academyId).collectLatest { list ->
                list.forEach { mgmtDao.insertInvitation(it) }
            }
        }
    }

    fun startAthleteInvitationSync(email: String) {
        repositoryScope.launch {
            firestore.getInvitationsForAthleteFlow(email).collectLatest { list ->
                list.forEach { mgmtDao.insertInvitation(it) }
            }
        }
    }

    fun startDiscoverySync() {
        repositoryScope.launch {
            firestore.getFirebaseFirestore().collection("tournaments")
                .limit(50)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObjects(Tournament::class.java)?.forEach { tournament ->
                        repositoryScope.launch { mgmtDao.insertTournament(tournament) }
                    }
                }
        }
    }

    // --- CRUD with Cloud Sync ---

    suspend fun sendInvitation(invitation: AcademyInvitation): String {
        return withContext(Dispatchers.IO) {
            val existing = mgmtDao.getPendingInvitation(invitation.academyId, invitation.athleteEmail)
            if (existing != null) return@withContext "Invitation already sent."
            
            val success = firestore.saveEntity("academy_invitations", invitation.id.toString(), invitation)
            if (success) {
                mgmtDao.insertInvitation(invitation)
                "Invitation sent successfully."
            } else {
                "Failed to send invitation."
            }
        }
    }

    suspend fun updateInvitationStatus(invitation: AcademyInvitation, status: String): Boolean {
        return withContext(Dispatchers.IO) {
            val updated = invitation.copy(status = status)
            val success = firestore.saveEntity("academy_invitations", updated.id.toString(), updated)
            if (success) {
                mgmtDao.insertInvitation(updated)
            }
            success
        }
    }

    suspend fun updateRegistrationStatus(registration: TournamentRegistration, status: String, reason: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            val updated = registration.copy(status = status, rejectionReason = reason)
            mgmtDao.insertRegistration(updated)
            firestore.saveEntity("tournament_registrations", updated.registrationId.toString(), updated)
        }
    }

    suspend fun updateAcademy(academy: Academy): Boolean {
        return withContext(Dispatchers.IO) {
            academyDao.insertAcademy(academy)
            firestore.saveAcademyProfile(academy)
        }
    }

    suspend fun registerAthlete(athlete: AcademyAthlete): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertAthlete(athlete)
            firestore.saveAthlete(athlete)
        }
    }

    suspend fun deleteAthlete(athlete: AcademyAthlete): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteAthlete(athlete)
            firestore.deleteEntity("athletes", athlete.athleteId.toString())
        }
    }

    suspend fun addCoach(coach: Coach): Boolean {
        return withContext(Dispatchers.IO) {
            academyDao.insertCoach(coach)
            firestore.saveCoach(coach)
        }
    }

    suspend fun deleteCoach(coach: Coach): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteCoach(coach)
            firestore.deleteEntity("coaches", coach.coachId.toString())
        }
    }

    suspend fun addFacility(facility: Facility): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertFacility(facility)
            firestore.saveFacility(facility)
        }
    }

    suspend fun deleteFacility(facility: Facility): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteFacility(facility)
            firestore.deleteEntity("facilities", facility.id.toString())
        }
    }

    suspend fun addTeam(team: Team): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertTeam(team)
            firestore.saveTeam(team)
        }
    }

    suspend fun deleteTeam(team: Team): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteTeam(team)
            firestore.deleteEntity("teams", team.teamId.toString())
        }
    }

    suspend fun addTournament(tournament: Tournament): Boolean {
        return withContext(Dispatchers.IO) {
            val id = mgmtDao.insertTournament(tournament)
            firestore.saveEntity("tournaments", tournament.tournamentId.toString(), tournament)
        }
    }

    suspend fun deleteTournament(tournament: Tournament): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteTournament(tournament)
            FirebaseManager.deleteTournament(tournament.tournamentId)
        }
    }

    suspend fun addEquipment(equipment: Equipment): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertEquipment(equipment)
            FirebaseManager.saveEquipment(equipment)
        }
    }

    suspend fun deleteEquipment(equipment: Equipment): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteEquipment(equipment)
            FirebaseManager.deleteEquipment(equipment.equipmentId)
        }
    }

    suspend fun saveAttendance(attendance: Attendance): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertAttendance(attendance)
            FirebaseManager.saveAttendanceRecord(attendance)
        }
    }

    suspend fun saveMedicalRecord(record: MedicalRecord): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertMedicalRecord(record)
            FirebaseManager.saveMedicalRecord(record)
        }
    }

    suspend fun saveDietPlan(plan: DietPlan): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertDietPlan(plan)
            FirebaseManager.saveDietPlan(plan)
        }
    }

    suspend fun savePayroll(payroll: Payroll): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertPayroll(payroll)
            FirebaseManager.savePayroll(payroll)
        }
    }

    suspend fun registerForTournament(registration: TournamentRegistration): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertRegistration(registration)
            firestore.saveRegistration(registration)
        }
    }

    // --- Legacy / Compatibility ---
    fun getAthletesFlow(academyId: Int): Flow<List<AcademyAthlete>> = mgmtDao.getAthletesFlow(academyId)
    fun getCoachesFlow(academyId: Int): Flow<List<Coach>> = mgmtDao.getCoachesFlow(academyId)
    fun getFacilitiesFlow(academyId: Int): Flow<List<Facility>> = mgmtDao.getFacilitiesFlow(academyId)
    fun getAllTournamentsFlow(): Flow<List<Tournament>> = mgmtDao.getAllTournamentsFlow()
}
