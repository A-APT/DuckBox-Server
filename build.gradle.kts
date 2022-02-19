import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val springBootVersion = "2.5.4"

    repositories {
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.4.21")
    }
}

plugins {
    java
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.jpa") version "1.3.61"
    kotlin("plugin.allopen") version "1.4.21"
}

noArg {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}

group = "org.duckbox"
version = "1.0-SNAPSHOT"

apply {
    plugin("kotlin-spring")
    plugin("org.springframework.boot")
    plugin("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("mysql:mysql-connector-java")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.12.2")

    // JWT
    implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.2")

    // Email
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Ethereum
    implementation("org.web3j:core:4.5.11")
    testImplementation("org.web3j:web3j-unit:4.5.11")

    // SMS (coolSMS)
    implementation("net.nurigo:javaSDK:2.2")

    // mongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")

}

tasks.test {
    useJUnit()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
