# DeviceDNA iOS

A native SwiftUI iOS app with full hardware diagnostics, Firebase Authentication, and Google Sign-In.

## Setup (requires macOS + Xcode)

### 1. Create Xcode project

Open Xcode → File → New → Project → App
- Product Name: `DeviceDNAApp`
- Team: your Apple Developer account
- Bundle ID: `com.devstdvad.devicedna`
- Interface: SwiftUI
- Language: Swift
- Save to: `ios/` (this directory)

Add all `.swift` files from `ios/DeviceDNAApp/` to the Xcode project.

### 2. Install CocoaPods dependencies

```sh
cd ios
pod install
open DeviceDNAApp.xcworkspace
```

### 3. Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a project (or use the same one as Android)
3. Add iOS app with bundle ID `com.devstdvad.devicedna`
4. Download `GoogleService-Info.plist` and add it to the Xcode project root
5. Enable **Google Sign-In** under Authentication → Sign-in providers
6. In Firebase Console → Authentication → Settings → Authorized domains, ensure your domain is listed

### 4. URL Scheme

In Xcode → Target → Info → URL Types, add:
- URL Schemes: the `REVERSED_CLIENT_ID` value from `GoogleService-Info.plist`

### 5. Entitlements (optional, for SSID access)

To read the current Wi-Fi SSID, add `com.apple.developer.networking.wifi-info` entitlement and enable it in App Capabilities.

## What's collected on iOS

| Feature | Status |
|---------|--------|
| Device model & OS | ✅ Full |
| Battery level & state | ✅ Full |
| CPU usage & cores | ✅ Full |
| CPU model (by identifier) | ✅ Partial |
| RAM usage | ✅ Full |
| Storage usage | ✅ Full |
| Display info + brightness | ✅ Full |
| Camera specs | ✅ Full (via AVFoundation) |
| Network type + local IP | ✅ Full |
| Wi-Fi SSID | ⚠️ Requires entitlement |
| Sensors availability | ✅ Full (CoreMotion) |
| Thermal state | ✅ Full (ProcessInfo) |
| App list | ❌ iOS privacy restriction |
| Battery capacity / cycles | ❌ iOS private API |
| Carrier / SIM info | ❌ Requires special entitlement |

## Architecture

- `DeviceDNAApp.swift` — App entry, Firebase/Google setup
- `AuthState.swift` — Firebase auth state management (ObservableObject)
- `ContentView.swift` — Root routing (auth gate → TabView)
- `HardwareService.swift` — All native hardware data collection
- `DesignSystem.swift` — Shared colors, MetricCard, InfoRow, GaugeCard
- `AuthView.swift` — Sign-in screen
- `DashboardView.swift` — Overview with gauges
- `HardwareView.swift` — Hardware tabs (Device/CPU/Battery/Display/Camera)
- `SystemView.swift` — OS, Memory, Network, Sensors
- `AppsView.swift` — App list (restricted, explains iOS policy)
- `SettingsView.swift` — User prefs + sign out
