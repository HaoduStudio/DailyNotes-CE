plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("io.sentry.android.gradle") version "5.12.0"
}

android {
    namespace = "com.haodustudio.DailyNotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.haodustudio.DailyNotes"
        minSdk = 24
        targetSdk = 33
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resConfigs("xhdpi")
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
    }
}

dependencies {
    val room_version = "2.5.0"

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    // Jetpack
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    // androidx views
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    // other
    implementation("com.github.bumptech.glide:glide:4.9.0")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
    implementation("com.github.rahatarmanahmed:circularprogressview:2.5.0")
    implementation("com.github.VictorAlbertos:RxActivityResult:0.5.0-2.x")
    implementation("io.reactivex.rxjava2:rxjava:2.2.3")
    implementation("com.github.yellowcath:VideoProcessor:2.4.2")
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.6.2")
    implementation("com.squareup.retrofit2:converter-gson:2.6.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("io.sentry:sentry-android:7.13.0")
}

sentry {
    org = "nexaorion"
    projectName = "dailynotes-ce"
    includeSourceContext = true
}

