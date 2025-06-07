package com.example.weatherappjetpackcompose.utils

import android.content.Context
import com.example.weatherappjetpackcompose.api.model.WeatherResponse
import com.google.gson.Gson
import java.io.File

object CacheUtils {
    private const val CACHE_DIR = "weather_cache"
    private const val CACHE_AGE_MS = 1 * 60 * 1000L  // 3 godziny 3 * 60 * 60 * 1000L

    fun getFile(context: Context, city: String): File {
        val dir = File(context.filesDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        // nazwa pliku: miasto.json
        return File(dir, "${city.lowercase().replace(" ", "_")}.json")
    }

    fun saveCache(context: Context, city: String, data: WeatherResponse) {
        val file = getFile(context, city)
        val wrapper = CacheWrapper(System.currentTimeMillis(), data)
        file.writeText(Gson().toJson(wrapper))
    }

    fun loadCache(context: Context, city: String): WeatherResponse? {
        val file = getFile(context, city)
        if (!file.exists()) return null
        val wrapper = try {
            Gson().fromJson(file.readText(), CacheWrapper::class.java)
        } catch (e: Exception) {
            file.delete()
            return null
        }
        return wrapper.data
    }

    fun isCacheValid(context: Context, city: String): Boolean {
        val file = getFile(context, city)
        if (!file.exists()) return false
        val wrapper = try {
            Gson().fromJson(file.readText(), CacheWrapper::class.java)
        } catch (e: Exception) {
            file.delete()
            return false
        }
        return System.currentTimeMillis() - wrapper.timestamp <= CACHE_AGE_MS
    }

    fun getLastUpdateTime(context: Context, city: String): Long? {
        val file = getFile(context, city)
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
