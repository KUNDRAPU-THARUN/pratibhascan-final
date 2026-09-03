package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.Student
import com.example.prathibhascanfinal.R

class StudentListAdapter(
    private val onView: (Student) -> Unit,
    private val onEdit: (Student) -> Unit,
    private val onDelete: (Student) -> Unit
) : ListAdapter<Student, StudentListAdapter.ViewHolder>(DiffCallback) {

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
        
        holder.title.text = "🎓 ${item.fullName}"
        holder.status.text = item.healthStatus
        holder.status.setTextColor(if(item.healthStatus == "Good") 0xFF10B981.toInt() else 0xFFF59E0B.toInt())
        
        holder.detail1.text = "Sport: ${item.selectedSport} | Class: ${item.grade}-${item.section}"
        holder.detail2.text = "ID: ${item.rollNumber} • Age: ${item.age} • AI Score: ${item.aiFitnessScore}"
        
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<Student>() {
        override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean {
            return oldItem.studentId == newItem.studentId
        }

        override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean {
            return oldItem == newItem
        }
    }
}
