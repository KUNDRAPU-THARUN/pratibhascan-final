package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity

class InstitutionSportsAnalyticsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_sports_analytics)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<AnalyticsGraphView>(R.id.graph_inst_performance)?.setData(listOf(0.4f, 0.5f, 0.6f, 0.55f, 0.7f, 0.8f))

        Toast.makeText(this, "Generating Real-time Sports Analytics...", Toast.LENGTH_SHORT).show()
    }
}

