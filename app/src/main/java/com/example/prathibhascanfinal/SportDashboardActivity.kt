package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import com.example.prathibhascanfinal.data.SportData
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SportDashboardActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var userEmail: String
    private lateinit var sportName: String
    private var enrollment: SportEnrollment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sport_dashboard)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        userEmail = SessionManager(this).getEmail() ?: ""
        
        // Dynamic loading from Intent object
        enrollment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("ENROLLMENT_DATA", SportEnrollment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("ENROLLMENT_DATA")
        }
        sportName = enrollment?.sportName ?: intent.getStringExtra("SPORT_NAME") ?: SessionManager(this).getSport() ?: "Cricket"

        setupHeader()
        setupModules()
        setupArenaActions()
        loadPerformanceStats()
    }

    private fun setupHeader() {
        val detail = SportData.getDetail(sportName)
        findViewById<TextView>(R.id.tv_welcome_name)?.text = getString(R.string.arena_title_format, detail.name)
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = getString(R.string.athlete_profile_subtitle, detail.category)
        
        // Banner updates
        findViewById<TextView>(R.id.tv_arena_sport_name)?.text = detail.name.uppercase()
        findViewById<TextView>(R.id.tv_current_skill)?.text = "Main Strategy: ${detail.drills.firstOrNull() ?: "General Training"}"
        findViewById<ImageView>(R.id.iv_arena_banner)?.setImageResource(detail.iconRes)
        
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

    private fun setupArenaActions() {
        findViewById<View>(R.id.btn_launch_ai_coach).setSafeOnClickListener {
            val intent = Intent(this, AICoachActivity::class.java)
            intent.putExtra("ENROLLMENT_DATA", enrollment)
            intent.putExtra("SPORT_NAME", sportName)
            startActivity(intent)
        }
        
        findViewById<View>(R.id.btn_launch_ar_ghost).setSafeOnClickListener {
            val intent = Intent(this, SkillSelectionActivity::class.java)
            intent.putExtra("ENROLLMENT_DATA", enrollment)
            intent.putExtra("SPORT_NAME", sportName)
            intent.putExtra("LAUNCH_AR_GHOST", true)
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_open_live_cam).setSafeOnClickListener {
            val intent = Intent(this, GameDashboardActivity::class.java)
            intent.putExtra("ENROLLMENT_DATA", enrollment)
            intent.putExtra("SPORT_NAME", sportName)
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_arena_upload_video).setSafeOnClickListener {
            val intent = Intent(this, SportAnalysisActivity::class.java)
            intent.putExtra("ENROLLMENT_DATA", enrollment)
            intent.putExtra("SPORT_NAME", sportName)
            intent.putExtra("SKILL_NAME", "Post-Match Analysis")
            startActivity(intent)
        }
        
        findViewById<View>(R.id.btn_arena_reports).setSafeOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            intent.putExtra("ENROLLMENT_DATA", enrollment)
            intent.putExtra("SPORT_NAME", sportName)
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_arena_achievements).setSafeOnClickListener {
            val intent = Intent(this, AchievementVaultActivity::class.java)
            intent.putExtra("ENROLLMENT_DATA", enrollment)
            startActivity(intent)
        }
        // ... rest stays same
    }

    private fun setupModules() {
        val container = findViewById<LinearLayout>(R.id.container_sport_modules)
        container.removeAllViews()

        val detail = SportData.getDetail(sportName)
        val modules = detail.modules

        modules.forEach { moduleName ->
            val moduleView = layoutInflater.inflate(R.layout.item_sport_module, container, false)
            moduleView.findViewById<TextView>(R.id.tv_module_name).text = moduleName
            moduleView.setSafeOnClickListener {
                val intent = Intent(this, SkillSelectionActivity::class.java)
                intent.putExtra("ENROLLMENT_DATA", enrollment)
                intent.putExtra("SPORT_NAME", sportName)
                intent.putExtra("MODULE_NAME", moduleName)
                startActivity(intent)
            }
            container.addView(moduleView)
        }
    }

    private fun loadPerformanceStats() {
        // Mocking performance data
        findViewById<TextView>(R.id.tv_today_score)?.text = "842"
        findViewById<TextView>(R.id.tv_progress_pct)?.text = "65% Mastery"
        findViewById<ProgressBar>(R.id.pb_training)?.progress = 65
    }
}

