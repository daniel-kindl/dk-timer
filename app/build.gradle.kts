import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.detekt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

/**
 * CI build counter for the `dev` channel, passed as `-PdevBuildNumber=<n>`.
 *
 * Dev builds are published on every push to `dev`, so they need a version that
 * increases on its own without anyone editing [versionName]. The workflow passes
 * `github.run_number`, which is monotonic for the lifetime of the repository.
 * Absent (local builds), the variant still assembles and is marked `-dev.local`.
 */
val devBuildNumber = (project.findProperty("devBuildNumber") as String?)?.toIntOrNull()

android {
    namespace = "dev.danielkindl.ocho"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.danielkindl.ocho"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "3.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Read only by AppModule, which turns these into a domain-level UpdateConfig.
        buildConfigField("String", "UPDATE_REPO", "\"daniel-kindl/ocho\"")
    }

    signingConfigs {
        create("release") {
            val keyFile = System.getenv("KEYSTORE_FILE")
                ?: keystoreProperties["storeFile"]?.toString()
            if (keyFile != null) {
                storeFile = file(keyFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: keystoreProperties["storePassword"]?.toString()
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: keystoreProperties["keyAlias"]?.toString()
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: keystoreProperties["keyPassword"]?.toString()
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("String", "UPDATE_CHANNEL", "\"dev\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val hasSigningConfig = signingConfigs.getByName("release").storeFile != null
            if (hasSigningConfig) signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "UPDATE_CHANNEL", "\"stable\"")
        }
        // Testing channel: installs alongside the stable app and self-updates from
        // GitHub prereleases. Inherits release's minification deliberately — an R8
        // rule stripping something it shouldn't is exactly the class of bug this
        // channel exists to catch before `main` sees it.
        create("dev") {
            initWith(getByName("release"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = devBuildNumber?.let { "-dev.$it" } ?: "-dev.local"
            isDebuggable = false
            buildConfigField("String", "UPDATE_CHANNEL", "\"dev\"")
            // Must be the release key, not CI's per-run debug key: successive dev
            // APKs signed by different keys fail with INSTALL_FAILED_UPDATE_INCOMPATIBLE.
            val hasSigningConfig = signingConfigs.getByName("release").storeFile != null
            if (hasSigningConfig) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // The codebase compiles clean, so this costs nothing today and stops the
        // first warning from becoming the first of fifty.
        allWarningsAsErrors = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Excluded because they report on the environment rather than on this code,
        // and would fail the build for reasons no commit here can fix:
        //   GradleDependency        - dependency freshness; Dependabot's job
        //   OldTargetApi            - fires whenever Google ships a new SDK
        //   ObsoleteLintCustomCheck - Compose's bundled lint jar vs our Kotlin version
        disable += setOf("GradleDependency", "OldTargetApi", "ObsoleteLintCustomCheck")
    }
    // buildConfig is off by default from AGP 8.0 onwards; the update channel needs it.
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

/**
 * Gives each dev build its own increasing `versionCode`.
 *
 * Android refuses to install an APK whose `versionCode` is not greater than the
 * installed one, so a fixed value would let only the first dev build install.
 * The dev channel has its own `applicationId`, so this counter is independent of
 * the stable app's `versionCode` and cannot collide with it.
 */
androidComponents {
    onVariants(selector().withBuildType("dev")) { variant ->
        variant.outputs.forEach { it.versionCode.set(devBuildNumber ?: 1) }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.compose.material.icons.extended)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.org.json)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}

detekt {
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
    source.setFrom("src/main/kotlin", "src/test/kotlin")
}
