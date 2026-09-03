package com.example.prathibhascanfinal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.adapter.AcademyTeamMemberAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademyTeamDetailActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository
    private val firestoreRepo = FirestoreRepository()

    private var teamId: Int = 0
    private var academyId: Int = 0
    private var currentTeam: Team? = null

    private lateinit var adapter: AcademyTeamMemberAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var allRosterAthletes = listOf<AcademyAthlete>()
    private var allAcademyAthletes = listOf<AcademyAthlete>()
    private var academyCoaches = listOf<Coach>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_team_detail)
        setupEdgeToEdge(findViewById(R.id.team_detail_root))

        teamId = intent.getIntExtra("TEAM_ID", 0)
        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        repository = AcademyRepository(this)

        initUI()
        loadData()
    }

    private fun initUI() {
        findViewById<View>(R.id.btn_back_academy_team_detail)?.setOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh_academy_roster)
        swipeRefresh.setOnRefreshListener { loadData() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_academy_team_roster)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AcademyTeamMemberAdapter(
            onViewPlayer = { showPlayerProfileDialog(it) },
            onEditPlayer = { showEditPlayerDialog(it) },
            onRemovePlayer = { confirmRemovePlayer(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.btn_edit_academy_team_info)?.setOnClickListener { showEditTeamDialog() }
        findViewById<View>(R.id.btn_delete_academy_team_info)?.setOnClickListener { confirmDeleteTeam() }

        findViewById<View>(R.id.btn_add_player_to_academy_team)?.setOnClickListener { showAddPlayerDialog() }
        findViewById<View>(R.id.btn_empty_add_academy_player)?.setOnClickListener { showAddPlayerDialog() }

        findViewById<View>(R.id.btn_manage_academy_captain)?.setOnClickListener { showLeadershipDialog(isCaptain = true) }
        findViewById<View>(R.id.btn_manage_academy_vice_captain)?.setOnClickListener { showLeadershipDialog(isCaptain = false) }

        findViewById<EditText>(R.id.et_search_academy_roster)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRoster(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        if (teamId <= 0) {
            Toast.makeText(this, "Invalid Team ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        swipeRefresh.isRefreshing = true

        val db = AppDatabase.getDatabase(this)

        // 1. Observe Team Info
        lifecycleScope.launch {
            val team = db.academyManagementDao().getTeamById(teamId)
            if (team != null) {
                currentTeam = team
                updateTeamHeaderUI(team)
            }
        }

        // 2. Observe Coaches
        lifecycleScope.launch {
            db.academyManagementDao().getCoachesFlow(academyId).collectLatest { coaches ->
                academyCoaches = coaches
            }
        }

        // 3. Observe All Academy Athletes Flow
        lifecycleScope.launch {
            db.academyManagementDao().getAthletesFlow(academyId).collectLatest { allAthletes ->
                allAcademyAthletes = allAthletes
                allRosterAthletes = allAthletes.filter { it.teamId == teamId }
                updateRosterUI()
                updateLeadershipCard()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateTeamHeaderUI(team: Team) {
        findViewById<TextView>(R.id.tv_academy_team_name)?.text = team.teamName
        findViewById<TextView>(R.id.tv_academy_team_sport)?.text = team.sport.ifEmpty { "General" }
        findViewById<TextView>(R.id.tv_academy_team_category)?.text = team.category.ifEmpty { team.ageGroup.ifEmpty { "Open" } }
        findViewById<TextView>(R.id.tv_academy_team_gender)?.text = team.gender.ifEmpty { "Mixed" }
        findViewById<TextView>(R.id.tv_academy_team_status)?.text = team.status.ifEmpty { "ACTIVE" }.uppercase()

        val coachStr = team.coachName ?: if (team.coachId > 0) "Coach #${team.coachId}" else "Unassigned"
        findViewById<TextView>(R.id.tv_academy_team_coach_info)?.text = "Coach/In-charge: $coachStr"
    }

    private fun updateLeadershipCard() {
        val captain = allRosterAthletes.find { it.role.equals("Captain", ignoreCase = true) || it.athleteId == currentTeam?.captainId }
        val viceCaptain = allRosterAthletes.find { it.role.contains("Vice", ignoreCase = true) || it.athleteId == currentTeam?.viceCaptainId }

        val tvCaptName = findViewById<TextView>(R.id.tv_academy_captain_name)
        val tvCaptJersey = findViewById<TextView>(R.id.tv_academy_captain_jersey)
        val tvViceName = findViewById<TextView>(R.id.tv_academy_vice_captain_name)
        val tvViceJersey = findViewById<TextView>(R.id.tv_academy_vice_captain_jersey)

        if (captain != null) {
            tvCaptName?.text = captain.fullName
            tvCaptJersey?.text = if (captain.jerseyNumber.isNotEmpty()) "Jersey: #${captain.jerseyNumber}" else "Captain"
        } else {
            tvCaptName?.text = "Not Assigned"
            tvCaptJersey?.text = "Jersey: --"
        }

        if (viceCaptain != null) {
            tvViceName?.text = viceCaptain.fullName
            tvViceJersey?.text = if (viceCaptain.jerseyNumber.isNotEmpty()) "Jersey: #${viceCaptain.jerseyNumber}" else "Vice-Captain"
        } else {
            tvViceName?.text = "Not Assigned"
            tvViceJersey?.text = "Jersey: --"
        }

        val activeCount = allRosterAthletes.count { it.isActive && it.membershipStatus.equals("Active", ignoreCase = true) }
        findViewById<TextView>(R.id.tv_academy_team_roster_stats)?.text = "Total Players: ${allRosterAthletes.size} | Active Players: $activeCount"
    }

    private fun updateRosterUI() {
        val query = findViewById<EditText>(R.id.et_search_academy_roster)?.text?.toString() ?: ""
        filterRoster(query)
    }

    private fun filterRoster(query: String) {
        val filtered = if (query.trim().isEmpty()) {
            allRosterAthletes
        } else {
            allRosterAthletes.filter { item ->
                item.fullName.contains(query, ignoreCase = true) ||
                item.admissionNumber.contains(query, ignoreCase = true) ||
                item.jerseyNumber.contains(query, ignoreCase = true) ||
                item.position.contains(query, ignoreCase = true)
            }
        }

        adapter.submitList(filtered)
        findViewById<TextView>(R.id.tv_academy_roster_count_title)?.text = "Team Roster (${allRosterAthletes.size} Players)"
        findViewById<View>(R.id.layout_empty_academy_roster)?.visibility = if (allRosterAthletes.isEmpty()) View.VISIBLE else View.GONE
    }

    // --- Add Player Dialog ---
    private fun showAddPlayerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_academy_player, null)
        val spDirectory = dialogView.findViewById<Spinner>(R.id.sp_select_existing_athlete)
        val etName = dialogView.findViewById<EditText>(R.id.et_add_player_name)
        val etJersey = dialogView.findViewById<EditText>(R.id.et_add_player_jersey)
        val spPosition = dialogView.findViewById<Spinner>(R.id.sp_add_player_position)
        val spRole = dialogView.findViewById<Spinner>(R.id.sp_add_player_role)
        val etAge = dialogView.findViewById<EditText>(R.id.et_add_player_age)
        val spPerf = dialogView.findViewById<Spinner>(R.id.sp_add_player_performance)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_add_player_phone)
        val etEmergency = dialogView.findViewById<EditText>(R.id.et_add_player_emergency)

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_add_academy_player)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_academy_player)

        // 1. Populate Existing Directory Athletes Dropdown
        val availableDirectoryAthletes = allAcademyAthletes.filter { it.teamId != teamId }
        val directoryLabels = mutableListOf("-- Choose from Athlete Directory --")
        directoryLabels.addAll(availableDirectoryAthletes.map { "${it.fullName} (ID: ${it.admissionNumber.ifEmpty { it.athleteId.toString() }})" })

        val directoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, directoryLabels)
        spDirectory.adapter = directoryAdapter

        spDirectory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && (position - 1) in availableDirectoryAthletes.indices) {
                    val sel = availableDirectoryAthletes[position - 1]
                    etName.setText(sel.fullName)
                    etAge.setText(if (sel.age > 0) sel.age.toString() else "")
                    etPhone.setText(sel.contactNumber)
                    etEmergency.setText(sel.parentPhone)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Populate Positions Dropdown
        val sportName = currentTeam?.sport ?: "Cricket"
        val positionOptions = SportsPositionHelper.getPositionsForSport(sportName)
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positionOptions)
        spPosition.adapter = posAdapter

        // 3. Roles & Performance
        val roleOptions = listOf("Player", "Captain", "Vice-Captain")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roleOptions)
        spRole.adapter = roleAdapter

        val perfOptions = listOf("Intermediate", "Beginner", "Advanced", "Elite")
        val perfAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, perfOptions)
        spPerf.adapter = perfAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val directoryIdx = spDirectory.selectedItemPosition
            val name = etName.text.toString().trim()
            val jersey = etJersey.text.toString().trim()
            val positionStr = spPosition.selectedItem?.toString() ?: "Player"
            val roleStr = spRole.selectedItem?.toString() ?: "Player"
            val ageVal = etAge.text.toString().toIntOrNull() ?: 0
            val perfStr = spPerf.selectedItem?.toString() ?: "Intermediate"
            val phone = etPhone.text.toString().trim()
            val emergency = etEmergency.text.toString().trim()

            if (directoryIdx == 0 && name.isEmpty()) {
                etName.error = "Full Name is required"
                etName.requestFocus()
                return@setOnClickListener
            }

            // Check duplicate jersey
            if (jersey.isNotEmpty() && allRosterAthletes.any { it.jerseyNumber == jersey }) {
                Toast.makeText(this, "Jersey number #$jersey is already assigned to another athlete in this team!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val targetAthlete = if (directoryIdx > 0 && (directoryIdx - 1) in availableDirectoryAthletes.indices) {
                availableDirectoryAthletes[directoryIdx - 1].copy(
                    teamId = teamId,
                    jerseyNumber = jersey,
                    position = positionStr,
                    role = roleStr,
                    performanceLevel = perfStr
                )
            } else {
                AcademyAthlete(
                    academyId = academyId,
                    teamId = teamId,
                    fullName = name,
                    jerseyNumber = jersey,
                    position = positionStr,
                    role = roleStr,
                    performanceLevel = perfStr,
                    age = ageVal,
                    contactNumber = phone,
                    parentPhone = emergency,
                    sportDomain = currentTeam?.sport ?: "Cricket",
                    admissionNumber = "ATH-${(1000..9999).random()}"
                )
            }

            handleRoleAndSaveAthlete(targetAthlete, roleStr, dialog)
        }

        dialog.show()
    }

    private fun handleRoleAndSaveAthlete(athlete: AcademyAthlete, role: String, dialog: AlertDialog) {
        val isCaptain = role.equals("Captain", ignoreCase = true)
        val isViceCaptain = role.contains("Vice", ignoreCase = true)

        val existingCaptain = allRosterAthletes.find { it.role.equals("Captain", ignoreCase = true) }
        val existingVice = allRosterAthletes.find { it.role.contains("Vice", ignoreCase = true) }

        if (isCaptain && existingCaptain != null && existingCaptain.athleteId != athlete.athleteId) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Captain Already Assigned")
                .setMessage("'$existingCaptain.fullName' is currently Captain of this team. Replace current Captain?")
                .setPositiveButton("REPLACE") { _, _ ->
                    lifecycleScope.launch {
                        val demoted = existingCaptain.copy(role = "Player")
                        repository.registerAthlete(demoted)
                        saveAthleteToTeam(athlete, dialog)
                    }
                }
                .setNegativeButton("CANCEL", null)
                .show()
        } else if (isViceCaptain && existingVice != null && existingVice.athleteId != athlete.athleteId) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Vice-Captain Already Assigned")
                .setMessage("'$existingVice.fullName' is currently Vice-Captain of this team. Replace current Vice-Captain?")
                .setPositiveButton("REPLACE") { _, _ ->
                    lifecycleScope.launch {
                        val demoted = existingVice.copy(role = "Player")
                        repository.registerAthlete(demoted)
                        saveAthleteToTeam(athlete, dialog)
                    }
                }
                .setNegativeButton("CANCEL", null)
                .show()
        } else {
            saveAthleteToTeam(athlete, dialog)
        }
    }

    private fun saveAthleteToTeam(athlete: AcademyAthlete, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                repository.registerAthlete(athlete)

                // Update team captain / vice-captain refs if needed
                currentTeam?.let { team ->
                    var newCaptId = team.captainId
                    var newViceId = team.viceCaptainId

                    if (athlete.role.equals("Captain", ignoreCase = true)) {
                        newCaptId = athlete.athleteId
                    }
                    if (athlete.role.contains("Vice", ignoreCase = true)) {
                        newViceId = athlete.athleteId
                    }

                    val updatedTeam = team.copy(captainId = newCaptId, viceCaptainId = newViceId)
                    if (updatedTeam != team) {
                        repository.addTeam(updatedTeam)
                        currentTeam = updatedTeam
                    }
                }

                Snackbar.make(findViewById(R.id.rv_academy_team_roster), "✔ ${athlete.fullName} added to team roster", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this@AcademyTeamDetailActivity, "Error saving player: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Edit Player Dialog ---
    private fun showEditPlayerDialog(athlete: AcademyAthlete) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_academy_player, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_edit_academy_player_title)
        val tvSubTitle = dialogView.findViewById<TextView>(R.id.tv_edit_academy_player_subtitle)
        val etJersey = dialogView.findViewById<EditText>(R.id.et_edit_academy_jersey)
        val spPosition = dialogView.findViewById<Spinner>(R.id.sp_edit_academy_position)
        val spRole = dialogView.findViewById<Spinner>(R.id.sp_edit_academy_role)
        val spPerf = dialogView.findViewById<Spinner>(R.id.sp_edit_academy_performance)
        val spStatus = dialogView.findViewById<Spinner>(R.id.sp_edit_academy_status)

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_edit_academy_player)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_edit_academy_player)

        tvTitle.text = "Edit ${athlete.fullName}"
        tvSubTitle.text = "Athlete ID: ${athlete.admissionNumber.ifEmpty { athlete.athleteId.toString() }}"
        etJersey.setText(athlete.jerseyNumber)

        // Positions
        val sportName = currentTeam?.sport ?: athlete.sportDomain
        val positionOptions = SportsPositionHelper.getPositionsForSport(sportName)
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positionOptions)
        spPosition.adapter = posAdapter
        if (positionOptions.contains(athlete.position)) {
            spPosition.setSelection(positionOptions.indexOf(athlete.position))
        }

        // Roles & Performance & Status
        val roleOptions = listOf("Player", "Captain", "Vice-Captain")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roleOptions)
        spRole.adapter = roleAdapter
        val currentRole = when {
            athlete.role.equals("Captain", ignoreCase = true) -> "Captain"
            athlete.role.contains("Vice", ignoreCase = true) -> "Vice-Captain"
            else -> "Player"
        }
        spRole.setSelection(roleOptions.indexOf(currentRole))

        val perfOptions = listOf("Intermediate", "Beginner", "Advanced", "Elite")
        val perfAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, perfOptions)
        spPerf.adapter = perfAdapter
        if (perfOptions.contains(athlete.performanceLevel)) {
            spPerf.setSelection(perfOptions.indexOf(athlete.performanceLevel))
        }

        val statusOptions = listOf("Active", "Inactive")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusOptions)
        spStatus.adapter = statusAdapter
        val currentStatus = if (athlete.isActive && athlete.membershipStatus.equals("Active", ignoreCase = true)) "Active" else "Inactive"
        spStatus.setSelection(statusOptions.indexOf(currentStatus))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val jersey = etJersey.text.toString().trim()
            val positionStr = spPosition.selectedItem?.toString() ?: "Player"
            val roleStr = spRole.selectedItem?.toString() ?: "Player"
            val perfStr = spPerf.selectedItem?.toString() ?: "Intermediate"
            val statusStr = spStatus.selectedItem?.toString() ?: "Active"

            // Validate duplicate jersey number
            if (jersey.isNotEmpty() && allRosterAthletes.any { it.jerseyNumber == jersey && it.athleteId != athlete.athleteId }) {
                Toast.makeText(this, "Jersey number #$jersey is already assigned to another player in this team!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val updatedAthlete = athlete.copy(
                jerseyNumber = jersey,
                position = positionStr,
                role = roleStr,
                performanceLevel = perfStr,
                membershipStatus = statusStr,
                isActive = (statusStr == "Active")
            )

            handleRoleAndSaveAthlete(updatedAthlete, roleStr, dialog)
        }

        dialog.show()
    }

    // --- Remove Player Dialog ---
    private fun confirmRemovePlayer(athlete: AcademyAthlete) {
        val isLeadership = athlete.role.equals("Captain", ignoreCase = true) || athlete.role.contains("Vice", ignoreCase = true)

        val message = if (isLeadership) {
            "'${athlete.fullName}' is currently ${athlete.role.uppercase()}.\n\nRemove from team and release ${athlete.role} status?\n(Note: The athlete profile will NOT be deleted from the Academy Directory)."
        } else {
            "Remove '${athlete.fullName}' from '${currentTeam?.teamName ?: "team"}'?\n(Note: The athlete profile will NOT be deleted from the Academy Directory)."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Player from Team")
            .setMessage(message)
            .setPositiveButton("REMOVE") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val unlinkedAthlete = athlete.copy(teamId = null, role = "Player")
                        repository.registerAthlete(unlinkedAthlete)

                        // Clear team leadership if needed
                        currentTeam?.let { team ->
                            var newCaptId = team.captainId
                            var newViceId = team.viceCaptainId

                            if (team.captainId == athlete.athleteId) newCaptId = null
                            if (team.viceCaptainId == athlete.athleteId) newViceId = null

                            val updatedTeam = team.copy(captainId = newCaptId, viceCaptainId = newViceId)
                            if (updatedTeam != team) {
                                repository.addTeam(updatedTeam)
                                currentTeam = updatedTeam
                            }
                        }

                        Snackbar.make(findViewById(R.id.rv_academy_team_roster), "Player removed from team", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyTeamDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // --- Leadership Assignment Dialog ---
    private fun showLeadershipDialog(isCaptain: Boolean) {
        if (allRosterAthletes.isEmpty()) {
            Toast.makeText(this, "Add players to team roster first", Toast.LENGTH_SHORT).show()
            return
        }

        val roleTitle = if (isCaptain) "Captain" else "Vice-Captain"
        val candidateNames = allRosterAthletes.map { it.fullName }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Assign $roleTitle")
            .setItems(candidateNames) { _, which ->
                val selectedAthlete = allRosterAthletes[which]
                val updatedAthlete = selectedAthlete.copy(role = roleTitle)
                handleRoleAndSaveAthlete(updatedAthlete, roleTitle, AlertDialog.Builder(this).create())
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // --- View Player Profile Dialog ---
    private fun showPlayerProfileDialog(athlete: AcademyAthlete) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_academy_player, null)

        dialogView.findViewById<TextView>(R.id.tv_academy_profile_name).text = athlete.fullName
        dialogView.findViewById<TextView>(R.id.tv_academy_profile_id).text = "ID: ${athlete.admissionNumber.ifEmpty { athlete.athleteId.toString() }} | DOB: ${athlete.dob.ifEmpty { "--" }}"
        dialogView.findViewById<TextView>(R.id.tv_academy_profile_demographics).text = "Age: ${athlete.age} | Gender: ${athlete.gender} | Blood: ${athlete.bloodGroup}"
        dialogView.findViewById<TextView>(R.id.tv_academy_profile_sport_primary).text = "Sport: ${athlete.sportDomain} | Position: ${athlete.position} | Jersey: #${athlete.jerseyNumber.ifEmpty { "--" }}"
        dialogView.findViewById<TextView>(R.id.tv_academy_profile_physical).text = "Height: ${athlete.heightCm} cm | Weight: ${athlete.weightKg} kg | BMI: ${athlete.bmi}"
        dialogView.findViewById<TextView>(R.id.tv_academy_profile_contacts).text = "Phone: ${athlete.contactNumber.ifEmpty { "N/A" }} | Email: ${athlete.email.ifEmpty { "N/A" }}"
        dialogView.findViewById<TextView>(R.id.tv_academy_profile_emergency).text = "Parent/Emergency: ${athlete.parentPhone.ifEmpty { "N/A" }}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_close_academy_player_profile).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // --- Edit & Delete Team Actions ---
    private fun showEditTeamDialog() {
        val team = currentTeam ?: return
        lifecycleScope.launch {
            repository.addTeam(team)
            Toast.makeText(this@AcademyTeamDetailActivity, "Team updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteTeam() {
        val team = currentTeam ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Team")
            .setMessage("Are you sure you want to delete '${team.teamName}'?\n\nDeleting the team will unlink team roster membership but will NOT delete athlete profiles from the Academy Directory.")
            .setPositiveButton("DELETE") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteTeam(team)
                    Toast.makeText(this@AcademyTeamDetailActivity, "Team Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}
