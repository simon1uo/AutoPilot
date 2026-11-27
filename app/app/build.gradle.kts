import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// 签名信息：优先 keystore.properties（本地，不入库），其次环境变量（CI Secrets）
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun propOrEnv(name: String, envName: String): String? =
    keystoreProps.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.simon1uo.autopilot"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "io.github.simon1uo.autopilot"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        val storePath = propOrEnv("storeFile", "KEYSTORE_PATH")
        if (storePath != null) {
            create("release") {
                val abs = rootProject.file(storePath)
                storeFile = if (abs.exists()) abs else file(storePath)
                storePassword = propOrEnv("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = propOrEnv("keyAlias", "KEY_ALIAS")
                keyPassword = propOrEnv("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 有 release 签名配置时才签名，否则保持 unsigned（CI 无密钥场景）
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // 分发渠道：
    //  direct = GitHub / 酷安 / 官网直发，带应用内自更新
    //  foss   = F-Droid / 应用商店，禁用自更新（避免与收录政策冲突）
    flavorDimensions += "dist"
    productFlavors {
        create("direct") {
            dimension = "dist"
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
        create("foss") {
            dimension = "dist"
            buildConfigField("boolean", "SELF_UPDATE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
