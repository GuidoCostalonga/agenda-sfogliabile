package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.util.maiuscola
import it.guidocostalonga.agendasfogliabile.util.nomeGiorno
import it.guidocostalonga.agendasfogliabile.util.nomeMese
import java.time.LocalDate
import java.time.ZoneId

/** Pagina di un giorno: intestazione con la data e una riga per ogni ora. */
@Composable
fun PaginaGiorno(
    giorno: LocalDate,
    impegni: List<Impegno>?,
    modifier: Modifier = Modifier,
    onApri: (Impegno) -> Unit,
    onNuovo: (LocalDate, Int?) -> Unit,
    onIndice: () -> Unit,
) {
    val zona = remember { ZoneId.systemDefault() }
    val oggi = LocalDate.now()
    val eOggi = giorno == oggi
    Pagina(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onIndice),
        ) {
            Text(
                giorno.dayOfMonth.toString(),
                fontFamily = Grazie,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                color = if (eOggi) Colori.Nastro else Colori.Inchiostro,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(giorno.nomeGiorno(), fontFamily = Grazie, fontSize = 24.sp, color = Colori.Inchiostro)
                Text(
                    buildString {
                        append(giorno.nomeMese().maiuscola())
                        append(' ')
                        append(giorno.year)
                        if (eOggi) append(" · oggi")
                    },
                    fontFamily = Grazie,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                    color = if (eOggi) Colori.Nastro else Colori.InchiostroTenue,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(thickness = 2.dp, color = Colori.Inchiostro.copy(alpha = 0.7f))
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(thickness = 1.dp, color = Colori.Inchiostro.copy(alpha = 0.4f))

        if (impegni == null) {
            Spacer(Modifier.weight(1f))
        } else {
            val giornata = impegni.filter { it.cadeIl(giorno, zona) }
            val interi = giornata.filter { !it.iniziaIl(giorno, zona) }
            val conOrario = giornata.filter { it.iniziaIl(giorno, zona) }
            val primaOra = minOf(7, conOrario.minOfOrNull { it.oraInizio(zona).hour } ?: 7)
            val ultimaOra = maxOf(21, conOrario.maxOfOrNull { it.oraInizio(zona).hour } ?: 21)

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (interi.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Etichetta("Tutto il giorno")
                    interi.forEach { impegno -> RigaImpegno(impegno, giorno) { onApri(impegno) } }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Colori.Riga)
                }
                for (ora in primaOra..ultimaOra) {
                    val diQuestOra = conOrario.filter { it.oraInizio(zona).hour == ora }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onNuovo(giorno, ora) }
                            .drawBehind {
                                val spessore = 1.dp.toPx()
                                drawLine(
                                    color = Colori.Riga,
                                    start = Offset(0f, size.height - spessore),
                                    end = Offset(size.width, size.height - spessore),
                                    strokeWidth = spessore,
                                )
                                val margine = 54.dp.toPx()
                                drawLine(
                                    color = Colori.Margine,
                                    start = Offset(margine, 0f),
                                    end = Offset(margine, size.height),
                                    strokeWidth = spessore,
                                )
                            },
                    ) {
                        Text(
                            "%02d:00".format(ora),
                            modifier = Modifier.width(54.dp).padding(top = 8.dp),
                            fontFamily = Grazie,
                            fontSize = 13.sp,
                            color = Colori.InchiostroTenue,
                        )
                        Column(Modifier.weight(1f).padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                            diQuestOra.forEach { impegno -> RigaImpegno(impegno, giorno) { onApri(impegno) } }
                        }
                    }
                }
            }
        }
    }
}
