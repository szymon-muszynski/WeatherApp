package com.example.weatherappjetpackcompose.api

import android.content.Context
import com.example.weatherappjetpackcompose.api.model.WeatherResponse
import com.example.weatherappjetpackcompose.utils.CacheUtils
import com.example.weatherappjetpackcompose.utils.ConnectivityUtils
import com.example.weatherappjetpackcompose.utils.LatestCityCacheUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository(private val context: Context) {

    private val api = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)

    suspend fun getWeatherForecast(city: String, country: String, isFavorite: Boolean = false): WeatherResponse {
        val online = ConnectivityUtils.isOnline(context)

        if (online) {
            val isCacheValid = if (isFavorite)
                CacheUtils.isCacheValid(context, city, country)
            else
                false

            if (!isCacheValid) {
                val fresh = fetchFromNetwork("$city,$country")

                if (isFavorite)
                    CacheUtils.saveCache(context, city, country, fresh)
                else
                    LatestCityCacheUtils.saveLatestCity(context, fresh)

                return fresh
            }

            return CacheUtils.loadCache(context, city, country)
                ?: fetchFromNetwork("$city,$country").also {
                    if (isFavorite) CacheUtils.saveCache(context, city, country, it)
                    else LatestCityCacheUtils.saveLatestCity(context, it)
                }
        } else {
            return if (isFavorite)
                CacheUtils.loadCache(context, city, country)
                    ?: throw IllegalStateException("Brak internetu i cache dla $city,$country")
            else
                LatestCityCacheUtils.loadLatestCity(context)
                    ?: throw IllegalStateException("Brak internetu i brak danych latest_city")
        }
    }

    suspend fun refreshFavoritesIfNeeded(favoriteCities: List<String>) {
        if (!ConnectivityUtils.isOnline(context)) return

        for (favorite in favoriteCities) {
            val parts = favorite.split(",")
            if (parts.size == 2) {
                val city = parts[0]
                val country = parts[1]
                if (!CacheUtils.isCacheValid(context, city, country)) {
                    try {
                        val fresh = fetchFromNetwork("$city,$country")
                        CacheUtils.saveCache(context, city, country, fresh)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private suspend fun fetchFromNetwork(cityAndCountry: String): WeatherResponse {
        val apiKey = "3134e3769c5e4c5e990190005250106"
        return api.getForecast(apiKey, cityAndCountry)
    }
}