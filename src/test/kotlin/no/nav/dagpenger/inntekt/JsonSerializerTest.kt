package no.nav.dagpenger.inntekt

import no.nav.dagpenger.datalaster.inntekt.JsonDeserializer
import no.nav.dagpenger.datalaster.inntekt.JsonSerializer
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class JsonSerializerTest {

    @Test
    fun `serialize dagpenger behov events given null input`() {
        val serialized = JsonSerializer().serialize("topic", null)
        assertNull(serialized)
    }

    @Test
    fun `serialize dagpenger behov events given input`() {
        val serialized = JsonSerializer().serialize("topic", JSONObject(json))
        assertNotNull(serialized)
    }

    @Test
    fun `Deserialize dagpenger behov events given null input`() {
        val serialized = JsonDeserializer().deserialize("topic", null)
        assertNull(serialized)
    }

    @Test
    fun `Deserialize dagpenger behov events given bad input`() {
        val serialized = JsonDeserializer().deserialize("topic", "bad input".toByteArray())
        assertNull(serialized)
    }

    @Test
    fun `Deserialize dagpenger behov events given input`() {
        val serialized = JsonDeserializer().deserialize("topic", json.toByteArray())
        assertNotNull(serialized)
    }

    private val json = """
        {
            "tasks": ["hentInntekt"],
            "inntekt": 0
        }
    """.trimIndent()
}