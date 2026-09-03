package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class ProfileSetupActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private var currentStep = 1
    private var totalSteps = 6
    private lateinit var userEmail: String
    private var userRole = "Athlete"
    
    private lateinit var stepLayouts: MutableList<View>
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile_setup)
            findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

            // Normalize email immediately
            userEmail = (intent.getStringExtra("USER_EMAIL") ?: "").lowercase().trim()
            
            // EMERGENCY RESCUE: If email is lost during navigation, recover from Session or Firebase
            if (userEmail.isEmpty()) {
                val sessionEmail = SessionManager(this@ProfileSetupActivity).getEmail()
                val firebaseEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                userEmail = (sessionEmail ?: firebaseEmail ?: "").lowercase().trim()
            }

            if (userEmail.isEmpty()) {
                Log.e("SETUP_ERROR", "User identity completely lost")
                Toast.makeText(this, "Session identity lost. Please login again.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                return
            }
            
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@ProfileSetupActivity)
                var user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                
                // If not in Room, try fetching from Firestore as a last resort
                if (user == null) {
                    user = withContext(Dispatchers.IO) { com.example.prathibhascanfinal.data.repository.FirestoreRepository().findUserByEmail(userEmail) }
                }

                if (user == null) {
                    // Create a skeleton if they are authenticated to prevent getting stuck
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && ((firebaseUser.email ?: "").lowercase().trim() == userEmail)) {
                        val skeletonUser = User(email = userEmail, role = "Athlete", fullName = "New User")
                        withContext(Dispatchers.IO) { db.userDao().insertUser(skeletonUser) }
                        user = skeletonUser
                    }
                }

                userRole = user?.role ?: "Athlete"
                
                initViews(user)
                setupNavigation()
                updateStepUI()
            }
        } catch (e: Exception) {
            Log.e("SETUP_CRASH", "onCreate failed", e)
            finish()
        }
    }

    private fun initViews(user: User?) {
        stepLayouts = mutableListOf(
            findViewById(R.id.layout_step1),
            findViewById(R.id.layout_step2),
        )

        // Pre-fill fields from user object
        user?.let { u ->
            findViewById<EditText>(R.id.et_full_name_setup)?.setText(u.fullName)
            findViewById<EditText>(R.id.et_school_enrollment)?.setText(u.schoolEnrollmentId)
            findViewById<EditText>(R.id.et_guardian_phone)?.setText(u.parentMobile)
            findViewById<EditText>(R.id.et_wingspan_setup)?.setText(u.wingSpan)
            findViewById<EditText>(R.id.et_seating_setup)?.setText(u.seatingHeight)
            findViewById<EditText>(R.id.et_specific_games_setup)?.setText(u.specificGames)
            findViewById<EditText>(R.id.et_bio_setup)?.setText(u.bio)
            findViewById<EditText>(R.id.et_location_setup)?.setText(u.location)
            findViewById<EditText>(R.id.et_age_setup)?.setText(u.age)
            
            // Academy/Inst specific
            findViewById<EditText>(R.id.et_academy_infra)?.setText(u.achievements)
            findViewById<EditText>(R.id.et_academy_domains)?.setText(u.primaryDiscipline)
            findViewById<EditText>(R.id.et_inst_board_edit)?.setText(u.specificGames)
            findViewById<EditText>(R.id.et_inst_pe_teacher_edit)?.setText(u.certificates)

            if (u.dominantSide == "Left") {
                findViewById<View>(R.id.btn_setup_side_left)?.performClick()
            }
        }

        // Conditional steps based on role
        when (userRole) {
            "Athlete" -> {
                stepLayouts.add(findViewById(R.id.layout_step3))
                stepLayouts.add(findViewById(R.id.layout_step4))
                stepLayouts.add(findViewById(R.id.layout_step5))
                stepLayouts.add(findViewById(R.id.layout_step6))
                totalSteps = 6
            }
            "Academy" -> {
                stepLayouts.add(findViewById(R.id.layout_step_academy))
                totalSteps = 3
            }
            "Institution" -> {
                stepLayouts.add(findViewById(R.id.layout_step_institution))
                totalSteps = 3
            }
        }
        
        tvTitle = findViewById(R.id.tv_setup_step_title)
        progressBar = findViewById(R.id.setup_progress_bar)
        btnPrev = findViewById(R.id.btn_setup_prev)
        btnNext = findViewById(R.id.btn_setup_next)

        // Spinners
        val spinnerDomain = findViewById<Spinner>(R.id.spinner_primary_domain_setup)
        val domains = com.example.prathibhascanfinal.data.SportData.getAllSportNames()
        spinnerDomain?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, domains)

        val spinnerState = findViewById<Spinner>(R.id.spinner_state)
        val states = arrayOf("Andhra Pradesh", "Delhi", "Maharashtra", "Tamil Nadu", "Rajasthan")
        spinnerState?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, states)
        
        findViewById<View>(R.id.btn_setup_side_right)?.setOnClickListener {
            findViewById<View>(R.id.btn_setup_side_right).setBackgroundResource(R.drawable.bg_button_blue)
            findViewById<View>(R.id.btn_setup_side_left).setBackgroundResource(0)
        }
        findViewById<View>(R.id.btn_setup_side_left)?.setOnClickListener {
            findViewById<View>(R.id.btn_setup_side_left).setBackgroundResource(R.drawable.bg_button_blue)
            findViewById<View>(R.id.btn_setup_side_right).setBackgroundResource(0)
        }
    }

    private fun setupNavigation() {
        btnNext.setSafeOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                updateStepUI()
            } else {
                btnNext.isEnabled = false
                saveProfile()
            }
        }

        btnPrev.setSafeOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI()
            }
        }
    }

    private fun updateStepUI() {
        if (!::stepLayouts.isInitialized) return
        
        // Hide all steps first
        listOf(
            R.id.layout_step1, R.id.layout_step2, R.id.layout_step3, R.id.layout_step4, 
            R.id.layout_step5, R.id.layout_step6, R.id.layout_step_academy, R.id.layout_step_institution
        ).forEach { findViewById<View>(it)?.isVisible = false }

        // Show current step
        stepLayouts[currentStep - 1].isVisible = true
        
        btnPrev.visibility = if (currentStep > 1) View.VISIBLE else View.INVISIBLE
        btnNext.text = if (currentStep == totalSteps) "Complete Passport" else "Next Step"
        
        progressBar.progress = (currentStep * 100) / totalSteps
        
        tvTitle.text = if (userRole == "Athlete") {
            when(currentStep) {
                1 -> "Step 1: Identity & Integrity"
                2 -> "Step 2: Regional Mapping"
                3 -> "Step 3: Biomechanics HUD"
                4 -> "Step 4: Skill Preference"
                5 -> "Step 5: Achievement Vault"
                else -> "Step 6: AI Video Sandbox"
            }
        } else {
            when(currentStep) {
                1 -> "Step 1: Entity Identity"
                2 -> "Step 2: Regional Mapping"
                else -> if (userRole == "Academy") "Step 3: Infrastructure Setup" else "Step 3: Institutional Governance"
            }
        }
    }

    private fun saveProfile() {
        val email = userEmail.lowercase().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Session identity lost. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@ProfileSetupActivity)
                var user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(email) }
                
                // 1. Double check cloud if local is missing
                if (user == null) {
                    user = withContext(Dispatchers.IO) { com.example.prathibhascanfinal.data.repository.FirestoreRepository().findUserByEmail(email) }
                }

                // 2. Still null? Create a base record if authenticated
                if (user == null) {
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && (firebaseUser.email ?: "").lowercase().trim() == email) {
                        val prefix = if (userRole == "Academy") "ACD" else if (userRole == "Institution") "INS" else "ATH"
                        val uid = "PR-$prefix-${(100000000..999999999).random()}"
                        val skeleton = User(email = email, role = userRole, fullName = "User", uniqueId = uid)
                        withContext(Dispatchers.IO) { db.userDao().insertUser(skeleton) }
                        user = skeleton
                    }
                }

                if (user != null) {
                    // Ensure uniqueId is present
                    if (user.uniqueId.isEmpty()) {
                        val prefix = if (userRole == "Academy") "ACD" else if (userRole == "Institution") "INS" else "ATH"
                        user = user.copy(uniqueId = "PR-$prefix-${(100000000..999999999).random()}")
                    }

                    val setupName = findViewById<EditText>(R.id.et_full_name_setup)?.text?.toString()?.trim()
                        ?.takeIf { it.isNotEmpty() } ?: user.fullName
                    
                    val updatedUser = when (userRole) {
                        "Athlete" -> user.copy(
                            fullName = setupName,
                            schoolEnrollmentId = findViewById<EditText>(R.id.et_school_enrollment)?.text?.toString(),
                            parentMobile = findViewById<EditText>(R.id.et_guardian_phone)?.text?.toString(),
                            wingSpan = findViewById<EditText>(R.id.et_wingspan_setup)?.text?.toString(),
                            seatingHeight = findViewById<EditText>(R.id.et_seating_setup)?.text?.toString(),
                            primaryDiscipline = findViewById<Spinner>(R.id.spinner_primary_domain_setup)?.selectedItem?.toString(),
                            state = findViewById<Spinner>(R.id.spinner_state)?.selectedItem?.toString(),
                            bio = findViewById<EditText>(R.id.et_bio_setup)?.text?.toString(),
                            location = findViewById<EditText>(R.id.et_location_setup)?.text?.toString(),
                            age = findViewById<EditText>(R.id.et_age_setup)?.text?.toString(),
                            specificGames = findViewById<EditText>(R.id.et_specific_games_setup)?.text?.toString()
                        )
                        "Academy" -> {
                            val bio = findViewById<EditText>(R.id.et_bio_setup)?.text?.toString()
                            val infra = findViewById<EditText>(R.id.et_academy_infra)?.text?.toString()
                            val domains = findViewById<EditText>(R.id.et_academy_domains)?.text?.toString()
                            val loc = findViewById<EditText>(R.id.et_location_setup)?.text?.toString()
                            val st = findViewById<Spinner>(R.id.spinner_state)?.selectedItem?.toString()
                            
                            // Create Academy Record
                            val academy = Academy(
                                academyName = setupName,
                                contactEmail = email,
                                description = bio ?: "",
                                infrastructureTier = infra ?: "",
                                specializedDomains = domains ?: "",
                                city = loc ?: "",
                                state = st ?: "",
                                directorName = setupName // Default for now
                            )
                            lifecycleScope.launch(Dispatchers.IO) {
                                com.example.prathibhascanfinal.data.repository.AcademyRepository(this@ProfileSetupActivity).updateAcademy(academy)
                            }
                            
                            user.copy(
                                fullName = setupName,
                                achievements = infra,
                                primaryDiscipline = domains,
                                state = st,
                                location = loc,
                                bio = bio
                            )
                        }
                        else -> { // Institution
                            val bio = findViewById<EditText>(R.id.et_bio_setup)?.text?.toString()
                            val board = findViewById<EditText>(R.id.et_inst_board_edit)?.text?.toString()
                            val teacher = findViewById<EditText>(R.id.et_inst_pe_teacher_edit)?.text?.toString()
                            val loc = findViewById<EditText>(R.id.et_location_setup)?.text?.toString()
                            val st = findViewById<Spinner>(R.id.spinner_state)?.selectedItem?.toString()

                            // Create Institution Record
                            val institution = Institution(
                                institutionName = setupName,
                                contactEmail = email,
                                boardAffiliation = board ?: "",
                                principalName = setupName, // Default for now
                                campusAddress = loc ?: "",
                                state = st ?: ""
                            )
                            lifecycleScope.launch(Dispatchers.IO) {
                                com.example.prathibhascanfinal.data.repository.InstitutionRepository(this@ProfileSetupActivity).updateInstitution(institution)
                            }

                            user.copy(
                                fullName = setupName,
                                specificGames = board,
                                certificates = teacher,
                                state = st,
                                location = loc,
                                bio = bio
                            )
                        }
                    }

                    // 3. Save locally and cloud
                    withContext(Dispatchers.IO) { 
                        db.userDao().insertUser(updatedUser)
                        try {
                            FirebaseManager.getFirebaseFirestore().collection("users").document(email).set(updatedUser).await()
                        } catch (e: Exception) { Log.e("PROFILE_SYNC", "Firestore sync failed", e) }
                    }

                    // 4. Update session before dashboard opens
                    SessionManager(this@ProfileSetupActivity).saveSession(
                        updatedUser.email, 
                        updatedUser.fullName, 
                        updatedUser.uniqueId, 
                        updatedUser.role,
                        updatedUser.primaryDiscipline
                    )

                    Toast.makeText(this@ProfileSetupActivity, "Passport Verified!", Toast.LENGTH_SHORT).show()
                    
                    val normalizedRole = updatedUser.role.trim().lowercase().replaceFirstChar { it.uppercase() }
                    val intent = when (normalizedRole) {
                        "Athlete" -> Intent(this@ProfileSetupActivity, DashboardActivity::class.java).apply {
                            putExtra("USER_SPORT", updatedUser.primaryDiscipline)
                        }
                        "Academy" -> Intent(this@ProfileSetupActivity, AcademyPortalActivity::class.java)
                        "Institution" -> Intent(this@ProfileSetupActivity, InstitutionPortalActivity::class.java)
                        else -> Intent(this@ProfileSetupActivity, DashboardActivity::class.java).apply {
                            putExtra("USER_SPORT", updatedUser.primaryDiscipline)
                        }
                    }
                    
                    intent.putExtra("USER_EMAIL", email)
                    intent.putExtra("USER_NAME", updatedUser.fullName)
                    intent.putExtra("UNIQUE_ID", updatedUser.uniqueId)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ProfileSetupActivity, "Profile recovery failed. Please login again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ProfileSetupActivity, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                Log.e("SETUP_SAVE", "Final save failed", e)
                Toast.makeText(this@ProfileSetupActivity, "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                btnNext.isEnabled = true
            }
        }
    }
}

