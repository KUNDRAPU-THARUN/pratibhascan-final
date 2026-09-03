package com.example.prathibhascanfinal.data.repository

import com.example.prathibhascanfinal.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    fun getFirebaseFirestore(): FirebaseFirestore = db

    fun getUserFlow(email: String): Flow<User?> = callbackFlow {
        val lowerEmail = email.lowercase().trim()
        val listener = db.collection("users")
            .document(lowerEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = if (snapshot != null && snapshot.exists()) snapshot.toObject(User::class.java) else null
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    suspend fun findUserByEmail(email: String): User? {
        val lowerEmail = email.lowercase().trim()
        try {
            // 1. Try direct lookup by ID
            val doc = db.collection("users").document(lowerEmail).get().await()
            if (doc.exists()) return doc.toObject(User::class.java)

            // 2. Try query by email field
            val query = db.collection("users").whereEqualTo("email", lowerEmail).limit(1).get().await()
            if (!query.isEmpty) return query.documents[0].toObject(User::class.java)

            // 3. Search by Role fallback
            val roles = listOf("Athlete", "Academy", "Institution")
            for (role in roles) {
                val roleQuery = db.collection("users").whereEqualTo("role", role).whereEqualTo("email", lowerEmail).limit(1).get().await()
                if (!roleQuery.isEmpty) return roleQuery.documents[0].toObject(User::class.java)
            }

            // 4. Check Academies fallback
            val acaQuery = db.collection("academies").whereEqualTo("contactEmail", lowerEmail).limit(1).get().await()
            if (!acaQuery.isEmpty) {
                val aca = acaQuery.documents[0].toObject(Academy::class.java)
                val newUser = User(
                    email = lowerEmail, 
                    fullName = aca?.academyName ?: "Academy", 
                    role = "Academy", 
                    uniqueId = "PR-ACD-${(100000000..999999999).random()}",
                    location = aca?.city,
                    state = aca?.state
                )
                // Persist this synthetic user so they don't get stuck in setup
                saveEntity("users", lowerEmail, newUser)
                return newUser
            }

            // 5. Check Institutions fallback
            val instQuery = db.collection("institutions").whereEqualTo("contactEmail", lowerEmail).limit(1).get().await()
            if (!instQuery.isEmpty) {
                val inst = instQuery.documents[0].toObject(Institution::class.java)
                val newUser = User(
                    email = lowerEmail, 
                    fullName = inst?.institutionName ?: "Institution", 
                    role = "Institution", 
                    uniqueId = "PR-INS-${(100000000..999999999).random()}",
                    location = inst?.campusAddress,
                    state = inst?.state
                )
                // Persist this synthetic user
                saveEntity("users", lowerEmail, newUser)
                return newUser
            }

            return null
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE_RECOVERY", "Search failed for $lowerEmail", e)
            return null
        }
    }

    // --- Real-time Flows ---

    fun getAcademyFlow(email: String): Flow<Academy?> = callbackFlow {
        val listener = db.collection("academies")
            .whereEqualTo("contactEmail", email)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val academy = if (snapshot != null && !snapshot.isEmpty) snapshot.documents[0].toObject(Academy::class.java) else null
                trySend(academy)
            }
        awaitClose { listener.remove() }
    }

    fun getAthletesFlow(academyId: Int): Flow<List<AcademyAthlete>> = callbackFlow {
        val listener = db.collection("athletes")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(AcademyAthlete::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getAthletesCountFlow(academyId: Int): Flow<Int> = callbackFlow {
        val listener = db.collection("athletes")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    fun getCoachesFlow(academyId: Int): Flow<List<Coach>> = callbackFlow {
        val listener = db.collection("coaches")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Coach::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getCoachesCountFlow(academyId: Int): Flow<Int> = callbackFlow {
        val listener = db.collection("coaches")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    fun getTeamsFlow(academyId: Int): Flow<List<Team>> = callbackFlow {
        val listener = db.collection("teams")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Team::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getTournamentsFlow(academyId: Int): Flow<List<Tournament>> = callbackFlow {
        val listener = db.collection("tournaments")
            .whereEqualTo("academyId", academyId)
            .orderBy("startDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Tournament::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getFacilitiesFlow(academyId: Int): Flow<List<Facility>> = callbackFlow {
        val listener = db.collection("facilities")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Facility::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getRegistrationsForOrganizerFlow(academyId: Int): Flow<List<TournamentRegistration>> = callbackFlow {
        val listener = db.collection("tournament_registrations")
            .whereEqualTo("organizerAcademyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(TournamentRegistration::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getRegistrationsForAthleteFlow(email: String): Flow<List<TournamentRegistration>> = callbackFlow {
        val listener = db.collection("tournament_registrations")
            .whereEqualTo("athleteEmail", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(TournamentRegistration::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getInvitationsForAcademyFlow(academyId: Int): Flow<List<AcademyInvitation>> = callbackFlow {
        val listener = db.collection("academy_invitations")
            .whereEqualTo("academyId", academyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(AcademyInvitation::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getInvitationsForAthleteFlow(email: String): Flow<List<AcademyInvitation>> = callbackFlow {
        val listener = db.collection("academy_invitations")
            .whereEqualTo("athleteEmail", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(AcademyInvitation::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getDiscoveryAthletesFlow(sport: String? = null): Flow<List<User>> = callbackFlow {
        // Relaxed query: only filter by role(s), handle privacy in-memory for the demo
        val query = db.collection("users")
            .whereIn("role", listOf("Athlete", "athlete")) 
            
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FIRESTORE_DISCOVERY", "Query failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(User::class.java) ?: emptyList()
            
            // Filter by sport and privacy in-memory to avoid complex index requirements for the demo
            val filtered = list.filter { 
                (sport == null || it.primaryDiscipline.equals(sport, ignoreCase = true)) &&
                it.privacy != "Private" 
            }
            trySend(filtered)
        }
        awaitClose { listener.remove() }
    }

    // --- Institution Flows ---

    fun getInstitutionFlow(email: String): Flow<Institution?> = callbackFlow {
        val listener = db.collection("institutions")
            .whereEqualTo("contactEmail", email)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val inst = if (snapshot != null && !snapshot.isEmpty) snapshot.documents[0].toObject(Institution::class.java) else null
                trySend(inst)
            }
        awaitClose { listener.remove() }
    }

    fun getStudentsFlow(instId: Int): Flow<List<Student>> = callbackFlow {
        val listener = db.collection("institution_students")
            .whereEqualTo("institutionId", instId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Student::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getTeachersFlow(instId: Int): Flow<List<InstitutionTeacher>> = callbackFlow {
        val listener = db.collection("teachers")
            .whereEqualTo("institutionId", instId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(InstitutionTeacher::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getTeamMembersFlow(teamId: Int): Flow<List<InstitutionTeamMember>> = callbackFlow {
        val listener = db.collection("institution_team_members")
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(InstitutionTeamMember::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // --- CRUD Operations ---

    suspend fun saveEntity(collection: String, id: String, data: Any): Boolean {
        return try {
            db.collection(collection).document(id).set(data, SetOptions.merge()).await()
            android.util.Log.d("FIRESTORE_SYNC", "SUCCESS: Document [$id] saved to [$collection]")
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE_SYNC", "FAILURE: Document [$id] failed to save to [$collection]: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteEntity(collection: String, id: String): Boolean {
        return try {
            db.collection(collection).document(id).delete().await()
            android.util.Log.d("FIRESTORE_SYNC", "SUCCESS: Document [$id] deleted from [$collection]")
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE_SYNC", "FAILURE: Document [$id] failed to delete from [$collection]: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Specialized Helpers
    suspend fun saveAcademyProfile(academy: Academy): Boolean {
        val id = academy.id.takeIf { it > 0 }?.toString() ?: db.collection("academies").document().id
        return saveEntity("academies", id, academy)
    }

    suspend fun saveAthlete(athlete: AcademyAthlete): Boolean {
        val id = athlete.athleteId.takeIf { it > 0 }?.toString() ?: db.collection("athletes").document().id
        return saveEntity("athletes", id, athlete)
    }

    suspend fun saveCoach(coach: Coach): Boolean {
        val id = coach.coachId.takeIf { it > 0 }?.toString() ?: db.collection("coaches").document().id
        return saveEntity("coaches", id, coach)
    }

    suspend fun saveTeam(team: Team): Boolean {
        val id = team.teamId.takeIf { it > 0 }?.toString() ?: db.collection("teams").document().id
        return saveEntity("teams", id, team)
    }

    suspend fun saveTournament(tournament: Tournament): Boolean {
        val id = tournament.tournamentId.takeIf { it > 0 }?.toString() ?: db.collection("tournaments").document().id
        return saveEntity("tournaments", id, tournament)
    }

    suspend fun saveFacility(facility: Facility): Boolean {
        val id = facility.id.takeIf { it > 0 }?.toString() ?: db.collection("facilities").document().id
        return saveEntity("facilities", id, facility)
    }

    suspend fun saveInstitution(inst: Institution): Boolean {
        val id = inst.id.takeIf { it > 0 }?.toString() ?: db.collection("institutions").document().id
        return saveEntity("institutions", id, inst)
    }

    suspend fun saveStudent(student: Student): Boolean {
        val id = student.studentId.takeIf { it > 0 }?.toString() ?: db.collection("institution_students").document().id
        return saveEntity("institution_students", id, student)
    }

    suspend fun saveTeacher(teacher: InstitutionTeacher): Boolean {
        val id = teacher.teacherId.takeIf { it > 0 }?.toString() ?: db.collection("teachers").document().id
        return saveEntity("teachers", id, teacher)
    }

    suspend fun saveRegistration(registration: TournamentRegistration): Boolean {
        val id = registration.registrationId.takeIf { it > 0 }?.toString() ?: db.collection("tournament_registrations").document().id
        return saveEntity("tournament_registrations", id, registration)
    }

    suspend fun uploadImage(path: String, uri: android.net.Uri): String? {
        return try {
            val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }
}
