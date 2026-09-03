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
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.ui.adapter.TeamListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademyTeamActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository

    private lateinit var adapter: TeamListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var academyId: Int = 0
    private var allTeams = listOf<Team>()
    private var academyCoaches = listOf<Coach>()
    private var totalRosterAthletesCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_team)
        setupEdgeToEdge(findViewById(R.id.team_root))

        repository = AcademyRepository(this)
        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""

        lifecycleScope.launch {
            if (academyId == 0) {
                val academy = AppDatabase.getDatabase(this@AcademyTeamActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                }
            }
            if (academyId > 0) {
                repository.startSync(academyId)
            }
            initUI()
            observeData()
        }
    }

    private fun initUI() {
        findViewById<View>(R.id.btn_back_academy_teams)?.setOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeData() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_teams)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = TeamListAdapter(
            onView = { showTeamDetails(it) },
            onEdit = { showEditTeamDialog(it) },
            onDelete = { confirmDeleteTeam(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_create_team)?.setOnClickListener { showCreateTeamDialog() }
        findViewById<View>(R.id.btn_top_create_team)?.setOnClickListener { showCreateTeamDialog() }
        findViewById<View>(R.id.btn_empty_create_team)?.setOnClickListener { showCreateTeamDialog() }

        findViewById<EditText>(R.id.et_search_team)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTeams(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeData() {
        if (academyId <= 0) {
            swipeRefresh.isRefreshing = false
            return
        }
        swipeRefresh.isRefreshing = true

        val db = AppDatabase.getDatabase(this@AcademyTeamActivity).academyManagementDao()

        // 1. Observe Teams Flow
        lifecycleScope.launch {
            db.getTeamsForAcademyFlow(academyId).collectLatest { list ->
                allTeams = list
                updateList(list)
                updateStatsHeader()
                swipeRefresh.isRefreshing = false
            }
        }

        // 2. Observe Coaches Flow
        lifecycleScope.launch {
            db.getCoachesFlow(academyId).collectLatest { coaches ->
                academyCoaches = coaches
            }
        }

        // 3. Observe All Athletes Flow to count Roster Members
        lifecycleScope.launch {
            db.getAthletesFlow(academyId).collectLatest { athletes ->
                totalRosterAthletesCount = athletes.count { it.teamId != null && it.teamId!! > 0 }
                updateStatsHeader()
            }
        }
    }

    private fun updateStatsHeader() {
        findViewById<TextView>(R.id.tv_academy_stat_total_teams)?.text = allTeams.size.toString()
        val distinctSports = allTeams.map { it.sport }.filter { it.isNotEmpty() }.toSet()
        findViewById<TextView>(R.id.tv_academy_stat_active_sports)?.text = distinctSports.size.toString()
        findViewById<TextView>(R.id.tv_academy_stat_roster_athletes)?.text = totalRosterAthletesCount.toString()
    }

    private fun updateList(list: List<Team>) {
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

    private fun showCreateTeamDialog() {
        showTeamDialog(null)
    }

    private fun showEditTeamDialog(team: Team) {
        showTeamDialog(team)
    }

    private fun showTeamDialog(existingTeam: Team?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_team, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_team_title)
        val etName = dialogView.findViewById<EditText>(R.id.et_team_name)
        val spSport = dialogView.findViewById<Spinner>(R.id.sp_team_sport)
        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_team_category)
        val spGender = dialogView.findViewById<Spinner>(R.id.sp_team_gender)
        val spType = dialogView.findViewById<Spinner>(R.id.sp_team_type)
        val spCoach = dialogView.findViewById<Spinner>(R.id.sp_team_coach)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_team_description)

        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_create_team)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_create_team)

        if (existingTeam != null) {
            tvTitle?.text = "Edit Academy Team"
            etName?.setText(existingTeam.teamName)
            etDesc?.setText(existingTeam.description ?: "")
            btnConfirm?.text = "UPDATE TEAM"
        } else {
            tvTitle?.text = "Create Academy Team"
            btnConfirm?.text = "CREATE TEAM"
        }

        // 1. Sport Options
        val sportOptions = listOf("Cricket", "Football", "Basketball", "Volleyball", "Hockey", "Athletics", "Badminton", "Kabaddi", "Tennis")
        val sportAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sportOptions)
        spSport?.adapter = sportAdapter

        if (existingTeam != null && sportOptions.contains(existingTeam.sport)) {
            spSport?.setSelection(sportOptions.indexOf(existingTeam.sport))
        }

        // 2. Category / Age Options
        val ageOptions = SportsPositionHelper.getAgeGroups()
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ageOptions)
        spCategory?.adapter = ageAdapter

        if (existingTeam != null && ageOptions.contains(existingTeam.category.ifEmpty { existingTeam.ageGroup })) {
            spCategory?.setSelection(ageOptions.indexOf(existingTeam.category.ifEmpty { existingTeam.ageGroup }))
        }

        // 3. Gender Options
        val genderOptions = SportsPositionHelper.getGenders()
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)
        spGender?.adapter = genderAdapter

        if (existingTeam != null && genderOptions.contains(existingTeam.gender)) {
            spGender?.setSelection(genderOptions.indexOf(existingTeam.gender))
        }

        // 4. Team Type Options
        val typeOptions = SportsPositionHelper.getTeamTypes()
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spType?.adapter = typeAdapter

        // 5. Coach Options
        val coachNames = mutableListOf("Unassigned")
        coachNames.addAll(academyCoaches.map { it.name })
        val coachAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, coachNames)
        spCoach?.adapter = coachAdapter

        if (existingTeam != null && existingTeam.coachName != null && coachNames.contains(existingTeam.coachName)) {
            spCoach?.setSelection(coachNames.indexOf(existingTeam.coachName))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel?.setOnClickListener { dialog.dismiss() }

        btnConfirm?.setOnClickListener {
            val name = etName?.text.toString().trim()
            val sport = spSport?.selectedItem?.toString() ?: "Cricket"
            val ageCategory = spCategory?.selectedItem?.toString() ?: "Under-19"
            val gender = spGender?.selectedItem?.toString() ?: "Mixed"
            val selectedCoach = spCoach?.selectedItem?.toString()
            val coachName = if (selectedCoach != "Unassigned") selectedCoach else null
            val coachId = if (coachName != null) {
                academyCoaches.find { it.name == coachName }?.coachId ?: 0
            } else 0
            val desc = etDesc?.text.toString().trim()

            if (name.isEmpty()) {
                etName?.error = "Team Name is required"
                etName?.requestFocus()
                return@setOnClickListener
            }

            val updatedTeam = (existingTeam ?: Team(academyId = academyId)).copy(
                teamName = name,
                sport = sport,
                ageGroup = ageCategory,
                category = ageCategory,
                gender = gender,
                coachId = coachId,
                coachName = coachName,
                description = desc.ifEmpty { null }
            )

            lifecycleScope.launch {
                try {
                    repository.addTeam(updatedTeam)
                    Snackbar.make(findViewById(R.id.rv_teams), "✔ Team Saved Successfully", Snackbar.LENGTH_LONG).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@AcademyTeamActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteTeam(team: Team) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Team")
            .setMessage("Are you sure you want to delete '${team.teamName}' (${team.sport})?\n\nWarning: Removing this team will unlink team membership from athletes, but will NOT delete athlete profiles from the Academy Directory.")
            .setPositiveButton("DELETE") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteTeam(team)
                        Snackbar.make(findViewById(R.id.rv_teams), "Team Deleted", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyTeamActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showTeamDetails(team: Team) {
        val intent = Intent(this, AcademyTeamDetailActivity::class.java).apply {
            putExtra("TEAM_ID", team.teamId)
            putExtra("ACADEMY_ID", academyId)
        }
        startActivity(intent)
    }
}
