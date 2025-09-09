// call the package name
package com.kelunie.autowificonnect

// call the imports
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kelunie.autowificonnect.ui.theme.AutoWifiConnectTheme

// creating the main activity
class MainActivity : ComponentActivity() {

    // estade: if we should show the permission rationale
    var shouldShowPermissionRationale by mutableStateOf(false)

    // Android 13+ launcher need ask for permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startWifiService()
            } else {
                // here you can use a message using Toast or Snackbar
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AutoWifiConnectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartServiceClick = { checkAndRequestNotificationPermission() },
                        activity = this
                    )
                }
            }
        }

        // check and request notification permission
        checkAndRequestNotificationPermission()
    }

    // check and request notification permission if needed
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startWifiService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    shouldShowPermissionRationale = true
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startWifiService()
        }
    }

    // start the service in foreground
    private fun startWifiService() {
        val serviceIntent = Intent(this, WifiService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

// main screen composable with start button
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartServiceClick: () -> Unit,
    activity: MainActivity? = null
) {
    var permissionRationaleVisible by remember { mutableStateOf(false) }

    // Observar cambios en shouldShowPermissionRationale
    activity?.let {
        LaunchedEffect(it.shouldShowPermissionRationale) {
            permissionRationaleVisible = it.shouldShowPermissionRationale
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Auto Wi-Fi Connect")

        Button(onClick = {
            activity?.shouldShowPermissionRationale = false
            onStartServiceClick()
        }) {
            Text("Ensure Wi-Fi Service is Running")
        }

        if (permissionRationaleVisible) {
            Text(
                text = "Notification permission is needed to show service status. Please grant the permission.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AutoWifiConnectTheme {
        MainScreen(onStartServiceClick = {})
    }
}
