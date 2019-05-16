package no.nav.dagpenger.datalaster.inntekt

import no.nav.dagpenger.events.Problem
import java.lang.RuntimeException
import java.time.LocalDate

interface InntektApiClient {
    fun getInntekt(aktørId: String, vedtakId: Int, beregningsDato: LocalDate): Inntekt
}

class InntektApiHttpClientException(
    override val message: String,
    val problem: Problem
) : RuntimeException(message)