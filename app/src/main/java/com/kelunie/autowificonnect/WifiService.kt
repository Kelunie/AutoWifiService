// call the package name
package com.kelunie.autowificonnect

// imports
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat


// creating the service class
class WifiService : Service() {

    // creating variables
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var handler: Handler
    private val checkInterval: Long = 3000

    private val targetSsid = "Wifi_name" // WiFi name
    private val targetPassword = "Wifi_password" // WiFi password

    // creating the receiver
    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == WifiManager.WIFI_STATE_CHANGED_ACTION ||
                action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                Log.d("WifiService", "Cambio detectado en Wi-Fi. Reintentando conexión...")
                checkAndConnectToWifi()
            }
        }
    }

    /* creating the service lifecycle methods */
    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        handler = Handler(Looper.getMainLooper())

        registerReceiver(
            wifiStateReceiver,
            IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            }
        )

        startForegroundService()
        startRepeatingCheck()
    }

    // creating the service lifecycle methods
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(wifiStateReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // creating the foreground service
    private fun startForegroundService() {
        val channelId = "wifi_service_channel"
        val channel = NotificationChannel(channelId, "Wi-Fi Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Wi-Fi Auto Connect")
            .setContentText("Manteniendo conexión a $targetSsid")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
    }

    // creating the repeating check
    private fun startRepeatingCheck() {
        handler.post(object : Runnable {
            override fun run() {
                checkAndConnectToWifi()
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    // updating the notification
    private fun updateNotification(text: String) {
        val notification: Notification = NotificationCompat.Builder(this, "wifi_service_channel")
            .setContentTitle("Wi-Fi Auto Connect")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    // creating the check method
    private fun checkAndConnectToWifi() {
        if (!wifiManager.isWifiEnabled) {
            Log.d("WifiService", "Wi-Fi apagado. Intentando habilitarlo...")
            updateNotification("Wi-Fi apagado, habilitándolo...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(panelIntent)
                } catch (e: Exception) {
                    Log.e("WifiService", "Error abriendo panel de Wi-Fi", e)
                }
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }
        }

        val currentNetwork = connectivityManager.activeNetwork
        if (currentNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                if (wifiInfo != null && wifiInfo.ssid == "\"$targetSsid\"" &&
                    wifiInfo.networkId != -1 && wifiInfo.bssid != null
                ) {
                    Log.d("WifiService", "Ya conectado a $targetSsid")
                    updateNotification("Conectado a $targetSsid")
                    return
                }
            }
        }

        Log.d("WifiService", "No conectado a $targetSsid, forzando reconexión...")
        updateNotification("Reconectando a $targetSsid...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(targetSsid)
                .setWpa2Passphrase(targetPassword)
                .build()
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Log.d("WifiService", "Sugerencia enviada correctamente.")
            } else {
                Log.e("WifiService", "Error al agregar sugerencia: $status")
            }
        } else {
            @Suppress("DEPRECATION")
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$targetSsid\""
                preSharedKey = "\"$targetPassword\""
            }
            @Suppress("DEPRECATION")
            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId != -1) {
                @Suppress("DEPRECATION")
                wifiManager.enableNetwork(netId, true)
                @Suppress("DEPRECATION")
                wifiManager.reconnect()
            }
        }
    }
}
