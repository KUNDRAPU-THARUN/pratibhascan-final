package com.example.prathibhascanfinal.data.repository

import android.content.Context
import com.example.prathibhascanfinal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class InstitutionRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val institutionDao = db.institutionDao()
    private val mgmtDao = db.institutionManagementDao()
    private val firestore = FirestoreRepository()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getInstitutionFlowByEmail(email: String): Flow<Institution?> = institutionDao.getInstitutionFlowByEmail(email)

    suspend fun getInstitutionByEmail(email: String): Institution? = institutionDao.getInstitutionByEmail(email)

    // --- Mediator Sync Logic ---

    fun startSync(instId: Int) {
        if (instId <= 0) return

        repositoryScope.launch {
            // Students Sync
            firestore.getStudentsFlow(instId).collectLatest { list ->
                list.forEach { mgmtDao.insertStudent(it) }
            }
        }

        repositoryScope.launch {
            // Teachers Sync
            firestore.getTeachersFlow(instId).collectLatest { list ->
                list.forEach { mgmtDao.insertTeacher(it) }
            }
        }
        
        repositoryScope.launch {
            // Teams Sync
            firestore.getFirebaseFirestore().collection("institution_teams").whereEqualTo("institutionId", instId)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObjects(InstitutionTeam::class.java)?.forEach { team ->
                        repositoryScope.launch { mgmtDao.insertTeam(team) }
                    }
                }
        }

        repositoryScope.launch {
            // Team Members Sync
            firestore.getFirebaseFirestore().collection("institution_team_members").whereEqualTo("institutionId", instId)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObjects(InstitutionTeamMember::class.java)?.forEach { member ->
                        repositoryScope.launch { mgmtDao.insertTeamMember(member) }
                    }
                }
        }

        repositoryScope.launch {
            // Equipment Sync
            firestore.getFirebaseFirestore().collection("institution_equipment").whereEqualTo("institutionId", instId)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObjects(InstitutionEquipment::class.java)?.forEach { equip ->
                        repositoryScope.launch { mgmtDao.insertEquipment(equip) }
                    }
                }
        }
    }

    // --- CRUD with Cloud Sync ---

    suspend fun updateInstitution(institution: Institution): Boolean {
        return withContext(Dispatchers.IO) {
            institutionDao.insertInstitution(institution)
            firestore.saveInstitution(institution)
        }
    }

    suspend fun addStudent(student: Student): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertStudent(student)
            firestore.saveStudent(student)
        }
    }

    suspend fun deleteStudent(student: Student): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteStudent(student)
            firestore.deleteEntity("institution_students", student.studentId.toString())
        }
    }

    suspend fun addTeacher(teacher: InstitutionTeacher): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertTeacher(teacher)
            firestore.saveTeacher(teacher)
        }
    }

    suspend fun deleteTeacher(teacher: InstitutionTeacher): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteTeacher(teacher)
            firestore.deleteEntity("teachers", teacher.teacherId.toString())
        }
    }

    suspend fun addTeam(team: InstitutionTeam): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertTeam(team)
            firestore.saveEntity("institution_teams", team.teamId.toString(), team)
        }
    }

    suspend fun deleteTeam(teamId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val team = mgmtDao.getTeamById(teamId)
            team?.let { mgmtDao.deleteTeam(it) }
            mgmtDao.deleteTeamMembersByTeamId(teamId)
            firestore.deleteEntity("institution_teams", teamId.toString())
        }
    }

    suspend fun addTeamMember(member: InstitutionTeamMember): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertTeamMember(member)
            FirebaseManager.saveInstitutionTeamMember(member)
        }
    }

    suspend fun deleteTeamMember(member: InstitutionTeamMember): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteTeamMember(member)
            FirebaseManager.deleteInstitutionTeamMember(member.memberId)
        }
    }

    fun getTeamMembersFlow(teamId: Int): Flow<List<InstitutionTeamMember>> = mgmtDao.getTeamMembersFlow(teamId)

    suspend fun addEquipment(equipment: InstitutionEquipment): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertEquipment(equipment)
            firestore.saveEntity("institution_equipment", equipment.equipmentId.toString(), equipment)
        }
    }

    suspend fun deleteEquipment(equipment: InstitutionEquipment): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteEquipment(equipment)
            firestore.deleteEntity("institution_equipment", equipment.equipmentId.toString())
        }
    }

    suspend fun addTournament(tournament: InstitutionTournament): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertTournament(tournament)
            firestore.saveEntity("institution_tournaments", tournament.tournamentId.toString(), tournament)
        }
    }

    suspend fun deleteTournament(tournament: InstitutionTournament): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteTournament(tournament)
            firestore.deleteEntity("institution_tournaments", tournament.tournamentId.toString())
        }
    }

    suspend fun addInstitutionSport(sport: InstitutionSport): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertInstitutionSport(sport)
            FirebaseManager.saveInstitutionSport(sport)
        }
    }

    suspend fun addBooking(booking: GroundBooking): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertBooking(booking)
            FirebaseManager.saveGroundBooking(booking)
        }
    }

    suspend fun deleteBooking(booking: GroundBooking): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteBooking(booking)
            FirebaseManager.deleteEntity("institution_bookings", booking.bookingId)
        }
    }

    suspend fun addPracticalExam(exam: PracticalExam): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertExam(exam)
            FirebaseManager.savePracticalExam(exam)
        }
    }

    suspend fun deleteExam(exam: PracticalExam): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteExam(exam)
            FirebaseManager.deleteEntity("practical_exams", exam.examId)
        }
    }

    suspend fun saveMedicalRecord(record: InstitutionMedicalRecord): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertMedicalRecord(record)
            FirebaseManager.saveInstitutionMedicalRecord(record)
        }
    }

    suspend fun deleteMedicalRecord(record: InstitutionMedicalRecord): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteMedicalRecord(record)
            FirebaseManager.deleteEntity("institution_medical_records", record.recordId)
        }
    }

    suspend fun saveTrainingSlot(slot: InstitutionTrainingSlot): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.insertTrainingSlot(slot)
            FirebaseManager.saveTrainingSlot(slot)
        }
    }

    suspend fun deleteTrainingSlot(slot: InstitutionTrainingSlot): Boolean {
        return withContext(Dispatchers.IO) {
            mgmtDao.deleteTrainingSlot(slot)
            FirebaseManager.deleteEntity("institution_training_slots", slot.slotId)
        }
    }

    // Statistics Flows (Room-based for local-first UI)
    fun getTotalStudents(instId: Int): Flow<Int> = mgmtDao.getTotalStudents(instId)
    fun getStudentCountBySport(instId: Int): Flow<List<SportCount>> = mgmtDao.getStudentCountBySport(instId)
    fun getInstitutionSports(instId: Int): Flow<List<InstitutionSport>> = mgmtDao.getInstitutionSports(instId)
    fun getTotalTeams(instId: Int): Flow<Int> = mgmtDao.getTotalTeams(instId)
    fun getTeamCountBySport(instId: Int): Flow<List<SportCount>> = mgmtDao.getTeamCountBySport(instId)
    fun getTotalEquipmentCount(instId: Int): Flow<Int> = mgmtDao.getTotalEquipmentCount(instId)
    fun getActiveBookingCount(instId: Int, now: Long): Flow<Int> = mgmtDao.getActiveBookingCount(instId, now)
    fun getTotalTournamentCount(instId: Int): Flow<Int> = mgmtDao.getTotalTournamentCount(instId)
    fun getTeachersFlow(instId: Int): Flow<List<InstitutionTeacher>> = mgmtDao.getTeachersFlow(instId)
    fun getTotalTeacherCount(instId: Int): Flow<Int> = mgmtDao.getTotalTeacherCount(instId)
    fun getAverageAttendance(instId: Int): Flow<Double?> = mgmtDao.getAverageAttendance(instId)
    fun getPendingMedicalCaseCount(instId: Int): Flow<Int> = mgmtDao.getPendingMedicalCaseCount(instId)
    fun getScholarshipCandidateCount(instId: Int): Flow<Int> = mgmtDao.getScholarshipCandidateCount(instId)
}
