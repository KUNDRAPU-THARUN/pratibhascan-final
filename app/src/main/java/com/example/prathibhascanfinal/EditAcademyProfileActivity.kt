package com.example.prathibhascanfinal

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditAcademyProfileActivity : BaseActivity() {

    override val viewModel: AcademyProfileViewModel by lazy {
        ViewModelProvider(this)[AcademyProfileViewModel::class.java]
    }

    private lateinit var userEmail: String
    private lateinit var spType: Spinner
    private var currentAcademy: Academy? = null
    
    private var newLogoUri: Uri? = null
    private var newPhotoUri: Uri? = null

    private val pickLogo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            newLogoUri = uri
            findViewById<ImageView>(R.id.iv_edit_logo).setImageURI(uri)
        }
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            newPhotoUri = uri
            findViewById<ImageView>(R.id.iv_edit_photo).setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_edit_academy_profile)
        setupEdgeToEdge(findViewById(R.id.edit_academy_profile_root))

        userEmail = SessionManager(this).getEmail() ?: ""

        setupTypeSpinner()
        setupHeader()
        setupImagePickers()
        observeViewModel()

        viewModel.loadProfile(userEmail)

        findViewById<View>(R.id.btn_save_profile).setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupImagePickers() {
        findViewById<View>(R.id.btn_change_logo).setOnClickListener {
            pickLogo.launch("image/*")
        }
        findViewById<View>(R.id.btn_change_photo).setOnClickListener {
            pickPhoto.launch("image/*")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.profileState.collectLatest { state ->
                if (state.academy != null && currentAcademy == null) {
                    currentAcademy = state.academy
                    populateFields(state.academy)
                } else if (state.userFallback != null && currentAcademy == null) {
                    preFillFromUser(state.userFallback)
                }

                if (state.isSaveSuccess) {
                    Toast.makeText(this@EditAcademyProfileActivity, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveStatus()
                    finish()
                }

                if (state.error != null) {
                    Toast.makeText(this@EditAcademyProfileActivity, state.error, Toast.LENGTH_LONG).show()
                    viewModel.resetSaveStatus()
                }

                val btnSave = findViewById<View>(R.id.btn_save_profile)
                btnSave.isEnabled = !state.isLoading
                if (btnSave is Button) {
                    btnSave.text = if (state.isLoading) "SAVING..." else "SAVE CHANGES"
                }
            }
        }
    }

    private fun preFillFromUser(user: User) {
        findViewById<TextInputEditText>(R.id.et_edit_aca_name).setText(user.fullName)
        findViewById<TextInputEditText>(R.id.et_edit_aca_email).setText(user.email)
        findViewById<TextInputEditText>(R.id.et_edit_aca_city).setText(user.location)
    }

    private fun populateFields(aca: Academy) {
        findViewById<TextInputEditText>(R.id.et_edit_aca_name).setText(aca.academyName)
        findViewById<TextInputEditText>(R.id.et_edit_aca_director).setText(aca.directorName)
        findViewById<TextInputEditText>(R.id.et_edit_aca_reg_no).setText(aca.registrationNumber)
        
        val year = aca.establishmentYear.takeIf { it > 0 }?.toString() ?: ""
        findViewById<TextInputEditText>(R.id.et_edit_aca_year).setText(year)
        
        findViewById<TextInputEditText>(R.id.et_edit_aca_phone).setText(aca.phoneNumber)
        findViewById<TextInputEditText>(R.id.et_edit_aca_email).setText(aca.contactEmail)
        findViewById<TextInputEditText>(R.id.et_edit_aca_address).setText(aca.address)
        findViewById<TextInputEditText>(R.id.et_edit_aca_city).setText(aca.city)
        findViewById<TextInputEditText>(R.id.et_edit_aca_district).setText(aca.district)
        findViewById<TextInputEditText>(R.id.et_edit_aca_state).setText(aca.state)
        findViewById<TextInputEditText>(R.id.et_edit_aca_pin).setText(aca.pinCode)
        findViewById<TextInputEditText>(R.id.et_edit_aca_sports).setText(aca.specializedDomains)
        findViewById<TextInputEditText>(R.id.et_edit_aca_website).setText(aca.websiteUrl.ifEmpty { aca.website })
        findViewById<TextInputEditText>(R.id.et_edit_aca_bio).setText(aca.description)
        
        val typePos = if (aca.academyType == "Government") 1 else 0
        spType.setSelection(typePos)

        if (!aca.logoUri.isNullOrEmpty()) {
            findViewById<ImageView>(R.id.iv_edit_logo).load(aca.logoUri) {
                crossfade(true)
                placeholder(R.drawable.ic_academy_emblem)
                transformations(CircleCropTransformation())
            }
        }
    }

    private fun validateAndSave() {
        val name = findViewById<TextInputEditText>(R.id.et_edit_aca_name).text.toString().trim()
        val phone = findViewById<TextInputEditText>(R.id.et_edit_aca_phone).text.toString().trim()
        val email = findViewById<TextInputEditText>(R.id.et_edit_aca_email).text.toString().trim()
        val website = findViewById<TextInputEditText>(R.id.et_edit_aca_website).text.toString().trim()

        if (name.isEmpty()) {
            showError("Academy Name is required")
            return
        }
        if (!Patterns.PHONE.matcher(phone).matches()) {
            showError("Invalid Phone Number")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid Email Address")
            return
        }
        if (website.isNotEmpty() && !Patterns.WEB_URL.matcher(website).matches()) {
            showError("Invalid Website URL")
            return
        }

        val aca = currentAcademy ?: Academy(contactEmail = email)
        val updated = aca.copy(
            academyName = name,
            directorName = findViewById<TextInputEditText>(R.id.et_edit_aca_director).text.toString(),
            academyType = spType.selectedItem.toString(),
            registrationNumber = findViewById<TextInputEditText>(R.id.et_edit_aca_reg_no).text.toString(),
            establishmentYear = findViewById<TextInputEditText>(R.id.et_edit_aca_year).text.toString().toIntOrNull() ?: 2024,
            phoneNumber = phone,
            contactEmail = email,
            address = findViewById<TextInputEditText>(R.id.et_edit_aca_address).text.toString(),
            city = findViewById<TextInputEditText>(R.id.et_edit_aca_city).text.toString(),
            district = findViewById<TextInputEditText>(R.id.et_edit_aca_district).text.toString(),
            state = findViewById<TextInputEditText>(R.id.et_edit_aca_state).text.toString(),
            pinCode = findViewById<TextInputEditText>(R.id.et_edit_aca_pin).text.toString(),
            specializedDomains = findViewById<TextInputEditText>(R.id.et_edit_aca_sports).text.toString(),
            websiteUrl = website,
            website = website,
            description = findViewById<TextInputEditText>(R.id.et_edit_aca_bio).text.toString(),
            lastUpdated = System.currentTimeMillis()
        )
        viewModel.updateProfile(updated, newLogoUri, newPhotoUri)
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun setupTypeSpinner() {
        spType = findViewById(R.id.sp_edit_aca_type)
        val types = arrayOf("Private", "Government")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spType.adapter = adapter
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "Edit Profile"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Update organization info"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
    }
}
