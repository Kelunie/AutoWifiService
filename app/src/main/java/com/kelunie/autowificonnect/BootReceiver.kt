// call the package name
package com.kelunie.autowificonnect

// call the imports
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log


// creating the boot receiver
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, WifiService::class.java)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8+ → se requiere startForegroundService
                    context?.startForegroundService(serviceIntent)
                    Log.d("BootReceiver", "Servicio WiFi arrancado en modo foreground (Android 8+).")
                } else {
                    // Android 7 o menor → funciona con startService
                    context?.startService(serviceIntent)
                    Log.d("BootReceiver", "Servicio WiFi arrancado en modo normal (Android 7-).")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error al arrancar WifiService tras boot", e)
            }
        }
    }
}
