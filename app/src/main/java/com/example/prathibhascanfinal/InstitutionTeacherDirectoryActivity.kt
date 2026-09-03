package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.InstitutionRepository
import com.example.prathibhascanfinal.ui.adapter.TeacherListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstitutionTeacherDirectoryActivity : BaseActivity() {

    override val viewModel: InstitutionPortalViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[InstitutionPortalViewModel::class.java]
    }
    
    private lateinit var repository: InstitutionRepository
    private lateinit var adapter: TeacherListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var institutionId: Int = 0
    private var allTeachers = listOf<InstitutionTeacher>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_teacher_directory)
        setupEdgeToEdge(findViewById(R.id.inst_teacher_directory_root))

        repository = InstitutionRepository(this)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_teachers)
        toolbar.setNavigationOnClickListener { finish() }

        initUI()

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionTeacherDirectoryActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                observeTeachers()
            } else {
                Toast.makeText(this@InstitutionTeacherDirectoryActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeTeachers() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_teachers)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = TeacherListAdapter(
            onView = { showTeacherDetails(it) },
            onEdit = { showEditTeacherDialog(it) },
            onDelete = { confirmDeleteTeacher(it) }
        )
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add_teacher).setOnClickListener {
            showEditTeacherDialog(null)
        }

        findViewById<EditText>(R.id.et_search_teacher)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTeachers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeTeachers() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            repository.getTeachersFlow(institutionId).collectLatest { list ->
                allTeachers = list
                updateList(list)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<InstitutionTeacher>) {
        adapter.submitList(list)
    }

    private fun filterTeachers(query: String) {
        val filtered = if (query.isEmpty()) {
            allTeachers
        } else {
            allTeachers.filter { 
                it.fullName.contains(query, ignoreCase = true) || it.specialization.contains(query, ignoreCase = true) 
            }
        }
        updateList(filtered)
    }

    private fun showEditTeacherDialog(teacher: InstitutionTeacher?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_teacher, null)
        val nameEt = dialogView.findViewById<EditText>(R.id.et_teacher_name_entry)
        val specEt = dialogView.findViewById<EditText>(R.id.et_teacher_specialty)
        val phoneEt = dialogView.findViewById<EditText>(R.id.et_teacher_phone)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_teacher)

        if (teacher != null) {
            nameEt.setText(teacher.fullName)
            specEt.setText(teacher.specialization)
            phoneEt.setText(teacher.phone)
            btnConfirm.text = "UPDATE TEACHER"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnConfirm.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val spec = specEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()

            if (name.isNotEmpty() && spec.isNotEmpty()) {
                val updated = (teacher ?: InstitutionTeacher(institutionId = institutionId)).copy(
                    fullName = name,
                    specialization = spec,
                    phone = phone
                )
                lifecycleScope.launch {
                    try {
                        repository.addTeacher(updated)
                        Snackbar.make(findViewById(R.id.rv_teachers), "✔ Teacher Saved Successfully", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionTeacherDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Name and Specialty are required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun confirmDeleteTeacher(teacher: InstitutionTeacher) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Teacher")
            .setMessage("Are you sure you want to remove '${teacher.fullName}'? This will also delete their cloud records.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteTeacher(teacher)
                        Snackbar.make(findViewById(R.id.rv_teachers), "Teacher Removed", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionTeacherDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTeacherDetails(teacher: InstitutionTeacher) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "TEACHER")
            putExtra("ENTITY_ID", teacher.teacherId)
        }
        startActivity(intent)
    }
}
