package com.example.prathibhascanfinal

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InstitutionManagementDao {
    // --- Students ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("SELECT * FROM institution_students WHERE institutionId = :instId")
    suspend fun getStudents(instId: Int): List<Student>

    @Query("SELECT * FROM institution_students WHERE institutionId = :instId")
    fun getStudentsFlow(instId: Int): Flow<List<Student>>

    @Query("SELECT * FROM institution_students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT COUNT(*) FROM institution_students WHERE institutionId = :instId")
    fun getTotalStudents(instId: Int): Flow<Int>

    @Query("SELECT selectedSport as sport, COUNT(*) as count FROM institution_students WHERE institutionId = :instId GROUP BY selectedSport")
    fun getStudentCountBySport(instId: Int): Flow<List<SportCount>>

    @Query("SELECT AVG(attendancePercentage) FROM institution_students WHERE institutionId = :instId")
    fun getAverageAttendance(instId: Int): Flow<Double?>

    // --- Sports ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitutionSport(sport: InstitutionSport)

    @Query("SELECT * FROM institution_sports WHERE institutionId = :instId")
    fun getInstitutionSports(instId: Int): Flow<List<InstitutionSport>>

    @Delete
    suspend fun deleteSport(sport: InstitutionSport)

    // --- Teams ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: InstitutionTeam)

    @Query("SELECT * FROM institution_teams WHERE institutionId = :instId")
    fun getTeamsFlow(instId: Int): Flow<List<InstitutionTeam>>

    @Query("SELECT * FROM institution_teams WHERE teamId = :teamId")
    suspend fun getTeamById(teamId: Int): InstitutionTeam?

    @Query("SELECT * FROM institution_teams WHERE institutionId = :instId")
    suspend fun getTeams(instId: Int): List<InstitutionTeam>

    @Delete
    suspend fun deleteTeam(team: InstitutionTeam)

    @Query("SELECT COUNT(*) FROM institution_teams WHERE institutionId = :instId")
    fun getTotalTeams(instId: Int): Flow<Int>

    @Query("SELECT sport as sport, COUNT(*) as count FROM institution_teams WHERE institutionId = :instId GROUP BY sport")
    fun getTeamCountBySport(instId: Int): Flow<List<SportCount>>

    // --- Team Members ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(member: InstitutionTeamMember)

    @Query("SELECT * FROM institution_team_members WHERE teamId = :teamId")
    fun getTeamMembersFlow(teamId: Int): Flow<List<InstitutionTeamMember>>

    @Query("SELECT * FROM institution_team_members WHERE teamId = :teamId")
    suspend fun getTeamMembers(teamId: Int): List<InstitutionTeamMember>

    @Query("SELECT * FROM institution_team_members WHERE teamId = :teamId AND studentId = :studentId LIMIT 1")
    suspend fun getTeamMemberByStudentId(teamId: Int, studentId: Int): InstitutionTeamMember?

    @Query("SELECT * FROM institution_team_members WHERE institutionId = :instId")
    suspend fun getAllTeamMembersForInstitution(instId: Int): List<InstitutionTeamMember>

    @Query("SELECT * FROM institution_team_members WHERE institutionId = :instId")
    fun getAllTeamMembersFlowForInstitution(instId: Int): Flow<List<InstitutionTeamMember>>

    @Delete
    suspend fun deleteTeamMember(member: InstitutionTeamMember)

    @Query("DELETE FROM institution_team_members WHERE teamId = :teamId")
    suspend fun deleteTeamMembersByTeamId(teamId: Int)

    @Query("DELETE FROM institution_team_members WHERE memberId = :memberId")
    suspend fun deleteTeamMemberById(memberId: Int)

    @Query("SELECT COUNT(*) FROM institution_team_members WHERE teamId = :teamId")
    fun getTeamMemberCount(teamId: Int): Flow<Int>

    // --- Equipment ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: InstitutionEquipment)

    @Query("SELECT * FROM institution_equipment WHERE institutionId = :instId")
    suspend fun getInventory(instId: Int): List<InstitutionEquipment>

    @Query("SELECT * FROM institution_equipment WHERE institutionId = :instId")
    fun getInventoryFlow(instId: Int): Flow<List<InstitutionEquipment>>

    @Query("SELECT * FROM institution_equipment WHERE equipmentId = :equipmentId")
    suspend fun getEquipmentById(equipmentId: Int): InstitutionEquipment?

    @Delete
    suspend fun deleteEquipment(equipment: InstitutionEquipment)

    @Query("SELECT COUNT(*) FROM institution_equipment WHERE institutionId = :instId")
    fun getTotalEquipmentCount(instId: Int): Flow<Int>

    // --- Bookings ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: GroundBooking)

    @Delete
    suspend fun deleteBooking(booking: GroundBooking)

    @Query("SELECT * FROM institution_bookings WHERE institutionId = :instId")
    suspend fun getBookings(instId: Int): List<GroundBooking>

    @Query("SELECT COUNT(*) FROM institution_bookings WHERE institutionId = :instId AND endTime > :now")
    fun getActiveBookingCount(instId: Int, now: Long): Flow<Int>

    // --- Exams ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: PracticalExam)

    @Delete
    suspend fun deleteExam(exam: PracticalExam)

    @Query("SELECT * FROM practical_exams WHERE institutionId = :instId")
    suspend fun getExams(instId: Int): List<PracticalExam>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(mark: ExamMark)

    @Query("SELECT * FROM student_exam_marks WHERE examId = :examId")
    suspend fun getMarksForExam(examId: Int): List<ExamMark>

    // --- Tournaments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: InstitutionTournament)

    @Query("SELECT * FROM institution_tournaments WHERE institutionId = :instId")
    suspend fun getTournaments(instId: Int): List<InstitutionTournament>

    @Query("SELECT * FROM institution_tournaments WHERE institutionId = :instId ORDER BY startDate DESC")
    fun getTournamentsFlow(instId: Int): Flow<List<InstitutionTournament>>

    @Query("SELECT * FROM institution_tournaments WHERE tournamentId = :tournamentId")
    suspend fun getTournamentById(tournamentId: Int): InstitutionTournament?

    @Delete
    suspend fun deleteTournament(tournament: InstitutionTournament)

    @Query("SELECT COUNT(*) FROM institution_tournaments WHERE institutionId = :instId")
    fun getTotalTournamentCount(instId: Int): Flow<Int>

    // --- Teachers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: InstitutionTeacher)

    @Query("SELECT * FROM institution_teachers WHERE institutionId = :instId")
    fun getTeachersFlow(instId: Int): Flow<List<InstitutionTeacher>>

    @Query("SELECT * FROM institution_teachers WHERE teacherId = :teacherId")
    suspend fun getTeacherById(teacherId: Int): InstitutionTeacher?

    @Delete
    suspend fun deleteTeacher(teacher: InstitutionTeacher)

    @Query("SELECT COUNT(*) FROM institution_teachers WHERE institutionId = :instId")
    fun getTotalTeacherCount(instId: Int): Flow<Int>

    // --- Medical ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: InstitutionMedicalRecord)

    @Delete
    suspend fun deleteMedicalRecord(record: InstitutionMedicalRecord)

    @Query("SELECT * FROM institution_medical_records WHERE institutionId = :instId ORDER BY date DESC")
    fun getMedicalRecordsFlow(instId: Int): Flow<List<InstitutionMedicalRecord>>

    @Query("SELECT COUNT(*) FROM institution_medical_records WHERE institutionId = :instId AND clearanceStatus = 'Pending'")
    fun getPendingMedicalCaseCount(instId: Int): Flow<Int>

    // --- Training Slots ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingSlot(slot: InstitutionTrainingSlot)

    @Delete
    suspend fun deleteTrainingSlot(slot: InstitutionTrainingSlot)

    @Query("SELECT * FROM institution_training_slots WHERE institutionId = :instId")
    fun getTrainingSlotsFlow(instId: Int): Flow<List<InstitutionTrainingSlot>>

    // --- Additional Stats ---
    @Query("SELECT COUNT(*) FROM institution_students WHERE institutionId = :instId AND scholarshipEligible = 1")
    fun getScholarshipCandidateCount(instId: Int): Flow<Int>

    // --- Student Transfer ---
    @Query("UPDATE institution_students SET institutionId = :newInstId WHERE studentId = :studentId")
    suspend fun transferStudent(studentId: Int, newInstId: Int)
}
