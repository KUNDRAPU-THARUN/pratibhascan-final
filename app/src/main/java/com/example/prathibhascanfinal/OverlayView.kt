package com.example.prathibhascanfinal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var userLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null
    private var ghostLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null
    private var incorrectJoints: Set<Int> = emptySet()
    private var warningJoints: Set<Int> = emptySet()
    private var currentAngles: Map<String, Double> = emptyMap()
    private var currentState: String = "READY"
    
    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var errorPaint = Paint()
    private var warningPaint = Paint()
    private var ghostPaint = Paint()
    private var ghostPointPaint = Paint()
    private var textPaint = Paint()

    private var correctPaint = Paint()
    private var ghostLinePaint = Paint()
    
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var isFrontCamera: Boolean = true 
    
    private var showGhost: Boolean = true
    private var ghostOpacity: Int = 160 
    private var ghostRotation: Float = 0f
    private var showAngles: Boolean = true
    private var splitScreenMode: Boolean = false
    private var debugMode: Boolean = false

    private val transformMatrix = android.graphics.Matrix()

    init {
        initPaints()
    }

    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        invalidate()
    }

    fun setSplitScreenMode(enabled: Boolean) {
        splitScreenMode = enabled
        invalidate()
    }

    private fun initPaints() {
        linePaint.color = Color.parseColor("#00E5FF") // Electric Cyan
        linePaint.strokeWidth = 12f
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.isAntiAlias = true
        linePaint.alpha = 240
        linePaint.setShadowLayer(5f, 0f, 0f, Color.BLACK)

        pointPaint.color = Color.WHITE
        pointPaint.strokeWidth = 16f
        pointPaint.style = Paint.Style.FILL
        pointPaint.isAntiAlias = true
        pointPaint.setShadowLayer(8f, 0f, 0f, Color.BLACK)

        errorPaint.color = Color.parseColor("#FF1744") // Vivid Red
        errorPaint.strokeWidth = 14f
        errorPaint.style = Paint.Style.STROKE
        errorPaint.strokeCap = Paint.Cap.ROUND
        errorPaint.isAntiAlias = true
        errorPaint.setShadowLayer(20f, 0f, 0f, Color.RED)

        warningPaint.color = Color.parseColor("#FFEA00") // Neon Yellow
        warningPaint.strokeWidth = 12f
        warningPaint.style = Paint.Style.STROKE
        warningPaint.strokeCap = Paint.Cap.ROUND
        warningPaint.isAntiAlias = true
        warningPaint.setShadowLayer(10f, 0f, 0f, Color.YELLOW)

        correctPaint.color = Color.parseColor("#00E676") // Spring Green
        correctPaint.strokeWidth = 12f
        correctPaint.style = Paint.Style.STROKE
        correctPaint.strokeCap = Paint.Cap.ROUND
        correctPaint.isAntiAlias = true
        correctPaint.setShadowLayer(15f, 0f, 0f, Color.GREEN)

        ghostPaint.color = Color.argb(ghostOpacity, 0, 229, 255) 
        ghostPaint.strokeWidth = 8f
        ghostPaint.style = Paint.Style.STROKE
        ghostPaint.strokeCap = Paint.Cap.ROUND
        ghostPaint.isAntiAlias = true
        ghostPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f)
        
        ghostLinePaint.color = Color.argb(ghostOpacity, 0, 229, 255)
        ghostLinePaint.strokeWidth = 3f
        ghostLinePaint.style = Paint.Style.STROKE
        ghostLinePaint.isAntiAlias = true

        ghostPointPaint.color = Color.WHITE
        ghostPointPaint.strokeWidth = 8f
        ghostPointPaint.style = Paint.Style.FILL
        ghostPointPaint.isAntiAlias = true

        textPaint.color = Color.WHITE
        textPaint.textSize = 36f
        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true
        textPaint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        textPaint.setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        userLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null,
        incorrect: Set<Int> = emptySet(),
        warnings: Set<Int> = emptySet(),
        angles: Map<String, Double> = emptyMap(),
        isFront: Boolean = true,
        repState: String = "READY"
    ) {
        results = poseLandmarkerResults
        this.userLandmarks = userLandmarks
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        this.incorrectJoints = incorrect
        this.warningJoints = warnings
        this.currentAngles = angles
        this.isFrontCamera = isFront
        this.currentState = repState

        updateTransformationMatrix()
        invalidate()
    }

    private fun updateTransformationMatrix() {
        // Use unified Matrix helper from PoseMathUtils
        // Since frames are already rotated in PoseLandmarkerHelper, we pass 0 degrees rotation
        transformMatrix.set(
            PoseMathUtils.getTransformationMatrix(
                width, height, imageWidth, imageHeight, 0, isFrontCamera
            )
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTransformationMatrix()
    }

    fun setGhostLandmarks(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>?) {
        this.ghostLandmarks = landmarks
        invalidate()
    }
    
    fun setGhostMode(enabled: Boolean, opacity: Int = 160, rotation: Float = 0f) {
        showGhost = enabled
        ghostOpacity = opacity
        ghostRotation = rotation
        ghostPaint.alpha = ghostOpacity
        ghostPointPaint.alpha = ghostOpacity
        invalidate()
    }

    fun toggleAngles(visible: Boolean) {
        showAngles = visible
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (splitScreenMode) {
            drawSplitMode(canvas)
        } else {
            drawNormalMode(canvas)
        }
    }

    private fun drawNormalMode(canvas: Canvas) {
        if (showGhost) {
            ghostLandmarks?.let { landmarks ->
                draw3DGhost(canvas, landmarks)
            }
        }

        userLandmarks?.let { landmarks ->
            drawUserPose(canvas, landmarks)
        } ?: results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                drawUserPose(canvas, landmark)
            }
        }
    }

    private fun drawSplitMode(canvas: Canvas) {
        val halfWidth = width / 2f
        
        // Draw User on the left (Adjust matrix for half width)
        val userMatrix = PoseMathUtils.getTransformationMatrix(
            halfWidth.toInt(), height, imageWidth, imageHeight, 0, isFrontCamera
        )
        
        userLandmarks?.let { landmarks ->
            drawUserPose(canvas, landmarks, userMatrix)
        } ?: results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                drawUserPose(canvas, landmark, userMatrix)
            }
        }
        
        // Draw Ghost on the right (Adjust matrix for half width and translate)
        val ghostMatrix = PoseMathUtils.getTransformationMatrix(
            halfWidth.toInt(), height, imageWidth, imageHeight, 0, false // No mirroring on ghost in split
        )
        ghostMatrix.postTranslate(halfWidth, 0f)
        
        ghostLandmarks?.let { landmarks ->
            draw3DGhost(canvas, landmarks, ghostMatrix)
        }
        
        // Split line
        canvas.drawLine(halfWidth, 0f, halfWidth, height.toFloat(), warningPaint)
    }

    private fun drawUserPose(
        canvas: Canvas, 
        landmark: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        matrix: android.graphics.Matrix = transformMatrix
    ) {
        // 1. Draw Skeleton Connections
        PoseLandmarkMapping.SKELETON_CONNECTIONS.forEach { connection ->
            val start = landmark[connection.first]
            val end = landmark[connection.second]
            
            val startConf = start.visibility().orElse(0f)
            val endConf = end.visibility().orElse(0f)
            
            // Render connections even at low confidence, but fade them out
            val startPos = mapPoint(start.x(), start.y(), matrix)
            val endPos = mapPoint(end.x(), end.y(), matrix)

            val isStartError = incorrectJoints.contains(connection.first)
            val isEndError = incorrectJoints.contains(connection.second)
            
            val paint = when {
                isStartError || isEndError -> errorPaint
                warningJoints.contains(connection.first) || warningJoints.contains(connection.second) -> warningPaint
                else -> linePaint
            }
            
            val originalAlpha = paint.alpha
            // Fade out if either end has low confidence (< 0.5)
            val minConf = Math.min(startConf, endConf)
            if (minConf < 0.5f) {
                paint.alpha = (originalAlpha * (minConf * 2.0f)).toInt().coerceIn(40, originalAlpha)
            }
            
            canvas.drawLine(startPos[0], startPos[1], endPos[0], endPos[1], paint)
            paint.alpha = originalAlpha
        }

        // 2. Draw Landmark Points
        for (i in landmark.indices) {
            val normalizedLandmark = landmark[i]
            val conf = normalizedLandmark.visibility().orElse(0f)

            val pos = mapPoint(normalizedLandmark.x(), normalizedLandmark.y(), matrix)
            val px = pos[0]
            val py = pos[1]
            
            val isError = incorrectJoints.contains(i)
            val isWarning = warningJoints.contains(i)
            
            val paint = when {
                isError -> errorPaint
                isWarning -> warningPaint
                else -> pointPaint
            }
            
            val originalAlpha = paint.alpha
            // Dim points with low visibility
            if (conf < 0.5f) {
                paint.alpha = (originalAlpha * (conf * 2.0f)).toInt().coerceIn(60, originalAlpha)
            }
            
            if (isError || isWarning) {
                // Large Pulse for Form Issues
                val pulseSize = 14f + (System.currentTimeMillis() % 800) / 40f
                val pulsePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    alpha = (100 + 155 * Math.abs(Math.sin(System.currentTimeMillis() / 150.0))).toInt()
                }
                canvas.drawCircle(px, py, pulseSize, pulsePaint)
                canvas.drawCircle(px, py, 12f, paint)
            } else {
                // Professional Point Style
                val isKeyJoint = i in listOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)
                if (isKeyJoint) {
                     canvas.drawCircle(px, py, 10f, correctPaint)
                     canvas.drawCircle(px, py, 6f, pointPaint)
                } else {
                     canvas.drawCircle(px, py, 5f, pointPaint)
                }
            }
            
            paint.alpha = originalAlpha
            
            // 3. Render Joint Angles (If enabled)
            if (showAngles) {
                val angleText = when(i) {
                    26 -> currentAngles["RKnee"] ?: currentAngles["Knee"]
                    25 -> currentAngles["LKnee"]
                    14 -> currentAngles["RElbow"] ?: currentAngles["Elbow"]
                    13 -> currentAngles["LElbow"]
                    24 -> currentAngles["RHip"] ?: currentAngles["Hip"]
                    23 -> currentAngles["LHip"]
                    else -> null
                }?.toInt()?.toString()
                
                angleText?.let { 
                    textPaint.color = if (isError) Color.RED else Color.WHITE
                    canvas.drawText(it, px, py - 25f, textPaint) 
                }
            }

            if (debugMode) {
                textPaint.textSize = 22f
                val conf = normalizedLandmark.visibility().orElse(0f)
                val debugText = "$i ${PoseLandmarkMapping.getName(i)}\nX:${String.format(java.util.Locale.US, "%.2f", normalizedLandmark.x())} Y:${String.format(java.util.Locale.US, "%.2f", normalizedLandmark.y())}\nC:${String.format(java.util.Locale.US, "%.2f", conf)}"
                val lines = debugText.split("\n")
                lines.forEachIndexed { index, line ->
                    canvas.drawText(line, px + 20f, py + (index * 25f), textPaint)
                }
                
                // Draw Rep State info near the head
                if (i == 0) {
                    textPaint.textSize = 32f
                    textPaint.color = Color.YELLOW
                    canvas.drawText("STATE: $currentState", px - 50f, py - 100f, textPaint)
                }
                textPaint.textSize = 36f
            }
        }
    }

    private fun draw3DGhost(
        canvas: Canvas,
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        matrix: android.graphics.Matrix = transformMatrix
    ) {
        val isOverlay = !splitScreenMode
        val pulse = if (isOverlay) (150 + 100 * Math.sin(System.currentTimeMillis() / 250.0)).toInt() else ghostOpacity
        
        val ghostColor = if (results != null && !incorrectJoints.any()) {
            Color.argb(pulse, 74, 222, 128) // Neon Green
        } else {
            Color.argb(pulse, 0, 255, 255) // Cyber Cyan
        }
        ghostPaint.color = ghostColor
        ghostLinePaint.color = ghostColor
        ghostLinePaint.alpha = pulse / 2

        PoseLandmarkMapping.SKELETON_CONNECTIONS.forEach { connection ->
            val start = landmarks[connection.first]
            val end = landmarks[connection.second]
            
            val startPos = mapPoint(start.x(), start.y(), matrix)
            val endPos = mapPoint(end.x(), end.y(), matrix)

            val avgZ = (start.z() + end.z()) / 2f
            val zWeight = (1.0f - avgZ).coerceIn(0.6f, 1.4f)
            
            ghostPaint.strokeWidth = 6f * zWeight
            canvas.drawLine(startPos[0], startPos[1], endPos[0], endPos[1], ghostPaint)
            canvas.drawLine(startPos[0], startPos[1], endPos[0], endPos[1], ghostLinePaint)
        }
        
        landmarks.forEachIndexed { i, landmark ->
            val pos = mapPoint(landmark.x(), landmark.y(), matrix)
            val px = pos[0]
            val py = pos[1]
            val pzWeight = (1.0f - landmark.z()).coerceIn(0.6f, 1.4f)
            
            ghostPointPaint.color = Color.WHITE
            ghostPointPaint.alpha = pulse
            canvas.drawCircle(px, py, 4f * pzWeight, ghostPointPaint)
            
            if (i == 24 || i == 23 || i == 12 || i == 11) {
                val outerPaint = Paint(ghostPointPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = ghostColor
                }
                canvas.drawCircle(px, py, 10f * pzWeight, outerPaint)
            }
        }
        
        if (isOverlay) invalidate()
    }

    private fun mapPoint(x: Float, y: Float, matrix: android.graphics.Matrix): FloatArray {
        val pts = floatArrayOf(x, y)
        matrix.mapPoints(pts)
        return pts
    }
}
