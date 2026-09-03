package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import java.util.*

class AcademyAthleteRegistrationActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: com.example.prathibhascanfinal.data.repository.AcademyRepository

    private var photoUri: Uri? = null
    private var docUris = mutableMapOf<String, String>()

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            findViewById<ImageView>(R.id.iv_athlete_photo).setImageURI(uri)
        }
    }

    private var currentDocType = ""
    private var isEditMode = false
    private var existingAthleteId = 0
    private var existingAthlete: AcademyAthlete? = null

    private val pickDoc = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            docUris[currentDocType] = uri.toString()
            Toast.makeText(this, "$currentDocType Attached", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_athlete_registration)
        setupEdgeToEdge(findViewById(R.id.athlete_reg_root))

        repository = com.example.prathibhascanfinal.data.repository.AcademyRepository(this)
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        existingAthleteId = intent.getIntExtra("ATHLETE_ID", 0)

        setupSpinners()
        setupDatePicker()
        setupBmiCalculator()
        setupDocUploads()
        
        findViewById<ImageView>(R.id.iv_athlete_photo).setOnClickListener {
            pickPhoto.launch("image/*")
        }

        if (isEditMode) {
            loadAthleteData()
            findViewById<TextView>(R.id.tv_reg_title)?.text = "Edit Athlete Profile"
            findViewById<Button>(R.id.btn_save_athlete)?.text = "UPDATE ATHLETE"
        }

        intent.getStringExtra("PRE_SELECT_SPORT")?.let { sport ->
            findViewById<EditText>(R.id.et_sport_domain).setText(sport)
        }

        val btnSave = findViewById<Button>(R.id.btn_save_athlete)
        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            saveAthlete()
        }
    }

    private fun loadAthleteData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyAthleteRegistrationActivity)
            val athlete = db.academyManagementDao().getAthleteById(existingAthleteId)
            athlete?.let { a ->
                existingAthlete = a
                findViewById<EditText>(R.id.et_athlete_name).setText(a.fullName)
                findViewById<EditText>(R.id.et_dob).setText(a.dob)
                findViewById<EditText>(R.id.et_age).setText(a.age.toString())
                findViewById<EditText>(R.id.et_height).setText(a.heightCm.toString())
                findViewById<EditText>(R.id.et_weight).setText(a.weightKg.toString())
                findViewById<EditText>(R.id.et_bmi).setText(a.bmi.toString())
                findViewById<EditText>(R.id.et_blood_group).setText(a.bloodGroup)
                findViewById<EditText>(R.id.et_admission_no).setText(a.admissionNumber)
                findViewById<EditText>(R.id.et_nationality).setText(a.nationality)
                findViewById<EditText>(R.id.et_address).setText(a.address)
                findViewById<EditText>(R.id.et_parent_details).setText(a.parentName)
                findViewById<EditText>(R.id.et_parent_phone).setText(a.parentPhone)
                findViewById<EditText>(R.id.et_contact_number).setText(a.contactNumber)
                findViewById<EditText>(R.id.et_email).setText(a.email)
                findViewById<EditText>(R.id.et_aadhaar_id).setText(a.identityId)
                findViewById<EditText>(R.id.et_sport_domain).setText(a.sportDomain)
                findViewById<EditText>(R.id.et_secondary_sport).setText(a.secondarySport ?: "")
                findViewById<EditText>(R.id.et_position).setText(a.position)
                findViewById<EditText>(R.id.et_exp_years).setText(a.experienceYears.toString())
                findViewById<EditText>(R.id.et_medical_history).setText(a.medicalHistory)
                
                // Set Spinners
                val genderPos = (findViewById<Spinner>(R.id.sp_gender).adapter as ArrayAdapter<String>).getPosition(a.gender)
                findViewById<Spinner>(R.id.sp_gender).setSelection(genderPos)
                
                val sidePos = (findViewById<Spinner>(R.id.sp_dominant_side).adapter as ArrayAdapter<String>).getPosition(a.dominantSide)
                findViewById<Spinner>(R.id.sp_dominant_side).setSelection(sidePos)
            }
        }
    }

    private fun setupSpinners() {
        val genders = arrayOf("Male", "Female", "Other")
        val gAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        gAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        findViewById<Spinner>(R.id.sp_gender).adapter = gAdapter

        val sides = arrayOf("Right", "Left")
        val sAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sides)
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        findViewById<Spinner>(R.id.sp_dominant_side).adapter = sAdapter
    }

    private fun setupDatePicker() {
        val etDob = findViewById<EditText>(R.id.et_dob)
        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                val dob = String.format("%04d-%02d-%02d", y, m + 1, d)
                etDob.setText(dob)
                
                // Calculate age
                val age = year - y
                findViewById<EditText>(R.id.et_age).setText(age.toString())
            }, year, month, day).show()
        }
    }

    private fun setupBmiCalculator() {
        val etHeight = findViewById<EditText>(R.id.et_height)
        val etWeight = findViewById<EditText>(R.id.et_weight)
        val etBmi = findViewById<EditText>(R.id.et_bmi)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val h = etHeight.text.toString().toDoubleOrNull() ?: 0.0
                val w = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                if (h > 0.0 && w > 0.0) {
                    val heightInMeters = h / 100.0
                    val bmi = w / (heightInMeters * heightInMeters)
                    etBmi.setText(getString(R.string.bmi_format, bmi))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etHeight.addTextChangedListener(watcher)
        etWeight.addTextChangedListener(watcher)
    }

    private fun setupDocUploads() {
        findViewById<Button>(R.id.btn_upload_id_proof).setOnClickListener {
            currentDocType = "ID_Proof"
            pickDoc.launch("*/*")
        }
        findViewById<Button>(R.id.btn_upload_medical).setOnClickListener {
            currentDocType = "Medical_Cert"
            pickDoc.launch("*/*")
        }
        findViewById<Button>(R.id.btn_upload_consent).setOnClickListener {
            currentDocType = "Parent_Consent"
            pickDoc.launch("*/*")
        }
    }

    private fun saveAthlete() {
        val name = findViewById<EditText>(R.id.et_athlete_name).text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.btn_save_athlete).isEnabled = true
            return
        }

        lifecycleScope.launch {
            try {
                val imageUrl = if (photoUri != null) {
                    FirebaseManager.uploadFile(photoUri!!, "athletes/${System.currentTimeMillis()}.jpg")
                } else existingAthlete?.photoUri
                
                val session = SessionManager(this@AcademyAthleteRegistrationActivity)
                val email = session.getEmail() ?: ""
                val academy = AppDatabase.getDatabase(this@AcademyAthleteRegistrationActivity).academyDao().getAcademyByEmail(email)
                
                // Use intent ID first, then DB ID, finally error
                val academyIdToUse = intent.getIntExtra("ACADEMY_ID", 0).takeIf { it > 0 } 
                    ?: academy?.id 
                    ?: 0

                if (academyIdToUse <= 0) {
                    Toast.makeText(this@AcademyAthleteRegistrationActivity, "Critical Error: Academy record not found. Please complete profile setup.", Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.btn_save_athlete).isEnabled = true
                    return@launch
                }

                val athlete = AcademyAthlete(
                    athleteId = if (isEditMode) existingAthleteId else 0,
                    fullName = name,
                    photoUri = imageUrl,
                    dob = findViewById<EditText>(R.id.et_dob).text.toString(),
                    age = findViewById<EditText>(R.id.et_age).text.toString().toIntOrNull() ?: 0,
                    gender = findViewById<Spinner>(R.id.sp_gender).selectedItem.toString(),
                    heightCm = findViewById<EditText>(R.id.et_height).text.toString().toDoubleOrNull() ?: 0.0,
                    weightKg = findViewById<EditText>(R.id.et_weight).text.toString().toDoubleOrNull() ?: 0.0,
                    bmi = findViewById<EditText>(R.id.et_bmi).text.toString().toDoubleOrNull() ?: 0.0,
                    bloodGroup = findViewById<EditText>(R.id.et_blood_group).text.toString(),
                    admissionNumber = findViewById<EditText>(R.id.et_admission_no).text.toString(),
                    nationality = findViewById<EditText>(R.id.et_nationality).text.toString(),
                    address = findViewById<EditText>(R.id.et_address).text.toString(),
                    parentName = findViewById<EditText>(R.id.et_parent_details).text.toString(),
                    parentPhone = findViewById<EditText>(R.id.et_parent_phone).text.toString(),
                    contactNumber = findViewById<EditText>(R.id.et_contact_number).text.toString(),
                    email = findViewById<EditText>(R.id.et_email).text.toString(),
                    identityId = findViewById<EditText>(R.id.et_aadhaar_id).text.toString(),
                    sportDomain = findViewById<EditText>(R.id.et_sport_domain).text.toString(),
                    secondarySport = findViewById<EditText>(R.id.et_secondary_sport).text.toString(),
                    position = findViewById<EditText>(R.id.et_position).text.toString(),
                    experienceYears = findViewById<EditText>(R.id.et_exp_years).text.toString().toIntOrNull() ?: 0,
                    dominantSide = findViewById<Spinner>(R.id.sp_dominant_side).selectedItem.toString(),
                    medicalHistory = findViewById<EditText>(R.id.et_medical_history).text.toString(),
                    academyId = academyIdToUse,
                    verificationStatus = if (isEditMode) (existingAthlete?.verificationStatus ?: "Pending") else "Pending",
                    documentUris = com.google.gson.Gson().toJson(docUris)
                )

                val success = repository.registerAthlete(athlete)
                if (success) {
                    val message = if (isEditMode) "✔ Athlete Profile Updated Successfully" else "✔ Athlete Registered Successfully"
                    Toast.makeText(this@AcademyAthleteRegistrationActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@AcademyAthleteRegistrationActivity, "Failed to save athlete to cloud. Check internet.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AcademyAthleteRegistrationActivity, "Error saving athlete: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                findViewById<Button>(R.id.btn_save_athlete).isEnabled = true
            }
        }
    }
}

