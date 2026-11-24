
package com.gefuhlskarte.citymood.ui_screens.journal

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gefuhlskarte.citymood.data.TravelJournalEntry
import com.gefuhlskarte.citymood.viewmodel.TravelJournalViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VintageBrown = Color(0xFF594333)
private val VintageDark = Color(0xFF38291F)
private val VintageCream = Color(0xFFFFF4DD)
private val VintagePaper = Color(0xFFFFF8E9)
private val VintageGold = Color(0xFFC49A55)
private val VintageGreen = Color(0xFF78866B)
private val VintageBackground = Color(0xFFE8D8BB)

private enum class JournalSection {
    COVER,
    DASHBOARD,
    MEMORY
}

@Composable
fun TravelJournalScreen(
    viewModel: TravelJournalViewModel,
    onBack: () -> Unit = {}
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    var section by remember {
        mutableStateOf(JournalSection.COVER)
    }

    var showAddScreen by remember { mutableStateOf(false) }

    var selectedEntryId by remember {
        mutableStateOf<Long?>(null)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var entryToDelete by remember {
        mutableStateOf<TravelJournalEntry?>(null)
    }

    var statusMessage by remember { mutableStateOf("") }

    // Refresh dashboard photos every 5 minutes.
    var photoRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(section) {
        if (section == JournalSection.DASHBOARD) {
            while (true) {
                delay(5 * 60 * 1000L)
                photoRefreshKey++
            }
        }
    }

    // Select up to two photos with detected faces and two without faces.
    // If either group is too small, fill the remaining slots with other journal photos.
    // Select photos for diagonal face / no-face arrangement.
    val dashboardPhotos = remember(entries, photoRefreshKey) {

        val withFaces = entries
            .filter {
                !it.photoUri.isNullOrBlank() &&
                        it.containsFace == true
            }
            .shuffled()
            .take(2)

        val withoutFaces = entries
            .filter {
                !it.photoUri.isNullOrBlank() &&
                        it.containsFace == false
            }
            .shuffled()
            .take(2)

        // Positions:
        // 0 = top left: face
        // 1 = top right: no face
        // 2 = bottom left: no face
        // 3 = bottom right: face

        val selected = arrayOfNulls<TravelJournalEntry>(4)

        selected[0] = withFaces.getOrNull(0)
        selected[3] = withFaces.getOrNull(1)

        selected[1] = withoutFaces.getOrNull(0)
        selected[2] = withoutFaces.getOrNull(1)

        // Fill missing positions with other journal photos.
        val usedIds = selected
            .filterNotNull()
            .map { it.id }
            .toSet()

        val remaining = entries
            .filter {
                !it.photoUri.isNullOrBlank() &&
                        it.id !in usedIds
            }
            .shuffled()

        var remainingIndex = 0

        for (i in selected.indices) {
            if (selected[i] == null &&
                remainingIndex < remaining.size
            ) {
                selected[i] = remaining[remainingIndex]
                remainingIndex++
            }
        }

        selected.toList()
    }

    val visitedPlaces = remember(entries) {
        entries.distinctBy {
            it.placeName.trim().lowercase()
        }
    }

    val selectedEntry = entries.find {
        it.id == selectedEntryId
    }

    if (showAddScreen) {

        AddJournalEntryScreen(
            onSave = {
                    placeName,
                    description,
                    photoUri,
                    pageColor,
                    textBackgroundColor,
                    fontStyle,
                    containsFace,
                    onResult ->

                viewModel.saveEntry(
                    placeName,
                    description,
                    photoUri,
                    pageColor,
                    textBackgroundColor,
                    fontStyle,
                    containsFace
                ) { result ->

                    statusMessage = result

                    if (result == "Experiența a fost salvată!") {
                        showAddScreen = false
                        section = JournalSection.DASHBOARD
                    }
                }
            },
            onCancel = {
                showAddScreen = false
            }
        )

    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VintageBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (section) {

                // --------------------------------
                // COVER
                // --------------------------------

                JournalSection.COVER -> {

                    Text(
                        text = "✦  MY TRAVEL DIARY  ✦",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = VintageDark,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "A collection of beautiful memories",
                        fontSize = 14.sp,
                        color = VintageBrown,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(onClick = onBack) {
                        Text(
                            "← Back to CityMood",
                            color = VintageBrown
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    VintageBookCover(
                        memoriesCount = entries.size,
                        onOpen = {
                            section = JournalSection.DASHBOARD
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            statusMessage = ""
                            showAddScreen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageBrown
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "＋ Add a New Memory",
                            modifier = Modifier.padding(6.dp),
                            color = Color.White
                        )
                    }
                }

                // --------------------------------
                // DASHBOARD
                // --------------------------------

                JournalSection.DASHBOARD -> {

                    DashboardHeader(
                        onBackToCover = {
                            section = JournalSection.COVER
                        },
                        onBack = onBack
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    DashboardSectionTitle(
                        title = "Little Moments, Big Memories",
                        subtitle = "A few snapshots from your journey ♡"
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    DashboardPhotoCollage(
                        photos = dashboardPhotos,
                        onPhotoClick = { entry ->
                            selectedEntryId = entry.id
                            section = JournalSection.MEMORY
                        }
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    Text(
                        text = "✦  Every picture tells a story  ✦",
                        color = VintageBrown,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Cursive,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    Button(
                        onClick = {
                            if (entries.isNotEmpty()) {
                                selectedEntryId = entries.first().id
                                section = JournalSection.MEMORY
                            }
                        },
                        enabled = entries.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageBrown,
                            disabledContainerColor = VintageBrown.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("OPEN MY JOURNAL PAGES  →")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            statusMessage = ""
                            showAddScreen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageGreen
                        )
                    ) {
                        Text("＋ Add a New Memory")
                    }

                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage,
                            color = VintageBrown
                        )
                    }
                }

                // --------------------------------
                // INDIVIDUAL MEMORY PAGE
                // --------------------------------

                JournalSection.MEMORY -> {

                    if (selectedEntry == null) {

                        Text(
                            "This memory is no longer available.",
                            color = VintageBrown
                        )

                        Button(
                            onClick = {
                                section = JournalSection.DASHBOARD
                            }
                        ) {
                            Text("Back to Dashboard")
                        }

                    } else {

                        VintageMemoryPage(
                            entry = selectedEntry,
                            pageNumber = entries.indexOf(selectedEntry) + 1,
                            onDelete = {
                                entryToDelete = selectedEntry
                                showDeleteDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val currentIndex = entries.indexOf(selectedEntry)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Button(
                                onClick = {
                                    if (currentIndex > 0) {
                                        selectedEntryId =
                                            entries[currentIndex - 1].id
                                    }
                                },
                                enabled = currentIndex > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VintageBrown
                                )
                            ) {
                                Text("← Previous")
                            }

                            Text(
                                "${currentIndex + 1} / ${entries.size}",
                                color = VintageDark,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = {
                                    if (currentIndex < entries.lastIndex) {
                                        selectedEntryId =
                                            entries[currentIndex + 1].id
                                    }
                                },
                                enabled = currentIndex < entries.lastIndex,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VintageBrown
                                )
                            ) {
                                Text("Next →")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                section = JournalSection.DASHBOARD
                            }
                        ) {
                            Text(
                                "← Back to Travel Dashboard",
                                color = VintageBrown
                            )
                        }

                        Button(
                            onClick = {
                                statusMessage = ""
                                showAddScreen = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VintageGreen
                            )
                        ) {
                            Text("＋ Add another memory")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --------------------------------
    // DELETE CONFIRMATION
    // --------------------------------

    if (showDeleteDialog && entryToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                entryToDelete = null
            },
            title = {
                Text("Delete this memory?")
            },
            text = {
                Text(
                    "Are you sure you want to remove this page from your travel journal?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let { entry ->
                            viewModel.deleteEntry(entry)
                            statusMessage = "Memory deleted."
                        }

                        showDeleteDialog = false
                        entryToDelete = null
                        section = JournalSection.DASHBOARD
                    }
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFB3261E)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        entryToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = VintageCream
        )
    }
}

// ====================================
// DASHBOARD HEADER
// ====================================

@Composable
private fun DashboardHeader(
    onBackToCover: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF72513B),
                RoundedCornerShape(14.dp)
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TextButton(onClick = onBack) {
            Text(
                "← CityMood",
                color = VintageCream
            )
        }

        Text(
            "✦  MY TRAVEL JOURNAL  ✦",
            color = VintageGold,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "My Memories",
            color = VintageCream,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Text(
            "Every journey leaves a beautiful memory.",
            color = VintageCream,
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif
        )

        TextButton(onClick = onBackToCover) {
            Text(
                "Return to book cover ↑",
                color = VintageGold
            )
        }
    }
}

// ====================================
// SECTION TITLE
// ====================================

@Composable
private fun DashboardSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            title,
            fontSize = 21.sp,
            color = VintageDark,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Text(
            subtitle,
            fontSize = 12.sp,
            color = VintageBrown,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VintageGold)
        )
    }
}

// ====================================
// DASHBOARD PHOTO COLLAGE
// ====================================

@Composable
private fun DashboardPhotoCollage(
    photos: List<TravelJournalEntry?>,
    onPhotoClick: (TravelJournalEntry) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PolaroidPhotoCard(
                imageModel = photos.getOrNull(0)?.photoUri,
                caption = photos.getOrNull(0)?.placeName ?: "Add a memory ♡",
                rotation = -5f,
                modifier = Modifier.weight(1f),
                onClick = { photos.getOrNull(0)?.let(onPhotoClick) }
            )
            PolaroidPhotoCard(
                imageModel = photos.getOrNull(1)?.photoUri,
                caption = photos.getOrNull(1)?.placeName ?: "Add a memory ♡",
                rotation = 4f,
                modifier = Modifier.weight(1f),
                onClick = { photos.getOrNull(1)?.let(onPhotoClick) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PolaroidPhotoCard(
                imageModel = photos.getOrNull(2)?.photoUri,
                caption = photos.getOrNull(2)?.placeName ?: "Add a memory ♡",
                rotation = -3f,
                modifier = Modifier.weight(1f),
                onClick = { photos.getOrNull(2)?.let(onPhotoClick) }
            )
            PolaroidPhotoCard(
                imageModel = photos.getOrNull(3)?.photoUri,
                caption = photos.getOrNull(3)?.placeName ?: "Add a memory ♡",
                rotation = 5f,
                modifier = Modifier.weight(1f),
                onClick = { photos.getOrNull(3)?.let(onPhotoClick) }
            )
        }
    }
}

// ====================================
// POLAROID PHOTO CARD
// ====================================

@Composable
private fun PolaroidPhotoCard(
    imageModel: Any?,
    caption: String,
    rotation: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .rotate(rotation)
            .shadow(7.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (imageModel != null) {

                AsyncImage(
                    model = imageModel,
                    contentDescription = caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp),
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .background(Color(0xFFE8DDC9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♡",
                        fontSize = 36.sp,
                        color = VintageGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = caption,
                color = VintageBrown,
                fontSize = 12.sp,
                fontFamily = FontFamily.Cursive,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

// ====================================
// VINTAGE BOOK COVER
// ====================================

@Composable
private fun VintageBookCover(
    memoriesCount: Int,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF72513B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(
                    Color(0xFF8A684D),
                    RoundedCornerShape(12.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "✦  ✧  ✦",
                color = VintageGold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                "MY",
                color = VintageCream,
                fontSize = 23.sp,
                letterSpacing = 5.sp
            )

            Text(
                "TRAVEL",
                color = VintageCream,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Text(
                "JOURNAL",
                color = VintageGold,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Memories from places\nthat touched my heart",
                color = VintageCream,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                "❧  ❦  ❧",
                color = VintageGold,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "$memoriesCount beautiful memories",
                color = VintageCream,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VintageGold,
                    contentColor = VintageDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "OPEN MY JOURNAL  →",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "EST. WITH LOVE",
                color = VintageCream.copy(alpha = 0.7f),
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )
        }
    }
}

// ====================================
// INDIVIDUAL MEMORY PAGE
// ====================================

@Composable
private fun VintageMemoryPage(
    entry: TravelJournalEntry,
    pageNumber: Int,
    onDelete: () -> Unit
) {
    val pageColor = remember(entry.pageColor) {
        parseJournalColor(entry.pageColor, VintagePaper)
    }

    val textBackgroundColor = remember(entry.textBackgroundColor) {
        parseJournalColor(entry.textBackgroundColor, VintageCream)
    }

    val font = when (entry.fontStyle) {
        "Elegant" -> FontFamily.Serif
        "Typewriter" -> FontFamily.Monospace
        "Handwritten" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    val formattedDate = remember(entry.createdAt) {
        SimpleDateFormat(
            "dd MMMM yyyy",
            Locale.getDefault()
        ).format(Date(entry.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = pageColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Text(
                "✦  TRAVEL MEMORIES  ✦",
                modifier = Modifier.fillMaxWidth(),
                color = VintageGold,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                entry.placeName,
                modifier = Modifier.fillMaxWidth(),
                color = VintageDark,
                fontSize = 27.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                formattedDate,
                modifier = Modifier.fillMaxWidth(),
                color = VintageBrown,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (!entry.photoUri.isNullOrBlank()) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .shadow(5.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        AsyncImage(
                            model = entry.photoUri,
                            contentDescription = "Travel memory from ${entry.placeName}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            "A little piece of my journey ♡",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 9.dp, bottom = 4.dp),
                            fontFamily = FontFamily.Cursive,
                            fontSize = 16.sp,
                            color = VintageBrown,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = textBackgroundColor
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Dear Diary,",
                        fontSize = 19.sp,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        color = VintageBrown
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        entry.description,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        fontFamily = font,
                        color = VintageDark
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        "With love, ♡",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Cursive,
                        color = VintageBrown
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "❧  ❦  ❧",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = VintageGold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PAGE $pageNumber",
                    color = VintageBrown,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                TextButton(onClick = onDelete) {
                    Text(
                        "Delete page",
                        color = Color(0xFF9E4940),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ====================================
// COLOR PARSER
// ====================================

private fun parseJournalColor(
    hex: String,
    fallback: Color
): Color {
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}
