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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    val kotlinx_serialization_json_version = "1.4.1"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_json_version")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    val ktor_version = "2.2.3"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    val slf4j_version = "2.0.6"
    implementation("org.slf4j:slf4j-api:$slf4j_version")
    implementation("org.slf4j:slf4j-simple:$slf4j_version")

    val resilience4jVersion = "2.1.0"
    implementation("io.github.resilience4j:resilience4j-kotlin:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-retry:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-timelimiter:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${resilience4jVersion}")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.1.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")
}

tasks.test {
    useJUnitPlatform()
}
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview", "--add-modules", "jdk.incubator.concurrent"))
}
kotlin {
    jvmToolchain(19)
}