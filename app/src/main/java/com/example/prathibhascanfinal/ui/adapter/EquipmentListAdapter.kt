package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.InstitutionEquipment
import com.example.prathibhascanfinal.R

class EquipmentListAdapter(
    private val onView: (InstitutionEquipment) -> Unit,
    private val onEdit: (InstitutionEquipment) -> Unit,
    private val onDelete: (InstitutionEquipment) -> Unit
) : ListAdapter<InstitutionEquipment, EquipmentListAdapter.ViewHolder>(DiffCallback) {

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
        
        holder.title.text = "📦 ${item.name}"
        holder.status.text = item.category
        holder.status.setTextColor(0xFFEF4444.toInt()) // Red
        
        val available = item.totalQuantity - item.issuedQuantity
        holder.detail1.text = "Stock: $available / ${item.totalQuantity}"
        holder.detail2.text = "Condition: New | Last Maintained: Recently"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<InstitutionEquipment>() {
        override fun areItemsTheSame(oldItem: InstitutionEquipment, newItem: InstitutionEquipment): Boolean {
            return oldItem.equipmentId == newItem.equipmentId
        }

        override fun areContentsTheSame(oldItem: InstitutionEquipment, newItem: InstitutionEquipment): Boolean {
            return oldItem == newItem
        }
    }
}
