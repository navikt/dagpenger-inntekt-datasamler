package no.nav.dagpenger.inntekt

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.dagpenger.datalaster.inntekt.InntektApiHttpClient
import no.nav.dagpenger.datalaster.inntekt.oidc.OidcClient
import no.nav.dagpenger.datalaster.inntekt.oidc.OidcToken
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class InntektApiHttpClientTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort())

    class DummyOidcClient : OidcClient {
        override fun oidcToken(): OidcToken =
            OidcToken(UUID.randomUUID().toString(), "openid", 3000)
    }

    @Test
    fun `fetch uklassifisert inntekt on 200 ok`() {

        val responseBodyJson = InntektApiHttpClientTest::class.java
            .getResource("/test-data/example-klassifisert-inntekt-payload.json").readText()

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v1/inntekt"))
                // .withHeader("Authorization", RegexPattern("Bearer\\s[\\d|a-f]{8}-([\\d|a-f]{4}-){3}[\\d|a-f]{12}"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson)
                )
        )

        val inntektApiClient = InntektApiHttpClient(
            wireMockRule.url(""),
            DummyOidcClient()
        )

        val inntektResponse =
            inntektApiClient.getInntekt(
                "",
                123,
                LocalDate.now())

        assertEquals("12345", inntektResponse.inntektsId)
    }
}