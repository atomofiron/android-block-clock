package app.blockclock.convention

import app.blockclock.convention.configureKotlinAndroid
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.VariantDimension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.run {
            apply("com.android.library")
            apply("maven-publish")
        }
        project.extensions.configure<LibraryExtension> {
            configureKotlinAndroid()
            configureAndroidCommon()
        }
    }

    private fun LibraryExtension.configureAndroidCommon() {
        buildFeatures {
            resValues = true
            buildConfig = true
        }
        defaultConfig {
            resValue("string", "version_name", "v${AppConfig.versionName} (${AppConfig.versionCode})")
        }
        buildTypes {
            getByName("debug") {
                buildConfig(true)
            }
            create("alpha") {
                buildConfig(true)
            }
            create("beta") {
                buildConfig(false)
            }
            getByName("release") {
                buildConfig(false)
            }
        }
    }
}

fun VariantDimension.buildConfig(debug: Boolean) {
    // for the future
}
