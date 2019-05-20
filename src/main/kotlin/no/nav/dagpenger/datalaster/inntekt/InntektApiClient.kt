package no.nav.dagpenger.datalaster.inntekt

import com.github.kittinunf.result.Result
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import java.lang.RuntimeException
import java.time.LocalDate

interface InntektApiClient {
    fun getInntekt(aktørId: String, vedtakId: Int, beregningsDato: LocalDate): Result<Inntekt, InntektApiHttpClientException>
}

class InntektApiHttpClientException(
    override val message: String,
    val problem: Problem
) : RuntimeException(message)