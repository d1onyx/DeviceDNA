# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

DeviceDNA is a device diagnostics app with two native frontends sharing a single Kotlin Multiplatform (KMM) core:

- `shared/` — KMM module (`commonMain`, `androidMain`, `iosMain`). Holds domain models, repository interfaces, use cases, and **platform-agnostic business logic** (`HealthAnalyzer`, `Formatters`, `PrivacyMask`). Builds to a static iOS framework named `shared`.
- `android/` — Android app (Jetpack Compose, single-module `com.devstdvad.devicedna`). Implements the shared repository interfaces with Android data sources.
- `ios/` — native Swift shell that **hosts the shared Compose Multiplatform UI** from the `shared` framework. The Swift files (`DeviceDNAApp.swift`, `AuthBridge.swift`, `AdsHost.swift`, `StoreKitBilling.swift`) only bridge iOS-native SDKs (Firebase Auth/Google Sign-In, AdMob, StoreKit); every screen and all business logic come from `shared/src/iosMain` on top of `commonMain` (see below).

## Commands

All Gradle commands run from the repo root (use `./gradlew`).

```bash
./gradlew :android:assembleDebug          # build Android debug APK
./gradlew :android:installDebug           # build + install on device/emulator
./gradlew :android:testDebugUnitTest      # run JVM unit tests (all suites live in android/src/test)
./gradlew :android:lint                   # Android lint
./gradlew :android:signingReport          # SHA-1 fingerprints (needed for Firebase Google Sign-In)
```

Run a single test class/method:

```bash
./gradlew :android:testDebugUnitTest --tests "com.devstdvad.devicedna.HealthAnalyzerTest"
./gradlew :android:testDebugUnitTest --tests "*OverviewViewModelTest.someMethod"
```

iOS is built in Xcode (requires macOS): `cd ios && xcodegen generate && pod install && open DeviceDNAApp.xcworkspace` (the `.xcodeproj` is generated from `project.yml`, not committed). The shared framework can be compiled on macOS with `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.

## Architecture

The data flow is the same on every screen, Clean-Architecture style across the module boundary:

```
Compose Screen → ViewModel → UseCase → Repository (interface, shared) → RepositoryImpl (android) → AndroidXxxDataSource
```

- **Domain (shared, `commonMain`)**: `domain/model/*` immutable models, `domain/repository/Repositories.kt` interfaces, `domain/usecase/UseCases.kt` thin invokable wrappers (`operator fun invoke()`). Every repository call returns `AppResult<T>` (sealed `Success`/`Error`, with `AppError` variants like `PermissionDenied`, `Unavailable`). Live metrics (battery, RAM, CPU, thermal, network) additionally expose `observeXxx(): Flow<AppResult<T>>`.
- **Data (android)**: `data/repository/RepositoryImpls.kt` (all impls in one file) delegate to `data/source/AndroidXxxDataSource.kt`. Data sources do the actual Android API calls and wrap results in `AppResult`. `observe*` flows are typically poll loops (`flow { while(true){ emit(...); delay(...) } }.flowOn(Dispatchers.IO)`).
- **Presentation (android)**: one package per feature under `presentation/`. ViewModels are plain `androidx.lifecycle.ViewModel`s exposing a single `data class XxxState` via `StateFlow`, collecting use-case flows in `init`. Match this pattern (see `presentation/battery/BatteryViewModel.kt`) for new screens.
- **DI**: Koin, single module in `android/.../di/AppModule.kt`. Registration order is DataSource → Repository (bound to interface) → UseCase → `viewModelOf(::XxxViewModel)`. Started in `DeviceDnaApp` (the `Application`). Add every new data source, repository, use case, and view model here.

### Navigation

`navigation/NavRoutes.kt` defines 5 bottom-nav roots (`dashboard`, `hardware`, `system`, `apps`, `tests`) plus `settings`. Hardware and System screens are **hubs**: their sub-sections (CPU, Battery, Display, OS, Network, Sensors, …) are tab IDs *within* `HardwareScreen`/`SystemHubScreen`, not separate NavHost destinations. `navigation/AppNavigation.kt` also gates on auth/onboarding state before showing the main scaffold.

### Design system

Two layers, do not confuse them:
- `core/design/` — the **active** design system (`AppTheme`, `AppColors`, `AppSpacing`, `AppTypography`, and `core/design/component/*` reusable components). Shared tokens live in `shared/.../core/design/DesignTokens.kt`.
- `ui/theme/` — legacy default Compose theme scaffolding (`Color.kt`, `Theme.kt`, `Type.kt`); prefer `core/design` for new UI.

### iOS uses the shared framework directly

iOS links the `shared` KMP framework and renders the shared Compose Multiplatform UI: `shared/src/iosMain/.../ui/MainViewController.kt` exposes it as a `UIViewController` that the Swift shell hosts. There is **no** hand-written Swift mirror of the business logic — formatting and health-scoring live only in `shared/commonMain` (`Formatters.kt`, `HealthAnalyzer.kt`) and are shared by both platforms, so a change there needs no iOS follow-up. iOS-specific data sources and DI live in `shared/src/iosMain` (`di/KoinIos.kt`, `data/source/IosRepositories.kt`). Xcode compiles the framework via the `:shared:embedAndSignAppleFrameworkForXcode` build phase.

## Conventions

- Versions are centralized in `gradle/libs.versions.toml` (version catalog `libs.*`); do not hardcode dependency versions in `build.gradle.kts`.
- `minSdk = 26`, `compileSdk`/`targetSdk = 36`, Java 11.
- Tests use JUnit4 + MockK + `kotlinx-coroutines-test` (+ `koin-test`). Existing suites: `HealthAnalyzerTest`, `HardwareUseCaseTest`, `OverviewViewModelTest`, `ExportManagerTest`, `BatteryDataTest`, `PrivacyMaskTest`.

## Firebase

Google Sign-In via Firebase Auth. Customer-specific Firebase/app ids come from `local.properties` and generated Firebase config files. The `com.google.gms.google-services` plugin only applies if `android/google-services.json` exists. Config files are refreshed via `scripts/setup-firebase-auth.ps1` (PowerShell, requires `firebase login`). See `FIREBASE_AUTH_SETUP.md` for the Android SHA-1 / web-OAuth-client requirement and `ios/README.md` for the Xcode URL-scheme setup.
