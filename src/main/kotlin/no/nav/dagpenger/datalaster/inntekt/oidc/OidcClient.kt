package no.nav.dagpenger.datalaster.inntekt.oidc

interface OidcClient {
    fun oidcToken(): OidcToken
}
