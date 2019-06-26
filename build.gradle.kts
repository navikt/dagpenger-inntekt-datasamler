import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version "1.3.21"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

buildscript {
    repositories {
        mavenCentral()
    }
}

apply {
    plugin("com.diffplug.gradle.spotless")
}

repositories {
    mavenCentral()
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kittinunf/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Multi-Release"] = "true" // https://github.com/johnrengelman/shadow/issues/449
    }
}

val kafkaVersion = "2.0.1"
val kotlinLoggingVersion = "1.6.22"
val log4j2Version = "2.11.1"
val jupiterVersion = "5.3.2"
val confluentVersion = "5.0.2"
val prometheusVersion = "0.6.0"
val orgJsonVersion = "20180813"
val ktorVersion = "1.2.0"
val fuelVersion = "2.0.1"
val moshiVersion = "1.8.0"
val ktorMoshiVersion = "1.0.1"
val mockkVersion = "1.9.3"
val konfigVersion = "1.6.10.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    implementation("com.github.navikt:dagpenger-streams:2019.06.21-11.13.27b0917e56b9")
    implementation("com.github.navikt:dagpenger-events:2019.06.12-14.01.4b1e1a663635")
    implementation("com.github.navikt.dp-biblioteker:ktor-utils:2019.05.21-09.57.669ffe8e266f")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_log4j2:$prometheusVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    implementation("com.ryanharter.ktor:ktor-moshi:$ktorMoshiVersion")

    implementation("com.natpryce:konfig:$konfigVersion")

    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-moshi:$fuelVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("com.vlkan.log4j2:log4j2-logstash-layout-fatjar:0.15")
    implementation("org.json:json:$orgJsonVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$jupiterVersion")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("no.nav:kafka-embedded-env:2.0.2")
    testImplementation("com.github.tomakehurst:wiremock:2.19.0")
    testImplementation("junit:junit:4.12")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

application {
    applicationName = "dp-datalaster-inntekt"
    mainClassName = "no.nav.dagpenger.datalaster.inntekt.DatalasterKt"
}

spotless {
    kotlin {
        ktlint("0.31.0")
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint("0.31.0")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
