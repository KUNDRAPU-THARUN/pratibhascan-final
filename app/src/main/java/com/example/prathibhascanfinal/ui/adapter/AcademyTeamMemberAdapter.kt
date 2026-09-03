package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.AcademyAthlete
import com.example.prathibhascanfinal.R

class AcademyTeamMemberAdapter(
    private val onViewPlayer: (AcademyAthlete) -> Unit,
    private val onEditPlayer: (AcademyAthlete) -> Unit,
    private val onRemovePlayer: (AcademyAthlete) -> Unit
) : ListAdapter<AcademyAthlete, AcademyTeamMemberAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_academy_player_name)
        val tvRoleBadge: TextView = view.findViewById(R.id.tv_academy_player_role_badge)
        val tvSubInfo: TextView = view.findViewById(R.id.tv_academy_player_sub_info)
        val tvJersey: TextView = view.findViewById(R.id.tv_academy_player_jersey)
        val tvPosition: TextView = view.findViewById(R.id.tv_academy_player_position)
        val tvStatus: TextView = view.findViewById(R.id.tv_academy_player_status)

        val btnView: Button = view.findViewById(R.id.btn_view_academy_player)
        val btnEdit: Button = view.findViewById(R.id.btn_edit_academy_player)
        val btnRemove: Button = view.findViewById(R.id.btn_remove_academy_player)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_academy_team_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val athlete = getItem(position)

        holder.tvName.text = athlete.fullName.ifEmpty { "Athlete #${athlete.athleteId}" }

        val idStr = if (athlete.admissionNumber.isNotEmpty()) "ID: ${athlete.admissionNumber}" else "ID: ATH-${athlete.athleteId}"
        val ageStr = if (athlete.age > 0) "Age: ${athlete.age}" else ""
        holder.tvSubInfo.text = if (ageStr.isNotEmpty()) "$idStr | $ageStr" else idStr

        val jerseyText = if (athlete.jerseyNumber.isNotEmpty()) "No: ${athlete.jerseyNumber}" else "No: --"
        holder.tvJersey.text = jerseyText

        val posText = if (athlete.position.isNotEmpty()) athlete.position else "Player"
        holder.tvPosition.text = posText

        val statusText = if (athlete.isActive && athlete.membershipStatus.equals("Active", ignoreCase = true)) "Active" else "Inactive"
        holder.tvStatus.text = statusText
        if (statusText == "Active") {
            holder.tvStatus.setTextColor(0xFF22C55E.toInt())
            holder.tvStatus.setBackgroundColor(0xFF052E16.toInt())
        } else {
            holder.tvStatus.setTextColor(0xFFEF4444.toInt())
            holder.tvStatus.setBackgroundColor(0xFF450A0A.toInt())
        }

        // Role Badge
        when (athlete.role.lowercase()) {
            "captain" -> {
                holder.tvRoleBadge.visibility = View.VISIBLE
                holder.tvRoleBadge.text = "⭐ CAPTAIN"
                holder.tvRoleBadge.setTextColor(0xFFEAB308.toInt())
                holder.tvRoleBadge.setBackgroundColor(0xFF422006.toInt())
            }
            "vice-captain", "vice captain" -> {
                holder.tvRoleBadge.visibility = View.VISIBLE
                holder.tvRoleBadge.text = "🛡️ VICE-CAPTAIN"
                holder.tvRoleBadge.setTextColor(0xFF3B82F6.toInt())
                holder.tvRoleBadge.setBackgroundColor(0xFF1E3A8A.toInt())
            }
            else -> {
                holder.tvRoleBadge.visibility = View.GONE
            }
        }

        holder.btnView.setOnClickListener { onViewPlayer(athlete) }
        holder.btnEdit.setOnClickListener { onEditPlayer(athlete) }
        holder.btnRemove.setOnClickListener { onRemovePlayer(athlete) }
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
