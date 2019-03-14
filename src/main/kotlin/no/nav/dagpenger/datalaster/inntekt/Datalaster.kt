package no.nav.dagpenger.datalaster.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.datalaster.inntekt.oidc.StsOidcClient
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Packet
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.KStream
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class Datalaster(val env: Environment, val inntektApiHttpClient: InntektApiClient) : River() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val inntektApiHttpClient = InntektApiHttpClient(
                env.inntektApiUrl,
                StsOidcClient(env.oicdStsUrl, env.username, env.password)
            )
            val datalaster = Datalaster(env, inntektApiHttpClient)
            datalaster.start()
        }

        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val VEDTAKID = "vedtakId"
        const val BEREGNINGSDATO = "beregningsDato"
    }

    override fun river(stream: KStream<String, Packet>): KStream<String, Packet> = stream
        .peek { _, value -> LOGGER.info { "Received dagpenger packet $value" } }
        .filter { _, packet -> !packet.hasField(INNTEKT) }
        .filter { _, packet -> packet.hasFields(AKTØRID, VEDTAKID, BEREGNINGSDATO) }
        .mapValues { packet ->
            run {
                val aktørId = packet.getStringValue(AKTØRID) ?: throw RuntimeException("Missing aktørId")
                val vedtakId = packet.getIntValue(VEDTAKID) ?: throw RuntimeException("Missing aktørId")
                val beregningsDato = packet.getLocalDate(BEREGNINGSDATO) ?: throw RuntimeException("Missing aktørId")

                val inntekt = inntektApiHttpClient.getInntekt(aktørId, vedtakId, beregningsDato)
                packet.putValue(INNTEKT, inntekt, inntektJsonAdapter::toJson)
                return@run packet
            }
        }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password)
        )
    }
}

