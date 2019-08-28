package no.nav.dagpenger.datalaster.inntekt

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.github.kittinunf.result.Result
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.moshiInstance
import java.net.URI
import java.time.LocalDate

class InntektApiHttpClient(
    private val inntektApiUrl: String,
    private val apiKey: String
) : InntektApiClient {

    private val jsonRequestRequestAdapter = moshiInstance.adapter(InntektRequest::class.java)
    private val problemAdapter = moshiInstance.adapter(Problem::class.java)!!

    override fun getInntekt(
        aktørId: String,
        vedtakId: Int,
        beregningsDato: LocalDate
    ): Inntekt {

        val url = "${inntektApiUrl}v1/inntekt"

        val requestBody = InntektRequest(
            aktørId,
            vedtakId,
            beregningsDato
        )
        val jsonBody = jsonRequestRequestAdapter.toJson(requestBody)

        val (_, response, result) = with(url.httpPost()) {
            header("Content-Type" to "application/json")
            header("X-API-KEY", apiKey)
            body(jsonBody)
            responseObject(moshiDeserializerOf(inntektJsonAdapter))
        }

        return if (result is Result.Failure) {
            val problem = runCatching {
                problemAdapter.fromJson(response.body().asString("application/json"))!!
            }.getOrDefault(
                Problem(
                    URI.create("urn:dp:error:inntektskomponenten"),
                    "Klarte ikke å hente inntekt"
                )
            )
            throw InntektApiHttpClientException(
                "Failed to fetch inntekt. Response message ${response.responseMessage}",
                problem
            )
        } else {
            result.get()
        }
    }

    override fun getInntektById(
        inntektsId: String
    ): Inntekt {
        val url = "${inntektApiUrl}v1/inntekt/$inntektsId"

        val (_, response, result) = with(url.httpGet()) {
            header("Content-Type" to "application/json")
            header("X-API-KEY", apiKey)
            responseObject(moshiDeserializerOf(inntektJsonAdapter))
        }

        return if (result is Result.Failure) {
            val problem = runCatching {
                problemAdapter.fromJson(response.body().asString("application/json"))!!
            }.getOrDefault(
                Problem(
                    URI.create("urn:dp:error:inntektskomponenten"),
                    "Klarte ikke å hente inntekt"
                )
            )
            throw InntektApiHttpClientException(
                "Failed to fetch inntekt. Response message ${response.responseMessage}",
                problem
            )
        } else {
            result.get()
        }
    }
}

data class InntektRequest(
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate
)

private fun String.toBearerToken() = "Bearer $this"
