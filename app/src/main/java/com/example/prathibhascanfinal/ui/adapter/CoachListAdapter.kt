package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.Coach
import com.example.prathibhascanfinal.R

class CoachListAdapter(
    private val onView: (Coach) -> Unit,
    private val onEdit: (Coach) -> Unit,
    private val onDelete: (Coach) -> Unit
) : ListAdapter<Coach, CoachListAdapter.ViewHolder>(DiffCallback) {

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
        
        holder.title.text = "👤 ${item.name}"
        holder.status.text = item.status
        holder.status.setTextColor(if(item.status == "Active") 0xFF10B981.toInt() else 0xFFEF4444.toInt())
        
        holder.detail1.text = "Specialization: ${item.specialization}"
        holder.detail2.text = "${item.experienceYears} Yrs Experience • ${item.phone}"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<Coach>() {
        override fun areItemsTheSame(oldItem: Coach, newItem: Coach): Boolean {
            return oldItem.coachId == newItem.coachId
        }

        override fun areContentsTheSame(oldItem: Coach, newItem: Coach): Boolean {
            return oldItem == newItem
        }
    }
}
