package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AcademyBookingActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var adapter: BookingAdapter
    private val slotList = mutableListOf<TrainingSlot>()
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_booking)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyBookingActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                } else {
                    Toast.makeText(this@AcademyBookingActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
        }
    }

    private fun initUI() {
        val rv = findViewById<RecyclerView>(R.id.rv_slots)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = BookingAdapter(slotList)
        rv.adapter = adapter

        findViewById<View>(R.id.fab_book_slot).setOnClickListener {
            bookNewSlot()
        }

        loadSlots()
    }

    private fun loadSlots() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyBookingActivity)
            val items = db.academyManagementDao().getSlots(academyId)
            slotList.clear()
            slotList.addAll(items)
            
            findViewById<View>(R.id.layout_empty_slots).visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            
            adapter.notifyDataSetChanged()
        }
    }

    private fun bookNewSlot() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_book_slot, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btn_confirm_book_slot).setOnClickListener {
            val ground = dialogView.findViewById<EditText>(R.id.et_slot_ground).text.toString().trim()
            val type = dialogView.findViewById<EditText>(R.id.et_slot_type).text.toString().trim()
            val offset = dialogView.findViewById<EditText>(R.id.et_slot_start_offset).text.toString().toLongOrNull() ?: 60

            if (ground.isNotEmpty()) {
                val start = System.currentTimeMillis() + (offset * 60000)
                val newSlot = TrainingSlot(
                    academyId = academyId,
                    groundName = ground,
                    startTime = start,
                    endTime = start + 3600000, // Default 1 hour
                    sessionType = type
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@AcademyBookingActivity).academyManagementDao().insertSlot(newSlot)
                    FirebaseManager.saveTrainingSlot(newSlot)
                    loadSlots()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Ground Name is required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    class BookingAdapter(private val items: List<TrainingSlot>) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {
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
            val item = items[position]
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            
            holder.title.text = "🏟 ${item.groundName}"
            holder.status.text = if (System.currentTimeMillis() < item.endTime) "Confirmed" else "Expired"
            holder.status.setTextColor(if (System.currentTimeMillis() < item.endTime) 0xFF10B981.toInt() else 0xFFEF4444.toInt())
            
            holder.detail1.text = "Type: ${item.sessionType} | Date: ${dateSdf.format(Date(item.startTime))}"
            holder.detail2.text = "Time: ${sdf.format(Date(item.startTime))} - ${sdf.format(Date(item.endTime))}"
            
            holder.btnView.setOnClickListener { /* View Logic */ }
            holder.btnEdit.setOnClickListener { /* Edit Logic */ }
            holder.btnDelete.setOnClickListener { /* Delete Logic */ }
        }

        override fun getItemCount() = items.size
    }
}

