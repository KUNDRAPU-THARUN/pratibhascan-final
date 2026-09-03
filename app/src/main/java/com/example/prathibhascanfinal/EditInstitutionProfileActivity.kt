package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditInstitutionProfileActivity : BaseActivity() {

    override val viewModel: InstitutionProfileViewModel by lazy {
        ViewModelProvider(this)[InstitutionProfileViewModel::class.java]
    }

    private lateinit var currentInstitution: Institution

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_edit_institution_profile)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        val email = SessionManager(this).getEmail() ?: ""
        
        setupHeader()
        observeState()
        
        findViewById<View>(R.id.btn_save_profile).setOnClickListener {
            saveChanges()
        }
        
        viewModel.loadProfile(email)
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "Manage Identity"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Edit Profile Details"
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                state.institution?.let { inst ->
                    currentInstitution = inst
                    findViewById<EditText>(R.id.et_edit_inst_name).setText(inst.institutionName)
                    findViewById<EditText>(R.id.et_edit_inst_type).setText(inst.institutionType)
                    findViewById<EditText>(R.id.et_edit_inst_ownership).setText(inst.ownershipType)
                    findViewById<EditText>(R.id.et_edit_reg_no).setText(inst.registrationNumber)
                    findViewById<EditText>(R.id.et_edit_est_year).setText(inst.establishedYear)
                    findViewById<EditText>(R.id.et_edit_board).setText(inst.boardAffiliation)
                    findViewById<EditText>(R.id.et_edit_principal).setText(inst.principalName)
                    findViewById<EditText>(R.id.et_edit_phone).setText(inst.officialPhone)
                    findViewById<EditText>(R.id.et_edit_website).setText(inst.website)
                    findViewById<EditText>(R.id.et_edit_address).setText(inst.campusAddress)
                }
            }
        }
    }

    private fun saveChanges() {
        if (!::currentInstitution.isInitialized) return
        
        val updatedInst = currentInstitution.copy(
            institutionName = findViewById<EditText>(R.id.et_edit_inst_name).text.toString().trim(),
            institutionType = findViewById<EditText>(R.id.et_edit_inst_type).text.toString().trim(),
            ownershipType = findViewById<EditText>(R.id.et_edit_inst_ownership).text.toString().trim(),
            registrationNumber = findViewById<EditText>(R.id.et_edit_reg_no).text.toString().trim(),
            establishedYear = findViewById<EditText>(R.id.et_edit_est_year).text.toString().trim(),
            boardAffiliation = findViewById<EditText>(R.id.et_edit_board).text.toString().trim(),
            principalName = findViewById<EditText>(R.id.et_edit_principal).text.toString().trim(),
            officialPhone = findViewById<EditText>(R.id.et_edit_phone).text.toString().trim(),
            website = findViewById<EditText>(R.id.et_edit_website).text.toString().trim(),
            campusAddress = findViewById<EditText>(R.id.et_edit_address).text.toString().trim()
        )
        
        viewModel.updateProfile(updatedInst)
        Toast.makeText(this, "Identity Updated Successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
}
