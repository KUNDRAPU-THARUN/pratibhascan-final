package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.adapter.AthleteListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademyAthleteDirectoryActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository

    private lateinit var adapter: AthleteListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var academyId: Int = 0 
    private var currentFilter: String = "All"
    private var filterSport: String = ""
    private var allAthletes = listOf<AcademyAthlete>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_athlete_directory)
        setupEdgeToEdge(findViewById(R.id.athlete_directory_root))

        repository = AcademyRepository(this)
        filterSport = intent.getStringExtra("FILTER_SPORT") ?: ""
        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        initUI()

        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyAthleteDirectoryActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    repository.startSync(academyId)
                    observeAthletes()
                } else {
                    Toast.makeText(this@AcademyAthleteDirectoryActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            repository.startSync(academyId)
            observeAthletes()
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeAthletes() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_athletes)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AthleteListAdapter(
            onView = { showAthleteDetails(it) },
            onEdit = { editAthlete(it) },
            onDelete = { confirmDeleteAthlete(it) }
        )
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add_athlete).setOnClickListener {
            val intent = Intent(this, AcademyAthleteRegistrationActivity::class.java)
            intent.putExtra("ACADEMY_ID", academyId)
            startActivity(intent)
        }

        findViewById<EditText>(R.id.et_search_athlete)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAthletes(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        setupFilters()
    }

    private fun setupFilters() {
        findViewById<ChipGroup>(R.id.chip_group_filters)?.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_verified -> "Verified"
                R.id.chip_pending -> "Pending"
                else -> "All"
            }
            filterAthletes(findViewById<EditText>(R.id.et_search_athlete)?.text.toString())
        }
    }

    private fun observeAthletes() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            repository.getAthletesFlow(academyId).collectLatest { list ->
                allAthletes = list
                filterAthletes(findViewById<EditText>(R.id.et_search_athlete)?.text.toString())
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<AcademyAthlete>) {
        adapter.submitList(list)
        findViewById<View>(R.id.layout_empty_athletes)?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterAthletes(query: String) {
        var filtered = allAthletes
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.fullName.contains(query, ignoreCase = true) || it.admissionNumber.contains(query, ignoreCase = true) }
        }
        if (currentFilter != "All") {
            filtered = filtered.filter { it.verificationStatus == currentFilter }
        }
        if (filterSport.isNotEmpty()) {
            filtered = filtered.filter { it.sportDomain == filterSport }
        }
        updateList(filtered.sortedBy { it.fullName })
    }

    private fun showAthleteDetails(athlete: AcademyAthlete) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "ATHLETE")
            putExtra("ENTITY_ID", athlete.athleteId)
        }
        startActivity(intent)
    }

    private fun editAthlete(athlete: AcademyAthlete) {
        val intent = Intent(this, AcademyAthleteRegistrationActivity::class.java)
        intent.putExtra("EDIT_MODE", true)
        intent.putExtra("ATHLETE_ID", athlete.athleteId)
        startActivity(intent)
    }

    private fun confirmDeleteAthlete(athlete: AcademyAthlete) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Athlete")
            .setMessage("Are you sure you want to remove '${athlete.fullName}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteAthlete(athlete)
                        Snackbar.make(findViewById(R.id.rv_athletes), "Athlete Deleted Successfully", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyAthleteDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

