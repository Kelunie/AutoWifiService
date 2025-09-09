# AutoWifiConnect

An Android application designed to automatically manage and maintain Wi-Fi connectivity.

## Features

*   **Automatic Wi-Fi Management:** Keeps your device connected to Wi-Fi.
*   **Starts on Boot:** The Wi-Fi management service starts automatically when your device boots up.
*   **Foreground Service:** Runs a foreground service to ensure reliable operation, categorized as a "connectedDevice" service.

## Permissions

This application requires the following permissions to function correctly:

*   `android.permission.ACCESS_WIFI_STATE`: To access the current state of Wi-Fi.
*   `android.permission.CHANGE_WIFI_STATE`: To enable or disable Wi-Fi and manage connections.
*   `android.permission.ACCESS_FINE_LOCATION` & `android.permission.ACCESS_COARSE_LOCATION`: Required by Android systems (typically Android 8.0 Oreo and above) to scan for Wi-Fi networks.
*   `android.permission.INTERNET`: To allow network communication if needed (e.g., checking connectivity).
*   `android.permission.ACCESS_NETWORK_STATE`: To access the current state of network connectivity.
*   `android.permission.RECEIVE_BOOT_COMPLETED`: To start the service automatically after the device reboots.
*   `android.permission.POST_NOTIFICATIONS`: (For Android 13 and above) To display notifications related to the service.
*   `android.permission.FOREGROUND_SERVICE`: To run a foreground service.
*   `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`: To specify the type of foreground service.

## Components

*   **`WifiService`**: The core service responsible for managing the Wi-Fi connection. It runs as a foreground service to ensure it's not killed by the system.
*   **`BootReceiver`**: A broadcast receiver that listens for the `BOOT_COMPLETED` action to automatically start the `WifiService` when the device starts.
*   **`MainActivity`**: The main user interface of the application.

## How to Build

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build the project using Gradle.

## Additional Information

*   **Version:** 1.0.0
*   **Wi-Fi Configuration:** In the current version, Wi-Fi credentials (SSID and password) must be configured directly in the code.
*   **Initial Setup:** The application must be run at least once to grant all necessary permissions for it to function correctly.
*   **User Interface:** This version does not include a custom application icon or an advanced user interface. The `MainActivity` is primarily for initial permission granting and basic interaction.

## License

MIT License

Copyright (c) 2025 Caleb Rodriguez Cordero

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.