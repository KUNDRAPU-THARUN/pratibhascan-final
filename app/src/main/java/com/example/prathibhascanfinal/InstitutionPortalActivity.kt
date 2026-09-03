package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstitutionPortalActivity : BaseActivity() {

    override val viewModel: InstitutionPortalViewModel by lazy {
        ViewModelProvider(this)[InstitutionPortalViewModel::class.java]
    }

    private lateinit var session: SessionManager
    private var currentInstitutionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_institution_portal)
        
        session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        // Dynamic ID from Intent or Session
        currentInstitutionId = intent.getIntExtra("INSTITUTION_ID", 0)

        val root = findViewById<View>(R.id.inst_portal_root)
        setupEdgeToEdge(root)
        adjustGridSpanCount()
        
        setupButtons()
        setupSearch()
        observeState()
        
        viewModel.loadDashboard(email)
    }

    private fun setupSearch() {
        val searchEt = findViewById<EditText>(R.id.et_global_search)
        searchEt.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterManagementPortal(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterManagementPortal(query: String) {
        val grid = findViewById<GridLayout>(R.id.grid_portal)
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            val title = child.findViewById<TextView>(R.id.tv_btn_label)?.text.toString()
            if (title.contains(query, ignoreCase = true)) {
                child.visibility = View.VISIBLE
            } else {
                child.visibility = View.GONE
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLoading) {
                    findViewById<View>(R.id.loading_overlay).visibility = View.VISIBLE
                } else {
                    findViewById<View>(R.id.loading_overlay).visibility = View.GONE
                    
                    if (state.institution == null) {
                        // Double check with session flag before redirecting
                        if (!session.isRegistrationComplete()) {
                            if (viewModel.uiState.value.institution == null) {
                                Toast.makeText(this@InstitutionPortalActivity, "Institution registration required.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@InstitutionPortalActivity, InstitutionRegistrationActivity::class.java))
                                finish()
                            }
                        }
                        return@collectLatest
                    }

                    // Update Header Time/Date
                    findViewById<TextView>(R.id.tv_live_time)?.text = state.time
                    
                    state.institution.let { 
                        currentInstitutionId = it.id
                        updateInstitutionInfo(it) 
                    }
                    updateStats(state)
                    updateSportsOffered(state)
                }
            }
        }
    }

    private fun updateInstitutionInfo(inst: Institution) {
        findViewById<TextView>(R.id.tv_inst_name_display).text = inst.institutionName
        findViewById<TextView>(R.id.tv_inst_type_location).text = getString(R.string.academy_type_location_format, inst.boardAffiliation, inst.city, inst.state)
        findViewById<TextView>(R.id.tv_inst_id).text = getString(R.string.label_inst_id_format, inst.id)
        
        val displayName = inst.principalName.ifEmpty { "Principal" }
        findViewById<TextView>(R.id.tv_welcome_name)?.text = getString(R.string.welcome_user, displayName)
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = getString(R.string.institution_portal_subtitle)

        val verTv = findViewById<TextView>(R.id.tv_inst_verification)
        if (inst.isVerified) {
            verTv.text = getString(R.string.verified_institution)
            verTv.setTextColor(getColor(R.color.brand_green))
        } else {
            verTv.text = getString(R.string.pending_verification)
            verTv.setTextColor(getColor(R.color.brand_gold))
        }
    }

    private fun updateStats(state: InstitutionDashboardState) {
        updateStatCard(R.id.stat_total_students, state.totalStudents.toString(), getString(R.string.label_total_students))
        updateStatCard(R.id.stat_sports_students, state.sportsStudents.toString(), getString(R.string.label_sports_students))
        updateStatCard(R.id.stat_pe_teachers, state.peTeachers.toString(), getString(R.string.label_pe_teachers))
        updateStatCard(R.id.stat_sports_teams, state.totalTeams.toString(), getString(R.string.label_sports_teams))
        updateStatCard(R.id.stat_sports_offered, state.sportsOfferedCount.toString(), getString(R.string.label_sports_offered))
        updateStatCard(R.id.stat_grounds, state.totalGrounds.toString(), getString(R.string.label_grounds))
        updateStatCard(R.id.stat_equipment, state.totalEquipment.toString(), getString(R.string.label_equipment))
        updateStatCard(R.id.stat_attendance, state.todayAttendance, getString(R.string.label_attendance_stat))
        updateStatCard(R.id.stat_upcoming_events, state.upcomingEvents.toString(), getString(R.string.label_upcoming_events))
        updateStatCard(R.id.stat_tournaments, state.activeTournaments.toString(), getString(R.string.label_active_tournaments))
        updateStatCard(R.id.stat_pending_approvals, state.pendingApprovals.toString(), getString(R.string.label_pending_approvals))
        updateStatCard(R.id.stat_medical_cases, state.medicalCases.toString(), getString(R.string.label_medical_cases))
    }

    private fun updateStatCard(id: Int, value: String, label: String) {
        val card = findViewById<View>(id)
        card.findViewById<TextView>(R.id.tv_stat_value).text = value
        card.findViewById<TextView>(R.id.tv_stat_label).text = label
    }

    private var lastSportsList: List<InstitutionSport>? = null
    private fun updateSportsOffered(state: InstitutionDashboardState) {
        val sports = state.institutionSports
        val container = findViewById<LinearLayout>(R.id.layout_sport_summaries) ?: return
        
        if (lastSportsList == sports && container.childCount > 0) return
        lastSportsList = sports

        container.removeAllViews()
        if (sports.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, container, false)
            emptyView.findViewById<TextView>(android.R.id.text1).apply {
                text = "No Sports Added Yet\nTap \"Manage Sports\" to configure institution sports."
                setTextColor(getColor(R.color.text_tertiary))
                textSize = 14f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 40, 0, 40)
            }
            container.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(this)
        sports.forEach { instSport ->
            val card = inflater.inflate(R.layout.item_institution_sport_card, container, false)
            
            val summary = state.sportSummaries.find { it.sportName.equals(instSport.sportName, ignoreCase = true) }
            
            card.findViewById<TextView>(R.id.tv_sport_name).text = instSport.sportName
            card.findViewById<TextView>(R.id.tv_sport_category).text = instSport.category
            card.findViewById<TextView>(R.id.tv_sport_status).text = instSport.status
            
            card.findViewById<TextView>(R.id.tv_count_students).text = (summary?.studentCount ?: 0).toString()
            card.findViewById<TextView>(R.id.tv_count_teachers).text = (summary?.teacherCount ?: 0).toString()
            card.findViewById<TextView>(R.id.tv_count_teams).text = (summary?.teamCount ?: 0).toString()

            card.findViewById<Button>(R.id.btn_ground_status).text = if(instSport.groundAvailable) "Ground Ready" else "No Ground"
            card.findViewById<Button>(R.id.btn_equipment_status).text = if(instSport.equipmentReady) "Equipment Ready" else "Low Stock"

            card.findViewById<Button>(R.id.btn_view_details_quick).setOnClickListener {
                Toast.makeText(this, "Opening ${instSport.sportName} Management...", Toast.LENGTH_SHORT).show()
            }

            container.addView(card)
        }
    }

    private fun setupButtons() {
        // Institution Profile Edit
        findViewById<View>(R.id.btn_edit_inst_profile).setOnClickListener {
            startActivity(Intent(this, InstitutionProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_add_inst_sport)?.setOnClickListener {
            showAddSportDialog()
        }

        // Quick Actions
        setupListButton(R.id.btn_student_mgmt, getString(R.string.title_student_mgmt), getString(R.string.desc_student_mgmt), R.drawable.ic_person, R.color.brand_blue) {
            val intent = Intent(this, InstitutionStudentDirectoryActivity::class.java)
            intent.putExtra("INSTITUTION_ID", currentInstitutionId)
            startActivity(intent)
        }

        setupListButton(R.id.btn_pe_portal, getString(R.string.title_pe_teacher_portal), getString(R.string.desc_pe_teacher_portal), R.drawable.ic_coach_voice, R.color.brand_gold) {
            startActivity(Intent(this, InstitutionTeacherDirectoryActivity::class.java))
        }

        setupListButton(R.id.btn_inst_teams, getString(R.string.title_sports_teams), getString(R.string.desc_sports_teams), R.drawable.ic_groups_team, R.color.brand_green) {
            startActivity(Intent(this, InstitutionTeamActivity::class.java))
        }

        setupListButton(R.id.btn_inst_equipment, getString(R.string.title_equipment_inventory), getString(R.string.desc_equipment_inventory), R.drawable.ic_inventory_box, R.color.brand_red) {
            startActivity(Intent(this, InstitutionEquipmentActivity::class.java))
        }

        setupListButton(R.id.btn_inst_booking, getString(R.string.title_ground_facilities), getString(R.string.desc_ground_facilities), R.drawable.ic_stadium_facility, R.color.brand_blue) {
            startActivity(Intent(this, InstitutionBookingActivity::class.java))
        }

        setupListButton(R.id.btn_fit_india, getString(R.string.title_fit_india), getString(R.string.desc_fit_india), R.drawable.ic_fit_india_emblem, R.color.brand_green) {
            startActivity(Intent(this, InstitutionFitIndiaActivity::class.java))
        }

        setupListButton(R.id.btn_practical_exam, getString(R.string.title_practical_exam), getString(R.string.desc_practical_exam), R.drawable.ic_fact_check_attendance, R.color.brand_gold) {
            startActivity(Intent(this, InstitutionExamActivity::class.java))
        }

        setupListButton(R.id.btn_ai_student_dash, getString(R.string.title_ai_student_assessment), getString(R.string.desc_ai_student_assessment), R.drawable.ic_analytics_chart, R.color.brand_blue) {
            startActivity(Intent(this, InstitutionPerformanceActivity::class.java))
        }

        setupListButton(R.id.btn_inst_tournament, getString(R.string.title_tournament_management), getString(R.string.desc_tournament_management), R.drawable.ic_tournament_trophy, R.color.brand_gold) {
            startActivity(Intent(this, InstitutionTournamentActivity::class.java))
        }

        setupListButton(R.id.btn_inst_medical, getString(R.string.title_medical_injury_tracking), getString(R.string.desc_medical_injury_tracking), R.drawable.ic_medical_health, R.color.brand_red) {
            startActivity(Intent(this, InstitutionMedicalActivity::class.java))
        }

        setupListButton(R.id.btn_inst_calendar, getString(R.string.title_training_calendar), getString(R.string.desc_training_calendar), R.drawable.ic_calendar_schedule, R.color.brand_blue) {
            startActivity(Intent(this, InstitutionCalendarActivity::class.java))
        }

        setupListButton(R.id.btn_student_transfer, getString(R.string.title_student_record_transfer), getString(R.string.desc_student_record_transfer), R.drawable.ic_file_transfer, R.color.brand_gold) {
            val intent = Intent(this, AcademyTransferActivity::class.java)
            intent.putExtra("IS_INSTITUTION", true)
            startActivity(intent)
        }

        setupListButton(R.id.btn_inst_reports, getString(R.string.title_reports), getString(R.string.desc_reports), R.drawable.ic_report_summary, R.color.brand_red) {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        setupListButton(R.id.btn_parent_portal, getString(R.string.title_parent_portal), getString(R.string.desc_parent_portal), R.drawable.ic_groups_team, R.color.brand_blue) {
            startActivity(Intent(this, InstitutionParentPortalActivity::class.java))
        }

        setupListButton(R.id.btn_student_sports_profile, getString(R.string.title_student_sports_profile), getString(R.string.desc_student_sports_profile), R.drawable.ic_person, R.color.brand_green) {
            startActivity(Intent(this, StudentSportsProfileActivity::class.java))
        }

        setupListButton(R.id.btn_scholarship_mgmt, getString(R.string.title_scholarship_recommendation), getString(R.string.desc_scholarship_recommendation), android.R.drawable.btn_star_big_on, R.color.brand_gold) {
            startActivity(Intent(this, ScholarshipRecommendationActivity::class.java))
        }

        setupListButton(R.id.btn_university_recruitment, getString(R.string.title_university_recruitment), getString(R.string.desc_university_recruitment), android.R.drawable.ic_menu_share, R.color.brand_red) {
            startActivity(Intent(this, UniversityRecruitmentActivity::class.java))
        }

        setupListButton(R.id.btn_sports_analytics, getString(R.string.title_sports_analytics), getString(R.string.desc_sports_analytics), R.drawable.ic_analytics_chart, R.color.brand_blue) {
            startActivity(Intent(this, InstitutionSportsAnalyticsActivity::class.java))
        }

        setupListButton(R.id.btn_attendance_analytics, getString(R.string.title_attendance_analytics), getString(R.string.desc_attendance_analytics), R.drawable.ic_fact_check_attendance, R.color.brand_green) {
            startActivity(Intent(this, InstitutionAttendanceAnalyticsActivity::class.java))
        }

        setupListButton(R.id.btn_notification_center, getString(R.string.title_notification_center), getString(R.string.desc_notification_center), android.R.drawable.ic_popup_reminder, R.color.brand_gold) {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }

        setupListButton(R.id.btn_ai_video_analysis, getString(R.string.title_ai_video_analysis), getString(R.string.desc_ai_video_analysis), R.drawable.ic_ai_robot, R.color.brand_blue) {
            val intent = Intent(this, SportAnalysisActivity::class.java)
            intent.putExtra("SPORT_NAME", "Institution PE")
            startActivity(intent)
        }
    }

    private fun setupListButton(id: Int, title: String, subtitle: String, iconRes: Int, colorRes: Int, onClick: () -> Unit) {
        val view = findViewById<View>(id) ?: return
        view.findViewById<TextView>(R.id.tv_btn_label).text = title
        view.findViewById<TextView>(R.id.tv_btn_subtitle).text = subtitle
        val iv = view.findViewById<ImageView>(R.id.iv_btn_icon)
        iv.setImageResource(iconRes)
        iv.setColorFilter(getColor(colorRes))
        view.setOnClickListener { onClick() }
    }

    private fun showAddSportDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sport, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_sport_category)
        val categories = arrayOf("Indoor", "Outdoor", "Athletics", "Other")
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)

        val spGender = dialogView.findViewById<Spinner>(R.id.sp_sport_gender)
        val genders = arrayOf("Mixed", "Boys", "Girls")
        spGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)

        dialogView.findViewById<Button>(R.id.btn_confirm_add_sport).setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.et_sport_name_entry).text.toString().trim()
            if (name.isNotEmpty()) {
                val newSport = InstitutionSport(
                    institutionId = currentInstitutionId,
                    sportName = name,
                    category = spCategory.selectedItem.toString(),
                    genderType = spGender.selectedItem.toString()
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@InstitutionPortalActivity).institutionManagementDao().insertInstitutionSport(newSport)
                    dialog.dismiss()
                    Toast.makeText(this@InstitutionPortalActivity, "$name added to institution sports", Toast.LENGTH_SHORT).show()
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
