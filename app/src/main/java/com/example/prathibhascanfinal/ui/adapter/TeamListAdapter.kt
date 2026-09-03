package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.R
import com.example.prathibhascanfinal.Team
import java.text.SimpleDateFormat
import java.util.*

class TeamListAdapter(
    private val onView: (Team) -> Unit,
    private val onEdit: (Team) -> Unit,
    private val onDelete: (Team) -> Unit
) : ListAdapter<Team, TeamListAdapter.ViewHolder>(DiffCallback) {

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
        holder.status.setTextColor(0xFF22C55E.toInt()) 
        
        val catStr = if (item.category.isNotEmpty()) " | ${item.category}" else ""
        val ageStr = if (item.ageGroup.isNotEmpty()) " (${item.ageGroup})" else ""
        val genderStr = if (item.gender.isNotEmpty()) " [${item.gender}]" else ""
        holder.detail1.text = "Sport: ${item.sport}$catStr$ageStr$genderStr"

        val coachStr = item.coachName ?: "Unassigned"
        holder.detail2.text = "Coach: $coachStr"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<Team>() {
        override fun areItemsTheSame(oldItem: Team, newItem: Team): Boolean {
            return oldItem.teamId == newItem.teamId
        }

        override fun areContentsTheSame(oldItem: Team, newItem: Team): Boolean {
            return oldItem == newItem
        }
    }
}
