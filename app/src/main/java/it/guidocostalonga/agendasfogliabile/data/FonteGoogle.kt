package it.guidocostalonga.agendasfogliabile.data

import android.accounts.Account
import android.content.Context
import android.graphics.Color
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private val FORMATO_GOOGLE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

/**
 * Data e ora nel formato che Google Calendar accetta, con i secondi sempre presenti:
 * «2026-09-29T15:00:00+02:00». Il toString() di Java li omette quando valgono zero.
 */
fun orarioGoogle(momento: LocalDateTime, zona: ZoneId): String = momento.atZone(zona).format(FORMATO_GOOGLE)

/** Accesso all'account Google con il consenso della persona. */
object AccessoGoogle {
    private const val AMBITO = "https://www.googleapis.com/auth/calendar"

    fun richiesta(email: String?): AuthorizationRequest {
        val costruttore = AuthorizationRequest.Builder().setRequestedScopes(listOf(Scope(AMBITO)))
        if (email != null) costruttore.setAccount(Account(email, "com.google"))
        return costruttore.build()
    }

    /** Da usare fuori dal thread principale. Restituisce il gettone se il consenso c'è già. */
    fun gettoneSilenzioso(context: Context, email: String): String {
        val risultato = Tasks.await(
            Identity.getAuthorizationClient(context).authorize(richiesta(email)),
            30,
            TimeUnit.SECONDS,
        )
        if (risultato.hasResolution()) throw AccessoNecessario()
        return risultato.accessToken ?: throw AccessoNecessario()
    }

    /** L'indirizzo dell'account: è l'identificativo del calendario principale. */
    fun emailPrincipale(gettone: String): String {
        val connessione = URL("${FonteGoogle.BASE}users/me/calendarList/primary").openConnection() as HttpURLConnection
        connessione.setRequestProperty("Authorization", "Bearer $gettone")
        connessione.connectTimeout = 15_000
        connessione.readTimeout = 20_000
        try {
            if (connessione.responseCode !in 200..299) {
                val dettaglio = connessione.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Google ha risposto ${connessione.responseCode}. ${spiegaRisposta(dettaglio)}")
            }
            return JSONObject(connessione.inputStream.bufferedReader().use { it.readText() }).getString("id")
        } finally {
            connessione.disconnect()
        }
    }

    fun spiegaRisposta(corpo: String): String {
        val messaggio = runCatching { JSONObject(corpo).getJSONObject("error").optString("message") }
            .getOrNull()
            .orEmpty()
        return when {
            "accessNotConfigured" in corpo || "SERVICE_DISABLED" in corpo ->
                "Su Google Cloud Console va abilitata la Google Calendar API per il progetto."
            "forbiddenForNonOrganizer" in corpo ->
                "Solo chi ha organizzato l'impegno può modificarlo."
            "eventTypeRestriction" in corpo || "birthday" in corpo.lowercase() ->
                "Google non consente di modificare questo tipo di impegno."
            "timeRangeEmpty" in corpo -> "La fine deve venire dopo l'inizio."
            messaggio.isNotBlank() -> "Motivo indicato da Google: $messaggio."
            else -> corpo.take(200)
        }
    }
}

/** Legge e scrive gli impegni direttamente su Google Calendar. */
class FonteGoogle(private val context: Context, val email: String) : FonteCalendario {

    private var gettone: String? = null
    private var scadenza = 0L
    private var elenco: Pair<Long, List<Calendario>>? = null
    private var coloriEventi: Map<String, Int>? = null
    private val mesi = HashMap<YearMonth, Pair<Long, List<Impegno>>>()

    override fun puoScrivere(): Boolean = true

    @Synchronized
    override fun svuota() {
        elenco = null
        mesi.clear()
    }

    @Synchronized
    private fun gettone(rinnova: Boolean): String {
        val attuale = gettone
        if (!rinnova && attuale != null && System.currentTimeMillis() < scadenza) return attuale
        val nuovo = AccessoGoogle.gettoneSilenzioso(context, email)
        gettone = nuovo
        scadenza = System.currentTimeMillis() + 45 * 60_000L
        return nuovo
    }

    private fun codifica(testo: String): String = URLEncoder.encode(testo, "UTF-8")

    /** Chiamata all'interfaccia di Google Calendar; al primo rifiuto rinnova il gettone e riprova. */
    private fun chiama(
        metodo: String,
        percorso: String,
        parametri: Map<String, String> = emptyMap(),
        corpo: JSONObject? = null,
    ): JSONObject? {
        val domanda = if (parametri.isEmpty()) {
            ""
        } else {
            "?" + parametri.entries.joinToString("&") { "${it.key}=${codifica(it.value)}" }
        }
        val indirizzo = URL(BASE + percorso + domanda)
        for (tentativo in 0..1) {
            val connessione = indirizzo.openConnection() as HttpURLConnection
            try {
                connessione.connectTimeout = 15_000
                connessione.readTimeout = 30_000
                if (metodo == "PATCH") {
                    connessione.requestMethod = "POST"
                    connessione.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    connessione.requestMethod = metodo
                }
                connessione.setRequestProperty("Authorization", "Bearer ${gettone(rinnova = tentativo > 0)}")
                if (corpo != null) {
                    connessione.doOutput = true
                    connessione.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connessione.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
                }
                val codice = connessione.responseCode
                if (codice == 401 && tentativo == 0) continue
                if (codice !in 200..299) {
                    val dettaglio = connessione.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IOException("Google ha rifiutato l'operazione (codice $codice). ${AccessoGoogle.spiegaRisposta(dettaglio)}")
                }
                if (codice == 204) return null
                val testo = connessione.inputStream.bufferedReader().use { it.readText() }
                return if (testo.isBlank()) null else JSONObject(testo)
            } finally {
                connessione.disconnect()
            }
        }
        throw AccessoNecessario()
    }

    /** Legge tutte le pagine di un elenco. */
    private fun tutti(percorso: String, parametri: Map<String, String>): List<JSONObject> {
        val risultato = mutableListOf<JSONObject>()
        var pagina: String? = null
        do {
            val richiesta = if (pagina == null) parametri else parametri + ("pageToken" to pagina)
            val risposta = chiama("GET", percorso, richiesta) ?: break
            val voci = risposta.optJSONArray("items")
            if (voci != null) for (i in 0 until voci.length()) risultato += voci.getJSONObject(i)
            pagina = risposta.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (pagina != null)
        return risultato
    }

    private fun colore(esadecimale: String?, predefinito: Int): Int =
        try {
            if (esadecimale.isNullOrBlank()) predefinito else Color.parseColor(esadecimale)
        } catch (e: IllegalArgumentException) {
            predefinito
        }

    @Synchronized
    override fun calendari(): List<Calendario> {
        elenco?.let { (quando, valore) -> if (System.currentTimeMillis() - quando < 10 * 60_000L) return valore }
        val voci = tutti("users/me/calendarList", mapOf("maxResults" to "250"))
        val calendari = voci
            .sortedWith(compareBy<JSONObject>({ !it.optBoolean("primary") }, { it.optString("summary") }))
            .map { voce ->
                val ruolo = voce.optString("accessRole")
                Calendario(
                    id = voce.getString("id"),
                    nome = voce.optString("summaryOverride").ifBlank { voce.optString("summary") }.ifBlank { voce.getString("id") },
                    account = email,
                    tipoAccount = "google",
                    colore = colore(voce.optString("backgroundColor"), 0xFF1E88E5.toInt()),
                    scrivibile = ruolo == "owner" || ruolo == "writer",
                )
            }
        elenco = System.currentTimeMillis() to calendari
        return calendari
    }

    @Synchronized
    private fun coloriEventi(): Map<String, Int> {
        coloriEventi?.let { return it }
        val colori = mutableMapOf<String, Int>()
        runCatching {
            chiama("GET", "colors")?.optJSONObject("event")?.let { eventi ->
                for (chiave in eventi.keys()) {
                    colori[chiave] = colore(eventi.getJSONObject(chiave).optString("background"), 0)
                }
            }
        }
        coloriEventi = colori
        return colori
    }

    private fun leggi(voce: JSONObject, calendario: Calendario): Impegno? {
        if (voce.optString("status") == "cancelled") return null
        val inizio = voce.optJSONObject("start") ?: return null
        val fine = voce.optJSONObject("end") ?: inizio
        val tuttoIlGiorno = !inizio.has("dateTime")
        val (da, a) = if (tuttoIlGiorno) {
            val primo = LocalDate.parse(inizio.getString("date"))
            val ultimo = if (fine.has("date")) LocalDate.parse(fine.getString("date")) else primo.plusDays(1)
            primo.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() to
                ultimo.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            val primo = OffsetDateTime.parse(inizio.getString("dateTime")).toInstant().toEpochMilli()
            val ultimo = if (fine.has("dateTime")) OffsetDateTime.parse(fine.getString("dateTime")).toInstant().toEpochMilli() else primo
            primo to ultimo
        }
        val coloreProprio = voce.optString("colorId").takeIf { it.isNotEmpty() }?.let { coloriEventi()[it] }
        return Impegno(
            eventoId = voce.getString("id"),
            calendarioId = calendario.id,
            titolo = voce.optString("summary").ifBlank { "(senza titolo)" },
            luogo = voce.optString("location"),
            inizio = da,
            fine = a,
            tuttoIlGiorno = tuttoIlGiorno,
            colore = coloreProprio?.takeIf { it != 0 } ?: calendario.colore,
            scrivibile = calendario.scrivibile && voce.optString("eventType", "default") in TIPI_MODIFICABILI,
            ricorrente = voce.has("recurringEventId") || voce.has("recurrence"),
            collegamento = voce.optString("htmlLink").takeIf { it.isNotEmpty() },
        )
    }

    private fun scarica(calendari: List<Calendario>, da: Instant, a: Instant, filtro: String?): List<Impegno> {
        val risultato = mutableListOf<Impegno>()
        for (calendario in calendari) {
            val parametri = buildMap {
                put("timeMin", da.toString())
                put("timeMax", a.toString())
                put("singleEvents", "true")
                put("orderBy", "startTime")
                put("maxResults", "2500")
                if (!filtro.isNullOrBlank()) put("q", filtro.trim())
            }
            tutti("calendars/${codifica(calendario.id)}/events", parametri).forEach { voce ->
                leggi(voce, calendario)?.let { risultato += it }
            }
        }
        return risultato
    }

    /** Gli impegni di un mese di tutti i calendari, tenuti in memoria per cinque minuti. */
    @Synchronized
    private fun mese(mese: YearMonth): List<Impegno> {
        mesi[mese]?.let { (quando, valore) -> if (System.currentTimeMillis() - quando < 5 * 60_000L) return valore }
        val zona = ZoneId.systemDefault()
        // Un giorno di margine per parte: gli impegni di un giorno intero sono in tempo universale.
        val da = mese.atDay(1).minusDays(1).atStartOfDay(zona).toInstant()
        val a = mese.atEndOfMonth().plusDays(2).atStartOfDay(zona).toInstant()
        val valore = scarica(calendari(), da, a, null)
        mesi[mese] = System.currentTimeMillis() to valore
        return valore
    }

    override fun impegni(
        dal: LocalDate,
        al: LocalDate,
        scelti: Set<String>?,
        filtro: String?,
        limite: Int,
    ): List<Impegno> {
        if (scelti != null && scelti.isEmpty()) return emptyList()
        val zona = ZoneId.systemDefault()
        val grezzi = if (!filtro.isNullOrBlank()) {
            val calendari = calendari().filter { scelti == null || it.id in scelti }
            scarica(calendari, dal.atStartOfDay(zona).toInstant(), al.plusDays(1).atStartOfDay(zona).toInstant(), filtro)
        } else {
            val mesiUtili = generateSequence(YearMonth.from(dal)) { it.plusMonths(1) }
                .takeWhile { !it.isAfter(YearMonth.from(al)) }
                .toList()
            mesiUtili.flatMap { mese(it) }
        }
        return grezzi
            .asSequence()
            .filter { scelti == null || it.calendarioId in scelti }
            .filter { !it.giornoFine(zona).isBefore(dal) && !it.giornoInizio(zona).isAfter(al) }
            .distinctBy { it.calendarioId + "|" + it.eventoId }
            .sortedWith(compareBy<Impegno>({ it.giornoInizio(zona) }, { !it.tuttoIlGiorno }, { it.inizio }))
            .take(limite)
            .toList()
    }

    override fun bozza(impegno: Impegno): Bozza {
        val zona = ZoneId.systemDefault()
        val voce = chiama("GET", "calendars/${codifica(impegno.calendarioId)}/events/${codifica(impegno.eventoId)}")
        val inizio = if (impegno.tuttoIlGiorno) {
            impegno.giornoInizio(zona).atTime(9, 0)
        } else {
            Instant.ofEpochMilli(impegno.inizio).atZone(zona).toLocalDateTime()
        }
        val fine = if (impegno.tuttoIlGiorno) {
            impegno.giornoFine(zona).atTime(10, 0)
        } else {
            Instant.ofEpochMilli(impegno.fine).atZone(zona).toLocalDateTime()
        }
        return Bozza(
            eventoId = impegno.eventoId,
            calendarioId = impegno.calendarioId,
            titolo = voce?.optString("summary") ?: impegno.titolo,
            luogo = voce?.optString("location") ?: impegno.luogo,
            descrizione = voce?.optString("description").orEmpty(),
            tuttoIlGiorno = impegno.tuttoIlGiorno,
            inizio = inizio,
            fine = fine,
        )
    }

    override fun salva(bozza: Bozza): String {
        val zona = ZoneId.systemDefault()
        val percorso = "calendars/${codifica(bozza.calendarioId)}/events"
        val id = bozza.eventoId
        // Per una modifica si parte dall'impegno completo, così invitati e promemoria restano com'erano.
        val corpo = if (id != null) {
            chiama("GET", "$percorso/${codifica(id)}") ?: JSONObject()
        } else {
            JSONObject()
        }
        corpo.put("summary", bozza.titolo.trim())
        corpo.put("location", bozza.luogo.trim())
        corpo.put("description", bozza.descrizione.trim())
        if (bozza.tuttoIlGiorno) {
            corpo.put("start", JSONObject().put("date", bozza.inizio.toLocalDate().toString()))
            corpo.put("end", JSONObject().put("date", bozza.fine.toLocalDate().plusDays(1).toString()))
        } else {
            corpo.put(
                "start",
                JSONObject()
                    .put("dateTime", orarioGoogle(bozza.inizio, zona))
                    .put("timeZone", zona.id),
            )
            corpo.put(
                "end",
                JSONObject()
                    .put("dateTime", orarioGoogle(bozza.fine, zona))
                    .put("timeZone", zona.id),
            )
        }
        val risposta = if (id != null) {
            chiama("PUT", "$percorso/${codifica(id)}", corpo = corpo)
        } else {
            chiama("POST", percorso, corpo = corpo)
        }
        svuota()
        return risposta?.optString("id") ?: id.orEmpty()
    }

    override fun elimina(impegno: Impegno) {
        chiama("DELETE", "calendars/${codifica(impegno.calendarioId)}/events/${codifica(impegno.eventoId)}")
        svuota()
    }

    companion object {
        const val BASE = "https://www.googleapis.com/calendar/v3/"

        // Compleanni, eventi creati da Gmail e simili non si possono modificare liberamente.
        private val TIPI_MODIFICABILI = setOf("default", "")
    }
}
