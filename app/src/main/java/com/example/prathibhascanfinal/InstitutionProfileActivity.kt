package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class InstitutionProfileActivity : BaseActivity() {

    override val viewModel: InstitutionProfileViewModel by lazy {
        ViewModelProvider(this)[InstitutionProfileViewModel::class.java]
    }

    private val dashboardViewModel: DashboardViewModel by lazy {
        ViewModelProvider(this)[DashboardViewModel::class.java]
    }

    private lateinit var session: SessionManager
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_institution_profile)
        findViewById<View>(R.id.inst_profile_root)?.applySystemBarsPadding()

        session = SessionManager(this)
        userEmail = session.getEmail() ?: ""

        setupHeader()
        setupClickListeners()
        observeState()
        
        viewModel.loadProfile(userEmail)

        // Trigger weather update
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
                .lastLocation.addOnSuccessListener { loc ->
                    loc?.let { dashboardViewModel.fetchWeather(it.latitude, it.longitude) }
                }
        }
    }

    private fun observeState() {
        // Observe Dashboard for Header Sync
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
            viewModel.uiState.collectLatest { state ->
                state.institution?.let { inst ->
                    findViewById<TextView>(R.id.tv_profile_inst_name).text = inst.institutionName
                    findViewById<TextView>(R.id.tv_profile_inst_type).text = String.format(Locale.getDefault(), "%s • %s", inst.institutionType, inst.ownershipType)
                    
                    findViewById<TextView>(R.id.tv_verification_status).text = if (inst.isVerified) "VERIFIED INSTITUTION" else "PENDING VERIFICATION"
                    
                    // Stats
                    updateStatCard(R.id.stat_total_students, state.totalStudents.toString(), "TOTAL STUDENTS")
                    updateStatCard(R.id.stat_sports_offered, state.sportsOfferedCount.toString(), "SPORTS OFFERED")
                    updateStatCard(R.id.stat_pe_teachers, state.peTeachers.toString(), "PE TEACHERS")
                    updateStatCard(R.id.stat_grounds, state.totalGrounds.toString(), "FACILITIES")

                    // Identity
                    findViewById<TextView>(R.id.tv_detail_inst_id).text = String.format(Locale.getDefault(), "INST-ID: %04d", inst.id)
                    findViewById<TextView>(R.id.tv_detail_reg_no).text = String.format(Locale.getDefault(), "Reg No: %s", inst.registrationNumber.ifEmpty { "----" })
                    findViewById<TextView>(R.id.tv_detail_est_year).text = String.format(Locale.getDefault(), "Established: %s", inst.establishedYear.ifEmpty { "----" })
                    findViewById<TextView>(R.id.tv_detail_affiliation).text = String.format(Locale.getDefault(), "Affiliation: %s", inst.boardAffiliation.ifEmpty { "----" })
                    findViewById<TextView>(R.id.tv_detail_principal).text = String.format(Locale.getDefault(), "Principal: %s", inst.principalName.ifEmpty { "----" })

                    // Contact
                    findViewById<TextView>(R.id.tv_contact_email).text = inst.contactEmail
                    findViewById<TextView>(R.id.tv_contact_phone).text = inst.officialPhone
                    findViewById<TextView>(R.id.tv_contact_website).text = inst.website.ifEmpty { "Add Website" }
                    findViewById<TextView>(R.id.tv_contact_address).text = inst.campusAddress

                    // Infra
                    findViewById<TextView>(R.id.tv_infra_status).text = String.format(Locale.getDefault(), "Status: %s", inst.recognitionStatus)
                    findViewById<TextView>(R.id.tv_infra_area).text = String.format(Locale.getDefault(), "Campus Area: %s", inst.campusArea.ifEmpty { "----" })
                    findViewById<TextView>(R.id.tv_infra_buildings).text = String.format(Locale.getDefault(), "Buildings: %d Wings", inst.buildingCount)

                    // Sync
                    findViewById<TextView>(R.id.tv_last_sync).text = String.format(Locale.getDefault(), "Last Sync: %s", state.lastSyncTime)
                }
            }
        }
    }

    private fun updateStatCard(id: Int, value: String, label: String) {
        val card = findViewById<View>(id) ?: return
        card.findViewById<TextView>(R.id.tv_stat_value).text = value
        card.findViewById<TextView>(R.id.tv_stat_label).text = label
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "Institution Profile"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Control Center"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btn_edit_profile_main).setOnClickListener {
            // Navigate to Edit Screen
            startActivity(Intent(this, EditInstitutionProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_logout_inst).setOnClickListener {
            showLogoutDialog()
        }

        // Featureplaceholders
        findViewById<View>(R.id.btn_change_password).setOnClickListener {
            Toast.makeText(this, "Security Settings Opening...", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_data_backup).setOnClickListener {
            Toast.makeText(this, "Syncing Cloud Backup...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Secure Logout")
            .setMessage("Are you sure you want to log out from the institution ecosystem?")
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
}
