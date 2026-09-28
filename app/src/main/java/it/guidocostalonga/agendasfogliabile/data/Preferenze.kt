package it.guidocostalonga.agendasfogliabile.data

import android.content.Context

class Preferenze(context: Context) {
    private val archivio = context.applicationContext
        .getSharedPreferences("agenda", Context.MODE_PRIVATE)

    /** Da dove leggere gli impegni: calendario del telefono oppure accesso diretto a Google. */
    var fonte: String
        get() = archivio.getString(FONTE, FONTE_TELEFONO) ?: FONTE_TELEFONO
        set(valore) = archivio.edit().putString(FONTE, valore).apply()

    /** Account usato per l'accesso diretto a Google. */
    var emailGoogle: String?
        get() = archivio.getString(EMAIL_GOOGLE, null)
        set(valore) = archivio.edit().putString(EMAIL_GOOGLE, valore).apply()

    // Ogni fonte ha la sua scelta di calendari.
    private val chiaveCalendari get() = if (fonte == FONTE_GOOGLE) "calendari_scelti_google" else "calendari_scelti"
    private val chiaveUltimo get() = if (fonte == FONTE_GOOGLE) "ultimo_calendario_google" else "ultimo_calendario_testo"

    /** Calendari da mostrare; null significa «tutti». */
    var calendariScelti: Set<String>?
        get() = if (!archivio.contains(chiaveCalendari)) {
            null
        } else {
            archivio.getStringSet(chiaveCalendari, emptySet()).orEmpty().toSet()
        }
        set(valore) {
            val modifica = archivio.edit()
            if (valore == null) modifica.remove(chiaveCalendari) else modifica.putStringSet(chiaveCalendari, valore)
            modifica.apply()
        }

    var vistaSettimana: Boolean
        get() = archivio.getBoolean(VISTA_SETTIMANA, false)
        set(valore) = archivio.edit().putBoolean(VISTA_SETTIMANA, valore).apply()

    var ultimoCalendario: String
        get() = archivio.getString(chiaveUltimo, "") ?: ""
        set(valore) = archivio.edit().putString(chiaveUltimo, valore).apply()

    companion object {
        const val FONTE_TELEFONO = "telefono"
        const val FONTE_GOOGLE = "google"
        private const val FONTE = "fonte"
        private const val EMAIL_GOOGLE = "email_google"
        private const val VISTA_SETTIMANA = "vista_settimana"
    }
}
