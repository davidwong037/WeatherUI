package davidwong.example.weatherui

data class WeatherResponse(
    val location: LocationData,
    val current: CurrentWeather
)

data class LocationData(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double
)

data class CurrentWeather(
    val temperature: Int,
    val weather_descriptions: List<String>,
    val wind_speed: Int,
    val pressure: Int
)
