package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.prathibhascanfinal.analysis.RepStrategy
import com.example.prathibhascanfinal.databinding.ActivitySportAnalysisBinding
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import com.example.prathibhascanfinal.VideoAnalysisActivity

import androidx.core.view.updateLayoutParams

class SportAnalysisActivity : BaseActivity(), PoseLandmarkerHelper.LandmarkerListener, TextToSpeech.OnInitListener {

    private lateinit var binding: ActivitySportAnalysisBinding
    override val viewModel: AICoachViewModel by viewModels()
    
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var isSwitchingCamera = false

    private lateinit var sportName: String
    private var skillName: String? = null
    private var enrollment: SportEnrollment? = null

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySportAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        enrollment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("ENROLLMENT_DATA", SportEnrollment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("ENROLLMENT_DATA")
        }
        sportName = enrollment?.sportName ?: intent.getStringExtra("SPORT_NAME") ?: "General Training"
        skillName = intent.getStringExtra("SKILL_NAME")

        setupSystemBars()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        tts = TextToSpeech(this, this)

        checkPermissions()
        observeViewModel()
        setupControls()
        setupExerciseChips()
        
        lifecycleScope.launch(Dispatchers.Default) {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = this@SportAnalysisActivity,
                runningMode = RunningMode.LIVE_STREAM,
                poseLandmarkerHelperListener = this@SportAnalysisActivity
            )
        }
    }

    private fun setupSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAnalysisLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.analysisHeader.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            binding.scrollViewStats.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun checkPermissions() {
        if (REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private var bitmapBuffer: Bitmap? = null
    private var lastAnalysisTime = 0L

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { proxy ->
                    // Limit processing to ~15 FPS to avoid GPU congestion
                    val now = System.currentTimeMillis()
                    if (now - lastAnalysisTime < 65) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    lastAnalysisTime = now

                    if (bitmapBuffer == null || bitmapBuffer!!.width != proxy.width || bitmapBuffer!!.height != proxy.height) {
                        bitmapBuffer?.recycle()
                        bitmapBuffer = Bitmap.createBitmap(proxy.width, proxy.height, Bitmap.Config.ARGB_8888)
                    }
                    
                    // Safe buffer copy handling potential padding/stride
                    try {
                        val buffer = proxy.planes[0].buffer
                        buffer.rewind()
                        bitmapBuffer?.copyPixelsFromBuffer(buffer)
                        
                        if (::poseLandmarkerHelper.isInitialized) {
                            poseLandmarkerHelper.detectLiveStream(bitmapBuffer!!, proxy.imageInfo.rotationDegrees)
                        }
                    } catch (e: Exception) {
                        Log.e("SportAnalysis", "Analyzer error: ${e.message}")
                    } finally {
                        proxy.close()
                    }
                }
            }

        provider.unbindAll()
        try {
            if (!isFinishing && !isDestroyed) {
                provider.bindToLifecycle(this, selector, preview, analysis)
            }
        } catch (e: Exception) {
            Log.e("SportAnalysis", "Binding failed", e)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: CoachState) {
        binding.tvRepCount.text = "${state.repCount} / ${state.targetReps}"
        binding.tvWorkoutProgressPct.text = "${(state.repCount * 100 / state.targetReps)}%"
        binding.tvSimilarityScore.text = "${state.accuracy}%"
        binding.tvCalories.text = state.calories.toInt().toString()
        binding.tvLiveFeedback.text = state.feedback
        binding.tvActiveExerciseName.text = state.exerciseType.name.replace("_", " ")
        
        binding.tvVisibilityText.text = state.bodyVisibilityMessage ?: "DETECTING..."
        when (state.visibilityStatus) {
            "GREEN" -> binding.viewVisibilityIndicator.setBackgroundResource(R.drawable.bg_circle_green)
            "YELLOW" -> binding.viewVisibilityIndicator.setBackgroundResource(R.drawable.bg_circle_yellow)
            else -> binding.viewVisibilityIndicator.setBackgroundResource(R.drawable.bg_circle_red)
        }

        if (state.countdown > 0) {
            binding.tvVisibilityText.text = "STARTING IN ${state.countdown}..."
            binding.tvVisibilityText.setTextColor(Color.YELLOW)
        } else {
            binding.tvVisibilityText.setTextColor(Color.WHITE)
        }

        binding.overlayView.setGhostLandmarks(state.ghostLandmarks)
        
        if (state.feedback.isNotEmpty()) speak(state.feedback)
    }

    private fun setupControls() {
        binding.btnAnalysisBack.setOnClickListener { finish() }
        
        binding.btnSwitchCamera.setOnClickListener {
            if (isSwitchingCamera) return@setOnClickListener
            
            lifecycleScope.launch {
                try {
                    isSwitchingCamera = true
                    
                    // Unbind everything first
                    cameraProvider?.unbindAll()
                    
                    // Toggle lens
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                    
                    viewModel.resetDetection()
                    Toast.makeText(this@SportAnalysisActivity, "Switching Camera...", Toast.LENGTH_SHORT).show()
                    
                    // Small delay to allow hardware to release
                    kotlinx.coroutines.delay(500L)
                    
                    // Re-bind
                    bindCameraUseCases()
                    
                } catch (e: Exception) {
                    Log.e("SportAnalysis", "Switch failed", e)
                    Toast.makeText(this@SportAnalysisActivity, "Camera switch failed. Restarting...", Toast.LENGTH_SHORT).show()
                    startCamera() // Try to recover by fully restarting CameraX
                } finally {
                    isSwitchingCamera = false
                }
            }
        }

        binding.sbGhostOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { binding.overlayView.setGhostMode(true, p) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        
        binding.btnPauseAnalysis.setOnClickListener { cameraProvider?.unbindAll() }
        binding.btnResumeAnalysis.setOnClickListener { bindCameraUseCases() }
        binding.btnFinishAnalysis.setOnClickListener { finish() }

        binding.btnUploadAnalysisVideo.setOnClickListener {
            startActivity(Intent(this, VideoAnalysisActivity::class.java))
        }

        binding.btnSplitScreen.setOnClickListener {
            binding.overlayView.setSplitScreenMode(true)
            Toast.makeText(this, "Professional Athlete Comparison Enabled", Toast.LENGTH_SHORT).show()
        }

        var isDebug = false
        binding.btnDebugMode.setOnClickListener {
            isDebug = !isDebug
            binding.overlayView.setDebugMode(isDebug)
            Toast.makeText(this, "Debug Landmarks: ${if (isDebug) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        binding.btnStartAiSession.setOnClickListener {
            val selectedType = viewModel.uiState.value.exerciseType.takeIf { it != ExerciseType.NONE } ?: ExerciseType.SQUATS
            viewModel.setExercise(selectedType)
            it.visibility = View.GONE
            Toast.makeText(this, "Recording Analysis Session...", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnFinishAnalysis.setOnClickListener { 
            showWorkoutSummary()
        }
    }

    private fun showWorkoutSummary() {
        val state = viewModel.uiState.value
        val message = """
            Workout Summary:
            Exercise: ${state.exerciseType}
            Reps: ${state.repCount}
            Calories: ${state.calories.toInt()}
            Avg Similarity: ${state.accuracy}%
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Session Complete")
            .setMessage(message)
            .setPositiveButton("Save Report") { _, _ -> 
                viewModel.finishWorkout()
                Toast.makeText(this, "Report Saved Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Close") { _, _ -> finish() }
            .show()
    }

    private fun setupExerciseChips() {
        binding.containerExerciseChips.removeAllViews()
        val exercises = ExerciseType.entries.filter { it != ExerciseType.NONE && it != ExerciseType.SPORT_SKILL }
        exercises.forEach { type ->
            val btn = Button(this).apply {
                text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                textSize = 12f
                setOnClickListener { viewModel.setExercise(type) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 8.dpToPx(), 0)
                }
            }
                binding.containerExerciseChips.addView(btn)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private var lastSpeechTime = 0L
    private fun speak(text: String) {
        if (isTtsReady && System.currentTimeMillis() - lastSpeechTime > 3000) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpeechTime = System.currentTimeMillis()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isTtsReady = true
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        if (resultBundle.results.isEmpty()) return
        viewModel.processPoseResults(resultBundle.results[0])
        runOnUiThread {
            val state = viewModel.uiState.value
            val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
            binding.overlayView.setResults(
                resultBundle.results[0],
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                state.userLandmarks,
                state.incorrectJoints,
                state.warningJoints,
                state.currentAngles,
                isFront,
                state.exerciseState.name
            )
        }
    }

    override fun onError(error: String) {
        runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_SHORT).show() }
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tts.shutdown()
    }
}

