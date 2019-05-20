package no.nav.dagpenger.datalaster.inntekt

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMapError
import com.squareup.moshi.JsonAdapter
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.moshiInstance
import java.net.URI
import java.time.LocalDate

class InntektApiHttpClient(
    private val inntektApiUrl: String
) : InntektApiClient {

    private val inntektJsonAdapter: JsonAdapter<Inntekt> = moshiInstance.adapter(Inntekt::class.java)

    private val jsonRequestRequestAdapter = moshiInstance.adapter(InntektRequest::class.java)
    private val problemAdapter = moshiInstance.adapter(Problem::class.java)!!

    override fun getInntekt(
        aktørId: String,
        vedtakId: Int,
        beregningsDato: LocalDate
    ): Result<Inntekt, InntektApiHttpClientException> {

        val url = "${inntektApiUrl}v1/inntekt"

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

        return result.flatMapError {

            val problem = runCatching {
                problemAdapter.fromJson(it.response.body().asString("application/json"))!!
            }.getOrDefault(
                Problem(
                    URI.create("urn:dp:error:inntektskomponenten"),
                    "Klarte ikke å hente inntekt"
                )
            )
            Result.error(
                InntektApiHttpClientException(
                    "Failed to fetch inntekt. Response message ${response.responseMessage}. Error message: ${it.message}",
                    problem
                )
            )
        }
    }
}

data class InntektRequest(
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate
)

fun String.toBearerToken() = "Bearer $this"
