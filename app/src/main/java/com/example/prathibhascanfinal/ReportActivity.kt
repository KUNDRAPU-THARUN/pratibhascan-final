package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ReportActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<ImageView>(R.id.btn_report_back).setOnClickListener { finish() }

        val sportName = intent.getStringExtra("SPORT_NAME") ?: "General"
        val skillName = intent.getStringExtra("SKILL_NAME") ?: "Training"
        
        findViewById<TextView>(R.id.tv_report_title).text = "Performance Report: $skillName"
        findViewById<TextView>(R.id.tv_report_sport).text = sportName

        // Mock data populating
        findViewById<TextView>(R.id.tv_report_overall_score).text = "85"
        findViewById<TextView>(R.id.tv_report_accuracy).text = "82%"
        findViewById<TextView>(R.id.tv_report_duration).text = "12:45"
        findViewById<TextView>(R.id.tv_report_mistakes).text = "• Back not straight during descent\n• Knees extending past toes"
        findViewById<TextView>(R.id.tv_report_recommendations).text = "• Practice core stability exercises\n• Increase ankle mobility"
        
        findViewById<AnalyticsGraphView>(R.id.report_progress_graph).setData(listOf(0.4f, 0.55f, 0.6f, 0.72f, 0.85f))

        findViewById<View>(R.id.btn_save_report_cloud)?.setOnClickListener {
            saveReportToFirebase(sportName, skillName)
        }
    }

    private fun saveReportToFirebase(sport: String, skill: String) {
        val userEmail = SessionManager(this).getEmail() ?: "anonymous"
        val report = mapOf(
            "user" to userEmail,
            "sport" to sport,
            "skill" to skill,
            "score" to 85,
            "accuracy" to 82,
            "timestamp" to System.currentTimeMillis()
        )
        
        lifecycleScope.launch {
            try {
                FirebaseManager.getFirebaseFirestore().collection("reports").add(report).await()
                Toast.makeText(this@ReportActivity, "Report saved to cloud!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ReportActivity, "Cloud Sync Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

