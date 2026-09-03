package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import com.example.prathibhascanfinal.data.SportData
import com.example.prathibhascanfinal.ui.base.BaseActivity

class SkillSelectionActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var sportName: String
    private lateinit var moduleName: String
    private var enrollment: SportEnrollment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skill_selection)
        findViewById<android.widget.FrameLayout>(android.R.id.content).applySystemBarsPadding()

        enrollment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("ENROLLMENT_DATA", SportEnrollment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("ENROLLMENT_DATA")
        }
        sportName = enrollment?.sportName ?: intent.getStringExtra("SPORT_NAME") ?: "Cricket"
        moduleName = intent.getStringExtra("MODULE_NAME") ?: "Batting"

        findViewById<TextView>(R.id.tv_skill_title).text = getString(R.string.skill_title_format, sportName, moduleName)
        findViewById<ImageView>(R.id.btn_skill_back).setOnClickListener { finish() }

        setupSkills()
    }

    private fun setupSkills() {
        val container = findViewById<LinearLayout>(R.id.container_skills)
        container.removeAllViews()

        val detail = SportData.getDetail(sportName)
        val skills = detail.skills[moduleName] ?: listOf("Technique Analysis", "Form Drill")

        skills.forEach { skillName ->
            val skillView = layoutInflater.inflate(R.layout.item_sport_module, container, false)
            skillView.findViewById<TextView>(R.id.tv_module_name).text = skillName
            skillView.setOnClickListener {
                startAnalysis(skillName)
            }
            container.addView(skillView)
        }
    }

    private fun startAnalysis(skillName: String) {
        val launchGhost = intent.getBooleanExtra("LAUNCH_AR_GHOST", false)
        val intent = Intent(this, SportAnalysisActivity::class.java)
        intent.putExtra("ENROLLMENT_DATA", enrollment)
        intent.putExtra("SPORT_NAME", sportName)
        intent.putExtra("MODULE_NAME", moduleName)
        intent.putExtra("SKILL_NAME", skillName)
        intent.putExtra("SHOW_GHOST", launchGhost)
        startActivity(intent)
    }
}

