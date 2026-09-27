import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.mikepenz.aboutlibraries.plugin.android")
}

// AboutLibraries: exclude timestamp for reproducible builds
// See: https://codeberg.org/Gonbei774/CalisthenicsMemory/issues/2
aboutLibraries {
    // Never reach the network while building (F-Droid builds offline and must be reproducible).
    offlineMode = true
    collect {
        configPath = file("../config/aboutlibraries")
    }
    export {
        excludeFields.addAll("generated")
    }
}

android {
    namespace = "io.github.gonbei774.calisthenicsmemory"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.gonbei774.calisthenicsmemory"
        minSdk = 26
        targetSdk = 35
        versionCode = 35
        versionName = "1.26.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Keystore設定を読み込み
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    // 署名設定
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // リリースビルド時に署名を適用
            signingConfig = signingConfigs.getByName("release")
            // 依存関係メタデータを無効化（F-Droid要件）
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    sourceSets {
        // Exported Room schemas are MigrationTestHelper inputs.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha29")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // AppCompat for language settings
    implementation("androidx.appcompat:appcompat:1.7.0")

    // AboutLibraries
    implementation("com.mikepenz:aboutlibraries-core:15.2.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:15.2.0")

    // Reorderable (drag and drop)
    implementation("sh.calvin.reorderable:reorderable:3.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.8.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")   
}
