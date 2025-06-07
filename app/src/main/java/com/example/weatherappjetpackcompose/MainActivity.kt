package com.example.weatherappjetpackcompose

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.weatherappjetpackcompose.api.WeatherRepository
import com.example.weatherappjetpackcompose.ui.WeatherScreen
import com.example.weatherappjetpackcompose.ui.theme.WeatherAppJetpackComposeTheme
import com.example.weatherappjetpackcompose.utils.FavoriteUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WeatherAppJetpackComposeTheme {
                var selectedCity by rememberSaveable { mutableStateOf("Lodz") }

                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val cityCountry = result.data?.getStringExtra("selected_city")
                        if (!cityCountry.isNullOrEmpty()) {
                            selectedCity = cityCountry
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    val favorites = FavoriteUtils.getFavorites(this@MainActivity).toList()
                    WeatherRepository(this@MainActivity).refreshFavoritesIfNeeded(favorites)
                }

                val city = selectedCity.split(",").getOrNull(0) ?: ""
                val country = selectedCity.split(",").getOrNull(1) ?: ""

                WeatherScreen(
                    defaultCity = city,
                    defaultCountry = country,
                    onOpenSettings = {
                        val intent = Intent(this, SettingsActivity::class.java)
                        settingsLauncher.launch(intent)
                    },
                    onCityChanged = { newCityCountry ->
                        selectedCity = newCityCountry
                    }
                )
            }
        }
    }
}