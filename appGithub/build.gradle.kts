import app.blockclock.convention.AppConfig
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.serialization)
    id("app.blockclock.convention.application")
}

android {
    namespace = AppConfig.packageId

    defaultConfig {
        val threshold = Date().apply { time += 1000 * 60 * 60 * 8 }
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(threshold)
        buildConfigField("String", "UPDATE_THRESHOLD", "\"$date\"")
    }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.ktor.core)
    implementation(libs.ktor.cio)
    implementation(libs.ktor.negotiation)
    implementation(libs.ktor.json)
}
