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
import com.example.prathibhascanfinal.data.repository.InstitutionRepository
import com.example.prathibhascanfinal.ui.adapter.InstitutionTeamMemberAdapter
import com.example.prathibhascanfinal.ui.adapter.TeamMemberUiModel
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstitutionTeamDetailActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: InstitutionRepository

    private var teamId: Int = 0
    private var institutionId: Int = 0
    private var currentTeam: InstitutionTeam? = null

    private lateinit var adapter: InstitutionTeamMemberAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var allRosterMembers = listOf<InstitutionTeamMember>()
    private var allStudentsMap = mapOf<Int, Student>()
    private var allStudentsList = listOf<Student>()
    private var currentUiList = listOf<TeamMemberUiModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_team_detail)
        setupEdgeToEdge(findViewById(R.id.inst_team_detail_root))

        teamId = intent.getIntExtra("TEAM_ID", 0)
        institutionId = intent.getIntExtra("INSTITUTION_ID", 0)

        repository = InstitutionRepository(this)

        initUI()
        loadData()
    }

    private fun initUI() {
        findViewById<View>(R.id.btn_back_team_detail)?.setOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh_roster)
        swipeRefresh.setOnRefreshListener { loadData() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_team_roster)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InstitutionTeamMemberAdapter(
            onViewProfile = { showPlayerProfileDialog(it) },
            onEditMember = { showEditMemberDialog(it) },
            onRemoveMember = { confirmRemoveMember(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.btn_edit_team_info)?.setOnClickListener { showEditTeamDialog() }
        findViewById<View>(R.id.btn_delete_team_info)?.setOnClickListener { confirmDeleteTeam() }

        findViewById<View>(R.id.btn_add_player_to_team)?.setOnClickListener { showAddPlayerDialog() }
        findViewById<View>(R.id.btn_empty_add_player)?.setOnClickListener { showAddPlayerDialog() }

        findViewById<View>(R.id.btn_manage_captain)?.setOnClickListener { showLeadershipDialog(isCaptain = true) }
        findViewById<View>(R.id.btn_manage_vice_captain)?.setOnClickListener { showLeadershipDialog(isCaptain = false) }

        findViewById<EditText>(R.id.et_search_roster)?.addTextChangedListener(object : TextWatcher {
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
            val team = db.institutionManagementDao().getTeamById(teamId)
            if (team != null) {
                currentTeam = team
                updateTeamHeaderUI(team)
            }
        }

        // 2. Observe Institution Students
        lifecycleScope.launch {
            db.institutionManagementDao().getStudentsFlow(institutionId).collectLatest { students ->
                allStudentsList = students
                allStudentsMap = students.associateBy { it.studentId }
                updateRosterUI()
            }
        }

        // 3. Observe Team Members Flow
        lifecycleScope.launch {
            repository.getTeamMembersFlow(teamId).collectLatest { members ->
                allRosterMembers = members
                updateRosterUI()
                updateLeadershipCard()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateTeamHeaderUI(team: InstitutionTeam) {
        findViewById<TextView>(R.id.tv_detail_team_name)?.text = team.teamName
        findViewById<TextView>(R.id.tv_detail_sport)?.text = team.sport.ifEmpty { "General" }
        findViewById<TextView>(R.id.tv_detail_category)?.text = team.category.ifEmpty { "Open" }
        findViewById<TextView>(R.id.tv_detail_gender)?.text = team.gender.ifEmpty { "Mixed" }
        findViewById<TextView>(R.id.tv_detail_status)?.text = team.status.ifEmpty { "ACTIVE" }.uppercase()

        val coachStr = team.coachName ?: team.teacherInCharge ?: "Unassigned"
        findViewById<TextView>(R.id.tv_detail_coach)?.text = "Coach/In-charge: $coachStr"
        findViewById<TextView>(R.id.tv_detail_description)?.text = team.description ?: "No description provided."
    }

    private fun updateLeadershipCard() {
        val captainMember = allRosterMembers.find { it.role.equals("Captain", ignoreCase = true) || it.studentId == currentTeam?.captainId }
        val viceCaptainMember = allRosterMembers.find { it.role.contains("Vice", ignoreCase = true) || it.studentId == currentTeam?.viceCaptainId }

        val captainStudent = captainMember?.let { allStudentsMap[it.studentId] }
        val viceCaptainStudent = viceCaptainMember?.let { allStudentsMap[it.studentId] }

        val tvCaptName = findViewById<TextView>(R.id.tv_captain_name)
        val tvCaptJersey = findViewById<TextView>(R.id.tv_captain_jersey)
        val tvViceName = findViewById<TextView>(R.id.tv_vice_captain_name)
        val tvViceJersey = findViewById<TextView>(R.id.tv_vice_captain_jersey)

        if (captainStudent != null) {
            tvCaptName?.text = captainStudent.fullName
            tvCaptJersey?.text = if (!captainMember?.jerseyNumber.isNullOrEmpty()) "Jersey: #${captainMember?.jerseyNumber}" else "Captain"
        } else {
            tvCaptName?.text = "Not Assigned"
            tvCaptJersey?.text = "Jersey: --"
        }

        if (viceCaptainStudent != null) {
            tvViceName?.text = viceCaptainStudent.fullName
            tvViceJersey?.text = if (!viceCaptainMember?.jerseyNumber.isNullOrEmpty()) "Jersey: #${viceCaptainMember?.jerseyNumber}" else "Vice-Captain"
        } else {
            tvViceName?.text = "Not Assigned"
            tvViceJersey?.text = "Jersey: --"
        }
    }

    private fun updateRosterUI() {
        val uiModels = allRosterMembers.map { member ->
            TeamMemberUiModel(
                member = member,
                student = allStudentsMap[member.studentId]
            )
        }
        currentUiList = uiModels

        val query = findViewById<EditText>(R.id.et_search_roster)?.text?.toString() ?: ""
        filterRoster(query)
    }

    private fun filterRoster(query: String) {
        val filtered = if (query.trim().isEmpty()) {
            currentUiList
        } else {
            currentUiList.filter { item ->
                val nameMatch = item.student?.fullName?.contains(query, ignoreCase = true) == true
                val jerseyMatch = item.member.jerseyNumber.contains(query, ignoreCase = true)
                val posMatch = item.member.position.contains(query, ignoreCase = true)
                nameMatch || jerseyMatch || posMatch
            }
        }

        adapter.submitList(filtered)
        findViewById<TextView>(R.id.tv_roster_count_title)?.text = "Team Roster (${allRosterMembers.size} Players)"
        findViewById<View>(R.id.layout_empty_roster)?.visibility = if (allRosterMembers.isEmpty()) View.VISIBLE else View.GONE
    }

    // --- Add Player Dialog ---
    private fun showAddPlayerDialog() {
        val existingStudentIds = allRosterMembers.map { it.studentId }.toSet()
        val availableStudents = allStudentsList.filter { !existingStudentIds.contains(it.studentId) }

        if (availableStudents.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("No Eligible Students")
                .setMessage("All registered students in the institution are already added to this team, or no students exist in Student Directory.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_player_to_team, null)
        val spStudent = dialogView.findViewById<Spinner>(R.id.sp_select_student)
        val tvPreviewName = dialogView.findViewById<TextView>(R.id.tv_preview_student_name)
        val tvPreviewDetails = dialogView.findViewById<TextView>(R.id.tv_preview_student_details)
        val etJersey = dialogView.findViewById<EditText>(R.id.et_dialog_jersey)
        val spPosition = dialogView.findViewById<Spinner>(R.id.sp_dialog_position)
        val spRole = dialogView.findViewById<Spinner>(R.id.sp_dialog_role)
        val spPerf = dialogView.findViewById<Spinner>(R.id.sp_dialog_performance)
        val etNotes = dialogView.findViewById<EditText>(R.id.et_dialog_player_notes)

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_add_player)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_player)

        // 1. Populate Student Spinner
        val studentLabels = availableStudents.map { "${it.fullName} (ID: ${it.studentId} | Class: ${it.grade})" }
        val studentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, studentLabels)
        spStudent.adapter = studentAdapter

        spStudent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in availableStudents.indices) {
                    val stu = availableStudents[position]
                    tvPreviewName.text = stu.fullName
                    tvPreviewDetails.text = "Roll: ${stu.rollNumber} | Grade: ${stu.grade}-${stu.section} | Sport: ${stu.selectedSport}"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Populate Positions
        val sportName = currentTeam?.sport ?: "Cricket"
        val positionOptions = SportsPositionHelper.getPositionsForSport(sportName)
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positionOptions)
        spPosition.adapter = posAdapter

        // 3. Populate Roles & Performance
        val roleOptions = listOf("Player", "Captain", "Vice-Captain")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roleOptions)
        spRole.adapter = roleAdapter

        val perfOptions = listOf("Good", "Excellent", "Average")
        val perfAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, perfOptions)
        spPerf.adapter = perfAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val selectedIdx = spStudent.selectedItemPosition
            if (selectedIdx !in availableStudents.indices) {
                Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val student = availableStudents[selectedIdx]
            val jersey = etJersey.text.toString().trim()
            val positionStr = spPosition.selectedItem?.toString() ?: "Player"
            val roleStr = spRole.selectedItem?.toString() ?: "Player"
            val perfStr = spPerf.selectedItem?.toString() ?: "Good"
            val notesStr = etNotes.text.toString().trim()

            // Check duplicate jersey number
            if (jersey.isNotEmpty() && allRosterMembers.any { it.jerseyNumber == jersey }) {
                Toast.makeText(this, "Jersey number #$jersey is already assigned to another team member!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // If assigning Captain / Vice-Captain, handle role rules
            handleRoleAndAddPlayer(
                student = student,
                jersey = jersey,
                position = positionStr,
                role = roleStr,
                performance = perfStr,
                notes = notesStr,
                dialog = dialog
            )
        }

        dialog.show()
    }

    private fun handleRoleAndAddPlayer(
        student: Student,
        jersey: String,
        position: String,
        role: String,
        performance: String,
        notes: String,
        dialog: AlertDialog
    ) {
        val isCaptain = role.equals("Captain", ignoreCase = true)
        val isViceCaptain = role.contains("Vice", ignoreCase = true)

        val existingCaptain = allRosterMembers.find { it.role.equals("Captain", ignoreCase = true) }
        val existingVice = allRosterMembers.find { it.role.contains("Vice", ignoreCase = true) }

        if (isCaptain && existingCaptain != null && existingCaptain.studentId != student.studentId) {
            val existingCaptName = allStudentsMap[existingCaptain.studentId]?.fullName ?: "Current Captain"
            MaterialAlertDialogBuilder(this)
                .setTitle("Captain Already Assigned")
                .setMessage("'$existingCaptName' is currently Captain of this team. Replace current Captain?")
                .setPositiveButton("REPLACE") { _, _ ->
                    // Demote existing captain
                    lifecycleScope.launch {
                        val demoted = existingCaptain.copy(role = "Player")
                        repository.addTeamMember(demoted)
                        saveNewMember(student, jersey, position, role, performance, notes, dialog)
                    }
                }
                .setNegativeButton("CANCEL", null)
                .show()
        } else if (isViceCaptain && existingVice != null && existingVice.studentId != student.studentId) {
            val existingViceName = allStudentsMap[existingVice.studentId]?.fullName ?: "Current Vice-Captain"
            MaterialAlertDialogBuilder(this)
                .setTitle("Vice-Captain Already Assigned")
                .setMessage("'$existingViceName' is currently Vice-Captain of this team. Replace current Vice-Captain?")
                .setPositiveButton("REPLACE") { _, _ ->
                    lifecycleScope.launch {
                        val demoted = existingVice.copy(role = "Player")
                        repository.addTeamMember(demoted)
                        saveNewMember(student, jersey, position, role, performance, notes, dialog)
                    }
                }
                .setNegativeButton("CANCEL", null)
                .show()
        } else {
            saveNewMember(student, jersey, position, role, performance, notes, dialog)
        }
    }

    private fun saveNewMember(
        student: Student,
        jersey: String,
        position: String,
        role: String,
        performance: String,
        notes: String,
        dialog: AlertDialog
    ) {
        val newMember = InstitutionTeamMember(
            teamId = teamId,
            studentId = student.studentId,
            institutionId = institutionId,
            jerseyNumber = jersey,
            position = position,
            role = role,
            performanceStatus = performance,
            notes = notes
        )

        lifecycleScope.launch {
            try {
                repository.addTeamMember(newMember)

                // Update team leadership ids if needed
                currentTeam?.let { team ->
                    val updatedTeam = when (role.lowercase()) {
                        "captain" -> team.copy(captainId = student.studentId)
                        "vice-captain", "vice captain" -> team.copy(viceCaptainId = student.studentId)
                        else -> team
                    }
                    if (updatedTeam != team) {
                        repository.addTeam(updatedTeam)
                        currentTeam = updatedTeam
                    }
                }

                Snackbar.make(findViewById(R.id.rv_team_roster), "✔ ${student.fullName} added to team", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this@InstitutionTeamDetailActivity, "Error adding player: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Edit Member Dialog ---
    private fun showEditMemberDialog(uiModel: TeamMemberUiModel) {
        val member = uiModel.member
        val student = uiModel.student

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_team_member, null)
        val tvSubTitle = dialogView.findViewById<TextView>(R.id.tv_edit_member_subtitle)
        val etJersey = dialogView.findViewById<EditText>(R.id.et_edit_jersey)
        val spPosition = dialogView.findViewById<Spinner>(R.id.sp_edit_position)
        val spRole = dialogView.findViewById<Spinner>(R.id.sp_edit_role)
        val spPerf = dialogView.findViewById<Spinner>(R.id.sp_edit_performance)
        val etNotes = dialogView.findViewById<EditText>(R.id.et_edit_player_notes)

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_edit_player)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_edit_player)

        tvSubTitle.text = "${student?.fullName ?: "Student #${member.studentId}"} (ID: ${student?.rollNumber ?: member.studentId})"
        etJersey.setText(member.jerseyNumber)
        etNotes.setText(member.notes)

        // Populate Positions
        val sportName = currentTeam?.sport ?: "Cricket"
        val positionOptions = SportsPositionHelper.getPositionsForSport(sportName)
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positionOptions)
        spPosition.adapter = posAdapter
        if (positionOptions.contains(member.position)) {
            spPosition.setSelection(positionOptions.indexOf(member.position))
        }

        // Roles & Performance
        val roleOptions = listOf("Player", "Captain", "Vice-Captain")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roleOptions)
        spRole.adapter = roleAdapter
        val currentRole = when {
            member.role.equals("Captain", ignoreCase = true) -> "Captain"
            member.role.contains("Vice", ignoreCase = true) -> "Vice-Captain"
            else -> "Player"
        }
        spRole.setSelection(roleOptions.indexOf(currentRole))

        val perfOptions = listOf("Good", "Excellent", "Average")
        val perfAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, perfOptions)
        spPerf.adapter = perfAdapter
        if (perfOptions.contains(member.performanceStatus)) {
            spPerf.setSelection(perfOptions.indexOf(member.performanceStatus))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val jersey = etJersey.text.toString().trim()
            val positionStr = spPosition.selectedItem?.toString() ?: "Player"
            val roleStr = spRole.selectedItem?.toString() ?: "Player"
            val perfStr = spPerf.selectedItem?.toString() ?: "Good"
            val notesStr = etNotes.text.toString().trim()

            // Validate duplicate jersey number (excluding self)
            if (jersey.isNotEmpty() && allRosterMembers.any { it.jerseyNumber == jersey && it.memberId != member.memberId }) {
                Toast.makeText(this, "Jersey number #$jersey is already assigned to another team member!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val updatedMember = member.copy(
                jerseyNumber = jersey,
                position = positionStr,
                role = roleStr,
                performanceStatus = perfStr,
                notes = notesStr
            )

            lifecycleScope.launch {
                try {
                    repository.addTeamMember(updatedMember)

                    // Update team leadership refs
                    currentTeam?.let { team ->
                        var newCaptId = team.captainId
                        var newViceId = team.viceCaptainId

                        if (roleStr.equals("Captain", ignoreCase = true)) {
                            newCaptId = member.studentId
                        } else if (team.captainId == member.studentId) {
                            newCaptId = null
                        }

                        if (roleStr.contains("Vice", ignoreCase = true)) {
                            newViceId = member.studentId
                        } else if (team.viceCaptainId == member.studentId) {
                            newViceId = null
                        }

                        val updatedTeam = team.copy(captainId = newCaptId, viceCaptainId = newViceId)
                        if (updatedTeam != team) {
                            repository.addTeam(updatedTeam)
                            currentTeam = updatedTeam
                        }
                    }

                    Snackbar.make(findViewById(R.id.rv_team_roster), "✔ Player Roster Updated", Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@InstitutionTeamDetailActivity, "Error updating player: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // --- Remove Member Dialog ---
    private fun confirmRemoveMember(uiModel: TeamMemberUiModel) {
        val member = uiModel.member
        val studentName = uiModel.student?.fullName ?: "Student #${member.studentId}"
        val isLeadership = member.role.equals("Captain", ignoreCase = true) || member.role.contains("Vice", ignoreCase = true)

        val message = if (isLeadership) {
            "'$studentName' is currently ${member.role.uppercase()}.\n\nRemove from this team and release ${member.role} status?\n(Note: The student profile will NOT be deleted from Student Directory)."
        } else {
            "Remove '$studentName' from '${currentTeam?.teamName ?: "team"}'?\n(Note: The student profile will NOT be deleted from Student Directory)."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Player")
            .setMessage(message)
            .setPositiveButton("REMOVE") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteTeamMember(member)

                        // Clear leadership if needed
                        currentTeam?.let { team ->
                            var newCaptId = team.captainId
                            var newViceId = team.viceCaptainId

                            if (team.captainId == member.studentId) newCaptId = null
                            if (team.viceCaptainId == member.studentId) newViceId = null

                            val updatedTeam = team.copy(captainId = newCaptId, viceCaptainId = newViceId)
                            if (updatedTeam != team) {
                                repository.addTeam(updatedTeam)
                                currentTeam = updatedTeam
                            }
                        }

                        Snackbar.make(findViewById(R.id.rv_team_roster), "Player removed from team", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionTeamDetailActivity, "Error removing player: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // --- Leadership Assignment Dialog ---
    private fun showLeadershipDialog(isCaptain: Boolean) {
        if (allRosterMembers.isEmpty()) {
            Toast.makeText(this, "Add players to team roster first", Toast.LENGTH_SHORT).show()
            return
        }

        val roleTitle = if (isCaptain) "Captain" else "Vice-Captain"
        val candidates = allRosterMembers.mapNotNull { allStudentsMap[it.studentId] }
        val candidateNames = candidates.map { it.fullName }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Assign $roleTitle")
            .setItems(candidateNames) { _, which ->
                val selectedStudent = candidates[which]
                val selectedMember = allRosterMembers.find { it.studentId == selectedStudent.studentId }
                if (selectedMember != null) {
                    handleRoleAndAddPlayer(
                        student = selectedStudent,
                        jersey = selectedMember.jerseyNumber,
                        position = selectedMember.position,
                        role = roleTitle,
                        performance = selectedMember.performanceStatus,
                        notes = selectedMember.notes,
                        dialog = AlertDialog.Builder(this).create() // Dummy dialog for reuse
                    )
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // --- Player Profile Dialog ---
    private fun showPlayerProfileDialog(uiModel: TeamMemberUiModel) {
        val student = uiModel.student ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_player_profile, null)

        dialogView.findViewById<TextView>(R.id.tv_profile_name).text = student.fullName
        dialogView.findViewById<TextView>(R.id.tv_profile_id).text = "Student ID: ${student.studentId} | Roll: ${student.rollNumber}"
        dialogView.findViewById<TextView>(R.id.tv_profile_academic).text = "Grade: ${student.grade}-${student.section} | Gender: ${student.gender} | Age: ${student.age}"
        dialogView.findViewById<TextView>(R.id.tv_profile_sport_primary).text = "Primary Sport: ${student.selectedSport} | Game: ${student.selectedGame}"
        dialogView.findViewById<TextView>(R.id.tv_profile_physical).text = "Height: ${student.heightCm} cm | Weight: ${student.weightKg} kg | BMI: ${student.bmi}"
        dialogView.findViewById<TextView>(R.id.tv_profile_health).text = "Health Status: ${student.healthStatus} | Blood Group: ${student.bloodGroup}"
        dialogView.findViewById<TextView>(R.id.tv_profile_scores).text = "AI Fitness: ${student.aiFitnessScore}/100 | Technique: ${student.aiTechniqueScore}/100 | Attendance: ${student.attendancePercentage}%"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_close_player_profile).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // --- Edit & Delete Team Actions ---
    private fun showEditTeamDialog() {
        val team = currentTeam ?: return
        // Trigger team edit
        lifecycleScope.launch {
            repository.addTeam(team)
            Toast.makeText(this@InstitutionTeamDetailActivity, "Team updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteTeam() {
        val team = currentTeam ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Team")
            .setMessage("Are you sure you want to delete '${team.teamName}'?\n\nDeleting the team will remove team roster membership but will NOT delete student profiles.")
            .setPositiveButton("DELETE") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteTeam(team.teamId)
                    Toast.makeText(this@InstitutionTeamDetailActivity, "Team Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}
