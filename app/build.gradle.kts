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
        // No baseline (#154): every finding is an error, so a warning fails the build. A count
        // that is not a plural (an abbreviation, an ordinal) is marked tools:ignore="PluralsCandidate".
        warningsAsErrors = true
        error += setOf("PluralsCandidate", "TypographyEllipsis")
        // These report that a newer library, plugin or target API exists, so their result changes
        // whenever something is released, not when the code changes. Dependabot opens the
        // dependency updates; raising targetSdk is a deliberate change of its own.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Core KTX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    // Declared so the tests compile against the same coroutines as the app (one version ref).
    implementation(libs.kotlinx.coroutines.android)

    // AppCompat for language settings
    implementation(libs.androidx.appcompat)

    // AboutLibraries
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    // Reorderable (drag and drop)
    implementation(libs.reorderable)

    // Installs the Baseline Profile on devices without Play Store cloud profiles (F-Droid).
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    robolectricRuntime(libs.robolectric.android.all)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
