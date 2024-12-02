package davidwong.example.weatherui

// WeatherResponse model to parse the JSON response from OpenWeatherMap
data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: Main,
    val name: String, // City name
 //   val wind: Wind
)

data class WeatherDescription(
    val description: String // description (e.g, sunny, rainy, cloudy)
)

data class Main(
    val temp: Float, // temp

) /*
data class Wind(
    val speed: Float
)
 */