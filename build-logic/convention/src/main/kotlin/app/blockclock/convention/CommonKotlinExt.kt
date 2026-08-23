package app.blockclock.convention

import app.blockclock.convention.AppConfig
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

internal fun CommonExtension.configureKotlinAndroid() {
    compileSdk {
        version = release(AppConfig.compileSdk) { minorApiLevel = AppConfig.compileSdkMinor }
    }

    defaultConfig.minSdk = AppConfig.minSdk
    compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    compileOptions.targetCompatibility = JavaVersion.VERSION_17
}
