package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.launch

class ManagementDetailActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var container: LinearLayout
    private var entityType: String = ""
    private var entityId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_management_detail)
        setupEdgeToEdge(findViewById(R.id.management_detail_root))
        
        entityType = intent.getStringExtra("ENTITY_TYPE") ?: ""
        entityId = intent.getIntExtra("ENTITY_ID", 0)

        container = findViewById(R.id.ll_detail_container)
        
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@ManagementDetailActivity)
            val mgmtDao = db.academyManagementDao()
            val instMgmtDao = db.institutionManagementDao()
            
            when (entityType) {
                "ATHLETE" -> {
                    val athlete = mgmtDao.getAthleteById(entityId)
                    athlete?.let { displayAthlete(it) }
                }
                "COACH" -> {
                    val coach = mgmtDao.getCoachById(entityId)
                    coach?.let { displayCoach(it) }
                }
                "TEAM" -> {
                    val team = mgmtDao.getTeamById(entityId)
                    team?.let { displayTeam(it) }
                }
                "TOURNAMENT" -> {
                    val tournament = mgmtDao.getTournamentById(entityId)
                    tournament?.let { displayTournament(it) }
                }
                "TEACHER" -> {
                    val teacher = instMgmtDao.getTeacherById(entityId)
                    teacher?.let { displayTeacher(it) }
                }
                "STUDENT" -> {
                    val student = instMgmtDao.getStudentById(entityId)
                    student?.let { displayStudent(it) }
                }
                "INST_TEAM" -> {
                    val team = instMgmtDao.getTeamById(entityId)
                    team?.let { displayInstTeam(it) }
                }
                "INST_TOURNAMENT" -> {
                    val tournament = instMgmtDao.getTournamentById(entityId)
                    tournament?.let { displayInstTournament(it) }
                }
                "INST_EQUIPMENT" -> {
                    val eq = instMgmtDao.getEquipmentById(entityId)
                    eq?.let { displayInstEquipment(it) }
                }
                "FACILITY" -> {
                    val facility = mgmtDao.getFacilityById(entityId)
                    facility?.let { displayFacility(it) }
                }
                else -> {
                    Toast.makeText(this@ManagementDetailActivity, "Unknown entity type", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun displayFacility(facility: Facility) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = facility.name
        addField("Type", facility.type)
        addField("Sport", facility.sport)
        addField("Capacity", facility.capacity.toString())
        addField("Indoor", if (facility.isIndoor) "Yes" else "No")
        addField("Availability", facility.availability)
        addField("Status", facility.status)
    }

    private fun displayInstEquipment(eq: InstitutionEquipment) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = eq.name
        addField("Category", eq.category)
        addField("Total Quantity", eq.totalQuantity.toString())
        addField("Issued Quantity", eq.issuedQuantity.toString())
        addField("Available Stock", (eq.totalQuantity - eq.issuedQuantity).toString())
        addField("Purchase Date", java.util.Date(eq.purchaseDate).toString())
    }

    private fun displayInstTournament(tournament: InstitutionTournament) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = tournament.title
        addField("Sport", tournament.sport)
        addField("Level", tournament.level)
        addField("Venue", tournament.venue)
        addField("Start Date", java.util.Date(tournament.startDate).toString())
    }

    private fun displayInstTeam(team: InstitutionTeam) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = team.teamName
        addField("Sport", team.sport)
        addField("Category / Age Group", "${team.category} (${team.ageGroup})")
        addField("Gender", team.gender)
        addField("Type", team.teamType)
        addField("Coach / Teacher In-charge", team.coachName ?: team.teacherInCharge ?: "Not Assigned")
        if (!team.description.isNullOrEmpty()) addField("Description", team.description)

        val btn = com.google.android.material.button.MaterialButton(this).apply {
            text = "OPEN FULL TEAM ROSTER"
            setOnClickListener {
                val intent = android.content.Intent(this@ManagementDetailActivity, InstitutionTeamDetailActivity::class.java).apply {
                    putExtra("TEAM_ID", team.teamId)
                    putExtra("INSTITUTION_ID", team.institutionId)
                }
                startActivity(intent)
            }
        }
        container.addView(btn)
    }

    private fun displayTeacher(teacher: InstitutionTeacher) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = teacher.fullName
        addField("Specialization", teacher.specialization)
        addField("Qualification", teacher.qualification)
        addField("Experience", "${teacher.experienceYears} Years")
        addField("Email", teacher.email)
        addField("Phone", teacher.phone)
        addField("Verified", teacher.isVerified.toString())
    }

    private fun displayStudent(student: Student) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = student.fullName
        addField("Roll Number", student.rollNumber)
        addField("Grade", student.grade)
        addField("Section", student.section)
        addField("Sport", student.selectedSport)
        addField("Game", student.selectedGame)
        addField("Age", student.age.toString())
        addField("Gender", student.gender)
        addField("Height", "${student.heightCm} cm")
        addField("Weight", "${student.weightKg} kg")
        addField("BMI", student.bmi.toString())
        addField("Health Status", student.healthStatus)
        addField("Sprint Score", student.sprintScore.toString())
        addField("Balance Score", student.balanceScore.toString())
        addField("Flexibility Score", student.flexibilityScore.toString())
        addField("Strength Score", student.strengthScore.toString())
        addField("Endurance Score", student.enduranceScore.toString())
    }

    private fun displayAthlete(athlete: AcademyAthlete) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = athlete.fullName
        
        addField("Admission Number", athlete.admissionNumber)
        addField("Sport", athlete.sportDomain)
        if (!athlete.secondarySport.isNullOrEmpty()) addField("Secondary Sport", athlete.secondarySport)
        addField("Skill Level", athlete.skillLevel)
        addField("Position", athlete.position)
        addField("Age", athlete.age.toString())
        addField("Gender", athlete.gender)
        addField("Height", "${athlete.heightCm} cm")
        addField("Weight", "${athlete.weightKg} kg")
        addField("BMI", athlete.bmi.toString())
        addField("Blood Group", athlete.bloodGroup)
        addField("Contact", athlete.contactNumber)
        addField("Email", athlete.email)
        addField("Address", athlete.address)
        addField("Parent Name", athlete.parentName)
        addField("Parent Phone", athlete.parentPhone)
        addField("Identity ID", athlete.identityId)
        addField("Experience", "${athlete.experienceYears} Years")
        addField("Dominant Side", athlete.dominantSide)
        addField("Verification Status", athlete.verificationStatus)
        if (athlete.medicalHistory.isNotEmpty()) addField("Medical History", athlete.medicalHistory)
    }

    private fun displayCoach(coach: Coach) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = coach.name
        addField("Specialization", coach.specialization)
        addField("Qualification", coach.qualification)
        addField("Experience", "${coach.experienceYears} Years")
        addField("Email", coach.email)
        addField("Phone", coach.phone)
        addField("Status", coach.status)
        addField("Salary", coach.salary.toString())
    }

    private fun displayTeam(team: Team) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = team.teamName
        addField("Sport", team.sport)
        addField("Age Group", team.ageGroup)
        // Fetch coach name asynchronously if needed
    }

    private fun displayTournament(tournament: Tournament) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = tournament.title
        addField("Level", tournament.level)
        addField("Sport", tournament.sport)
        addField("Venue", tournament.venue)
        addField("Start Date", java.util.Date(tournament.startDate).toString())
        addField("End Date", java.util.Date(tournament.endDate).toString())
        addField("Description", tournament.description)
    }

    private fun addField(label: String, value: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_detail_field, container, false)
        view.findViewById<TextView>(R.id.tv_field_label).text = label
        view.findViewById<TextView>(R.id.tv_field_value).text = value
        container.addView(view)
    }
}

