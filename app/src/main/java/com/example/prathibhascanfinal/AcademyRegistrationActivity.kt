package com.example.prathibhascanfinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.launch

class AcademyRegistrationActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private var currentStep = 1
    private val totalSteps = 4
    
    private lateinit var layoutStep1: View
    private lateinit var layoutStep2: View
    private lateinit var layoutStep3: View
    private lateinit var layoutFinal: View
    
    private lateinit var tvStepTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var coachListContainer: LinearLayout

    private var licenseUri: Uri? = null
    
    private val pickLicense = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            licenseUri = uri
            findViewById<Button>(R.id.btn_upload_license)?.text = getString(R.string.license_attached)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_academy_registration)
            findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

            initViews()
            setupNavigation()
            addCoachEntry() // Add initial coach
        } catch (_: Exception) {
            Toast.makeText(this, "System UI Error. Please restart app.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        layoutStep1 = findViewById(R.id.layout_step1)
        layoutStep2 = findViewById(R.id.layout_step2)
        layoutStep3 = findViewById(R.id.layout_step3)
        layoutFinal = findViewById(R.id.layout_step_final)
        
        tvStepTitle = findViewById(R.id.tv_step_title)
        progressBar = findViewById(R.id.registration_progress)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next_submit)
        coachListContainer = findViewById(R.id.coach_list_container)

        findViewById<Button>(R.id.btn_upload_license)?.setOnClickListener {
            pickLicense.launch("*/*") 
        }

        findViewById<Button>(R.id.btn_add_coach)?.setOnClickListener {
            addCoachEntry()
        }
    }

    private fun addCoachEntry() {
        try {
            val coachView = LayoutInflater.from(this).inflate(R.layout.item_coach_entry, coachListContainer, false)
            val btnCert = coachView.findViewById<Button>(R.id.btn_upload_coach_cert)
            val tvStatus = coachView.findViewById<TextView>(R.id.tv_cert_status)
            
            btnCert?.setOnClickListener {
                pickLicense.launch("*/*")
                tvStatus?.text = getString(R.string.document_attached)
            }
            coachListContainer.addView(coachView)
        } catch (e: Exception) {}
    }

    private fun setupNavigation() {
        btnNext.setOnClickListener {
            if (currentStep < totalSteps) {
                if (validateCurrentStep()) {
                    currentStep++
                    updateStepUI()
                }
            } else {
                submitForm()
            }
        }

        btnPrev.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI()
            }
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                val name = findViewById<EditText>(R.id.et_academy_name)?.text.toString()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Academy Name is required", Toast.LENGTH_SHORT).show()
                    false
                } else true
            }
            else -> true
        }
    }

    private fun updateStepUI() {
        layoutStep1.isVisible = currentStep == 1
        layoutStep2.isVisible = currentStep == 2
        layoutStep3.isVisible = currentStep == 3
        layoutFinal.isVisible = currentStep == 4

        btnPrev.visibility = if (currentStep > 1) View.VISIBLE else View.INVISIBLE
        btnNext.text = if (currentStep == totalSteps) "Complete Registration" else "Next Step"
        
        progressBar.progress = (currentStep * 100) / totalSteps
        tvStepTitle.text = when(currentStep) {
            1 -> "Step 1: Academy Core"
            2 -> "Step 2: AI & Technology"
            3 -> "Step 3: Coaching Staff"
            else -> "Step 4: Compliance"
        }
    }

    private fun submitForm() {
        val academyName = findViewById<EditText>(R.id.et_academy_name)?.text?.toString() ?: ""
        val address = findViewById<EditText>(R.id.et_academy_address)?.text?.toString() ?: ""
        val city = findViewById<EditText>(R.id.et_academy_city)?.text?.toString() ?: ""
        val district = findViewById<EditText>(R.id.et_academy_district)?.text?.toString() ?: ""
        val state = findViewById<EditText>(R.id.et_academy_state)?.text?.toString() ?: ""
        val director = findViewById<EditText>(R.id.et_director_name)?.text?.toString() ?: ""

        val session = SessionManager(this)
        val userEmail = session.getEmail() ?: ""

        // Loading State
        btnNext.isEnabled = false
        btnNext.text = "Finalizing Setup..."
        
        lifecycleScope.launch {
            try {
                val repository = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyRegistrationActivity)
                
                val academy = Academy(
                    academyName = academyName,
                    directorName = director,
                    contactEmail = userEmail,
                    registrationNumber = findViewById<EditText>(R.id.et_reg_no)?.text?.toString() ?: "",
                    address = address,
                    city = city,
                    district = district,
                    state = state,
                    specializedDomains = getSelectedDomains(),
                    primaryMetrics = findViewById<EditText>(R.id.et_target_metrics)?.text?.toString() ?: "",
                    dpoContact = findViewById<EditText>(R.id.et_dpo_email)?.text?.toString() ?: "",
                    hasParentalConsentSystem = findViewById<CheckBox>(R.id.cb_consent_system)?.isChecked ?: false,
                    licenseUri = licenseUri?.toString(),
                    lastUpdated = System.currentTimeMillis()
                )

                val coaches = mutableListOf<Coach>()
                for (i in 0 until coachListContainer.childCount) {
                    val view = coachListContainer.getChildAt(i)
                    val name = view.findViewById<EditText>(R.id.et_coach_name)?.text?.toString() ?: ""
                    if (name.isNotEmpty()) {
                        coaches.add(
                            Coach(
                                academyId = 0, 
                                name = name, 
                                specialization = view.findViewById<EditText>(R.id.et_coach_specialization)?.text?.toString() ?: "",
                                email = view.findViewById<EditText>(R.id.et_coach_email)?.text?.toString() ?: "",
                                phone = view.findViewById<EditText>(R.id.et_coach_phone)?.text?.toString() ?: ""
                            )
                        )
                    }
                }

                // 1. Save and Sync
                val savedId = repository.registerAcademy(academy, coaches)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AcademyRegistrationActivity, getString(R.string.academy_success), Toast.LENGTH_SHORT).show()

                    // 2. Update session with new ID
                    session.saveSession(userEmail, academyName, "PR-ACD-$savedId", "Academy", null)
                    val intent = Intent(this@AcademyRegistrationActivity, AcademyPortalActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                Log.e("REG_ERROR", "Submission failed", e)
                Toast.makeText(this@AcademyRegistrationActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                btnNext.isEnabled = true
                btnNext.text = "Complete Registration"
            }
        }
    }

    private fun getSelectedDomains(): String {
        val domains = mutableListOf<String>()
        if (findViewById<CheckBox>(R.id.cb_domain_indoor)?.isChecked == true) domains.add("Indoor")
        if (findViewById<CheckBox>(R.id.cb_domain_outdoor)?.isChecked == true) domains.add("Outdoor")
        if (findViewById<CheckBox>(R.id.cb_domain_athletics)?.isChecked == true) domains.add("Athletics")
        return domains.joinToString(", ")
    }
}

