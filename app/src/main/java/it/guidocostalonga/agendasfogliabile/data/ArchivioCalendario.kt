package it.guidocostalonga.agendasfogliabile.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Legge e scrive gli impegni nel calendario del telefono.
 * Android sincronizza questo calendario con Google Calendar in automatico.
 */
class ArchivioCalendario(context: Context) {
    private val context: Context = context.applicationContext
    private val resolver: ContentResolver get() = context.contentResolver

    fun puoLeggere(): Boolean = concesso(Manifest.permission.READ_CALENDAR)

    fun puoScrivere(): Boolean = concesso(Manifest.permission.WRITE_CALENDAR)

    private fun concesso(permesso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permesso) == PackageManager.PERMISSION_GRANTED

    fun calendari(): List<Calendario> {
        if (!puoLeggere()) return emptyList()
        val proiezione = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val ordine = "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, " +
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        val elenco = mutableListOf<Calendario>()
        resolver.query(CalendarContract.Calendars.CONTENT_URI, proiezione, null, null, ordine)?.use { c ->
            while (c.moveToNext()) {
                elenco += Calendario(
                    id = c.getLong(0),
                    nome = c.getString(1) ?: "",
                    account = c.getString(2) ?: "",
                    colore = c.getInt(3),
                    scrivibile = c.getInt(4) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                )
            }
        }
        return elenco
    }

    /**
     * Impegni che toccano i giorni da [dal] ad [al] compresi.
     * [scelti] null significa tutti i calendari; [filtro] cerca in titolo, luogo e descrizione.
     */
    fun impegni(
        dal: LocalDate,
        al: LocalDate,
        scelti: Set<Long>?,
        filtro: String? = null,
        limite: Int = Int.MAX_VALUE,
    ): List<Impegno> {
        if (!puoLeggere()) return emptyList()
        if (scelti != null && scelti.isEmpty()) return emptyList()
        val zona = ZoneId.systemDefault()
        // Margine di un giorno per parte: gli eventi di un giorno intero sono in tempo universale.
        val inizio = dal.minusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()
        val fine = al.plusDays(2).atStartOfDay(zona).toInstant().toEpochMilli()
        val costruttore = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(costruttore, inizio)
        ContentUris.appendId(costruttore, fine)

        val condizioni = mutableListOf<String>()
        val argomenti = mutableListOf<String>()
        if (scelti != null) {
            condizioni += "${CalendarContract.Instances.CALENDAR_ID} IN (${scelti.joinToString(",")})"
        }
        if (!filtro.isNullOrBlank()) {
            condizioni += "(${CalendarContract.Instances.TITLE} LIKE ? OR " +
                "${CalendarContract.Instances.EVENT_LOCATION} LIKE ? OR " +
                "${CalendarContract.Instances.DESCRIPTION} LIKE ?)"
            val cercato = "%${filtro.trim()}%"
            repeat(3) { argomenti += cercato }
        }

        val proiezione = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Instances.RRULE,
            CalendarContract.Instances.RDATE,
        )
        val risultato = mutableListOf<Impegno>()
        resolver.query(
            costruttore.build(),
            proiezione,
            if (condizioni.isEmpty()) null else condizioni.joinToString(" AND "),
            if (argomenti.isEmpty()) null else argomenti.toTypedArray(),
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val impegno = Impegno(
                    eventoId = c.getLong(0),
                    calendarioId = c.getLong(1),
                    titolo = c.getString(2)?.takeIf { it.isNotBlank() } ?: "(senza titolo)",
                    luogo = c.getString(3) ?: "",
                    inizio = c.getLong(4),
                    fine = c.getLong(5),
                    tuttoIlGiorno = c.getInt(6) == 1,
                    colore = c.getInt(7),
                    scrivibile = c.getInt(8) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                    ricorrente = !c.getString(9).isNullOrEmpty() || !c.getString(10).isNullOrEmpty(),
                )
                if (!impegno.giornoFine(zona).isBefore(dal) && !impegno.giornoInizio(zona).isAfter(al)) {
                    risultato += impegno
                    if (risultato.size >= limite) break
                }
            }
        }
        return risultato.sortedWith(
            compareBy<Impegno>({ it.giornoInizio(zona) }, { !it.tuttoIlGiorno }, { it.inizio }),
        )
    }

    /** Prepara la bozza per modificare un impegno esistente. */
    fun bozza(impegno: Impegno): Bozza {
        val zona = ZoneId.systemDefault()
        var titolo = impegno.titolo
        var descrizione = ""
        resolver.query(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, impegno.eventoId),
            arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DESCRIPTION),
            null,
            null,
            null,
        )?.use { c ->
            if (c.moveToFirst()) {
                titolo = c.getString(0) ?: ""
                descrizione = c.getString(1) ?: ""
            }
        }
        val inizio: LocalDateTime
        val fine: LocalDateTime
        if (impegno.tuttoIlGiorno) {
            inizio = impegno.giornoInizio(zona).atTime(9, 0)
            fine = impegno.giornoFine(zona).atTime(10, 0)
        } else {
            inizio = Instant.ofEpochMilli(impegno.inizio).atZone(zona).toLocalDateTime()
            fine = Instant.ofEpochMilli(impegno.fine).atZone(zona).toLocalDateTime()
        }
        return Bozza(
            eventoId = impegno.eventoId,
            calendarioId = impegno.calendarioId,
            titolo = titolo,
            luogo = impegno.luogo,
            descrizione = descrizione,
            tuttoIlGiorno = impegno.tuttoIlGiorno,
            inizio = inizio,
            fine = fine,
        )
    }

    /** Salva la bozza e restituisce l'identificativo dell'evento. */
    fun salva(bozza: Bozza): Long {
        val valori = ContentValues().apply {
            put(CalendarContract.Events.TITLE, bozza.titolo.trim())
            put(CalendarContract.Events.EVENT_LOCATION, bozza.luogo.trim())
            put(CalendarContract.Events.DESCRIPTION, bozza.descrizione.trim())
            if (bozza.tuttoIlGiorno) {
                val primo = bozza.inizio.toLocalDate()
                val ultimo = bozza.fine.toLocalDate()
                put(CalendarContract.Events.DTSTART, primo.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, ultimo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                val zona = ZoneId.systemDefault()
                put(CalendarContract.Events.DTSTART, bozza.inizio.atZone(zona).toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, bozza.fine.atZone(zona).toInstant().toEpochMilli())
                put(CalendarContract.Events.ALL_DAY, 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, zona.id)
            }
        }
        val id = bozza.eventoId
        return if (id == null) {
            valori.put(CalendarContract.Events.CALENDAR_ID, bozza.calendarioId)
            val indirizzo = resolver.insert(CalendarContract.Events.CONTENT_URI, valori)
                ?: error("Inserimento non riuscito")
            ContentUris.parseId(indirizzo)
        } else {
            resolver.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), valori, null, null)
            id
        }
    }

    fun elimina(eventoId: Long) {
        resolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventoId), null, null)
    }
}
