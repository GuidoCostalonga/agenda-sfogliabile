package it.guidocostalonga.agendasfogliabile.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import it.guidocostalonga.agendasfogliabile.data.AccessoGoogle
import it.guidocostalonga.agendasfogliabile.data.descriviErrore
import it.guidocostalonga.agendasfogliabile.data.erroreGoogle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collega l'agenda all'account Google: Google chiede quale account usare e il consenso
 * per il calendario, poi restituisce l'indirizzo dell'account collegato.
 */
@Composable
fun rememberCollegamentoGoogle(onFatto: (String) -> Unit, onErrore: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fatto by rememberUpdatedState(onFatto)
    val errore by rememberUpdatedState(onErrore)

    fun completa(gettone: String) {
        scope.launch {
            val esito = withContext(Dispatchers.IO) { runCatching { AccessoGoogle.emailPrincipale(gettone) } }
            esito.fold(onSuccess = { fatto(it) }, onFailure = { errore(descriviErrore(it)) })
        }
    }

    val consenso = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { risultato ->
        if (risultato.resultCode != Activity.RESULT_OK) {
            errore("Accesso a Google annullato.")
            return@rememberLauncherForActivityResult
        }
        try {
            val autorizzazione = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(risultato.data)
            val gettone = autorizzazione.accessToken
            if (gettone != null) completa(gettone) else errore("Google non ha concesso l'accesso al calendario.")
        } catch (e: ApiException) {
            errore(erroreGoogle(e))
        }
    }

    return {
        Identity.getAuthorizationClient(context)
            .authorize(AccessoGoogle.richiesta(null))
            .addOnSuccessListener { autorizzazione ->
                val passaggio = autorizzazione.pendingIntent
                val gettone = autorizzazione.accessToken
                when {
                    autorizzazione.hasResolution() && passaggio != null ->
                        consenso.launch(IntentSenderRequest.Builder(passaggio.intentSender).build())
                    gettone != null -> completa(gettone)
                    else -> errore("Google non ha concesso l'accesso al calendario.")
                }
            }
            .addOnFailureListener { e ->
                errore(if (e is ApiException) erroreGoogle(e) else descriviErrore(e))
            }
    }
}
