package davidwong.example.weatherui

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    fun fetchWeatherData(lat: Double, lon: Double, onResult: (String, Int) -> Unit) {
        val query = "$lat,$lon"
        val apiKey = "APIKEYHERE"

        weatherApiService.getCurrentWeather(apiKey, query).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        val description = it.current.weather_descriptions.firstOrNull() ?: "No data"
                        val temp = it.current.temperature
                        onResult(description, temp)
                    }
                } else {
                    Log.e("WeatherRepository", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherRepository", "Failed to fetch weather data: ${t.message}")
            }
        })
    }
}
