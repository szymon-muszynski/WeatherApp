package com.example.weatherappjetpackcompose.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weatherappjetpackcompose.api.WeatherRepository
import com.example.weatherappjetpackcompose.utils.CacheUtils
import com.example.weatherappjetpackcompose.utils.ConnectivityUtils.isOnline
import kotlinx.coroutines.launch
import com.example.weatherappjetpackcompose.utils.FavoriteUtils
import com.example.weatherappjetpackcompose.utils.LatestCityCacheUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}

@Composable
fun WeatherScreen(
    defaultCity: String = "Lodz",
    onOpenSettings: () -> Unit,
    onCityChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // 1) Zmieniamy inicjalizację na pustą
    var city by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTempUnit by rememberSaveable { mutableStateOf("°C") }

    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var forecast by remember { mutableStateOf<List<WeatherDayUi>>(emptyList()) }
    var locationName by remember { mutableStateOf("") }
    var locationCountry by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }

    var refreshTrigger by remember { mutableStateOf(0) }

    // 2) Zawsze reagujemy, gdy defaultCity się zmieni (czyli po wyborze z menu)
    LaunchedEffect(defaultCity) {
        if (defaultCity.isNotEmpty()) {
            city = defaultCity
            searchQuery = defaultCity
        }
    }

    LaunchedEffect(city, refreshTrigger) {
        loading = true
        error = null
        scope.launch {
            try {
                val isFav = FavoriteUtils.getFavorites(context).contains(city)
                val data = WeatherRepository(context).getWeatherForecast(city, isFav)

                lastUpdateTime = if (isFav) {
                    CacheUtils.getLastUpdateTime(context, city)
                } else {
                    LatestCityCacheUtils.getLastUpdateTime(context)
                }

                locationName = data.location.name
                locationCountry = data.location.country
                forecast = data.forecast.forecastday.map {
                    WeatherDayUi(
                        date = it.date,
                        temperature = it.day.avgtemp_c,
                        humidity = it.day.avghumidity,
                        uv = it.day.uv,
                        conditionText = it.day.condition.text,
                        conditionIconUrl = "https:${it.day.condition.icon}"
                    )
                }
            } catch (e: Exception) {
                error = e.localizedMessage
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    city = searchQuery
                    onCityChanged(searchQuery)

                    if (!isOnline(context)) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Brak internetu. Pokazano ostatnią zapisaną lokalizację.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                onOpenSettings = onOpenSettings
            )

            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Error: $error", Modifier.padding(16.dp))
                }

                else -> {

                    val onRefresh: () -> Unit = {
                        if (isOnline(context)) {
                            if (FavoriteUtils.getFavorites(context).contains(city)) {
                                CacheUtils.getFile(context, city).delete()
                            }
                            refreshTrigger++
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Brak internetu. Nie można odświeżyć pogody.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }

                    if (isPortrait) {
                        PortraitLayout(
                            locationName, locationCountry, forecast, selectedTempUnit,
                            onTempUnitChange = { selectedTempUnit = it },
                            onRefresh = onRefresh,
                            lastUpdateTime = lastUpdateTime
                        )
                    } else {
                        LandscapeLayout(
                            locationName, locationCountry, forecast, selectedTempUnit,
                            onTempUnitChange = { selectedTempUnit = it },
                            onRefresh = onRefresh,
                            lastUpdateTime = lastUpdateTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortraitLayout(
    locationName: String,
    locationCountry: String,
    forecast: List<WeatherDayUi>,
    selectedTempUnit: String,
    onTempUnitChange: (String) -> Unit,
    onRefresh: () -> Unit,
    lastUpdateTime: Long?
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {

        UnitSelection(selectedTempUnit, onTempUnitChange, onRefresh)
        Spacer(modifier = Modifier.height(8.dp))
        WeatherHeader(locationName, locationCountry, showRefreshButton = false, onRefresh=onRefresh)
        if (forecast.isNotEmpty()) {
            MainWeatherCard(
                modifier = Modifier.fillMaxWidth(),
                day = forecast[0],
                selectedTempUnit,
                lastUpdateTime = lastUpdateTime
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // fill remaining space
                    .verticalScroll(scrollState)
            ) {
                ForecastRow(
                    forecast = forecast.drop(1).take(8),
                    selectedTempUnit = selectedTempUnit
                )
            }
        }
    }
}


@Composable
fun LandscapeLayout(
    locationName: String,
    locationCountry: String,
    forecast: List<WeatherDayUi>,
    selectedTempUnit: String,
    onTempUnitChange: (String) -> Unit,
    onRefresh: () -> Unit,
    lastUpdateTime: Long?
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {

        WeatherHeader(
            locationName,
            locationCountry,
            showRefreshButton = true,
            onRefresh = onRefresh,
            selectedTempUnit = selectedTempUnit,
            onTempUnitChange = onTempUnitChange
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(end = 8.dp)

            ) {
                if (forecast.isNotEmpty()) {
                    MainWeatherCard(
                        modifier = Modifier
                            .fillMaxHeight(),
                        day = forecast[0],
                        selectedTempUnit = selectedTempUnit,
                        lastUpdateTime = lastUpdateTime

                    )
                }
            }

            val scrollState = rememberScrollState()

            Column(modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(start = 8.dp)
                .verticalScroll(scrollState)
            ) {
                if (forecast.size > 1) {
                    ForecastRow(
                        forecast = forecast.drop(1).take(8),
                        selectedTempUnit = selectedTempUnit
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = {
                Text(
                    "Search for any location",
                    fontSize = if (isTablet()) 20.sp else 16.sp
                ) },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearch) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(40.dp)
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}


@Composable
fun WeatherHeader(
    locationName: String,
    locationCountry: String,
    showRefreshButton: Boolean = true,
    onRefresh: () -> Unit,
    selectedTempUnit: String? = null,
    onTempUnitChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val favorite = remember(locationName) {
        mutableStateOf(FavoriteUtils.isFavorite(context, locationName))
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location icon",
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "$locationName, $locationCountry",
                fontSize = if (isTablet()) 30.sp else 24.sp,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLandscape && selectedTempUnit != null && onTempUnitChange != null) {
                TextButton(onClick = { onTempUnitChange("°C") }) {
                    Text("°C",
                        fontSize = if (isTablet()) 24.sp else 18.sp,
                        color = if (selectedTempUnit == "°C") MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
                TextButton(onClick = { onTempUnitChange("°F") }) {
                    Text("°F",
                        fontSize = if (isTablet()) 24.sp else 18.sp,
                        color = if (selectedTempUnit == "°F") MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
            }
        }

        if (showRefreshButton) {
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        IconButton(onClick = {
            val newFavorite = !favorite.value
            favorite.value = newFavorite
            if (newFavorite) {
                FavoriteUtils.addFavorite(context, locationName)
            } else {
                FavoriteUtils.removeFavorite(context, locationName)
                val file = CacheUtils.getFile(context, locationName)
                if (file.exists()) {
                    file.delete()
                }
            }
        }) {
            Icon(
                imageVector = if (favorite.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (favorite.value) "Unfavorite" else "Favorite",
                modifier = Modifier.size(40.dp),
            )
        }

    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun UnitSelection(
    selectedTempUnit: String,
    onTempUnitChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Temperatura
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onTempUnitChange("°C") }) {
                Text("°C", fontSize = if (isTablet()) 30.sp else 24.sp, color = if (selectedTempUnit == "°C") MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            }
            TextButton(onClick = { onTempUnitChange("°F") }) {
                Text("°F", fontSize = if (isTablet()) 30.sp else 24.sp, color = if (selectedTempUnit == "°F") MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}



@Composable
fun MainWeatherCard(
    modifier: Modifier = Modifier,
    day: WeatherDayUi,
    selectedTempUnit: String,
    lastUpdateTime: Long?
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 👇 Info o braku internetu
            if (!isOnline(context)) {
                Text(
                    text = "Brak połączenia z internetem – dane mogą być nieaktualne",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val formattedTime = lastUpdateTime?.let {
                        val sdf = SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "?"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            modifier = Modifier.size(if (isTablet()) 28.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Date: ${formattedTime}", fontSize = if (isTablet()) 24.sp else 18.sp)                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InvertColors, // kropla
                            contentDescription = "Humidity",
                            modifier = Modifier.size(if (isTablet()) 28.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Humidity: ${day.humidity}%", fontSize = if (isTablet()) 24.sp else 18.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny, // słońce
                            contentDescription = "UV Index",
                            modifier = Modifier.size(if (isTablet()) 28.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UV index: ${day.uv}", fontSize = if (isTablet()) 24.sp else 18.sp)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    val displayedTemp = if (selectedTempUnit == "°C") {
                        "${day.temperature}°C"
                    } else {
                        val tempF = (day.temperature * 9 / 5) + 32
                        "${"%.1f".format(tempF)}°F"
                    }
                    Text(displayedTemp,fontSize = if (isTablet()) 40.sp else 30.sp)
                    AsyncImage(
                        model = day.conditionIconUrl,
                        contentDescription = day.conditionText,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(day.conditionText, fontSize = if (isTablet()) 34.sp else 24.sp)
                }
            }
        }
    }
}

@Composable
fun ForecastRow(forecast: List<WeatherDayUi>, selectedTempUnit: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        forecast.forEach { day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth, // kropla
                                contentDescription = "Date",
                                modifier = Modifier.size(if (isTablet()) 28.dp else 20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = day.date,
                                fontSize = if (isTablet()) 24.sp else 18.sp,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        val displayedTemp = if (selectedTempUnit == "°C") {
                            "${day.temperature}°C"
                        } else {
                            val tempF = (day.temperature * 9 / 5) + 32
                            "${"%.1f".format(tempF)}°F"
                        }
                        Text(displayedTemp,
                            fontSize = if (isTablet()) 24.sp else 18.sp,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.InvertColors, // kropla
                                contentDescription = "Humidity",
                                modifier = Modifier.size(if (isTablet()) 28.dp else 20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${day.humidity}%", fontSize = if (isTablet()) 24.sp else 18.sp)
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny, // słońce
                                contentDescription = "UV Index",
                                modifier = Modifier.size(if (isTablet()) 28.dp else 20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${day.uv}", fontSize = if (isTablet()) 24.sp else 18.sp)
                        }
                    }
                    AsyncImage(
                        model = day.conditionIconUrl,
                        contentDescription = day.conditionText,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

data class WeatherDayUi(
    val date: String,
    val temperature: Float,
    val humidity: Int,
    val uv: Float,
    val conditionText: String,
    val conditionIconUrl: String
)

