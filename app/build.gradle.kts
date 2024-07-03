import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.bunny.entertainment.factoid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bunny.entertainment.factoid"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read the API key from local.properties
        val waifuDotItApiKey: String = localProperties.getProperty("WAIFU_DOT_IT_API_KEY") ?: ""
        buildConfigField("String", "WAIFU_DOT_IT_API_KEY", "\"$waifuDotItApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            buildFeatures {
                buildConfig = true
            }
            applicationIdSuffix = ".debug"
            isDebuggable = true
            resValue("string", "app_name", "Anify Debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}
//noinspection UseTomlInstead
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    //glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.1")
}