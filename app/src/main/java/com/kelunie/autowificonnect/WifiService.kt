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
import android.net.wifi.WifiNetworkSpecifier
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

    private val targetSsid = "" // WiFi name
    private val targetPassword = "" // WiFi password - Corrected: Escaped '$'

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
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        connectivityManager = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        handler = Handler(Looper.getMainLooper())

        val intentFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiStateReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(wifiStateReceiver, intentFilter)
        }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Wi-Fi Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Wi-Fi Auto Connect")
            .setContentText("Manteniendo conexión a $targetSsid")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
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
            .setOngoing(true)
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
                    startActivity(panelIntent) // This will open a panel for the user to enable Wi-Fi
                } catch (e: Exception) {
                    Log.e("WifiService", "Error abriendo panel de Wi-Fi", e)
                }
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }
            // Give some time for Wi-Fi to enable before checking connection
            handler.postDelayed({ checkAndConnectToWifiInternal() }, 2000)
            return // Exit and let the delayed call handle the connection attempt
        }
        checkAndConnectToWifiInternal()
    }


    private fun checkAndConnectToWifiInternal() {
        val currentNetwork = connectivityManager.activeNetwork
        if (currentNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                if (wifiInfo != null && wifiInfo.networkId != -1 && wifiInfo.bssid != null) {
                    val currentSsidRaw = wifiInfo.ssid.removeSurrounding("\"") // Used for isConnectedToOurTarget
                    val isConnectedToOurTarget = currentSsidRaw == targetSsid

                    if (isConnectedToOurTarget) {
                        Log.d("WifiService", "Ya conectado a $targetSsid")
                        updateNotification("Conectado a $targetSsid")
                        return
                    }
                    // Check for hidden network case where SSID might be <unknown ssid>
                    if (wifiInfo.ssid == "<unknown ssid>") {
                        val configuredNetwork = wifiManager.configuredNetworks?.find {
                            @Suppress("DEPRECATION")
                            it.networkId == wifiInfo.networkId && it.SSID == "\"$targetSsid\"" && it.hiddenSSID
                        }
                        if (configuredNetwork != null) {
                            Log.d("WifiService", "Ya conectado a red oculta $targetSsid")
                            updateNotification("Conectado a $targetSsid")
                            return
                        }
                    }
                }
            }
        }

        Log.d("WifiService", "No conectado a $targetSsid, forzando reconexión (posiblemente oculta)...")
        updateNotification("Reconectando a $targetSsid...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(targetSsid)
                .setWpa2Passphrase(targetPassword)
                .setIsHiddenSsid(true) // For hidden networks
                .build()

            val suggestions = listOf(suggestion)
            val status = wifiManager.addNetworkSuggestions(suggestions)

            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Log.d("WifiService", "Sugerencia para red oculta $targetSsid enviada correctamente.")
                
                // Optionally, try to force connection using NetworkRequest if suggestion alone isn't enough
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(targetSsid)
                    .setWpa2Passphrase(targetPassword)
                    .setIsHiddenSsid(true)
                    .build()
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier) 
                    .build()
                
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        Log.d("WifiService", "Red (sugerida/especificada) $targetSsid disponible. Vinculando proceso.")
                        try {
                           connectivityManager.bindProcessToNetwork(network)
                        } catch (e: IllegalStateException) { // 'e' is defined here
                            Log.e("WifiService", "Error vinculando proceso a red: ${e.message}")
                        }
                        // Consider unregistering the callback if it's a one-shot attempt
                        // connectivityManager.unregisterNetworkCallback(this)
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        Log.d("WifiService", "Red (sugerida/especificada) $targetSsid no disponible.")
                        // connectivityManager.unregisterNetworkCallback(this)
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        Log.d("WifiService", "Conexión a red (sugerida/especificada) $targetSsid perdida.")
                        // connectivityManager.unregisterNetworkCallback(this)
                    }
                }
                try {
                    connectivityManager.requestNetwork(request, networkCallback, handler, 30000) // 30s timeout
                } catch (se: SecurityException) { // 'se' is defined here
                    Log.e("WifiService", "SecurityException al solicitar red con specifier: ${se.message}")
                } catch (iae: IllegalArgumentException) { // 'iae' is defined here
                     Log.e("WifiService", "IllegalArgumentException al solicitar red con specifier: ${iae.message}")
                }

            } else {
                // 'status' from addNetworkSuggestions is in scope here
                Log.e("WifiService", "Error al agregar sugerencia para red oculta $targetSsid: $status")
            }
        } else { // Pre-Q devices
            @Suppress("DEPRECATION")
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$targetSsid\""
                preSharedKey = "\"$targetPassword\""
                hiddenSSID = true // For hidden networks
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK) // For WPA/WPA2
                // The 'status' field in WifiConfiguration refers to the network's status (enabled/disabled)
                this.status = WifiConfiguration.Status.ENABLED // Correctly assign to WifiConfiguration.status
            }

            @Suppress("DEPRECATION")
            var netId = wifiManager.addNetwork(wifiConfig) // 'netId' is declared here
            var networkUpdated = false // 'networkUpdated' is declared here

            if (netId == -1) { 
                // Try to update if the network already exists
                val existingNetwork = wifiManager.configuredNetworks?.find { it.SSID == "\"$targetSsid\"" } // 'existingNetwork' is declared here
                if (existingNetwork != null) {
                    Log.d("WifiService", "Red oculta $targetSsid existente encontrada (${existingNetwork.networkId}), actualizando config.")
                    wifiConfig.networkId = existingNetwork.networkId 
                    netId = wifiManager.updateNetwork(wifiConfig) // 'netId' is updated here
                    networkUpdated = (netId != -1) // 'networkUpdated' is updated here
                }
            }

            if (netId != -1) {
                Log.d("WifiService", "Red oculta $targetSsid ${if(networkUpdated) "actualizada" else "agregada"} (legacy): $netId. Habilitando...")
                @Suppress("DEPRECATION")
                wifiManager.disconnect() 
                @Suppress("DEPRECATION")
                val enabled = wifiManager.enableNetwork(netId, true) // 'enabled' is declared here
                Log.d("WifiService", "enableNetwork($netId, true) para $targetSsid: $enabled")
                @Suppress("DEPRECATION")
                val reconnected = wifiManager.reconnect() // 'reconnected' is declared here
                Log.d("WifiService", "reconnect() para $targetSsid: $reconnected")

                if (!enabled && !reconnected) {
                     Log.e("WifiService", "Fallo al habilitar o reconectar a la red oculta $targetSsid (legacy)")
                } else if (!enabled) {
                    Log.w("WifiService", "enableNetwork falló pero reconnect() fue llamado para $targetSsid (legacy)")
                }

            } else {
                Log.e("WifiService", "Error al agregar/actualizar configuración para red oculta $targetSsid (legacy)")
            }
        }
    }
}
