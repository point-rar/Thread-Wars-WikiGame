plugins {
    id("java")
    kotlin("jvm") version "1.9.10"
}

group = "point.rar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(mapOf("path" to ":")))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    val ktor_version = "2.2.3"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    val slf4j_version = "2.0.6"
    implementation("org.slf4j:slf4j-api:$slf4j_version")
    implementation("org.slf4j:slf4j-simple:$slf4j_version")

    val resilience4jVersion = "2.0.0"
    implementation("io.github.resilience4j:resilience4j-kotlin:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-retry:${resilience4jVersion}")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}