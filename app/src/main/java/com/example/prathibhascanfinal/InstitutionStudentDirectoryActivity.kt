package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.InstitutionRepository
import com.example.prathibhascanfinal.ui.adapter.StudentListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstitutionStudentDirectoryActivity : BaseActivity() {

    override val viewModel: InstitutionPortalViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[InstitutionPortalViewModel::class.java]
    }
    
    private lateinit var repository: InstitutionRepository
    private lateinit var adapter: StudentListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var institutionId: Int = 0
    private var allStudents = listOf<Student>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_student_directory)
        setupEdgeToEdge(findViewById(R.id.inst_student_directory_root))

        repository = InstitutionRepository(this)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_students)
        toolbar.setNavigationOnClickListener { finish() }

        initUI()

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionStudentDirectoryActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                repository.startSync(institutionId)
                observeStudents()
            } else {
                Toast.makeText(this@InstitutionStudentDirectoryActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeStudents() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_students)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = StudentListAdapter(
            onView = { showStudentDetails(it) },
            onEdit = { editStudent(it) },
            onDelete = { confirmDeleteStudent(it) }
        )
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add_student).setOnClickListener {
            val intent = Intent(this, InstitutionStudentRegistrationActivity::class.java)
            intent.putExtra("INSTITUTION_ID", institutionId)
            startActivity(intent)
        }

        findViewById<EditText>(R.id.et_search_student)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStudents(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeStudents() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            // Need a Flow for Students in InstitutionManagementDao. Adding it if missing.
            val db = AppDatabase.getDatabase(this@InstitutionStudentDirectoryActivity).institutionManagementDao()
            db.getStudentsFlow(institutionId).collectLatest { list ->
                allStudents = list
                updateList(list)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<Student>) {
        adapter.submitList(list)
        findViewById<View>(R.id.layout_empty_students).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterStudents(query: String) {
        val filtered = if (query.isEmpty()) {
            allStudents
        } else {
            allStudents.filter { 
                it.fullName.contains(query, ignoreCase = true) || it.rollNumber.contains(query, ignoreCase = true) 
            }
        }
        updateList(filtered)
    }

    private fun showStudentDetails(student: Student) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "STUDENT")
            putExtra("ENTITY_ID", student.studentId)
        }
        startActivity(intent)
    }

    private fun editStudent(student: Student) {
        val intent = Intent(this, InstitutionStudentRegistrationActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("STUDENT_ID", student.studentId)
        }
        startActivity(intent)
    }

    private fun confirmDeleteStudent(student: Student) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to remove '${student.fullName}'? This will also delete their cloud records.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteStudent(student)
                        Snackbar.make(findViewById(R.id.rv_students), "Student Removed Successfully", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionStudentDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
