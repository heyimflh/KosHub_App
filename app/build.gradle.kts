plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {

    namespace = "com.koshub.psdku"

    compileSdk = 36

    defaultConfig {

        applicationId = "com.koshub.psdku"

        minSdk = 24

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAPBOX_TOKEN", "\"${project.findProperty("MAPBOX_TOKEN") ?: ""}\"")
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_11

        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.activity.ktx)

    implementation(libs.appcompat)

    implementation(libs.constraintlayout)

    implementation(libs.material)

    implementation("androidx.core:core:1.12.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("com.mapbox.maps:android:11.10.0")
    implementation("com.mapbox.navigationcore:android:3.10.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    // Google Sign In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:3.0.2")

    testImplementation(libs.junit)

    androidTestImplementation(libs.espresso.core)

    androidTestImplementation(libs.ext.junit)
}