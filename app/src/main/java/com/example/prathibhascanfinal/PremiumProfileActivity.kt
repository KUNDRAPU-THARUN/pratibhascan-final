package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.prathibhascanfinal.databinding.ActivityPremiumProfileBinding
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class PremiumProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityPremiumProfileBinding
    override val viewModel: DashboardViewModel by viewModels()

    private var userEmail: String = ""
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            photoUri?.let { handleImageSelected(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        
        setupToolbar()
        setupListeners()
        loadProfileData()
    }

    private fun setupToolbar() {
        binding.toolbarProfile.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnChangePhoto.setOnClickListener { showPhotoOptions() }
        binding.etDob.setOnClickListener { showDatePicker() }
        binding.btnSavePremiumProfile.setOnClickListener { saveProfile() }
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickImage.launch("image/*")
                    2 -> removePhoto()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 2001)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("IMG_", ".jpg", cacheDir).apply {
                currentPhotoPath = absolutePath
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            photoUri = uri
            takePhoto.launch(uri)
        } catch (e: Exception) {
            Log.e("CAMERA", "Failed to create temp file", e)
        }
    }

    private fun handleImageSelected(uri: Uri) {
        binding.ivPremiumProfilePic.load(uri)
        uploadPhotoToFirebase(uri)
    }

    private fun uploadPhotoToFirebase(uri: Uri) {
        lifecycleScope.launch {
            try {
                val downloadUrl = FirebaseManager.uploadFile(uri, "profile_photos/$userEmail.jpg")
                if (downloadUrl != null) {
                    val db = AppDatabase.getDatabase(this@PremiumProfileActivity)
                    val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                    user?.let {
                        val updated = it.copy(profilePicture = downloadUrl)
                        withContext(Dispatchers.IO) { db.userDao().insertUser(updated) }
                        FirebaseManager.getFirebaseFirestore().collection("users").document(userEmail).update("profilePicture", downloadUrl).await()
                    }
                    Toast.makeText(this@PremiumProfileActivity, "Photo updated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PHOTO_UPLOAD", "Failed", e)
            }
        }
    }

    private fun removePhoto() {
        binding.ivPremiumProfilePic.setImageResource(android.R.drawable.ic_menu_myplaces)
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@PremiumProfileActivity)
                val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                user?.let {
                    val updated = it.copy(profilePicture = null)
                    withContext(Dispatchers.IO) { db.userDao().insertUser(updated) }
                    FirebaseManager.getFirebaseFirestore().collection("users").document(userEmail).update("profilePicture", null).await()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            binding.etDob.setText("$day/${month + 1}/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@PremiumProfileActivity)
            val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
            user?.let { u ->
                binding.etFullName.setText(u.fullName)
                binding.etNickname.setText(u.nickname)
                binding.etDob.setText(u.dob)
                binding.etGender.setText(u.gender)
                binding.etHeight.setText(u.height)
                binding.etWeight.setText(u.weight)
                binding.etWingspan.setText(u.wingSpan)
                binding.etBloodGroup.setText(u.bloodGroup)
                binding.etPrimarySport.setText(u.primaryDiscipline)
                binding.etPosition.setText(u.position)
                binding.etCoachName.setText(u.coachName)
                binding.etEmergencyContact.setText(u.emergencyContact)
                binding.etAddress.setText(u.address)
                
                if (u.profilePicture != null) {
                    binding.ivPremiumProfilePic.load(u.profilePicture)
                }
            }
        }
    }

    private fun saveProfile() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@PremiumProfileActivity)
                val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(userEmail) }
                user?.let { u ->
                    val updatedUser = u.copy(
                        fullName = binding.etFullName.text.toString(),
                        nickname = binding.etNickname.text.toString(),
                        dob = binding.etDob.text.toString(),
                        gender = binding.etGender.text.toString(),
                        height = binding.etHeight.text.toString(),
                        weight = binding.etWeight.text.toString(),
                        wingSpan = binding.etWingspan.text.toString(),
                        bloodGroup = binding.etBloodGroup.text.toString(),
                        primaryDiscipline = binding.etPrimarySport.text.toString(),
                        position = binding.etPosition.text.toString(),
                        coachName = binding.etCoachName.text.toString(),
                        emergencyContact = binding.etEmergencyContact.text.toString(),
                        address = binding.etAddress.text.toString()
                    )
                    withContext(Dispatchers.IO) {
                        db.userDao().insertUser(updatedUser)
                        FirebaseManager.getFirebaseFirestore().collection("users").document(userEmail).set(updatedUser).await()
                    }
                    Toast.makeText(this@PremiumProfileActivity, "Profile Saved Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PremiumProfileActivity, "Error saving profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        }
    }
}

