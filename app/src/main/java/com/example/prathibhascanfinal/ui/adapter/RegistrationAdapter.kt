package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.R
import com.example.prathibhascanfinal.TournamentRegistration

class RegistrationAdapter(
    private val onAccept: (TournamentRegistration) -> Unit,
    private val onReject: (TournamentRegistration) -> Unit
) : ListAdapter<TournamentRegistration, RegistrationAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_card_title)
        val status: TextView = view.findViewById(R.id.tv_card_status)
        val athlete: TextView = view.findViewById(R.id.tv_card_detail1)
        val date: TextView = view.findViewById(R.id.tv_card_detail2)
        
        val btnAccept: View = view.findViewById(R.id.btn_card_view) // Reuse view as Accept
        val btnReject: View = view.findViewById(R.id.btn_card_delete) // Reuse delete as Reject
        val btnEdit: View = view.findViewById(R.id.btn_card_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_management_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.title.text = item.tournamentTitle
        holder.status.text = item.status
        holder.athlete.text = "Athlete: ${item.athleteName}"
        holder.date.text = "Applied on: ${java.util.Date(item.appliedAt)}"
        
        holder.btnEdit.visibility = View.GONE
        (holder.btnAccept as com.google.android.material.button.MaterialButton).apply {
            text = "ACCEPT"
            icon = null
            setOnClickListener { onAccept(item) }
        }
        (holder.btnReject as com.google.android.material.button.MaterialButton).apply {
            text = "REJECT"
            icon = null
            setOnClickListener { onReject(item) }
        }
        
        if (item.status != "PENDING") {
            holder.btnAccept.visibility = View.GONE
            holder.btnReject.visibility = View.GONE
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<TournamentRegistration>() {
        override fun areItemsTheSame(oldItem: TournamentRegistration, newItem: TournamentRegistration): Boolean {
            return oldItem.registrationId == newItem.registrationId
        }

        override fun areContentsTheSame(oldItem: TournamentRegistration, newItem: TournamentRegistration): Boolean {
            return oldItem == newItem
        }
    }
}
