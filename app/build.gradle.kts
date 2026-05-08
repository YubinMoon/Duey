plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

val appVersionName = "0.0.1"

fun semVerCode(versionName: String): Int {
    val parts = versionName.split(".")
    require(parts.size == 3) { "versionName must use MAJOR.MINOR.PATCH SemVer format." }
    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    val patch = parts[2].toInt()
    require(major >= 0 && minor in 0..999 && patch in 0..999) {
        "SemVer parts must be non-negative, and minor/patch must be below 1000."
    }
    return major * 1_000_000 + minor * 1_000 + patch
}

fun quotedBuildConfig(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.terry.duey"
    compileSdk = 37

    val prodStoreFile = project.findProperty("PROD_STORE_FILE") as String?
    val prodStorePassword = project.findProperty("PROD_STORE_PASSWORD") as String?
    val prodKeyAlias = project.findProperty("PROD_KEY_ALIAS") as String?
    val prodKeyPassword = project.findProperty("PROD_KEY_PASSWORD") as String?

    signingConfigs {
        if (
            !prodStoreFile.isNullOrBlank() &&
            !prodStorePassword.isNullOrBlank() &&
            !prodKeyAlias.isNullOrBlank() &&
            !prodKeyPassword.isNullOrBlank()
        ) {
            create("prod") {
                storeFile = file(prodStoreFile)
                storePassword = prodStorePassword
                keyAlias = prodKeyAlias
                keyPassword = prodKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.terry.duey"
        minSdk = 26
        targetSdk = 35
        versionCode = semVerCode(appVersionName)
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val googleWebClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?) ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quotedBuildConfig(googleWebClientId))
        buildConfigField("String", "GITHUB_RELEASES_URL", quotedBuildConfig("https://api.github.com/repos/YubinMoon/Duey/releases"))
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Duey Debug")
            buildConfigField("String", "APP_ENV", quotedBuildConfig("debug"))
            val serverBaseUrl = (project.findProperty("SERVER_BASE_URL") as String?) ?: "http://10.0.2.2:8080"
            buildConfigField("String", "SERVER_BASE_URL", quotedBuildConfig(serverBaseUrl))
            buildConfigField("boolean", "IS_STAGE", "false")
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")
        }
        create("stage") {
            applicationIdSuffix = ".stage"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
            resValue("string", "app_name", "Duey Stage")
            buildConfigField("String", "APP_ENV", quotedBuildConfig("stage"))
            val stageServerBaseUrl =
                (project.findProperty("STAGE_SERVER_BASE_URL") as String?)
                    ?: (project.findProperty("SERVER_BASE_URL") as String?)
                    ?: ""
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                quotedBuildConfig(stageServerBaseUrl),
            )
            buildConfigField("boolean", "IS_STAGE", "true")
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "true")
        }
        create("prod") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingConfigs.names.contains("prod")) {
                signingConfig = signingConfigs.getByName("prod")
            }
            resValue("string", "app_name", "Duey")
            buildConfigField("String", "APP_ENV", quotedBuildConfig("prod"))
            val prodServerBaseUrl =
                (project.findProperty("PROD_SERVER_BASE_URL") as String?)
                    ?: (project.findProperty("SERVER_BASE_URL") as String?)
                    ?: ""
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                quotedBuildConfig(prodServerBaseUrl),
            )
            buildConfigField("boolean", "IS_STAGE", "false")
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")
        }
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
    implementation("androidx.credentials:credentials:1.7.0-alpha02")
    implementation("androidx.credentials:credentials-play-services-auth:1.7.0-alpha02")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
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
