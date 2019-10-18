package no.nav.dagpenger.datalaster.inntekt

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig

fun setupUnleash(profile: String): DefaultUnleash {
    val appName = "dp-datalaster-inntekt"
    val unleashconfig = UnleashConfig.builder()
        .appName(appName)
        .instanceId(appName + profile)
        .unleashAPI("http://unleash.nais.adeo.no/api/")
        .build()

    return DefaultUnleash(unleashconfig, ByProfileStrategy(profile))
}

class ByProfileStrategy(private val profile: String) : Strategy {
    override fun isEnabled(parameters: MutableMap<String, String>?): Boolean {
        if (parameters == null) {
            return false
        }

        val profileParameter = parameters["profile"] ?: return false

        val allClusters = profileParameter.split(",")
        return allClusters.contains(profile)
    }

    override fun getName() = "byProfile"
}
