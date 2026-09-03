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

class AcademyInvitationsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository
    private lateinit var adapter: InvitationListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_invitations)
        setupEdgeToEdge(findViewById(R.id.academy_invitations_root))

        repository = AcademyRepository(this)
        initUI()
        observeInvitations()
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeInvitations() }

        val rv = findViewById<RecyclerView>(R.id.rv_invitations)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InvitationListAdapter(
            onAccept = { updateStatus(it, "ACCEPTED") },
            onDecline = { updateStatus(it, "DECLINED") }
        )
        rv.adapter = adapter

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }

    private fun observeInvitations() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val session = SessionManager(this@AcademyInvitationsActivity)
            val email = session.getEmail() ?: ""
            val academy = AppDatabase.getDatabase(this@AcademyInvitationsActivity).academyDao().getAcademyByEmail(email)
            
            academy?.let { aca ->
                repository.startSync(aca.id)
                AppDatabase.getDatabase(this@AcademyInvitationsActivity).academyManagementDao()
                    .getInvitationsForAcademyFlow(aca.id).collectLatest { list ->
                        adapter.submitList(list)
                        findViewById<View>(R.id.layout_empty_invitations).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        swipeRefresh.isRefreshing = false
                    }
            }
        }
    }

    private fun updateStatus(inv: AcademyInvitation, status: String) {
        lifecycleScope.launch {
            val success = repository.updateInvitationStatus(inv, status)
            if (success) {
                Toast.makeText(this@AcademyInvitationsActivity, "Invitation $status", Toast.LENGTH_SHORT).show()
                
                // Notify Athlete
                val notificationRepo = com.example.prathibhascanfinal.data.repository.NotificationRepository(
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                )
                notificationRepo.sendNotification(
                    userEmail = inv.athleteEmail,
                    title = if (status == "ACCEPTED") "🤝 Invitation Accepted" else "❌ Invitation Declined",
                    message = "${inv.academyName} has $status your request.",
                    category = com.example.prathibhascanfinal.data.NotificationCategories.ACADEMY,
                    action = "ACADEMY_DETAILS"
                )
            }
        }
    }

    inner class InvitationListAdapter(
        private val onAccept: (AcademyInvitation) -> Unit,
        private val onDecline: (AcademyInvitation) -> Unit
    ) : ListAdapter<AcademyInvitation, InvitationListAdapter.ViewHolder>(DiffCallback) {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_athlete_name)
            val sport: TextView = view.findViewById(R.id.tv_athlete_sport)
            val msg: TextView = view.findViewById(R.id.tv_invitation_msg)
            val status: TextView = view.findViewById(R.id.tv_inv_status)
            val btnAccept: MaterialButton = view.findViewById(R.id.btn_accept_inv)
            val btnDecline: MaterialButton = view.findViewById(R.id.btn_decline_inv)
            val layoutActions: View = view.findViewById(R.id.layout_inv_actions)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_academy_invitation_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.athleteName
            holder.sport.text = item.sport
            holder.msg.text = item.message
            holder.status.text = item.status
            
            if (item.status == "PENDING") {
                holder.layoutActions.visibility = View.VISIBLE
                holder.status.setTextColor(getColor(R.color.brand_gold))
            } else {
                holder.layoutActions.visibility = View.GONE
                holder.status.setTextColor(if (item.status == "ACCEPTED") getColor(R.color.brand_green) else getColor(R.color.brand_red))
            }

            holder.btnAccept.setOnClickListener { onAccept(item) }
            holder.btnDecline.setOnClickListener { onDecline(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<AcademyInvitation>() {
        override fun areItemsTheSame(oldItem: AcademyInvitation, newItem: AcademyInvitation): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AcademyInvitation, newItem: AcademyInvitation): Boolean = oldItem == newItem
    }
}
