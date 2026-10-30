buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21")
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            if (project.findProperty("enableComposeCompilerReports") == "true") {
                val metricsDir = "${project.layout.buildDirectory.asFile.get().absolutePath}/compose_metrics"
                freeCompilerArgs.addAll(
                    listOf(
                        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir",
                        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir"
                    )
                )
            }
        }
    }
}
