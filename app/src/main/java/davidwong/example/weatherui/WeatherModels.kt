package davidwong.example.weatherui

// WeatherResponse model to parse the JSON response from OpenWeatherMap
data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: Main,
    val name: String // City name
)

data class WeatherDescription(
    val description: String
)

data class Main(
    val temp: Float
)
