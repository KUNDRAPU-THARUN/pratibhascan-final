package com.example.prathibhascanfinal.ui.notification

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.R
import com.example.prathibhascanfinal.data.AppNotification
import com.example.prathibhascanfinal.data.NotificationCategories

class NotificationAdapter(
    private val onNotificationClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.NotificationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<CardView>(R.id.card_notification)
        private val icon = itemView.findViewById<ImageView>(R.id.iv_category_icon)
        private val title = itemView.findViewById<TextView>(R.id.tv_title)
        private val time = itemView.findViewById<TextView>(R.id.tv_time)
        private val description = itemView.findViewById<TextView>(R.id.tv_description)
        private val badge = itemView.findViewById<TextView>(R.id.tv_category_badge)
        private val unreadIndicator = itemView.findViewById<View>(R.id.unread_indicator)

        fun bind(notification: AppNotification) {
            title.text = notification.title
            description.text = notification.description
            time.text = DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            badge.text = notification.category.uppercase()
            val categoryColor = NotificationCategories.getColor(notification.category)
            badge.backgroundTintList = ColorStateList.valueOf(categoryColor)
            icon.setImageResource(NotificationCategories.getIcon(notification.category))
            icon.imageTintList = ColorStateList.valueOf(categoryColor)

            if (notification.isRead) {
                card.setCardBackgroundColor(Color.parseColor("#0F172A")) // Lighter/Darker background for read
                unreadIndicator.isVisible = false
            } else {
                card.setCardBackgroundColor(Color.parseColor("#1E293B")) // Highlighted background for unread
                unreadIndicator.isVisible = true
            }

            itemView.setOnClickListener { onNotificationClick(notification) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem == newItem
        }
    }
}
