package com.gefuhlskarte.citymood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

import com.gefuhlskarte.citymood.data.CityMoodDatabase
import com.gefuhlskarte.citymood.repository.AuthManager
import com.gefuhlskarte.citymood.repository.MoodRepository
import com.gefuhlskarte.citymood.repository.TravelJournalRepository

import com.gefuhlskarte.citymood.ui_screens.camera.CameraScreen
import com.gefuhlskarte.citymood.ui_screens.home.HomeScreen
import com.gefuhlskarte.citymood.ui_screens.journal.TravelJournalScreen
import com.gefuhlskarte.citymood.ui_screens.login.LoginScreen
import com.gefuhlskarte.citymood.ui_screens.map.MapScreen

import com.gefuhlskarte.citymood.viewmodel.MoodViewModel
import com.gefuhlskarte.citymood.viewmodel.MoodViewModelFactory
import com.gefuhlskarte.citymood.viewmodel.TravelJournalViewModel
import com.gefuhlskarte.citymood.viewmodel.TravelJournalViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = CityMoodDatabase.getDatabase(applicationContext)

        val moodRepository = MoodRepository(database.moodDao())
        val moodFactory = MoodViewModelFactory(moodRepository)

        val journalRepository =
            TravelJournalRepository(database.travelJournalDao())
        val journalFactory =
            TravelJournalViewModelFactory(journalRepository)

        val authManager = AuthManager()

        setContent {
            MaterialTheme {

                var isLoggedIn by remember {
                    mutableStateOf(authManager.isUserLoggedIn())
                }

                var showCamera by remember {
                    mutableStateOf(false)
                }

                var selectedMoodFromCamera by remember {
                    mutableStateOf("Calm")
                }

                var selectedScreen by remember {
                    mutableStateOf("home")
                }

                if (!isLoggedIn) {

                    LoginScreen(
                        authManager = authManager,
                        onLoginSuccess = {
                            isLoggedIn = true
                            selectedScreen = "home"
                        }
                    )

                } else if (showCamera) {

                    CameraScreen(
                        onMoodSelected = { detectedMood ->
                            selectedMoodFromCamera = detectedMood
                            showCamera = false
                        },
                        onBack = {
                            showCamera = false
                        }
                    )

                } else {

                    val moodViewModel: MoodViewModel =
                        viewModel(factory = moodFactory)

                    val journalViewModel: TravelJournalViewModel =
                        viewModel(factory = journalFactory)

                    when (selectedScreen) {

                        "journal" -> {
                            TravelJournalScreen(
                                viewModel = journalViewModel,
                                onBack = {
                                    selectedScreen = "home"
                                }
                            )
                        }

                        "map" -> {
                            MapScreen(
                                initialMoodFilter = selectedMoodFromCamera,
                                onBack = {
                                    selectedScreen = "home"
                                }
                            )
                        }

                        else -> {
                            HomeScreen(
                                viewModel = moodViewModel,
                                initialMood = selectedMoodFromCamera,
                                onLogout = {
                                    authManager.logout()
                                    isLoggedIn = false
                                    selectedScreen = "home"
                                },
                                onOpenFaceDetector = {
                                    showCamera = true
                                },
                                onOpenTravelJournal = {
                                    selectedScreen = "journal"
                                },
                                onOpenMap = {
                                    selectedScreen = "map"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}