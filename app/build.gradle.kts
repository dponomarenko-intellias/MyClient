plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
    alias(pluginLibs.plugins.android.application)
    alias(pluginLibs.plugins.kotlin.android)
    alias(pluginLibs.plugins.ksp)
    alias(pluginLibs.plugins.hilt)
}

android {
    namespace = "com.example.myclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myclient"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation(libs.coroutines.android)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.timber)

    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.junit.ext)
    androidTestImplementation(testLibs.espresso.core)

}
