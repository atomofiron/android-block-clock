plugins {
    `java-gradle-plugin` // needed?
    `kotlin-dsl`
}

group = "app.blockclock.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.gradle)
}

gradlePlugin {
    plugins {
        create("androidApplication") {
            id = "app.blockclock.convention.application"
            implementationClass = "app.blockclock.convention.AndroidApplicationConventionPlugin"
        }
        create("androidLibrary") {
            id = "app.blockclock.convention.library"
            implementationClass = "app.blockclock.convention.AndroidLibraryConventionPlugin"
        }
    }
}