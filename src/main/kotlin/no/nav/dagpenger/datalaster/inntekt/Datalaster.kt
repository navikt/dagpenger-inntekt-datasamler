package no.nav.dagpenger.datalaster.inntekt

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.oidc.StsOidcClient
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.util.Properties

class Datalaster(val env: Environment, val inntektApiHttpClient: InntektApiClient) : River() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT

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
            Predicate { _, packet -> !packet.hasField(MANUELT_GRUNNLAG) }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val aktørId = packet.getStringValue(AKTØRID)
        val vedtakId = packet.getIntValue(VEDTAKID)
        val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)
        val inntekt = inntektApiHttpClient.getInntekt(aktørId, vedtakId, beregningsDato)
        packet.putValue(INNTEKT, inntektJsonAdapter.toJsonValue(inntekt)!!)
        return packet
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password)
        )
    }
}

fun main(args: Array<String>) {
    val env = Environment()
    val inntektApiHttpClient = InntektApiHttpClient(
        env.inntektApiUrl,
        StsOidcClient(env.oicdStsUrl, env.username, env.password)
    )
    val datalaster = Datalaster(env, inntektApiHttpClient)
    datalaster.start()
}
