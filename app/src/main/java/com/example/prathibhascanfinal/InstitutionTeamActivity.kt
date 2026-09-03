package com.example.prathibhascanfinal

import android.content.Intent
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
import com.example.prathibhascanfinal.ui.adapter.InstitutionTeamListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class InstitutionTeamActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: InstitutionRepository

    private lateinit var adapter: InstitutionTeamListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var institutionId: Int = 0
    private var allTeams = listOf<InstitutionTeam>()
    private var configuredSports = listOf<InstitutionSport>()
    private var institutionTeachers = listOf<InstitutionTeacher>()
    private var totalRosterPlayerCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_team)
        setupEdgeToEdge(findViewById(R.id.inst_team_root))

        repository = InstitutionRepository(this)

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionTeamActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                repository.startSync(institutionId)
            }
            initUI()
            observeData()
        }
    }

    private fun initUI() {
        findViewById<View>(R.id.btn_back_teams)?.setOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeData() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_inst_teams)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InstitutionTeamListAdapter(
            onView = { openTeamDetails(it) },
            onEdit = { showTeamDialog(it) },
            onDelete = { confirmDeleteTeam(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_create_team)?.setOnClickListener { showTeamDialog(null) }
        findViewById<View>(R.id.btn_top_create_team)?.setOnClickListener { showTeamDialog(null) }
        findViewById<View>(R.id.btn_empty_create_team)?.setOnClickListener { showTeamDialog(null) }

        findViewById<EditText>(R.id.et_search_team)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTeams(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeData() {
        if (institutionId <= 0) {
            swipeRefresh.isRefreshing = false
            return
        }
        swipeRefresh.isRefreshing = true

        val db = AppDatabase.getDatabase(this@InstitutionTeamActivity).institutionManagementDao()

        // 1. Observe Teams
        lifecycleScope.launch {
            db.getTeamsFlow(institutionId).collectLatest { list ->
                allTeams = list
                updateList(list)
                updateStatsHeader()
                swipeRefresh.isRefreshing = false
            }
        }

        // 2. Observe Configured Sports
        lifecycleScope.launch {
            repository.getInstitutionSports(institutionId).collectLatest { sports ->
                configuredSports = sports
                updateStatsHeader()
            }
        }

        // 3. Observe Teachers
        lifecycleScope.launch {
            repository.getTeachersFlow(institutionId).collectLatest { teachers ->
                institutionTeachers = teachers
            }
        }

        // 4. Observe Total Roster Player Count
        lifecycleScope.launch {
            db.getAllTeamMembersFlowForInstitution(institutionId).collectLatest { members ->
                totalRosterPlayerCount = members.size
                updateStatsHeader()
            }
        }
    }

    private fun updateStatsHeader() {
        findViewById<TextView>(R.id.tv_stat_total_teams)?.text = allTeams.size.toString()
        val distinctSports = allTeams.map { it.sport }.filter { it.isNotEmpty() }.toSet()
        findViewById<TextView>(R.id.tv_stat_active_sports)?.text = distinctSports.size.toString()
        findViewById<TextView>(R.id.tv_stat_total_players)?.text = totalRosterPlayerCount.toString()
    }

    private fun updateList(list: List<InstitutionTeam>) {
        adapter.submitList(list)
        findViewById<View>(R.id.layout_empty_teams)?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterTeams(query: String) {
        val filtered = if (query.trim().isEmpty()) {
            allTeams
        } else {
            allTeams.filter {
                it.teamName.contains(query, ignoreCase = true) ||
                it.sport.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
        updateList(filtered)
    }

    private fun showTeamDialog(existingTeam: InstitutionTeam?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_team, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_team_title)
        val etName = dialogView.findViewById<EditText>(R.id.et_team_name)
        val spSport = dialogView.findViewById<Spinner>(R.id.sp_team_sport)
        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_team_category)
        val spGender = dialogView.findViewById<Spinner>(R.id.sp_team_gender)
        val spType = dialogView.findViewById<Spinner>(R.id.sp_team_type)
        val spCoach = dialogView.findViewById<Spinner>(R.id.sp_team_coach)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_team_description)

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_create_team)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_create_team)

        if (existingTeam != null) {
            tvTitle.text = "Edit Team Information"
            etName.setText(existingTeam.teamName)
            etDesc.setText(existingTeam.description ?: "")
            btnConfirm.text = "UPDATE TEAM"
        } else {
            tvTitle.text = "Create Institution Team"
            btnConfirm.text = "CREATE TEAM"
        }

        // 1. Populate Sports Dropdown
        val sportOptions = if (configuredSports.isNotEmpty()) {
            configuredSports.map { it.sportName }.distinct()
        } else {
            listOf("Cricket", "Football", "Basketball", "Volleyball", "Hockey", "Athletics", "Badminton", "Kabaddi", "Tennis")
        }
        val sportAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sportOptions)
        spSport.adapter = sportAdapter

        if (existingTeam != null && sportOptions.contains(existingTeam.sport)) {
            spSport.setSelection(sportOptions.indexOf(existingTeam.sport))
        }

        // 2. Populate Categories Dropdown
        val categoryOptions = SportsPositionHelper.getAgeGroups()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)
        spCategory.adapter = categoryAdapter

        if (existingTeam != null && categoryOptions.contains(existingTeam.category)) {
            spCategory.setSelection(categoryOptions.indexOf(existingTeam.category))
        }

        // 3. Populate Gender Dropdown
        val genderOptions = SportsPositionHelper.getGenders()
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)
        spGender.adapter = genderAdapter

        if (existingTeam != null && genderOptions.contains(existingTeam.gender)) {
            spGender.setSelection(genderOptions.indexOf(existingTeam.gender))
        }

        // 4. Populate Team Type Dropdown
        val typeOptions = SportsPositionHelper.getTeamTypes()
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spType.adapter = typeAdapter

        if (existingTeam != null && typeOptions.contains(existingTeam.teamType)) {
            spType.setSelection(typeOptions.indexOf(existingTeam.teamType))
        }

        // 5. Populate Coach Dropdown
        val coachNames = mutableListOf("Unassigned")
        coachNames.addAll(institutionTeachers.map { it.fullName })
        val coachAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, coachNames)
        spCoach.adapter = coachAdapter

        if (existingTeam != null && existingTeam.coachName != null && coachNames.contains(existingTeam.coachName)) {
            spCoach.setSelection(coachNames.indexOf(existingTeam.coachName))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val selectedSport = spSport.selectedItem?.toString() ?: ""
            val selectedCategory = spCategory.selectedItem?.toString() ?: ""
            val selectedGender = spGender.selectedItem?.toString() ?: "Male"
            val selectedType = spType.selectedItem?.toString() ?: "School Team"
            val selectedCoach = spCoach.selectedItem?.toString()
            val coachName = if (selectedCoach != "Unassigned") selectedCoach else null

            val coachId = if (coachName != null) {
                institutionTeachers.find { it.fullName == coachName }?.teacherId
            } else null

            val desc = etDesc.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Team Name is required"
                etName.requestFocus()
                return@setOnClickListener
            }

            if (selectedSport.isEmpty()) {
                Toast.makeText(this, "Please select a valid sport", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedTeam = (existingTeam ?: InstitutionTeam(institutionId = institutionId)).copy(
                teamName = name,
                sport = selectedSport,
                category = selectedCategory,
                ageGroup = selectedCategory,
                gender = selectedGender,
                teamType = selectedType,
                coachId = coachId ?: existingTeam?.coachId,
                coachName = coachName ?: existingTeam?.coachName,
                teacherInCharge = coachName ?: existingTeam?.teacherInCharge,
                description = desc.ifEmpty { null },
                updatedAt = System.currentTimeMillis()
            )

            // If changing sport when team already exists, warn user
            if (existingTeam != null && existingTeam.sport != selectedSport) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Change Team Sport?")
                    .setMessage("Changing the sport from '${existingTeam.sport}' to '$selectedSport' may invalidate positions of existing roster members. Proceed?")
                    .setPositiveButton("Proceed") { _, _ ->
                        saveTeamAndDismiss(updatedTeam, dialog)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                saveTeamAndDismiss(updatedTeam, dialog)
            }
        }
        dialog.show()
    }

    private fun saveTeamAndDismiss(team: InstitutionTeam, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                repository.addTeam(team)
                Snackbar.make(findViewById(R.id.rv_inst_teams), "✔ Team Saved Successfully", Snackbar.LENGTH_LONG).show()
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this@InstitutionTeamActivity, "Error saving team: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteTeam(team: InstitutionTeam) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Team")
            .setMessage("Are you sure you want to delete '${team.teamName}' (${team.sport})?\n\nWarning: Deleting the team will remove the team roster association but will NOT delete student profiles from the Student Directory.")
            .setPositiveButton("DELETE TEAM") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteTeam(team.teamId)
                        Snackbar.make(findViewById(R.id.rv_inst_teams), "Team removed successfully", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionTeamActivity, "Error deleting team: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun openTeamDetails(team: InstitutionTeam) {
        val intent = Intent(this, InstitutionTeamDetailActivity::class.java).apply {
            putExtra("TEAM_ID", team.teamId)
            putExtra("INSTITUTION_ID", institutionId)
        }
        startActivity(intent)
    }
}
