package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.InstitutionTeam
import com.example.prathibhascanfinal.R

class InstitutionTeamListAdapter(
    private val onView: (InstitutionTeam) -> Unit,
    private val onEdit: (InstitutionTeam) -> Unit,
    private val onDelete: (InstitutionTeam) -> Unit
) : ListAdapter<InstitutionTeam, InstitutionTeamListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_card_title)
        val status: TextView = view.findViewById(R.id.tv_card_status)
        val detail1: TextView = view.findViewById(R.id.tv_card_detail1)
        val detail2: TextView = view.findViewById(R.id.tv_card_detail2)
        
        val btnView: View = view.findViewById(R.id.btn_card_view)
        val btnEdit: View = view.findViewById(R.id.btn_card_edit)
        val btnDelete: View = view.findViewById(R.id.btn_card_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_management_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.title.text = "🛡️ ${item.teamName}"
        holder.status.text = item.status.ifEmpty { "Active" }
        holder.status.setTextColor(0xFF22C55E.toInt()) // Green

        val catStr = if (item.category.isNotEmpty()) " | ${item.category}" else ""
        val ageStr = if (item.ageGroup.isNotEmpty()) " (${item.ageGroup})" else ""
        val genderStr = if (item.gender.isNotEmpty()) " [${item.gender}]" else ""
        holder.detail1.text = "Sport: ${item.sport}$catStr$ageStr$genderStr"

        val coachStr = item.coachName ?: item.teacherInCharge ?: "Not Assigned"
        holder.detail2.text = "Coach/In-charge: $coachStr | Type: ${item.teamType}"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<InstitutionTeam>() {
        override fun areItemsTheSame(oldItem: InstitutionTeam, newItem: InstitutionTeam): Boolean {
            return oldItem.teamId == newItem.teamId
        }

        override fun areContentsTheSame(oldItem: InstitutionTeam, newItem: InstitutionTeam): Boolean {
            return oldItem == newItem
        }
    }
}
