package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class AchievementVaultActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private var certUri: Uri? = null
    private var videoUri: Uri? = null
    private var tempUri: Uri? = null
    private lateinit var userEmail: String

    private val pickCert = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { certUri = it; updateStatus() }
    }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { videoUri = it; updateStatus() }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { certUri = tempUri; updateStatus() }
    }

    private val captureVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) { videoUri = tempUri; updateStatus() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievement_vault)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        userEmail = SessionManager(this).getEmail() ?: ""

        findViewById<Button>(R.id.btn_upload_cert).setSafeOnClickListener { 
            showSourcePicker("Certificate", true)
        }
        findViewById<Button>(R.id.btn_upload_proof_vid).setSafeOnClickListener { 
            showSourcePicker("Proof Video", false)
        }

        findViewById<Button>(R.id.btn_ai_verify).setSafeOnClickListener { performAIVerification() }
        findViewById<Button>(R.id.btn_submit_achievement).setSafeOnClickListener { submitAchievement() }
        findViewById<View>(R.id.btn_vault_back).setSafeOnClickListener { finish() }
    }

    private fun showSourcePicker(title: String, isImage: Boolean) {
        val options = arrayOf("Camera", "Gallery", "Files")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Upload $title")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission(isImage)
                    1 -> if (isImage) pickCert.launch("image/*") else pickVideo.launch("video/*")
                    2 -> if (isImage) pickCert.launch("*/*") else pickVideo.launch("*/*")
                }
            }
            .show()
    }

    private fun checkCameraPermission(isImage: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 3001)
        } else {
            launchCamera(isImage)
        }
    }

    private fun launchCamera(isImage: Boolean) {
        try {
            val prefix = if (isImage) "IMG_" else "VID_"
            val suffix = if (isImage) ".jpg" else ".mp4"
            val file = File.createTempFile(prefix, suffix, cacheDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            tempUri = uri
            if (isImage) takePicture.launch(uri) else captureVideo.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus() {
        val status = findViewById<TextView>(R.id.tv_bundle_status)
        val btnVerify = findViewById<Button>(R.id.btn_ai_verify)
        if (certUri != null && videoUri != null) {
            status.text = "Bundle Ready for AI Verification ✓"
            status.setTextColor(0xFF10B981.toInt()) // Green
            btnVerify.visibility = View.VISIBLE
        } else if (certUri != null || videoUri != null) {
            status.text = "Partial Bundle (Need both files)"
            status.setTextColor(0xFF3B82F6.toInt()) // Blue
            btnVerify.visibility = View.GONE
        }
    }

    private fun performAIVerification() {
        val uri = certUri ?: return
        Toast.makeText(this, "AI: Analyzing certificate authenticity...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient()
                val image = com.google.mlkit.vision.common.InputImage.fromFilePath(this@AchievementVaultActivity, uri)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val resultText = visionText.text
                        if (resultText.contains("Certificate", true) || resultText.contains("Achievement", true) || resultText.contains("Sports", true)) {
                            findViewById<TextView>(R.id.tv_bundle_status).text = "AI Verified: Valid Sports Credential ✓"
                            findViewById<TextView>(R.id.tv_bundle_status).setTextColor(0xFFFBBF24.toInt()) // Gold
                        } else {
                            findViewById<TextView>(R.id.tv_bundle_status).text = "AI Warning: Unclear document text."
                            findViewById<TextView>(R.id.tv_bundle_status).setTextColor(0xFFEF4444.toInt()) // Red
                        }
                    }
                    .addOnFailureListener {
                        findViewById<TextView>(R.id.tv_bundle_status).text = "AI Verification Failed. Try a clearer photo."
                    }
            } catch (e: Exception) {
                findViewById<TextView>(R.id.tv_bundle_status).text = "AI Verification error."
            }
        }
    }

    private fun submitAchievement() {
        val name = findViewById<EditText>(R.id.et_tournament_name).text.toString().trim()
        val year = findViewById<EditText>(R.id.et_event_year).text.toString().trim()
        val pos = findViewById<EditText>(R.id.et_position).text.toString().trim()

        if (name.isEmpty() || certUri == null || videoUri == null) {
            Toast.makeText(this, "Please complete all fields and attachments", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@AchievementVaultActivity)
                val achievement = Achievement(
                    userEmail = userEmail,
                    tournamentName = name,
                    eventYear = year,
                    position = pos,
                    discipline = "Athletics",
                    isVerified = true,
                    certificateUri = certUri.toString(),
                    proofVideoUri = videoUri.toString()
                )
                
                withContext(Dispatchers.IO) { 
                    db.achievementDao().insertAchievement(achievement)
                    val user = db.userDao().getUserByEmail(userEmail)
                    user?.let {
                        db.userDao().insertUser(it.copy(totalXP = it.totalXP + 300))
                    }
                }
                Toast.makeText(this@AchievementVaultActivity, "Timeline Updated! +300 XP", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AchievementVaultActivity, "Error saving achievement", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

