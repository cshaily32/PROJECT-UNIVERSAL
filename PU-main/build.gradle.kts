plugins {
    id("com.android.application") version "8.11.0" apply false
    id("com.android.library") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
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
