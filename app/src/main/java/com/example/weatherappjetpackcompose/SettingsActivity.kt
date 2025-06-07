package com.example.weatherappjetpackcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherappjetpackcompose.utils.FavoriteUtils
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.weatherappjetpackcompose.utils.CacheUtils

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(activity: ComponentActivity) {
    val context = activity.applicationContext

    val allFavorites = remember {
        mutableStateListOf<String>().apply {
            addAll(FavoriteUtils.getFavorites(context))
        }
    }

    val favoriteStates = remember {
        mutableStateMapOf<String, MutableState<Boolean>>().apply {
            allFavorites.forEach { cityCountry ->
                this[cityCountry] = mutableStateOf(true)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            favoriteStates.forEach { (cityCountry, isFavState) ->
                val parts = cityCountry.split(",")
                if (parts.size == 2) {
                    val city = parts[0]
                    val country = parts[1]
                    if (isFavState.value) {
                        FavoriteUtils.addFavorite(context, city, country)
                    } else {
                        FavoriteUtils.removeFavorite(context, city, country)
                        val file = CacheUtils.getFile(context, city, country)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Favorite Locations:", style = MaterialTheme.typography.headlineMedium)

            if (allFavorites.isEmpty()) {
                Text("No favorite locations.", style = MaterialTheme.typography.headlineSmall)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allFavorites) { cityCountry ->
                        val isFavorite = favoriteStates[cityCountry]!!

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .clickable {
                                        val resultIntent = Intent().apply {
                                            putExtra("selected_city", cityCountry)
                                        }
                                        activity.setResult(android.app.Activity.RESULT_OK, resultIntent)
                                        activity.finish()
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location icon",
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = cityCountry,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            IconButton(onClick = {
                                isFavorite.value = !isFavorite.value
                            }) {
                                Icon(
                                    imageVector = if (isFavorite.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite.value) "Unfavorite" else "Favorite",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}