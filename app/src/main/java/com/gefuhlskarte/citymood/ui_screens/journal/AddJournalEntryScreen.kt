package com.gefuhlskarte.citymood.ui_screens.journal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private data class JournalStyle(
    val name: String,
    val pageColor: Color,
    val textBackgroundColor: Color
)

@Composable
fun AddJournalEntryScreen(
    onSave: (
        placeName: String,
        description: String,
        photoUri: String?,
        pageColor: String,
        textBackgroundColor: String,
        fontStyle: String,
        containsFace: Boolean?,
        onResult: (String) -> Unit
    ) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var placeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedStyle by remember { mutableStateOf(0) }
    var selectedFont by remember { mutableStateOf("Modern") }
    var message by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val styles = listOf(
        JournalStyle("Cream", Color(0xFFFFF8E7), Color(0xFFFFE8C2)),
        JournalStyle("Green", Color(0xFFEAF4E5), Color(0xFFD4E8CC)),
        JournalStyle("Blue", Color(0xFFE8F1FA), Color(0xFFD2E5F5)),
        JournalStyle("Pink", Color(0xFFFFEEF2), Color(0xFFFFDCE5)),
        JournalStyle("Purple", Color(0xFFF1EAFE), Color(0xFFE4D8FA))
    )

    val fonts = listOf("Modern", "Elegant", "Typewriter", "Handwritten")

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Photo Picker may already provide a temporary readable grant.
            }
            selectedPhotoUri = uri
            message = ""
        }
    }

    val currentStyle = styles[selectedStyle]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(currentStyle.pageColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create a Journal Page 📖",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF493B32)
        )

        Spacer(modifier = Modifier.height(20.dp))

        androidx.compose.material3.OutlinedTextField(
            value = placeName,
            onValueChange = { placeName = it; message = "" },
            label = { Text("Place name") },
            placeholder = { Text("Where did you go?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it; message = "" },
            label = { Text("Your experience") },
            placeholder = { Text("Write about your experience...") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text("Add a photo 📸", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                photoPicker.launch("image/*")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DAB91))
        ) {
            Text("Upload photo")
        }

        selectedPhotoUri?.let { uri ->
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = uri,
                contentDescription = "Selected journal photo",
                modifier = Modifier.fillMaxWidth().height(220.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Choose your page style 🎨", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        styles.forEachIndexed { index, style ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = style.pageColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (selectedStyle == index) 5.dp else 1.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(style.name, fontWeight = FontWeight.Medium)
                    Button(
                        onClick = { selectedStyle = index },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStyle == index) Color(0xFF657F68)
                            else Color(0xFF9EAAA0)
                        )
                    ) {
                        Text(if (selectedStyle == index) "Selected" else "Choose")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Choose your font ✍️", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        fonts.forEach { font ->
            Button(
                onClick = { selectedFont = font },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedFont == font) Color(0xFF657F68)
                    else Color(0xFF9EAAA0)
                )
            ) {
                Text("$font ${if (selectedFont == font) "✓" else ""}")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = currentStyle.textBackgroundColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (placeName.isBlank()) "Your place" else placeName,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF493B32)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (description.isBlank()) "Your memories will appear here..." else description,
                    color = Color(0xFF493B32),
                    fontFamily = when (selectedFont) {
                        "Elegant" -> FontFamily.Serif
                        "Typewriter" -> FontFamily.Monospace
                        "Handwritten" -> FontFamily.Cursive
                        else -> FontFamily.SansSerif
                    }
                )
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = if (message.contains("salvat")) Color(0xFF397447) else Color(0xFFB3261E)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (placeName.isBlank()) {
                    message = "Please enter a place name."
                    return@Button
                }
                if (description.isBlank()) {
                    message = "Please write about your experience."
                    return@Button
                }

                coroutineScope.launch {
                    isSaving = true
                    message = ""

                    // null means no photo or that the analysis could not be completed.
                    val faceResult = selectedPhotoUri?.let { uri ->
                        FaceDetectionHelper.containsFace(context, uri.toString())
                    }

                    onSave(
                        placeName.trim(),
                        description.trim(),
                        selectedPhotoUri?.toString(),
                        colorToHex(currentStyle.pageColor),
                        colorToHex(currentStyle.textBackgroundColor),
                        selectedFont,
                        faceResult
                    ) { result ->
                        isSaving = false
                        message = result
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF657F68))
        ) {
            Text(if (isSaving) "Analyzing photo and saving..." else "Save journal page")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7777))
        ) {
            Text("Cancel")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun colorToHex(color: Color): String {
    val alpha = (color.alpha * 255).toInt()
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}
