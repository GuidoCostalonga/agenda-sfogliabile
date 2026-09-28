package it.guidocostalonga.agendasfogliabile.data

import android.content.Context

class Preferenze(context: Context) {
    private val archivio = context.applicationContext
        .getSharedPreferences("agenda", Context.MODE_PRIVATE)

    /** Calendari da mostrare; null significa «tutti». */
    var calendariScelti: Set<Long>?
        get() = if (!archivio.contains(CALENDARI)) {
            null
        } else {
            archivio.getStringSet(CALENDARI, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        }
        set(valore) {
            val modifica = archivio.edit()
            if (valore == null) {
                modifica.remove(CALENDARI)
            } else {
                modifica.putStringSet(CALENDARI, valore.map { it.toString() }.toSet())
            }
            modifica.apply()
        }

    var vistaSettimana: Boolean
        get() = archivio.getBoolean(VISTA_SETTIMANA, false)
        set(valore) = archivio.edit().putBoolean(VISTA_SETTIMANA, valore).apply()

    var ultimoCalendario: Long
        get() = archivio.getLong(ULTIMO_CALENDARIO, -1L)
        set(valore) = archivio.edit().putLong(ULTIMO_CALENDARIO, valore).apply()

    private companion object {
        const val CALENDARI = "calendari_scelti"
        const val VISTA_SETTIMANA = "vista_settimana"
        const val ULTIMO_CALENDARIO = "ultimo_calendario"
    }
}
