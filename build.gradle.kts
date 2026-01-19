// MessManagement/build.gradle.kts  (TOP LEVEL)

plugins {
    alias(libs.plugins.android.application) apply false

    // 🔹 Add this line: plugin id + version
    id("com.google.gms.google-services") version "4.4.2" apply false
}
