package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HealthUpdateActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var userEmail: String
    private var healthCertUri: Uri? = null

    private val pickHealthCert = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            healthCertUri = uri
            findViewById<TextView>(R.id.tv_health_cert_status)?.text = getString(R.string.cert_attached)
            findViewById<TextView>(R.id.tv_health_cert_status)?.setTextColor(0xFFFBBF24.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_update)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        userEmail = SessionManager(this).getEmail() ?: ""

        val etHeart = findViewById<EditText>(R.id.et_heart_rate)
        val etSleep = findViewById<EditText>(R.id.et_sleep_hours)
        val sbHydration = findViewById<SeekBar>(R.id.sb_hydration)
        
        val tvProtein = findViewById<TextView>(R.id.tv_ai_protein)
        val tvCarbs = findViewById<TextView>(R.id.tv_ai_carbs)

        loadCurrentHealth(tvProtein, tvCarbs)

        findViewById<Button>(R.id.btn_upload_health_cert)?.setOnClickListener {
            pickHealthCert.launch("*/*")
        }

        findViewById<Button>(R.id.btn_save_health).setOnClickListener {
            saveHealth(
                etHeart.text.toString().toIntOrNull() ?: 72,
                etSleep.text.toString().toDoubleOrNull() ?: 8.0,
                sbHydration.progress / 10.0,
            )
        }

        findViewById<TextView>(R.id.btn_health_back).setOnClickListener { finish() }
    }

    private fun loadCurrentHealth(tvP: TextView, tvC: TextView) {
        lifecycleScope.launch {
            val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@HealthUpdateActivity) }
            val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
            user?.let {
                val protein = when(it.primaryDiscipline) {
                    "Wrestling", "Weightlifting" -> 160.0
                    "Athletics", "Football" -> 120.0
                    else -> 90.0
                }
                val carbs = when(it.primaryDiscipline) {
                    "Athletics", "Cycling" -> 400.0
                    else -> 250.0
                }
                tvP.text = getString(R.string.gram_format, protein)
                tvC.text = getString(R.string.gram_format, carbs)
                
                if (it.healthCertificateUri != null) {
                    findViewById<TextView>(R.id.tv_health_cert_status)?.text = getString(R.string.verified_doc_linked)
                    findViewById<TextView>(R.id.tv_health_cert_status)?.setTextColor(0xFF10B981.toInt())
                }
            }
        }
    }

    private fun saveHealth(heart: Int, sleep: Double, hydration: Double) {
        lifecycleScope.launch {
            try {
                val db = withContext(Dispatchers.IO) { AppDatabase.getDatabase(this@HealthUpdateActivity) }
                val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                user?.let {
                    val updated = it.copy(
                        heartRate = heart,
                        sleepHours = sleep,
                        hydrationLevel = hydration,
                        healthCertificateUri = healthCertUri?.toString() ?: it.healthCertificateUri
                    )
                    withContext(Dispatchers.IO) { db.userDao().insertUser(updated) }
                    Toast.makeText(this@HealthUpdateActivity, "Medical Passport Updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (_: Exception) {
                Toast.makeText(this@HealthUpdateActivity, "Sync Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

