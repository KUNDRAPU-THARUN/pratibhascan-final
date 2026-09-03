package com.example.prathibhascanfinal

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademyManagementDao {
    // --- Athletes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAthlete(athlete: AcademyAthlete)

    @Query("SELECT * FROM academy_athletes WHERE academyId = :academyId")
    fun getAthletesFlow(academyId: Int): Flow<List<AcademyAthlete>>

    @Query("SELECT * FROM academy_athletes WHERE academyId = :academyId")
    suspend fun getAthletesForAcademy(academyId: Int): List<AcademyAthlete>

    @Query("SELECT sportDomain as sport, COUNT(*) as count FROM academy_athletes WHERE academyId = :academyId GROUP BY sportDomain")
    fun getAthleteCountBySport(academyId: Int): Flow<List<SportCount>>

    @Query("SELECT * FROM academy_athletes WHERE academyId = :academyId AND (fullName LIKE '%' || :query || '%' OR admissionNumber LIKE '%' || :query || '%')")
    fun searchAthletes(academyId: Int, query: String): Flow<List<AcademyAthlete>>

    @Query("SELECT * FROM academy_athletes WHERE academyId = :academyId AND (:sport = '' OR sportDomain = :sport) AND (:gender = '' OR gender = :gender) AND (:status = '' OR verificationStatus = :status)")
    fun filterAthletes(academyId: Int, sport: String, gender: String, status: String): Flow<List<AcademyAthlete>>

    // --- Sports ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademySport(sport: AcademySport)

    @Query("SELECT * FROM academy_sports WHERE academyId = :academyId")
    fun getAcademySports(academyId: Int): Flow<List<AcademySport>>

    @Query("SELECT * FROM academy_sports WHERE sportId = :id")
    suspend fun getAcademySportById(id: Int): AcademySport?

    @Delete
    suspend fun deleteAcademySport(sport: AcademySport)

    @Query("SELECT * FROM academy_sports WHERE academyId = :academyId AND (sportName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')")
    fun searchAcademySports(academyId: Int, query: String): Flow<List<AcademySport>>

    // --- Coaches ---
    @Query("SELECT * FROM academy_coaches WHERE academyId = :academyId")
    fun getCoachesFlow(academyId: Int): Flow<List<Coach>>

    @Query("SELECT * FROM academy_coaches WHERE academyId = :academyId AND (name LIKE '%' || :query || '%' OR specialization LIKE '%' || :query || '%')")
    fun searchCoaches(academyId: Int, query: String): Flow<List<Coach>>

    @Query("SELECT specialization as sport, COUNT(*) as count FROM academy_coaches WHERE academyId = :academyId GROUP BY specialization")
    fun getCoachCountBySport(academyId: Int): Flow<List<SportCount>>

    @Query("SELECT gender as sport, COUNT(*) as count FROM academy_athletes WHERE academyId = :academyId GROUP BY gender")
    fun getGenderCounts(academyId: Int): Flow<List<SportCount>>

    @Query("SELECT CASE WHEN age < 14 THEN 'U14' WHEN age < 18 THEN 'U18' ELSE 'Senior' END as sport, COUNT(*) as count FROM academy_athletes WHERE academyId = :academyId GROUP BY sport")
    fun getAgeGroupCounts(academyId: Int): Flow<List<SportCount>>

    // --- Facilities ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: Facility)

    @Query("SELECT * FROM academy_facilities WHERE academyId = :academyId")
    fun getFacilitiesFlow(academyId: Int): Flow<List<Facility>>

    @Query("SELECT * FROM academy_facilities WHERE id = :facilityId")
    suspend fun getFacilityById(facilityId: Int): Facility?

    @Delete
    suspend fun deleteFacility(facility: Facility)

    @Query("SELECT sport as sport, COUNT(*) as count FROM academy_facilities WHERE academyId = :academyId GROUP BY sport")
    fun getFacilityCountBySport(academyId: Int): Flow<List<SportCount>>

    @Query("SELECT * FROM academy_coaches WHERE coachId = :coachId")
    suspend fun getCoachById(coachId: Int): Coach?

    @Delete
    suspend fun deleteCoach(coach: Coach)

    @Query("SELECT * FROM teams WHERE teamId = :teamId")
    suspend fun getTeamById(teamId: Int): Team?

    @Delete
    suspend fun deleteTeam(team: Team)

    @Query("SELECT * FROM tournaments WHERE tournamentId = :tournamentId")
    suspend fun getTournamentById(tournamentId: Int): Tournament?

    @Delete
    suspend fun deleteTournament(tournament: Tournament)

    // --- Stats ---
    @Query("SELECT COUNT(*) FROM academy_athletes WHERE academyId = :academyId")
    fun getTotalAthletes(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM academy_athletes WHERE academyId = :academyId AND verificationStatus = 'Verified'")
    fun getVerifiedAthleteCount(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM academy_athletes WHERE academyId = :academyId AND verificationStatus = 'Pending'")
    fun getPendingAthleteCount(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM academy_athletes WHERE academyId = :academyId AND isActive = 1")
    fun getActiveAthleteCount(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM academy_coaches WHERE academyId = :academyId")
    fun getTotalCoaches(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM academy_facilities WHERE academyId = :academyId")
    fun getTotalFacilities(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM tournaments WHERE academyId = :academyId")
    fun getTotalTournaments(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM equipment_inventory WHERE academyId = :academyId")
    fun getTotalInventoryCount(academyId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM training_slots WHERE academyId = :academyId AND endTime > :now")
    fun getActiveSlotBookingCount(academyId: Int, now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM teams WHERE academyId = :academyId")
    fun getTotalTeams(academyId: Int): Flow<Int>

    // --- Athletes ---
    @Query("SELECT * FROM academy_athletes WHERE academyId = :academyId AND sportDomain = :sport")
    fun getAthletesBySport(academyId: Int, sport: String): Flow<List<AcademyAthlete>>

    @Query("SELECT * FROM academy_coaches WHERE academyId = :academyId AND specialization = :sport")
    fun getCoachesBySport(academyId: Int, sport: String): Flow<List<Coach>>

    @Query("SELECT * FROM academy_facilities WHERE academyId = :academyId AND sport = :sport")
    fun getFacilitiesBySport(academyId: Int, sport: String): Flow<List<Facility>>

    @Query("SELECT * FROM tournaments WHERE academyId = :academyId AND sport = :sport")
    fun getTournamentsBySport(academyId: Int, sport: String): Flow<List<Tournament>>

    @Query("SELECT * FROM academy_athletes WHERE athleteId = :athleteId")
    suspend fun getAthleteById(athleteId: Int): AcademyAthlete?

    @Delete
    suspend fun deleteAthlete(athlete: AcademyAthlete)

    @Query("UPDATE academy_athletes SET academyId = :newAcademyId WHERE athleteId = :athleteId")
    suspend fun transferAthlete(athleteId: Int, newAcademyId: Int)

    // --- Teams ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team)

    @Query("SELECT * FROM teams WHERE academyId = :academyId")
    suspend fun getTeamsForAcademy(academyId: Int): List<Team>

    @Query("SELECT * FROM teams WHERE academyId = :academyId")
    fun getTeamsForAcademyFlow(academyId: Int): Flow<List<Team>>

    @Query("SELECT * FROM academy_athletes WHERE teamId = :teamId")
    fun getAthletesForTeamFlow(teamId: Int): Flow<List<AcademyAthlete>>

    @Query("SELECT * FROM academy_athletes WHERE teamId = :teamId")
    suspend fun getAthletesForTeam(teamId: Int): List<AcademyAthlete>

    // --- Equipment ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment)

    @Delete
    suspend fun deleteEquipment(equipment: Equipment)

    @Query("SELECT * FROM equipment_inventory WHERE academyId = :academyId")
    suspend fun getInventory(academyId: Int): List<Equipment>

    // --- Training Slots ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TrainingSlot)

    @Query("SELECT * FROM training_slots WHERE academyId = :academyId")
    suspend fun getSlots(academyId: Int): List<TrainingSlot>

    // --- Attendance ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance_records WHERE academyId = :academyId AND date = :date")
    suspend fun getAttendanceForDate(academyId: Int, date: Long): List<Attendance>

    // --- Tournaments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: Tournament)

    @Query("SELECT * FROM tournaments WHERE academyId = :academyId")
    suspend fun getTournaments(academyId: Int): List<Tournament>

    @Query("SELECT * FROM tournaments WHERE academyId = :academyId ORDER BY startDate DESC")
    fun getTournamentsFlow(academyId: Int): Flow<List<Tournament>>

    // --- Matches ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match)

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId")
    suspend fun getMatchesForTournament(tournamentId: Int): List<Match>

    // --- Medical ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecord)

    @Query("SELECT * FROM medical_records WHERE athleteId = :athleteId")
    suspend fun getMedicalRecords(athleteId: Int): List<MedicalRecord>

    @Query("SELECT * FROM medical_records WHERE athleteId IN (SELECT athleteId FROM academy_athletes WHERE academyId = :academyId) ORDER BY date DESC")
    fun getAcademyMedicalRecords(academyId: Int): Flow<List<MedicalRecord>>

    // --- Diet ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietPlan(plan: DietPlan)

    @Query("SELECT * FROM diet_plans WHERE athleteId = :athleteId")
    suspend fun getDietPlan(athleteId: Int): DietPlan?

    @Query("SELECT * FROM diet_plans WHERE athleteId IN (SELECT athleteId FROM academy_athletes WHERE academyId = :academyId)")
    fun getAcademyDietPlans(academyId: Int): Flow<List<DietPlan>>

    // --- Payroll ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayroll(payroll: Payroll)

    @Query("SELECT * FROM payroll_records WHERE academyId = :academyId ORDER BY paymentDate DESC")
    fun getAcademyPayroll(academyId: Int): Flow<List<Payroll>>

    // --- Tournament Registrations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: TournamentRegistration)

    @Query("SELECT * FROM tournament_registrations WHERE organizerAcademyId = :academyId")
    fun getRegistrationsForOrganizerFlow(academyId: Int): Flow<List<TournamentRegistration>>

    @Query("SELECT * FROM tournament_registrations WHERE athleteEmail = :email")
    fun getRegistrationsForAthleteFlow(email: String): Flow<List<TournamentRegistration>>

    @Query("SELECT * FROM tournament_registrations WHERE registrationId = :id")
    suspend fun getRegistrationById(id: Int): TournamentRegistration?

    @Query("SELECT * FROM tournaments ORDER BY startDate DESC")
    fun getAllTournamentsFlow(): Flow<List<Tournament>>

    // --- Invitations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: AcademyInvitation)

    @Query("SELECT * FROM academy_invitations WHERE academyId = :academyId ORDER BY createdAt DESC")
    fun getInvitationsForAcademyFlow(academyId: Int): Flow<List<AcademyInvitation>>

    @Query("SELECT * FROM academy_invitations WHERE athleteEmail = :email ORDER BY createdAt DESC")
    fun getInvitationsForAthleteFlow(email: String): Flow<List<AcademyInvitation>>

    @Query("SELECT * FROM academy_invitations WHERE id = :id")
    suspend fun getInvitationById(id: Int): AcademyInvitation?

    @Query("SELECT * FROM academy_invitations WHERE academyId = :academyId AND athleteEmail = :athleteEmail AND status = 'PENDING'")
    suspend fun getPendingInvitation(academyId: Int, athleteEmail: String): AcademyInvitation?
}
