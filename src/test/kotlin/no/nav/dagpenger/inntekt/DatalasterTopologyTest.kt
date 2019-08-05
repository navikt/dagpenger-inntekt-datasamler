package no.nav.dagpenger.inntekt

import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.datalaster.inntekt.Configuration
import no.nav.dagpenger.datalaster.inntekt.Datalaster
import no.nav.dagpenger.datalaster.inntekt.InntektApiClient
import no.nav.dagpenger.datalaster.inntekt.InntektApiHttpClientException
import no.nav.dagpenger.datalaster.inntekt.SpesifisertInntektHttpClient
import no.nav.dagpenger.datalaster.inntekt.inntektJsonAdapter
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Aktør
import no.nav.dagpenger.events.inntekt.v1.AktørType
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektId
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
        ) = Inntekt("12345", emptyList(), sisteAvsluttendeKalenderMåned = YearMonth.now())
    }

    private val emptySpesifisertInntekt = SpesifisertInntekt(
        inntektId = InntektId("01DHGJ2NDVV2TDSPT8K9FXMAN7"),
        avvik = emptyList(),
        posteringer = emptyList(),
        ident = Aktør(AktørType.AKTOER_ID, "111"),
        manueltRedigert = false,
        timestamp = LocalDateTime.of(2019, 5, 3, 1, 1)
    )

    @Test
    fun `Should add spesifisert inntekt to packet`() {
        val spesifisertInntektHttpClientMock: SpesifisertInntektHttpClient = mockk()

        every {
            spesifisertInntektHttpClientMock.getSpesifisertInntekt(any(), any(), any())
        } returns emptySpesifisertInntekt

        val datalaster = Datalaster(
            Configuration(),
            DummyInntektApiClient(),
            spesifisertInntektHttpClientMock
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
    fun `Should not add problem to packet if error when fetching spesifisert inntekt occurs `() {
        val spesifisertInntektHttpClientMock: SpesifisertInntektHttpClient = mockk()

        every {
            spesifisertInntektHttpClientMock.getSpesifisertInntekt(any(), any(), any())
        } throws InntektApiHttpClientException("", Problem(title = "failed"))

        val datalaster = Datalaster(
            Configuration(),
            DummyInntektApiClient(),
            spesifisertInntektHttpClientMock
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
            assertFalse { ut.value().hasProblem() }
        }
    }

    @Test
    fun `Should ignore packet with spesifisert inntekt `() {
        val datalaster = Datalaster(
            Configuration(),
            DummyInntektApiClient(),
            mockk(relaxed = true)
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "spesifisertInntektV1": "something"
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
    fun `Should add klassifisert inntekt to packet`() {
        val datalaster = Datalaster(
            Configuration(),
            DatalasterTopologyTest.DummyInntektApiClient(),
            mockk(relaxed = true)
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
    fun `Should add problem to packet if error when fetching klassifisert inntekt occurs `() {
        val mockInntektApiClient: InntektApiClient = mockk()
        every {
            mockInntektApiClient.getInntekt(any(), any(), any())
        } throws InntektApiHttpClientException("", Problem(title = "failed"))

        val datalaster = Datalaster(
            Configuration(),
            mockInntektApiClient,
            mockk(relaxed = true)
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
    fun `Should ignore packet with klassifisert inntekt `() {
        val datalaster = Datalaster(
            Configuration(),
            DatalasterTopologyTest.DummyInntektApiClient(),
            mockk(relaxed = true)
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
            DatalasterTopologyTest.DummyInntektApiClient(),
            mockk()
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
    fun `Should ignore packets with problem `() {

        val datalaster = Datalaster(
            Configuration(),
            mockk(),
            mockk()
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

    @Test
    fun `Should add problem on unexpected failure`() {

        val datalaster = Datalaster(
            Configuration(),
            DatalasterTopologyTest.DummyInntektApiClient(),
            mockk(relaxed = true)
        )

        val packet = Packet()

        packet.putValue("aktørId", "123")
        packet.putValue("vedtakId", 123)
        packet.putValue("beregningsDato", "ERROR")

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(packet)
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assert(ut.value().hasProblem())
            assertEquals(URI("urn:dp:error:datalaster"), ut.value().getProblem()!!.type)
        }
    }
}