package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.Facility
import com.example.prathibhascanfinal.R

class FacilityListAdapter(
    private val onView: (Facility) -> Unit,
    private val onEdit: (Facility) -> Unit,
    private val onDelete: (Facility) -> Unit
) : ListAdapter<Facility, FacilityListAdapter.ViewHolder>(DiffCallback) {

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
        
        holder.title.text = "🏟️ ${item.name}"
        holder.status.text = item.availability
        holder.status.setTextColor(if(item.availability == "Available") 0xFF10B981.toInt() else 0xFFF59E0B.toInt())
        
        holder.detail1.text = "Type: ${item.type} | Sport: ${item.sport}"
        holder.detail2.text = "Capacity: ${item.capacity} | ${if(item.isIndoor) "Indoor" else "Outdoor"}"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<Facility>() {
        override fun areItemsTheSame(oldItem: Facility, newItem: Facility): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Facility, newItem: Facility): Boolean {
            return oldItem == newItem
        }
    }
}
