package com.example.prathibhascanfinal

import androidx.activity.viewModels

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
import com.example.prathibhascanfinal.ui.adapter.InstitutionTournamentListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstitutionTournamentActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: InstitutionRepository

    private lateinit var adapter: InstitutionTournamentListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var institutionId: Int = 0
    private var allTournaments = listOf<InstitutionTournament>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_tournament)
        setupEdgeToEdge(findViewById(R.id.inst_tournament_root))

        repository = InstitutionRepository(this)
        
        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionTournamentActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                initUI()
                observeTournaments()
            } else {
                initUI()
                observeTournaments()
            }
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeTournaments() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_inst_tournaments)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InstitutionTournamentListAdapter(
            onView = { showTournamentDetails(it) },
            onEdit = { showTournamentDialog(it) },
            onDelete = { confirmDeleteTournament(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_create_tournament).setOnClickListener {
            showTournamentDialog(null)
        }

        findViewById<EditText>(R.id.et_search_tournament)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTournaments(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeTournaments() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            // Need a Flow for Tournaments in InstitutionManagementDao.
            val db = AppDatabase.getDatabase(this@InstitutionTournamentActivity).institutionManagementDao()
            db.getTournamentsFlow(institutionId).collectLatest { list ->
                allTournaments = list
                updateList(list)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<InstitutionTournament>) {
        adapter.submitList(list)
        findViewById<View>(R.id.layout_empty_tournaments).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterTournaments(query: String) {
        val filtered = if (query.isEmpty()) {
            allTournaments
        } else {
            allTournaments.filter { 
                it.title.contains(query, ignoreCase = true) || it.sport.contains(query, ignoreCase = true) 
            }
        }
        updateList(filtered)
    }

    private fun showTournamentDialog(tournament: InstitutionTournament?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_tournament, null)
        val titleEt = dialogView.findViewById<EditText>(R.id.et_tourney_title)
        val sportEt = dialogView.findViewById<EditText>(R.id.et_tourney_sport)
        val venueEt = dialogView.findViewById<EditText>(R.id.et_tourney_venue)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_create_tourney)

        if (tournament != null) {
            titleEt.setText(tournament.title)
            sportEt.setText(tournament.sport)
            venueEt.setText(tournament.venue)
            btnConfirm.text = "UPDATE TOURNAMENT"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val title = titleEt.text.toString().trim()
            val sport = sportEt.text.toString().trim()
            val venue = venueEt.text.toString().trim()

            if (title.isNotEmpty()) {
                val updated = (tournament ?: InstitutionTournament(institutionId = institutionId)).copy(
                    title = title,
                    sport = sport,
                    venue = venue,
                    startDate = tournament?.startDate ?: System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    try {
                        repository.addTournament(updated)
                        Snackbar.make(findViewById(R.id.rv_inst_tournaments), "✔ Tournament Saved Successfully", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionTournamentActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteTournament(tournament: InstitutionTournament) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Tournament")
            .setMessage("Are you sure you want to remove '${tournament.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteTournament(tournament)
                        Snackbar.make(findViewById(R.id.rv_inst_tournaments), "Tournament Removed", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionTournamentActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTournamentDetails(tournament: InstitutionTournament) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "INST_TOURNAMENT")
            putExtra("ENTITY_ID", tournament.tournamentId)
        }
        startActivity(intent)
    }
}

