package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.InstitutionTeamMember
import com.example.prathibhascanfinal.R
import com.example.prathibhascanfinal.Student

data class TeamMemberUiModel(
    val member: InstitutionTeamMember,
    val student: Student?
)

class InstitutionTeamMemberAdapter(
    private val onViewProfile: (TeamMemberUiModel) -> Unit,
    private val onEditMember: (TeamMemberUiModel) -> Unit,
    private val onRemoveMember: (TeamMemberUiModel) -> Unit
) : ListAdapter<TeamMemberUiModel, InstitutionTeamMemberAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_player_name)
        val tvRoleBadge: TextView = view.findViewById(R.id.tv_player_role_badge)
        val tvSubInfo: TextView = view.findViewById(R.id.tv_player_sub_info)
        val tvJersey: TextView = view.findViewById(R.id.tv_player_jersey)
        val tvPosition: TextView = view.findViewById(R.id.tv_player_position)
        val tvPerformance: TextView = view.findViewById(R.id.tv_player_performance)

        val btnProfile: Button = view.findViewById(R.id.btn_view_player_profile)
        val btnEdit: Button = view.findViewById(R.id.btn_edit_player_member)
        val btnRemove: Button = view.findViewById(R.id.btn_remove_player_member)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_institution_team_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val member = item.member
        val student = item.student

        holder.tvName.text = student?.fullName ?: "Student #${member.studentId}"
        
        // Sub info
        val rollStr = if (!student?.rollNumber.isNullOrEmpty()) "ID/Roll: ${student?.rollNumber}" else "Student ID: ${member.studentId}"
        val gradeStr = if (!student?.grade.isNullOrEmpty()) "Class ${student?.grade}-${student?.section}" else ""
        holder.tvSubInfo.text = if (gradeStr.isNotEmpty()) "$rollStr | $gradeStr" else rollStr

        // Jersey
        val jerseyText = if (member.jerseyNumber.isNotEmpty()) "No: ${member.jerseyNumber}" else "No: --"
        holder.tvJersey.text = jerseyText

        // Position
        val posText = if (member.position.isNotEmpty()) member.position else "Player"
        holder.tvPosition.text = posText

        // Performance
        holder.tvPerformance.text = member.performanceStatus
        when (member.performanceStatus.lowercase()) {
            "excellent" -> {
                holder.tvPerformance.setTextColor(0xFF22C55E.toInt())
                holder.tvPerformance.setBackgroundColor(0xFF052E16.toInt())
            }
            "good" -> {
                holder.tvPerformance.setTextColor(0xFF3B82F6.toInt())
                holder.tvPerformance.setBackgroundColor(0xFF1E3A8A.toInt())
            }
            else -> {
                holder.tvPerformance.setTextColor(0xFFEAB308.toInt())
                holder.tvPerformance.setBackgroundColor(0xFF422006.toInt())
            }
        }

        // Role Badge
        when (member.role.lowercase()) {
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

        holder.btnProfile.setOnClickListener { onViewProfile(item) }
        holder.btnEdit.setOnClickListener { onEditMember(item) }
        holder.btnRemove.setOnClickListener { onRemoveMember(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<TeamMemberUiModel>() {
        override fun areItemsTheSame(oldItem: TeamMemberUiModel, newItem: TeamMemberUiModel): Boolean {
            return oldItem.member.memberId == newItem.member.memberId
        }

        override fun areContentsTheSame(oldItem: TeamMemberUiModel, newItem: TeamMemberUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
