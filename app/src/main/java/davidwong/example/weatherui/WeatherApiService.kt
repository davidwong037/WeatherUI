package davidwong.example.weatherui

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    // OpenWeatherMap current weather API endpoint
    @GET("data/2.5/weather")
    fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String, // OpenWeatherMap API Key
        @Query("units") units: String = "metric" // To get temperature in Celsius
    ): Call<WeatherResponse>
}
