package no.nav.dagpenger.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.codehaus.jackson.map.ObjectMapper
import org.json.JSONObject
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class Datalaster(val env: Environment) : Service() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT
    private val objectMapper = ObjectMapper()

    override fun setupStreams(): KafkaStreams {
        LOGGER.info { "Initiating start of $SERVICE_APP_ID" }
        return KafkaStreams(buildTopology(), getConfig())
    }

    internal fun buildTopology(): Topology {
        val builder = StreamsBuilder()

        val stream = builder.stream(
            dagpengerBehovTopic.name,
            Consumed.with(dagpengerBehovTopic.keySerde, dagpengerBehovTopic.valueSerde)
        )

        stream
            .peek { _, value -> LOGGER.info { "Received dagpenger behov $value" } }
            .mapValues { value: JSONObject -> DagpengerBehov(value) }
            .filter { _, dpBehov -> dpBehov.needInntekt() }
            .mapValues { value ->
                run {
                    val inntekt = fetchInntektData()
                    val jsonObject = value.jsonObject
                    jsonObject.put("inntekt", inntekt)
                    return@run jsonObject
                }
            }
            .to(dagpengerBehovTopic.name, Produced.with(dagpengerBehovTopic.keySerde, dagpengerBehovTopic.valueSerde))


        return builder.build()
    }

    private fun fetchInntektData(): Int {
        return 0
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

val dagpengerBehovTopic = Topic(
    name = "privat-dagpenger-behov-alpha",
    keySerde = Serdes.StringSerde(),
    valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
)
