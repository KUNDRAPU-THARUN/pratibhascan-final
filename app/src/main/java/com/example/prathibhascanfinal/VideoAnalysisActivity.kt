package com.example.prathibhascanfinal

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.databinding.ActivityVideoAnalysisBinding
import com.example.prathibhascanfinal.analysis.SquatStrategy
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoAnalysisActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var binding: ActivityVideoAnalysisBinding
    private lateinit var poseHelper: PoseLandmarkerHelper
    private lateinit var networkMonitor: NetworkMonitor
    private var isOnline = true

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedVideo(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        networkMonitor = NetworkMonitor(this)
        lifecycleScope.launch {
            networkMonitor.isOnline.collect { online ->
                isOnline = online
                updateNetworkStatusUI()
            }
        }

        binding.btnSelectVideo.setOnClickListener { pickVideo.launch("video/*") }
        binding.btnVideoBack.setOnClickListener { finish() }

        lifecycleScope.launch(Dispatchers.Default) {
            poseHelper = PoseLandmarkerHelper(
                context = this@VideoAnalysisActivity,
                runningMode = RunningMode.VIDEO
            )
        }
        
        observePendingQueue()
    }

    private fun updateNetworkStatusUI() {
        if (!isOnline) {
            Toast.makeText(this, "Limited network. Videos will be queued for offline analysis.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedVideo(uri: Uri) {
        if (!isOnline) {
            queueVideoForLater(uri)
        } else {
            startAnalysis(uri)
        }
    }

    private fun queueVideoForLater(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(application)
            db.pendingAnalysisDao().insert(
                PendingAnalysis(
                    videoUri = uri.toString(),
                    exerciseType = "SQUATS", // Default for demo
                    status = "PENDING"
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@VideoAnalysisActivity, "Video saved locally. Analysis will start when back online.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observePendingQueue() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(application)
            db.pendingAnalysisDao().getAllFlow().collect { list ->
                if (list.any { it.status == "PENDING" } && isOnline) {
                    processNextInQueue()
                }
            }
        }
    }

    private fun processNextInQueue() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(application)
            val next = db.pendingAnalysisDao().getNextPending()
            next?.let { 
                db.pendingAnalysisDao().update(it.copy(status = "PROCESSING"))
                startAnalysis(it.videoUri.toUri())
                db.pendingAnalysisDao().update(it.copy(status = "COMPLETED"))
            }
        }
    }

    private fun startAnalysis(uri: Uri) {
        binding.btnSelectVideo.visibility = View.GONE
        binding.progressVideoAnalysis.visibility = View.VISIBLE
        binding.tvAnalysisStatus.visibility = View.VISIBLE
        binding.tvAnalysisStatus.text = getString(R.string.processing_frames)

        lifecycleScope.launch {
            try {
                // Process every 200ms for better accuracy
                val results = withContext(Dispatchers.Default) {
                    poseHelper.detectVideoFile(uri, 200) 
                }

                if (results.isEmpty()) {
                    Toast.makeText(this@VideoAnalysisActivity, "Could not detect movement in video", Toast.LENGTH_LONG).show()
                    resetUI()
                    return@launch
                }

                binding.tvAnalysisStatus.text = "AI Analyzing Biomechanics..."
                
                var totalSimilarity = 0
                var repsFound = 0
                val strategy = SquatStrategy() // Use real strategy logic
                var currentState = ExerciseState.READY
                
                results.forEach { result ->
                    if (result.landmarks().isNotEmpty()) {
                        val landmarks = result.landmarks()[0]
                        
                        // 1. Process movement logic
                        val analysis = strategy.process(landmarks, currentState) {
                            repsFound++
                        }
                        currentState = analysis.newState
                        
                        // 2. Calculate real similarity vs Ideal Form
                        val ghost = strategy.generateGhost(landmarks, currentState)
                        val frameSimilarity = strategy.calculateSimilarity(landmarks, ghost)
                        totalSimilarity += frameSimilarity
                    }
                }

                val avgSimilarity = if (results.isNotEmpty()) totalSimilarity / results.size else 0
                val calories = (repsFound * 0.5) + (results.size * 0.02)
                
                val report = generateAIReport(repsFound, avgSimilarity)
                
                saveVideoSession(repsFound, avgSimilarity, calories.toInt())
                showResults(report, repsFound, avgSimilarity, calories.toInt())

            } catch (_: Exception) {
                Toast.makeText(this@VideoAnalysisActivity, "Analysis failed. Please try again.", Toast.LENGTH_SHORT).show()
                resetUI()
            }
        }
    }

    private fun saveVideoSession(reps: Int, accuracy: Int, calories: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(application)
            val userEmail = intent.getStringExtra("TARGET_ATHLETE_EMAIL") ?: SessionManager(application).getEmail() ?: ""
            val session = AnalyticsSession(
                userEmail = userEmail,
                sportName = "Video Analysis",
                exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: "SQUATS",
                accuracy = accuracy,
                techScore = (accuracy * 0.9).toInt(),
                repCount = reps,
                durationSeconds = 30, // Heuristic for video
                calories = calories.toDouble(),
                aiFeedback = "Video analysis completed with $accuracy% accuracy."
            )
            db.analyticsDao().saveSession(session)
        }
    }

    private suspend fun generateAIReport(reps: Int, accuracy: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val generativeModel = Firebase.ai(
                    backend = GenerativeBackend.googleAI(),
                    useLimitedUseAppCheckTokens = true
                ).generativeModel(modelName = "gemini-3.6-flash")
                val prompt = "Analyze workout video results: $reps reps completed with $accuracy% technique accuracy. Provide a 2-sentence summary and 1 tip."
                val response = generativeModel.generateContent(prompt)
                response.text ?: "Workout analyzed. Great consistency in depth, but watch your speed on the descent."
            } catch (e: Exception) {
                "Workout analyzed. Great consistency in depth, but watch your speed on the descent."
            }
        }
    }

    private fun showResults(report: String, reps: Int, accuracy: Int, calories: Int) {
        binding.progressVideoAnalysis.visibility = View.GONE
        binding.tvAnalysisStatus.visibility = View.GONE
        binding.layoutAnalysisResults.visibility = View.VISIBLE
        binding.tvVideoFeedback.text = report
        
        binding.tvFinalScore.text = getString(R.string.final_score_format, accuracy)
        binding.tvFinalAccuracy.text = getString(R.string.final_reps_format, reps)
        
        // Use a generic result message to show calories
        Toast.makeText(this, getString(R.string.calories_burned_format, calories), Toast.LENGTH_LONG).show()
    }

    private fun resetUI() {
        binding.btnSelectVideo.visibility = View.VISIBLE
        binding.progressVideoAnalysis.visibility = View.GONE
        binding.tvAnalysisStatus.visibility = View.GONE
    }
}
