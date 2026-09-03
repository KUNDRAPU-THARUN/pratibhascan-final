package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class AuthActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var networkMonitor: NetworkMonitor
    private var isOnline = true

    private var isLoginMode = true
    private var selectedRole = "Athlete"
    private var selectedGender = "Male"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        try {
            setContentView(R.layout.activity_auth)
            setupEdgeToEdge(findViewById(R.id.auth_main))

            networkMonitor = NetworkMonitor(this)
            lifecycleScope.launch {
                networkMonitor.isOnline.collect { online ->
                    isOnline = online
                }
            }

            val etEmail = findViewById<EditText>(R.id.et_email)
            val etPassword = findViewById<EditText>(R.id.et_password)
            val etFullName = findViewById<EditText>(R.id.et_full_name)
            val btnMainAction = findViewById<MaterialButton>(R.id.btn_main_action)

            // ... (rest of findViewByIds remain the same)

            
            val btnToggleLogin = findViewById<MaterialButton>(R.id.btn_toggle_login)
            val btnToggleSignup = findViewById<MaterialButton>(R.id.btn_toggle_signup)
            
            val btnRoleAthlete = findViewById<MaterialButton>(R.id.btn_role_athlete)
            val btnRoleAcademy = findViewById<MaterialButton>(R.id.btn_role_academy)
            val btnRoleInst = findViewById<MaterialButton>(R.id.btn_role_inst)

            val btnMale = findViewById<MaterialButton>(R.id.btn_gender_male)
            val btnFemale = findViewById<MaterialButton>(R.id.btn_gender_female)
            val btnOther = findViewById<MaterialButton>(R.id.btn_gender_other)

            val llAcademyFields = findViewById<View>(R.id.ll_academy_fields)
            val llInstFields = findViewById<View>(R.id.ll_inst_fields)
            val tvLabelAddress = findViewById<View>(R.id.til_address_auth)
            val etAddressAuth = findViewById<EditText>(R.id.et_address_auth)
            val btnForgotPassword = findViewById<View>(R.id.btn_forgot_password)

            fun updateUI() {
                val blue = ColorStateList.valueOf(getColor(R.color.brand_blue))
                val transparent = ColorStateList.valueOf(0x00000000)

                if (isLoginMode) {
                    btnToggleLogin?.backgroundTintList = blue
                    btnToggleSignup?.backgroundTintList = transparent
                    
                    findViewById<View>(R.id.tv_label_role)?.visibility = View.GONE
                    findViewById<View>(R.id.ll_toggle_role)?.visibility = View.GONE
                    findViewById<View>(R.id.til_full_name)?.visibility = View.GONE
                    etFullName?.visibility = View.GONE
                    findViewById<View>(R.id.ll_age_location_labels)?.visibility = View.GONE
                    findViewById<View>(R.id.ll_age_location_inputs)?.visibility = View.GONE
                    findViewById<View>(R.id.tv_label_gender)?.visibility = View.GONE
                    findViewById<View>(R.id.ll_toggle_gender)?.visibility = View.GONE
                    findViewById<View>(R.id.tv_label_bio)?.visibility = View.GONE
                    findViewById<View>(R.id.et_bio)?.visibility = View.GONE
                    
                    llAcademyFields?.visibility = View.GONE
                    llInstFields?.visibility = View.GONE
                    tvLabelAddress?.visibility = View.GONE
                    etAddressAuth?.visibility = View.GONE
                    
                    btnForgotPassword?.visibility = View.VISIBLE
                    btnMainAction?.text = getString(R.string.login)
                } else {
                    btnToggleSignup?.backgroundTintList = blue
                    btnToggleLogin?.backgroundTintList = transparent
                    
                    findViewById<View>(R.id.tv_label_role)?.visibility = View.VISIBLE
                    findViewById<View>(R.id.ll_toggle_role)?.visibility = View.VISIBLE
                    
                    btnForgotPassword?.visibility = View.GONE
                    updateRoleUI(
                        when(selectedRole) {
                            "Athlete" -> btnRoleAthlete
                            "Academy" -> btnRoleAcademy
                            else -> btnRoleInst
                        },
                        when(selectedRole) {
                            "Athlete" -> btnRoleAcademy
                            "Academy" -> btnRoleAthlete
                            else -> btnRoleAthlete
                        },
                        when(selectedRole) {
                            "Athlete" -> btnRoleInst
                            "Academy" -> btnRoleInst
                            else -> btnRoleAcademy
                        }
                    )

                    when(selectedRole) {
                        "Athlete" -> {
                            findViewById<View>(R.id.til_full_name)?.visibility = View.VISIBLE
                            etFullName?.visibility = View.VISIBLE
                            findViewById<View>(R.id.ll_age_location_labels)?.visibility = View.VISIBLE
                            findViewById<View>(R.id.ll_age_location_inputs)?.visibility = View.VISIBLE
                            findViewById<View>(R.id.tv_label_gender)?.visibility = View.VISIBLE
                            findViewById<View>(R.id.ll_toggle_gender)?.visibility = View.VISIBLE
                            findViewById<View>(R.id.tv_label_bio)?.visibility = View.VISIBLE
                            findViewById<View>(R.id.et_bio)?.visibility = View.VISIBLE
                            llAcademyFields?.visibility = View.GONE
                            llInstFields?.visibility = View.GONE
                            tvLabelAddress?.visibility = View.GONE
                            etAddressAuth?.visibility = View.GONE
                        }
                        "Academy" -> {
                            findViewById<View>(R.id.til_full_name)?.visibility = View.GONE
                            etFullName?.visibility = View.GONE
                            findViewById<View>(R.id.ll_age_location_labels)?.visibility = View.GONE
                            findViewById<View>(R.id.ll_age_location_inputs)?.visibility = View.GONE
                            findViewById<View>(R.id.tv_label_gender)?.visibility = View.GONE
                            findViewById<View>(R.id.ll_toggle_gender)?.visibility = View.GONE
                            findViewById<View>(R.id.tv_label_bio)?.visibility = View.GONE
                            findViewById<View>(R.id.et_bio)?.visibility = View.GONE
                            llAcademyFields?.visibility = View.VISIBLE
                            llInstFields?.visibility = View.GONE
                            tvLabelAddress?.visibility = View.VISIBLE
                            etAddressAuth?.visibility = View.VISIBLE
                        }
                        "Institution" -> {
                            findViewById<View>(R.id.til_full_name)?.visibility = View.GONE
                            etFullName?.visibility = View.GONE
                            findViewById<View>(R.id.ll_age_location_labels)?.visibility = View.GONE
                            findViewById<View>(R.id.ll_age_location_inputs)?.visibility = View.GONE
                            findViewById<View>(R.id.tv_label_gender)?.visibility = View.GONE
                            findViewById<View>(R.id.ll_toggle_gender)?.visibility = View.GONE
                            findViewById<View>(R.id.tv_label_bio)?.visibility = View.GONE
                            findViewById<View>(R.id.et_bio)?.visibility = View.GONE
                            llAcademyFields?.visibility = View.GONE
                            llInstFields?.visibility = View.VISIBLE
                            tvLabelAddress?.visibility = View.VISIBLE
                            etAddressAuth?.visibility = View.VISIBLE
                        }
                    }
                    btnMainAction?.text = getString(R.string.signup)
                }
            }

            btnToggleLogin?.setSafeOnClickListener { isLoginMode = true; updateUI() }
            btnToggleSignup?.setSafeOnClickListener { isLoginMode = false; updateUI() }

            btnForgotPassword?.setOnClickListener {
                val emailInput = etEmail?.text.toString().trim().lowercase()
                if (emailInput.isEmpty()) {
                    Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
                } else {
                    FirebaseManager.getFirebaseAuth().sendPasswordResetEmail(emailInput)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Reset link sent to $emailInput", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }

            btnRoleAthlete?.setSafeOnClickListener { selectedRole = "Athlete"; updateRoleUI(btnRoleAthlete, btnRoleAcademy, btnRoleInst); updateUI() }
            btnRoleAcademy?.setSafeOnClickListener { selectedRole = "Academy"; updateRoleUI(btnRoleAcademy, btnRoleAthlete, btnRoleInst); updateUI() }
            btnRoleInst?.setSafeOnClickListener { selectedRole = "Institution"; updateRoleUI(btnRoleInst, btnRoleAthlete, btnRoleAcademy); updateUI() }

            btnMale?.setSafeOnClickListener { selectedGender = "Male"; updateGenderUI(btnMale, btnFemale, btnOther) }
            btnFemale?.setSafeOnClickListener { selectedGender = "Female"; updateGenderUI(btnFemale, btnMale, btnOther) }
            btnOther?.setSafeOnClickListener { selectedGender = "Other"; updateGenderUI(btnOther, btnMale, btnFemale) }

            btnMainAction?.setSafeOnClickListener {
                val emailInput = etEmail?.text.toString().trim().lowercase()
                val password = etPassword?.text.toString().trim()

                if (emailInput.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, R.string.error_email_password_required, Toast.LENGTH_SHORT).show()
                    return@setSafeOnClickListener
                }

                if (!isOnline) {
                    Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show()
                    return@setSafeOnClickListener
                }

                btnMainAction.isEnabled = false
                lifecycleScope.launch {
                    try {
                        if (isLoginMode) {
                            // 1. Firebase Auth Sign In
                            try {
                                FirebaseManager.getFirebaseAuth().signInWithEmailAndPassword(emailInput, password).await()
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                when {
                                    e is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                                        Toast.makeText(this@AuthActivity, "No account found with this email. Try signing up!", Toast.LENGTH_LONG).show()
                                    }
                                    e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                                        Toast.makeText(this@AuthActivity, "Incorrect password. Please try again or use 'Forgot Password'.", Toast.LENGTH_LONG).show()
                                    }
                                    msg.contains("malformed", ignoreCase = true) || msg.contains("expired", ignoreCase = true) -> {
                                        Toast.makeText(this@AuthActivity, "Auth session expired. Please restart the app.", Toast.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        Toast.makeText(this@AuthActivity, "Login Error: $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                btnMainAction.isEnabled = true
                                return@launch
                            }

                            val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@AuthActivity) }
                            var user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(emailInput) }
                            
                            // If user not in Room, try robust fetching from Firestore
                            if (user == null) {
                                user = com.example.prathibhascanfinal.data.repository.FirestoreRepository().findUserByEmail(emailInput)
                                if (user != null) {
                                    withContext(Dispatchers.IO) { db.userDao().insertUser(user!!) }
                                }
                            }

                            if (user != null) {
                                SessionManager(this@AuthActivity).saveSession(
                                    user.email, 
                                    user.fullName, 
                                    user.uniqueId, 
                                    user.role,
                                    user.primaryDiscipline,
                                )
                                navigateToDashboard(user)
                            } else {
                                // Firebase Auth worked but No Profile in Firestore!
                                // Auto-redirect to setup instead of failing
                                Toast.makeText(this@AuthActivity, "Account found. Please complete your profile.", Toast.LENGTH_LONG).show()
                                val intent = Intent(this@AuthActivity, ProfileSetupActivity::class.java)
                                intent.putExtra("USER_EMAIL", emailInput)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            // Signup Mode
                            val name = when(selectedRole) {
                                "Athlete" -> etFullName?.text.toString().trim()
                                "Academy" -> findViewById<EditText>(R.id.et_academy_name_auth)?.text.toString().trim()
                                else -> findViewById<EditText>(R.id.et_inst_name_auth)?.text.toString().trim()
                            }
                            if (name.isEmpty()) {
                                Toast.makeText(this@AuthActivity, R.string.error_entity_name_required, Toast.LENGTH_SHORT).show()
                                btnMainAction.isEnabled = true
                                return@launch
                            }
                            
                            // 1. Firebase Auth Create User
                            try {
                                FirebaseManager.getFirebaseAuth().createUserWithEmailAndPassword(emailInput, password).await()
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException || msg.contains("already in use", ignoreCase = true)) {
                                    Toast.makeText(this@AuthActivity, "Account already exists! Switching to Login mode...", Toast.LENGTH_LONG).show()
                                    isLoginMode = true
                                    updateUI()
                                    btnMainAction.isEnabled = true
                                    return@launch
                                }
                                Log.w("AUTH_FIREBASE", "Firebase Signup failed: ${e.message}")
                            }

                            val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@AuthActivity) }
                            val prefix = when(selectedRole) {
                                "Athlete" -> "ATH"
                                "Academy" -> "ACD"
                                else -> "INS"
                            }
                            val uid = "PR-$prefix-${(100000000..999999999).random()}"
                            val newUser = User(
                                email = emailInput, 
                                password = password, 
                                role = selectedRole, 
                                fullName = name, 
                                uniqueId = uid, 
                                gender = selectedGender,
                                location = if(selectedRole == "Athlete") findViewById<EditText>(R.id.et_location)?.text.toString().trim() else findViewById<EditText>(R.id.et_address_auth)?.text.toString().trim(),
                                parentMobile = if(selectedRole == "Academy") findViewById<EditText>(R.id.et_academy_phone_auth)?.text.toString().trim() else null,
                                specificGames = if(selectedRole == "Institution") findViewById<EditText>(R.id.et_inst_board_auth)?.text.toString().trim() else null,
                                bio = findViewById<EditText>(R.id.et_bio)?.text.toString().trim(),
                                age = findViewById<EditText>(R.id.et_age)?.text.toString().trim()
                            )
                            
                            // 2. Save to Room & Firestore
                            withContext(Dispatchers.IO) { db.userDao().insertUser(newUser) }
                            try {
                                FirebaseManager.getFirebaseFirestore().collection("users").document(emailInput).set(newUser).await()
                            } catch (e: Exception) {
                                Log.e("AUTH_FIRESTORE", "Save failed", e)
                            }
                            
                            SessionManager(this@AuthActivity).saveSession(emailInput, name, uid, selectedRole, null)
                            
                            Toast.makeText(this@AuthActivity, getString(R.string.toast_signed_up_as, selectedRole), Toast.LENGTH_LONG).show()
                            val intent = Intent(this@AuthActivity, ProfileSetupActivity::class.java)
                            intent.putExtra("USER_EMAIL", emailInput)
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        val msg = e.message ?: e.localizedMessage ?: ""
                        if (e is kotlinx.coroutines.CancellationException || 
                            msg.contains("Job was cancelled", true) ||
                            msg.contains("cancelled", true)) {
                            // Silently ignore coroutine cancellations - they are common during navigation
                            return@launch
                        }
                        Log.e("AUTH_ERROR", "Action failed", e)
                        Toast.makeText(this@AuthActivity, "Error: $msg", Toast.LENGTH_LONG).show()
                        btnMainAction.isEnabled = true
                    }
                }
            }

            findViewById<TextView>(R.id.btn_back_home)?.setOnClickListener { finish() }
            updateUI()

            } catch (_: Exception) {
                finish()
            }
    }

    private fun navigateToDashboard(user: User) {
        val intent = when (user.role) {
            "Athlete" -> Intent(this, DashboardActivity::class.java).apply {
                putExtra("USER_SPORT", user.primaryDiscipline)
            }
            "Academy" -> Intent(this, AcademyPortalActivity::class.java)
            "Institution" -> Intent(this, InstitutionPortalActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java).apply {
                putExtra("USER_SPORT", user.primaryDiscipline)
            }
        }
        
        intent.putExtra("USER_EMAIL", user.email)
        intent.putExtra("USER_NAME", user.fullName)
        intent.putExtra("UNIQUE_ID", user.uniqueId)
        startActivity(intent)
        finish()
    }

    private fun updateRoleUI(s: MaterialButton?, u1: MaterialButton?, u2: MaterialButton?) {
        val blue = ColorStateList.valueOf(getColor(R.color.brand_blue))
        val transparent = ColorStateList.valueOf(0x00000000)
        val white = 0xFFFFFFFF.toInt()
        val secondary = 0xFF94A3B8.toInt()

        s?.backgroundTintList = blue
        s?.setTextColor(white)
        u1?.backgroundTintList = transparent
        u1?.setTextColor(secondary)
        u2?.backgroundTintList = transparent
        u2?.setTextColor(secondary)
    }

    private fun updateGenderUI(s: MaterialButton?, u1: MaterialButton?, u2: MaterialButton?) {
        val blue = ColorStateList.valueOf(getColor(R.color.brand_blue))
        val transparent = ColorStateList.valueOf(0x00000000)
        val white = 0xFFFFFFFF.toInt()
        val secondary = 0xFF94A3B8.toInt()

        s?.backgroundTintList = blue
        s?.setTextColor(white)
        u1?.backgroundTintList = transparent
        u1?.setTextColor(secondary)
        u2?.backgroundTintList = transparent
        u2?.setTextColor(secondary)
    }
}

