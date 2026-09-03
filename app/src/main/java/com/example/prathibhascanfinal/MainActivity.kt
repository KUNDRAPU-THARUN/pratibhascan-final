package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class MainActivity : BaseActivity() {
    override val viewModel: DashboardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        
        // --- EMERGENCY CRASH PROTECTION ---
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRITICAL_CRASH", "Fatal error in thread $thread", throwable)
            try { SessionManager(applicationContext).logout() } catch (_: Exception) {}
        }

        try {
            setContentView(R.layout.activity_main)
            findViewById<View>(android.R.id.content)?.applySystemBarsPadding()
        } catch (e: Exception) {
            Log.e("BOOT_ERROR", "setContentView failed", e)
            val tv = TextView(this)
            tv.text = "Critical Error: Could not load layout."
            setContentView(tv)
        }


        setupClickListeners()
        checkSession()
    }

    private fun setupClickListeners() {
        val openAuth = {
            try {
                startActivity(Intent(this, AuthActivity::class.java))
            } catch (e: Exception) {
                Log.e("BOOT_ERROR", "Could not start AuthActivity", e)
            }
        }

        findViewById<View>(R.id.btn_get_started_free)?.setSafeOnClickListener { openAuth() }
        findViewById<View>(R.id.btn_login_signup_header)?.setSafeOnClickListener { openAuth() }
    }

    private fun checkSession() {
        try {
            val sessionManager = SessionManager(this)
            var firebaseUser: com.google.firebase.auth.FirebaseUser? = null
            try {
                firebaseUser = FirebaseManager.getFirebaseAuth().currentUser
            } catch (e: Exception) {
                Log.w("BOOT_ERROR", "Firebase not initialized or available", e)
            }
            
            if (sessionManager.isLoggedIn() || firebaseUser != null) {
                lifecycleScope.launch {
                    val userEmail = (firebaseUser?.email ?: sessionManager.getEmail() ?: "").lowercase().trim()
                    if (userEmail.isEmpty()) {
                        Log.d("BOOT_CHECK", "Email empty, redirect to Auth")
                        startActivity(Intent(this@MainActivity, AuthActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                        return@launch
                    }

                    // Pre-sync delay to ensure Firestore writes are settled
                    kotlinx.coroutines.delay(500)

                    val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@MainActivity) }
                    var user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                    
                    if (user == null) {
                         Log.d("BOOT_CHECK", "User not in Room, robust fetching from Firestore for $userEmail")
                         try {
                             user = com.example.prathibhascanfinal.data.repository.FirestoreRepository().findUserByEmail(userEmail)
                             user?.let { u ->
                                 Log.d("BOOT_CHECK", "Profile found in cloud, syncing to Room")
                                 withContext(Dispatchers.IO) { db.userDao().insertUser(u) }
                                 sessionManager.saveSession(u.email, u.fullName, u.uniqueId, u.role, u.primaryDiscipline)
                             }
                         } catch (e: Exception) { Log.e("BOOT_SYNC", "Sync failed", e) }
                    }

                    if (user != null) {
                        Log.d("BOOT_CHECK", "Session valid for ${user.email}, navigating to dashboard")
                        
                        val role = user.role.trim().lowercase().replaceFirstChar { it.uppercase() }

                        // Robust Sync before navigation
                        try {
                            if (role == "Academy") {
                                withContext(Dispatchers.IO) {
                                    val localAcademy = db.academyDao().getAcademyByEmail(userEmail)
                                    if (localAcademy == null) {
                                        val cloudAcademy = FirebaseManager.getAcademy(userEmail)
                                        cloudAcademy?.let { aca ->
                                            db.academyDao().insertAcademy(aca)
                                            // Pre-fetch critical data
                                            FirebaseManager.getAcademyAthletes(aca.id).forEach { 
                                                db.academyManagementDao().insertAthlete(it) 
                                            }
                                        }
                                    }
                                }
                            }

                            if (role == "Institution") {
                                withContext(Dispatchers.IO) {
                                    val localInst = db.institutionDao().getInstitutionByEmail(userEmail)
                                    if (localInst == null) {
                                        val cloudInst = FirebaseManager.getInstitution(userEmail)
                                        cloudInst?.let { inst ->
                                            db.institutionDao().insertInstitution(inst)
                                            sessionManager.setRegistrationComplete(true)
                                        }
                                    } else {
                                        sessionManager.setRegistrationComplete(true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BOOT_SYNC", "Critical sync error: ${e.message}")
                        }

                        navigateToDashboard(user)
                    } else if (firebaseUser != null) {
                        Log.d("BOOT_CHECK", "Auth session exists but profile missing, redirect to Setup")
                        val intent = Intent(this@MainActivity, ProfileSetupActivity::class.java)
                        intent.putExtra("USER_EMAIL", firebaseUser.email)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Log.d("BOOT_CHECK", "Profile still null and no auth session, redirect to Auth")
                        startActivity(Intent(this@MainActivity, AuthActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                }
            } else {
                Log.d("BOOT_CHECK", "No active session, staying on Main")
            }
        } catch (e: Exception) {
            Log.e("BOOT_ERROR", "Session check failed", e)
        }
    }

    private fun navigateToDashboard(user: User) {
        val role = user.role.trim().lowercase().replaceFirstChar { it.uppercase() }
        val intent = when (role) {
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
}

