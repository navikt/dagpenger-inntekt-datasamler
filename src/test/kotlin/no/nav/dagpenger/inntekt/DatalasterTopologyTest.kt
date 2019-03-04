package no.nav.dagpenger.inntekt

import no.nav.dagpenger.datalaster.inntekt.Datalaster
import no.nav.dagpenger.datalaster.inntekt.Environment
import no.nav.dagpenger.datalaster.inntekt.Inntekt
import no.nav.dagpenger.datalaster.inntekt.InntektApiClient
import no.nav.dagpenger.datalaster.inntekt.SubsumsjonsBehov
import no.nav.dagpenger.datalaster.inntekt.dagpengerBehovTopic
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Properties

class DatalasterTopologyTest {

    companion object {

        val factory = ConsumerRecordFactory<String, JSONObject>(
            dagpengerBehovTopic.name,
            dagpengerBehovTopic.keySerde.serializer(),
            dagpengerBehovTopic.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    class DummyInntektApiClient : InntektApiClient {
        override fun getInntekt(aktørId: String, vedtakId: Int, beregningsDato: LocalDate): Inntekt {
            return Inntekt("12345", emptyList())
        }
    }

    @Test
    fun ` Should add inntekt to dagpenger behov `() {
        val datalaster = Datalaster(
            Environment(
                "user",
                "pass",
                "",
                ""
            ),
            DummyInntektApiClient()
        )

        val behov = SubsumsjonsBehov.Builder()
            .aktørId("12345")
            .vedtakId(123)
            .beregningsDato(LocalDate.now())
            .task(listOf("hentInntekt"))
            .build()

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behov.jsonObject)
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                dagpengerBehovTopic.name,
                dagpengerBehovTopic.keySerde.deserializer(),
                dagpengerBehovTopic.valueSerde.deserializer()
            )

            val utBehov = SubsumsjonsBehov(ut.value())
            assert(utBehov.hasInntekt())
        }
    }
}