package com.example.prathibhascanfinal

import androidx.activity.viewModels

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

class AcademyMedicalActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var adapter: MedicalAdapter
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_medical)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyMedicalActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                } else {
                    Toast.makeText(this@AcademyMedicalActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
        }
    }

    private fun initUI() {
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_add_medical).setOnClickListener {
            addNewMedicalRecord()
        }

        observeMedicalRecords()
    }

    private fun addNewMedicalRecord() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medical, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_confirm_add_medical).setOnClickListener {
            val aId = dialogView.findViewById<EditText>(R.id.et_medical_athlete_id).text.toString().toIntOrNull() ?: 0
            val injury = dialogView.findViewById<EditText>(R.id.et_medical_injury).text.toString().trim()
            val notes = dialogView.findViewById<EditText>(R.id.et_medical_notes).text.toString().trim()

            if (aId > 0 && injury.isNotEmpty()) {
                val record = MedicalRecord(
                    athleteId = aId,
                    date = System.currentTimeMillis(),
                    injuryType = injury,
                    physiotherapyNotes = notes
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyMedicalActivity)
                    repo.saveMedicalRecord(record)
                    dialog.dismiss()
                    Toast.makeText(this@AcademyMedicalActivity, "Medical Record Saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Athlete ID and Injury are required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_medical_records)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MedicalAdapter()
        rv.adapter = adapter
    }

    private fun observeMedicalRecords() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyMedicalActivity).academyManagementDao()
            db.getAcademyMedicalRecords(academyId).collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    class MedicalAdapter : RecyclerView.Adapter<MedicalAdapter.ViewHolder>() {
        private var list = emptyList<MedicalRecord>()

        fun submitList(newList: List<MedicalRecord>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.title.text = item.injuryType
            holder.title.setTextColor(android.graphics.Color.WHITE)
            holder.subtitle.text = "${sdf.format(Date(item.date))} | ${item.physiotherapyNotes}"
            holder.subtitle.setTextColor(android.graphics.Color.LTGRAY)
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(android.R.id.text1)
            val subtitle: TextView = v.findViewById(android.R.id.text2)
        }
    }
}

