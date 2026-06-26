plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("Clearer-${variant.name}.apk")
        }
    }
}

android {
    namespace = "com.example.clearer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.clearer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
}
