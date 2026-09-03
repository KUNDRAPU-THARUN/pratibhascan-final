package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch

class InstitutionRegistrationActivity : BaseActivity() {

    override val viewModel: InstitutionPortalViewModel by lazy {
        ViewModelProvider(this)[InstitutionPortalViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_registration)
        findViewById<android.view.View>(android.R.id.content)?.applySystemBarsPadding()

        val session = SessionManager(this)
        val userEmail = session.getEmail() ?: ""

        val etName = findViewById<EditText>(R.id.et_inst_name)
        val etBoard = findViewById<EditText>(R.id.et_inst_board)
        val etCode = findViewById<EditText>(R.id.et_inst_code)
        val etAddress = findViewById<EditText>(R.id.et_inst_address)
        val etPrincipal = findViewById<EditText>(R.id.et_inst_principal)
        val etPeTeacher = findViewById<EditText>(R.id.et_inst_pe_teacher)
        val etPhone = findViewById<EditText>(R.id.et_inst_phone)

        findViewById<Button>(R.id.btn_register_institution).setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Institution Name and Admin Phone are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(this@InstitutionRegistrationActivity)
                    val institution = Institution(
                        institutionName = name,
                        boardAffiliation = etBoard.text.toString(),
                        affiliationCode = etCode.text.toString(),
                        campusAddress = etAddress.text.toString(),
                        principalName = etPrincipal.text.toString(),
                        sportsCoordinatorName = etPeTeacher.text.toString(),
                        officialPhone = phone,
                        contactEmail = userEmail // CRITICAL: Link to user session
                    )
                    db.institutionDao().insertInstitution(institution)
                    
                    // Sync to Firebase
                    val success = FirebaseManager.saveInstitution(institution)
                    
                    session.setRegistrationComplete(true)

                    Toast.makeText(this@InstitutionRegistrationActivity, 
                        if(success) "Institution Registered & Synced Successfully!" 
                        else "Registered Locally. Firebase sync pending.", 
                        Toast.LENGTH_LONG).show()

                    // Redirect to Premium Portal
                    startActivity(Intent(this@InstitutionRegistrationActivity, InstitutionPortalActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@InstitutionRegistrationActivity, "Registration Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<TextView>(R.id.btn_cancel_inst).setOnClickListener { finish() }
    }
}
