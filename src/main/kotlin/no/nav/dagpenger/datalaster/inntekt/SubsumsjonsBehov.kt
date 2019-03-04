package no.nav.dagpenger.datalaster.inntekt

import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class SubsumsjonsBehov(val jsonObject: JSONObject) {

    companion object {
        val TASKS = "tasks"
        val TASK_HENT_INNTEKT = "hentInntekt"
        val INNTEKT = "inntektV1"
        val AKTØRID = "aktørId"
        val VEDTAKID = "vedtakId"
        val BEREGNINGSDATD = "beregningsDato"
        val jsonAdapterInntekt = moshiInstance.adapter(Inntekt::class.java)
    }

    val jsonAdapterInntekt = moshiInstance.adapter(Inntekt::class.java)

    fun needInntekt(): Boolean = !hasInntekt() && hasHentInntektTask()

    fun hasInntekt() = jsonObject.has(INNTEKT)

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
        val json = jsonAdapterInntekt.toJson(inntekt)
        jsonObject.put(INNTEKT, JSONObject(json))
    }

    fun getAktørId(): String {
        return jsonObject.get(AKTØRID) as String
    }

    fun getVedtakId(): Int {
        return jsonObject.get(VEDTAKID) as Int
    }

    fun getBeregningsDato(): LocalDate {
        return LocalDate.parse(jsonObject.get(BEREGNINGSDATD) as String)
    }

    class Builder {

        val jsonObject = JSONObject()

        fun inntekt(inntekt: Inntekt): Builder {
            val json = jsonAdapterInntekt.toJson(inntekt)
            jsonObject.put(
                INNTEKT,
                JSONObject(json)
            )
            return this
        }

        fun task(tasks: List<String>): Builder {
            jsonObject.put(TASKS, tasks)
            return this
        }

        fun aktørId(aktørId: String): Builder {
            jsonObject.put(AKTØRID, aktørId)
            return this
        }

        fun vedtakId(vedtakId: Int): Builder {
            jsonObject.put(VEDTAKID, vedtakId)
            return this
        }

        fun beregningsDato(beregningsDato: LocalDate): Builder {
            jsonObject.put(BEREGNINGSDATD, beregningsDato)
            return this
        }

        fun build(): SubsumsjonsBehov =
            SubsumsjonsBehov(jsonObject)
    }
}

data class Inntekt(
    val inntektsId: String,
    val inntektsListe: List<KlassifisertInntektMåned>
)

data class KlassifisertInntektMåned(
    val årMåned: YearMonth,
    val klassifiserteInntekter: List<KlassifisertInntekt>
)

data class KlassifisertInntekt(
    val beløp: BigDecimal,
    val inntektKlasse: InntektKlasse
)

enum class InntektKlasse {
    ARBEIDSINNTEKT,
    DAGPENGER,
    DAGPENGER_FANGST_FISKE,
    SYKEPENGER_FANGST_FISKE,
    FANGST_FISKE,
    SYKEPENGER,
    TILTAKSLØNN
}