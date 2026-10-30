plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }

    versionCatalogs {
        create("libs") {
            version("kotlin", "2.0.21")
            plugin("kotlin-serialization","org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")

            library("kotlin-coroutines","org.jetbrains.kotlinx", "kotlinx-coroutines-core").version("1.7.3")

            version("compose-compiler", "1.3.2")

            version("compose", "1.3.0")
            library("compose-foundation", "androidx.compose.foundation", "foundation").versionRef("compose")
            library("compose-ui", "androidx.compose.ui", "ui").versionRef("compose")
            library("compose-ui-util", "androidx.compose.ui", "ui-util").versionRef("compose")
            library("compose-ripple", "androidx.compose.material", "material-ripple").versionRef("compose")

            library("compose-shimmer", "com.valentinilk.shimmer", "compose-shimmer").version("1.0.3")

            library("compose-activity", "androidx.activity", "activity-compose").version("1.7.0")

            library("compose-coil", "io.coil-kt", "coil-compose").version("2.2.2")

            version("room", "2.5.2")
            library("room", "androidx.room", "room-ktx").versionRef("room")
            library("room-compiler", "androidx.room", "room-compiler").versionRef("room")

            version("media3", "1.1.1")
            library("exoplayer", "androidx.media3", "media3-exoplayer").versionRef("media3")
            library("media3-ui", "androidx.media3", "media3-ui").versionRef("media3")

            version("ktor", "2.3.12")
            library("ktor-client-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-client-cio", "io.ktor", "ktor-client-okhttp").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-client-encoding", "io.ktor", "ktor-client-encoding").versionRef("ktor")
            library("ktor-client-serialization", "io.ktor", "ktor-client-serialization").versionRef("ktor")
            library("ktor-client-websockets", "io.ktor", "ktor-client-websockets").versionRef("ktor")
            library("ktor-serialization-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")

            library("brotli", "org.brotli", "dec").version("0.1.2")

            version("youtubedl", "0.18.1")
            library("youtubedl", "io.github.junkfood02.youtubedl-android", "library").versionRef("youtubedl")
            library("youtubedl-ffmpeg", "io.github.junkfood02.youtubedl-android", "ffmpeg").versionRef("youtubedl")

            library("palette", "androidx.palette", "palette").version("1.0.0")

            library("desugaring", "com.android.tools", "desugar_jdk_libs").version("1.1.5")

            library("junit", "junit", "junit").version("4.13.2")
            library("mockito-core", "org.mockito", "mockito-core").version("5.3.1")
            library("mockito-kotlin", "org.mockito.kotlin", "mockito-kotlin").version("5.0.0")
            version("work", "2.8.1")
            library("work-runtime", "androidx.work", "work-runtime-ktx").versionRef("work")
        }
    }
}

rootProject.name = "SynapMusic"
include(":app")
include(":compose-routing")
include(":compose-reordering")
include(":compose-persist")
include(":pulse-engine")
include(":ktor-client-brotli")
include(":lyric-flow")
