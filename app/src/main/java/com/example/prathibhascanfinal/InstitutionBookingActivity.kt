package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InstitutionBookingActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var adapter: BookingAdapter
    private val bookingList = mutableListOf<GroundBooking>()
    private var institutionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_booking)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""

        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionBookingActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                initUI()
            } else {
                Toast.makeText(this@InstitutionBookingActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initUI() {
        val rv = findViewById<RecyclerView>(R.id.rv_inst_bookings)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = BookingAdapter(bookingList)
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_book_facility).setOnClickListener {
            showAddBookingDialog()
        }

        loadBookings()
    }

    private fun loadBookings() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InstitutionBookingActivity)
            val items = db.institutionManagementDao().getBookings(institutionId)
            bookingList.clear()
            bookingList.addAll(items)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddBookingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_booking, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btn_confirm_booking).setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.et_facility_name).text.toString().trim()
            val purpose = dialogView.findViewById<EditText>(R.id.et_booking_purpose).text.toString().trim()

            if (name.isNotEmpty()) {
                val newBooking = GroundBooking(
                    institutionId = institutionId,
                    facilityName = name,
                    startTime = System.currentTimeMillis() + 3600000,
                    endTime = System.currentTimeMillis() + 7200000,
                    purpose = purpose
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.InstitutionRepository(this@InstitutionBookingActivity)
                    repo.addBooking(newBooking)
                    loadBookings()
                    dialog.dismiss()
                    Toast.makeText(this@InstitutionBookingActivity, "Facility Booked Successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Facility Name is required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    class BookingAdapter(private val items: List<GroundBooking>) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(android.R.id.text1)
            val details: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.name.text = item.facilityName
            holder.name.setTextColor(android.graphics.Color.WHITE)
            holder.details.text = "${sdf.format(Date(item.startTime))} - ${sdf.format(Date(item.endTime))} | ${item.purpose}"
            holder.details.setTextColor(android.graphics.Color.LTGRAY)
        }

        override fun getItemCount() = items.size
    }
}

