package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.data.repository.InstitutionRepository
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class InstitutionStudentRegistrationActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: InstitutionRepository

    private var photoUri: Uri? = null
    private var isEditMode = false
    private var studentId = 0
    private var institutionId = 0
    private var existingStudent: Student? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            findViewById<ImageView>(R.id.iv_student_photo).setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_student_registration)
        setupEdgeToEdge(findViewById(R.id.student_reg_root))

        repository = InstitutionRepository(this)
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        studentId = intent.getIntExtra("STUDENT_ID", 0)
        institutionId = intent.getIntExtra("INSTITUTION_ID", 0)

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionStudentRegistrationActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
            }
        }

        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        findViewById<Spinner>(R.id.sp_gender).adapter = adapter

        findViewById<ImageView>(R.id.iv_student_photo).setOnClickListener {
            pickPhoto.launch("image/*")
        }

        findViewById<Button>(R.id.btn_save_student).setOnClickListener {
            saveStudent()
        }

        if (isEditMode) {
            loadStudentData()
            findViewById<TextView>(R.id.tv_reg_title)?.text = "Edit Student Profile"
            findViewById<Button>(R.id.btn_save_student)?.text = "UPDATE STUDENT"
        }
    }

    private fun loadStudentData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InstitutionStudentRegistrationActivity)
            val student = db.institutionManagementDao().getStudentById(studentId)
            student?.let { s ->
                existingStudent = s
                findViewById<EditText>(R.id.et_student_name).setText(s.fullName)
                findViewById<EditText>(R.id.et_roll_no).setText(s.rollNumber)
                findViewById<EditText>(R.id.et_class).setText(s.grade)
                findViewById<EditText>(R.id.et_section).setText(s.section)
                findViewById<EditText>(R.id.et_dept).setText(s.department)
                findViewById<EditText>(R.id.et_age).setText(s.age.toString())
                findViewById<EditText>(R.id.et_parent_details).setText(s.parentName)
                findViewById<EditText>(R.id.et_student_id).setText(s.studentIdentityId)
                findViewById<EditText>(R.id.et_selected_sport).setText(s.selectedSport)
                findViewById<EditText>(R.id.et_medical_history).setText(s.medicalHistory)
                
                val genderPos = (findViewById<Spinner>(R.id.sp_gender).adapter as ArrayAdapter<String>).getPosition(s.gender)
                findViewById<Spinner>(R.id.sp_gender).setSelection(genderPos)
                
                // Set photo if exists (using Coil or similar would be better, but simple for now)
            }
        }
    }

    private fun saveStudent() {
        val name = findViewById<EditText>(R.id.et_student_name).text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Robust ID Fetching
                if (institutionId <= 0) {
                    val session = SessionManager(this@InstitutionStudentRegistrationActivity)
                    val email = session.getEmail() ?: ""
                    val inst = AppDatabase.getDatabase(this@InstitutionStudentRegistrationActivity).institutionDao().getInstitutionByEmail(email)
                    if (inst != null) {
                        institutionId = inst.id
                    } else {
                        Toast.makeText(this@InstitutionStudentRegistrationActivity, "Critical Error: Institution record not found. Please complete profile setup.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val imageUrl = if (photoUri != null) {
                    FirebaseManager.uploadFile(photoUri!!, "students/${System.currentTimeMillis()}.jpg")
                } else existingStudent?.photoUri

                val student = Student(
                    studentId = if (isEditMode) studentId else 0,
                    fullName = name,
                    photoUri = imageUrl,
                    rollNumber = findViewById<EditText>(R.id.et_roll_no).text.toString(),
                    grade = findViewById<EditText>(R.id.et_class).text.toString(),
                    section = findViewById<EditText>(R.id.et_section).text.toString(),
                    department = findViewById<EditText>(R.id.et_dept).text.toString(),
                    age = findViewById<EditText>(R.id.et_age).text.toString().toIntOrNull() ?: 0,
                    gender = findViewById<Spinner>(R.id.sp_gender).selectedItem.toString(),
                    parentName = findViewById<EditText>(R.id.et_parent_details).text.toString(),
                    studentIdentityId = findViewById<EditText>(R.id.et_student_id).text.toString(),
                    selectedSport = findViewById<EditText>(R.id.et_selected_sport).text.toString(),
                    medicalHistory = findViewById<EditText>(R.id.et_medical_history).text.toString(),
                    institutionId = institutionId,
                    healthStatus = existingStudent?.healthStatus ?: "Good"
                )

                val success = repository.addStudent(student)
                if (success) {
                    val message = if (isEditMode) "✔ Student Updated Successfully" else "✔ Student Registered Successfully"
                    Toast.makeText(this@InstitutionStudentRegistrationActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@InstitutionStudentRegistrationActivity, "Failed to save student. Check connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("REG_ERROR", "Student Save Failed", e)
                Toast.makeText(this@InstitutionStudentRegistrationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

