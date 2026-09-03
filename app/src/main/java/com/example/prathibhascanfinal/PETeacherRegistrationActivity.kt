package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.firebase.firestore.FirebaseFirestore

class PETeacherRegistrationActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pe_teacher_registration)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        val etName = findViewById<EditText>(R.id.et_teacher_name)
        val etInst = findViewById<EditText>(R.id.et_assigned_inst)

        findViewById<Button>(R.id.btn_submit_teacher_reg).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf(
                "name" to name,
                "institution" to etInst.text.toString().trim(),
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("pe_teachers").add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Registration Submitted for Verification", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Verification Error", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

