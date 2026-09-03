package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.databinding.ActivityTalentScoutingBinding
import kotlinx.coroutines.launch

class TalentScoutingActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var binding: ActivityTalentScoutingBinding
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private var athleteList: List<User> = emptyList()
    private var academyList: List<Academy> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalentScoutingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        loadSpinners()

        binding.btnSubmitReport.setOnClickListener {
            submitReport()
        }

        binding.spinnerAthletes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (athleteList.isNotEmpty()) {
                    val athlete = athleteList[position]
                    binding.layoutAthletePreview.visibility = View.VISIBLE
                    binding.tvAthleteStats.text = "Impact Score: ${String.format(java.util.Locale.US, "%.1f", athlete.technicalImpactScore)} | Tier: ${athlete.currentTier ?: "N/A"}"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.layoutAthletePreview.visibility = View.GONE
            }
        }
    }

    private fun loadSpinners() {
        lifecycleScope.launch {
            // Load Athletes
            athleteList = db.userDao().getAllAthletes()
            if (athleteList.isEmpty()) {
                Toast.makeText(this@TalentScoutingActivity, "No athletes found in the database", Toast.LENGTH_SHORT).show()
                binding.layoutAthletePreview.visibility = View.GONE
            }
            val athleteNames = athleteList.map { "${it.fullName} (${it.primaryDiscipline ?: "N/A"})" }
            val athleteAdapter = ArrayAdapter(this@TalentScoutingActivity, android.R.layout.simple_spinner_dropdown_item, athleteNames)
            binding.spinnerAthletes.adapter = athleteAdapter

            // Load Academies
            academyList = db.academyDao().getAllAcademies()
            if (academyList.isEmpty()) {
                Toast.makeText(this@TalentScoutingActivity, "No academies found in the database", Toast.LENGTH_SHORT).show()
            }
            val academyNames = academyList.map { it.academyName }
            val academyAdapter = ArrayAdapter(this@TalentScoutingActivity, android.R.layout.simple_spinner_dropdown_item, academyNames)
            binding.spinnerAcademies.adapter = academyAdapter
        }
    }

    private fun submitReport() {
        val athleteIndex = binding.spinnerAthletes.selectedItemPosition
        val academyIndex = binding.spinnerAcademies.selectedItemPosition
        val note = binding.etNote.text.toString()

        if (athleteIndex < 0 || athleteList.isEmpty()) {
            Toast.makeText(this, "Please select an athlete", Toast.LENGTH_SHORT).show()
            return
        }
        if (academyIndex < 0 || academyList.isEmpty()) {
            Toast.makeText(this, "Please select an academy", Toast.LENGTH_SHORT).show()
            return
        }

        if (note.isEmpty()) {
            Toast.makeText(this, "Please add a recommendation note", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAthlete = athleteList[athleteIndex]
        val selectedAcademy = academyList[academyIndex]

        lifecycleScope.launch {
            val report = ScoutReport(
                studentEmail = selectedAthlete.email,
                studentName = selectedAthlete.fullName,
                teacherEmail = sessionManager.getEmail() ?: "unknown@teacher.com",
                institutionName = sessionManager.getName() ?: "Institutional Partner",
                targetAcademyId = selectedAcademy.id,
                academyName = selectedAcademy.academyName,
                sportCategory = selectedAthlete.primaryDiscipline ?: "General",
                recommendationNote = note,
                aiScore = selectedAthlete.technicalImpactScore
            )

            db.scoutReportDao().insertReport(report)
            Toast.makeText(this@TalentScoutingActivity, "Scouting Report Sent Successfully!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

