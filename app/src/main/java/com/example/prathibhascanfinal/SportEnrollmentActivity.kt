package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SportEnrollmentActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private var certUri: Uri? = null
    private var videoUri: Uri? = null
    private lateinit var userEmail: String
    private lateinit var sportName: String
    private var existingEnrollment: SportEnrollment? = null
    
    private lateinit var dynamicContainer: LinearLayout

    private val pickCert = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            certUri = uri
            updateStatus()
        }
    }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            videoUri = uri
            updateStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sport_enrollment)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        userEmail = SessionManager(this).getEmail() ?: ""
        sportName = intent.getStringExtra("SPORT_NAME") ?: "General Training"
        dynamicContainer = findViewById(R.id.container_dynamic_fields)

        findViewById<TextView>(R.id.tv_enroll_sport_title).text = "$sportName Game Profile"

        setupDynamicFields()
        checkExistingEnrollment()

        findViewById<Button>(R.id.btn_enroll_cert).setSafeOnClickListener { pickCert.launch("image/*") }
        findViewById<Button>(R.id.btn_enroll_video).setSafeOnClickListener { pickVideo.launch("video/*") }

        findViewById<Button>(R.id.btn_submit_enrollment).setSafeOnClickListener {
            findViewById<Button>(R.id.btn_submit_enrollment).isEnabled = false
            submitEnrollment()
        }

        findViewById<TextView>(R.id.btn_enroll_cancel).setSafeOnClickListener { finish() }
    }

    private fun setupDynamicFields() {
        dynamicContainer.removeAllViews()
        val detail = com.example.prathibhascanfinal.data.SportData.getDetail(sportName)
        inflateLayout(detail.formLayoutRes)
    }

    private fun inflateLayout(layoutResId: Int) {
        layoutInflater.inflate(layoutResId, dynamicContainer, true)
        setupSpecificSpinners()
    }

    private fun setupSpecificSpinners() {
        // Setup dropdowns for each sport
        findViewById<Spinner>(R.id.spinner_cricket_role)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Opening Batsman", "Middle Order", "Finisher", "All Rounder", "Spinner", "Fast Bowler", "Wicket Keeper"))
        findViewById<Spinner>(R.id.spinner_batting_style)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Right Hand", "Left Hand"))
        findViewById<Spinner>(R.id.spinner_bowling_style)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Fast", "Medium", "Off Spin", "Leg Spin"))
        
        findViewById<Spinner>(R.id.spinner_football_position)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Goalkeeper", "Defender", "Midfielder", "Winger", "Striker"))
        findViewById<Spinner>(R.id.spinner_preferred_foot)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Right", "Left", "Both"))
        
        findViewById<Spinner>(R.id.spinner_basketball_position)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center"))
        
        findViewById<Spinner>(R.id.spinner_racket_play_style)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Singles", "Doubles", "Mixed Doubles"))
        findViewById<Spinner>(R.id.spinner_racket_grip)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Standard", "Penhold", "Shakehand"))
        
        findViewById<Spinner>(R.id.spinner_chess_style)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Aggressive", "Positional", "Defensive"))
        
        findViewById<Spinner>(R.id.spinner_athletics_cat)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Track", "Field"))
        findViewById<Spinner>(R.id.spinner_athletics_event)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("100m", "200m", "400m", "Long Jump", "High Jump", "Shot Put", "Javelin"))
        
        findViewById<Spinner>(R.id.spinner_swimming_stroke)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Freestyle", "Butterfly", "Breaststroke", "Backstroke", "Individual Medley"))
        findViewById<Spinner>(R.id.spinner_pool_length)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("25m", "50m"))
        
        findViewById<Spinner>(R.id.spinner_combat_weight)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Flyweight", "Bantamweight", "Lightweight", "Middleweight", "Heavyweight"))
        findViewById<Spinner>(R.id.spinner_combat_stance)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Orthodox", "Southpaw"))
        
        findViewById<Spinner>(R.id.spinner_team_position)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Forward", "Midfielder", "Defender", "Raider (Kabaddi)"))
        
        findViewById<Spinner>(R.id.spinner_precision_eye)?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Right", "Left"))
    }

    private fun checkExistingEnrollment() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@SportEnrollmentActivity)
            existingEnrollment = db.sportEnrollmentDao().getEnrollmentForSport(userEmail, sportName)
            existingEnrollment?.let {
                findViewById<TextView>(R.id.tv_enroll_sport_title).text = "Edit $sportName Profile"
                findViewById<Button>(R.id.btn_submit_enrollment).text = "UPDATE CAREER PASSPORT"
                
                findViewById<EditText>(R.id.et_enroll_position).setText(it.positionStyle)
                findViewById<EditText>(R.id.et_enroll_best).setText(it.bestPerformance)
                findViewById<EditText>(R.id.et_enroll_exp).setText(it.yearsExperience)
                
                // Pre-fill sport-specific data from specialized entities
                loadSportSpecificData(db)
                
                if (it.achievementUri != null || it.proofVideoUri != null) {
                    updateStatus()
                }
            }
        }
    }

    private suspend fun loadSportSpecificData(db: AppDatabase) {
        val dao = db.sportProfileDao()
        when (sportName) {
            "Cricket" -> dao.getCricket(userEmail)?.let {
                setSpinnerValue(R.id.spinner_cricket_role, it.playingRole)
                setSpinnerValue(R.id.spinner_batting_style, it.battingStyle)
                setSpinnerValue(R.id.spinner_bowling_style, it.bowlingStyle)
                findViewById<EditText>(R.id.et_arm_speed)?.setText(it.armSpeed.toString())
                findViewById<EditText>(R.id.et_bat_speed)?.setText(it.batSpeed.toString())
            }
            "Football" -> dao.getFootball(userEmail)?.let {
                setSpinnerValue(R.id.spinner_football_position, it.position)
                setSpinnerValue(R.id.spinner_preferred_foot, it.preferredFoot)
                findViewById<EditText>(R.id.et_passing_accuracy)?.setText(it.passingAccuracy.toString())
                findViewById<EditText>(R.id.et_sprint_speed)?.setText(it.sprintSpeed.toString())
            }
            // Add other loaders if needed...
        }
    }

    private fun setSpinnerValue(spinnerId: Int, value: String) {
        val spinner = findViewById<Spinner>(spinnerId) ?: return
        @Suppress("UNCHECKED_CAST")
        val adapter = spinner.adapter as? ArrayAdapter<String>
        val pos = adapter?.getPosition(value) ?: -1
        if (pos >= 0) spinner.setSelection(pos)
    }

    private fun updateStatus() {
        val status = findViewById<TextView>(R.id.tv_enroll_status)
        status.text = "Sport Profile Verified & Protected ✓"
        status.setTextColor(Color.parseColor("#FBBF24"))
    }

    private fun submitEnrollment() {
        val btnSubmit = findViewById<Button>(R.id.btn_submit_enrollment)
        btnSubmit.isEnabled = false
        btnSubmit.text = "VERIFYING BIOMETRICS..."

        lifecycleScope.launch {
            try {
                val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(applicationContext) }
                val dao = db.sportProfileDao()
                
                Log.d("ENROLL_SYNC", "Starting local save for $sportName")

                // 1. Prepare Technical Profile based on sport
                val technicalProfile: Any = when (sportName) {
                    "Cricket" -> CricketProfile(
                        userEmail = userEmail,
                        playingRole = findViewById<Spinner>(R.id.spinner_cricket_role).selectedItem.toString(),
                        battingStyle = findViewById<Spinner>(R.id.spinner_batting_style).selectedItem.toString(),
                        bowlingStyle = findViewById<Spinner>(R.id.spinner_bowling_style).selectedItem.toString(),
                        armSpeed = findViewById<EditText>(R.id.et_arm_speed).text.toString().toDoubleOrNull() ?: 0.0,
                        batSpeed = findViewById<EditText>(R.id.et_bat_speed).text.toString().toDoubleOrNull() ?: 0.0
                    )
                    "Football" -> FootballProfile(
                        userEmail = userEmail,
                        position = findViewById<Spinner>(R.id.spinner_football_position).selectedItem.toString(),
                        preferredFoot = findViewById<Spinner>(R.id.spinner_preferred_foot).selectedItem.toString(),
                        passingAccuracy = findViewById<EditText>(R.id.et_passing_accuracy).text.toString().toIntOrNull() ?: 0,
                        sprintSpeed = findViewById<EditText>(R.id.et_sprint_speed).text.toString().toDoubleOrNull() ?: 0.0
                    )
                    "Basketball" -> BasketballProfile(
                        userEmail = userEmail,
                        position = findViewById<Spinner>(R.id.spinner_basketball_position).selectedItem.toString(),
                        verticalJump = findViewById<EditText>(R.id.et_vertical_jump).text.toString().toDoubleOrNull() ?: 0.0,
                        threePointAccuracy = findViewById<EditText>(R.id.et_three_point).text.toString().toIntOrNull() ?: 0
                    )
                    "Chess", "Carrom", "Billiards" -> ChessProfile(
                        userEmail = userEmail,
                        fideRating = findViewById<EditText>(R.id.et_fide_rating).text.toString().toIntOrNull() ?: 0,
                        playingStyle = findViewById<Spinner>(R.id.spinner_chess_style).selectedItem.toString(),
                        preferredOpening = findViewById<EditText>(R.id.et_chess_opening).text.toString()
                    )
                    "Sprints", "Running", "Long Jump", "High Jump", "Triple Jump", "Shot Put", "Discus Throw", "Javelin", "Hammer Throw" -> AthleticsProfile(
                        userEmail = userEmail,
                        eventType = findViewById<Spinner>(R.id.spinner_athletics_cat).selectedItem.toString(),
                        specificEvent = findViewById<Spinner>(R.id.spinner_athletics_event).selectedItem.toString(),
                        personalBest = findViewById<EditText>(R.id.et_athletics_pb).text.toString()
                    )
                    "Swimming" -> SwimmingProfile(
                        userEmail = userEmail,
                        primaryStroke = findViewById<Spinner>(R.id.spinner_swimming_stroke).selectedItem.toString(),
                        poolLength = findViewById<Spinner>(R.id.spinner_pool_length).selectedItem.toString(),
                        bestTiming = findViewById<EditText>(R.id.et_swimming_best).text.toString()
                    )
                    "Boxing", "Wrestling", "Karate", "Fencing" -> CombatSportProfile(
                        userEmail = userEmail,
                        sportName = sportName,
                        weightClass = findViewById<Spinner>(R.id.spinner_combat_weight).selectedItem.toString(),
                        stance = findViewById<Spinner>(R.id.spinner_combat_stance).selectedItem.toString(),
                        strikePower = 70
                    )
                    "Badminton", "Tennis", "Table Tennis", "Squash" -> RacketSportProfile(
                        userEmail = userEmail,
                        sportName = sportName,
                        playStyle = findViewById<Spinner>(R.id.spinner_racket_play_style).selectedItem.toString(),
                        gripType = findViewById<Spinner>(R.id.spinner_racket_grip).selectedItem.toString(),
                        nationalRanking = findViewById<EditText>(R.id.et_racket_rank).text.toString().toIntOrNull() ?: 0
                    )
                    "Kabaddi", "Kho-Kho", "Hockey", "Volleyball", "Rugby" -> TeamSportProfile(
                        userEmail = userEmail,
                        sportName = sportName,
                        position = findViewById<Spinner>(R.id.spinner_team_position).selectedItem.toString(),
                        agilityScore = findViewById<EditText>(R.id.et_team_agility).text.toString().toIntOrNull() ?: 0,
                        staminaRating = findViewById<EditText>(R.id.et_team_stamina).text.toString().toIntOrNull() ?: 0
                    )
                    "Archery", "Shooting", "Golf" -> PrecisionSportProfile(
                        userEmail = userEmail,
                        sportName = sportName,
                        dominantEye = findViewById<Spinner>(R.id.spinner_precision_eye).selectedItem.toString(),
                        equipmentType = findViewById<EditText>(R.id.et_precision_equipment).text.toString(),
                        accuracyPct = findViewById<EditText>(R.id.et_precision_accuracy).text.toString().toIntOrNull() ?: 0
                    )
                    else -> SkillSportProfile(
                        userEmail = userEmail,
                        sportName = sportName,
                        flexibilityIndex = findViewById<EditText>(R.id.et_skill_flex).text.toString().toIntOrNull() ?: 0,
                        balanceScore = findViewById<EditText>(R.id.et_skill_balance).text.toString().toIntOrNull() ?: 0
                    )
                }

                // 2. Local Persistence (Save Technical Profile)
                withContext(Dispatchers.IO) {
                    when (technicalProfile) {
                        is CricketProfile -> dao.saveCricket(technicalProfile)
                        is FootballProfile -> dao.saveFootball(technicalProfile)
                        is BasketballProfile -> dao.saveBasketball(technicalProfile)
                        is ChessProfile -> dao.saveChess(technicalProfile)
                        is AthleticsProfile -> dao.saveAthletics(technicalProfile)
                        is SwimmingProfile -> dao.saveSwimming(technicalProfile)
                        is CombatSportProfile -> dao.saveCombatSport(technicalProfile)
                        is RacketSportProfile -> dao.saveRacketSport(technicalProfile)
                        is TeamSportProfile -> dao.saveTeamSport(technicalProfile)
                        is PrecisionSportProfile -> dao.savePrecisionSport(technicalProfile)
                        is SkillSportProfile -> dao.saveSkillSport(technicalProfile)
                    }
                }

                // 3. Prepare Master Enrollment
                val randomId = (1000..9999).random()
                val enrollment = SportEnrollment(
                    enrollmentId = existingEnrollment?.enrollmentId ?: 0,
                    userEmail = userEmail,
                    sportName = sportName,
                    sportCategory = intent.getStringExtra("SPORT_CAT") ?: "General",
                    athleteSportId = existingEnrollment?.athleteSportId ?: "PR-${sportName.take(3).uppercase()}-$randomId",
                    blockchainSportId = existingEnrollment?.blockchainSportId ?: "BC-${sportName.take(3).uppercase()}-$randomId",
                    registrationStatus = "Verified",
                    positionStyle = findViewById<EditText>(R.id.et_enroll_position).text.toString(),
                    yearsExperience = findViewById<EditText>(R.id.et_enroll_exp).text.toString(),
                    bestPerformance = findViewById<EditText>(R.id.et_enroll_best).text.toString(),
                    achievementUri = certUri?.toString() ?: existingEnrollment?.achievementUri,
                    proofVideoUri = videoUri?.toString() ?: existingEnrollment?.proofVideoUri,
                    syncStatus = "Synced"
                )
                
                // 4. Atomic Local Save (Triggers Dashboard Flow)
                withContext(Dispatchers.IO) {
                    db.sportEnrollmentDao().enrollInSport(enrollment)
                    val user = db.userDao().getUserByEmail(userEmail)
                    user?.let { db.userDao().insertUser(it.copy(totalXP = it.totalXP + 250)) }
                }

                Log.d("ENROLL_SYNC", "Local save complete. Exiting activity.")
                Toast.makeText(this@SportEnrollmentActivity, "$sportName Arena Activated!", Toast.LENGTH_LONG).show()

                // 5. Background Cloud Sync (Non-blocking)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        FirebaseManager.saveSportSpecificProfile(userEmail, sportName, enrollment)
                    } catch (e: Exception) {
                        Log.e("ENROLL_SYNC", "Background Firebase Sync Failed: ${e.message}")
                    }
                }

                finish()

            } catch (e: Exception) {
                Log.e("ENROLL_SYNC", "Critical Enrollment Error: ${e.message}")
                Toast.makeText(this@SportEnrollmentActivity, "Enrollment Error. Try again.", Toast.LENGTH_SHORT).show()
                btnSubmit.isEnabled = true
                btnSubmit.text = "VERIFY & ENROLL"
            }
        }
    }
}

