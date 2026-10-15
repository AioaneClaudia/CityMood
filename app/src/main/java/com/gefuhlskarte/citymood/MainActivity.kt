package com.gefuhlskarte.citymood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gefuhlskarte.citymood.data.CityMoodDatabase
import com.gefuhlskarte.citymood.repository.MoodRepository
import com.gefuhlskarte.citymood.ui_screens.home.HomeScreen
import com.gefuhlskarte.citymood.viewmodel.MoodViewModel
import com.gefuhlskarte.citymood.viewmodel.MoodViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = CityMoodDatabase.getDatabase(applicationContext)
        val repository = MoodRepository(database.moodDao())
        val factory = MoodViewModelFactory(repository)

        setContent {
            MaterialTheme {
                val moodViewModel: MoodViewModel = viewModel(
                    factory = factory
                )

                HomeScreen(viewModel = moodViewModel)
            }
        }
    }
}


