val scrimageVersion: String by settings


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "SemestralWork"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("scrimage", "com.sksamuel.scrimage:scrimage-core:$scrimageVersion")
        }
    }
}

