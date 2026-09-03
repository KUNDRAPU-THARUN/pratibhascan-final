package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AcademyPerformanceActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_performance)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 0)
        
        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyPerformanceActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    loadMockPerformance()
                } else {
                    Toast.makeText(this@AcademyPerformanceActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            loadMockPerformance()
        }
    }

    private fun loadMockPerformance() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyPerformanceActivity)
            val athletes = db.academyManagementDao().getAthletesForAcademy(academyId)
            
            if (athletes.isNotEmpty()) {
                val a = athletes.first()
                findViewById<TextView>(R.id.tv_perf_athlete_name).text = a.fullName
                findViewById<TextView>(R.id.tv_technique_score).text = a.techniqueScore.toString().takeIf { it != "0" } ?: "85"
                findViewById<TextView>(R.id.tv_fitness_score).text = a.fitnessScore.toString().takeIf { it != "0" } ?: "92"
                findViewById<TextView>(R.id.tv_consistency).text = "${a.consistency}%".takeIf { a.consistency > 0 } ?: "78%"
                findViewById<TextView>(R.id.tv_ai_ranking).text = "#${(a.aiScore / 10).toInt()}".takeIf { a.aiScore > 0 } ?: "#12"
                
                findViewById<AnalyticsGraphView>(R.id.graph_improvement).setData(listOf(0.4f, 0.5f, 0.45f, 0.6f, 0.7f, 0.65f, 0.85f))
            } else {
                findViewById<AnalyticsGraphView>(R.id.graph_improvement).setData(listOf(0.3f, 0.4f, 0.5f, 0.45f, 0.6f, 0.75f, 0.8f))
            }
        }
    }
}

