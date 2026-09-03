package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import com.google.android.material.imageview.ShapeableImageView
import coil.load
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AcademyProfileActivity : BaseActivity() {

    override val viewModel: AcademyProfileViewModel by lazy {
        ViewModelProvider(this)[AcademyProfileViewModel::class.java]
    }

    private val dashboardViewModel: DashboardViewModel by lazy {
        ViewModelProvider(this)[DashboardViewModel::class.java]
    }

    private lateinit var session: SessionManager
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_academy_profile)
        setupEdgeToEdge(findViewById(R.id.academy_profile_root))

        session = SessionManager(this)
        userEmail = session.getEmail() ?: ""

        setupHeader()
        setupClickListeners()
        observeState()
        
        viewModel.loadProfile(userEmail)
        
        // Trigger weather update if permissions allow
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
                .lastLocation.addOnSuccessListener { loc ->
                    loc?.let { dashboardViewModel.fetchWeather(it.latitude, it.longitude) }
                }
        }
    }

    private fun observeState() {
        // Observe Dashboard State for Header Sync (Time/Weather)
        lifecycleScope.launch {
            dashboardViewModel.uiState.collectLatest { state ->
                findViewById<TextView>(R.id.tv_live_time)?.text = state.time
                state.weather?.let { w ->
                    findViewById<TextView>(R.id.tv_header_weather)?.text = getString(R.string.weather_format, w.temp.toInt(), w.condition)
                    findViewById<TextView>(R.id.tv_weather_extra)?.text = getString(R.string.aqi_label, w.aqi)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.profileState.collectLatest { state ->
                val aca = state.academy
                val user = state.userFallback

                if (aca != null) {
                    findViewById<TextView>(R.id.tv_profile_academy_name)?.text = aca.academyName
                    findViewById<TextView>(R.id.tv_detail_reg_no)?.text = getString(R.string.reg_no_format, aca.registrationNumber.ifEmpty { "Not Provided" })
                    findViewById<TextView>(R.id.tv_detail_owner)?.text = getString(R.string.director_format, aca.directorName.ifEmpty { "Not Provided" })
                    findViewById<TextView>(R.id.tv_detail_address)?.text = getString(R.string.address_format, aca.city, aca.state).ifEmpty { "Location Not Set" }
                    findViewById<TextView>(R.id.tv_detail_membership)?.text = getString(R.string.membership_format, aca.membershipPlan)
                    
                    findViewById<TextView>(R.id.tv_stat_total_athletes)?.text = state.athleteCount.toString()
                    findViewById<TextView>(R.id.tv_stat_total_coaches)?.text = state.coachCount.toString()
                    
                    findViewById<TextView>(R.id.tv_verification_status)?.text = if (aca.isVerified) "VERIFIED" else "PENDING"
                    
                    // Load Logo
                    val ivLogo = findViewById<ImageView>(R.id.iv_academy_logo_profile)
                    if (!aca.logoUri.isNullOrEmpty()) {
                        ivLogo.load(aca.logoUri) {
                            crossfade(true)
                            placeholder(R.drawable.ic_academy_emblem)
                            transformations(CircleCropTransformation())
                        }
                    }
                } else if (user != null) {
                    // Fallback to User Data
                    findViewById<TextView>(R.id.tv_profile_academy_name)?.text = user.fullName
                    findViewById<TextView>(R.id.tv_detail_reg_no)?.text = "Reg No: Pending Setup"
                    findViewById<TextView>(R.id.tv_detail_owner)?.text = "Role: ${user.role}"
                    findViewById<TextView>(R.id.tv_detail_address)?.text = user.location ?: "Address Not Provided"
                    findViewById<TextView>(R.id.tv_detail_membership)?.text = "Membership: Basic"
                    
                    findViewById<TextView>(R.id.tv_stat_total_athletes)?.text = "0"
                    findViewById<TextView>(R.id.tv_stat_total_coaches)?.text = "0"
                    findViewById<TextView>(R.id.tv_verification_status)?.text = "SETUP REQUIRED"
                }
                
                if (state.isLoading) {
                    // Show some loading indicator if needed
                }
            }
        }
    }

    private fun setupHeader() {
        val name = session.getName() ?: "Academy Central"
        findViewById<TextView>(R.id.tv_welcome_name)?.text = name
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Organization Management"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }

        findViewById<View>(R.id.btn_vernacular_toggle)?.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btn_edit_profile_main)?.setOnClickListener {
            startActivity(Intent(this, EditAcademyProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_open_analytics)?.setOnClickListener {
            startActivity(Intent(this, AcademyAnalyticsActivity::class.java))
        }

        findViewById<View>(R.id.btn_open_reports)?.setOnClickListener {
            startActivity(Intent(this, AcademyReportsActivity::class.java))
        }

        findViewById<View>(R.id.btn_open_settings)?.setOnClickListener {
            startActivity(Intent(this, AcademySettingsActivity::class.java))
        }

        findViewById<View>(R.id.iv_notification_header)?.setOnClickListener {
            startActivity(Intent(this, AcademyNotificationsActivity::class.java))
        }

        findViewById<View>(R.id.btn_logout_confirm)?.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Secure Logout")
            .setMessage("Are you sure you want to log out from the academy ecosystem?")
            .setPositiveButton("Logout") { _, _ ->
                session.logout()
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Hindi", "Telugu", "Tamil", "Kannada", "Malayalam", "Marathi", "Bengali", "Gujarati", "Punjabi", "Odia")
        val codes = arrayOf("en", "hi", "te", "ta", "kn", "ml", "mr", "bn", "gu", "pa", "or")

        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setItems(languages) { _, which ->
                LocaleHelper.setLocale(this, codes[which])
                recreate()
            }
            .show()
    }
}
