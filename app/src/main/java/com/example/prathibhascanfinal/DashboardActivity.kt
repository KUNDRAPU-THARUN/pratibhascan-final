package com.example.prathibhascanfinal

import android.graphics.Color
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.example.prathibhascanfinal.data.SportData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels() 

    private var userEmail: String = ""
    private lateinit var userName: String
    private lateinit var talentPoolAdapter: com.example.prathibhascanfinal.ui.adapter.TournamentListAdapter
    private lateinit var invitationAdapter: com.example.prathibhascanfinal.ui.adapter.InvitationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        
        try {
            val session = SessionManager(this)
            val userRole = session.getRole() ?: "Athlete"

            setContentView(R.layout.activity_dashboard)
            
            val root = findViewById<View>(R.id.dashboard_root)
            setupEdgeToEdge(root)

            // Specifically handle insets for header and bottom nav to avoid double padding if root already handles it
            // Actually, if setupEdgeToEdge handles the root, we might want to manually adjust specific views.
            // Let's use a more granular approach in setupEdgeToEdge if needed, or just apply specifically.
            
            ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val cutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                
                findViewById<View>(R.id.layout_global_header)?.updatePadding(
                    top = systemBars.top,
                    left = systemBars.left + cutouts.left,
                    right = systemBars.right + cutouts.right
                )
                
                findViewById<View>(R.id.bottom_nav)?.updatePadding(
                    bottom = systemBars.bottom
                )
                
                insets
            }

            userEmail = intent.getStringExtra("USER_EMAIL") ?: session.getEmail() ?: ""
            userName = intent.getStringExtra("USER_NAME") ?: session.getName() ?: "User"
            val uniqueId = intent.getStringExtra("UNIQUE_ID") ?: session.getUniqueId() ?: "PR-ATH-0000"

            findViewById<TextView>(R.id.tv_welcome_name)?.text = getString(R.string.welcome_user, userName)
            findViewById<TextView>(R.id.tv_unique_id)?.text = uniqueId
            findViewById<TextView>(R.id.tv_profile_id)?.text = getString(R.string.profile_id_format, uniqueId, userRole)

            setupRoleBasedUI(userRole)
            
            setupCategoryToggles()
            setupBottomNav()
            setupHeaderActions()
            setupBackNavigation()
            setupVernacularToggle()
            setupSportListeners()
            setupAiAssistant()
            setupAnalyticsListeners()
            setupTalentPoolUI()
            setupInstitutionGridListeners()
            loadUserProfile()
            
            if (userEmail.isNotEmpty()) {
                viewModel.startEnrollmentUpdates(this, userEmail)
            }
            
            observeDashboardState()
            checkLocationPermissions()
            handleIntentExtras(intent)

        } catch (e: Exception) {
            Log.e("DashboardActivity", "Dashboard UI Error: ${e.message}", e)
            Toast.makeText(this, "Dashboard UI Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAnalyticsListeners() {
        findViewById<View>(R.id.block_talent_heatmap)?.setOnClickListener {
            startActivity(Intent(this, TalentRegionActivity::class.java))
        }

        findViewById<View>(R.id.block_ai_coach_card)?.setOnClickListener {
            startActivity(Intent(this, AICoachActivity::class.java))
        }

        findViewById<View>(R.id.tv_performance_rank)?.parent?.let { parent ->
            (parent as View).setOnClickListener {
                findViewById<View>(R.id.nav_btn_rank)?.performClick()
            }
        }

        findViewById<View>(R.id.block_anti_cheat)?.setOnClickListener {
            startActivity(Intent(this, AchievementVaultActivity::class.java))
        }

        // Leaderboard logic inside the tab
        setupLeaderboardUI()
    }

    private fun setupTalentPoolUI() {
        findViewById<View>(R.id.btn_explore_scouts)?.setOnClickListener {
            startActivity(Intent(this, AcademyDiscoveryActivity::class.java))
        }
        
        findViewById<View>(R.id.btn_aca_locator_action)?.setOnClickListener {
            startActivity(Intent(this, TalentRegionActivity::class.java))
        }

        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_talent_pool_tournaments)
        talentPoolAdapter = com.example.prathibhascanfinal.ui.adapter.TournamentListAdapter(
            onView = { /* Details */ },
            onApply = { tournament ->
                viewModel.applyForTournament(tournament)
                Toast.makeText(this, "Application submitted for ${tournament.title}", Toast.LENGTH_SHORT).show()
            }
        )
        rv?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rv?.adapter = talentPoolAdapter

        // Invitations
        val rvInv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_academy_invitations)
        invitationAdapter = com.example.prathibhascanfinal.ui.adapter.InvitationAdapter(
            onAccept = { viewModel.respondToInvitation(it, true) },
            onDecline = { viewModel.respondToInvitation(it, false) }
        )
        rvInv?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvInv?.adapter = invitationAdapter
    }

    private fun setupLeaderboardUI() {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_leaderboard)
        val adapter = LeaderboardAdapter()
        rv?.adapter = adapter

        val btnNational = findViewById<TextView>(R.id.btn_toggle_national)
        val btnState = findViewById<TextView>(R.id.btn_toggle_state)

        btnNational?.setOnClickListener {
            btnNational.setBackgroundResource(R.drawable.bg_button_blue)
            btnNational.setTextColor(Color.WHITE)
            btnState?.setBackgroundResource(0)
            btnState?.setTextColor("#94A3B8".toColorInt())
            loadLeaderboardData(adapter, true)
        }

        btnState?.setOnClickListener {
            btnState.setBackgroundResource(R.drawable.bg_button_blue)
            btnState.setTextColor(Color.WHITE)
            btnNational?.setBackgroundResource(0)
            btnNational?.setTextColor("#94A3B8".toColorInt())
            loadLeaderboardData(adapter, false)
        }

        loadLeaderboardData(adapter, true)
    }

    private fun loadLeaderboardData(adapter: LeaderboardAdapter, isNational: Boolean) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val athletes = db.userDao().getAllAthletes()
            val filtered = if (isNational) {
                athletes.sortedByDescending { it.totalXP }
            } else {
                val myState = SessionManager(this@DashboardActivity).getEmail()?.let { 
                    db.userDao().getUserByEmail(it)?.state 
                }
                athletes.filter { it.state == myState }.sortedByDescending { it.totalXP }
            }
            adapter.submitList(filtered)
        }
    }

    private fun observeDashboardState() {
        val loadingProgress = findViewById<ProgressBar>(R.id.dashboard_loading_progress)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    loadingProgress?.isVisible = state.isLoadingWeather // Or a general isLoading flag
                    
                    // Update Time/Date
                    findViewById<TextView>(R.id.tv_live_time)?.text = state.time
                    
                    // Update Greeting
                    updateDynamicGreeting(state)

                    // Update Profile Score
                    state.userProfile?.let { user ->
                        val score = viewModel.calculateTalentScore(user)
                        findViewById<TextView>(R.id.tv_talent_score_profile_value)?.text = score.toString()
                        
                        findViewById<ProgressBar>(R.id.pb_speed)?.progress = user.speedScore
                        findViewById<ProgressBar>(R.id.pb_agility)?.progress = user.agilityScore
                        findViewById<ProgressBar>(R.id.pb_stamina)?.progress = user.staminaScore
                        findViewById<ProgressBar>(R.id.pb_strength)?.progress = user.strengthScore
                    }

                    // Update Weather
                    state.weather?.let { w ->
                        findViewById<TextView>(R.id.tv_header_weather)?.text = getString(R.string.weather_format, w.temp.toInt(), w.condition)
                        findViewById<TextView>(R.id.tv_weather_extra)?.text = getString(R.string.aqi_uv_format, w.aqi, w.uv)
                    }
                    
                    if (state.weatherError != null) {
                        findViewById<TextView>(R.id.tv_header_weather)?.text = getString(R.string.weather_unavailable)
                    }
                    
                    // Update Notification Badge
                    findViewById<View>(R.id.badge_notification)?.isVisible = state.unreadNotifications > 0

                    // Update Active Sports Arena (PREMIUM)
                    updateActiveSportsArena(state.enrolledSports)

                    // Update Talent Pool
                    if (::talentPoolAdapter.isInitialized) {
                        talentPoolAdapter.submitList(state.availableTournaments)
                    }

                    // Update Invitations
                    if (::invitationAdapter.isInitialized) {
                        invitationAdapter.submitList(state.pendingInvitations)
                    }
                    findViewById<View>(R.id.tv_label_invitations)?.isVisible = state.pendingInvitations.isNotEmpty()
                    findViewById<View>(R.id.rv_academy_invitations)?.isVisible = state.pendingInvitations.isNotEmpty()

                    // Update Analytics & Integrity
                    updateAnalyticsAndIntegrity(state)

                    // Update Performance Graph
                    if (state.accuracyTrend.isNotEmpty()) {
                        findViewById<AnalyticsGraphView>(R.id.graph_athlete_performance)?.setData(state.accuracyTrend)
                    }
                }
            }
        }
    }

    private fun updateAnalyticsAndIntegrity(state: DashboardUIState) {
        val u = state.userProfile ?: return
        
        // Impact & National Rank
        findViewById<TextView>(R.id.tv_impact_score)?.text = String.format(java.util.Locale.US, "%.1f", u.technicalImpactScore.takeIf { it > 0 } ?: 84.2)
        findViewById<TextView>(R.id.tv_national_rank)?.text = if (u.nationalRank > 0) "#${u.nationalRank}" else "#142"
        findViewById<TextView>(R.id.tv_global_rank)?.text = if (u.globalRank > 0) "#${u.globalRank}" else "#2109"

        // State Leaderboard Card on Dashboard
        findViewById<TextView>(R.id.tv_performance_rank)?.text = "Rank: #${u.districtRank.takeIf { it > 0 } ?: 4}"
        findViewById<TextView>(R.id.tv_rank_status)?.text = "Performance: ${if(u.technicalImpactScore > 80) "Elite" else if(u.technicalImpactScore > 50) "Pro" else "Rising"}"

        // Real-time AI Feedback Card
        val aiCoachStats = findViewById<TextView>(R.id.tv_ai_coach_stats)
        state.latestSession?.let { session ->
            aiCoachStats?.text = "Last: ${session.accuracy}% Accuracy | ${session.exerciseType}"
        } ?: run {
            aiCoachStats?.text = "Pose Accuracy: 92% (Demo)"
        }

        // Anti-Cheat System Card
        val antiCheatText = findViewById<TextView>(R.id.tv_anti_cheat_status)
        val antiCheatIcon = findViewById<ImageView>(R.id.iv_anti_cheat_status)
        
        state.latestSession?.let { session ->
            if (session.verificationStatus == "Verified") {
                antiCheatText?.text = "VERIFIED"
                antiCheatText?.setTextColor(Color.parseColor("#10B981"))
                antiCheatIcon?.setColorFilter(Color.parseColor("#10B981"))
            } else {
                antiCheatText?.text = "PENDING"
                antiCheatText?.setTextColor(Color.parseColor("#FBBF24"))
                antiCheatIcon?.setColorFilter(Color.parseColor("#FBBF24"))
            }
        }
    }

    private fun setupInstitutionGridListeners() {
        findViewById<View>(R.id.btn_student_mgmt_grid)?.setOnClickListener {
            startActivity(Intent(this, InstitutionPortalActivity::class.java))
        }
        findViewById<View>(R.id.btn_pe_portal_grid)?.setOnClickListener {
            startActivity(Intent(this, InstitutionPortalActivity::class.java))
        }
        findViewById<View>(R.id.btn_team_mgmt_grid)?.setOnClickListener {
            startActivity(Intent(this, InstitutionPortalActivity::class.java))
        }
        findViewById<View>(R.id.btn_inventory_mgmt_grid)?.setOnClickListener {
            startActivity(Intent(this, InstitutionPortalActivity::class.java))
        }
    }

    private var lastEnrolledSports: List<SportEnrollment> = emptyList()

    private fun updateActiveSportsArena(sports: List<SportEnrollment>) {
        val container = findViewById<LinearLayout>(R.id.container_active_game_buttons)
        val block = findViewById<View>(R.id.block_active_games)

        if (sports == lastEnrolledSports && (container?.childCount ?: 0) > 0) {
            return // Optimization: No changes in list, skip re-inflation
        }
        lastEnrolledSports = sports
        
        Log.d("ENROLL_SYNC", "Updating Arena with ${sports.size} sports")
        
        if (sports.isNotEmpty()) {
            block?.visibility = View.VISIBLE
            container?.removeAllViews()
            sports.forEach { enrollment ->
                val cardView = layoutInflater.inflate(R.layout.item_active_sport_card, container, false)
                
                cardView.findViewById<TextView>(R.id.tv_sport_name).text = enrollment.sportName.uppercase()
                cardView.findViewById<TextView>(R.id.tv_sport_details).text = "${enrollment.positionStyle} • ID: ${enrollment.athleteSportId}"
                cardView.findViewById<TextView>(R.id.tv_sport_xp).text = "+300" 
                
                val statusBadge = cardView.findViewById<TextView>(R.id.tv_status_badge)
                statusBadge.text = enrollment.registrationStatus.uppercase()
                
                val statusColor = when(enrollment.registrationStatus) {
                    "Verified" -> Color.parseColor("#10B981")
                    "Pending" -> Color.parseColor("#FBBF24")
                    else -> Color.parseColor("#3B82F6")
                }
                TextViewCompat.setCompoundDrawableTintList(statusBadge, android.content.res.ColorStateList.valueOf(statusColor))
                statusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(statusColor)

                val icon = cardView.findViewById<ImageView>(R.id.iv_sport_icon)
                icon.setImageResource(getSportIcon(enrollment.sportName))
                
                cardView.findViewById<View>(R.id.iv_blockchain_badge).isVisible = enrollment.blockchainSportId.isNotEmpty()

                cardView.findViewById<View>(R.id.btn_open_arena).setOnClickListener {
                    val intent = Intent(this, SportDashboardActivity::class.java)
                    intent.putExtra("ENROLLMENT_DATA", enrollment)
                    startActivity(intent)
                }

                cardView.findViewById<View>(R.id.btn_ai_coach).setOnClickListener {
                    val intent = Intent(this, AICoachActivity::class.java)
                    intent.putExtra("ENROLLMENT_DATA", enrollment)
                    startActivity(intent)
                }

                cardView.findViewById<View>(R.id.btn_ar_ghost).setOnClickListener {
                    val intent = Intent(this, SkillSelectionActivity::class.java)
                    intent.putExtra("ENROLLMENT_DATA", enrollment)
                    intent.putExtra("LAUNCH_AR_GHOST", true)
                    startActivity(intent)
                }

                container?.addView(cardView)
            }
        } else {
            block?.visibility = View.GONE
        }
    }

    private fun getSportIcon(sport: String): Int = SportData.getIcon(sport)

    private fun getSportCategory(sport: String): String = SportData.getCategory(sport)

    private fun updateDynamicGreeting(state: DashboardUIState) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            in 17..20 -> R.string.good_evening
            else -> R.string.good_night
        }
        val displayName = state.userProfile?.fullName ?: userName
        findViewById<TextView>(R.id.tv_welcome_name)?.text = getString(greetingRes, displayName)
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            fetchCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        location?.let {
                            viewModel.fetchWeather(it.latitude, it.longitude)
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("LOCATION", "Failed to get location", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        }
    }

    private fun setupRoleBasedUI(role: String) {
        val isViewOnly = intent.getBooleanExtra("VIEW_ONLY", false)
        
        val passport = findViewById<View>(R.id.card_passport)
        val aiAssistant = findViewById<View>(R.id.block_ai_assistant)
        val localEvents = findViewById<View>(R.id.block_local_events)
        val athleticDomain = findViewById<View>(R.id.block_athletic_domain)
        val impactRankLabel = findViewById<View>(R.id.tv_label_impact_rank)
        val impactRankLayout = findViewById<View>(R.id.layout_impact_rank)
        val realTimeAiLabel = findViewById<View>(R.id.tv_label_real_time_ai)
        val realTimeAiLayout = findViewById<View>(R.id.layout_real_time_ai)
        val analyticsBlock = findViewById<View>(R.id.block_performance_analytics)
        val institutional = findViewById<View>(R.id.block_institutional_ecosystem)
        val instPortalGrid = findViewById<View>(R.id.block_institution_portal_grid)
        val academyDiscovery = findViewById<View>(R.id.block_academy_discovery)
        val academyMgmtGrid = findViewById<View>(R.id.block_academy_portal_grid)
        
        val bottomNav = findViewById<View>(R.id.bottom_nav)

        // Reset all to visible first
        val allViews = listOf(passport, aiAssistant, localEvents, academyDiscovery, athleticDomain, 
                              impactRankLabel, impactRankLayout, realTimeAiLabel, realTimeAiLayout, 
                              analyticsBlock, institutional, instPortalGrid, academyMgmtGrid)
        allViews.forEach { it?.visibility = View.VISIBLE }

        if (isViewOnly) {
            bottomNav?.visibility = View.GONE
            findViewById<View>(R.id.layout_global_header)?.visibility = View.GONE
            findViewById<View>(R.id.btn_header_back)?.visibility = View.VISIBLE
            findViewById<View>(R.id.btn_header_back)?.setOnClickListener { finish() }
            
            // Only show performance relevant sections
            localEvents?.visibility = View.GONE
            academyDiscovery?.visibility = View.GONE
            institutional?.visibility = View.GONE
            instPortalGrid?.visibility = View.GONE
            academyMgmtGrid?.visibility = View.GONE
            athleticDomain?.visibility = View.GONE
            aiAssistant?.visibility = View.GONE
            
            // Special treatment for Profile Tab ScrollView
            val sProfile = findViewById<View>(R.id.scroll_view_profile)
            updateTabVisibility(sProfile)
            return
        }

        when (role) {
            "Athlete" -> {
                findViewById<TextView>(R.id.tv_profile_subtitle)?.text = getString(R.string.athlete_profile)
                academyDiscovery?.visibility = View.GONE 
                institutional?.visibility = View.GONE
                instPortalGrid?.visibility = View.GONE
                academyMgmtGrid?.visibility = View.GONE
            }
            "Academy" -> {
                findViewById<TextView>(R.id.tv_profile_subtitle)?.text = getString(R.string.academy_profile_subtitle)
                findViewById<TextView>(R.id.badge_blockchain)?.text = getString(R.string.academy_blockchain_credentials)
                val tvVerified = findViewById<TextView>(R.id.tv_verified)
                tvVerified?.let {
                    it.text = getString(R.string.verified_academy_partner)
                    it.setTextColor("#3B82F6".toColorInt())
                    TextViewCompat.setCompoundDrawableTintList(it, android.content.res.ColorStateList.valueOf("#3B82F6".toColorInt()))
                }

                aiAssistant?.visibility = View.VISIBLE
                localEvents?.visibility = View.GONE
                athleticDomain?.visibility = View.GONE
                realTimeAiLabel?.visibility = View.GONE
                realTimeAiLayout?.visibility = View.GONE
                analyticsBlock?.visibility = View.GONE
                institutional?.visibility = View.GONE
                instPortalGrid?.visibility = View.GONE
                
                academyDiscovery?.visibility = View.VISIBLE
                academyMgmtGrid?.visibility = View.GONE
            }
            "Institution" -> {
                findViewById<TextView>(R.id.tv_profile_subtitle)?.text = getString(R.string.institution_portal_subtitle)
                passport?.visibility = View.GONE
                aiAssistant?.visibility = View.GONE
                academyDiscovery?.visibility = View.GONE
                athleticDomain?.visibility = View.GONE
                realTimeAiLabel?.visibility = View.GONE
                realTimeAiLayout?.visibility = View.GONE
                analyticsBlock?.visibility = View.GONE
                academyMgmtGrid?.visibility = View.GONE
                
                institutional?.visibility = View.VISIBLE
                instPortalGrid?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupAiAssistant() {
        findViewById<View>(R.id.btn_launch_ai_chat)?.setOnClickListener {
            startActivity(Intent(this, AICoachActivity::class.java))
        }
        findViewById<View>(R.id.btn_launch_ar_guidance)?.setOnClickListener {
            startActivity(Intent(this, SportAnalysisActivity::class.java))
        }
    }

    private fun setupSportListeners() {
        val hsvIds = listOf(R.id.hsv_indoor, R.id.hsv_outdoor, R.id.hsv_athletics, R.id.hsv_specialty)
        hsvIds.forEach { id ->
            val hsv = findViewById<HorizontalScrollView>(id)
            val parent = hsv?.getChildAt(0) as? LinearLayout
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val sportModule = parent.getChildAt(i) as? LinearLayout
                    sportModule?.setSafeOnClickListener {
                        var sportTitle = "Selected Sport"
                        for (j in 0 until sportModule.childCount) {
                            val child = sportModule.getChildAt(j)
                            if (child is TextView) {
                                sportTitle = child.text.toString()
                                break
                            }
                        }
                        
                        lifecycleScope.launch {
                            val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@DashboardActivity) }
                            
                            val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                            user?.let { 
                                val updatedUser = it.copy(primaryDiscipline = sportTitle)
                                withContext(Dispatchers.IO) { db.userDao().insertUser(updatedUser) }
                                SessionManager(this@DashboardActivity).saveSession(
                                    it.email, it.fullName, it.uniqueId, it.role, sportTitle
                                )
                            }

                            val enrollment = withContext(Dispatchers.IO) { db.sportEnrollmentDao().getEnrollmentForSport(userEmail, sportTitle) }
                            
                            if (enrollment == null) {
                                val intent = Intent(this@DashboardActivity, SportEnrollmentActivity::class.java)
                                intent.putExtra("SPORT_NAME", sportTitle)
                                intent.putExtra("SPORT_CAT", getSportCategory(sportTitle))
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@DashboardActivity, "Already Enrolled! Opening Arena...", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@DashboardActivity, SportDashboardActivity::class.java)
                                intent.putExtra("ENROLLMENT_DATA", enrollment)
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupVernacularToggle() {
        findViewById<TextView>(R.id.btn_vernacular_toggle)?.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun setupHeaderActions() {
        findViewById<View>(R.id.iv_profile_header)?.setOnClickListener {
            val bottomSheet = ProfileBottomSheetFragment()
            bottomSheet.onMenuItemClick = { id ->
                when(id) {
                    R.id.menu_my_profile -> {
                        val sProfile = findViewById<View>(R.id.scroll_view_profile)
                        updateTabVisibility(sProfile)
                        updateNavUI(0) 
                        loadUserProfile()
                    }
                    R.id.menu_logout -> {
                        SessionManager(this).logout()
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    }
                    R.id.menu_settings -> {
                        val sSettings = findViewById<View>(R.id.scroll_view_settings)
                        updateTabVisibility(sSettings)
                    }
                    R.id.menu_achievements -> {
                        startActivity(Intent(this, AchievementVaultActivity::class.java))
                    }
                    R.id.menu_history -> {
                        startActivity(Intent(this, AchievementVaultActivity::class.java))
                    }
                    R.id.menu_privacy -> {
                        val sSettings = findViewById<View>(R.id.scroll_view_settings)
                        updateTabVisibility(sSettings)
                    }
                    R.id.menu_help -> {
                        Toast.makeText(this, "Opening Help Center...", Toast.LENGTH_SHORT).show()
                    }
                    else -> Toast.makeText(this, "Opening Feature...", Toast.LENGTH_SHORT).show()
                }
            }
            bottomSheet.show(supportFragmentManager, ProfileBottomSheetFragment.TAG)
        }

        findViewById<View>(R.id.layout_notification_bell)?.setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }
        
        findViewById<View>(R.id.iv_header_search)?.setOnClickListener {
            SearchDialog().show(supportFragmentManager, SearchDialog.TAG)
        }
        
        findViewById<View>(R.id.iv_ai_coach_header)?.setOnClickListener {
            startActivity(Intent(this, AICoachActivity::class.java))
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    val homeScroll = findViewById<View>(R.id.scroll_view_home)
                    if (homeScroll != null && homeScroll.visibility != View.VISIBLE) {
                        findViewById<View>(R.id.nav_btn_home)?.performClick()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    override fun onResume() {
        super.onResume()
        if (userEmail.isNotEmpty()) {
            loadUserProfile()
            viewModel.startEnrollmentUpdates(this, userEmail)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent?) {
        val tab = intent?.getIntExtra("OPEN_TAB", -1) ?: -1
        if (tab != -1) {
            updateNavUI(tab)
            when (tab) {
                1 -> updateTabVisibility(findViewById(R.id.scroll_view_home))
                2 -> updateTabVisibility(findViewById(R.id.scroll_view_talent))
                3 -> updateTabVisibility(findViewById(R.id.scroll_view_rank))
                4 -> updateTabVisibility(findViewById(R.id.scroll_view_vault))
            }
        }
    }

    private fun setupCategoryToggles() {
        fun bindToggle(headerId: Int, listId: Int) {
            val header = findViewById<View>(headerId)
            val list = findViewById<View>(listId)
            if (header != null && list != null) {
                header.setOnClickListener { toggleCategory(header, list) }
            }
        }
        bindToggle(R.id.tv_header_indoor, R.id.hsv_indoor)
        bindToggle(R.id.tv_header_outdoor, R.id.hsv_outdoor)
        bindToggle(R.id.tv_header_athletics, R.id.hsv_athletics)
        bindToggle(R.id.tv_header_specialty, R.id.hsv_specialty)
        bindToggle(R.id.tv_aca_profile_header, R.id.hsv_aca_profile)
        bindToggle(R.id.tv_aca_coach_header, R.id.hsv_aca_coach)
        bindToggle(R.id.tv_aca_map_header, R.id.hsv_aca_map)

        findViewById<View>(R.id.btn_aca_reg_discovery)?.setSafeOnClickListener {
            startActivity(Intent(this, AcademyRegistrationActivity::class.java))
        }
        findViewById<View>(R.id.btn_aca_ops_portal)?.setSafeOnClickListener {
            startActivity(Intent(this, AcademyPortalActivity::class.java))
        }
        findViewById<View>(R.id.btn_aca_recruit_talent)?.setSafeOnClickListener {
            startActivity(Intent(this, AthleteDiscoveryActivity::class.java))
        }
        findViewById<View>(R.id.btn_aca_view_profile)?.setSafeOnClickListener {
            val sProfile = findViewById<View>(R.id.scroll_view_profile)
            updateTabVisibility(sProfile)
            updateNavUI(0)
            loadUserProfile()
        }
        findViewById<View>(R.id.btn_aca_direct_coach)?.setSafeOnClickListener {
            startActivity(Intent(this, CoachPortfolioActivity::class.java))
        }
        findViewById<View>(R.id.btn_aca_radar_action)?.setSafeOnClickListener {
            startActivity(Intent(this, TalentRadarActivity::class.java))
        }

        bindToggle(R.id.tv_header_school_reg, R.id.hsv_school_reg)
        bindToggle(R.id.tv_header_pe_reg, R.id.hsv_pe_reg)
        bindToggle(R.id.tv_header_student_reg_eco, R.id.hsv_student_reg_eco)
        bindToggle(R.id.tv_header_digital_records, R.id.hsv_digital_records)
        bindToggle(R.id.tv_header_talent_discovery, R.id.hsv_talent_discovery)

        findViewById<View>(R.id.btn_school_reg_eco)?.setSafeOnClickListener {
            startActivity(Intent(this, InstitutionRegistrationActivity::class.java))
        }
    }

    private fun toggleCategory(header: View, list: View) {
        val isVisible = list.isVisible
        if (isVisible) {
            list.visibility = View.GONE
        } else {
            list.visibility = View.VISIBLE
        }
        updateArrow(header, !isVisible)
    }

    private fun updateArrow(header: View, isExpanded: Boolean) {
        val arrowRes = if (isExpanded) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float
        if (header is TextView) {
            header.setCompoundDrawablesWithIntrinsicBounds(
                header.compoundDrawables[0],
                null,
                AppCompatResources.getDrawable(this, arrowRes),
                null
            )
        } else if (header is ViewGroup) {
            val arrowId = when (header.id) {
                R.id.tv_header_indoor -> R.id.iv_arrow_indoor
                R.id.tv_header_outdoor -> R.id.iv_arrow_outdoor
                R.id.tv_header_athletics -> R.id.iv_arrow_athletics
                R.id.tv_header_specialty -> R.id.iv_arrow_specialty
                else -> -1
            }
            if (arrowId != -1) {
                header.findViewById<ImageView>(arrowId)?.setImageResource(arrowRes)
            }
        }
    }

    private fun setupBottomNav() {
        val sArena = findViewById<View>(R.id.scroll_view_home)
        val sTalent = findViewById<View>(R.id.scroll_view_talent)
        val sRank = findViewById<View>(R.id.scroll_view_rank)
        val sVault = findViewById<View>(R.id.scroll_view_vault)

        findViewById<View>(R.id.nav_btn_home)?.setOnClickListener {
            val sport = SessionManager(this).getSport()
            if (sport != null) {
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(this@DashboardActivity)
                    val enrollment = db.sportEnrollmentDao().getEnrollmentForSport(userEmail, sport)
                    val intent = Intent(this@DashboardActivity, SportDashboardActivity::class.java)
                    if (enrollment != null) {
                        intent.putExtra("ENROLLMENT_DATA", enrollment)
                    } else {
                        intent.putExtra("SPORT_NAME", sport)
                    }
                    startActivity(intent)
                }
            } else {
                updateTabVisibility(sArena)
                updateNavUI(1)
            }
        }
        findViewById<View>(R.id.nav_btn_talent)?.setOnClickListener {
            updateTabVisibility(sTalent)
            updateNavUI(2)
        }
        findViewById<View>(R.id.nav_btn_rank)?.setOnClickListener {
            updateTabVisibility(sRank)
            updateNavUI(3)
        }
        findViewById<View>(R.id.nav_btn_vault)?.setOnClickListener {
            updateTabVisibility(sVault)
            updateNavUI(4)
        }

        findViewById<View>(R.id.btn_edit_profile)?.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
        findViewById<View>(R.id.btn_profile_settings)?.setOnClickListener {
            val sSettings = findViewById<View>(R.id.scroll_view_settings)
            updateTabVisibility(sSettings)
        }
        findViewById<View>(R.id.btn_set_edit_profile)?.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
        findViewById<View>(R.id.btn_logout_profile)?.setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.btn_open_vault)?.setOnClickListener {
            startActivity(Intent(this, AchievementVaultActivity::class.java))
        }
        findViewById<View>(R.id.btn_add_achievement_profile)?.setOnClickListener {
            startActivity(Intent(this, AchievementVaultActivity::class.java))
        }
        findViewById<View>(R.id.btn_achievements_stats)?.setOnClickListener {
            startActivity(Intent(this, AchievementVaultActivity::class.java))
        }
        findViewById<View>(R.id.btn_upload_achievement_profile)?.setOnClickListener {
            startActivity(Intent(this, AchievementVaultActivity::class.java))
        }
        findViewById<View>(R.id.btn_upload_vault_new)?.setOnClickListener {
            startActivity(Intent(this, AchievementVaultActivity::class.java))
        }
        findViewById<View>(R.id.btn_open_health)?.setOnClickListener {
            startActivity(Intent(this, HealthUpdateActivity::class.java))
        }
        findViewById<View>(R.id.btn_set_language)?.setOnClickListener {
            showLanguageDialog()
        }

        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_discovery_opt)?.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@DashboardActivity)
                val user = db.userDao().getUserByEmail(userEmail)
                user?.let {
                    val updated = it.copy(privacy = if (isChecked) "Public" else "Private")
                    db.userDao().insertUser(updated)
                    Log.d("FIRESTORE_SYNC", "TRIGGER: Privacy change for ${updated.email}")
                    com.example.prathibhascanfinal.data.repository.FirestoreRepository().saveEntity("users", updated.email, updated)
                }
            }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.lang_en), getString(R.string.lang_hi), getString(R.string.lang_te),
            getString(R.string.lang_ta), getString(R.string.lang_kn), getString(R.string.lang_ml),
            getString(R.string.lang_mr), getString(R.string.lang_bn), getString(R.string.lang_gu),
            getString(R.string.lang_pa), getString(R.string.lang_or)
        )
        val codes = arrayOf("en", "hi", "te", "ta", "kn", "ml", "mr", "bn", "gu", "pa", "or")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setItems(languages) { _, which ->
                LocaleHelper.setLocale(this, codes[which])
                Toast.makeText(this, R.string.toast_language_changed, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateTabVisibility(activeTab: View?) {
        findViewById<View>(R.id.scroll_view_home)?.visibility = View.GONE
        findViewById<View>(R.id.scroll_view_talent)?.visibility = View.GONE
        findViewById<View>(R.id.scroll_view_rank)?.visibility = View.GONE
        findViewById<View>(R.id.scroll_view_vault)?.visibility = View.GONE
        findViewById<View>(R.id.scroll_view_profile)?.visibility = View.GONE
        findViewById<View>(R.id.scroll_view_settings)?.visibility = View.GONE
        activeTab?.visibility = View.VISIBLE
    }

    private fun updateNavUI(activeBtn: Int) {
        val activeColor = 0xFF3B82F6.toInt()
        val inactiveColor = 0xFF94A3B8.toInt()
        findViewById<ImageView>(R.id.iv_nav_home)?.setColorFilter(if (activeBtn == 1) activeColor else inactiveColor)
        findViewById<TextView>(R.id.tv_nav_home)?.setTextColor(if (activeBtn == 1) activeColor else inactiveColor)
        findViewById<ImageView>(R.id.iv_nav_talent)?.setColorFilter(if (activeBtn == 2) activeColor else inactiveColor)
        findViewById<TextView>(R.id.tv_nav_talent)?.setTextColor(if (activeBtn == 2) activeColor else inactiveColor)
        findViewById<ImageView>(R.id.iv_nav_rank)?.setColorFilter(if (activeBtn == 3) activeColor else inactiveColor)
        findViewById<TextView>(R.id.tv_nav_rank)?.setTextColor(if (activeBtn == 3) activeColor else inactiveColor)
        findViewById<ImageView>(R.id.iv_nav_vault)?.setColorFilter(if (activeBtn == 4) activeColor else inactiveColor)
        findViewById<TextView>(R.id.tv_nav_vault)?.setTextColor(if (activeBtn == 4) activeColor else inactiveColor)
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@DashboardActivity)
                val user = db.userDao().getUserByEmail(userEmail)
                user?.let { u ->
                    val role = u.role
                    findViewById<TextView>(R.id.tv_profile_name)?.text = u.fullName
                    findViewById<TextView>(R.id.tv_profile_id)?.text = getString(R.string.profile_id_format, u.uniqueId, role)
                    
                    val displayId = if (u.uniqueId.isNotEmpty() && !u.uniqueId.contains("RECOVERING")) u.uniqueId else {
                        val prefix = if (role == "Academy") "ACD" else if (role == "Institution") "INS" else "ATH"
                        val newId = "PR-$prefix-${(100000000..999999999).random()}"
                        // Auto-save the new ID if it was missing or a recovery placeholder
                        lifecycleScope.launch(Dispatchers.IO) {
                            val updated = u.copy(uniqueId = newId)
                            db.userDao().insertUser(updated)
                            Log.d("FIRESTORE_SYNC", "TRIGGER: ID recovery for ${updated.email}")
                            com.example.prathibhascanfinal.data.repository.FirestoreRepository().saveEntity("users", updated.email, updated)
                        }
                        newId
                    }
                    findViewById<TextView>(R.id.tv_unique_id)?.text = displayId
                    
                    val athleteStats = findViewById<View>(R.id.grid_physical_stats)
                    val athleteHealth = findViewById<View>(R.id.btn_open_health)
                    val athleteAnalytics = findViewById<View>(R.id.block_athlete_analytics)
                    
                    val academyRoot = findViewById<View>(R.id.layout_academy_profile_root)
                    val institutionRoot = findViewById<View>(R.id.layout_institution_profile_root)
                    
                    athleteStats?.visibility = View.GONE
                    athleteHealth?.visibility = View.GONE
                    athleteAnalytics?.visibility = View.GONE
                    academyRoot?.visibility = View.GONE
                    institutionRoot?.visibility = View.GONE

                    when (role) {
                        "Athlete" -> {
                            athleteStats?.visibility = View.VISIBLE
                            athleteHealth?.visibility = View.VISIBLE
                            athleteAnalytics?.visibility = View.VISIBLE
                            
                            findViewById<TextView>(R.id.tv_stat_height)?.text = getString(R.string.stat_height_format, u.height ?: "---")
                            findViewById<TextView>(R.id.tv_stat_wingspan)?.text = getString(R.string.stat_wingspan_format, u.wingSpan ?: "---")
                            findViewById<TextView>(R.id.tv_stat_side)?.text = u.dominantSide
                            findViewById<TextView>(R.id.tv_stat_tier)?.text = u.currentTier ?: "District"
                            
                            findViewById<AnalyticsGraphView>(R.id.graph_athlete_performance)?.setData(listOf(0.3f, 0.45f, 0.4f, 0.6f, 0.55f, 0.75f, 0.85f))
                            findViewById<TextView>(R.id.tv_streak_count)?.text = u.streakCount.toString()
                            findViewById<TextView>(R.id.tv_xp_points)?.text = u.totalXP.toString()
                        }
                        "Academy" -> {
                            academyRoot?.visibility = View.VISIBLE
                            findViewById<TextView>(R.id.tv_streak_label)?.text = "Active Coaches"
                            findViewById<TextView>(R.id.tv_streak_count)?.text = "12"
                            findViewById<TextView>(R.id.tv_xp_label)?.text = "Success Rate"
                            findViewById<TextView>(R.id.tv_xp_points)?.text = "82%"
                            
                            findViewById<AnalyticsGraphView>(R.id.graph_academy_discovery)?.setData(listOf(0.2f, 0.3f, 0.5f, 0.4f, 0.7f, 0.8f, 0.9f))
                        }
                        "Institution" -> {
                            institutionRoot?.visibility = View.VISIBLE
                            findViewById<TextView>(R.id.tv_streak_label)?.text = "Total Students"
                            findViewById<TextView>(R.id.tv_streak_count)?.text = "850"
                            findViewById<TextView>(R.id.tv_xp_label)?.text = "Pro-Pool Referral"
                            findViewById<TextView>(R.id.tv_xp_points)?.text = "14"
                            
                            findViewById<AnalyticsGraphView>(R.id.graph_institution_fitness)?.setData(listOf(0.5f, 0.55f, 0.52f, 0.62f, 0.65f, 0.68f, 0.72f))
                        }
                    }

                    findViewById<TextView>(R.id.tv_impact_score)?.text = String.format(java.util.Locale.US, "%.1f", u.technicalImpactScore.takeIf { s -> s > 0 } ?: 84.2)
                    findViewById<TextView>(R.id.tv_national_rank)?.text = if (u.nationalRank > 0) "#${u.nationalRank}" else "#142"
                    findViewById<TextView>(R.id.tv_global_rank)?.text = if (u.globalRank > 0) "#${u.globalRank}" else "#2109"
                    
                    findViewById<TextView>(R.id.tv_profile_health_status)?.text = "Status: ${u.injuryStatus}"
                    findViewById<TextView>(R.id.tv_profile_hydration)?.text = "Hydration: ${u.hydrationLevel}L"

                    loadGloryTimeline(db)
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadGloryTimeline(db: AppDatabase) {
        lifecycleScope.launch {
            val achievements = db.achievementDao().getAchievementsForUser(userEmail)
            val container = findViewById<LinearLayout>(R.id.container_timeline)
            if (achievements.isNotEmpty()) {
                container?.removeAllViews()
                achievements.take(3).forEach { ach ->
                    val tv = TextView(this@DashboardActivity).apply {
                        text = "[${ach.eventYear}] 🏆 ${ach.tournamentName}"
                        setTextColor(0xFFFBBF24.toInt()) // Gold
                        textSize = 12f
                        setPadding(0, 8, 0, 0)
                    }
                    container?.addView(tv)
                }
            }
        }
    }
}

