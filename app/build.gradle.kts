import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.licenses)
}

android {
    namespace = "app.atomofiron.blockclock"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "app.atomofiron.blockclock"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

afterEvaluate {
    registerUpdateBundledOssLicensesTask()
}

fun Project.registerUpdateBundledOssLicensesTask() {
    val releaseTaskName = "releaseOssLicensesTask"
    if (tasks.findByName(releaseTaskName) == null) {
        return logger.lifecycle("OSS release task not found, skipping OSS license bundling for $path")
    }
    tasks.register<Copy>("updateBundledLicenses") {
        group = "licenses"
        description = "Generate OSS licenses (release) and bundle them into src/main/assets"

        dependsOn(releaseTaskName)

        from(layout.buildDirectory.dir("generated/res/releaseOssLicensesTask/raw"))
        into(layout.projectDirectory.dir("src/main/assets/licenses"))
        doFirst {
            logger.lifecycle("Updating bundled OSS licenses for $path")
        }
    }
}
