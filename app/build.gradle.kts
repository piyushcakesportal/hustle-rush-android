plugins {
    id("com.android.application")
}

val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank()
        && !releaseKeystorePassword.isNullOrBlank()
        && !releaseKeyAlias.isNullOrBlank()
        && !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.hustlerush.cashrunner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hustlerush.cashrunner"
        minSdk = 23
        targetSdk = 36
    }

    flavorDimensions += "releaseMode"
    productFlavors {
        create("internal") {
            dimension = "releaseMode"
            versionCode = 8
            versionName = "2.0.0-test"
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("boolean", "USE_TEST_ADS", "true")
            buildConfigField("String", "REWARDED_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "INTERSTITIAL_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        create("production") {
            dimension = "releaseMode"
            versionCode = 9
            versionName = "2.0.0"
            manifestPlaceholders["admobAppId"] = "ca-app-pub-8512097229727629~4157317521"
            buildConfigField("boolean", "USE_TEST_ADS", "false")
            buildConfigField("String", "REWARDED_UNIT_ID", "\"ca-app-pub-8512097229727629/6574376119\"")
            buildConfigField("String", "INTERSTITIAL_UNIT_ID", "\"ca-app-pub-8512097229727629/3171538913\"")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug { }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}
