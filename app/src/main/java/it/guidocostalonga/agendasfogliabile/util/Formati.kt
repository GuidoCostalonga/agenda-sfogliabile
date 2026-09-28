package it.guidocostalonga.agendasfogliabile.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

val ITALIANO: Locale = Locale.ITALIAN

private val FORMATO_ORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", ITALIANO)

fun String.maiuscola(): String = replaceFirstChar { it.titlecase(ITALIANO) }

fun LocalDate.nomeGiorno(): String = dayOfWeek.getDisplayName(TextStyle.FULL, ITALIANO).maiuscola()

fun LocalDate.nomeGiornoBreve(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, ITALIANO).maiuscola()

fun LocalDate.nomeMese(): String = month.getDisplayName(TextStyle.FULL, ITALIANO)

/** «28 settembre 2026» */
fun LocalDate.dataEstesa(): String = "$dayOfMonth ${nomeMese()} $year"

/** «Lunedì 28 settembre 2026» */
fun LocalDate.dataConGiorno(): String = "${nomeGiorno()} ${dataEstesa()}"

/** «Settembre 2026» */
fun YearMonth.titolo(): String = "${month.getDisplayName(TextStyle.FULL, ITALIANO).maiuscola()} $year"

fun LocalTime.testo(): String = format(FORMATO_ORA)

fun lunediDi(giorno: LocalDate): LocalDate = giorno.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.numeroSettimana(): Int = get(WeekFields.ISO.weekOfWeekBasedYear())

fun giorniTra(da: LocalDate, a: LocalDate): Int = ChronoUnit.DAYS.between(da, a).toInt()

fun settimaneTra(da: LocalDate, a: LocalDate): Int = ChronoUnit.WEEKS.between(lunediDi(da), lunediDi(a)).toInt()

// Numeri che si leggono con una vocale iniziale: uno, otto, undici.
private fun iniziaPerVocale(numero: Int) = numero == 1 || numero == 8 || numero == 11

/** «Dal 28» oppure «Dall'1» */
private fun dalGiorno(n: Int) = if (iniziaPerVocale(n)) "Dall'$n" else "Dal $n"

/** «al 4» oppure «all'8» */
private fun alGiorno(n: Int) = if (iniziaPerVocale(n)) "all'$n" else "al $n"

/** «Dal 28 settembre al 4 ottobre 2026» */
fun intervalloSettimana(lunedi: LocalDate): String {
    val domenica = lunedi.plusDays(6)
    val fine = "${alGiorno(domenica.dayOfMonth)} ${domenica.nomeMese()} ${domenica.year}"
    return when {
        lunedi.month == domenica.month -> "${dalGiorno(lunedi.dayOfMonth)} $fine"
        lunedi.year == domenica.year -> "${dalGiorno(lunedi.dayOfMonth)} ${lunedi.nomeMese()} $fine"
        else -> "${dalGiorno(lunedi.dayOfMonth)} ${lunedi.nomeMese()} ${lunedi.year} $fine"
    }
}

/** «dalle 09:30» oppure «dall'01:30» */
fun dalleOre(ora: LocalTime): String = if (ora.hour == 1) "dall'${ora.testo()}" else "dalle ${ora.testo()}"

/** «alle 10:30» oppure «all'01:30» */
fun alleOre(ora: LocalTime): String = if (ora.hour == 1) "all'${ora.testo()}" else "alle ${ora.testo()}"
