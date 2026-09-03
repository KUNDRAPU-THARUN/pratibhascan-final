package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.InstitutionTeacher
import com.example.prathibhascanfinal.R

class TeacherListAdapter(
    private val onView: (InstitutionTeacher) -> Unit,
    private val onEdit: (InstitutionTeacher) -> Unit,
    private val onDelete: (InstitutionTeacher) -> Unit
) : ListAdapter<InstitutionTeacher, TeacherListAdapter.ViewHolder>(DiffCallback) {

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
        
        holder.title.text = "👨‍🏫 ${item.fullName}"
        holder.status.text = if (item.isVerified) "Verified" else "Pending"
        holder.status.setTextColor(if(item.isVerified) 0xFF10B981.toInt() else 0xFFF59E0B.toInt())
        
        holder.detail1.text = "Specialization: ${item.specialization}"
        holder.detail2.text = "${item.experienceYears} Yrs Experience • ${item.phone}"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<InstitutionTeacher>() {
        override fun areItemsTheSame(oldItem: InstitutionTeacher, newItem: InstitutionTeacher): Boolean {
            return oldItem.teacherId == newItem.teacherId
        }

        override fun areContentsTheSame(oldItem: InstitutionTeacher, newItem: InstitutionTeacher): Boolean {
            return oldItem == newItem
        }
    }
}
