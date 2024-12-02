package davidwong.example.weatherui

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WeatherAlertService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Periodically check for severe weather
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}