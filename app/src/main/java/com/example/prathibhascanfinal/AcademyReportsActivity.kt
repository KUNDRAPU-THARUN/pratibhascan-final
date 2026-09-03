package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AcademyReportsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_academy_reports)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 0)
        
        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyReportsActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    setupHeader()
                    setupReportButtons()
                } else {
                    Toast.makeText(this@AcademyReportsActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            setupHeader()
            setupReportButtons()
        }
    }

    private fun setupReportButtons() {
        findViewById<View>(R.id.btn_gen_all_reports)?.setOnClickListener {
            Toast.makeText(this, "Generating Academy PDF Report...", Toast.LENGTH_LONG).show()
        }

        val items = listOf(
            R.id.btn_athlete_report to "Athlete Performance",
            R.id.btn_coach_report to "Coach Attendance",
            R.id.btn_inventory_report to "Inventory Log",
            R.id.btn_financial_report to "Financial Report",
            R.id.btn_ai_readiness_report to "AI Readiness"
        )

        items.forEach { (id, name) ->
            findViewById<View>(id)?.setOnClickListener {
                Toast.makeText(this, "Exporting $name Report...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "Reports Center"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Organization metrics"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
    }
}

