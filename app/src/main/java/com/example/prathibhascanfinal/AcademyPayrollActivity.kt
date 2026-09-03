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

class AcademyPayrollActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var adapter: PayrollAdapter
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_payroll)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyPayrollActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                } else {
                    Toast.makeText(this@AcademyPayrollActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
        }
    }

    private fun initUI() {
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_add_payroll).setOnClickListener {
            processNewPayroll()
        }

        observePayroll()
    }

    private fun processNewPayroll() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payroll, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_confirm_add_payroll).setOnClickListener {
            val cId = dialogView.findViewById<EditText>(R.id.et_payroll_coach_id).text.toString().toIntOrNull() ?: 0
            val month = dialogView.findViewById<EditText>(R.id.et_payroll_month).text.toString().trim()
            val amount = dialogView.findViewById<EditText>(R.id.et_payroll_amount).text.toString().toDoubleOrNull() ?: 0.0

            if (cId > 0 && month.isNotEmpty()) {
                val payroll = Payroll(
                    academyId = academyId,
                    coachId = cId,
                    monthYear = month,
                    totalPaid = amount,
                    paymentDate = System.currentTimeMillis(),
                    status = "Paid"
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyPayrollActivity)
                    repo.savePayroll(payroll)
                    dialog.dismiss()
                    Toast.makeText(this@AcademyPayrollActivity, "Payroll Processed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Coach ID and Month are required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_payroll_records)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = PayrollAdapter()
        rv.adapter = adapter
    }

    private fun observePayroll() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyPayrollActivity).academyManagementDao()
            db.getAcademyPayroll(academyId).collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    class PayrollAdapter : RecyclerView.Adapter<PayrollAdapter.ViewHolder>() {
        private var list = emptyList<Payroll>()

        fun submitList(newList: List<Payroll>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            holder.title.text = "Coach ID: ${item.coachId} | ${item.monthYear}"
            holder.title.setTextColor(android.graphics.Color.WHITE)
            holder.subtitle.text = "Amount: ₹${item.totalPaid} | Status: ${item.status}"
            holder.subtitle.setTextColor(android.graphics.Color.LTGRAY)
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(android.R.id.text1)
            val subtitle: TextView = v.findViewById(android.R.id.text2)
        }
    }
}

