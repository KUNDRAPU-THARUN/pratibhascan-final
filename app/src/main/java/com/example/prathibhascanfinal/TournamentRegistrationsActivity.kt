package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TournamentRegistrationsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository
    private lateinit var adapter: RegistrationListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var tournamentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_registrations)
        setupEdgeToEdge(findViewById(R.id.tournament_registrations_root))

        repository = AcademyRepository(this)
        tournamentId = intent.getIntExtra("TOURNAMENT_ID", 0)

        if (tournamentId == 0) {
            Toast.makeText(this, "Tournament not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initUI()
        observeRegistrations()
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeRegistrations() }

        val rv = findViewById<RecyclerView>(R.id.rv_registrations)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = RegistrationListAdapter(
            onAccept = { updateStatus(it, "ACCEPTED") },
            onReject = { updateStatus(it, "REJECTED") }
        )
        rv.adapter = adapter

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }

    private fun observeRegistrations() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val session = SessionManager(this@TournamentRegistrationsActivity)
            val email = session.getEmail() ?: ""
            val academy = AppDatabase.getDatabase(this@TournamentRegistrationsActivity).academyDao().getAcademyByEmail(email)
            
            academy?.let { aca ->
                repository.startSync(aca.id) // Ensure we have latest from cloud
                AppDatabase.getDatabase(this@TournamentRegistrationsActivity).academyManagementDao()
                    .getRegistrationsForOrganizerFlow(aca.id).collectLatest { list ->
                        val filtered = list.filter { it.tournamentId == tournamentId }
                        adapter.submitList(filtered)
                        findViewById<View>(R.id.layout_empty_registrations).visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                        swipeRefresh.isRefreshing = false
                    }
            }
        }
    }

    private fun updateStatus(reg: TournamentRegistration, status: String) {
        lifecycleScope.launch {
            val success = repository.updateRegistrationStatus(reg, status)
            if (success) {
                Toast.makeText(this@TournamentRegistrationsActivity, "Registration $status", Toast.LENGTH_SHORT).show()
                
                // Notify Athlete
                val notificationRepo = com.example.prathibhascanfinal.data.repository.NotificationRepository(
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                )
                notificationRepo.sendNotification(
                    userEmail = reg.athleteEmail,
                    title = if (status == "ACCEPTED") "✅ Tournament Accepted" else "❌ Tournament Rejected",
                    message = "Your registration for ${reg.tournamentTitle} has been $status.",
                    category = com.example.prathibhascanfinal.data.NotificationCategories.TOURNAMENT,
                    action = "TOURNAMENT_DETAILS"
                )
            } else {
                Toast.makeText(this@TournamentRegistrationsActivity, "Failed to update registration", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class RegistrationListAdapter(
        private val onAccept: (TournamentRegistration) -> Unit,
        private val onReject: (TournamentRegistration) -> Unit
    ) : ListAdapter<TournamentRegistration, RegistrationListAdapter.ViewHolder>(DiffCallback) {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_athlete_name)
            val email: TextView = view.findViewById(R.id.tv_athlete_email)
            val date: TextView = view.findViewById(R.id.tv_applied_date)
            val status: TextView = view.findViewById(R.id.tv_reg_status)
            val btnAccept: MaterialButton = view.findViewById(R.id.btn_accept)
            val btnReject: MaterialButton = view.findViewById(R.id.btn_reject)
            val layoutActions: View = view.findViewById(R.id.layout_actions)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_registration_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.athleteName
            holder.email.text = item.athleteEmail
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.date.text = "Applied on: ${sdf.format(Date(item.appliedAt))}"
            holder.status.text = item.status
            
            if (item.status == "PENDING") {
                holder.layoutActions.visibility = View.VISIBLE
                holder.status.setTextColor(getColor(R.color.brand_gold))
            } else {
                holder.layoutActions.visibility = View.GONE
                holder.status.setTextColor(if (item.status == "ACCEPTED") getColor(R.color.brand_green) else getColor(R.color.brand_red))
            }

            holder.btnAccept.setOnClickListener { onAccept(item) }
            holder.btnReject.setOnClickListener { onReject(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<TournamentRegistration>() {
        override fun areItemsTheSame(oldItem: TournamentRegistration, newItem: TournamentRegistration): Boolean = oldItem.registrationId == newItem.registrationId
        override fun areContentsTheSame(oldItem: TournamentRegistration, newItem: TournamentRegistration): Boolean = oldItem == newItem
    }
}
