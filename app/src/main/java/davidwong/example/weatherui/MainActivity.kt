package davidwong.example.weatherui

import android.Manifest
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
import android.util.Log
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
import androidx.compose.ui.platform.ComposeView
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherRepository: WeatherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize fusedLocationClient and WeatherRepository
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val retrofitClient = Retrofit.Builder()
            .baseUrl("http://api.weatherstack.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherRepository = WeatherRepository(retrofitClient.create(WeatherApiService::class.java))

        // Set up the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

    private fun startTrackingLocation() {
        LocationHelper.startLocationUpdates(this, fusedLocationClient) { location ->
            weatherRepository.fetchWeatherData(location.latitude, location.longitude) { description, temp ->
                overlayWeatherOnMap(description, temp, location.latitude, location.longitude)
                updateOverlay(location.latitude, location.longitude, description, temp)
            }
        }
    }
    private fun updateOverlay(lat: Double, lon: Double, description: String, temperature: Int) {
        val composeView = findViewById<ComposeView>(R.id.tracker_overlay)
        composeView.setContent {
            WeatherUITheme {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Column {
                        Text("Location: ($lat, $lon)", style = MaterialTheme.typography.bodyLarge)
                        Text("Weather: $description", style = MaterialTheme.typography.bodyLarge)
                        Text("Temperature: $temperature°C", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    private fun overlayWeatherOnMap(description: String, temperature: Int, lat: Double, lon: Double) {
        val markerOptions = MarkerOptions()
            .position(LatLng(lat, lon))
            .title("Weather: $description")
            .snippet("Temp: $temperature°C")
        googleMap.addMarker(markerOptions)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}

