package no.nav.dagpenger.inntekt

import com.github.kittinunf.result.Result
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.datalaster.inntekt.Configuration
import no.nav.dagpenger.datalaster.inntekt.Datalaster

import no.nav.dagpenger.datalaster.inntekt.InntektApiClient
import no.nav.dagpenger.datalaster.inntekt.InntektApiHttpClientException
import no.nav.dagpenger.datalaster.inntekt.inntektJsonAdapter
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Properties

class DatalasterTopologyTest {

    companion object {

        val factory = ConsumerRecordFactory<String, Packet>(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    class DummyInntektApiClient : InntektApiClient {
        override fun getInntekt(
            aktørId: String,
            vedtakId: Int,
            beregningsDato: LocalDate
        ): Result<Inntekt, InntektApiHttpClientException> {
            return Result.of { Inntekt("12345", emptyList()) }
        }
    }

    @Test
    fun `Should add inntekt to packet`() {
        val datalaster = Datalaster(
            Configuration(),
            DummyInntektApiClient()
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "otherField": "should be unchanged"
            }
        """.trimIndent()

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(packetJson))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertTrue(ut.value().hasField("inntektV1"))
            assertEquals(
                "12345",
                ut.value().getObjectValue("inntektV1") { serialized ->
                    checkNotNull(
                        inntektJsonAdapter.fromJsonValue(serialized)
                    )
                }.inntektsId
            )
            assertEquals("12345", ut.value().getStringValue("aktørId"))
            assertEquals(123, ut.value().getIntValue("vedtakId"))
            assertEquals(LocalDate.of(2019, 1, 25), ut.value().getLocalDate("beregningsDato"))
            assertEquals("should be unchanged", ut.value().getStringValue("otherField"))
        }
    }

    @Test
    fun `Should ignore packet with inntekt `() {
        val datalaster = Datalaster(
            Configuration(),
            DummyInntektApiClient()
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "inntektV1": "something"
            }
        """.trimIndent()

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(packetJson))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertNull(ut)
        }
    }

    @Test
    fun `Should ignore packet with manuelt grunnlag `() {
        val datalaster = Datalaster(
            Configuration(),
            DummyInntektApiClient()
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "manueltGrunnlag": 50000
            }
        """.trimIndent()

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(packetJson))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertNull(ut)
        }
    }

    @Test
    fun `Should add problem to packet if error when fetching inntekt occurs `() {
        val mockInntektApiClient: InntektApiClient = mockk()
        every { mockInntektApiClient.getInntekt(any(), any(), any()) } returns Result.error(
            InntektApiHttpClientException(
                "",
                Problem(title = "failed")
            )
        )

        val datalaster = Datalaster(
            Configuration(),
            mockInntektApiClient
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25
            }
        """.trimIndent()

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(packetJson))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertNotNull(ut)
            assertTrue { ut.value().hasProblem() }
        }
    }

    @Test
    fun `Should ignore packets with problem `() {
        val mockInntektApiClient: InntektApiClient = mockk()

        val datalaster = Datalaster(
            Configuration(),
            mockInntektApiClient
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "system_problem":
                    {
                    "type":"about:blank",
                    "title":"failed",
                    "status":500,
                    "instance":"about:blank"
                    }
            }
        """.trimIndent()

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(packetJson))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertNull(ut)
        }
    }
}