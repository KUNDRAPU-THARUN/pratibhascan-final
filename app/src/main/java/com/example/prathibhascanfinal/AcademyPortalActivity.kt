package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.adapter.AthleteListAdapter
import com.example.prathibhascanfinal.ui.adapter.TournamentListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademyPortalActivity : BaseActivity() {

    override val viewModel: AcademyPortalViewModel by lazy {
        ViewModelProvider(this)[AcademyPortalViewModel::class.java]
    }

    private lateinit var session: SessionManager
    private var currentAcademyId: Int = 0
    private lateinit var recentAdapter: AthleteListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_academy_portal)
        
        val root = findViewById<View>(R.id.academy_portal_root)
        setupEdgeToEdge(root)
        adjustGridSpanCount()
        
        session = SessionManager(this)
        val email = session.getEmail() ?: ""

        setupRecentList()
        setupButtons()
        observeState()
        
        viewModel.loadDashboard(email)
    }

    private fun setupRecentList() {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_recent_registrations)
        rv?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recentAdapter = AthleteListAdapter(
            onView = { /* Open */ },
            onEdit = { /* Edit */ },
            onDelete = { /* Delete */ }
        )
        rv?.adapter = recentAdapter
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val loadingView = findViewById<View>(R.id.loading_overlay)
                if (state.isLoading) {
                    loadingView?.visibility = View.VISIBLE
                } else {
                    loadingView?.visibility = View.GONE
                    
                    if (state.academy == null) {
                        if (viewModel.uiState.value.academy == null) {
                            Toast.makeText(this@AcademyPortalActivity, "Registration required.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AcademyPortalActivity, AcademyRegistrationActivity::class.java))
                            finish()
                        }
                        return@collectLatest
                    }

                    // Update Header Time/Date
                    findViewById<TextView>(R.id.tv_live_time)?.text = state.time
                    
                    state.academy.let { 
                        currentAcademyId = it.id
                        updateAcademyInfo(it) 
                    }
                    updateStats(state)
                    updateSportsOffered(state)
                    recentAdapter.submitList(state.recentAthletes)
                }
            }
        }
    }

    private fun updateAcademyInfo(academy: Academy) {
        findViewById<TextView>(R.id.tv_aca_name_display).text = academy.academyName
        findViewById<TextView>(R.id.tv_aca_type_location).text = getString(R.string.academy_type_location_format, academy.academyType, academy.city, academy.state)
        
        val displayName = academy.directorName.ifEmpty { "Director" }
        findViewById<TextView>(R.id.tv_welcome_name)?.text = getString(R.string.welcome_user, displayName)
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = getString(R.string.district_location_format, academy.city)

        val verTv = findViewById<TextView>(R.id.tv_aca_verification)
        if (academy.isVerified) {
            verTv.text = getString(R.string.verified_academy)
            verTv.setTextColor(getColor(R.color.brand_green))
        } else {
            verTv.text = getString(R.string.pending_verification)
            verTv.setTextColor(getColor(R.color.brand_gold))
        }
    }

    private fun updateStats(state: AcademyDashboardState) {
        updateStatCard(R.id.stat_athletes, state.totalAthletes.toString(), getString(R.string.label_total_athletes))
        updateStatCard(R.id.stat_coaches, state.totalCoaches.toString(), getString(R.string.label_active_coaches))
        updateStatCard(R.id.stat_sports, state.totalSports.toString(), getString(R.string.label_sports_offered))
        updateStatCard(R.id.stat_grounds, state.totalFacilities.toString(), getString(R.string.label_ground_facilities))
        updateStatCard(R.id.stat_revenue, getString(R.string.currency_format, state.totalRevenue.toInt()), getString(R.string.label_est_revenue))
        updateStatCard(R.id.stat_pending, state.pendingRequests.toString(), getString(R.string.label_pending_requests))
        
        findViewById<View>(R.id.stat_pending).setOnClickListener {
            startActivity(Intent(this, AcademyInvitationsActivity::class.java))
        }
    }

    private fun updateStatCard(id: Int, value: String, label: String) {
        val card = findViewById<View>(id)
        card.findViewById<TextView>(R.id.tv_stat_value).text = value
        card.findViewById<TextView>(R.id.tv_stat_label).text = label
    }

    private var lastSportsList: List<AcademySport>? = null
    private fun updateSportsOffered(state: AcademyDashboardState) {
        val sports = state.academySports
        val container = findViewById<LinearLayout>(R.id.layout_sport_summaries) ?: return
        
        if (lastSportsList == sports && container.childCount > 0) return
        lastSportsList = sports

        container.removeAllViews()
        if (sports.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, container, false)
            emptyView.findViewById<TextView>(android.R.id.text1).apply {
                text = "No Sports Added Yet\nTap \"Add New Sport\" to create your first academy sport."
                setTextColor(getColor(R.color.text_tertiary))
                textSize = 14f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 40, 0, 40)
            }
            container.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(this)
        sports.forEach { academySport ->
            val card = inflater.inflate(R.layout.item_academy_sport_card, container, false)
            
            // Find matched summary for counts
            val summary = state.sportSummaries.find { it.sportName.equals(academySport.sportName, ignoreCase = true) }
            
            card.findViewById<TextView>(R.id.tv_sport_name).text = academySport.sportName
            card.findViewById<TextView>(R.id.tv_sport_category).text = academySport.category
            card.findViewById<TextView>(R.id.tv_sport_status).text = academySport.status
            
            card.findViewById<TextView>(R.id.tv_count_athletes).text = (summary?.athleteCount ?: 0).toString()
            card.findViewById<TextView>(R.id.tv_count_coaches).text = (summary?.coachCount ?: 0).toString()

            card.findViewById<Button>(R.id.btn_add_athlete_quick).setOnClickListener {
                val intent = Intent(this, AcademyAthleteRegistrationActivity::class.java)
                intent.putExtra("PRE_SELECT_SPORT", academySport.sportName)
                startActivity(intent)
            }

            card.findViewById<Button>(R.id.btn_add_coach_quick).setOnClickListener {
                val intent = Intent(this, CoachPortfolioActivity::class.java)
                intent.putExtra("PRE_SELECT_SPORT", academySport.sportName)
                startActivity(intent)
            }

            card.findViewById<Button>(R.id.btn_view_details_quick).setOnClickListener {
                val intent = Intent(this, AcademySportDetailsActivity::class.java)
                intent.putExtra("SPORT_NAME", academySport.sportName)
                intent.putExtra("ACADEMY_ID", currentAcademyId)
                startActivity(intent)
            }

            card.findViewById<View>(R.id.btn_edit_sport_quick).setOnClickListener {
                showAddSportDialog(academySport)
            }

            card.findViewById<View>(R.id.btn_delete_sport_quick).setOnClickListener {
                confirmDeleteSport(academySport)
            }

            container.addView(card)
        }
    }

    private fun confirmDeleteSport(sport: AcademySport) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Sport")
            .setMessage("Are you sure you want to remove ${sport.sportName}? This will not delete the associated athletes.")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@AcademyPortalActivity).academyManagementDao().deleteAcademySport(sport)
                    Toast.makeText(this@AcademyPortalActivity, "${sport.sportName} removed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupButtons() {
        // Top Shortcut Buttons
        findViewById<View>(R.id.btn_aca_ops_portal)?.setOnClickListener {
            // Navigation to ops if needed
        }
        findViewById<View>(R.id.btn_aca_recruit_talent)?.setOnClickListener {
            startActivity(Intent(this, AthleteDiscoveryActivity::class.java))
        }
        findViewById<View>(R.id.btn_aca_view_profile)?.setOnClickListener {
            startActivity(Intent(this, AcademyProfileActivity::class.java))
        }

        // Academy Profile Edit
        findViewById<View>(R.id.btn_edit_aca_profile).setOnClickListener {
            startActivity(Intent(this, AcademyProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_add_new_sport)?.setOnClickListener {
            showAddSportDialog()
        }

        // Management Portal List
        setupListButton(
            R.id.btn_talent_discovery, 
            getString(R.string.title_talent_discovery), 
            getString(R.string.desc_talent_discovery), 
            R.drawable.ic_radar_emblem, 
            R.color.brand_gold
        ) {
            startActivity(Intent(this, AthleteDiscoveryActivity::class.java))
        }

        setupListButton(
            R.id.btn_athlete_mgmt, 
            getString(R.string.title_athlete_mgmt), 
            getString(R.string.desc_athlete_mgmt), 
            R.drawable.ic_person, 
            R.color.brand_blue
        ) {
            val intent = Intent(this, AcademyAthleteDirectoryActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_coach_mgmt, 
            getString(R.string.title_coach_mgmt), 
            getString(R.string.desc_coach_mgmt), 
            R.drawable.ic_coach_voice, 
            R.color.brand_gold
        ) {
            val intent = Intent(this, AcademyCoachDirectoryActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_team_mgmt, 
            getString(R.string.title_team_mgmt), 
            getString(R.string.desc_team_mgmt), 
            R.drawable.ic_groups_team, 
            R.color.brand_green
        ) {
            val intent = Intent(this, AcademyTeamActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_booking_mgmt, 
            getString(R.string.title_ground_facilities), 
            getString(R.string.desc_ground_facilities), 
            R.drawable.ic_stadium_facility, 
            R.color.brand_brown
        ) {
            val intent = Intent(this, AcademyFacilityActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_inventory_mgmt, 
            getString(R.string.title_equipment_inventory), 
            getString(R.string.desc_equipment_inventory), 
            R.drawable.ic_inventory_box, 
            R.color.brand_purple
        ) {
            val intent = Intent(this, AcademyInventoryActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_attendance_mgmt, 
            getString(R.string.title_attendance_system), 
            getString(R.string.desc_attendance_system), 
            R.drawable.ic_fact_check_attendance, 
            R.color.brand_emerald
        ) {
            val intent = Intent(this, AcademyAttendanceActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_tournament_mgmt, 
            getString(R.string.title_tournament_management), 
            getString(R.string.desc_tournament_management), 
            R.drawable.ic_tournament_trophy, 
            R.color.brand_orange
        ) {
            val intent = Intent(this, AcademyTournamentActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_ai_dashboard, 
            getString(R.string.title_performance_dashboard), 
            getString(R.string.desc_performance_dashboard), 
            R.drawable.ic_analytics_chart, 
            R.color.brand_cyan
        ) {
            val intent = Intent(this, AcademyPerformanceActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_video_analysis, 
            getString(R.string.title_ai_video_analysis), 
            getString(R.string.desc_ai_video_analysis), 
            R.drawable.ic_ai_robot, 
            R.color.brand_indigo
        ) {
            val intent = Intent(this, AcademyVideoAnalysisActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_medical_tracking, 
            getString(R.string.title_medical_injury_tracking), 
            getString(R.string.desc_medical_injury_tracking), 
            R.drawable.ic_medical_health, 
            R.color.brand_red
        ) {
            val intent = Intent(this, AcademyMedicalActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_nutrition_diet, 
            getString(R.string.title_nutrition_diet_plans), 
            getString(R.string.desc_nutrition_diet_plans), 
            R.drawable.ic_restaurant_diet, 
            R.color.brand_lime
        ) {
            val intent = Intent(this, AcademyNutritionActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_training_calendar, 
            getString(R.string.title_training_calendar), 
            getString(R.string.desc_training_calendar), 
            R.drawable.ic_calendar_schedule, 
            R.color.brand_sky
        ) {
            val intent = Intent(this, AcademyTrainingCalendarActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_payroll_mgmt, 
            getString(R.string.title_coach_salary_payroll), 
            getString(R.string.desc_coach_salary_payroll), 
            R.drawable.ic_wallet_payroll, 
            R.color.brand_amber
        ) {
            val intent = Intent(this, AcademyPayrollActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_record_transfer, 
            getString(R.string.title_student_record_transfer), 
            getString(R.string.desc_student_record_transfer), 
            R.drawable.ic_file_transfer, 
            R.color.brand_violet
        ) {
            val intent = Intent(this, AcademyTransferActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }

        setupListButton(
            R.id.btn_report_gen, 
            getString(R.string.title_reports), 
            getString(R.string.desc_reports), 
            R.drawable.ic_report_summary, 
            R.color.brand_teal
        ) {
            val intent = Intent(this, AcademyReportsActivity::class.java)
            intent.putExtra("ACADEMY_ID", currentAcademyId)
            startActivity(intent)
        }
    }

    private fun setupListButton(id: Int, title: String, subtitle: String, iconRes: Int, colorRes: Int, onClick: () -> Unit) {
        val view = findViewById<View>(id)
        view.findViewById<TextView>(R.id.tv_btn_label).text = title
        view.findViewById<TextView>(R.id.tv_btn_subtitle).text = subtitle
        val iv = view.findViewById<ImageView>(R.id.iv_btn_icon)
        iv.setImageResource(iconRes)
        iv.setColorFilter(getColor(colorRes))
        view.setOnClickListener { onClick() }
    }

    private fun showAddSportDialog(existingSport: AcademySport? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sport, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Setup Spinners
        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_sport_category)
        val categories = arrayOf("Indoor", "Outdoor", "Athletics", "Other")
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)

        val spGender = dialogView.findViewById<Spinner>(R.id.sp_sport_gender)
        val genders = arrayOf("Mixed", "Boys", "Girls")
        spGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)

        val etName = dialogView.findViewById<EditText>(R.id.et_sport_name_entry)
        val etLevel = dialogView.findViewById<EditText>(R.id.et_sport_level)
        val etAge = dialogView.findViewById<EditText>(R.id.et_sport_age_groups)
        val etCap = dialogView.findViewById<EditText>(R.id.et_sport_capacity)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_sport)

        existingSport?.let { sport ->
            etName.setText(sport.sportName)
            etLevel.setText(sport.trainingLevel)
            etAge.setText(sport.ageGroups)
            etCap.setText(sport.maxCapacity.toString())
            spCategory.setSelection(categories.indexOf(sport.category).coerceAtLeast(0))
            spGender.setSelection(genders.indexOf(sport.genderType).coerceAtLeast(0))
            btnConfirm.text = "Update Sport"
        }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val level = etLevel.text.toString().trim()
            val age = etAge.text.toString().trim()
            val cap = etCap.text.toString().toIntOrNull() ?: 50

            if (name.isNotEmpty()) {
                val newSport = (existingSport ?: AcademySport(academyId = currentAcademyId)).copy(
                    sportName = name,
                    category = spCategory.selectedItem.toString(),
                    trainingLevel = level,
                    ageGroups = age,
                    genderType = spGender.selectedItem.toString(),
                    maxCapacity = cap,
                    updatedAt = System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@AcademyPortalActivity).academyManagementDao().insertAcademySport(newSport)
                    dialog.dismiss()
                    Toast.makeText(this@AcademyPortalActivity, "$name updated", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Sport Name is required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun adjustGridSpanCount() {
        val displayMetrics = resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        
        val statsGrid = findViewById<GridLayout>(R.id.grid_stats)
        val portalGrid = findViewById<GridLayout>(R.id.grid_portal)

        if (widthDp >= 600) { // Tablet or Landscape
            statsGrid?.columnCount = 3
            portalGrid?.columnCount = 2
        } else { // Phone
            statsGrid?.columnCount = 2
            portalGrid?.columnCount = 1
        }
    }
}
