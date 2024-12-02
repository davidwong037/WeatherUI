package davidwong.example.weatherui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import davidwong.example.weatherui.ui.theme.WeatherUITheme
import androidx.activity.result.contract.ActivityResultContracts
import android.location.Location
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.Marker
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherRepository: WeatherRepository
    private val apiKey = "APIKEYHERE" // Insert your OpenWeatherMap API key
    // State variables for weather data
    private var weatherDescription by mutableStateOf("")
    private var temperature by mutableStateOf(0f)
    private var windSpeed by mutableStateOf(0f)
    private var showDialog = mutableStateOf(false)
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            // Use a Box to overlay the map and dialog on top of each other
            Box(modifier = Modifier.fillMaxSize()) {
                // The map will be added here
                WeatherMap()

                // Show the dialog if the state is true
                WeatherAppContent(showDialogState = showDialog, alertMessage = "Severe weather alert! Stay safe!")
            }
        }

        setContentView(R.layout.activity_main)
      //  setContent {
            // Set the content to display the dialog when needed
          //  WeatherAppContent(showDialog)
     //   }
        // Initialize fusedLocationClient and WeatherRepository
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val retrofitClient = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherRepository = WeatherRepository(retrofitClient.create(WeatherApiService::class.java))

        // Set up the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }


    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            startTrackingLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
  //  @Composable
    private fun startTrackingLocation() {
    //    weatherRepository.enableTestMode()
        LocationHelper.startLocationUpdates(this, fusedLocationClient) { location ->
            weatherRepository.fetchWeatherData(location.latitude, location.longitude, apiKey) { description, temp ->
                weatherDescription = description // Set the weather description
                temperature = temp // Set the temperature
            //    windSpeed = wind
                overlayWeatherOnMap(description, temp, location.latitude, location.longitude)
                updateOverlay(location.latitude, location.longitude, description, temp.toInt())
                checkForSevereWeather(description, windSpeed = 60f, temperature = temp)

                // Smoothly update the camera to follow the user's location
                val userLocation = LatLng(location.latitude, location.longitude)
            //    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLocation, 16f) // Adjust zoom level as needed
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f), 1000, null)



            }
        }
    }
    private fun updateOverlay(lat: Double, lon: Double, description: String, temperature: Int) {
        val composeView = findViewById<ComposeView>(R.id.tracker_overlay)
        composeView.setContent {
            WeatherOverlay(description, temperature.toFloat(), windSpeed.toFloat())
        }
    }

    private fun overlayWeatherOnMap(description: String, temperature: Float, lat: Double, lon: Double) {
        val tempString = String.format("%.1f", temperature) // Format the float to a string with 1 decimal place
        val tempWind = String.format("%.1f", windSpeed)
        if (currentMarker == null) {
            // Create a new marker if it doesn't exist
            val markerOptions = MarkerOptions()
                .position(LatLng(lat, lon))
                .title("Weather: $description")
                .snippet("Temp: $tempString°C")
            currentMarker = googleMap.addMarker(markerOptions)
        } else {
            // Update the existing marker's position and information
            currentMarker?.position = LatLng(lat, lon)
            currentMarker?.title = "Weather: $description"
            currentMarker?.snippet = "Temp: $tempString°C"
            currentMarker?.snippet = "Wind: $tempWind km/h"
            currentMarker?.showInfoWindow()
        }
    }
    @Composable
    fun WeatherOverlay(description: String, temperature: Float, windSpeed: Float ) {
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.8f))
                .padding(16.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "Weather: $description",
                style = MaterialTheme.typography.headlineSmall // Updated typography style
            )
            Text(
                text = "Temperature: ${String.format("%.1f", temperature)}°C",
                style = MaterialTheme.typography.bodyMedium // Updated typography style
            )
            Text (
                text = "Wind: ${String.format("%.1f", windSpeed)}Km/h",
                style = MaterialTheme.typography.bodyMedium // Updated typography styl
            )

        }
    }
 //   @Composable
    private fun checkForSevereWeather(description: String, windSpeed: Float, temperature: Float) {
        val prefs = getSharedPreferences("WeatherPreferences", Context.MODE_PRIVATE)
        val stormAlertsEnabled = prefs.getBoolean("storm_alerts", true)

        // Check for severe weather keywords
        val severeWeatherKeywords = listOf("storm", "tornado", "hurricane", "severe", "thunder")
        val isSevereWeather = severeWeatherKeywords.any { keyword ->
            description.contains(keyword, ignoreCase = true)
        }

        if (stormAlertsEnabled && isSevereWeather) {
           //  Trigger Toast or AlertDialog here
            showToast("Severe weather expected: $description. Stay safe!")
            // Trigger Composable Alert Dialog
           showDialog.value=true
            // Trigger system notification (new code)
         //   WeatherAppContent(showDialogState = showDialog, alertMessage = "Severe weather alert! Stay safe!")
         //   sendNotification("Severe weather alert!", description)

        }
    }
    fun sendNotification(title: String, description: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // If you're targeting Android 13 (API level 33) or higher, you need to request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, handle it
                return
            }
        }

        val notificationId = 1
        val notification = NotificationCompat.Builder(this, "weather_alerts")
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_weather_alert) // Your own icon here
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }




    fun showToast(message: String) {

        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER, 0, -100)  // Position it in the center
        toast.show()
    }
    @Composable
    fun WeatherMap() {
        // This composable will be responsible for displaying the map
        AndroidView(
            factory = { context ->
                val mapFragment = SupportMapFragment.newInstance()
                val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                transaction.replace(R.id.map, mapFragment)
                transaction.commit()

                // Ensure that mapFragment.view is not null by returning it only when it's non-null
                mapFragment.view ?: throw IllegalStateException("Map fragment view is null")
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    fun WeatherAppContent(showDialogState: MutableState<Boolean>, alertMessage: String) {
        if (showDialogState.value) {
            CenterNotification(
                message = alertMessage,
                onDismiss = { showDialogState.value = false }
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("MainActivity", "Notification permission granted.")
            } else {
                Log.e("MainActivity", "Notification permission denied.")
            }
        }
    }
    @Composable
    fun CenterNotification(message: String, onDismiss: () -> Unit) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Box(
                modifier = Modifier
                    .size(300.dp) // Customize size
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Alert",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onDismiss() }) {
                        Text(text = "Dismiss")
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Alerts"
            val descriptionText = "Notifications for severe weather alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("weather_alerts", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 2000
    }
}

