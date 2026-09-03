package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.AcademyAthlete
import com.example.prathibhascanfinal.R

class AthleteListAdapter(
    private val onView: (AcademyAthlete) -> Unit,
    private val onEdit: (AcademyAthlete) -> Unit,
    private val onDelete: (AcademyAthlete) -> Unit
) : ListAdapter<AcademyAthlete, AthleteListAdapter.ViewHolder>(DiffCallback) {

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
        
        holder.title.text = item.fullName
        holder.status.text = item.verificationStatus
        val statusColor = when(item.verificationStatus) {
            "Verified" -> 0xFF10B981.toInt()
            "Rejected" -> 0xFFEF4444.toInt()
            else -> 0xFFF59E0B.toInt()
        }
        holder.status.setTextColor(statusColor)
        
        holder.detail1.text = "Sport: ${item.sportDomain} | Skill: ${item.skillLevel}"
        holder.detail2.text = "ID: ${item.admissionNumber} • Age: ${item.age} • ${item.gender}"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<AcademyAthlete>() {
        override fun areItemsTheSame(oldItem: AcademyAthlete, newItem: AcademyAthlete): Boolean {
            return oldItem.athleteId == newItem.athleteId
        }

        override fun areContentsTheSame(oldItem: AcademyAthlete, newItem: AcademyAthlete): Boolean {
            return oldItem == newItem
        }
    }
}
