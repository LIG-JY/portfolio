import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.20" apply false
    kotlin("plugin.spring") version "1.9.20" apply false
}

subprojects {
    apply {
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
    }

    group = "com.g2s"
    version = "0.0.1-SNAPSHOT"

    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = false
    jar.enabled = true

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter")
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "com.vaadin.external.google", module = "android-json")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_17.toString()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

project(":web") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    jar.enabled = false
    bootJar.enabled = true

    dependencies {
        "implementation"(project(":domain"))
        "runtimeOnly"(project(":infra"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
    }
}

project(":infra") {
    dependencies {
        "implementation"(project(":domain"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.3")
        "implementation"("io.github.binance:binance-connector-java:3.2.0")
        "implementation"("io.github.binance:binance-futures-connector-java:3.0.3")
        "implementation"("org.springframework.data:spring-data-mongodb:4.1.8")
        "implementation"("org.mongodb:mongodb-driver-kotlin-sync:4.11.0")
    }
}

project(":domain") {
    dependencies {
    }
}
