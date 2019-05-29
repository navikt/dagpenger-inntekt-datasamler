package no.nav.dagpenger.inntekt

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import no.nav.dagpenger.datalaster.inntekt.InntektApiHttpClient
import no.nav.dagpenger.datalaster.inntekt.InntektApiHttpClientException
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InntektApiHttpClientTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort())

    @Test
    fun `fetch klassifisert inntekt on 200 ok`() {

        val responseBodyJson = InntektApiHttpClientTest::class.java
            .getResource("/test-data/example-klassifisert-inntekt-payload.json").readText()

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt"))
                .withHeader("X-API-KEY", EqualToPattern("api-key"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson)
                )
        )

        val inntektApiClient = InntektApiHttpClient(
            wireMockRule.url(""),
            "api-key"
        )

        val inntektResponse =
            inntektApiClient.getInntekt(
                "",
                123,
                LocalDate.now()
            )

        assertEquals("12345", inntektResponse.inntektsId)
    }

    @Test
    fun `fetch inntekt on 500 server error`() {

        val responseBodyJson = """

         {
            "type": "urn:dp:error:inntektskomponenten",
            "title": "Klarte ikke å hente inntekt for beregningen",
            "status": 500,
            "detail": "Innhenting av inntekt mot inntektskomponenten feilet."
         }

        """.trimIndent()
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt"))
                .withHeader("X-API-KEY", EqualToPattern("api-key"))
                .willReturn(
                    WireMock.serverError()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson)
                )
        )

        val inntektApiClient = InntektApiHttpClient(
            wireMockRule.url(""),
            "api-key"
        )

        val inntektApiHttpClientException = assertFailsWith<InntektApiHttpClientException> {
            inntektApiClient.getInntekt(
                "",
                123,
                LocalDate.now()
            )
        }

        val problem = inntektApiHttpClientException.problem
        assertEquals("urn:dp:error:inntektskomponenten", problem.type.toASCIIString())
        assertEquals("Klarte ikke å hente inntekt for beregningen", problem.title)
        assertEquals(500, problem.status)
        assertEquals("Innhenting av inntekt mot inntektskomponenten feilet.", problem.detail)
    }

    @Test
    fun `fetch inntekt on error and no body`() {

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt"))
                // .withHeader("Authorization", RegexPattern("Bearer\\s[\\d|a-f]{8}-([\\d|a-f]{4}-){3}[\\d|a-f]{12}"))
                .willReturn(
                    WireMock.serviceUnavailable()
                )
        )

        val inntektApiClient = InntektApiHttpClient(
            wireMockRule.url(""),
            "api-"
        )

        val inntektApiHttpClientException = assertFailsWith<InntektApiHttpClientException> {
            inntektApiClient.getInntekt(
                "",
                123,
                LocalDate.now()
            )
        }

        val problem = inntektApiHttpClientException.problem
        assertEquals("urn:dp:error:inntektskomponenten", problem.type.toASCIIString())
        assertEquals("Klarte ikke å hente inntekt", problem.title)
        assertEquals(500, problem.status)
    }
}