plugins {
    kotlin("jvm") version "2.0.20"
    alias(libs.plugins.protobuf)
}

group = "cz.cvut.fel.dsva"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.scrimage)
    implementation(libs.protoc)
    implementation(libs.protobufKotlin)
    implementation(libs.grpc)
    implementation(libs.grpcKotlin)
    implementation(libs.coroutines)
    implementation(libs.jackson)
    implementation(libs.jacksonKotlin)
    implementation(libs.jacksonDateTime)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
    compilerOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.grpcJavaGen.get().toString()
        }
        create("grpckt") {
            artifact = "${libs.grpcKotlinGen.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}