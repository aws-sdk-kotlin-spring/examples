import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.springframework.boot.gradle.dsl.SpringBootExtension

plugins {
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.dependencyManagement) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinPluginSpring) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.testLogger) apply false
}

subprojects {
    // The generated `libs` accessor only resolves on the root project; bind the catalog
    // under another name so nested blocks do not hit the subproject extension lookup.
    val catalog = rootProject.extensions.getByType<LibrariesForLibs>()

    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "com.adarshr.test-logger")

    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        // Local awskotlin builds: run `./gradlew publishToMavenLocal` in the awskotlin repo.
        mavenLocal()
        mavenCentral()
        // awskotlin snapshots published by CI; GitHub Packages requires auth even for public packages.
        // Locally set gpr.user/gpr.key (a PAT with read:packages) in ~/.gradle/gradle.properties;
        // CI uses GITHUB_ACTOR/GITHUB_TOKEN.
        maven {
            name = "github-awskotlin-spring"
            url = uri("https://maven.pkg.github.com/aws-sdk-kotlin-spring/*")
            mavenContent { snapshotsOnly() }
            credentials {
                username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
                password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    dependencies {
        "implementation"(catalog.bundles.core)
        "implementation"(catalog.bundles.kotlinSupport)
        "implementation"(catalog.bundles.okhttp)

        "testImplementation"(catalog.bundles.testcore)
        "testImplementation"(catalog.bundles.testcontainers)
        "testImplementation"(catalog.bundles.kotest)
        "testImplementation"(catalog.bundles.mockk)
        "testImplementation"(catalog.bundles.awaitility)
        "testImplementation"(catalog.bundles.kotlintest)
        "testRuntimeOnly"(catalog.junitPlatformLauncher)
    }

    configure<KotlinJvmProjectExtension> {
        jvmToolchain(catalog.versions.java.get().toInt())
        compilerOptions {
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
            progressiveMode.set(true)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
            )
            optIn.addAll(
                "kotlin.RequiresOptIn",
                "kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
        }
    }

    // ktlint 1.8.0 (applied by kotlinter) requires kotlin-compiler-embeddable 2.2.x.
    // Pin the compiler artifacts for the lint classpath only; compilation itself uses
    // the Kotlin version from the version catalog.
    configurations.named("ktlint") {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" &&
                requested.name in
                setOf(
                    "kotlin-compiler-embeddable",
                    "kotlin-daemon-embeddable",
                    "kotlin-script-runtime",
                    "kotlin-build-tools-api",
                )
            ) {
                useVersion("2.2.21")
                because("ktlint 1.8.0 is not compatible with kotlin-compiler-embeddable 2.4.x")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        minHeapSize = "128m"
        maxHeapSize = "2048m"
    }

    tasks.withType<LintTask> {
        exclude { it.file.path.contains("build/generated") }
    }

    tasks.withType<FormatTask> {
        exclude { it.file.path.contains("build/generated") }
    }

    configure<KotlinterExtension> {
        ignoreLintFailures = false
        reporters = arrayOf("checkstyle", "plain")
    }

    configure<TestLoggerExtension> {
        theme = MOCHA
        showExceptions = true
        showStackTraces = true
        showFullStackTraces = false
        showCauses = true
        slowThreshold = 2000
        showSummary = true
        showPassed = true
        showSkipped = true
        showFailed = true
        showStandardStreams = false
        logLevel = LogLevel.LIFECYCLE
    }

    configure<SpringBootExtension> {
        buildInfo()
    }
}