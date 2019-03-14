package no.nav.dagpenger.datalaster.inntekt

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.github.kittinunf.result.Result
import no.nav.dagpenger.datalaster.inntekt.oidc.OidcClient
import java.time.LocalDate

class InntektApiHttpClient(
    private val inntektApiUrl: String,
    private val oidcClient: OidcClient
) : InntektApiClient {

    override fun getInntekt(
        aktørId: String,
        vedtakId: Int,
        beregningsDato: LocalDate
    ): Inntekt {

        val url = "${inntektApiUrl}v1/inntekt"

        val jsonRequestRequestAdapter = moshiInstance.adapter(InntektRequest::class.java)

        val requestBody = InntektRequest(
            aktørId,
            vedtakId,
            beregningsDato
        )
        val jsonBody = jsonRequestRequestAdapter.toJson(requestBody)

        val (_, response, result) = with(url.httpPost()) {
            header("Content-Type" to "application/json")
            body(jsonBody)
            responseObject(moshiDeserializerOf(inntektJsonAdapter))
        }
        return when (result) {
            is Result.Failure -> throw InntektApiHttpClientException(
                "Failed to fetch inntekt. Response message ${response.responseMessage}",
                result.getException()
            )
            is Result.Success -> result.get()
        }
    }
}

data class InntektRequest(
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate
)

fun String.toBearerToken() = "Bearer $this"

class InntektApiHttpClientException(
    override val message: String,
    override val cause: Throwable
) : RuntimeException(message, cause)