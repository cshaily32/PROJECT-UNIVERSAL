plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register("assembleLiteApk") {
    group = "build"
    description = "Builds the standalone PU Lite release APK."
    dependsOn(":lite:assembleRelease")
}

tasks.register("assemblePlusApk") {
    group = "build"
    description = "Builds the standalone PU Plus release APK."
    dependsOn(":plus:assembleRelease")
}

tasks.register("assembleEditions") {
    group = "build"
    description = "Builds the PU Lite and PU Plus release APKs."
    dependsOn(":lite:assembleRelease", ":plus:assembleRelease")
}
