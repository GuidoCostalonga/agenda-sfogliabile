plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Numero progressivo della compilazione su GitHub Actions: ogni nuova versione
// ha un codice più alto e Android la accetta come aggiornamento.
val numeroCompilazione = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val chiaveFirma: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "it.guidocostalonga.agendasfogliabile"
    compileSdk = 34

    defaultConfig {
        applicationId = "it.guidocostalonga.agendasfogliabile"
        minSdk = 26
        targetSdk = 34
        versionCode = numeroCompilazione
        versionName = "1.0.$numeroCompilazione"
    }

    signingConfigs {
        create("rilascio") {
            if (chiaveFirma != null) {
                storeFile = file(chiaveFirma)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (chiaveFirma != null) {
                signingConfigs.getByName("rilascio")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
