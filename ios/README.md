# DeviceDNA iOS

The iOS app shares its UI and business logic with Android through the Kotlin
Multiplatform `shared` module (Compose Multiplatform). The native Swift layer is a
thin shell that hosts the shared Compose UI and bridges the platform SDKs that must
be native on iOS: Firebase Authentication with Google/Apple Sign-In, AdMob, and
StoreKit billing.

## Project layout

- **Shared UI & logic** — `shared/src/iosMain` (Kotlin).
  `ui/MainViewController.kt` exposes the shared Compose UI as a `UIViewController`;
  screens, navigation, DI (`di/KoinIos.kt`), and the iOS data sources
  (`data/source/IosRepositories.kt`) live under the same tree, on top of
  `shared/commonMain`.
- **Native shell** — `ios/DeviceDNAApp/*.swift`:
  - `DeviceDNAApp.swift` — SwiftUI `@main` entry; configures Firebase and Google
    Sign-In, hosts the Compose `UIViewController`, and runs background refresh tasks.
  - `AuthBridge.swift` — Firebase Auth with Google and Apple Sign-In.
  - `AdsHost.swift` — AdMob banner and interstitial.
  - `StoreKitBilling.swift` — StoreKit subscription purchase and restore.
- **Project generation** — `ios/project.yml` (XcodeGen) and `ios/Podfile`
  (CocoaPods). There is no committed `.xcodeproj`; it is generated from `project.yml`.

## Setup (requires macOS + Xcode)

```sh
cd ios
xcodegen generate          # builds DeviceDNAApp.xcodeproj from project.yml
pod install                # Firebase, GoogleSignIn, AdMob pods
open DeviceDNAApp.xcworkspace
```

The Xcode "Build Kotlin shared framework" build phase runs
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`, so the `shared` framework is
recompiled and embedded on every build.

### Firebase configuration

1. In the Firebase Console (same project as Android), add an iOS app with the bundle
   id `com.devstdvad.devicedna` (or the customer's `iosBundleId`).
2. Enable **Google** under Authentication → Sign-in method.
3. Place `GoogleService-Info.plist` in `ios/DeviceDNAApp/`.
   `scripts/setup-firebase-auth.sh` can download it for you. (This file is
   gitignored; `GoogleService-Info.plist.example` is the committed template.)
4. The Google Sign-In URL scheme must equal the `REVERSED_CLIENT_ID` value from the
   plist — it is set in `project.yml`; regenerate the project with `xcodegen` after
   changing it.

### Entitlements (optional, for Wi-Fi SSID)

To read the current Wi-Fi SSID, add the `com.apple.developer.networking.wifi-info`
entitlement and enable it in the App capabilities.

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
