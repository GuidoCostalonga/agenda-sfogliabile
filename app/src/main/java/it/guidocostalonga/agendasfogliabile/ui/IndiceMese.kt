package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.ArchivioCalendario
import it.guidocostalonga.agendasfogliabile.util.titolo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** L'indice del mese: si tocca un giorno e l'agenda si apre lì. */
@Composable
fun IndiceMese(
    iniziale: LocalDate,
    archivio: ArchivioCalendario,
    scelti: Set<Long>?,
    versione: Int,
    onScegli: (LocalDate) -> Unit,
    onChiudi: () -> Unit,
) {
    var mese by remember { mutableStateOf(YearMonth.from(iniziale)) }
    val occupati by produceState(emptySet<LocalDate>(), mese, scelti, versione) {
        value = withContext(Dispatchers.IO) {
            val zona = ZoneId.systemDefault()
            val primo = mese.atDay(1)
            val ultimo = mese.atEndOfMonth()
            val giorni = mutableSetOf<LocalDate>()
            archivio.impegni(primo, ultimo, scelti).forEach { impegno ->
                var giorno = impegno.giornoInizio(zona).let { if (it.isBefore(primo)) primo else it }
                val fine = impegno.giornoFine(zona).let { if (it.isAfter(ultimo)) ultimo else it }
                while (!giorno.isAfter(fine)) {
                    giorni += giorno
                    giorno = giorno.plusDays(1)
                }
            }
            giorni
        }
    }
    val oggi = LocalDate.now()
    val nomi = listOf("L", "M", "M", "G", "V", "S", "D")

    Box(
        Modifier.fillMaxSize().background(Colori.Copertina).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Pagina(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mese = mese.minusMonths(1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mese precedente",
                        tint = Colori.Inchiostro,
                    )
                }
                Text(
                    mese.titolo(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontFamily = Grazie,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Colori.Inchiostro,
                )
                IconButton(onClick = { mese = mese.plusMonths(1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mese successivo",
                        tint = Colori.Inchiostro,
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = Colori.Inchiostro.copy(alpha = 0.7f))
            Row(Modifier.padding(top = 8.dp)) {
                nomi.forEachIndexed { indice, nome ->
                    Text(
                        nome,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = if (indice == 6) Colori.Nastro else Colori.InchiostroTenue,
                    )
                }
            }
            val vuote = mese.atDay(1).dayOfWeek.value - 1
            val lunghezza = mese.lengthOfMonth()
            val righe = (vuote + lunghezza + 6) / 7
            for (riga in 0 until righe) {
                Row {
                    for (colonna in 0 until 7) {
                        val numero = riga * 7 + colonna - vuote + 1
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).padding(3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (numero in 1..lunghezza) {
                                val giorno = mese.atDay(numero)
                                val scelto = giorno == iniziale
                                val eOggi = giorno == oggi
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(if (scelto) Colori.Copertina else Color.Transparent)
                                        .then(
                                            if (eOggi) Modifier.border(2.dp, Colori.Nastro, CircleShape) else Modifier,
                                        )
                                        .clickable { onScegli(giorno) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        numero.toString(),
                                        fontFamily = Grazie,
                                        fontSize = 17.sp,
                                        color = when {
                                            scelto -> Color.White
                                            colonna == 6 -> Colori.Nastro
                                            else -> Colori.Inchiostro
                                        },
                                    )
                                    if (giorno in occupati) {
                                        Box(
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 5.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (scelto) Color.White else Colori.Copertina),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextButton(onClick = { onScegli(oggi) }) { Text("Vai a oggi", color = Colori.Nastro) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onChiudi) { Text("Chiudi", color = Colori.Copertina) }
            }
        }
    }
}
