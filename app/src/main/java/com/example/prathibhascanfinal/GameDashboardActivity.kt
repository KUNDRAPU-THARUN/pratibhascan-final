package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import com.example.prathibhascanfinal.data.SportData
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.util.TypedValue

class GameDashboardActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var userEmail: String
    private lateinit var sportName: String
    private var enrollment: SportEnrollment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_dashboard)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        userEmail = SessionManager(this).getEmail() ?: ""
        
        enrollment = intent.getParcelableExtra("ENROLLMENT_DATA")
        sportName = enrollment?.sportName ?: intent.getStringExtra("SPORT_NAME") ?: "Cricket"

        setupHeader()
        loadGameDetails()
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "$sportName Arena"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "System Architect & Management"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }

        findViewById<View>(R.id.iv_profile_header)?.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_vernacular_toggle)?.setOnClickListener {
            showLanguageDialog()
        }
    }


    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Hindi", "Telugu", "Tamil", "Kannada", "Malayalam", "Marathi", "Bengali", "Gujarati", "Punjabi", "Odia")
        val codes = arrayOf("en", "hi", "te", "ta", "kn", "ml", "mr", "bn", "gu", "pa", "or")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setItems(languages) { _, which ->
                LocaleHelper.setLocale(this, codes[which])
                recreate()
            }
            .show()
    }

    private fun loadGameDetails() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@GameDashboardActivity)
            // Single source of truth: Load fresh from DB if we only have the name
            val activeEnrollment = enrollment ?: db.sportEnrollmentDao().getEnrollmentForSport(userEmail, sportName)
            
            // Set Sport Specific Data
            setSportMetadata()

            activeEnrollment?.let {
                val roleData = it.specializedData?.split(";")?.firstOrNull { part -> part.contains("Role") || part.contains("Position") }
                val roleValue = roleData?.split(":")?.lastOrNull() ?: it.positionStyle
                
                findViewById<TextView>(R.id.tv_game_role).text = "Role: $roleValue"
                findViewById<TextView>(R.id.tv_game_exp).text = "Experience: ${it.yearsExperience}"
                
                // Set Analytics Timeline
                val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val cal = java.util.Calendar.getInstance()
                val currentMonth = months[cal.get(java.util.Calendar.MONTH)]
                val currentYear = cal.get(java.util.Calendar.YEAR)
                findViewById<TextView>(R.id.tv_analytics_timeline).text = "Trend for $currentMonth $currentYear"

                findViewById<AnalyticsGraphView>(R.id.graph_game_growth).setData(listOf(0.4f, 0.5f, 0.45f, 0.65f, 0.7f, 0.75f, 0.82f))
            }
        }
    }

    private fun setSportMetadata() {
        val detail = SportData.getDetail(sportName)

        findViewById<TextView>(R.id.tv_game_description).text = detail.description
        findViewById<TextView>(R.id.tv_game_rules).text = detail.rules
        findViewById<TextView>(R.id.tv_game_equipment).text = detail.equipment
        
        val container = findViewById<LinearLayout>(R.id.container_game_drills)
        container.removeAllViews()
        
        detail.drills.forEach { drill ->
            val tv = TextView(this).apply {
                text = "• $drill"
                setTextColor(0xFFFFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 4, 0, 4)
            }
            container.addView(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGameDetails()
    }
}

