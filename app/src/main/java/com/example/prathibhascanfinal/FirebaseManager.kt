package com.example.prathibhascanfinal

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    fun getFirebaseAuth(): FirebaseAuth = auth
    fun getFirebaseFirestore(): FirebaseFirestore = db
    fun getFirebaseStorage(): FirebaseStorage = storage

    suspend fun saveAthlete(athlete: AcademyAthlete): Boolean {
        return try {
            val docId = athlete.athleteId.toString().takeIf { it != "0" } ?: db.collection("athletes").document().id
            db.collection("athletes")
                .document(docId)
                .set(athlete)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadFile(uri: Uri, path: String): String? {
        return try {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveAcademy(academy: Academy): Boolean {
        return try {
            val docId = academy.id.toString().takeIf { it != "0" } ?: db.collection("academies").document().id
            db.collection("academies")
                .document(docId)
                .set(academy)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAcademy(email: String): Academy? {
        return try {
            val query = db.collection("academies").whereEqualTo("contactEmail", email).limit(1).get().await()
            query.documents.firstOrNull()?.toObject(Academy::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun getAcademyAthletes(academyId: Int): List<AcademyAthlete> {
        return try {
            val query = db.collection("athletes").whereEqualTo("academyId", academyId).get().await()
            query.toObjects(AcademyAthlete::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAcademyCoaches(academyId: Int): List<Coach> {
        return try {
            val query = db.collection("coaches").whereEqualTo("academyId", academyId).get().await()
            query.toObjects(Coach::class.java)
        } catch (e: Exception) { emptyList() }
    }
    
    suspend fun saveCoach(coach: Coach): Boolean {
        return try {
            val docId = coach.coachId.toString().takeIf { it != "0" } ?: db.collection("coaches").document().id
            db.collection("coaches")
                .document(docId)
                .set(coach)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveStudent(student: Student): Boolean {
        return try {
            val docId = student.studentId.toString().takeIf { it != "0" } ?: db.collection("institution_students").document().id
            db.collection("institution_students")
                .document(docId)
                .set(student)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveInstitution(institution: Institution): Boolean {
        return try {
            val docId = institution.id.toString().takeIf { it != "0" } ?: db.collection("institutions").document().id
            db.collection("institutions")
                .document(docId)
                .set(institution)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getInstitution(email: String): Institution? {
        return try {
            val query = db.collection("institutions").whereEqualTo("contactEmail", email).limit(1).get().await()
            query.documents.firstOrNull()?.toObject(Institution::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun getInstitutionStudents(instId: Int): List<Student> {
        return try {
            val query = db.collection("institution_students").whereEqualTo("institutionId", instId).get().await()
            query.toObjects(Student::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getInstitutionTeachers(instId: Int): List<InstitutionTeacher> {
        return try {
            val query = db.collection("institution_teachers").whereEqualTo("institutionId", instId).get().await()
            query.toObjects(InstitutionTeacher::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getInstitutionTeams(instId: Int): List<InstitutionTeam> {
        return try {
            val query = db.collection("institution_teams").whereEqualTo("institutionId", instId).get().await()
            query.toObjects(InstitutionTeam::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getInstitutionSports(instId: Int): List<InstitutionSport> {
        return try {
            val query = db.collection("institution_sports").whereEqualTo("institutionId", instId).get().await()
            query.toObjects(InstitutionSport::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun saveTeam(team: Team): Boolean {
        return try {
            val docId = team.teamId.toString().takeIf { it != "0" } ?: db.collection("teams").document().id
            db.collection("teams").document(docId).set(team).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveFacility(facility: Facility): Boolean {
        return try {
            val docId = facility.id.toString().takeIf { it != "0" } ?: db.collection("facilities").document().id
            db.collection("facilities").document(docId).set(facility).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveTrainingSlot(slot: TrainingSlot): Boolean {
        return try {
            val docId = slot.slotId.toString().takeIf { it != "0" } ?: db.collection("training_slots").document().id
            db.collection("training_slots").document(docId).set(slot).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveAttendance(attendance: Attendance): Boolean {
        return try {
            val docId = attendance.attendanceId.toString().takeIf { it != "0" } ?: db.collection("attendance").document().id
            db.collection("attendance").document(docId).set(attendance).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveTournament(tournament: Tournament): Boolean {
        return try {
            val docId = tournament.tournamentId.toString().takeIf { it != "0" } ?: db.collection("tournaments").document().id
            db.collection("tournaments").document(docId).set(tournament).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveMatch(match: Match): Boolean {
        return try {
            val docId = match.matchId.toString().takeIf { it != "0" } ?: db.collection("matches").document().id
            db.collection("matches").document(docId).set(match).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveMedicalRecord(record: MedicalRecord): Boolean {
        return try {
            val docId = record.recordId.toString().takeIf { it != "0" } ?: db.collection("medical_records").document().id
            db.collection("medical_records").document(docId).set(record).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveDietPlan(plan: DietPlan): Boolean {
        return try {
            val docId = plan.planId.toString().takeIf { it != "0" } ?: db.collection("diet_plans").document().id
            db.collection("diet_plans").document(docId).set(plan).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun savePayroll(payroll: Payroll): Boolean {
        return try {
            val docId = payroll.payrollId.toString().takeIf { it != "0" } ?: db.collection("payroll").document().id
            db.collection("payroll").document(docId).set(payroll).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteEquipment(id: Int): Boolean {
        return try {
            db.collection("equipment").document(id.toString()).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteTournament(id: Int): Boolean {
        return try {
            db.collection("tournaments").document(id.toString()).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveAttendanceRecord(attendance: Attendance): Boolean {
        return try {
            val docId = attendance.attendanceId.toString().takeIf { it != "0" } ?: db.collection("attendance").document().id
            db.collection("attendance").document(docId).set(attendance).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveEquipment(equipment: Equipment): Boolean {
        return try {
            val docId = equipment.equipmentId.toString().takeIf { it != "0" } ?: db.collection("equipment").document().id
            db.collection("equipment").document(docId).set(equipment).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveInstitutionSport(sport: InstitutionSport): Boolean {
        return try {
            val docId = sport.sportId.toString().takeIf { it != "0" } ?: db.collection("institution_sports").document().id
            db.collection("institution_sports").document(docId).set(sport).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveTeacher(teacher: InstitutionTeacher): Boolean {
        return try {
            val docId = teacher.teacherId.toString().takeIf { it != "0" } ?: db.collection("teachers").document().id
            db.collection("teachers").document(docId).set(teacher).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveInstitutionTeam(team: InstitutionTeam): Boolean {
        return try {
            val docId = team.teamId.toString().takeIf { it != "0" } ?: db.collection("institution_teams").document().id
            db.collection("institution_teams").document(docId).set(team).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveInstitutionTeamMember(member: InstitutionTeamMember): Boolean {
        return try {
            val docId = member.memberId.toString().takeIf { it != "0" } ?: db.collection("institution_team_members").document().id
            db.collection("institution_team_members").document(docId).set(member).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteInstitutionTeamMember(memberId: Int): Boolean {
        return try {
            db.collection("institution_team_members").document(memberId.toString()).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveGroundBooking(booking: GroundBooking): Boolean {
        return try {
            val docId = booking.bookingId.toString().takeIf { it != "0" } ?: db.collection("institution_bookings").document().id
            db.collection("institution_bookings").document(docId).set(booking).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun savePracticalExam(exam: PracticalExam): Boolean {
        return try {
            val docId = exam.examId.toString().takeIf { it != "0" } ?: db.collection("practical_exams").document().id
            db.collection("practical_exams").document(docId).set(exam).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveInstitutionTournament(tournament: InstitutionTournament): Boolean {
        return try {
            val docId = tournament.tournamentId.toString().takeIf { it != "0" } ?: db.collection("institution_tournaments").document().id
            db.collection("institution_tournaments").document(docId).set(tournament).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveInstitutionMedicalRecord(record: InstitutionMedicalRecord): Boolean {
        return try {
            val docId = record.recordId.toString().takeIf { it != "0" } ?: db.collection("institution_medical_records").document().id
            db.collection("institution_medical_records").document(docId).set(record).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveTrainingSlot(slot: InstitutionTrainingSlot): Boolean {
        return try {
            val docId = slot.slotId.toString().takeIf { it != "0" } ?: db.collection("institution_training_slots").document().id
            db.collection("institution_training_slots").document(docId).set(slot).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteEntity(collection: String, id: Int): Boolean {
        return try {
            db.collection(collection).document(id.toString()).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveSportSpecificProfile(userEmail: String, sportName: String, profile: Any): Boolean {
        return try {
            db.collection("users")
                .document(userEmail)
                .collection("sport_profiles")
                .document(sportName)
                .set(profile)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
