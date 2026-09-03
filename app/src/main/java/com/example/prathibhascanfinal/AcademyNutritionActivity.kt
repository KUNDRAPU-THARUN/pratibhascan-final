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

class AcademyNutritionActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var adapter: DietAdapter
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_nutrition)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        academyId = intent.getIntExtra("ACADEMY_ID", 0)

        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyNutritionActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                } else {
                    Toast.makeText(this@AcademyNutritionActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
        }
    }

    private fun initUI() {
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_add_diet).setOnClickListener {
            addNewDietPlan()
        }

        observeDietPlans()
    }

    private fun addNewDietPlan() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_diet, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_confirm_add_diet).setOnClickListener {
            val aId = dialogView.findViewById<EditText>(R.id.et_diet_athlete_id).text.toString().toIntOrNull() ?: 0
            val cal = dialogView.findViewById<EditText>(R.id.et_diet_calories).text.toString().toIntOrNull() ?: 2000
            val goals = dialogView.findViewById<EditText>(R.id.et_diet_goals).text.toString().trim()

            if (aId > 0) {
                val plan = DietPlan(
                    athleteId = aId,
                    caloriesTarget = cal,
                    goals = goals
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyNutritionActivity)
                    repo.saveDietPlan(plan)
                    dialog.dismiss()
                    Toast.makeText(this@AcademyNutritionActivity, "Diet Plan Saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Athlete ID is required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_diet_plans)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = DietAdapter()
        rv.adapter = adapter
    }

    private fun observeDietPlans() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyNutritionActivity).academyManagementDao()
            db.getAcademyDietPlans(academyId).collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    class DietAdapter : RecyclerView.Adapter<DietAdapter.ViewHolder>() {
        private var list = emptyList<DietPlan>()

        fun submitList(newList: List<DietPlan>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.title.text = "Plan for Athlete ID: ${item.athleteId}"
            holder.title.setTextColor(android.graphics.Color.WHITE)
            holder.subtitle.text = "Calories: ${item.caloriesTarget} | Goals: ${item.goals}"
            holder.subtitle.setTextColor(android.graphics.Color.LTGRAY)
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(android.R.id.text1)
            val subtitle: TextView = v.findViewById(android.R.id.text2)
        }
    }
}

