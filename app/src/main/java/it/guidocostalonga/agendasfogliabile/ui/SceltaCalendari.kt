package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.Calendario
import it.guidocostalonga.agendasfogliabile.data.FonteCalendario
import it.guidocostalonga.agendasfogliabile.data.Preferenze
import it.guidocostalonga.agendasfogliabile.data.descriviErrore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sceglie da dove leggere gli impegni e quali calendari mostrare. */
@Composable
fun SceltaCalendari(
    fonte: FonteCalendario,
    fonteScelta: String,
    email: String?,
    preferenze: Preferenze,
    erroreCollegamento: String?,
    onIndietro: () -> Unit,
    onCambio: (Set<String>?) -> Unit,
    onCollegaGoogle: () -> Unit,
    onUsaTelefono: () -> Unit,
) {
    var ricarica by remember { mutableIntStateOf(0) }
    var problema by remember(fonte) { mutableStateOf<String?>(null) }
    val calendari by produceState<List<Calendario>?>(null, fonte, ricarica) {
        value = null
        value = try {
            withContext(Dispatchers.IO) { fonte.calendari() }
        } catch (e: Exception) {
            problema = descriviErrore(e)
            emptyList()
        }
    }
    var scelti by remember(fonte) { mutableStateOf(preferenze.calendariScelti) }
    val google = fonteScelta == Preferenze.FONTE_GOOGLE

    fun imposta(nuovi: Set<String>?) {
        scelti = nuovi
        preferenze.calendariScelti = nuovi
        onCambio(nuovi)
    }

    Scaffold(
        containerColor = Colori.Pagina,
        topBar = { BarraSuperiore("Calendari e collegamento", onIndietro) },
    ) { spazio ->
        val elenco = calendari.orEmpty()
        val tutti = elenco.map { it.id }.toSet()
        fun alterna(id: String) {
            val attuali = scelti ?: tutti
            val nuovi = if (id in attuali) attuali - id else attuali + id
            imposta(if (nuovi.containsAll(tutti)) null else nuovi)
        }
        LazyColumn(
            Modifier.padding(spazio).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Column {
                    Etichetta("Da dove leggere gli impegni")
                    Scelta(
                        titolo = "Google Calendar, accesso diretto",
                        spiegazione = if (google && email != null) {
                            "Collegato come $email. Tocca qui per cambiare account o ricollegarti."
                        } else {
                            "L'agenda entra nel tuo account Google con il tuo consenso. Serve Internet."
                        },
                        attiva = google,
                        onClick = onCollegaGoogle,
                    )
                    Scelta(
                        titolo = "Calendario del telefono",
                        spiegazione = "Legge i calendari che Android ha già sul telefono, anche senza rete.",
                        attiva = !google,
                        onClick = onUsaTelefono,
                    )
                    erroreCollegamento?.let { Avviso(it, Modifier.padding(top = 8.dp)) }
                }
            }
            item {
                Column(Modifier.padding(top = 20.dp)) {
                    Etichetta("Calendari da mostrare")
                    Text(
                        "Scegli quali calendari compaiono nell'agenda e nel riquadro della schermata iniziale.",
                        fontSize = 14.sp,
                        color = Colori.InchiostroTenue,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row {
                        TextButton(onClick = { imposta(null) }) { Text("Mostra tutti", color = Colori.Copertina) }
                        TextButton(onClick = { imposta(emptySet()) }) { Text("Nascondi tutti", color = Colori.Nastro) }
                        TextButton(onClick = {
                            problema = null
                            fonte.svuota()
                            ricarica++
                        }) { Text("Ricarica", color = Colori.Copertina) }
                    }
                    problema?.let { Avviso(it) }
                    when {
                        calendari == null -> Text("Caricamento…", color = Colori.InchiostroTenue)
                        elenco.isEmpty() && problema == null -> Avviso(
                            if (google) {
                                "Nell'account Google non risulta alcun calendario."
                            } else {
                                "Sul telefono non risulta alcun calendario Google. Se in Google Calendar " +
                                    "i tuoi impegni si vedono, scegli qui sopra l'accesso diretto a Google."
                            },
                        )
                    }
                }
            }
            elenco.groupBy { it.account }.forEach { (account, gruppo) ->
                item { Etichetta(account, Modifier.padding(top = 16.dp, bottom = 4.dp)) }
                items(gruppo) { calendario ->
                    val attivo = scelti?.contains(calendario.id) ?: true
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { alterna(calendario.id) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = attivo,
                            onCheckedChange = { alterna(calendario.id) },
                            colors = CheckboxDefaults.colors(checkedColor = coloreDi(calendario.colore)),
                        )
                        Pallino(calendario.colore)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(calendario.nome, fontSize = 16.sp, color = Colori.Inchiostro)
                            if (!calendario.scrivibile) {
                                Text(
                                    "sola lettura",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Colori.InchiostroTenue,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Scelta(titolo: String, spiegazione: String, attiva: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = attiva,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Colori.Copertina),
        )
        Column(Modifier.weight(1f)) {
            Text(titolo, fontSize = 16.sp, color = Colori.Inchiostro)
            Text(spiegazione, fontSize = 13.sp, color = Colori.InchiostroTenue)
        }
    }
}
