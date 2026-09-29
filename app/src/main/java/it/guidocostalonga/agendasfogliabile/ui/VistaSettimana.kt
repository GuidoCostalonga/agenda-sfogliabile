package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.util.intervalloSettimana
import it.guidocostalonga.agendasfogliabile.util.maiuscola
import it.guidocostalonga.agendasfogliabile.util.nomeGiorno
import it.guidocostalonga.agendasfogliabile.util.nomeMese
import it.guidocostalonga.agendasfogliabile.util.numeroSettimana
import java.time.LocalDate
import java.time.ZoneId

/**
 * La settimana. Su schermo largo (tablet o telefono ruotato) è un'agenda aperta:
 * lunedì, martedì e mercoledì a sinistra, gli altri giorni a destra.
 * Su schermo stretto i sette giorni stanno su una sola pagina.
 */
@Composable
fun PagineSettimana(
    lunedi: LocalDate,
    impegni: List<Impegno>?,
    larga: Boolean,
    modifier: Modifier = Modifier,
    onApri: (Impegno) -> Unit,
    onGiorno: (LocalDate) -> Unit,
    onNuovo: (LocalDate, Int?) -> Unit,
) {
    val giorni = remember(lunedi) { (0L..6L).map { lunedi.plusDays(it) } }
    val elenco = impegni.orEmpty()
    if (larga) {
        Row(modifier) {
            Pagina(Modifier.weight(1f).fillMaxHeight(), Lato.SINISTRO) {
                IntestazioneSettimana(lunedi, sinistra = true)
                giorni.take(3).forEach { giorno ->
                    CasellaGiorno(giorno, elenco, Modifier.weight(1f), true, onApri, onGiorno, onNuovo)
                }
            }
            Pagina(Modifier.weight(1f).fillMaxHeight(), Lato.DESTRO) {
                IntestazioneSettimana(lunedi, sinistra = false)
                giorni.drop(3).forEach { giorno ->
                    CasellaGiorno(giorno, elenco, Modifier.weight(1f), true, onApri, onGiorno, onNuovo)
                }
            }
        }
    } else {
        Pagina(modifier) {
            IntestazioneSettimana(lunedi, sinistra = true)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                giorni.forEach { giorno ->
                    CasellaGiorno(giorno, elenco, Modifier.heightIn(min = 76.dp), false, onApri, onGiorno, onNuovo)
                }
            }
        }
    }
}

@Composable
private fun IntestazioneSettimana(lunedi: LocalDate, sinistra: Boolean) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        if (sinistra) {
            Text(
                "${lunedi.nomeMese().maiuscola()} ${lunedi.year}",
                fontFamily = Grazie,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Colori.Inchiostro,
            )
            Text(
                intervalloSettimana(lunedi),
                fontFamily = Grazie,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = Colori.InchiostroTenue,
            )
        } else {
            Text(
                "Settimana ${lunedi.numeroSettimana()}",
                fontFamily = Grazie,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Colori.Inchiostro,
            )
            Text(
                "Tocca un giorno per aprirlo",
                fontFamily = Grazie,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = Colori.InchiostroTenue,
            )
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(thickness = 2.dp, color = Colori.Inchiostro.copy(alpha = 0.7f))
    }
}

@Composable
private fun CasellaGiorno(
    giorno: LocalDate,
    impegni: List<Impegno>,
    modifier: Modifier,
    scorrevole: Boolean,
    onApri: (Impegno) -> Unit,
    onGiorno: (LocalDate) -> Unit,
    onNuovo: (LocalDate, Int?) -> Unit,
) {
    val zona = remember { ZoneId.systemDefault() }
    val eOggi = giorno == LocalDate.now()
    val delGiorno = impegni.filter { it.cadeIl(giorno, zona) }
    Column(
        modifier
            .fillMaxWidth()
            .clickable { onNuovo(giorno, null) }
            .drawBehind {
                val spessore = 1.dp.toPx()
                drawLine(
                    color = Colori.Riga,
                    start = Offset(0f, size.height - spessore),
                    end = Offset(size.width, size.height - spessore),
                    strokeWidth = spessore,
                )
            }
            .padding(vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onGiorno(giorno) }
                .padding(end = 8.dp),
        ) {
            Text(
                giorno.dayOfMonth.toString(),
                fontFamily = Grazie,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (eOggi) Colori.Nastro else Colori.Inchiostro,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                giorno.nomeGiorno(),
                modifier = Modifier.padding(bottom = 3.dp),
                fontFamily = Grazie,
                fontSize = 15.sp,
                color = if (eOggi) Colori.Nastro else Colori.InchiostroTenue,
            )
        }
        if (scorrevole) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                delGiorno.forEach { impegno -> RigaImpegno(impegno, giorno, compatta = true) { onApri(impegno) } }
            }
        } else {
            delGiorno.forEach { impegno -> RigaImpegno(impegno, giorno, compatta = true) { onApri(impegno) } }
        }
    }
}
