package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity

class InstitutionAttendanceAnalyticsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_attendance_analytics)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<AnalyticsGraphView>(R.id.graph_attendance)?.setData(listOf(0.8f, 0.85f, 0.9f, 0.88f, 0.92f, 0.95f))

        Toast.makeText(this, "Fetching Attendance Analytics...", Toast.LENGTH_SHORT).show()
    }
}

