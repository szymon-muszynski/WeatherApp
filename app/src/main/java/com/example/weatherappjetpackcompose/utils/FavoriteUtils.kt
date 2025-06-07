package com.example.weatherappjetpackcompose.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object FavoriteUtils {
    private const val PREF_NAME = "weather_prefs"
    private const val FAVORITES_KEY = "favorite_locations"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getFavorites(context: Context): MutableSet<String> {
        return getPrefs(context).getStringSet(FAVORITES_KEY, mutableSetOf()) ?: mutableSetOf()
    }

    fun isFavorite(context: Context, city: String, country: String): Boolean {
        return getFavorites(context).contains("$city,$country")
    }

    fun addFavorite(context: Context, city: String, country: String) {
        val prefs = getPrefs(context)
        val favorites = getFavorites(context).toMutableSet()
        favorites.add("$city,$country")
        prefs.edit().putStringSet(FAVORITES_KEY, favorites).apply()
    }

    fun removeFavorite(context: Context, city: String, country: String) {
        val prefs = getPrefs(context)
        val favorites = getFavorites(context).toMutableSet()
        favorites.remove("$city,$country")
        prefs.edit().putStringSet(FAVORITES_KEY, favorites).apply()
    }
}
