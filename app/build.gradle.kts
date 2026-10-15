import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.mikepenz.aboutlibraries.plugin.android")
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.roborazzi)
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
        applicationId = "io.github.wildebeast2521.kalimory"
        minSdk = 26
        targetSdk = 35
        // Kalimory's own numbering, starting fresh with its own application ID.
        versionCode = 3
        versionName = "1.0.2"

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
            // R8 shrinks, optimises and obfuscates the release build; unused resources go too.
            // The mapping file (build/outputs/mapping/release/mapping.txt) turns a reported stack
            // trace back into source names; scripts/release.sh keeps it with each release.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed with the release key when keystore.properties is present (the maintainer's
            // machine); otherwise unsigned, as F-Droid and other source builds expect.
            signingConfig = if (keystorePropertiesFile.exists()) signingConfigs.getByName("release") else null
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

    testOptions {
        // Robolectric screenshot tests render with the app's real resources and fonts.
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        // Exported Room schemas are MigrationTestHelper inputs.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

// The Baseline Profile plugin adds benchmark and non-minified copies of release. Without a
// keystore in the repository they are signed with the debug key so they can be installed.
androidComponents {
    onVariants(selector().all()) { variant ->
        val type = variant.buildType.orEmpty()
        if (type.startsWith("benchmark") || type.startsWith("nonMinified")) {
            variant.signingConfig.setConfig(android.signingConfigs.getByName("debug"))
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Material 3 Expressive components are used throughout the app (owner decision, 2026-10-02).
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}

// Screenshot tests (Roborazzi on Robolectric). Golden images live in the repository and
// testDebugUnitTest compares against them (roborazzi.test.verify in gradle.properties);
// ./gradlew recordRoborazziDebug rewrites them after a deliberate visual change.
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

// Robolectric would otherwise download its Android framework jar while the tests run. Gradle
// resolves it like any other dependency instead, and Robolectric runs offline from Gradle's cached
// copy. (Not a copy in build/: Robolectric cannot load its native graphics from a path with a space.)
val robolectricRuntime: Configuration by configurations.creating
tasks.withType<Test>().configureEach {
    val runtimeJars = robolectricRuntime
    inputs.files(runtimeJars).withPropertyName("robolectricRuntime").withNormalizer(ClasspathNormalizer::class.java)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Drobolectric.offline=true", "-Drobolectric.dependency.dir=${runtimeJars.singleFile.parentFile.absolutePath}")
    })
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-beta01")
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
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    // Declared so the tests compile against the same coroutines as the app (one version ref).
    implementation(libs.kotlinx.coroutines.android)

    // AppCompat for language settings
    implementation("androidx.appcompat:appcompat:1.8.0")

    // AboutLibraries
    implementation("com.mikepenz:aboutlibraries-core:15.2.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:15.2.0")

    // Reorderable (drag and drop)
    implementation("sh.calvin.reorderable:reorderable:3.1.0")

    // Installs the Baseline Profile on devices without Play Store cloud profiles (F-Droid).
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.room:room-testing:2.8.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.24.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    robolectricRuntime(libs.robolectric.android.all)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")   
}
