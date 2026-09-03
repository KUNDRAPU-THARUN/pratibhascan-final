package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.adapter.TournamentListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AcademyTournamentActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository

    private lateinit var adapter: TournamentListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var academyId: Int = 0
    private var allTournaments = listOf<Tournament>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_tournament)
        setupEdgeToEdge(findViewById(R.id.tournament_root))

        repository = AcademyRepository(this)
        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyTournamentActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                    checkDeepLink()
                    observeTournaments()
                } else {
                    Toast.makeText(this@AcademyTournamentActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
            checkDeepLink()
            observeTournaments()
        }
    }

    private fun checkDeepLink() {
        val editId = intent.getIntExtra("EDIT_TOURNAMENT_ID", 0)
        if (editId != 0) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@AcademyTournamentActivity).academyManagementDao()
                val tournament = db.getTournamentById(editId)
                tournament?.let { showEditTournamentDialog(it) }
            }
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeTournaments() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_tournaments)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = TournamentListAdapter(
            onView = { showTournamentDetails(it) },
            onEdit = { showEditTournamentDialog(it) },
            onDelete = { confirmDeleteTournament(it) },
            onRegistrations = { showRegistrations(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_create_tournament).setOnClickListener {
            showCreateTournamentDialog()
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
            // Need a Flow for Tournaments in AcademyManagementDao. Adding it if missing.
            val db = AppDatabase.getDatabase(this@AcademyTournamentActivity).academyManagementDao()
            db.getTournamentsFlow(academyId).collectLatest { list ->
                allTournaments = list
                updateList(list)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<Tournament>) {
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

    private fun showCreateTournamentDialog() {
        showTournamentDialog(null)
    }

    private fun showEditTournamentDialog(tournament: Tournament) {
        showTournamentDialog(tournament)
    }

    private fun showTournamentDialog(tournament: Tournament?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_tournament, null)
        val titleEt = dialogView.findViewById<EditText>(R.id.et_tourney_title)
        val sportEt = dialogView.findViewById<EditText>(R.id.et_tourney_sport)
        val venueEt = dialogView.findViewById<EditText>(R.id.et_tourney_venue)
        val descEt = dialogView.findViewById<EditText>(R.id.et_tourney_desc)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_create_tourney)

        if (tournament != null) {
            titleEt.setText(tournament.title)
            sportEt.setText(tournament.sport)
            venueEt.setText(tournament.venue)
            descEt.setText(tournament.description)
            dialogView.findViewById<EditText>(R.id.et_tourney_fee)?.setText(tournament.entryFee)
            btnConfirm.text = "Update Tournament"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val title = titleEt.text.toString().trim()
            val sport = sportEt.text.toString().trim()
            val venue = venueEt.text.toString().trim()
            val desc = descEt.text.toString().trim()

            if (title.isNotEmpty()) {
                val updatedTournament = (tournament ?: Tournament(academyId = academyId)).copy(
                    title = title,
                    sport = sport,
                    venue = venue,
                    description = desc,
                    startDate = tournament?.startDate ?: System.currentTimeMillis(),
                    endDate = tournament?.endDate ?: (System.currentTimeMillis() + 86400000 * 7),
                    entryFee = dialogView.findViewById<EditText>(R.id.et_tourney_fee)?.text?.toString() ?: "Free"
                )
                lifecycleScope.launch {
                    try {
                        repository.addTournament(updatedTournament)
                        Snackbar.make(findViewById(R.id.rv_tournaments), "✔ Tournament Saved Successfully", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyTournamentActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun confirmDeleteTournament(tournament: Tournament) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Tournament")
            .setMessage("Are you sure you want to delete '${tournament.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteTournament(tournament)
                        Snackbar.make(findViewById(R.id.rv_tournaments), "Tournament Deleted", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyTournamentActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTournamentDetails(tournament: Tournament) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "TOURNAMENT")
            putExtra("ENTITY_ID", tournament.tournamentId)
        }
        startActivity(intent)
    }

    private fun showRegistrations(tournament: Tournament) {
        val intent = Intent(this, TournamentRegistrationsActivity::class.java).apply {
            putExtra("TOURNAMENT_ID", tournament.tournamentId)
        }
        startActivity(intent)
    }
}

