package no.nav.dagpenger.datalaster.inntekt

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.Topics

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "srvdp.datalaster.inntekt.username" to "username",
        "srvdp.datalaster.inntekt.password" to "password",
        "dp.inntekt.api.url" to "http://localhost/",
        "kafka.bootstrap.servers" to "localhost:9092",
        "dp.inntekt.api.key" to "dp-datalaster-inntekt",
        "dp.inntekt.api.secret" to "secret",
        "unleash.url" to "http://localhost",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name

    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "dp.inntekt.api.url" to "http://dp-inntekt-api//",
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "unleash.url" to "https://unleash.nais.preprod.local/api/",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "dp.inntekt.api.url" to "http://dp-inntekt-api/",
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
        "unleash.url" to "https://unleash.nais.adeo.no/api/",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name
    )
)

data class Configuration(
    val application: Application = Application()
) {

    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val username: String = config()[Key("srvdp.datalaster.inntekt.username", stringType)],
        val password: String = config()[Key("srvdp.datalaster.inntekt.password", stringType)],
        val inntektApiUrl: String = config()[Key("dp.inntekt.api.url", stringType)],
        val bootstrapServersUrl: String = config()[Key("kafka.bootstrap.servers", stringType)],
        val inntektApiKey: String = config()[Key("dp.inntekt.api.key", stringType)],
        val inntektApiSecret: String = config()[Key("dp.inntekt.api.secret", stringType)],
        val unleashUrl: String = config()[Key("unleash.url", stringType)],
        val httpPort: Int? = 8094,
        val behovTopic: Topic<String, Packet> = Topics.DAGPENGER_BEHOV_PACKET_EVENT.copy(
            name = config()[Key("behov.topic", stringType)]
        )
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}
