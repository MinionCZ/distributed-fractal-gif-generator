plugins {
    kotlin("jvm") version "2.0.20"
}

group = "cz.cvut.fel.dsva"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.scrimage)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}