package com.example.prathibhascanfinal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(
    val context: Context,
    var runningMode: RunningMode = RunningMode.IMAGE,
    var minPoseDetectionConfidence: Float = 0.5f,
    var minPoseTrackingConfidence: Float = 0.5f,
    var minPosePresenceConfidence: Float = 0.5f,
    var currentDelegate: Int = DELEGATE_GPU,
    var modelName: String = "pose_landmarker_heavy.task", // Default to heavy for accuracy
    var poseLandmarkerHelperListener: LandmarkerListener? = null
) {

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
        synchronized(bitmapPool) {
            for (i in bitmapPool.indices) {
                bitmapPool[i]?.recycle()
                bitmapPool[i] = null
            }
        }
    }

    fun isClose(): Boolean {
        return poseLandmarker == null
    }

    fun setupPoseLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()

        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }

        // The task file must be in the assets folder
        baseOptionBuilder.setModelAssetPath(modelName)

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                    .setMinTrackingConfidence(minPoseTrackingConfidence)
                    .setMinPosePresenceConfidence(minPosePresenceConfidence)
                    .setRunningMode(runningMode)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(context.applicationContext, options)
        } catch (e: IllegalStateException) {
            poseLandmarkerHelperListener?.onError(
                "Pose landmarker failed to initialize. See error logs for details"
            )
            Log.e(TAG, "MediaPipe failed to load the task with error: " + e.message)
        } catch (e: RuntimeException) {
            poseLandmarkerHelperListener?.onError(
                "Pose landmarker failed to initialize. See error logs for details"
            )
            Log.e(
                TAG,
                "Image classifier failed to load model with error: " + e.message
            )
        }
    }

    // Increase buffer pool for stability
    private val bitmapPool = Array<Bitmap?>(5) { null }
    private var poolIndex = 0

    fun detectLiveStream(
        bitmap: Bitmap,
        rotationDegrees: Int
    ) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis()

        val rotatedWidth = if (rotationDegrees % 180 == 0) bitmap.width else bitmap.height
        val rotatedHeight = if (rotationDegrees % 180 == 0) bitmap.height else bitmap.width
        
        // Get a bitmap from the pool with safe synchronization
        val targetBitmap = synchronized(bitmapPool) {
            val b = bitmapPool[poolIndex]
            val finalB = if (b == null || b.width != rotatedWidth || b.height != rotatedHeight) {
                b?.recycle()
                val newB = Bitmap.createBitmap(rotatedWidth, rotatedHeight, Bitmap.Config.ARGB_8888)
                bitmapPool[poolIndex] = newB
                newB
            } else {
                b
            }
            poolIndex = (poolIndex + 1) % bitmapPool.size
            finalB
        }
        
        val canvas = android.graphics.Canvas(targetBitmap)
        
        val drawMatrix = Matrix()
        drawMatrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        drawMatrix.postRotate(rotationDegrees.toFloat())
        drawMatrix.postTranslate(rotatedWidth / 2f, rotatedHeight / 2f)
        canvas.drawBitmap(bitmap, drawMatrix, null)

        val mpImage = BitmapImageBuilder(targetBitmap).build()
        detectAsync(mpImage, frameTime)
    }

    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        if (runningMode == RunningMode.LIVE_STREAM) {
            try {
                poseLandmarker?.detectAsync(mpImage, frameTime)
            } catch (e: Exception) {
                Log.e(TAG, "Detection failed: ${e.message}")
            } finally {
                // mpImage will be closed by MediaPipe internal callback once processing is done
                // in LIVE_STREAM mode to avoid concurrent access issues.
            }
        }
    }

    fun detectVideoFile(
        videoUri: android.net.Uri,
        inferenceIntervalMs: Long
    ): List<PoseLandmarkerResult> {
        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call detectVideoFile while not using RunningMode.VIDEO"
            )
        }

        val results = mutableListOf<PoseLandmarkerResult>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val videoLengthMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            
            var currentTimestampMs = 0L
            while (currentTimestampMs < videoLengthMs) {
                val bitmap = retriever.getFrameAtTime(currentTimestampMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    poseLandmarker?.detectForVideo(mpImage, currentTimestampMs)?.let {
                        results.add(it)
                    }
                }
                currentTimestampMs += inferenceIntervalMs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video detection failed", e)
        } finally {
            retriever.release()
        }

        return results
    }

    fun detectImage(bitmap: Bitmap): PoseLandmarkerResult? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" +
                        " while not using RunningMode.IMAGE"
            )
        }
        val mpImage = BitmapImageBuilder(bitmap).build()
        return poseLandmarker?.detect(mpImage)
    }

    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        if (result.landmarks().isNotEmpty()) {
            Log.d(TAG, "Pose Landmarks Detected: ${result.landmarks()[0].size} points. Inf time: ${inferenceTime}ms")
        } else {
            Log.v(TAG, "No landmarks detected in frame")
        }

        poseLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val TAG = "PoseLandmarkerHelper"
    }

    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(resultBundle: ResultBundle)
    }
}
