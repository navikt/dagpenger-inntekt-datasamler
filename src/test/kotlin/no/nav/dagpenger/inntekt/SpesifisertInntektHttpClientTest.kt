package no.nav.dagpenger.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import com.github.tomakehurst.wiremock.matching.EqualToPattern
import no.nav.dagpenger.datalaster.inntekt.InntektApiHttpClientException
import no.nav.dagpenger.datalaster.inntekt.SpesifisertInntektHttpClient

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpesifisertInntektHttpClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `fetch spesifisert inntekt on 200 ok`() {

        val responseBodyJson = SpesifisertInntektHttpClientTest::class.java
            .getResource("/test-data/example-spesifisert-inntekt-payload.json").readText()

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt/spesifisert"))
                .withHeader("X-API-KEY", EqualToPattern("api-key"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson)
                )
        )

        val spesifisertInntektHttpClient = SpesifisertInntektHttpClient(
            server.url(""),
            "api-key"
        )

        val spesifisertInntekt =
            spesifisertInntektHttpClient.getSpesifisertInntekt(
                "",
                123,
                LocalDate.now()
            )

        assertEquals("01D8G6FS9QGRT3JKBTA5KEE64C", spesifisertInntekt.inntektId.id)
        assertEquals(4, spesifisertInntekt.posteringer.size)
    }

    @Test
    fun `fetch spesifisert inntekt fails on 500 server error`() {

        val responseBodyJson = """

         {
            "type": "urn:dp:error:inntektskomponenten",
            "title": "Klarte ikke å hente inntekt for beregningen",
            "status": 500,
            "detail": "Innhenting av inntekt mot inntektskomponenten feilet."
         }

        """.trimIndent()
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt/spesifisert"))
                .withHeader("X-API-KEY", EqualToPattern("api-key"))
                .willReturn(
                    WireMock.serverError()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson)
                )
        )

        val spesifisertInntektHttpClient = SpesifisertInntektHttpClient(
            server.url(""),
            "api-key"
        )

        val inntektApiHttpClientException = assertFailsWith<InntektApiHttpClientException> {
            spesifisertInntektHttpClient.getSpesifisertInntekt(
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
    fun `fetch spesifisert inntekt fails on error and no body`() {

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt/spesifisert"))
                .willReturn(
                    WireMock.serviceUnavailable()
                )
        )

        val spesifisertInntektHttpClient = SpesifisertInntektHttpClient(
            server.url(""),
            "api-"
        )

        val inntektApiHttpClientException = assertFailsWith<InntektApiHttpClientException> {
            spesifisertInntektHttpClient.getSpesifisertInntekt(
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