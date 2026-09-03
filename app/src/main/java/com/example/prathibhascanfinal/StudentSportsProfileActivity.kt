package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.firebase.firestore.FirebaseFirestore

class StudentSportsProfileActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_sports_profile)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        // Mocking Data Load from Firestore
        db.collection("student_profiles").document("sample_student").get()
            .addOnSuccessListener {
                // Handle Success
            }
            .addOnFailureListener {
                // Handle Failure
            }

        Toast.makeText(this, "Loading Comprehensive Sports Profile...", Toast.LENGTH_SHORT).show()
    }
}

