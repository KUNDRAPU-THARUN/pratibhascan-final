package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.firebase.firestore.FirebaseFirestore

class UniversityRecruitmentActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_university_recruitment)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        Toast.makeText(this, "Loading University Recruitment Network...", Toast.LENGTH_SHORT).show()
    }
}

