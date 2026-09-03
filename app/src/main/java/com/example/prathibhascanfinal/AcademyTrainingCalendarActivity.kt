package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.launch

class AcademyTrainingCalendarActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_training_calendar)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        val rv = findViewById<RecyclerView>(R.id.rv_training_calendar)
        rv.layoutManager = LinearLayoutManager(this)
        
        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyTrainingCalendarActivity)
            val academy = db.academyDao().getAcademyByEmail(email)
            academy?.let { aca ->
                val slots = db.academyManagementDao().getSlots(aca.id)
                // Reusing BookingAdapter for simplicity as it displays slots
                rv.adapter = AcademyBookingActivity.BookingAdapter(slots)
            }
        }
    }
}

