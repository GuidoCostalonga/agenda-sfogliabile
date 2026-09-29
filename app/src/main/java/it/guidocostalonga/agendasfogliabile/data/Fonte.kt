package it.guidocostalonga.agendasfogliabile.data

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.util.concurrent.ExecutionException

/** Da dove l'agenda legge e scrive gli impegni. */
interface FonteCalendario {
    fun puoScrivere(): Boolean
    fun calendari(): List<Calendario>
    fun impegni(
        dal: LocalDate,
        al: LocalDate,
        scelti: Set<String>?,
        filtro: String? = null,
        limite: Int = Int.MAX_VALUE,
    ): List<Impegno>
    fun bozza(impegno: Impegno): Bozza
    fun salva(bozza: Bozza): String
    fun elimina(impegno: Impegno)

    /** Dimentica i dati tenuti in memoria, così la prossima lettura è fresca. */
    fun svuota() = Unit
}

/** Il consenso a Google manca, è scaduto o è stato revocato. */
class AccessoNecessario : Exception("Serve ricollegarsi a Google")

/** Sceglie la fonte indicata nelle preferenze. */
object Fonti {
    @Volatile
    private var google: FonteGoogle? = null

    fun attiva(context: Context, preferenze: Preferenze = Preferenze(context)): FonteCalendario {
        val email = preferenze.emailGoogle
        if (preferenze.fonte == Preferenze.FONTE_GOOGLE && email != null) {
            google?.takeIf { it.email == email }?.let { return it }
            return FonteGoogle(context.applicationContext, email).also { google = it }
        }
        return ArchivioCalendario(context)
    }
}

/** Spiega un errore con parole semplici. */
fun descriviErrore(errore: Throwable): String {
    val causa = if (errore is ExecutionException && errore.cause != null) errore.cause!! else errore
    return when (causa) {
        is java.util.concurrent.CancellationException ->
            "Caricamento interrotto: riprova."
        is AccessoNecessario ->
            "L'accesso a Google è scaduto o è stato revocato. Ricollegati dal menu ⋮, «Calendari e collegamento»."
        is UnknownHostException, is ConnectException, is SocketTimeoutException ->
            "Nessuna connessione a Internet: gli impegni si aggiorneranno appena torni in rete."
        is ApiException -> erroreGoogle(causa)
        is java.io.IOException -> causa.message?.takeIf { it.startsWith("Google ") }
            ?: "Collegamento con Google non riuscito: riprova."
        else -> "Google non ha risposto come previsto (${causa.message?.take(200) ?: causa.javaClass.simpleName})."
    }
}

fun erroreGoogle(errore: ApiException): String = when (errore.statusCode) {
    CommonStatusCodes.DEVELOPER_ERROR ->
        "Google non riconosce l'applicazione (codice 10). Su Google Cloud Console controlla il client OAuth " +
            "di tipo Android: nome del pacchetto it.guidocostalonga.agendasfogliabile e impronta SHA-1 della chiave di firma."
    CommonStatusCodes.NETWORK_ERROR -> "Nessuna connessione a Internet: riprova quando sei in rete."
    CommonStatusCodes.CANCELED -> "Accesso annullato."
    else -> "Accesso a Google non riuscito (codice ${errore.statusCode}: ${errore.message?.take(160)})."
}
