plugins {
    id("java")
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

group = "point.rar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))

    val kotlinx_serialization_json_version = "1.4.1"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_json_version")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}