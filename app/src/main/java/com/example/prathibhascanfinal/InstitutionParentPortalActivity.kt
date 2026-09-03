package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class InstitutionParentPortalActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_parent_portal)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<android.view.View>(R.id.btn_view_attendance).setOnClickListener {
            Toast.makeText(this, "Fetching Real-time Attendance from Firestore...", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.btn_view_ai_reports).setOnClickListener {
            Toast.makeText(this, "Loading AI Performance Reports...", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.btn_download_certs).setOnClickListener {
            Toast.makeText(this, "Accessing Digital Vault (Firebase Storage)...", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.btn_parent_notifications).setOnClickListener {
            Toast.makeText(this, "Opening Notification Center...", Toast.LENGTH_SHORT).show()
        }
    }
}

