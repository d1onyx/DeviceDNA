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

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun customerProperty(name: String): String? =
    (localProperties.getProperty(name) ?: (project.findProperty(name) as String?))
        ?.trim()
        ?.takeUnless { it.isBlank() }

fun signingProperty(name: String, envName: String): String? =
    (keystoreProperties.getProperty(name) ?: System.getenv(envName))
        ?.trim()
        ?.takeUnless { it.isBlank() }

fun releaseRequiredProperty(name: String, debugDefault: String? = null): String {
    val value = customerProperty(name)
    if (buildsReleaseArtifact && value == null) {
        error(
            "Missing $name. Add $name=... to local.properties or pass -P$name=... " +
                "before building release artifacts.",
        )
    }
    return value ?: debugDefault.orEmpty()
}

val configuredSyncBaseUrl = (
    customerProperty("syncBaseUrl")
    )
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
    // 37 (not 36) because a dependency requires compiling against API 37+. targetSdk stays 36.
    compileSdk = 37

    defaultConfig {
        applicationId = releaseRequiredProperty("androidApplicationId", "com.example.devicedna")
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Deployed Cloudflare Worker URL. Release builds require an explicit value.
        // Override via local.properties -> syncBaseUrl, or a -PsyncBaseUrl Gradle property.
        val syncBaseUrl = configuredSyncBaseUrl ?: "https://example.invalid"
        buildConfigField("String", "SYNC_BASE_URL", "\"$syncBaseUrl\"")

        val adMobAppId = releaseRequiredProperty(
            "adMobAppId",
            "ca-app-pub-3940256099942544~3347511713",
        )
        val adMobBannerAdUnitId = releaseRequiredProperty(
            "adMobBannerAdUnitId",
            "ca-app-pub-3940256099942544/9214589741",
        )
        val adMobInterstitialAdUnitId = releaseRequiredProperty(
            "adMobInterstitialAdUnitId",
            "ca-app-pub-3940256099942544/1033173712",
        )
        manifestPlaceholders["adMobAppId"] = adMobAppId
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$adMobBannerAdUnitId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$adMobInterstitialAdUnitId\"")

        // Google Play subscription Product ID (must match the one created in Play Console).
        // Override via local.properties -> premiumSubProductId, or a -PpremiumSubProductId Gradle property.
        val premiumSubProductId = releaseRequiredProperty("premiumSubProductId", "devicedna_premium")
        buildConfigField("String", "PREMIUM_SUB_PRODUCT_ID", "\"$premiumSubProductId\"")

        // Dev subscription mode: false (default) unlocks Premium locally with no network; true
        // activates the dev purchase through the backend so it is persisted to Neon end-to-end
        // (for testing the real flow). Has no effect on release (dev billing is debug-only).
        // Override via local.properties -> devSubscriptionUseBackend, or -PdevSubscriptionUseBackend.
        val devSubscriptionUseBackend = customerProperty("devSubscriptionUseBackend")?.toBooleanStrictOrNull() ?: false
        buildConfigField("boolean", "DEV_SUBSCRIPTION_USE_BACKEND", "$devSubscriptionUseBackend")

        // Remote config sync. Required for release artifacts; in debug, empty values keep it
        // inactive (no-op). Configure via local.properties (see the ops runbook):
        //   cfgProjectId, cfgAppId, cfgApiKey, cfgDocPath (default cfg/state),
        //   cfgPubKey (base64 of the raw 32-byte public key).
        buildConfigField("String", "CFG_PROJECT_ID", "\"${releaseRequiredProperty("cfgProjectId")}\"")
        buildConfigField("String", "CFG_APP_ID", "\"${releaseRequiredProperty("cfgAppId")}\"")
        buildConfigField("String", "CFG_API_KEY", "\"${releaseRequiredProperty("cfgApiKey")}\"")
        buildConfigField("String", "CFG_DOC_PATH", "\"${customerProperty("cfgDocPath") ?: "cfg/state"}\"")
        buildConfigField("String", "CFG_PUBKEY", "\"${releaseRequiredProperty("cfgPubKey")}\"")
    }

    // Selects the premium billing implementation (see AppModule). Defaults: debug = dev billing
    // (instant unlock, no Play needed), release = real Google Play Billing. Override either build
    // type with `-PrealBilling=true|false` (or realBilling=... in local.properties) to, e.g., build
    // a debug variant wired to real Play Billing for testing on an internal test track.
    val realBillingOverride = customerProperty("realBilling")?.toBooleanStrictOrNull()

    val releaseStoreFile = signingProperty("releaseStoreFile", "ANDROID_RELEASE_STORE_FILE")
    val releaseStorePassword = signingProperty("releaseStorePassword", "ANDROID_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = signingProperty("releaseKeyAlias", "ANDROID_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = signingProperty("releaseKeyPassword", "ANDROID_RELEASE_KEY_PASSWORD")
    val debugStoreFile = customerProperty("debugStoreFile")
    val debugStorePassword = customerProperty("debugStorePassword")
    val debugKeyAlias = customerProperty("debugKeyAlias")
    val debugKeyPassword = customerProperty("debugKeyPassword")
    val hasDebugSigning = debugStoreFile != null && debugStorePassword != null &&
        debugKeyAlias != null && debugKeyPassword != null
    val hasPartialDebugSigning = !hasDebugSigning &&
        (debugStoreFile != null || debugStorePassword != null ||
            debugKeyAlias != null || debugKeyPassword != null)
    val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null

    if (hasPartialDebugSigning) {
        error(
            "Incomplete Android debug signing. Add debugStoreFile, debugStorePassword, " +
                "debugKeyAlias and debugKeyPassword to local.properties, or remove all debug signing values.",
        )
    }

    if (buildsReleaseArtifact && !hasReleaseSigning) {
        error(
            "Missing Android release signing. Create keystore.properties with " +
                "releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword " +
                "or set ANDROID_RELEASE_STORE_FILE, ANDROID_RELEASE_STORE_PASSWORD, " +
                "ANDROID_RELEASE_KEY_ALIAS, ANDROID_RELEASE_KEY_PASSWORD.",
        )
    }

    signingConfigs {
        if (hasDebugSigning) {
            create("projectDebug") {
                storeFile = rootProject.file(checkNotNull(debugStoreFile))
                storePassword = checkNotNull(debugStorePassword)
                keyAlias = checkNotNull(debugKeyAlias)
                keyPassword = checkNotNull(debugKeyPassword)
            }
        }

        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(checkNotNull(releaseStoreFile))
                storePassword = checkNotNull(releaseStorePassword)
                keyAlias = checkNotNull(releaseKeyAlias)
                keyPassword = checkNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("projectDebug")?.let { signingConfig = it }
            buildConfigField("boolean", "USE_REAL_BILLING", "${realBillingOverride ?: false}")
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
    implementation(libs.androidx.lifecycle.process)
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

if (file("google-services.json").exists() && customerProperty("androidApplicationId") != null) {
    apply(plugin = "com.google.gms.google-services")
}
