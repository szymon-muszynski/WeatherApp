package com.example.weatherappjetpackcompose.api.model

data class WeatherResponse(
    val location: Location,
    val forecast: Forecast
)

data class Location(
    val name: String,
    val country: String
)

data class Forecast(
    val forecastday: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day
)

data class Day(
    val avgtemp_c: Float,
    val avghumidity: Int,
    val uv: Float,
    val condition: Condition
)

data class Condition(
    val text: String,
    val icon: String,
    val code: Int
)
