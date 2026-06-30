import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val configuredSyncBaseUrl = (
    localProperties.getProperty("syncBaseUrl")
        ?: (project.findProperty("syncBaseUrl") as String?)
    )
    ?.trim()
    ?.takeUnless { it.isBlank() }
    ?.removeSuffix("/")

val buildsReleaseArtifact = gradle.startParameter.taskNames.any { taskName ->
    taskName.equals("assemble", ignoreCase = true) ||
        taskName.equals("bundle", ignoreCase = true) ||
        taskName.endsWith(":assemble", ignoreCase = true) ||
        taskName.endsWith(":bundle", ignoreCase = true) ||
        taskName.contains("Release", ignoreCase = true)
}

if (buildsReleaseArtifact && configuredSyncBaseUrl == null) {
    error(
        "Missing syncBaseUrl. Add syncBaseUrl=https://<worker>.<subdomain>.workers.dev " +
            "to local.properties or pass -PsyncBaseUrl=... before building release artifacts.",
    )
}

android {
    namespace = "com.devstdvad.devicedna"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.devstdvad.devicedna"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Deployed Cloudflare Worker URL. Release builds require an explicit value.
        // Override via local.properties -> syncBaseUrl, or a -PsyncBaseUrl Gradle property.
        val syncBaseUrl = configuredSyncBaseUrl ?: "https://devicedna-sync.workers.dev"
        buildConfigField("String", "SYNC_BASE_URL", "\"$syncBaseUrl\"")

        val adMobAppId = localProperties.getProperty("adMobAppId")
            ?: (project.findProperty("adMobAppId") as String?)
            ?: "ca-app-pub-3940256099942544~3347511713"
        val adMobBannerAdUnitId = localProperties.getProperty("adMobBannerAdUnitId")
            ?: (project.findProperty("adMobBannerAdUnitId") as String?)
            ?: "ca-app-pub-3940256099942544/9214589741"
        val adMobInterstitialAdUnitId = localProperties.getProperty("adMobInterstitialAdUnitId")
            ?: (project.findProperty("adMobInterstitialAdUnitId") as String?)
            ?: "ca-app-pub-3940256099942544/1033173712"
        manifestPlaceholders["adMobAppId"] = adMobAppId
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$adMobBannerAdUnitId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$adMobInterstitialAdUnitId\"")

        // Google Play subscription Product ID (must match the one created in Play Console).
        // Override via local.properties -> premiumSubProductId, or a -PpremiumSubProductId Gradle property.
        val premiumSubProductId = localProperties.getProperty("premiumSubProductId")
            ?: (project.findProperty("premiumSubProductId") as String?)
            ?: "devicedna_premium"
        buildConfigField("String", "PREMIUM_SUB_PRODUCT_ID", "\"$premiumSubProductId\"")

        // Dev subscription mode: false (default) unlocks Premium locally with no network; true
        // activates the dev purchase through the backend so it is persisted to Neon end-to-end
        // (for testing the real flow). Has no effect on release (dev billing is debug-only).
        // Override via local.properties -> devSubscriptionUseBackend, or -PdevSubscriptionUseBackend.
        val devSubscriptionUseBackend = (localProperties.getProperty("devSubscriptionUseBackend")
            ?: (project.findProperty("devSubscriptionUseBackend") as String?))?.toBooleanStrictOrNull() ?: false
        buildConfigField("boolean", "DEV_SUBSCRIPTION_USE_BACKEND", "$devSubscriptionUseBackend")
    }

    // Selects the premium billing implementation (see AppModule). Defaults: debug = dev billing
    // (instant unlock, no Play needed), release = real Google Play Billing. Override either build
    // type with `-PrealBilling=true|false` (or realBilling=... in local.properties) to, e.g., build
    // a debug variant wired to real Play Billing for testing on an internal test track.
    val realBillingOverride = (localProperties.getProperty("realBilling")
        ?: (project.findProperty("realBilling") as String?))?.toBooleanStrictOrNull()

    val localKeystorePath = localProperties.getProperty("signingKeystore")
    if (localKeystorePath != null) {
        signingConfigs {
            create("localDebug") {
                storeFile = file(localKeystorePath)
                storePassword = localProperties.getProperty("signingKeystorePassword") ?: "android"
                keyAlias = localProperties.getProperty("signingKeyAlias") ?: "androiddebugkey"
                keyPassword = localProperties.getProperty("signingKeyPassword") ?: "android"
            }
        }
    }

    buildTypes {
        debug {
            val localDebug = signingConfigs.findByName("localDebug")
            if (localDebug != null) signingConfig = localDebug
            buildConfigField("boolean", "USE_REAL_BILLING", "${realBillingOverride ?: false}")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "USE_REAL_BILLING", "${realBillingOverride ?: true}")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.billing.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
