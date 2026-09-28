package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.ArchivioCalendario
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.util.dataConGiorno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Cerca un impegno per parola, dall'anno scorso ai prossimi due anni. */
@Composable
fun Ricerca(
    archivio: ArchivioCalendario,
    scelti: Set<Long>?,
    onIndietro: () -> Unit,
    onGiorno: (LocalDate) -> Unit,
) {
    var testo by rememberSaveable { mutableStateOf("") }
    var risultati by remember { mutableStateOf<List<Impegno>?>(emptyList()) }
    val fuoco = remember { FocusRequester() }
    val zona = remember { ZoneId.systemDefault() }

    LaunchedEffect(Unit) { fuoco.requestFocus() }
    LaunchedEffect(testo, scelti) {
        val cercato = testo.trim()
        if (cercato.length < 2) {
            risultati = emptyList()
            return@LaunchedEffect
        }
        risultati = null
        delay(300)
        val oggi = LocalDate.now()
        risultati = withContext(Dispatchers.IO) {
            archivio.impegni(oggi.minusYears(1), oggi.plusYears(2), scelti, cercato, 300)
        }
    }

    Scaffold(
        containerColor = Colori.Pagina,
        topBar = { BarraSuperiore("Cerca un impegno", onIndietro) },
    ) { spazio ->
        Column(
            Modifier
                .padding(spazio)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = testo,
                onValueChange = { testo = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .focusRequester(fuoco),
                singleLine = true,
                placeholder = { Text("Titolo, luogo o descrizione") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )
            val elenco = risultati
            when {
                testo.trim().length < 2 -> Text(
                    "Scrivi almeno due lettere. La ricerca copre gli impegni dall'anno scorso ai prossimi due anni.",
                    color = Colori.InchiostroTenue,
                    fontSize = 14.sp,
                )
                elenco == null -> Text("Ricerca in corso…", color = Colori.InchiostroTenue)
                elenco.isEmpty() -> Text("Nessun impegno trovato.", color = Colori.InchiostroTenue)
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    var precedente: LocalDate? = null
                    elenco.forEach { impegno ->
                        val giorno = impegno.giornoInizio(zona)
                        if (giorno != precedente) {
                            precedente = giorno
                            item {
                                Column {
                                    Text(
                                        giorno.dataConGiorno(),
                                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                                        fontFamily = Grazie,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Colori.Inchiostro,
                                    )
                                    HorizontalDivider(color = Colori.Riga)
                                }
                            }
                        }
                        item { RigaImpegno(impegno, giorno) { onGiorno(giorno) } }
                    }
                }
            }
        }
    }
}
