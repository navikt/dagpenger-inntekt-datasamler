package no.nav.dagpenger.inntekt

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Inntekt
import no.nav.dagpenger.events.avro.Inntektsdata
import no.nav.dagpenger.events.avro.Inntektstype
import no.nav.dagpenger.events.avro.Måned
import no.nav.dagpenger.events.avro.Vilkår
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class Datalaster(val env: Environment) : Service() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT

    override fun setupStreams(): KafkaStreams {
        LOGGER.info { "Initiating start of $SERVICE_APP_ID" }
        return KafkaStreams(buildTopology(), getConfig())
    }

    internal fun buildTopology(schemaRegistryClient: SchemaRegistryClient? = null): Topology {
        val builder = StreamsBuilder()

        val topic = schemaRegistryClient?.let { client ->
            Topics.VILKÅR_EVENT.copy(valueSerde = SpecificAvroSerde<Vilkår>(client))
        } ?: Topics.VILKÅR_EVENT

        val vilkårTopology = builder.consumeTopic(topic, env.schemaRegistryUrl)
            .filter { _, vilkår -> vilkår.getInntekter() == null }
            .peek { _, vilkår -> LOGGER.info { "Handling vilkår with id ${vilkår.getId()}" } }
            .mapValues { _, vilkår ->
                // fetch "inntekt" object from dp-inntekt-api
                // add "inntekt" to vilkår
                val inntektsdata = fetchInntektData(vilkår.getAktorId())
                val vilkårBuilder = Vilkår.newBuilder(vilkår).setInntekter(inntektsdata)
                return@mapValues vilkårBuilder.build()
            }.toTopic(Topics.VILKÅR_EVENT, env.schemaRegistryUrl)

        return builder.build()
    }

    private fun fetchInntektData(aktorId: String): Inntektsdata {
        return Inntektsdata.newBuilder().apply {
            fangstOgFisk = false
            verneplikt = false
            fraDato = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            inntekter = listOf(Inntekt.newBuilder().apply {
                virksomhet = "12345678"
                måned = Måned.april
                År = 2018
                beløp = BigDecimal(32000.50)
                inntektstype = Inntektstype.lønnsinntekt
                beskrivelse = "Beskrivelse"
                tilleggsinformasjon = "Tillegginfo"
            }.build())
        }.build()
    }

    override fun getConfig(): Properties {
        return streamConfig(
                appId = SERVICE_APP_ID,
                bootStapServerUrl = env.bootstrapServersUrl,
                credential = KafkaCredential(env.username, env.password)
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val datalaster = Datalaster(Environment())
            datalaster.start()
        }
    }
}
