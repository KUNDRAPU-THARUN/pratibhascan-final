package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.launch

class AcademyVideoAnalysisActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private var athleteEmails = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_video_analysis)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        loadAthletes()

        findViewById<Button>(R.id.btn_select_athlete_video).setOnClickListener {
            showAthletePicker()
        }
    }

    private fun loadAthletes() {
        lifecycleScope.launch {
            val session = SessionManager(this@AcademyVideoAnalysisActivity)
            val email = session.getEmail() ?: ""
            val db = AppDatabase.getDatabase(this@AcademyVideoAnalysisActivity)
            val academy = db.academyDao().getAcademyByEmail(email)
            
            academy?.let { aca ->
                val athletes = db.academyManagementDao().getAthletesForAcademy(aca.id)
                athleteEmails.clear()
                athleteEmails.addAll(athletes.map { it.email })
            }
        }
    }

    private fun showAthletePicker() {
        if (athleteEmails.isEmpty()) {
            Toast.makeText(this, "No athletes registered in academy.", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, athleteEmails)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Athlete for Analysis")
            .setAdapter(adapter) { _, which ->
                val selectedEmail = athleteEmails[which]
                val intent = Intent(this, VideoAnalysisActivity::class.java).apply {
                    putExtra("TARGET_ATHLETE_EMAIL", selectedEmail)
                }
                startActivity(intent)
            }
            .show()
    }
}
