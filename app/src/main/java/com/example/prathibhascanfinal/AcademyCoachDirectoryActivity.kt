package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.adapter.CoachListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class AcademyCoachDirectoryActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository

    private lateinit var adapter: CoachListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var academyId: Int = 0
    private var allCoaches = listOf<Coach>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_coach_directory)
        setupEdgeToEdge(findViewById(R.id.coach_directory_root))

        repository = AcademyRepository(this)
        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        initUI()
        
        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyCoachDirectoryActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    repository.startSync(academyId)
                    checkDeepLink()
                    observeCoaches()
                } else {
                    Toast.makeText(this@AcademyCoachDirectoryActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            repository.startSync(academyId)
            checkDeepLink()
            observeCoaches()
        }
    }

    private fun checkDeepLink() {
        val editId = intent.getIntExtra("EDIT_COACH_ID", 0)
        if (editId != 0) {
            lifecycleScope.launch {
                val coach = AppDatabase.getDatabase(this@AcademyCoachDirectoryActivity).academyManagementDao().getCoachById(editId)
                coach?.let { showEditCoachDialog(it) }
            }
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeCoaches() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_coaches)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = CoachListAdapter(
            onView = { showCoachDetails(it) },
            onEdit = { showEditCoachDialog(it) },
            onDelete = { confirmDeleteCoach(it) }
        )
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add_coach).setOnClickListener {
            showCreateCoachDialog()
        }

        findViewById<EditText>(R.id.et_search_coach)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCoaches(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeCoaches() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            repository.getCoachesFlow(academyId).collectLatest { list ->
                allCoaches = list
                updateList(list)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<Coach>) {
        adapter.submitList(list)
    }

    private fun filterCoaches(query: String) {
        val filtered = if (query.isEmpty()) {
            allCoaches
        } else {
            allCoaches.filter { 
                it.name.contains(query, ignoreCase = true) || it.specialization.contains(query, ignoreCase = true) 
            }
        }
        updateList(filtered)
    }

    private fun showCreateCoachDialog() {
        showCoachDialog(null)
    }

    private fun showEditCoachDialog(coach: Coach) {
        showCoachDialog(coach)
    }

    private fun showCoachDialog(coach: Coach?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_coach, null)
        val nameEt = dialogView.findViewById<EditText>(R.id.et_coach_name)
        val specEt = dialogView.findViewById<EditText>(R.id.et_coach_specialization)
        val expEt = dialogView.findViewById<EditText>(R.id.et_coach_experience)
        val emailEt = dialogView.findViewById<EditText>(R.id.et_coach_email)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_coach)

        if (coach != null) {
            nameEt.setText(coach.name)
            specEt.setText(coach.specialization)
            expEt.setText(coach.experienceYears.toString())
            emailEt.setText(coach.email)
            btnConfirm.text = "Update Coach"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val spec = specEt.text.toString().trim()
            val exp = expEt.text.toString().toIntOrNull() ?: 0
            val email = emailEt.text.toString().trim()

            if (name.isNotEmpty()) {
                val updatedCoach = (coach ?: Coach(academyId = academyId)).copy(
                    name = name,
                    specialization = spec,
                    experienceYears = exp,
                    email = email
                )
                lifecycleScope.launch {
                    try {
                        repository.addCoach(updatedCoach)
                        Snackbar.make(findViewById(R.id.rv_coaches), "Coach Saved Successfully", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyCoachDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteCoach(coach: Coach) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Coach")
            .setMessage("Are you sure you want to remove '${coach.name}'? This action cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteCoach(coach)
                        Snackbar.make(findViewById(R.id.rv_coaches), "Coach Removed Successfully", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyCoachDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCoachDetails(coach: Coach) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "COACH")
            putExtra("ENTITY_ID", coach.coachId)
        }
        startActivity(intent)
    }
}

