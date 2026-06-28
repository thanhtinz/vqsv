plugins {
    id("com.android.application") version "8.2.2"
    kotlin("android") version "1.9.24"
}

val gdxVersion = "1.12.1"

android {
    namespace = "com.vqsv.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vqsv.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Package the shared game assets (clients/core/assets/**) into the APK so
    // Gdx.files.internal("game/...") resolves on Android.
    sourceSets {
        getByName("main") {
            assets.srcDirs("../core/assets")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}
