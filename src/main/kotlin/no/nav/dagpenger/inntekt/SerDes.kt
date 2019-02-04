package no.nav.dagpenger.inntekt

import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.apache.logging.log4j.LogManager
import org.json.JSONObject

class JsonSerializer : Serializer<JSONObject> {
    override fun serialize(topic: String?, data: JSONObject?): ByteArray? {
        return data?.toString(0)?.toByteArray()
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

class JsonDeserializer : Deserializer<JSONObject> {
    private val LOGGER = LogManager.getLogger()

    override fun deserialize(topic: String?, data: ByteArray?): JSONObject? {
        return data?.let {
            val json = String(it)
            try {
                JSONObject(json)
            } catch (ex: Exception) {
                LOGGER.warn("'$json' is not valid json")
                null
            }
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}