package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InstitutionCalendarActivity : BaseActivity() {

    override val viewModel: InstitutionPortalViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[InstitutionPortalViewModel::class.java]
    }
    
    private lateinit var adapter: SlotAdapter
    private var institutionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_calendar)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_calendar).setNavigationOnClickListener { finish() }

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionCalendarActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                initUI()
            } else {
                Toast.makeText(this@InstitutionCalendarActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initUI() {
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_add_slot).setOnClickListener {
            addNewSlot()
        }

        observeSlots()
    }

    private fun addNewSlot() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_book_slot, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_confirm_book_slot).setOnClickListener {
            val venue = dialogView.findViewById<EditText>(R.id.et_slot_ground).text.toString().trim()
            val type = dialogView.findViewById<EditText>(R.id.et_slot_type).text.toString().trim()

            if (venue.isNotEmpty()) {
                val slot = InstitutionTrainingSlot(
                    institutionId = institutionId,
                    sportName = type,
                    dayOfWeek = "Today",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + 3600000,
                    venue = venue
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.InstitutionRepository(this@InstitutionCalendarActivity)
                    repo.saveTrainingSlot(slot)
                    dialog.dismiss()
                    Toast.makeText(this@InstitutionCalendarActivity, "Slot Scheduled & Synced", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Sport name is required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_calendar_slots)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SlotAdapter()
        rv.adapter = adapter
    }

    private fun observeSlots() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InstitutionCalendarActivity).institutionManagementDao()
            db.getTrainingSlotsFlow(institutionId).collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    class SlotAdapter : RecyclerView.Adapter<SlotAdapter.ViewHolder>() {
        private var list = emptyList<InstitutionTrainingSlot>()

        fun submitList(newList: List<InstitutionTrainingSlot>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            holder.title.text = "${item.sportName} Training"
            holder.title.setTextColor(android.graphics.Color.WHITE)
            holder.subtitle.text = "${item.dayOfWeek} | ${sdf.format(Date(item.startTime))}"
            holder.subtitle.setTextColor(android.graphics.Color.LTGRAY)
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(android.R.id.text1)
            val subtitle: TextView = v.findViewById(android.R.id.text2)
        }
    }
}
