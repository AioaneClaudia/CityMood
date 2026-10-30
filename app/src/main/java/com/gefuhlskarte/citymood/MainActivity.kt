package com.gefuhlskarte.citymood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gefuhlskarte.citymood.data.CityMoodDatabase
import com.gefuhlskarte.citymood.repository.AuthManager
import com.gefuhlskarte.citymood.repository.MoodRepository
import com.gefuhlskarte.citymood.ui_screens.camera.CameraScreen
import com.gefuhlskarte.citymood.ui_screens.home.HomeScreen
import com.gefuhlskarte.citymood.ui_screens.login.LoginScreen
import com.gefuhlskarte.citymood.viewmodel.MoodViewModel
import com.gefuhlskarte.citymood.viewmodel.MoodViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = CityMoodDatabase.getDatabase(applicationContext)
        val repository = MoodRepository(database.moodDao())
        val factory = MoodViewModelFactory(repository)
        val authManager = AuthManager()

        setContent {
            MaterialTheme {
                var isLoggedIn by remember { mutableStateOf(authManager.isUserLoggedIn()) }
                var showCamera by remember { mutableStateOf(false) }
                var selectedMoodFromCamera by remember { mutableStateOf("Calm") }

                if (!isLoggedIn) {
                    LoginScreen(
                        authManager = authManager,
                        onLoginSuccess = { isLoggedIn = true }
                    )
                } else if (showCamera) {
                    CameraScreen(
                        onMoodSelected = { detectedMood ->
                            selectedMoodFromCamera = detectedMood
                            showCamera = false
                        },
                        onBack = { showCamera = false }
                    )
                } else {
                    val moodViewModel: MoodViewModel = viewModel(factory = factory)
                    HomeScreen(
                        viewModel = moodViewModel,
                        initialMood = selectedMoodFromCamera,
                        onLogout = {
                            authManager.logout()
                            isLoggedIn = false
                        },
                        onOpenFaceDetector = {
                            showCamera = true
                        }
                    )
                }
            }
        }
    }
}