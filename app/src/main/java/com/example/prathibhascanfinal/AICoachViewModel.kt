package com.example.prathibhascanfinal

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.prathibhascanfinal.analysis.AnalysisResult
import com.example.prathibhascanfinal.analysis.BasketballStrategy
import com.example.prathibhascanfinal.analysis.BurpeeStrategy
import com.example.prathibhascanfinal.analysis.HighKneesStrategy
import com.example.prathibhascanfinal.analysis.JumpingJackStrategy
import com.example.prathibhascanfinal.analysis.LungeStrategy
import com.example.prathibhascanfinal.analysis.MountainClimberStrategy
import com.example.prathibhascanfinal.analysis.PlankStrategy
import com.example.prathibhascanfinal.analysis.PullUpStrategy
import com.example.prathibhascanfinal.analysis.PushUpStrategy
import com.example.prathibhascanfinal.analysis.RepStrategy
import com.example.prathibhascanfinal.analysis.SitUpStrategy
import com.example.prathibhascanfinal.analysis.SkippingStrategy
import com.example.prathibhascanfinal.analysis.SprintDrillsStrategy
import com.example.prathibhascanfinal.analysis.SquatStrategy
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExerciseType {
    SQUATS, PUSH_UPS, LUNGES, PLANKS, JUMPING_JACKS, BURPEES, BICEP_CURLS, SIT_UPS,
    MOUNTAIN_CLIMBERS, RUNNING, WALKING, STRETCHING, SPORT_SKILL, NONE, PULL_UPS, HIGH_KNEES, SKIPPING, SPRINT_DRILLS,
    BASKETBALL_SHOOT, BASKETBALL_DRIBBLING, BASKETBALL_DEFENSE
}

enum class ExerciseState {
    READY, START_VERIFIED, MOVING_DOWN, MID_VERIFIED, MOVING_UP, END_VERIFIED, COMPLETED
}

data class CoachState(
    val exerciseType: ExerciseType = ExerciseType.NONE,
    val chatHistory: List<ChatMessage> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
    val isOnline: Boolean = true,
    val sportName: String? = null,
    val skillName: String? = null,
    val repCount: Int = 0,
    val targetReps: Int = 10,
    val currentSet: Int = 1,
    val targetSets: Int = 3,
    val feedback: String = "Align yourself in front of the camera",
    val accuracy: Int = 0,
    val techniqueScore: Int = 0,
    val confidence: Float = 0f,
    val isExerciseStarted: Boolean = false,
    val currentAngle: Double = 0.0,
    val currentAngles: Map<String, Double> = emptyMap(),
    val calories: Double = 0.0,
    val durationSeconds: Long = 0,
    val incorrectJoints: Set<Int> = emptySet(),
    val warningJoints: Set<Int> = emptySet(),
    val userLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null,
    val ghostLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null,
    val isFullBodyVisible: Boolean = false,
    val visibilityStatus: String = "RED", // RED, YELLOW, GREEN
    val bodyVisibilityMessage: String? = null,
    val exerciseMatchVerified: Boolean = true,
    val detectedExercise: ExerciseType = ExerciseType.NONE,
    val isWorkoutCompleted: Boolean = false,
    val correctRepsCount: Int = 0,
    val incorrectRepsCount: Int = 0,
    val averageJointAngles: Map<String, Double> = emptyMap(),
    val movementQuality: Int = 0,
    val bodyAlignmentScore: Int = 0,
    val restRemaining: Int = 0,
    val isResting: Boolean = false,
    val personDetected: Boolean = false,
    val countdown: Int = 0,
    val exerciseState: ExerciseState = ExerciseState.READY
)

class AICoachViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val _uiState = MutableStateFlow(CoachState())
    val uiState: StateFlow<CoachState> = _uiState.asStateFlow()

    private val chatHistoryRepository = ChatHistoryRepository(AppDatabase.getDatabase(application).chatDao())
    private val geminiRepository = GeminiRepository()
    private val networkMonitor = NetworkMonitor(application)
    private var currentConversationId: String? = null

    private val poseSmoother = PoseSmoother(alpha = 0.65f)

    private var currentExerciseState = ExerciseState.READY
    private var lastFeedbackTime = 0L
    private var startTime = 0L
    
    // Performance metrics
    private var totalAccuracySum = 0
    private var frameCount = 0
    private val CONFIDENCE_THRESHOLD = 0.6f
    
    private var currentStrategy: RepStrategy? = null
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 800L
    
    // Exercise MET Values (Calories per minute = MET * 3.5 * weight_kg / 200)
    private val MET_VALUES = mapOf(
        ExerciseType.SQUATS to 8.0,
        ExerciseType.PUSH_UPS to 8.0,
        ExerciseType.LUNGES to 6.0,
        ExerciseType.PLANKS to 3.5,
        ExerciseType.JUMPING_JACKS to 8.0,
        ExerciseType.BURPEES to 10.0,
        ExerciseType.MOUNTAIN_CLIMBERS to 9.0,
        ExerciseType.SIT_UPS to 5.0,
        ExerciseType.PULL_UPS to 8.0,
        ExerciseType.HIGH_KNEES to 10.0,
        ExerciseType.SKIPPING to 12.0,
        ExerciseType.SPRINT_DRILLS to 15.0
    )
    
    // Cache for strategies
    private val strategies = mapOf(
        ExerciseType.SQUATS to SquatStrategy(),
        ExerciseType.PUSH_UPS to PushUpStrategy(),
        ExerciseType.LUNGES to LungeStrategy(),
        ExerciseType.PLANKS to PlankStrategy(),
        ExerciseType.JUMPING_JACKS to JumpingJackStrategy(),
        ExerciseType.BURPEES to BurpeeStrategy(),
        ExerciseType.MOUNTAIN_CLIMBERS to MountainClimberStrategy(),
        ExerciseType.SIT_UPS to SitUpStrategy(),
        ExerciseType.PULL_UPS to PullUpStrategy(),
        ExerciseType.HIGH_KNEES to HighKneesStrategy(),
        ExerciseType.SKIPPING to SkippingStrategy(),
        ExerciseType.SPRINT_DRILLS to SprintDrillsStrategy(),
        ExerciseType.BASKETBALL_SHOOT to BasketballStrategy(ExerciseType.BASKETBALL_SHOOT),
        ExerciseType.BASKETBALL_DRIBBLING to BasketballStrategy(ExerciseType.BASKETBALL_DRIBBLING),
        ExerciseType.BASKETBALL_DEFENSE to BasketballStrategy(ExerciseType.BASKETBALL_DEFENSE)
    )

    init {
        val userEmail = SessionManager(application).getEmail() ?: "guest@example.com"
        
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        viewModelScope.launch {
            chatHistoryRepository.getConversationsForUser(userEmail).collect { conversations ->
                if (conversations.isNotEmpty()) {
                    val lastConversation = conversations.first()
                    currentConversationId = lastConversation.id
                    loadMessages(lastConversation.id)
                }
            }
        }
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            chatHistoryRepository.getMessages(conversationId).collect { history ->
                _uiState.update { it.copy(chatHistory = history) }
            }
        }
    }

    fun setExercise(type: ExerciseType, sport: String? = null, skill: String? = null) {
        currentStrategy = strategies[type]
        val sportActual = sport ?: "General"
        _uiState.update { 
            it.copy(
                exerciseType = type,
                sportName = sportActual,
                skillName = skill,
                isExerciseStarted = true,
                durationSeconds = 0,
                calories = 0.0,
                feedback = "Starting ${type.name.replace("_", " ")}. Stand in frame.",
                suggestedQuestions = getSuggestionsForSport(sportActual)
            )
        }
        currentExerciseState = ExerciseState.READY
        startTime = System.currentTimeMillis()
    }

    private fun getSuggestionsForSport(sport: String): List<String> {
        return com.example.prathibhascanfinal.data.SportData.getDetail(sport).suggestions
    }

    private var isSending = false

    fun sendMessage(text: String, bitmap: Bitmap? = null) {
        if (isSending) return
        
        if (!_uiState.value.isOnline) {
            val offlineMsg = ChatMessage(
                text = "Coach: I'm currently offline. Your message has been saved and I'll respond as soon as you're back in the arena!",
                sender = MessageSender.AI
            )
            _uiState.update { it.copy(chatHistory = it.chatHistory + offlineMsg) }
            return
        }

        isSending = true
        val userEmail = SessionManager(getApplication()).getEmail() ?: "guest@example.com"
        val sport = _uiState.value.sportName ?: "General"

        viewModelScope.launch {
            try {
                if (currentConversationId == null) {
                    currentConversationId = chatHistoryRepository.createConversation(userEmail, sport)
                }

                val userMessage = ChatMessage(
                    text = text + if (bitmap != null) " [Media attached]" else "",
                    sender = MessageSender.USER,
                    status = MessageStatus.SENDING
                )
                
                _uiState.update { it.copy(chatHistory = it.chatHistory + userMessage, accuracy = -1) }
                
                val responseText = geminiRepository.sendMessage(
                    prompt = text,
                    bitmap = bitmap,
                    athleteProfile = getAthleteProfileContext(userEmail, sport)
                )

                val aiMessage = ChatMessage(
                    text = responseText,
                    sender = MessageSender.AI
                )

                val finalUserMessage = userMessage.copy(status = MessageStatus.SENT)
                
                _uiState.update { currentState ->
                    val newHistory = currentState.chatHistory.map { 
                        if (it.id == userMessage.id) finalUserMessage else it 
                    } + aiMessage
                    currentState.copy(chatHistory = newHistory, accuracy = 0)
                }

                currentConversationId?.let {
                    chatHistoryRepository.saveMessage(it, finalUserMessage)
                    chatHistoryRepository.saveMessage(it, aiMessage)
                }
            } finally {
                isSending = false
            }
        }
    }

    fun retryLastMessage() {
        val lastUserMessage = _uiState.value.chatHistory.lastOrNull { it.isUser }
        if (lastUserMessage != null) {
            // Remove the error message from history if it's there
            val lastAiMessage = _uiState.value.chatHistory.lastOrNull()
            if (lastAiMessage != null && !lastAiMessage.isUser && lastAiMessage.text.contains("trouble connecting")) {
                 _uiState.update { it.copy(chatHistory = it.chatHistory.dropLast(1)) }
            }
            sendMessage(lastUserMessage.text.replace(" [Media attached]", ""))
        }
    }

    private suspend fun getAthleteProfileContext(email: String, sport: String): String {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            val user = db.userDao().getUserByEmail(email)
            
            // Fetch recent performance sessions
            val accuracyHistory = db.analyticsDao().getRecentAccuracySync(email)
            val latestSession = db.analyticsDao().getLatestSessionSync(email)
            val avgAccuracy = if (accuracyHistory.isNotEmpty()) accuracyHistory.average().toInt() else 0
            val recentImprovement = if (accuracyHistory.size >= 2) accuracyHistory[0] - accuracyHistory[1] else 0
            
            // Fetch specific sport profile
            val sportProfile = when (sport.lowercase()) {
                "cricket" -> db.sportProfileDao().getCricket(email)?.toString()
                "football" -> db.sportProfileDao().getFootball(email)?.toString()
                "basketball" -> db.sportProfileDao().getBasketball(email)?.toString()
                else -> null
            }

            val achievements = db.achievementDao().getAchievementsForUser(email).take(3).joinToString { "${it.position} at ${it.tournamentName}" }
            
            """
                Athlete Name: ${user?.fullName ?: "Unknown"}
                Sport: $sport
                Skill Level: ${user?.experienceLevel ?: "Beginner"}
                Average AI Accuracy: $avgAccuracy%
                Recent Trend: ${if (recentImprovement >= 0) "+" else ""}$recentImprovement% in last session
                Latest Session Feedback: ${latestSession?.aiFeedback ?: "No feedback yet"}
                Sport Profile Data: ${sportProfile ?: "Generic training active"}
                Top Achievements: ${if (achievements.isNotEmpty()) achievements else "Building a legacy"}
                Current Goals: ${user?.primaryDiscipline ?: "Core technical development"}
            """.trimIndent()
        }
    }

    fun setWorkoutConfig(targetReps: Int, sets: Int) {
        _uiState.update {
            it.copy(
                targetReps = targetReps,
                targetSets = sets,
                repCount = 0,
                currentSet = 1,
                isWorkoutCompleted = false
            )
        }
    }

    private fun handleRepetitionComplete() {
        if (System.currentTimeMillis() - lastRepTime < REP_COOLDOWN_MS) return
        lastRepTime = System.currentTimeMillis()

        _uiState.update { currentState ->
            val newRepCount = currentState.repCount + 1
            
            if (newRepCount >= currentState.targetReps) {
                if (currentState.currentSet >= currentState.targetSets) {
                    saveAnalyticsSession(currentState.copy(repCount = newRepCount))
                    currentState.copy(repCount = newRepCount, isWorkoutCompleted = true, feedback = "Workout Complete!")
                } else {
                    startRestTimer()
                    currentState.copy(repCount = newRepCount, isResting = true, restRemaining = 30)
                }
            } else {
                currentState.copy(repCount = newRepCount, feedback = "Rep $newRepCount complete!")
            }
        }
    }

    fun finishWorkout() {
        saveAnalyticsSession(_uiState.value)
    }

    private fun saveAnalyticsSession(state: CoachState) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            val userEmail = SessionManager(getApplication()).getEmail() ?: ""
            
            // Get previous session for improvement calculation
            val lastSession = db.analyticsDao().getLatestSessionSync(userEmail)
            val improvement = if (lastSession != null) state.accuracy - lastSession.accuracy else 0
            
            val session = AnalyticsSession(
                userEmail = userEmail,
                sportName = state.sportName ?: "General",
                exerciseType = state.exerciseType.name,
                accuracy = state.accuracy,
                techScore = (state.accuracy * 0.8 + state.movementQuality * 0.2).toInt(),
                repCount = state.repCount,
                durationSeconds = state.durationSeconds,
                calories = state.calories,
                aiFeedback = state.feedback + if (improvement != 0) "\nImprovement: ${if (improvement > 0) "+" else ""}$improvement%" else ""
            )
            db.analyticsDao().saveSession(session)
            
            // Sync to Firestore
            try {
                if (_uiState.value.isOnline) {
                    val firestoreRepo = com.example.prathibhascanfinal.data.repository.FirestoreRepository()
                    firestoreRepo.saveEntity("analytics_sessions", System.currentTimeMillis().toString(), session)
                } else {
                    android.util.Log.w("FIRESTORE_SYNC", "SKIPPED: Device offline. Analytics session saved locally only.")
                }
            } catch (e: Exception) {
                android.util.Log.e("FIRESTORE_SYNC", "ERROR: Unexpected failure during sync initialization: ${e.message}")
            }
            
            // Update User XP and Stats
            val user = db.userDao().getUserByEmail(session.userEmail)
            user?.let {
                val updatedUser = it.copy(
                    totalXP = it.totalXP + (state.repCount * 5) + 50,
                    technicalImpactScore = (it.technicalImpactScore + state.accuracy) / 2.0
                )
                db.userDao().insertUser(updatedUser)
                // Also sync user profile
                if (_uiState.value.isOnline) {
                    com.example.prathibhascanfinal.data.repository.FirestoreRepository().saveEntity("users", updatedUser.email, updatedUser)
                }
            }
        }
    }

    private fun startRestTimer() {
        viewModelScope.launch {
            while (_uiState.value.isResting) {
                kotlinx.coroutines.delay(1000)
                _uiState.update { currentState ->
                    val remaining = currentState.restRemaining - 1
                    if (remaining <= 0) {
                        currentState.copy(isResting = false, currentSet = currentState.currentSet + 1, repCount = 0)
                    } else {
                        currentState.copy(restRemaining = remaining)
                    }
                }
            }
        }
    }

    fun resetDetection() {
        detectionStartTime = 0
        poseSmoother.reset()
        _uiState.update { it.copy(personDetected = false, countdown = 0, feedback = "Resetting detection...") }
    }

    private var lastGeminiTime = 0L
    private var detectionStartTime = 0L
    private val DETECTION_THRESHOLD_MS = 500L // Reduced for faster feedback
    
    fun processPoseResults(result: PoseLandmarkerResult) {
        val smoothedLandmarksList = poseSmoother.getSmoothedLandmarks(result)
        
        if (smoothedLandmarksList.isEmpty()) {
            detectionStartTime = 0
            if (_uiState.value.personDetected && _uiState.value.countdown == 0) {
                // Auto-pause if person lost during exercise
                _uiState.update { it.copy(feedback = "Person lost. Please stand in frame.") }
            } else {
                _uiState.update { it.copy(personDetected = false, visibilityStatus = "RED", bodyVisibilityMessage = "Please stand in frame") }
            }
            return
        }

        val landmarks = smoothedLandmarksList[0]
        
        // ALWAYS update landmarks for rendering first, even if detection hasn't fully locked in
        _uiState.update { it.copy(userLandmarks = landmarks) }

        // 0. Auto-Recognition Logic (If no exercise selected)
        if (_uiState.value.exerciseType == ExerciseType.NONE) {
            autoDetectExercise(landmarks)
            return
        }

        // 1. Person Detection with Hysteresis
        if (!_uiState.value.personDetected) {
            if (detectionStartTime == 0L) {
                detectionStartTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - detectionStartTime > DETECTION_THRESHOLD_MS) {
                _uiState.update { it.copy(personDetected = true, visibilityStatus = "GREEN", bodyVisibilityMessage = "Person Detected!") }
                startCountdown()
            }
            return
        }

        // 2. Countdown Lock
        if (_uiState.value.countdown > 0) return

        // 3. Strategy-based analysis
        val strategy = currentStrategy ?: return
        
        // Initialize start time if this is the first frame after countdown
        if (startTime == 0L) startTime = System.currentTimeMillis()
        
        val analysisResult = strategy.process(landmarks, currentExerciseState) {
            handleRepetitionComplete()
        }
        
        currentExerciseState = analysisResult.newState
        
        val ghost = strategy.generateGhost(landmarks, currentExerciseState)
        val similarity = strategy.calculateSimilarity(landmarks, ghost)
        
        // 4. AI Coaching Engine (Refined for Demo)
        val phaseText = when(currentExerciseState) {
            ExerciseState.MOVING_DOWN -> "DESCENDING"
            ExerciseState.MID_VERIFIED -> "BOTTOM"
            ExerciseState.MOVING_UP -> "ASCENDING"
            ExerciseState.START_VERIFIED -> "READY"
            ExerciseState.COMPLETED -> "REP COMPLETE"
            else -> "STABLE"
        }
        
        val displayFeedback = if (analysisResult.feedback.isNotEmpty()) {
             "[$phaseText] ${analysisResult.feedback}"
        } else {
             phaseText
        }
        
        // Use a combination of similarity and strategy alignment for score
        val formScore = if (analysisResult.alignmentScore > 0) {
            (similarity * 0.4 + analysisResult.alignmentScore * 0.6).toInt()
        } else {
            similarity
        }

        val visibilityStatus = when {
            formScore >= 80 -> "GREEN"
            formScore >= 50 -> "YELLOW"
            else -> "RED"
        }

        val similarityLabel = PoseMathUtils.getSimilarityLabel(formScore)

        // Trigger Gemini API for motivational/deep coaching if form is poor
        if (System.currentTimeMillis() - lastGeminiTime > 12000 && formScore < 60) {
            requestGeminiCoaching(strategy.exerciseType, formScore, analysisResult.feedback)
            lastGeminiTime = System.currentTimeMillis()
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000
        val calories = calculateCalories(_uiState.value.exerciseType, _uiState.value.repCount, duration)

        android.util.Log.d("AICoach", "Exercise: ${strategy.exerciseType}, State: $currentExerciseState, Angle: ${analysisResult.currentAngle}")

        _uiState.update {
            it.copy(
                feedback = displayFeedback,
                currentAngle = analysisResult.currentAngle,
                currentAngles = analysisResult.currentAngles,
                incorrectJoints = analysisResult.incorrectJoints,
                warningJoints = analysisResult.warningJoints,
                userLandmarks = landmarks,
                ghostLandmarks = ghost,
                accuracy = formScore,
                visibilityStatus = visibilityStatus,
                bodyVisibilityMessage = similarityLabel,
                durationSeconds = duration,
                calories = calories,
                exerciseState = currentExerciseState
            )
        }
    }

    private fun autoDetectExercise(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        // Only auto-detect if landmark confidence is high enough
        if (landmarks[24].visibility().orElse(0f) < 0.6f) return
        
        val hipKneeAnkle = PoseMathUtils.calculateAngle(landmarks[24], landmarks[26], landmarks[28])
        val shoulderElbowWrist = PoseMathUtils.calculateAngle(landmarks[12], landmarks[14], landmarks[16])
        val legsApart = Math.abs(landmarks[27].x() - landmarks[28].x()) > 0.35f
        
        when {
            hipKneeAnkle < 110 -> setExercise(ExerciseType.SQUATS)
            shoulderElbowWrist < 110 && landmarks[24].y() > landmarks[12].y() -> setExercise(ExerciseType.PUSH_UPS)
            legsApart && landmarks[15].y() < landmarks[11].y() -> setExercise(ExerciseType.JUMPING_JACKS)
        }
    }

    private fun requestGeminiCoaching(type: ExerciseType, accuracy: Int, errorFeedback: String) {
        viewModelScope.launch {
            val prompt = "As an expert sports coach, provide a 1-sentence correction for $type with accuracy $accuracy%. Error noted: $errorFeedback. Be motivational."
            val responseText = geminiRepository.sendMessage(prompt)
            _uiState.update { it.copy(feedback = responseText) }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(countdown = i, feedback = "$i...") }
                kotlinx.coroutines.delay(1000)
            }
            _uiState.update { it.copy(countdown = 0, feedback = "START!") }
        }
    }

    private fun calculateCalories(type: ExerciseType, reps: Int, duration: Long): Double {
        val metValue = MET_VALUES[type] ?: 5.0
        val weightKg = 70.0 // Default weight
        val caloriesFromDuration = (metValue * 3.5 * weightKg / 200) * (duration / 60.0)
        val caloriesFromReps = reps * (metValue * 0.05) // Heuristic: 0.05 cals per MET per rep
        return caloriesFromDuration + caloriesFromReps
    }
}
