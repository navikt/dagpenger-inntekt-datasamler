package no.nav.dagpenger.inntekt

import org.json.JSONObject

data class DagpengerBehov(val jsonObject: JSONObject) {

    fun needInntekt(): Boolean = missingInntekt() && hasHentInntektTask()

    private fun missingInntekt() = !jsonObject.has("inntekt")

    private fun hasHentInntektTask(): Boolean {
        if (jsonObject.has("tasks")) {
            val tasks = jsonObject.getJSONArray("tasks")
            for (task in tasks) {
                if (task.toString() == "hentInntekt") {
                    return true
                }
            }
        }
        return false
    }
}
