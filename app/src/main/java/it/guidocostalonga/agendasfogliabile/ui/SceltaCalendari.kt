package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.ArchivioCalendario
import it.guidocostalonga.agendasfogliabile.data.Calendario
import it.guidocostalonga.agendasfogliabile.data.Preferenze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sceglie quali calendari compaiono nell'agenda e nel riquadro della schermata iniziale. */
@Composable
fun SceltaCalendari(
    archivio: ArchivioCalendario,
    preferenze: Preferenze,
    onIndietro: () -> Unit,
    onCambio: (Set<Long>?) -> Unit,
) {
    val calendari by produceState<List<Calendario>?>(null) {
        value = withContext(Dispatchers.IO) { archivio.calendari() }
    }
    var scelti by remember { mutableStateOf(preferenze.calendariScelti) }

    fun imposta(nuovi: Set<Long>?) {
        scelti = nuovi
        preferenze.calendariScelti = nuovi
        onCambio(nuovi)
    }

    Scaffold(
        containerColor = Colori.Pagina,
        topBar = { BarraSuperiore("Calendari da mostrare", onIndietro) },
    ) { spazio ->
        val elenco = calendari
        if (elenco == null) {
            Box(Modifier.padding(spazio).fillMaxSize())
        } else {
            val tutti = elenco.map { it.id }.toSet()
            fun alterna(id: Long) {
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
                        Text(
                            "Scegli quali calendari compaiono nell'agenda e nel riquadro della schermata iniziale. " +
                                "I calendari arrivano dagli account Google configurati sul telefono.",
                            fontSize = 14.sp,
                            color = Colori.InchiostroTenue,
                        )
                        Row {
                            TextButton(onClick = { imposta(null) }) { Text("Mostra tutti", color = Colori.Copertina) }
                            TextButton(onClick = { imposta(emptySet()) }) { Text("Nascondi tutti", color = Colori.Nastro) }
                        }
                    }
                }
                if (elenco.isEmpty()) {
                    item {
                        Avviso(
                            "Sul telefono non risulta alcun calendario. Controlla in Impostazioni, Account, " +
                                "che l'account Google abbia attiva la sincronizzazione del calendario.",
                        )
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
}
