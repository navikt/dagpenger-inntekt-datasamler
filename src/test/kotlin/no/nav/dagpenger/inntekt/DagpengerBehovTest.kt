package no.nav.dagpenger.inntekt

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DagpengerBehovTest {

    @Test
    fun ` Should not need inntekt if task 'hentInntekt' is missing  `() {
        val behov = DagpengerBehov(JSONObject())
        assertFalse(behov.needInntekt())
    }

    @Test
    fun ` Should not need inntekt if task 'hentInntekt' is missing and inntekt already set `() {
        val json = JSONObject()
        json.put("inntekt", 0)

        val behov = DagpengerBehov(json)
        assertFalse(behov.needInntekt())
    }

    @Test
    fun ` Should not need inntekt if task 'hentInntekt' is present and inntekt already set `() {
        val json = JSONObject()
        json.put("inntekt", 0)
        json.put("tasks", listOf("hentInntekt"))

        val behov = DagpengerBehov(json)
        assertFalse(behov.needInntekt())
    }

    @Test
    fun ` Should fetch inntekt if task 'hentInntekt' is present and inntekt property is missing `() {
        val json = JSONObject()

        json.put("tasks", listOf("hentInntekt"))

        val behov = DagpengerBehov(json)
        assertTrue(behov.needInntekt())
    }
}