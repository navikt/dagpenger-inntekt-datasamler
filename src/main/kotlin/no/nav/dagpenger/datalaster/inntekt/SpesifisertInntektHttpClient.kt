package no.nav.dagpenger.datalaster.inntekt

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.github.kittinunf.result.Result
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.events.moshiInstance
import java.net.URI
import java.time.LocalDate

class SpesifisertInntektHttpClient(private val inntektApiUrl: String, private val apiKey: String) {

    private val jsonRequestRequestAdapter = moshiInstance.adapter(SpesifisertInntektRequest::class.java)
    private val problemAdapter = moshiInstance.adapter(Problem::class.java)!!

    fun getSpesifisertInntekt(
        aktørId: String,
        vedtakId: Int,
        beregningsDato: LocalDate
    ): SpesifisertInntekt {

        val url = "${inntektApiUrl}v1/inntekt/spesifisert"

        val requestBody = SpesifisertInntektRequest(
            aktørId,
            vedtakId,
            beregningsDato
        )
        val jsonBody = jsonRequestRequestAdapter.toJson(requestBody)

        val (_, response, result) = with(url.httpPost()) {
            header("Content-Type" to "application/json")
            header("X-API-KEY", apiKey)
            body(jsonBody)
            responseObject(moshiDeserializerOf(spesifisertInntektJsonAdapter))
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

private data class SpesifisertInntektRequest(
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate
)

private fun String.toBearerToken() = "Bearer $this"
