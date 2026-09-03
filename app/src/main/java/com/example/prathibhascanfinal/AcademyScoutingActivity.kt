package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.databinding.ActivityAcademyScoutingBinding
import kotlinx.coroutines.launch

class AcademyScoutingActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var binding: ActivityAcademyScoutingBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: ScoutReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcademyScoutingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        db = AppDatabase.getDatabase(this)
        
        setupRecyclerView()
        loadReports()
    }

    private fun setupRecyclerView() {
        adapter = ScoutReportAdapter(emptyList()) { report ->
            // Action click: Recruit or Trial
            recruitAthlete(report)
        }
        binding.rvScoutReports.adapter = adapter
    }

    private fun loadReports() {
        lifecycleScope.launch {
            val userEmail = SessionManager(this@AcademyScoutingActivity).getEmail()
            val academies = db.academyDao().getAllAcademies()
            val myAcademy = academies.find { it.contactEmail == userEmail || it.academyName == SessionManager(this@AcademyScoutingActivity).getName() }
            
            val reports = db.scoutReportDao().getReportsForAcademy(myAcademy?.id ?: 0)
            if (reports.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                adapter.updateData(reports)
            }
        }
    }

    private fun recruitAthlete(report: ScoutReport) {
        lifecycleScope.launch {
            val updatedReport = report.copy(status = "Interested")
            db.scoutReportDao().updateReport(updatedReport)
            Toast.makeText(this@AcademyScoutingActivity, "Sent Interest to ${report.studentName}!", Toast.LENGTH_SHORT).show()
            loadReports()
        }
    }
}

