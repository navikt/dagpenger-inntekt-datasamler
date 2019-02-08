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
import org.json.JSONObject
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class Datalaster(val env: Environment) : Service() {
    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"
    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT

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
            .mapValues { behov ->
                run {
                    val inntekt = fetchInntektData()
                    behov.addInntekt(inntekt)
                    return@run behov
                }
            }
            .mapValues { behov -> behov.jsonObject }
            .to(dagpengerBehovTopic.name, Produced.with(dagpengerBehovTopic.keySerde, dagpengerBehovTopic.valueSerde))

        return builder.build()
    }

    private fun fetchInntektData(): Inntekt {
        return Inntekt("id123", 0)
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
