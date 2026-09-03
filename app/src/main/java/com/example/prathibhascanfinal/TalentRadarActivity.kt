package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity

class TalentRadarActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talent_radar)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<Button>(R.id.btn_drop_invite)?.setOnClickListener {
            Toast.makeText(this, "Trial Invitation Sent to Athlete!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_close_radar)?.setOnClickListener {
            finish()
        }
    }
}

