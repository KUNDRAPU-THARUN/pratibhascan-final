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

class InstitutionMedicalActivity : BaseActivity() {

    override val viewModel: InstitutionPortalViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[InstitutionPortalViewModel::class.java]
    }
    
    private lateinit var adapter: MedicalAdapter
    private var institutionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_medical)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_medical).setNavigationOnClickListener { finish() }

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionMedicalActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                initUI()
            } else {
                Toast.makeText(this@InstitutionMedicalActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
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
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_confirm_add_medical).setOnClickListener {
            val sId = dialogView.findViewById<EditText>(R.id.et_medical_athlete_id).text.toString().toIntOrNull() ?: 0
            val injury = dialogView.findViewById<EditText>(R.id.et_medical_injury).text.toString().trim()
            val notes = dialogView.findViewById<EditText>(R.id.et_medical_notes).text.toString().trim()

            if (sId > 0 && injury.isNotEmpty()) {
                val record = InstitutionMedicalRecord(
                    institutionId = institutionId,
                    studentId = sId,
                    date = System.currentTimeMillis(),
                    injuryType = injury,
                    physiotherapyNotes = notes
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.InstitutionRepository(this@InstitutionMedicalActivity)
                    repo.saveMedicalRecord(record)
                    dialog.dismiss()
                    Toast.makeText(this@InstitutionMedicalActivity, "Record Added & Synced", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Student ID and Injury are required", Toast.LENGTH_SHORT).show()
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
            val db = AppDatabase.getDatabase(this@InstitutionMedicalActivity).institutionManagementDao()
            db.getMedicalRecordsFlow(institutionId).collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    class MedicalAdapter : RecyclerView.Adapter<MedicalAdapter.ViewHolder>() {
        private var list = emptyList<InstitutionMedicalRecord>()

        fun submitList(newList: List<InstitutionMedicalRecord>) {
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
            holder.title.text = "Student #${item.studentId}: ${item.injuryType}"
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
