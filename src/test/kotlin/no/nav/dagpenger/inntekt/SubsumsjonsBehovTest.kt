package no.nav.dagpenger.inntekt

import no.nav.dagpenger.datalaster.inntekt.JsonDeserializer
import no.nav.dagpenger.datalaster.inntekt.SubsumsjonsBehov
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class SubsumsjonsBehovTest {

    fun jsonToBehov(json: String): SubsumsjonsBehov =
        SubsumsjonsBehov(
            JsonDeserializer().deserialize(
                "",
                json.toByteArray()
            ) ?: JSONObject()
        )

    val emptyjsonBehov = "{}"

    val jsonBehovMedInntekt = """
        {
            "inntektV1": {
                "inntektsId": "12345",
                "inntektsListe": [
                    {
                        "årMåned": "2018-03",
                        "klassifiserteInntekter": [
                            {
                                "beløp": "25000",
                                "inntektKlasse": "ARBEIDSINNTEKT"
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

    val jsonBehovMedHentInntektTask = """
            {
                "tasks": ["hentInntekt"]
            }
            """.trimIndent()

    val jsonBehovMedInntektOgHentinntektTask = """
        {
            "inntektV1": {
                "inntektsId": "12345",
                "inntektsListe": [
                    {
                        "årMåned": "2018-03",
                        "klassifiserteInntekter": [
                            {
                                "beløp": "25000",
                                "inntektKlasse": "ARBEIDSINNTEKT"
                            }
                        ]
                    }
                ]
            },
             "tasks": ["hentInntekt"]
        }
        """.trimIndent()

    @Test
    fun ` Should not need inntekt if task 'hentInntekt' is missing  `() {
        assertFalse(jsonToBehov(emptyjsonBehov).needInntekt())
    }

    @Test
    fun ` Should not need inntekt if task 'hentInntekt' is missing and inntekt already set `() {

        assertFalse(jsonToBehov(jsonBehovMedInntekt).needInntekt())
    }

    @Test
    fun ` Should not need inntekt if task 'hentInntekt' is present and inntekt already set `() {
        assertFalse(jsonToBehov(jsonBehovMedInntektOgHentinntektTask).needInntekt())
    }

    @Test
    fun ` Should fetch inntekt if task 'hentInntekt' is present and inntekt property is missing `() {
        assert(jsonToBehov(jsonBehovMedHentInntektTask).needInntekt())
    }
}