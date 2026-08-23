import app.blockclock.convention.AppConfig

plugins {
    id("app.blockclock.convention.application")
}

android {
    namespace = AppConfig.packageId
}

dependencies {
    implementation(project(":app"))
    implementation(libs.play.core)
    implementation(libs.play.core.ktx)
}
