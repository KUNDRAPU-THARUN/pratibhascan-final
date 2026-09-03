package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CoachPortfolioActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private var videoUri: Uri? = null
    
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            videoUri = uri
            findViewById<TextView>(R.id.tv_video_status)?.text = "Video Attached ✓"
            findViewById<TextView>(R.id.tv_video_status)?.setTextColor(0xFFFBBF24.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coach_portfolio)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        intent.getStringExtra("PRE_SELECT_SPORT")?.let { sport ->
            findViewById<TextInputEditText>(R.id.et_coach_specialization)?.setText(sport)
        }

        val swAvailability = findViewById<SwitchCompat>(R.id.sw_availability)
        val tvStatus = findViewById<TextView>(R.id.tv_availability_status)

        swAvailability?.setOnCheckedChangeListener { _, isChecked ->
            tvStatus?.text = if (isChecked) getString(R.string.status_looking) else getString(R.string.status_employed)
            tvStatus?.setTextColor(if (isChecked) 0xFF10B981.toInt() else 0xFF94A3B8.toInt())
        }

        findViewById<Button>(R.id.btn_upload_video)?.setOnClickListener {
            pickVideo.launch("video/*")
        }

        findViewById<Button>(R.id.btn_save_portfolio)?.setOnClickListener {
            saveCoachPortfolio()
        }

        findViewById<View>(R.id.btn_close_portfolio)?.setOnClickListener {
            finish()
        }
    }

    private fun saveCoachPortfolio() {
        val email = SessionManager(this).getEmail() ?: ""
        if (email.isEmpty()) {
            Toast.makeText(this, "Please login to save portfolio", Toast.LENGTH_SHORT).show()
            return
        }

        val spec = findViewById<TextInputEditText>(R.id.et_coach_specialization)?.text.toString().trim()
        if (spec.isEmpty()) {
            Toast.makeText(this, "Specialization is required", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<Button>(R.id.btn_save_portfolio)?.isEnabled = false

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@CoachPortfolioActivity)
                val user = db.userDao().getUserByEmail(email)
                
                if (user == null) {
                    Toast.makeText(this@CoachPortfolioActivity, "User profile not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Check for video upload
                val uploadedVideoUrl = if (videoUri != null) {
                    findViewById<TextView>(R.id.tv_video_status)?.text = "Uploading Video..."
                    FirebaseManager.uploadFile(videoUri!!, "coach_portfolios/${email}_${System.currentTimeMillis()}.mp4")
                } else null

                val isLooking = findViewById<SwitchCompat>(R.id.sw_availability)?.isChecked ?: false
                
                // Create or Update Coach entry
                val existingCoach = db.academyDao().getCoachesForAcademy(0).find { it.email == email }
                
                val coach = (existingCoach ?: Coach(email = email)).copy(
                    name = user.fullName,
                    phone = user.phoneNumber ?: "", 
                    specialization = spec,
                    isLookingForWork = isLooking,
                    portfolioVideoUri = uploadedVideoUrl ?: existingCoach?.portfolioVideoUri
                )

                // Save locally
                db.academyDao().insertCoach(coach)
                
                // Sync to Firebase
                val firebaseSuccess = FirebaseManager.saveCoach(coach)

                if (firebaseSuccess) {
                    Toast.makeText(this@CoachPortfolioActivity, "Professional Portfolio Published!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@CoachPortfolioActivity, "Saved locally. Firebase sync pending.", Toast.LENGTH_LONG).show()
                }
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@CoachPortfolioActivity, "Error saving portfolio: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                findViewById<Button>(R.id.btn_save_portfolio)?.isEnabled = true
            }
        }
    }
}

