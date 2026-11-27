plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStoreFile = providers.gradleProperty("PU_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("PU_RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("PU_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("PU_RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("PU_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("PU_RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("PU_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("PU_RELEASE_KEY_PASSWORD"))
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isPresent }
val apiBaseUrl = providers.gradleProperty("PU_API_BASE_URL").getOrElse("")
val runtimeCatalogPublicKey = providers.gradleProperty("PU_RUNTIME_CATALOG_PUBLIC_KEY").getOrElse("")
val compatibilityPublicKey = providers.gradleProperty("PU_COMPATIBILITY_PUBLIC_KEY").getOrElse("")

android {
    namespace = "sgyt.pu.lite"
    compileSdk = 36

    defaultConfig {
        applicationId = "SGYT.PU_LITE"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-lite"

        buildConfigField("String", "PU_EDITION", "\"lite\"")
        buildConfigField("String", "PU_DEFAULT_PROFILE", "\"Low\"")
        buildConfigField("boolean", "PU_WINDOWS_INCLUDED", "false")
        buildConfigField("boolean", "PU_CLOUD_SYNC_ENABLED", "false")
        buildConfigField("boolean", "PU_TRANSLATION_ENABLED", "false")
        buildConfigField("boolean", "PU_BACKGROUND_WORK_ENABLED", "false")
        buildConfigField("String", "PU_API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "PU_RUNTIME_CATALOG_PUBLIC_KEY", "\"$runtimeCatalogPublicKey\"")
        buildConfigField("String", "PU_COMPATIBILITY_PUBLIC_KEY", "\"$compatibilityPublicKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("puRelease") {
                storeFile = rootProject.file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("puRelease")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:1.15.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
