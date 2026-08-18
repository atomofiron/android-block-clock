import java.io.ByteArrayOutputStream
import java.io.File

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

aboutLibraries {
    fetchRemoteLicense = true
    outputFileName = "licenses.json"
}

fun Project.registerUpdateBundledOssLicensesTask() {
    tasks.register("updateBundledLicenses") {
        group = "licenses"
        description = "Generate bundled OSS licenses (third_party_licenses format) into src/main/assets/licenses"

        dependsOn("exportLibraryDefinitions")

        val input = layout.buildDirectory.file("generated/aboutLibraries/licenses.json")
        val outputDir = layout.projectDirectory.dir("src/main/assets/licenses")
        inputs.file(input)
        outputs.dir(outputDir)

        doLast {
            val json = input.get().asFile.readText()
            val root = groovy.json.JsonSlurper().parseText(json) as Map<*, *>
            val libraries = root["libraries"] as List<Map<String, Any?>>

            val licensesBytes = ByteArrayOutputStream()
            val metadata = StringBuilder()
            libraries.forEach { library ->
                val licenseArray = library["licenses"] as? List<*> ?: emptyList<Any?>()
                var content: Any? = licenseArray.firstOrNull()
                    ?.takeIf { it is Map<*, *> }
                    ?.let {
                        val license = it as Map<*, *>
                        license["content"] ?: license["url"]
                    }
                if (content == null || content.toString().isEmpty()) {
                    content = library["website"]
                }
                if (content == null || content.toString().isEmpty()) return@forEach
                val bytes = content.toString().toByteArray(Charsets.UTF_8)
                val index = licensesBytes.size()
                licensesBytes.write(bytes)
                licensesBytes.write('\n'.code)
                val name = library["name"] ?: library["artifactId"]
                metadata.append("$index:${bytes.size}:$name\n")
            }

            outputDir.asFile.mkdirs()
            File(outputDir.asFile, "third_party_licenses").writeBytes(licensesBytes.toByteArray())
            File(outputDir.asFile, "third_party_license_metadata")
                .writeText(metadata.toString(), Charsets.UTF_8)
        }
    }
}

registerUpdateBundledOssLicensesTask()
