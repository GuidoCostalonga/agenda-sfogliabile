package it.guidocostalonga.agendasfogliabile.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract

/**
 * Fa arrivare sul calendario del telefono il calendario di un account Google.
 * Serve quando Google Calendar mostra gli impegni ma Android non li ha ancora ricevuti.
 */
object Sincronizzazione {

    const val TIPO_GOOGLE = "com.google"

    /** Finestra di sistema in cui la persona sceglie il proprio account Google. */
    fun sceltaAccount(): Intent = AccountManager.newChooseAccountIntent(
        null,
        null,
        arrayOf(TIPO_GOOGLE),
        "Scegli l'account Google del calendario da mostrare nell'agenda",
        null,
        null,
        null,
    )

    /** Attiva la sincronizzazione del calendario per l'account e la avvia subito. Restituisce l'esito da mostrare. */
    fun avvia(account: Account): String {
        val autorita = CalendarContract.AUTHORITY
        return try {
            val sincronizzabile = ContentResolver.getIsSyncable(account, autorita)
            if (sincronizzabile == 0) {
                return "Su questo telefono l'account ${account.name} non sincronizza il calendario con Android. " +
                    "Apri Impostazioni, Account, Google, ${account.name}: se la voce Calendario manca, " +
                    "aggiorna l'applicazione Google Calendar dal Play Store e riprova."
            }
            if (sincronizzabile < 0) ContentResolver.setIsSyncable(account, autorita, 1)
            val eraSpenta = !ContentResolver.getSyncAutomatically(account, autorita)
            if (eraSpenta) ContentResolver.setSyncAutomatically(account, autorita, true)
            val opzioni = Bundle().apply {
                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            }
            ContentResolver.requestSync(account, autorita, opzioni)
            buildString {
                if (eraSpenta) append("La sincronizzazione del calendario per ${account.name} era spenta: l'ho attivata. ")
                append("Sincronizzazione avviata. Il calendario comparirà in questo elenco entro qualche minuto.")
                if (!ContentResolver.getMasterSyncAutomatically()) {
                    append(
                        " Attenzione: sul telefono è spenta la sincronizzazione automatica dei dati, " +
                            "quindi le modifiche successive arriveranno solo quando la riattivi.",
                    )
                }
            }
        } catch (e: SecurityException) {
            "Il telefono non consente all'agenda di avviare la sincronizzazione. " +
                "Apri Impostazioni, Account, Google, ${account.name}, Sincronizza account, " +
                "attiva Calendario e scegli Sincronizza ora."
        }
    }
}
