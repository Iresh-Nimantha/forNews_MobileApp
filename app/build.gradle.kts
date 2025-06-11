plugins {
    id("com.android.application")
    // remove alias line for google-services
}

apply(plugin = "com.google.gms.google-services")



android {
    namespace = "com.example.fotnews"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fotnews"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
//buildFeatures {
//    viewBinding = true
//}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)

    // Firebase BoM for version management
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")

    // Firebase Storage for image URLs
    implementation("com.google.firebase:firebase-storage")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CardView for your news cards
    implementation("androidx.cardview:cardview:1.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}


