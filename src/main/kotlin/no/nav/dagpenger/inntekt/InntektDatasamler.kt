/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package no.nav.dagpenger.inntekt

import no.nav.dagpenger.streams.Service

class InntektDatasamler : Service() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"

    override fun setupStreams(): org.apache.kafka.streams.KafkaStreams {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
