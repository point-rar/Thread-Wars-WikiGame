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

    val kotlinVersion = "1.9.10"
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.1.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}