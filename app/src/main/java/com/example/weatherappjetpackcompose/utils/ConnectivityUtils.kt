package com.example.weatherappjetpackcompose.utils

import android.content.Context
import android.net.ConnectivityManager

object ConnectivityUtils {
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        //robie to tak, bo zalezy mi tylko na tym, czy telefon ma internet,
        //a nie koniecznie to, czy ten internet jest z wifi, ethernet czy inne zrodla
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return networkInfo != null && networkInfo.isConnected
    }
}
