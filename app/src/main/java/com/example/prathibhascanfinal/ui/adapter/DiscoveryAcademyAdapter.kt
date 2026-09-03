package com.example.prathibhascanfinal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.prathibhascanfinal.Academy
import com.example.prathibhascanfinal.R
import com.google.android.material.button.MaterialButton

class DiscoveryAcademyAdapter(
    private val onDetails: (Academy) -> Unit,
    private val onApply: (Academy) -> Unit
) : ListAdapter<Academy, DiscoveryAcademyAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.iv_academy_logo)
        val name: TextView = view.findViewById(R.id.tv_academy_name)
        val type: TextView = view.findViewById(R.id.tv_academy_type)
        val location: TextView = view.findViewById(R.id.tv_academy_location)
        val sports: TextView = view.findViewById(R.id.tv_academy_sports)
        val verified: View = view.findViewById(R.id.iv_verified_badge)
        val btnView: MaterialButton = view.findViewById(R.id.btn_view_academy)
        val btnApply: MaterialButton = view.findViewById(R.id.btn_apply_academy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_discovery_academy, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val academy = getItem(position)
        holder.name.text = academy.academyName
        holder.type.text = academy.academyType
        holder.location.text = "${academy.city}, ${academy.state}"
        holder.sports.text = academy.specializedDomains
        holder.verified.visibility = if (academy.isVerified) View.VISIBLE else View.GONE

        holder.logo.load(academy.logoUri) {
            placeholder(R.drawable.ic_stadium_facility)
            crossfade(true)
        }

        holder.btnView.setOnClickListener { onDetails(academy) }
        holder.btnApply.setOnClickListener { onApply(academy) }
    }

    object DiffCallback : DiffUtil.ItemCallback<Academy>() {
        override fun areItemsTheSame(oldItem: Academy, newItem: Academy): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Academy, newItem: Academy): Boolean = oldItem == newItem
    }
}
