package no.nav.dagpenger.datalaster.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.net.URI
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class Datalaster(
    private val config: Configuration,
    private val inntektApiHttpClient: InntektApiClient,
    private val spesifisertInntektHttpClient: SpesifisertInntektHttpClient
) : River() {

    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = config.application.httpPort ?: super.HTTP_PORT

    companion object {
        const val INNTEKT = "inntektV1"
        const val SPESIFISERT_INNTEKT = "spesifisertInntektV1"
        const val AKTØRID = "aktørId"
        const val VEDTAKID = "vedtakId"
        const val BEREGNINGSDATO = "beregningsDato"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val INNTEKTS_ID = "inntektsId"
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(INNTEKT) },
            Predicate { _, packet -> !packet.hasField(SPESIFISERT_INNTEKT) },
            Predicate { _, packet -> !packet.hasField(MANUELT_GRUNNLAG) }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val aktørId = packet.getStringValue(AKTØRID)
        val vedtakId = packet.getIntValue(VEDTAKID)
        val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)
        val inntektsId = packet.getNullableStringValue(INNTEKTS_ID)

        try {
            val spesifisertInntekt =
                spesifisertInntektHttpClient.getSpesifisertInntekt(aktørId, vedtakId, beregningsDato)
            packet.putValue(SPESIFISERT_INNTEKT, spesifisertInntektJsonAdapter.toJsonValue(spesifisertInntekt)!!)
        } catch (e: Exception) {
            LOGGER.warn("Could not add spesifisert inntekt", e)
        }

        val inntekt = when (inntektsId) {
            is String -> inntektApiHttpClient.getInntektById(inntektsId)
            else -> inntektApiHttpClient.getInntekt(aktørId, vedtakId, beregningsDato)
        }

        packet.putValue(INNTEKT, inntektJsonAdapter.toJsonValue(inntekt)!!)

        return packet
    }

    override fun onFailure(packet: Packet, error: Throwable?): Packet {
        if (error is InntektApiHttpClientException) {
            LOGGER.error("Failed to add inntekt", error)
            packet.addProblem(error.problem)
        } else {
            packet.addProblem(
                Problem(
                    type = URI("urn:dp:error:datalaster"),
                    title = "Ukjent feil ved lasting av inntektdata"
                )
            )
        }

        return packet
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = config.application.bootstrapServersUrl,
            credential = KafkaCredential(config.application.username, config.application.password)
        )
    }
}

fun main(args: Array<String>) {
    val config = Configuration()
    val apiKeyVerifier = ApiKeyVerifier(config.application.inntektApiSecret)
    val apiKey = apiKeyVerifier.generate(config.application.inntektApiKey)
    val inntektApiHttpClient = InntektApiHttpClient(
        config.application.inntektApiUrl,
        apiKey
    )
    val spesifisertInntektHttpClient = SpesifisertInntektHttpClient(
        config.application.inntektApiUrl,
        apiKey
    )
    val datalaster = Datalaster(config, inntektApiHttpClient, spesifisertInntektHttpClient)
    datalaster.start()
}
