package com.gefuhlskarte.citymood.ui_screens.map

import android.preference.PreferenceManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val Teal = Color(0xFF237C72)
private val Ink = Color(0xFF243431)

data class ClujLocation(
    val id: String,
    val name: String,
    val category: String,
    val dominantMood: String,
    val moodEmoji: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)

// Locații inițiale cu mood definit
val initialClujLocations = listOf(
    ClujLocation("1", "Meron Piața Unirii", "Cafenea", "Happy", "😊", 46.7694, 23.5898, "Piața Unirii 10"),
    ClujLocation("2", "Olivo Coffee Culture", "Cafenea", "Calm", "😌", 46.7702, 23.5925, "Bulevardul Eroilor 7"),
    ClujLocation("3", "Form Cafe", "Bar", "Great", "🤩", 46.7690, 23.5885, "Piața Unirii 25"),
    ClujLocation("4", "Booha Bar", "Bar", "Great", "🤩", 46.7668, 23.5795, "Str. Piezișă 19"),
    ClujLocation("5", "Flying Circus", "Bar", "Great", "🤩", 46.7685, 23.5890, "Str. Universității 3"),
    ClujLocation("6", "Samsara Foodhouse", "Restaurant", "Calm", "😌", 46.7652, 23.5742, "Str. Cardinal Iuliu Hossu 3"),
    ClujLocation("7", "Chios Social Lounge", "Restaurant", "Happy", "😊", 46.7689, 23.5772, "Parcul Central"),
    ClujLocation("8", "Cofetăria Friandise", "Cofetărie", "Sad", "😔", 46.7665, 23.5868, "Str. Ion Rațiu 2"),
    ClujLocation("9", "Pralina Cofetărie", "Cofetărie", "Happy", "😊", 46.7720, 23.5950, "Bulevardul 21 Decembrie 1989"),
    ClujLocation("10", "Parcul Central Simion Bărnuțiu", "Parc", "Stressed", "😰", 46.7691, 23.5768, "Parcul Central, Cluj-Napoca"),
    ClujLocation("11", "Grădina Botanică", "Parc", "Calm", "😌", 46.7618, 23.5882, "Str. Republicii 42"),
    ClujLocation("12", "Cetățuia Park", "Parc", "Sad", "😔", 46.7755, 23.5815, "Dealul Cetățuia")
)

// Funcție care caută LIVE locații reale în Cluj-Napoca prin OpenStreetMap
suspend fun searchRealPlacesInCluj(
    query: String,
    defaultCategory: String,
    defaultMood: String
): List<ClujLocation> = withContext(Dispatchers.IO) {
    try {
        val encodedQuery = URLEncoder.encode("$query Cluj-Napoca", "UTF-8")
        // Bounding box strict pe zona Cluj-Napoca
        val urlStr = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=20&bounded=1&viewbox=23.48,46.83,23.72,46.70"
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "CityMoodAndroidApp/1.0")
            connectTimeout = 8000
            readTimeout = 8000
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(response)
        val results = mutableListOf<ClujLocation>()

        val moodToUse = if (defaultMood == "Toate") "Happy" else defaultMood
        val emojiMap = mapOf("Great" to "🤩", "Happy" to "😊", "Calm" to "😌", "Stressed" to "😰", "Sad" to "😔")

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val displayName = obj.optString("display_name", "")
            val parts = displayName.split(",")
            val placeName = parts.firstOrNull()?.trim() ?: query
            val address = parts.drop(1).take(2).joinToString(",").trim()
            val lat = obj.optDouble("lat", 46.7694)
            val lon = obj.optDouble("lon", 23.5898)
            val osmType = obj.optString("type", "")

            val cat = when {
                defaultCategory != "Toate" -> defaultCategory
                osmType.contains("cafe") -> "Cafenea"
                osmType.contains("bar") || osmType.contains("pub") -> "Bar"
                osmType.contains("restaurant") || osmType.contains("fast_food") -> "Restaurant"
                osmType.contains("confectionery") || osmType.contains("bakery") -> "Cofetărie"
                osmType.contains("park") || osmType.contains("garden") -> "Parc"
                else -> "Locație Cluj"
            }

            results.add(
                ClujLocation(
                    id = obj.optString("place_id", i.toString()),
                    name = placeName,
                    category = cat,
                    dominantMood = moodToUse,
                    moodEmoji = emojiMap[moodToUse] ?: "📍",
                    latitude = lat,
                    longitude = lon,
                    address = if (address.isNotEmpty()) address else "Cluj-Napoca"
                )
            )
        }
        results
    } catch (e: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    initialMoodFilter: String = "Toate",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    var showSearchBar by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Toate") }
    var selectedMood by remember { mutableStateOf("Toate") }

    var onlineLocations by remember { mutableStateOf<List<ClujLocation>>(emptyList()) }
    var selectedPlace by remember { mutableStateOf<ClujLocation?>(null) }

    val categories = listOf("Toate", "Cafenea", "Cofetărie", "Restaurant", "Bar", "Parc")
    val moods = listOf(
        "Toate" to "🌍",
        "Great" to "🤩",
        "Happy" to "😊",
        "Calm" to "😌",
        "Stressed" to "😰",
        "Sad" to "😔"
    )

    // Funcție care declanșează căutarea reală pe harta Clujului
    fun performLiveSearch(customQuery: String) {
        if (customQuery.isBlank()) {
            onlineLocations = emptyList()
            return
        }
        coroutineScope.launch {
            isSearchingOnline = true
            val found = searchRealPlacesInCluj(
                query = customQuery,
                defaultCategory = selectedCategory,
                defaultMood = selectedMood
            )
            onlineLocations = found
            isSearchingOnline = false

            // Centrează harta pe primul rezultat găsit
            found.firstOrNull()?.let { first ->
                mapViewRef?.controller?.animateTo(GeoPoint(first.latitude, first.longitude))
                mapViewRef?.controller?.setZoom(16.5)
            }
        }
    }

    // Locațiile afișate pe hartă (combină căutarea live cu recomandările pe stări)
    val displayedLocations = remember(searchQuery, selectedCategory, selectedMood, onlineLocations) {
        val localMatches = initialClujLocations.filter { loc ->
            val matchesSearch = searchQuery.isBlank() ||
                    loc.name.contains(searchQuery, ignoreCase = true) ||
                    loc.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "Toate" ||
                    loc.category.equals(selectedCategory, ignoreCase = true)

            val matchesMood = selectedMood == "Toate" ||
                    loc.dominantMood.equals(selectedMood, ignoreCase = true)

            matchesSearch && matchesCategory && matchesMood
        }

        if (onlineLocations.isNotEmpty()) {
            (onlineLocations + localMatches).distinctBy { it.name.lowercase() }
        } else {
            localMatches
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. HARTA INTERACTIVĂ OPENSTREETMAP
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true) // Zoom cu 2 degete / pinch
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    minZoomLevel = 11.0
                    maxZoomLevel = 20.0
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(46.7694, 23.5898))
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                mapView.overlays.clear()

                displayedLocations.forEach { place ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(place.latitude, place.longitude)
                        title = "${place.moodEmoji} ${place.name}"
                        snippet = "${place.category} • ${place.address}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { m, _ ->
                            selectedPlace = place
                            m.showInfoWindow()
                            mapView.controller.animateTo(GeoPoint(place.latitude, place.longitude))
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        // 2. BARA - CĂUTARE CU LUPĂ + FILTRE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                        }
                        Column {
                            Text(
                                text = "Cluj-Napoca Map",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Ink
                            )
                            Text(
                                text = if (isSearchingOnline) "Searching in Cluj..." else "${displayedLocations.size} locațions on map",
                                fontSize = 12.sp,
                                color = Teal
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (showSearchBar) Teal.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Teal)
                        }

                        IconButton(
                            onClick = { showFilterPanel = !showFilterPanel },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (showFilterPanel) Teal.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = Teal)
                        }
                    }
                }
            }

            // BARA DE CĂUTARE LIVE (Caută orice restaurant, stradă din Cluj)
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Type...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    performLiveSearch(searchQuery)
                                }
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        onlineLocations = emptyList()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                performLiveSearch(searchQuery)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Search")
                        }
                    }
                }
            }

            // PANOUL DE FILTRE (Tipuri de locații + Stări)
            AnimatedVisibility(visible = showFilterPanel) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Location type:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = cat
                                        if (cat != "Toate") {
                                            performLiveSearch(cat)
                                        } else {
                                            onlineLocations = emptyList()
                                        }
                                    },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Teal,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Text("Filter by mood:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            moods.forEach { (moodName, emoji) ->
                                val isSelected = selectedMood == moodName
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMood = moodName },
                                    label = { Text("$emoji $moodName") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Teal,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                selectedMood = initialMoodFilter
                            }) {
                                Text("My mood: $initialMoodFilter", color = Teal)
                            }

                            TextButton(onClick = {
                                selectedCategory = "Toate"
                                selectedMood = "Toate"
                                searchQuery = ""
                                onlineLocations = emptyList()
                                mapViewRef?.controller?.animateTo(GeoPoint(46.7694, 23.5898))
                                mapViewRef?.controller?.setZoom(15.0)
                            }) {
                                Text("Reset all", color = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }

        // 3. BUTOANE DE ZOOM si centrare
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedPlace != null) 150.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                containerColor = Color.White,
                contentColor = Ink
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In (+)")
            }

            SmallFloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                containerColor = Color.White,
                contentColor = Ink
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out (-)")
            }

            SmallFloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.animateTo(GeoPoint(46.7694, 23.5898))
                    mapViewRef?.controller?.setZoom(15.0)
                },
                containerColor = Teal,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Centrează Cluj")
            }
        }

        // 4. DETALII LOCAL SELECTAT
        selectedPlace?.let { place ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Teal.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(place.moodEmoji, fontSize = 20.sp)
                                }
                            }
                            Column {
                                Text(place.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                                Text("${place.category} • ${place.address}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { selectedPlace = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    Text(
                        text = "Atmosferă asociată: ${place.moodEmoji} ${place.dominantMood}",
                        fontWeight = FontWeight.SemiBold,
                        color = Teal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}