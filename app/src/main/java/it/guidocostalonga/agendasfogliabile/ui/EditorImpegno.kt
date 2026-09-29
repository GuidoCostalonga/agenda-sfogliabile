package it.guidocostalonga.agendasfogliabile.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.FonteCalendario
import it.guidocostalonga.agendasfogliabile.data.descriviErrore
import it.guidocostalonga.agendasfogliabile.data.Bozza
import it.guidocostalonga.agendasfogliabile.data.Calendario
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.data.Preferenze
import it.guidocostalonga.agendasfogliabile.util.dataEstesa
import it.guidocostalonga.agendasfogliabile.util.nomeGiornoBreve
import it.guidocostalonga.agendasfogliabile.util.testo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Scheda di un impegno: nuovo, da modificare o in sola lettura.
 * Gli impegni che si ripetono si aprono in sola lettura per non alterare tutta la serie.
 */
@Composable
fun EditorImpegno(
    fonte: FonteCalendario,
    preferenze: Preferenze,
    impegno: Impegno?,
    giorno: LocalDate,
    ora: Int?,
    onChiudi: (salvato: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val calendari by produceState<List<Calendario>>(emptyList()) {
        value = withContext(Dispatchers.IO) { runCatching { fonte.calendari() }.getOrDefault(emptyList()) }
    }
    val modificabili = calendari.filter { it.scrivibile }
    var bozza by remember { mutableStateOf<Bozza?>(null) }
    var errore by remember { mutableStateOf<String?>(null) }
    var conferma by remember { mutableStateOf(false) }
    val puoScrivere = remember(fonte) { fonte.puoScrivere() }
    val solaLettura = !puoScrivere || (impegno != null && (!impegno.scrivibile || impegno.ricorrente))

    LaunchedEffect(Unit) {
        bozza = if (impegno != null) {
            withContext(Dispatchers.IO) {
                try {
                    fonte.bozza(impegno)
                } catch (e: Exception) {
                    errore = descriviErrore(e)
                    null
                }
            }
        } else {
            val inizio = giorno.atTime(ora ?: 9, 0)
            Bozza(calendarioId = preferenze.ultimoCalendario, inizio = inizio, fine = inizio.plusHours(1))
        }
    }
    // Per un impegno nuovo propone l'ultimo calendario usato, altrimenti quello principale.
    LaunchedEffect(calendari, bozza != null) {
        val attuale = bozza ?: return@LaunchedEffect
        if (attuale.eventoId == null && modificabili.isNotEmpty() && modificabili.none { it.id == attuale.calendarioId }) {
            val principale = modificabili.firstOrNull { it.nome == it.account } ?: modificabili.first()
            bozza = attuale.copy(calendarioId = principale.id)
        }
    }

    fun salva() {
        val b = bozza ?: return
        errore = null
        if (b.titolo.isBlank()) {
            errore = "Scrivi un titolo per l'impegno."
            return
        }
        if (modificabili.none { it.id == b.calendarioId }) {
            errore = "Scegli un calendario in cui salvare l'impegno."
            return
        }
        val valido = if (b.tuttoIlGiorno) {
            !b.fine.toLocalDate().isBefore(b.inizio.toLocalDate())
        } else {
            !b.fine.isBefore(b.inizio)
        }
        if (!valido) {
            errore = "La fine deve venire dopo l'inizio."
            return
        }
        scope.launch {
            val esito = withContext(Dispatchers.IO) { runCatching { fonte.salva(b) } }
            if (esito.isSuccess) {
                preferenze.ultimoCalendario = b.calendarioId
                onChiudi(true)
            } else {
                errore = "Salvataggio non riuscito. " + descriviErrore(esito.exceptionOrNull()!!)
            }
        }
    }

    fun apriNelCalendario(voce: Impegno) {
        val numero = voce.eventoId.toLongOrNull()
        val intento = if (voce.collegamento != null || numero == null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(voce.collegamento ?: "https://calendar.google.com/"))
        } else {
            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, numero))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, voce.inizio)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, voce.fine)
        }
        try {
            context.startActivity(intento)
        } catch (e: ActivityNotFoundException) {
            errore = "Sul telefono non c'è un'applicazione calendario da aprire."
        }
    }

    Scaffold(
        containerColor = Colori.Pagina,
        topBar = {
            BarraSuperiore(
                titolo = when {
                    impegno == null -> "Nuovo impegno"
                    solaLettura -> "Impegno"
                    else -> "Modifica impegno"
                },
                onIndietro = { onChiudi(false) },
            ) {
                if (!solaLettura) {
                    TextButton(
                        onClick = { salva() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) { Text("Salva", fontWeight = FontWeight.Bold) }
                }
            }
        },
    ) { spazio ->
        val b = bozza
        if (b == null) {
            Box(Modifier.padding(spazio).fillMaxSize().padding(16.dp)) {
                errore?.let { Avviso(it) }
            }
        } else {
            Column(
                Modifier
                    .padding(spazio)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (solaLettura) {
                    Avviso(
                        when {
                            !puoScrivere -> "Per aggiungere o modificare impegni consenti all'applicazione " +
                                "di modificare il calendario, dalle impostazioni del telefono."
                            impegno?.ricorrente == true -> "È un impegno che si ripete. Per non alterare " +
                                "tutta la serie, modificalo dall'applicazione Google Calendar."
                            else -> "Questo calendario è in sola lettura."
                        },
                    )
                }
                OutlinedTextField(
                    value = b.titolo,
                    onValueChange = { bozza = b.copy(titolo = it) },
                    label = { Text("Titolo") },
                    enabled = !solaLettura,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SelettoreCalendario(
                    calendari = calendari,
                    scelto = b.calendarioId,
                    abilitato = impegno == null && !solaLettura,
                    onScelta = { bozza = b.copy(calendarioId = it) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tutto il giorno", modifier = Modifier.weight(1f), fontSize = 16.sp, color = Colori.Inchiostro)
                    Switch(
                        checked = b.tuttoIlGiorno,
                        onCheckedChange = { bozza = b.copy(tuttoIlGiorno = it) },
                        enabled = !solaLettura,
                        colors = SwitchDefaults.colors(checkedTrackColor = Colori.Copertina),
                    )
                }
                RigaDataOra("Inizio", b.inizio, b.tuttoIlGiorno, !solaLettura) { bozza = b.conInizio(it) }
                RigaDataOra("Fine", b.fine, b.tuttoIlGiorno, !solaLettura) { bozza = b.copy(fine = it) }
                OutlinedTextField(
                    value = b.luogo,
                    onValueChange = { bozza = b.copy(luogo = it) },
                    label = { Text("Luogo") },
                    enabled = !solaLettura,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = b.descrizione,
                    onValueChange = { bozza = b.copy(descrizione = it) },
                    label = { Text("Note") },
                    enabled = !solaLettura,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                errore?.let { Text(it, color = Colori.Nastro, fontWeight = FontWeight.SemiBold) }
                if (impegno != null) {
                    OutlinedButton(onClick = { apriNelCalendario(impegno) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Apri in Google Calendar", color = Colori.Copertina)
                    }
                    if (!solaLettura) {
                        TextButton(onClick = { conferma = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Elimina impegno", color = Colori.Nastro)
                        }
                    }
                }
            }
        }
    }

    if (conferma && impegno != null) {
        AlertDialog(
            onDismissRequest = { conferma = false },
            title = { Text("Eliminare l'impegno?", fontFamily = Grazie) },
            text = { Text("L'impegno sarà cancellato anche da Google Calendar.") },
            confirmButton = {
                TextButton(onClick = {
                    conferma = false
                    scope.launch {
                        val esito = withContext(Dispatchers.IO) { runCatching { fonte.elimina(impegno) } }
                        if (esito.isSuccess) {
                            onChiudi(true)
                        } else {
                            errore = "Eliminazione non riuscita. " + descriviErrore(esito.exceptionOrNull()!!)
                        }
                    }
                }) { Text("Elimina", color = Colori.Nastro) }
            },
            dismissButton = {
                TextButton(onClick = { conferma = false }) { Text("Annulla", color = Colori.Copertina) }
            },
        )
    }
}

@Composable
private fun SelettoreCalendario(
    calendari: List<Calendario>,
    scelto: String,
    abilitato: Boolean,
    onScelta: (String) -> Unit,
) {
    var aperto by remember { mutableStateOf(false) }
    val attuale = calendari.firstOrNull { it.id == scelto }
    Box {
        OutlinedButton(
            onClick = { aperto = true },
            enabled = abilitato,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (attuale != null) {
                Pallino(attuale.colore)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                attuale?.nome ?: "Scegli il calendario",
                modifier = Modifier.weight(1f),
                color = Colori.Inchiostro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            calendari.filter { it.scrivibile }.forEach { calendario ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(calendario.nome, color = Colori.Inchiostro)
                            Text(calendario.account, fontSize = 12.sp, color = Colori.InchiostroTenue)
                        }
                    },
                    leadingIcon = { Pallino(calendario.colore) },
                    onClick = {
                        onScelta(calendario.id)
                        aperto = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RigaDataOra(
    etichetta: String,
    valore: LocalDateTime,
    soloData: Boolean,
    abilitato: Boolean,
    onCambio: (LocalDateTime) -> Unit,
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(etichetta, modifier = Modifier.width(56.dp), color = Colori.InchiostroTenue)
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, anno, mese, giorno -> onCambio(LocalDate.of(anno, mese + 1, giorno).atTime(valore.toLocalTime())) },
                    valore.year,
                    valore.monthValue - 1,
                    valore.dayOfMonth,
                ).show()
            },
            enabled = abilitato,
            modifier = Modifier.weight(1f),
        ) {
            val data = valore.toLocalDate()
            Text(
                "${data.nomeGiornoBreve()} ${data.dataEstesa()}",
                color = Colori.Inchiostro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!soloData) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, ore, minuti -> onCambio(valore.toLocalDate().atTime(ore, minuti)) },
                        valore.hour,
                        valore.minute,
                        true,
                    ).show()
                },
                enabled = abilitato,
            ) {
                Text(valore.toLocalTime().testo(), color = Colori.Inchiostro)
            }
        }
    }
}
