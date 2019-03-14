package no.nav.dagpenger.datalaster.inntekt

data class Environment(
    val username: String = getEnvVar("SRVDP_DATALASTER_INNTEKT_USERNAME"),
    val password: String = getEnvVar("SRVDP_DATALASTER_INNTEKT_PASSWORD"),
    val oicdStsUrl: String = getEnvVar("OIDC_STS_ISSUERURL"),
    val inntektApiUrl: String = getEnvVar("DAGPENGER_INNTEKT_API_REST_URL"),
    val bootstrapServersUrl: String = getEnvVar(
        "KAFKA_BOOTSTRAP_SERVERS",
        "localhost:9092"
    ),
    val schemaRegistryUrl: String = getEnvVar(
        "KAFKA_SCHEMA_REGISTRY_URL",
        "http://localhost:8081"
    ),
    val httpPort: Int? = 8094
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")