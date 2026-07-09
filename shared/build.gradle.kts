plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }

    android {
        namespace = "com.devstdvad.devicedna.shared"
        compileSdk = 37
        minSdk = 26

        compilerOptions {
            // 17 (not 11) because GitLive Firestore's inline functions are built for JVM 17.
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            export(libs.jetbrains.lifecycle.viewmodel.compose)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Compose Multiplatform UI (shared @Composable screens).
            // Applied via explicit coordinates (not the org.jetbrains.compose plugin) because
            // that plugin's Android integration is incompatible with AGP 9.x's
            // com.android.kotlin.multiplatform.library variant API. The compose-compiler
            // plugin (kotlin.compose) still handles @Composable compilation.
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.ui.tooling.preview)
            implementation(libs.compose.material.icons.extended)
            api(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)
            // Koin Multiplatform for shared ViewModels + koinViewModel() in commonMain
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Remote config: GitLive Firestore + multiplatform-settings for the persistent store.
            implementation(libs.firebase.gitlive.firestore)
            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            // Firebase BOM supplies the versions for the native Firestore SDK that GitLive wraps.
            implementation(project.dependencies.platform(libs.firebase.bom))
            // Ed25519 verification (works on minSdk 26, unlike java.security "Ed25519" which needs API 33).
            implementation(libs.bouncycastle.prov)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
