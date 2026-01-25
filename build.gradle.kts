import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "dev.gitty"
version = "0.0.1-SNAPSHOT"
description = "Gitty - инструмент для анализа коммитов"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
	maven(url = "https://jitpack.io")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

	implementation("org.flywaydb:flyway-core")

	configurations.all {
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
		exclude(group = "ch.qos.logback", module = "logback-classic")
	}

	implementation("org.apache.logging.log4j:log4j-core:2.25.3")
	implementation("org.apache.logging.log4j:log4j-api:2.25.1")
	implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.1")
	implementation("org.apache.logging.log4j:log4j-jul:2.25.1")
	implementation("org.apache.logging.log4j:log4j-layout-template-json:2.25.1")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:6.1.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<Jar> {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}