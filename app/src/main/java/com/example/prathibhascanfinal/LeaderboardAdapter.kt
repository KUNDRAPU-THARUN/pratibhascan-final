package com.example.prathibhascanfinal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.databinding.ItemLeaderboardEntryBinding

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    private var athletes = listOf<User>()

    fun submitList(list: List<User>) {
        athletes = list.sortedByDescending { it.totalXP }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val athlete = athletes[position]
        holder.binding.tvRankNum.text = "#${position + 1}"
        holder.binding.tvRankName.text = athlete.fullName
        holder.binding.tvRankLocation.text = athlete.location ?: "Unknown Location"
        holder.binding.tvRankPts.text = "${athlete.totalXP} pts"
    }

    override fun getItemCount(): Int = athletes.size

    class ViewHolder(val binding: ItemLeaderboardEntryBinding) : RecyclerView.ViewHolder(binding.root)
}
