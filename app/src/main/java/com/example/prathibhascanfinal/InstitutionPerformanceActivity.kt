package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.TextView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class InstitutionPerformanceActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_student_performance)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        loadMockStudentPerformance()
    }

    private fun loadMockStudentPerformance() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InstitutionPerformanceActivity)
            val students = db.institutionManagementDao().getStudents(1)
            
            if (students.isNotEmpty()) {
                val s = students.first()
                findViewById<TextView>(R.id.tv_student_perf_name).text = s.fullName
                findViewById<TextView>(R.id.tv_movement_score).text = s.aiTechniqueScore.toString().takeIf { it != "0" } ?: "72"
                findViewById<TextView>(R.id.tv_posture_score).text = s.aiPostureScore.toString().takeIf { it != "0" } ?: "88"
                findViewById<TextView>(R.id.tv_student_attendance).text = "${s.attendancePercentage}%".takeIf { s.attendancePercentage > 0 } ?: "94%"
                
                findViewById<AnalyticsGraphView>(R.id.graph_student_improvement).setData(listOf(0.5f, 0.45f, 0.6f, 0.55f, 0.7f, 0.8f, 0.75f))
            } else {
                findViewById<AnalyticsGraphView>(R.id.graph_student_improvement).setData(listOf(0.4f, 0.5f, 0.55f, 0.62f, 0.65f, 0.7f, 0.72f))
            }
        }
    }
}

