val scrimageVersion: String by settings
val protobufPluginVersion: String by settings
val protobufVersion: String by settings
val grpcVersion: String by settings
val grpcKotlinStubVersion: String by settings
val coroutinesVersion: String by settings
val jacksonVersion: String by settings
val kotlinLoggingVersion: String by settings
val slf4jVersion: String by settings
val logbackVersion: String by settings
val shadowVersion: String by settings
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "SemestralWork"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("scrimage", "com.sksamuel.scrimage:scrimage-core:$scrimageVersion")
            plugin("protobuf", "com.google.protobuf").version(protobufPluginVersion)
            library("protoc", "com.google.protobuf:protoc:$protobufVersion")
            library("protobufKotlin", "com.google.protobuf:protobuf-kotlin:$protobufVersion")
            library("grpc", "io.grpc:grpc-protobuf:$grpcVersion")
            library("grpcJavaGen", "io.grpc:protoc-gen-grpc-java:$grpcVersion")
            library("grpcKotlin", "io.grpc:grpc-kotlin-stub:$grpcKotlinStubVersion")
            library("grpcKotlinGen", "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinStubVersion")
            library("coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            library("jacksonKotlin", "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            library("jacksonDateTime", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
            library("grpcServer", "io.grpc:grpc-netty:$grpcVersion")
            plugin("fatjar", "com.gradleup.shadow").version(shadowVersion)
        }
        create("loggingLibs") {
            library("kotlinLogging", "io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
            library("slf4j", "org.slf4j:slf4j-api:$slf4jVersion")
            library("logbackCore", "ch.qos.logback:logback-core:$logbackVersion")
            library("logbackClassic", "ch.qos.logback:logback-classic:$logbackVersion")
        }
    }
}

