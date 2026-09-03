package com.example.prathibhascanfinal

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.base.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileState(
    val academy: Academy? = null,
    val userFallback: User? = null,
    val athleteCount: Int = 0,
    val coachCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaveSuccess: Boolean = false
)

class AcademyProfileViewModel(application: Application) : AndroidViewModel(application), BaseViewModel {

    private val db = AppDatabase.getDatabase(application)
    private val firestoreRepo = FirestoreRepository()
    
    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    fun loadProfile(email: String) {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true) }
            
            // 1. Load User Fallback from Room
            val localUser = db.userDao().getUserByEmail(email)
            _profileState.update { it.copy(userFallback = localUser) }

            // 2. Observe Academy from Firestore
            firestoreRepo.getAcademyFlow(email).collect { academy ->
                _profileState.update { it.copy(academy = academy, isLoading = false) }
                
                academy?.let { aca ->
                    // Sync to local Room
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.academyDao().insertAcademy(aca)
                    }

                    // 3. Start Count Listeners
                    launch {
                        firestoreRepo.getAthletesCountFlow(aca.id).collect { count ->
                            _profileState.update { it.copy(athleteCount = count) }
                        }
                    }
                    launch {
                        firestoreRepo.getCoachesCountFlow(aca.id).collect { count ->
                            _profileState.update { it.copy(coachCount = count) }
                        }
                    }
                }
            }
        }
    }

    fun updateProfile(
        updatedAcademy: Academy,
        newLogoUri: Uri?,
        newPhotoUri: Uri?
    ) {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true) }
            try {
                var academyToSave = updatedAcademy
                
                newLogoUri?.let { uri ->
                    val url = firestoreRepo.uploadImage("academies/${updatedAcademy.id}/logo.jpg", uri)
                    if (url != null) {
                        academyToSave = academyToSave.copy(logoUri = url)
                    }
                }

                newPhotoUri?.let { uri ->
                    val url = firestoreRepo.uploadImage("academies/${updatedAcademy.id}/photo.jpg", uri)
                    if (url != null) {
                        academyToSave = academyToSave.copy(profilePhotoUri = url)
                    }
                }

                val success = firestoreRepo.saveAcademyProfile(academyToSave)
                if (success) {
                    // After saving to Firestore, we should re-fetch or ensure local DB is updated.
                    // If it was a new academy, we might not have the auto-generated ID yet.
                    // But for this app, we mostly query by email.
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.academyDao().insertAcademy(academyToSave)
                    }
                    _profileState.update { it.copy(isLoading = false, isSaveSuccess = true, academy = academyToSave) }
                } else {
                    _profileState.update { it.copy(isLoading = false, error = "Failed to sync changes to cloud. Check your connection.") }
                }
            } catch (e: Exception) {
                android.util.Log.e("EDIT_PROFILE", "Update failed", e)
                _profileState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    fun resetSaveStatus() {
        _profileState.update { it.copy(isSaveSuccess = false, error = null) }
    }
}
