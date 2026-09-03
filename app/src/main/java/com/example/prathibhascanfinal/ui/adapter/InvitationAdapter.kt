package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.AcademyInvitation
import com.example.prathibhascanfinal.R

class InvitationAdapter(
    private val onAccept: (AcademyInvitation) -> Unit,
    private val onDecline: (AcademyInvitation) -> Unit
) : ListAdapter<AcademyInvitation, InvitationAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_card_title)
        val status: TextView = view.findViewById(R.id.tv_card_status)
        val details: TextView = view.findViewById(R.id.tv_card_detail1)
        val message: TextView = view.findViewById(R.id.tv_card_detail2)
        val btnAccept: View = view.findViewById(R.id.btn_card_view)
        val btnDecline: View = view.findViewById(R.id.btn_card_delete)
        val btnEdit: View = view.findViewById(R.id.btn_card_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_management_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.academyName
        holder.status.text = item.status
        holder.details.text = "Sport: ${item.sport}"
        holder.message.text = item.message
        
        holder.btnEdit.visibility = View.GONE
        (holder.btnAccept as com.google.android.material.button.MaterialButton).apply {
            text = "ACCEPT"
            icon = null
            setOnClickListener { onAccept(item) }
        }
        (holder.btnDecline as com.google.android.material.button.MaterialButton).apply {
            text = "DECLINE"
            icon = null
            setOnClickListener { onDecline(item) }
        }

        if (item.status != "PENDING") {
            holder.btnAccept.visibility = View.GONE
            holder.btnDecline.visibility = View.GONE
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<AcademyInvitation>() {
        override fun areItemsTheSame(oldItem: AcademyInvitation, newItem: AcademyInvitation): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AcademyInvitation, newItem: AcademyInvitation): Boolean = oldItem == newItem
    }
}
