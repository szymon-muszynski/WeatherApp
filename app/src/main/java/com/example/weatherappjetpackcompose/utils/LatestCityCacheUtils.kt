// Plik: LatestCityCacheUtils.kt
package com.example.weatherappjetpackcompose.utils

import android.content.Context
import com.example.weatherappjetpackcompose.api.model.WeatherResponse
import com.google.gson.Gson
import java.io.File

object LatestCityCacheUtils {
    private const val LATEST_CITY_FILE = "latest_city.json"

    fun saveLatestCity(context: Context, data: WeatherResponse) {
        val wrapper = CacheWrapper(System.currentTimeMillis(), data)
        File(context.filesDir, LATEST_CITY_FILE).writeText(Gson().toJson(wrapper))
    }

    fun loadLatestCity(context: Context): WeatherResponse? {
        val file = File(context.filesDir, LATEST_CITY_FILE)
        if (!file.exists()) return null
        val wrapper = try {
            Gson().fromJson(file.readText(), CacheWrapper::class.java)
        } catch (e: Exception) {
            file.delete()
            return null
        }
        return wrapper.data
    }

    fun getLastUpdateTime(context: Context): Long? {
        val file = File(context.filesDir, LATEST_CITY_FILE)
        if (!file.exists()) return null
        val wrapper = try {
            Gson().fromJson(file.readText(), CacheWrapper::class.java)
        } catch (e: Exception) {
            file.delete()
            return null
        }
        return wrapper.timestamp
    }

    private data class CacheWrapper(
        val timestamp: Long,
        val data: WeatherResponse
    )
}
