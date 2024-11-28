package davidwong.example.weatherui

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    // Function to fetch weather data from OpenWeatherMap
    fun fetchWeatherData(latitude: Double, longitude: Double, apiKey: String, onResult: (String, Float) -> Unit) {
        weatherApiService.getCurrentWeather(latitude, longitude, apiKey).enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: retrofit2.Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { weatherResponse ->
                        // Extract weather description and temperature
                        val description = weatherResponse.weather.firstOrNull()?.description ?: "No description"
                        val temperature = weatherResponse.main.temp
                        onResult(description, temperature)
                    }
                } else {
                    Log.e("WeatherRepository", "Error fetching weather data: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherRepository", "Network call failed: ${t.message}")
            }
        })
    }
}
