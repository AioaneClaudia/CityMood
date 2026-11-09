
package com.gefuhlskarte.citymood.ui_screens.journal

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gefuhlskarte.citymood.data.TravelJournalEntry
import com.gefuhlskarte.citymood.viewmodel.TravelJournalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VintageBrown = Color(0xFF594333)
private val VintageDark = Color(0xFF38291F)
private val VintageCream = Color(0xFFFFF4DD)
private val VintagePaper = Color(0xFFFFF8E9)
private val VintageGold = Color(0xFFC49A55)
private val VintageGreen = Color(0xFF78866B)

@Composable
fun TravelJournalScreen(
    viewModel: TravelJournalViewModel,
    onBack: () -> Unit = {}
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    var showAddScreen by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(-1) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember {
        mutableStateOf<TravelJournalEntry?>(null)
    }
    var statusMessage by remember { mutableStateOf("") }

    if (showAddScreen) {
        AddJournalEntryScreen(
            onSave = {
                    placeName,
                    description,
                    photoUri,
                    pageColor,
                    textBackgroundColor,
                    fontStyle,
                    onResult ->

                viewModel.saveEntry(
                    placeName,
                    description,
                    photoUri,
                    pageColor,
                    textBackgroundColor,
                    fontStyle
                ) { result ->
                    statusMessage = result

                    if (result == "Experiența a fost salvată!") {
                        showAddScreen = false
                        currentPage = 0
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
                .background(Color(0xFFE8D8BB))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // HEADER
            Text(
                text = "MY TRAVEL DIARY",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = VintageDark,
                letterSpacing = 2.sp
            )

            Text(
                text = "A collection of beautiful memories",
                fontSize = 14.sp,
                color = VintageBrown,
                fontFamily = FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(20.dp))

            // BACK BUTTON
            TextButton(onClick = onBack) {
                Text(
                    text = "← Back to CityMood",
                    color = VintageBrown
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentPage == -1 || entries.isEmpty()) {

                // VINTAGE BOOK COVER
                VintageBookCover(
                    memoriesCount = entries.size,
                    onOpen = {
                        if (entries.isNotEmpty()) {
                            currentPage = 0
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        statusMessage = ""
                        showAddScreen = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VintageBrown
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "＋ Add a New Memory",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                if (entries.isEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Your travel story starts here...",
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        color = VintageBrown,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Add your first memory and fill this book with adventures.",
                        fontSize = 14.sp,
                        color = VintageBrown,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

            } else {

                // OPEN BOOK PAGE
                val entry = entries[currentPage.coerceIn(0, entries.lastIndex)]

                VintageMemoryPage(
                    entry = entry,
                    pageNumber = currentPage + 1,
                    onDelete = {
                        entryToDelete = entry
                        showDeleteDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PAGE NAVIGATION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (currentPage > 0) {
                                currentPage--
                            } else {
                                currentPage = -1
                            }
                        },
                        enabled = currentPage >= 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageBrown,
                            disabledContainerColor = VintageBrown.copy(alpha = 0.35f)
                        )
                    ) {
                        Text("← Previous")
                    }

                    Text(
                        text = "${currentPage + 1} / ${entries.size}",
                        fontWeight = FontWeight.Bold,
                        color = VintageDark
                    )

                    Button(
                        onClick = {
                            if (currentPage < entries.lastIndex) {
                                currentPage++
                            }
                        },
                        enabled = currentPage < entries.lastIndex,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageBrown,
                            disabledContainerColor = VintageBrown.copy(alpha = 0.35f)
                        )
                    ) {
                        Text("Next →")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        currentPage = -1
                    }
                ) {
                    Text(
                        text = "Return to book cover",
                        color = VintageBrown
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

            if (statusMessage.isNotBlank() && !showAddScreen) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    color = VintageBrown
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DELETE CONFIRMATION
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
                        currentPage = -1
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
                text = "✦  ✧  ✦",
                color = VintageGold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "MY",
                color = VintageCream,
                fontSize = 23.sp,
                letterSpacing = 5.sp
            )

            Text(
                text = "TRAVEL",
                color = VintageCream,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Text(
                text = "JOURNAL",
                color = VintageGold,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Memories from places\nthat touched my heart",
                color = VintageCream,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "❧  ❦  ❧",
                color = VintageGold,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$memoriesCount beautiful memories",
                color = VintageCream,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpen,
                enabled = memoriesCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VintageGold,
                    contentColor = VintageDark,
                    disabledContainerColor = VintageGold.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "OPEN MY JOURNAL  →",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "EST. WITH LOVE",
                color = VintageCream.copy(alpha = 0.7f),
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )
        }
    }
}

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
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .animateContentSize(),
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

            // PAGE DECORATION
            Text(
                text = "✦  TRAVEL MEMORIES  ✦",
                modifier = Modifier.fillMaxWidth(),
                color = VintageGold,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = entry.placeName,
                modifier = Modifier.fillMaxWidth(),
                color = VintageDark,
                fontSize = 27.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formattedDate,
                modifier = Modifier.fillMaxWidth(),
                color = VintageBrown,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // PHOTO
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
                                .height(230.dp)
                        )

                        Text(
                            text = "A little piece of my journey ♡",
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

            // DESCRIPTION ON PAPER
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
                        text = "Dear Diary,",
                        fontSize = 19.sp,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        color = VintageBrown
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = entry.description,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        fontFamily = font,
                        color = VintageDark
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "With love, ♡",
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
                text = "❧  ❦  ❧",
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
                    text = "PAGE $pageNumber",
                    color = VintageBrown,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete page",
                        color = Color(0xFF9E4940),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

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
