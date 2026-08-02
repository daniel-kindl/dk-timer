import java.util.Properties
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
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
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.danielkindl.ocho"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "3.3.0"
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
                // rootProject.file, not file: the latter resolves relative paths
                // against app/ rather than the repository root. Absolute paths, which
                // is what CI passes, are returned unchanged either way. This lets
                // keystore.properties hold a relative path, so moving the checkout
                // cannot break signing.
                storeFile = rootProject.file(keyFile)
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

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Excluded because they report on the environment rather than on this code,
        // and would fail the build for reasons no commit here can fix:
        //   GradleDependency        - dependency freshness; Dependabot's job
        //   OldTargetApi            - fires whenever Google ships a new SDK
        //   ObsoleteLintCustomCheck - Compose's bundled lint jar vs our Kotlin version
//   PropertyEscape          - flags local.properties, which Studio generates and
        //                             git ignores; not a file this project controls
        disable += setOf(
            "GradleDependency",
            "OldTargetApi",
            "ObsoleteLintCustomCheck",
            "PropertyEscape",
        )
    }
    // buildConfig is off by default from AGP 8.0 onwards; the update channel needs it.
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // The codebase compiles clean, so this costs nothing today and stops the
        // first warning from becoming the first of fifty.
        allWarningsAsErrors.set(true)
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

/**
 * Coverage, reported but never gated on.
 *
 * A coverage percentage is easy to move dishonestly, so it is published as
 * information rather than enforced as a threshold. There is no `verify` rule here on
 * purpose: gating would reward tests written to touch lines over tests that assert
 * something.
 *
 * Only the `debug` variant is measured, because `testDebugUnitTest` is the only suite
 * that runs. The exclusions below decide whether the number means anything at all:
 * measured across every class it would mostly describe Compose, which this project
 * verifies by reading rather than by running.
 */
kover {
    reports {
        filters {
            excludes {
                // Composables, wherever they live. Excluded by annotation rather than
                // by package so plain logic that happens to sit under `ui/`, the setup
                // state in particular, still counts.
                annotatedBy("androidx.compose.runtime.Composable")

                // Thin by design: view models wire flows together and hold no logic of
                // their own. Documented in CLAUDE.md as deliberately untested.
                classes("*ViewModel")

                // Colour and type tokens. Declarations, nothing to execute.
                packages("dev.danielkindl.ocho.ui.theme")

                // Android entry points, which a unit test cannot instantiate. The
                // `$*` forms matter: patterns are anchored at the start of the
                // fully-qualified name, so without them the lambdas Kotlin compiles
                // into nested classes stay in the count.
                classes(
                    "dev.danielkindl.ocho.MainActivity",
                    "dev.danielkindl.ocho.MainActivity\$*",
                    "dev.danielkindl.ocho.OchoApp",
                    "dev.danielkindl.ocho.OchoApp\$*",
                    "dev.danielkindl.ocho.BuildConfig",
                )

                // Generated code. Counting it would measure Dagger and the Compose
                // compiler rather than anything written here.
                annotatedBy("*Generated*")
                classes(
                    "*.Hilt_*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_HiltModules*",
                    "*_GeneratedInjector",
                    "*ComposableSingletons*",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                )
            }
        }

        // Off during `check`: the reports are produced by an explicit CI step so a
        // local `./gradlew check` stays as fast as it was.
        variant("debug") {
            xml { onCheck = false }
            html { onCheck = false }

            // Grouped by package rather than reported as one figure. A single number
            // here would be a blend of two things measured for different reasons, and
            // it invites the wrong reaction; split by package it is obvious which
            // packages the tests are actually meant to cover. The aggregate and the
            // per-file detail are both in the HTML report.
            log {
                onCheck = false
                header = "Line coverage by package (reported, never gated):"
                groupBy = GroupingEntityType.PACKAGE
                coverageUnits = CoverageUnit.LINE
                aggregationForGroup = AggregationType.COVERED_PERCENTAGE
            }
        }
    }
}
