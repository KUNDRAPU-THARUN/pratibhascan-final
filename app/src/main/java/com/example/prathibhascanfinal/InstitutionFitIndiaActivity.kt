package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class InstitutionFitIndiaActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private var selectedStudentId: Int = 0
    private var institutionId: Int = 0
    private var studentList = listOf<Student>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_fit_india)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""

        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionFitIndiaActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                loadStudents()
            } else {
                Toast.makeText(this@InstitutionFitIndiaActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        findViewById<Button>(R.id.btn_save_fit_india).setOnClickListener {
            saveFitData()
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InstitutionFitIndiaActivity)
            studentList = db.institutionManagementDao().getStudents(institutionId)
            
            if (studentList.isEmpty()) {
                Toast.makeText(this@InstitutionFitIndiaActivity, "No students registered.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = studentList.map { "${it.fullName} (${it.rollNumber})" }
            val adapter = ArrayAdapter(this@InstitutionFitIndiaActivity, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            
            val spinner = findViewById<Spinner>(R.id.sp_student_selection_fit)
            spinner?.adapter = adapter
            spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    selectedStudentId = studentList[position].studentId
                    findViewById<TextView>(R.id.tv_student_name_fit)?.text = studentList[position].fullName
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }

    private fun saveFitData() {
        if (selectedStudentId == 0) return

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@InstitutionFitIndiaActivity)
                val s = db.institutionManagementDao().getStudentById(selectedStudentId)
                if (s != null) {
                    val updatedStudent = s.copy(
                        bmi = findViewById<EditText>(R.id.et_fit_bmi).text.toString().toDoubleOrNull() ?: 0.0,
                        sprintScore = findViewById<EditText>(R.id.et_fit_sprint).text.toString().toDoubleOrNull() ?: 0.0,
                        flexibilityScore = findViewById<EditText>(R.id.et_fit_flexibility).text.toString().toDoubleOrNull() ?: 0.0,
                        strengthScore = findViewById<EditText>(R.id.et_fit_strength).text.toString().toDoubleOrNull() ?: 0.0,
                        enduranceScore = findViewById<EditText>(R.id.et_fit_endurance).text.toString().toDoubleOrNull() ?: 0.0
                    )
                    db.institutionManagementDao().insertStudent(updatedStudent)
                    Toast.makeText(this@InstitutionFitIndiaActivity, "Fit India Data Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InstitutionFitIndiaActivity, "Error saving data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

