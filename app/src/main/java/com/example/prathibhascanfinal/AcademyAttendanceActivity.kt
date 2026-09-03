package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AcademyAttendanceActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var tvStatus: TextView
    private var academyId: Int = 1
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_attendance)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 1)

        previewView = findViewById(R.id.camera_preview)
        tvStatus = findViewById(R.id.tv_scan_status)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val value = barcode.rawValue
                        if (value != null) {
                            runOnUiThread {
                                handleScanResult(value)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScanResult(qrData: String) {
        // Expected format: "ATHLETE_ID:123" or "COACH_ID:456"
        tvStatus.text = getString(R.string.scanned_format, qrData)
        
        lifecycleScope.launch {
            try {
                val type = if (qrData.startsWith("ATHLETE_ID")) "Athlete" else "Coach"
                val id = qrData.split(":").lastOrNull()?.toIntOrNull() ?: 0
                
                val attendance = Attendance(
                    academyId = academyId,
                    entityType = type,
                    entityId = id,
                    isPresent = true,
                    qrCodeUsed = true
                )
                
                val repo = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyAttendanceActivity)
                repo.saveAttendance(attendance)
                Toast.makeText(this@AcademyAttendanceActivity, "Attendance marked for $type $id", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this@AcademyAttendanceActivity, "Invalid QR Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

