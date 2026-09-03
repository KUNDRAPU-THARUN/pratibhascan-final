package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.prathibhascanfinal.R
import com.example.prathibhascanfinal.User
import com.google.android.material.imageview.ShapeableImageView

class DiscoveryAdapter(
    private val onProfile: (User) -> Unit,
    private val onInvite: (User) -> Unit,
    private val scoreCalculator: (User) -> Int
) : ListAdapter<User, DiscoveryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ShapeableImageView = view.findViewById(R.id.iv_athlete_photo)
        val name: TextView = view.findViewById(R.id.tv_athlete_name)
        val sport: TextView = view.findViewById(R.id.tv_athlete_sport)
        val location: TextView = view.findViewById(R.id.tv_athlete_location)
        val score: TextView = view.findViewById(R.id.tv_talent_score_value)
        val btnProfile: View = view.findViewById(R.id.btn_view_profile)
        val btnInvite: View = view.findViewById(R.id.btn_invite_athlete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_discovery_athlete, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.name.text = item.fullName
        holder.sport.text = "${item.primaryDiscipline ?: "Multi-Sport"} • ${item.experienceLevel ?: "Emerging"}"
        holder.location.text = "${item.location ?: "India"}, ${item.state ?: ""}"
        holder.score.text = scoreCalculator(item).toString()
        
        if (item.profilePicture != null) {
            holder.photo.load(item.profilePicture)
        } else {
            holder.photo.setImageResource(R.drawable.ic_profile_placeholder)
        }

        holder.btnProfile.setOnClickListener { onProfile(item) }
        holder.btnInvite.setOnClickListener { onInvite(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.email == newItem.email
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
