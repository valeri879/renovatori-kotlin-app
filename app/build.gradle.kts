plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ge.renovatori.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ge.renovatori.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // Emulator reaches the host machine's dev server through 10.0.2.2.
            // Override with -PapiOrigin=http://<lan-ip>:3000 for a real device.
            val apiOrigin = providers.gradleProperty("apiOrigin").getOrElse("http://10.0.2.2:3000")
            buildConfigField("String", "API_ORIGIN", "\"$apiOrigin\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_ORIGIN", "\"https://renovatori.ge\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
