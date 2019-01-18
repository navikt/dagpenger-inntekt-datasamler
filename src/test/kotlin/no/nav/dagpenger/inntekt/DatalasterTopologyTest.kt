package no.nav.dagpenger.inntekt

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.dagpenger.events.avro.Vilkår
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.Random
import java.util.UUID
import kotlin.test.assertTrue

class DatalasterTopologyTest {

    companion object {
        val mockSchemaRegistryClient = MockSchemaRegistryClient().apply {
            register(Topics.VILKÅR_EVENT.name, Vilkår.getClassSchema())
        }
        val vilkårSerDes = SpecificAvroSerde<Vilkår>(mockSchemaRegistryClient).apply {
            configure(mapOf(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "bogus"), false)
        }
        val factory = ConsumerRecordFactory<String, Vilkår>(
                Topics.VILKÅR_EVENT.name,
                Serdes.String().serializer(),
                vilkårSerDes.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    @Test
    fun ` Should add inntekt to vilkår `() {
        val datalaster = Datalaster(Environment(
                username = "bogus",
                password = "bogus"
        ))

        val innVilkår = Vilkår.newBuilder().apply {
            aktorId = Random().nextInt().toString()
            id = UUID.randomUUID().toString()
            vedtaksId = Random().nextInt().toString()
        }.build()

        TopologyTestDriver(datalaster.buildTopology(mockSchemaRegistryClient), config).use { topologyTestDriver ->
            val inputRecord = factory.create(innVilkår)
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(Topics.VILKÅR_EVENT.name, Serdes.String().deserializer(), vilkårSerDes.deserializer())
            assertTrue("Inntektsdata should have beed added") { ut.value().getInntekter() != null }
        }
    }
}