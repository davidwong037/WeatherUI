package davidwong.example.weatherui

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    // Function to fetch weather data from OpenWeatherMap


    private var isTesting = false

    fun enableTestMode() {
        isTesting = true
    }

    fun fetchWeatherData(latitude: Double, longitude: Double, apiKey: String, onResult: (String, Float) -> Unit) {
        if (isTesting) {
            // Simulate severe weather conditions
            val simulatedDescription = "Thunder"
            val simulatedTemperature = 8.0f
      //      onResult(simulatedDescription, simulatedTemperature)
            return
        }



        weatherApiService.getCurrentWeather(latitude, longitude, apiKey).enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: retrofit2.Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { weatherResponse ->
                        // Extract weather description and temperature
                        val description = weatherResponse.weather.firstOrNull()?.description ?: "No description"
                        val temperature = weatherResponse.main.temp
                      //  val windspeed = weatherResponse.wind.speed
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



