package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.adapter.AthleteListAdapter
import com.example.prathibhascanfinal.ui.adapter.CoachListAdapter
import com.example.prathibhascanfinal.ui.adapter.FacilityListAdapter
import com.example.prathibhascanfinal.ui.adapter.TournamentListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademySportDetailsActivity : BaseActivity() {

    override val viewModel: AcademySportViewModel by lazy {
        ViewModelProvider(this)[AcademySportViewModel::class.java]
    }

    private lateinit var repository: com.example.prathibhascanfinal.data.repository.AcademyRepository
    private lateinit var sportName: String
    private var academyId: Int = 0 

    private lateinit var athleteAdapter: AthleteListAdapter
    private lateinit var coachAdapter: CoachListAdapter
    private lateinit var facilityAdapter: FacilityListAdapter
    private lateinit var tournamentAdapter: TournamentListAdapter
    private lateinit var registrationAdapter: com.example.prathibhascanfinal.ui.adapter.RegistrationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_sport_details)
        setupEdgeToEdge(findViewById(R.id.sport_details_root))

        sportName = intent.getStringExtra("SPORT_NAME") ?: "Selected Sport"
        // Sanitize sport name and fix title formatting
        val cleanSportName = sportName.replace("$", "").ifEmpty { "Sport" }
        findViewById<TextView>(R.id.tv_sport_detail_title).text = "$cleanSportName Management"
        
        academyId = intent.getIntExtra("ACADEMY_ID", 0)
        repository = com.example.prathibhascanfinal.data.repository.AcademyRepository(this)

        val session = SessionManager(this)
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "Welcome, ${session.getName() ?: "User"}"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Academy Sport Control"
        findViewById<View>(R.id.btn_header_back)?.visibility = View.VISIBLE
        findViewById<View>(R.id.btn_header_back)?.setOnClickListener { finish() }

        if (academyId == 0) {
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademySportDetailsActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                } else {
                    Toast.makeText(this@AcademySportDetailsActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
        }
    }

    private fun initUI() {
        setupSections()
        setupAdapters()
        observeState()
        
        viewModel.loadSportData(academyId, sportName)
    }

    private fun setupSections() {
        setupSection(R.id.section_athletes, "Registered Athletes")
        setupSection(R.id.section_coaches, "Assigned Coaches")
        setupSection(R.id.section_grounds, "Available Grounds & Facilities")
        setupSection(R.id.section_tournaments, "Upcoming Tournaments")
        setupSection(R.id.section_registrations, "Pending Applications")
        setupSection(R.id.section_attendance, "Attendance & Reports")
    }

    private fun setupSection(id: Int, title: String) {
        val section = findViewById<View>(id)
        section.findViewById<TextView>(R.id.tv_section_title).text = title
    }

    private fun setupAdapters() {
        athleteAdapter = AthleteListAdapter(
            onView = { showAthleteDetails(it) },
            onEdit = { editAthlete(it) },
            onDelete = { confirmDeleteAthlete(it) }
        )
        findViewById<RecyclerView>(R.id.section_athletes).findViewById<RecyclerView>(R.id.rv_section_content).apply {
            layoutManager = LinearLayoutManager(this@AcademySportDetailsActivity)
            adapter = athleteAdapter
        }

        coachAdapter = CoachListAdapter(
            onView = { showCoachDetails(it) },
            onEdit = { editCoach(it) },
            onDelete = { confirmDeleteCoach(it) }
        )
        findViewById<RecyclerView>(R.id.section_coaches).findViewById<RecyclerView>(R.id.rv_section_content).apply {
            layoutManager = LinearLayoutManager(this@AcademySportDetailsActivity)
            adapter = coachAdapter
        }

        facilityAdapter = FacilityListAdapter(
            onView = { showFacilityDetails(it) },
            onEdit = { editFacility(it) },
            onDelete = { confirmDeleteFacility(it) }
        )
        findViewById<RecyclerView>(R.id.section_grounds).findViewById<RecyclerView>(R.id.rv_section_content).apply {
            layoutManager = LinearLayoutManager(this@AcademySportDetailsActivity)
            adapter = facilityAdapter
        }

        tournamentAdapter = TournamentListAdapter(
            onView = { showTournamentDetails(it) },
            onEdit = { editTournament(it) },
            onDelete = { confirmDeleteTournament(it) }
        )
        findViewById<RecyclerView>(R.id.section_tournaments).findViewById<RecyclerView>(R.id.rv_section_content).apply {
            layoutManager = LinearLayoutManager(this@AcademySportDetailsActivity)
            adapter = tournamentAdapter
        }

        registrationAdapter = com.example.prathibhascanfinal.ui.adapter.RegistrationAdapter(
            onAccept = { viewModel.updateRegistration(it, "ACCEPTED") },
            onReject = { viewModel.updateRegistration(it, "REJECTED") }
        )
        findViewById<RecyclerView>(R.id.section_registrations).findViewById<RecyclerView>(R.id.rv_section_content).apply {
            layoutManager = LinearLayoutManager(this@AcademySportDetailsActivity)
            adapter = registrationAdapter
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (!state.isLoading) {
                    athleteAdapter.submitList(state.athletes)
                    coachAdapter.submitList(state.coaches)
                    facilityAdapter.submitList(state.facilities)
                    tournamentAdapter.submitList(state.tournaments)
                    registrationAdapter.submitList(state.registrations)

                    updateEmptyState(R.id.section_athletes, state.athletes.isEmpty())
                    updateEmptyState(R.id.section_coaches, state.coaches.isEmpty())
                    updateEmptyState(R.id.section_grounds, state.facilities.isEmpty())
                    updateEmptyState(R.id.section_tournaments, state.tournaments.isEmpty())
                    updateEmptyState(R.id.section_registrations, state.registrations.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(sectionId: Int, isEmpty: Boolean) {
        val section = findViewById<View>(sectionId)
        section.findViewById<View>(R.id.tv_empty_msg).visibility = if (isEmpty) View.VISIBLE else View.GONE
        section.findViewById<View>(R.id.rv_section_content).visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // --- Action Handlers ---

    private fun showAthleteDetails(it: AcademyAthlete) {
        MaterialAlertDialogBuilder(this).setTitle(it.fullName).setMessage("Details for ${it.sportDomain}").show()
    }
    private fun editAthlete(it: AcademyAthlete) {
        val intent = Intent(this, AcademyAthleteRegistrationActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("ATHLETE_ID", it.athleteId)
        }
        startActivity(intent)
    }
    private fun confirmDeleteAthlete(it: AcademyAthlete) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Athlete")
            .setMessage("Remove ${it.fullName} permanently?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteAthlete(it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCoachDetails(it: Coach) {
        MaterialAlertDialogBuilder(this).setTitle(it.name).setMessage("Specialization: ${it.specialization}\nExp: ${it.experienceYears} Yrs").show()
    }
    private fun editCoach(it: Coach) {
        val intent = Intent(this, AcademyCoachDirectoryActivity::class.java).apply {
            putExtra("EDIT_COACH_ID", it.coachId)
        }
        startActivity(intent)
    }
    private fun confirmDeleteCoach(it: Coach) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Coach")
            .setMessage("Remove ${it.name} from the academy?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteCoach(it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFacilityDetails(it: Facility) {
        MaterialAlertDialogBuilder(this).setTitle(it.name).setMessage("Type: ${it.type}\nCapacity: ${it.capacity}").show()
    }
    private fun editFacility(it: Facility) {
        val intent = Intent(this, AcademyBookingActivity::class.java).apply {
            putExtra("EDIT_FACILITY_ID", it.id)
        }
        startActivity(intent)
    }
    private fun confirmDeleteFacility(it: Facility) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Facility")
            .setMessage("Delete ${it.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteFacility(it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTournamentDetails(it: Tournament) {
        MaterialAlertDialogBuilder(this).setTitle(it.title).setMessage("Venue: ${it.venue}\nDate: ${it.startDate}").show()
    }
    private fun editTournament(it: Tournament) {
        val intent = Intent(this, AcademyTournamentActivity::class.java).apply {
            putExtra("EDIT_TOURNAMENT_ID", it.tournamentId)
        }
        startActivity(intent)
    }
    private fun confirmDeleteTournament(it: Tournament) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Tournament")
            .setMessage("Delete ${it.title}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteTournament(it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
