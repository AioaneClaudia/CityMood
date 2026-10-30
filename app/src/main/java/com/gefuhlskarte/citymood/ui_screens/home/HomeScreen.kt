package com.gefuhlskarte.citymood.ui_screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gefuhlskarte.citymood.viewmodel.MoodViewModel

private val Teal = Color(0xFF237C72)
private val Ink = Color(0xFF243431)
private val CanvasColor = Color(0xFFF6F7F3)
private val Lavender = Color(0xFFEAE5FA)

private data class MoodOption(
    val emoji: String,
    val label: String
)

private val moodOptions = listOf(
    MoodOption("🤩", "Great"),
    MoodOption("😊", "Happy"),
    MoodOption("😌", "Calm"),
    MoodOption("😰", "Stressed"),
    MoodOption("😔", "Sad")
)

@Composable
fun HomeScreen(
    viewModel: MoodViewModel,
    initialMood: String = "Calm",
    onLogout: () -> Unit,
    onOpenFaceDetector: () -> Unit
) {
    val moods by viewModel.moods.collectAsStateWithLifecycle()

    // Firestore: lista online si starea conexiunii
    val cloudMoods by viewModel.cloudMoods.collectAsStateWithLifecycle()
    val cloudStatus by viewModel.cloudStatus.collectAsStateWithLifecycle()

    // Porneste listener-ul Firestore cat timp ecranul este afisat
    DisposableEffect(viewModel) {
        viewModel.startObservingCloudMoods()

        onDispose {
            viewModel.stopObservingCloudMoods()
        }
    }

    var selectedMood by rememberSaveable {
        mutableStateOf(initialMood)
    }

    //actualizează cu o stare nouă din camera foto
    LaunchedEffect(initialMood) {
        if (initialMood.isNotBlank()) {
            selectedMood = initialMood
        }
    }

    var placeName by rememberSaveable {
        mutableStateOf("")
    }

    var valence by rememberSaveable {
        mutableFloatStateOf(0.5f)
    }

    var arousal by rememberSaveable {
        mutableFloatStateOf(0.3f)
    }

    var message by rememberSaveable {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CanvasColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header cu buton de Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📍 CityMood",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal
                    )
                    Text(
                        text = "Your city, your feelings.",
                        fontSize = 15.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Teal
                    )
                }
            }

            // Introduction
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "How are you feeling today?",
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Text(
                    text = "Capture the mood of a place.",
                    fontSize = 15.sp,
                    color = Ink.copy(alpha = 0.7f)
                )
            }

            // Mood form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Choose your mood",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )

                    // buton camera foto
                    OutlinedButton(
                        onClick = onOpenFaceDetector,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "📷 Află starea folosind camera",
                            color = Teal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Emoji selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        moodOptions.forEach { mood ->
                            val selected = selectedMood == mood.label

                            Surface(
                                onClick = {
                                    selectedMood = mood.label
                                    message = ""
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) Lavender else CanvasColor,
                                border = if (selected) {
                                    BorderStroke(2.dp, Teal)
                                } else {
                                    null
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(76.dp)
                                        .padding(
                                            horizontal = 6.dp,
                                            vertical = 12.dp
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = mood.emoji,
                                        fontSize = 28.sp
                                    )

                                    Text(
                                        text = mood.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        color = Ink
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CanvasColor)

                    // Valence
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Valence",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )

                        Text(
                            text = "How pleasant or unpleasant do you feel?",
                            fontSize = 13.sp,
                            color = Ink.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unpleasant", fontSize = 12.sp)

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%+.2f",
                                    valence * 2f - 1f
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Teal
                            )

                            Text("Pleasant", fontSize = 12.sp)
                        }

                        Slider(
                            value = valence,
                            onValueChange = {
                                valence = it
                                message = ""
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Teal,
                                activeTrackColor = Teal
                            )
                        )
                    }

                    // Arousal
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Arousal",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )

                        Text(
                            text = "How calm or activated do you feel?",
                            fontSize = 13.sp,
                            color = Ink.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Calm", fontSize = 12.sp)

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.2f",
                                    arousal
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Teal
                            )

                            Text("Highly activated", fontSize = 12.sp)
                        }

                        Slider(
                            value = arousal,
                            onValueChange = {
                                arousal = it
                                message = ""
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Teal,
                                activeTrackColor = Teal
                            )
                        )
                    }

                    HorizontalDivider(color = CanvasColor)

                    // Place input
                    Text(
                        text = "Where are you?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )

                    OutlinedTextField(
                        value = placeName,
                        onValueChange = {
                            placeName = it
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Enter a place name")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Save to Room and Firestore
                    Button(
                        onClick = {
                            val selectedOption = moodOptions.first {
                                it.label == selectedMood
                            }

                            viewModel.saveMood(
                                placeName = placeName,
                                moodLabel = selectedMood,
                                emoji = selectedOption.emoji,
                                valence = valence * 2f - 1f,
                                arousal = arousal
                            ) { result ->
                                message = result

                                if (result.startsWith("Mood saved locally")) {
                                    placeName = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Teal
                        )
                    ) {
                        Text(
                            text = "✓  Save mood",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = if (
                                message.contains("failed", ignoreCase = true) ||
                                message.contains("could not", ignoreCase = true)
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Teal
                            }
                        )
                    }
                }
            }

            // Recent moods: Room
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recent moods",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                if (moods.isEmpty()) {
                    Text(
                        text = "No moods saved locally yet.",
                        fontSize = 14.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                } else {
                    moods.forEach { mood ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = mood.emoji,
                                    fontSize = 30.sp
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = mood.moodLabel,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    )

                                    Text(
                                        text = mood.placeName,
                                        fontSize = 14.sp,
                                        color = Ink.copy(alpha = 0.7f)
                                    )

                                    Text(
                                        text = "Valence: %.2f · Arousal: %.2f".format(
                                            Locale.US,
                                            mood.valence,
                                            mood.arousal
                                        ),
                                        fontSize = 12.sp,
                                        color = Teal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Online moods: Firestore
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Online moods (Firestore)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Text(
                    text = cloudStatus,
                    fontSize = 13.sp,
                    color = if (cloudStatus.startsWith("Cloud error:")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Ink.copy(alpha = 0.7f)
                    }
                )

                if (cloudMoods.isEmpty()) {
                    Text(
                        text = "No online moods available yet.",
                        fontSize = 14.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                } else {
                    cloudMoods.forEach { mood ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = mood.emoji,
                                    fontSize = 30.sp
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = mood.moodLabel,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    )

                                    Text(
                                        text = mood.placeName,
                                        fontSize = 14.sp,
                                        color = Ink.copy(alpha = 0.7f)
                                    )

                                    Text(
                                        text = "Valence: %.2f · Arousal: %.2f".format(
                                            Locale.US,
                                            mood.valence,
                                            mood.arousal
                                        ),
                                        fontSize = 12.sp,
                                        color = Teal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

