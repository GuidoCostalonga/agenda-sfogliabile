package it.guidocostalonga.agendasfogliabile.data

import it.guidocostalonga.agendasfogliabile.util.alleOre
import it.guidocostalonga.agendasfogliabile.util.dalleOre
import it.guidocostalonga.agendasfogliabile.util.maiuscola
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Un calendario sincronizzato sul telefono (per esempio un calendario Google). */
data class Calendario(
    val id: Long,
    val nome: String,
    val account: String,
    val tipoAccount: String,
    val colore: Int,
    val scrivibile: Boolean,
)

/** Una singola occorrenza di un evento del calendario. */
data class Impegno(
    val eventoId: Long,
    val calendarioId: Long,
    val titolo: String,
    val luogo: String,
    val inizio: Long,
    val fine: Long,
    val tuttoIlGiorno: Boolean,
    val colore: Int,
    val scrivibile: Boolean,
    val ricorrente: Boolean,
) {
    // Gli eventi di un giorno intero sono salvati a mezzanotte del tempo universale.
    private fun zonaDi(zona: ZoneId): ZoneId = if (tuttoIlGiorno) ZoneOffset.UTC else zona

    fun giornoInizio(zona: ZoneId): LocalDate =
        Instant.ofEpochMilli(inizio).atZone(zonaDi(zona)).toLocalDate()

    /** Ultimo giorno occupato dall'impegno, compreso. */
    fun giornoFine(zona: ZoneId): LocalDate {
        val primo = giornoInizio(zona)
        if (fine <= inizio) return primo
        val termine = Instant.ofEpochMilli(fine).atZone(zonaDi(zona))
        val ultimo = if (termine.toLocalTime() == LocalTime.MIDNIGHT) {
            termine.toLocalDate().minusDays(1)
        } else {
            termine.toLocalDate()
        }
        return if (ultimo.isBefore(primo)) primo else ultimo
    }

    fun cadeIl(giorno: LocalDate, zona: ZoneId): Boolean =
        !giorno.isBefore(giornoInizio(zona)) && !giorno.isAfter(giornoFine(zona))

    fun oraInizio(zona: ZoneId): LocalTime = Instant.ofEpochMilli(inizio).atZone(zona).toLocalTime()

    fun oraFine(zona: ZoneId): LocalTime = Instant.ofEpochMilli(fine).atZone(zona).toLocalTime()

    /** Vero se l'impegno ha un orario e comincia proprio in quel giorno. */
    fun iniziaIl(giorno: LocalDate, zona: ZoneId): Boolean =
        !tuttoIlGiorno && giornoInizio(zona) == giorno

    fun descriviOrario(giorno: LocalDate, zona: ZoneId): String {
        if (tuttoIlGiorno) return "Tutto il giorno"
        val primo = giornoInizio(zona)
        val ultimo = giornoFine(zona)
        val da = oraInizio(zona)
        val a = oraFine(zona)
        return when {
            primo == giorno && ultimo == giorno ->
                if (fine <= inizio) alleOre(da).maiuscola() else "${dalleOre(da)} ${alleOre(a)}".maiuscola()
            primo == giorno -> dalleOre(da).maiuscola()
            ultimo == giorno -> "Fino ${alleOre(a)}"
            else -> "Tutto il giorno"
        }
    }
}

/** Dati di un impegno mentre lo si scrive o lo si modifica. */
data class Bozza(
    val eventoId: Long? = null,
    val calendarioId: Long = -1,
    val titolo: String = "",
    val luogo: String = "",
    val descrizione: String = "",
    val tuttoIlGiorno: Boolean = false,
    val inizio: LocalDateTime,
    /** Per gli impegni di un giorno intero conta solo la data, compresa. */
    val fine: LocalDateTime,
) {
    /** Sposta l'inizio mantenendo la stessa durata. */
    fun conInizio(nuovo: LocalDateTime): Bozza =
        copy(inizio = nuovo, fine = nuovo.plus(Duration.between(inizio, fine)))
}
