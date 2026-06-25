# Firebase Authentication Setup

This project is wired for Firebase Authentication with Google Sign-In on Android and iOS.

## Current Firebase project

- Project ID: `projektdna-4fc7d`
- Android package: `com.devstdvad.devicedna`
- iOS bundle ID: `com.devstdvad.devicedna`

## CLI setup

The Firebase CLI is available in this workspace, but it must be logged in before app configs can be created or downloaded:

```powershell
firebase login
.\scripts\setup-firebase-auth.ps1
```

For Android Google Sign-In, add the SHA-1 for each signing certificate you use:

```powershell
.\gradlew.bat :android:signingReport
.\scripts\setup-firebase-auth.ps1 -AndroidSha1 "AA:BB:CC:..."
```

The script refreshes:

- `android/google-services.json`
- `ios/DeviceDNAApp/GoogleService-Info.plist`

If the script warns that `android/google-services.json` does not contain a web OAuth client, enable the Google provider, add the Android SHA-1 fingerprint, and run the script again. Without that web client, Android cannot generate `default_web_client_id` for Google Sign-In.

## Firebase Console

Enable the Google provider in Firebase Console:

1. Open Authentication.
2. Open Sign-in method.
3. Enable Google.
4. Set the public support email.

The Firebase CLI can manage app registration and SDK config files, but the Google sign-in provider still needs to be enabled in the console for this project.

## iOS Xcode setup

After the script downloads `GoogleService-Info.plist`:

1. Add it to the `DeviceDNAApp` Xcode target.
2. Open the plist and copy `REVERSED_CLIENT_ID`.
3. In Target > Info > URL Types, add that value as a URL Scheme.

The SwiftUI app now checks for the plist before configuring Firebase, so a missing plist shows a setup error instead of touching Firebase Auth too early.
