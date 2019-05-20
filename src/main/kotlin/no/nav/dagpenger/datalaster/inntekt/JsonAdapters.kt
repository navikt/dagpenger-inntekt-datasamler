package no.nav.dagpenger.datalaster.inntekt

import com.squareup.moshi.JsonAdapter
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.moshiInstance

val inntektJsonAdapter: JsonAdapter<Inntekt> = moshiInstance.adapter(Inntekt::class.java)