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

    suspend fun getWeatherForecast(city: String, isFavorite: Boolean = false): WeatherResponse {
        val online = ConnectivityUtils.isOnline(context)

        //sprawdzam czy dane dla ulubionych lokalizacji sa aktualne, inne mnie nieinteresuja bo zawsze odswiezam je
        if (online) {
            val isCacheValid = if (isFavorite)
                CacheUtils.isCacheValid(context, city)
            else
                false // latest_city odswiezane zawsze

            //jezeli nie jest aktualny, czyli dane sa przestarzale dla danego miasta
            //to pobieram dane z neta, i w zaleznosci czy jest ulubiony czy nie,
            //to korzystam z innego utils, bo w rozny sposob rozdzielam zapisywanie plikow
            if (!isCacheValid) {
                val fresh = fetchFromNetwork(city)

                if (isFavorite)
                    CacheUtils.saveCache(context, city, fresh)
                else
                    LatestCityCacheUtils.saveLatestCity(context, fresh)

                return fresh
            }

            //jezeli sa aktualne to wczytuje z pliku
            //jezeli by sie okazalo jakims jednak cudem ze nie ma tam danych (null),
            //to pobieram z neta od nowa i zapisuje je w odpowiedni sposob
            return CacheUtils.loadCache(context, city)
                ?: fetchFromNetwork(city).also {
                    if (isFavorite) CacheUtils.saveCache(context, city, it)
                    else LatestCityCacheUtils.saveLatestCity(context, it)
                }
        }
        //jezeli nie ma neta, to wczytuje z plikow odpowiednich
        //jakby sie okazalo ze nie ma danych dla danego miasta to rzucam wyjatek
        else {
            return if (isFavorite)
                CacheUtils.loadCache(context, city)
                    ?: throw IllegalStateException("Brak internetu i cache dla $city")
            else
                LatestCityCacheUtils.loadLatestCity(context)
                    ?: throw IllegalStateException("Brak internetu i brak danych latest_city")
        }
    }

    suspend fun refreshFavoritesIfNeeded(favoriteCities: List<String>) {
        if (!ConnectivityUtils.isOnline(context)) return

        for (city in favoriteCities) {
            if (!CacheUtils.isCacheValid(context, city)) {
                try {
                    val fresh = fetchFromNetwork(city)
                    CacheUtils.saveCache(context, city, fresh)
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun fetchFromNetwork(city: String): WeatherResponse {
        val apiKey = "3134e3769c5e4c5e990190005250106"
        return api.getForecast(apiKey, city)
    }
}
