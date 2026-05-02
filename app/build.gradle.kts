plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.terry.duey"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.terry.duey"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val geminiApiKey = (project.findProperty("GEMINI_API_KEY") as String?) ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("stage") {
            dimension = "environment"
            applicationIdSuffix = ".stage"
            resValue("string", "app_name", "Duey Stage")
            buildConfigField("boolean", "IS_STAGE", "true")
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "true")
            // TODO: Replace with real GitHub-hosted stage version.json URL.
            buildConfigField("String", "UPDATE_CHECK_URL", "\"https://example.com/duey/stage/version.json\"")
            buildConfigField("String", "UPDATE_CHANNEL", "\"stage\"")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Duey")
            buildConfigField("boolean", "IS_STAGE", "false")
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")
            buildConfigField("String", "UPDATE_CHECK_URL", "\"\"")
            buildConfigField("String", "UPDATE_CHANNEL", "\"prod\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    val roomVersion = libs.versions.room.get()
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("org.json:json:20240303")
    
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
