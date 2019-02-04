package no.nav.dagpenger.inntekt

import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertTrue

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

    @Test
    fun ` Should add inntekt to dagpenger behov `() {
        val datalaster = Datalaster(
            Environment(
                username = "bogus",
                password = "bogus"
            )
        )

        val jsonObject = JSONObject()
        jsonObject.put("tasks", listOf("hentInntekt"))

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(jsonObject)
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                dagpengerBehovTopic.name,
                dagpengerBehovTopic.keySerde.deserializer(),
                dagpengerBehovTopic.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertEquals(ut.value().get("inntekt"), 0)
        }
    }

    @Test
    fun ` Should  not manipulate other data than add inntekt to dagpenger behov `() {
        val datalaster = Datalaster(
            Environment(
                username = "bogus",
                password = "bogus"
            )
        )

        val jsonObject = JSONObject()
        jsonObject.put("tasks", listOf("hentInntekt"))
        jsonObject.put("otherData", "data")

        TopologyTestDriver(datalaster.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(jsonObject)
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                dagpengerBehovTopic.name,
                dagpengerBehovTopic.keySerde.deserializer(),
                dagpengerBehovTopic.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertEquals(ut.value().get("inntekt"), 0)
            assertEquals(ut.value().get("otherData"), "data")
        }
    }
}