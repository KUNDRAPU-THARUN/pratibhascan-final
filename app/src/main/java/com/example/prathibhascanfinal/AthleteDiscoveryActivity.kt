package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prathibhascanfinal.ui.adapter.DiscoveryAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AthleteDiscoveryActivity : BaseActivity() {

    override val viewModel: AthleteDiscoveryViewModel by viewModels()
    private lateinit var adapter: DiscoveryAdapter
    private var myAcademy: Academy? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_athlete_discovery)
        setupEdgeToEdge(findViewById(R.id.athlete_discovery_root))

        loadMyAcademy()
        initUI()
        observeState()
    }

    private fun loadMyAcademy() {
        val email = SessionManager(this).getEmail() ?: ""
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AthleteDiscoveryActivity)
            myAcademy = db.academyDao().getAcademyByEmail(email)
        }
    }

    private fun initUI() {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_discovery_results)
        adapter = DiscoveryAdapter(
            onProfile = { showAthleteProfile(it) },
            onInvite = { inviteAthlete(it) },
            scoreCalculator = { viewModel.calculateTalentScore(it) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<EditText>(R.id.et_search_athletes).addTextChangedListener {
            viewModel.setSearchQuery(it?.toString() ?: "")
        }

        findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_sports).setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull() ?: R.id.chip_all
            val chip = group.findViewById<Chip>(chipId)
            val sport = if (chipId == R.id.chip_all) null else chip.text.toString()
            viewModel.setSportFilter(sport)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                findViewById<ProgressBar>(R.id.progress_discovery).visibility = if (state.isLoading) View.VISIBLE else View.GONE
                adapter.submitList(state.athletes)
                findViewById<TextView>(R.id.tv_empty_discovery).visibility = if (!state.isLoading && state.athletes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAthleteProfile(user: User) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            putExtra("USER_EMAIL", user.email)
            putExtra("VIEW_ONLY", true) // Dashboard should support view-only mode
        }
        startActivity(intent)
    }

    private fun inviteAthlete(athlete: User) {
        val academy = myAcademy ?: run {
            Toast.makeText(this, "Academy profile not loaded.", Toast.LENGTH_SHORT).show()
            return
        }

        // For hackathon, just use a standard AlertDialog with a text input
        val input = EditText(this)
        input.hint = "Enter recruitment message..."
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(60, 20, 60, 10)
        input.layoutParams = params
        container.addView(input)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Invite ${athlete.fullName}")
            .setMessage("Send a recruitment invitation to this athlete.")
            .setView(container)
            .setPositiveButton("Send Invitation") { _, _ ->
                val msg = input.text.toString().trim()
                viewModel.inviteAthlete(academy, athlete, msg)
                Toast.makeText(this, "Invitation sent to ${athlete.fullName}", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
