package no.nav.dagpenger.datalaster.inntekt

import java.time.LocalDate

interface InntektApiClient {
    fun getInntekt(aktørId: String, vedtakId: Int, beregningsDato: LocalDate): Inntekt
}