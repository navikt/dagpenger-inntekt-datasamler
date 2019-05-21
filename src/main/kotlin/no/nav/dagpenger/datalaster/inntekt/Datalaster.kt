package no.nav.dagpenger.datalaster.inntekt

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import org.apache.logging.log4j.LogManager
import java.util.Properties

class Datalaster(val config: Configuration, val inntektApiHttpClient: InntektApiClient) : River() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = config.application.httpPort ?: super.HTTP_PORT

    private val logger = LogManager.getLogger()

    companion object {
        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val VEDTAKID = "vedtakId"
        const val BEREGNINGSDATO = "beregningsDato"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(INNTEKT) },
            Predicate { _, packet -> !packet.hasField(MANUELT_GRUNNLAG) },
            Predicate { _, packet -> !packet.hasProblem() }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val aktørId = packet.getStringValue(AKTØRID)
        val vedtakId = packet.getIntValue(VEDTAKID)
        val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)

        when (val result: com.github.kittinunf.result.Result<Inntekt, InntektApiHttpClientException> =
            inntektApiHttpClient.getInntekt(aktørId, vedtakId, beregningsDato)) {
            is com.github.kittinunf.result.Result.Failure -> {
                logger.error("Failed to add inntekt", result.error)
                packet.addProblem(result.error.problem)
            }
            is com.github.kittinunf.result.Result.Success -> packet.putValue(
                INNTEKT,
                inntektJsonAdapter.toJsonValue(result.value)!!
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
    val datalaster = Datalaster(config, inntektApiHttpClient)
    datalaster.start()
}
