import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

buildscript {
    repositories {
        jcenter()
    }
}

apply {
    plugin(Spotless.spotless)
}

repositories {
    mavenCentral()
    jcenter()
    maven("http://packages.confluent.io/maven/")
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

val orgJsonVersion = "20180813"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Dagpenger.Streams)
    implementation(Dagpenger.Events)
    implementation(Dagpenger.Biblioteker.ktorUtils)

    implementation(Kafka.clients)
    implementation(Kafka.streams)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.log4j2)

    implementation(Ktor.serverNetty)

    implementation(Moshi.moshi)
    implementation(Moshi.moshiAdapters)
    implementation(Moshi.moshiKotlin)
    implementation(Moshi.moshiKtor)

    implementation(Konfig.konfig)

    implementation(Fuel.fuel)
    implementation(Fuel.fuelMoshi)

    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.Logstash.logstashLayout)

    implementation("no.finn.unleash:unleash-client-java:3.2.9")

    testImplementation(kotlin("test"))

    testImplementation(Junit5.api)
    testRuntimeOnly(Junit5.engine)
    testRuntimeOnly(Junit5.vintageEngine)

    testImplementation(Kafka.streamTestUtils)
    testImplementation(KafkaEmbedded.env)
    testImplementation(Wiremock.standalone)
    testImplementation(Mockk.mockk)
}

application {
    applicationName = "dp-datalaster-inntekt"

    mainClassName = "no.nav.dagpenger.datalaster.inntekt.DatalasterKt"
}

spotless {
    kotlin {
        ktlint(Klint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint(Klint.version)
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

tasks.withType<Wrapper> {
    gradleVersion = "5.5"
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}
