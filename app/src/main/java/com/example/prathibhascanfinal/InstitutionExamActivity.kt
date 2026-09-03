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

class InstitutionExamActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var adapter: ExamAdapter
    private val examList = mutableListOf<PracticalExam>()
    private var institutionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_exam)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        val session = SessionManager(this)
        val email = session.getEmail() ?: ""

        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionExamActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                initUI()
            } else {
                Toast.makeText(this@InstitutionExamActivity, "Institution not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initUI() {
        val rv = findViewById<RecyclerView>(R.id.rv_exams)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ExamAdapter(examList)
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_create_exam).setOnClickListener {
            showCreateExamDialog()
        }

        loadExams()
    }

    private fun loadExams() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InstitutionExamActivity)
            val items = db.institutionManagementDao().getExams(institutionId)
            examList.clear()
            examList.addAll(items)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showCreateExamDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_exam, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btn_confirm_exam).setOnClickListener {
            val title = dialogView.findViewById<EditText>(R.id.et_exam_title).text.toString().trim()
            val grade = dialogView.findViewById<EditText>(R.id.et_exam_grade).text.toString().trim()
            val marks = dialogView.findViewById<EditText>(R.id.et_max_marks).text.toString().toIntOrNull() ?: 50

            if (title.isNotEmpty()) {
                val newExam = PracticalExam(
                    institutionId = institutionId,
                    examTitle = title,
                    grade = grade,
                    date = System.currentTimeMillis(),
                    maxMarks = marks
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.InstitutionRepository(this@InstitutionExamActivity)
                    repo.addPracticalExam(newExam)
                    loadExams()
                    dialog.dismiss()
                    Toast.makeText(this@InstitutionExamActivity, "Exam Created Successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Exam Title is required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    class ExamAdapter(private val items: List<PracticalExam>) : RecyclerView.Adapter<ExamAdapter.ViewHolder>() {
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
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.name.text = item.examTitle
            holder.name.setTextColor(android.graphics.Color.WHITE)
            holder.details.text = "${item.grade} | Date: ${sdf.format(Date(item.date))} | Max: ${item.maxMarks}"
            holder.details.setTextColor(android.graphics.Color.LTGRAY)
        }

        override fun getItemCount() = items.size
    }
}

