package no.nav.dagpenger.inntekt

import org.json.JSONObject

data class DagpengerBehov(val jsonObject: JSONObject) {

    companion object {
        val TASKS = "tasks"
        val TASK_HENT_INNTEKT = "hentInntekt"
        val INNTEKT = "inntekt"
    }

    fun needInntekt(): Boolean = missingInntekt() && hasHentInntektTask()

    private fun missingInntekt() = !jsonObject.has(INNTEKT)

    private fun hasHentInntektTask(): Boolean {
        if (jsonObject.has(TASKS)) {
            val tasks = jsonObject.getJSONArray(TASKS)
            for (task in tasks) {
                if (task.toString() == TASK_HENT_INNTEKT) {
                    return true
                }
            }
        }
        return false
    }

    fun addInntekt(inntekt: Inntekt) {
        jsonObject.put(INNTEKT, inntekt.build())
    }
}

data class Inntekt(val inntektsId: String, val inntekt: Int) {

    companion object {
        val INNTEKTSID = "inntektsId"
        val INNTEKT = "inntekt"
    }

    fun build(): JSONObject {
        return JSONObject()
            .put(INNTEKTSID, inntektsId)
            .put(INNTEKT, inntekt)
    }
}