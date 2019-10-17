package no.nav.dagpenger.datalaster.inntekt

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig

fun setupUnleash(profile: Profile, cluster: String): DefaultUnleash {
    val appName = "dp-datalaster-inntekt"
    val unleashconfig = UnleashConfig.builder()
        .appName(appName)
        .instanceId(appName + profile.toString())
        .unleashAPI("http://unleash.nais.adeo.no/api/")
        .build()

    return DefaultUnleash(unleashconfig, ByClusterStrategy(cluster))
}

class ByClusterStrategy(private val cluster: String) : Strategy {
    override fun isEnabled(parameters: MutableMap<String, String>?): Boolean {
        if (parameters == null) {
            return false
        }

        val clusterParameter = parameters["cluster"] ?: return false

        val allClusters = clusterParameter.split(",")
        return allClusters.contains(cluster)
    }

    override fun getName() = "byCluster"
}
