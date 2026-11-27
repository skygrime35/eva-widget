import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Optional release signing: path to a properties file (storeFile, storePassword, keyAlias, keyPassword).
val signingPath: String? = (findProperty("EVA_WIDGET_SIGNING") as String?) ?: System.getenv("EVA_WIDGET_SIGNING")
val signingProps: Properties? = signingPath
    ?.let { File(it) }
    ?.takeIf { it.isFile }
    ?.let { f -> Properties().also { p -> f.inputStream().use { p.load(it) } } }

android {
    namespace = "fr.evacal.widget"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.evacal.widget"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (signingProps != null) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingProps != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
