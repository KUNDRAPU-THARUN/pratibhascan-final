package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.R
import com.example.prathibhascanfinal.Tournament
import java.text.SimpleDateFormat
import java.util.*

class TournamentListAdapter(
    private val onView: (Tournament) -> Unit,
    private val onEdit: ((Tournament) -> Unit)? = null,
    private val onDelete: ((Tournament) -> Unit)? = null,
    private val onApply: ((Tournament) -> Unit)? = null,
    private val onRegistrations: ((Tournament) -> Unit)? = null
) : ListAdapter<Tournament, TournamentListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_card_title)
        val status: TextView = view.findViewById(R.id.tv_card_status)
        val detail1: TextView = view.findViewById(R.id.tv_card_detail1)
        val detail2: TextView = view.findViewById(R.id.tv_card_detail2)
        
        val btnView: View = view.findViewById(R.id.btn_card_view)
        val btnEdit: View = view.findViewById(R.id.btn_card_edit)
        val btnDelete: View = view.findViewById(R.id.btn_card_delete)
        val btnApply: com.google.android.material.button.MaterialButton? = view.findViewById(R.id.btn_card_apply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (onApply != null) R.layout.item_tournament_apply_card else R.layout.item_management_card
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        holder.title.text = "🏆 ${item.title}"
        holder.status.text = item.level
        holder.status.setTextColor(0xFF3B82F6.toInt()) 
        
        holder.detail1.text = "Sport: ${item.sport} | Venue: ${item.venue}"
        holder.detail2.text = "Fee: ${item.entryFee} | Deadline: ${if (item.registrationDeadline > 0) sdf.format(Date(item.registrationDeadline)) else "N/A"}"
        
        holder.btnView.setOnClickListener { 
            if (onRegistrations != null) onRegistrations.invoke(item)
            else onView(item) 
        }
        if (onRegistrations != null && holder.btnView is com.google.android.material.button.MaterialButton) {
            holder.btnView.text = "REGS"
        }
        
        holder.btnEdit?.setOnClickListener { onEdit?.invoke(item) }
        holder.btnDelete?.setOnClickListener { onDelete?.invoke(item) }
        holder.btnApply?.setOnClickListener { onApply?.invoke(item) }
        
        if (onApply != null) {
             holder.btnEdit.visibility = View.GONE
             holder.btnDelete.visibility = View.GONE
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Tournament>() {
        override fun areItemsTheSame(oldItem: Tournament, newItem: Tournament): Boolean {
            return oldItem.tournamentId == newItem.tournamentId
        }

        override fun areContentsTheSame(oldItem: Tournament, newItem: Tournament): Boolean {
            return oldItem == newItem
        }
    }
}
