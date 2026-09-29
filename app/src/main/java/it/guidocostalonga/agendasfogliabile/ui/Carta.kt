package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.util.testo
import java.time.LocalDate
import java.time.ZoneId

/** Posizione della pagina rispetto al dorso dell'agenda. */
enum class Lato { SINISTRO, DESTRO, SINGOLO }

/** Una pagina color avorio, con l'ombra del dorso dal lato della rilegatura. */
@Composable
fun Pagina(
    modifier: Modifier = Modifier,
    lato: Lato = Lato.SINGOLO,
    contenuto: @Composable ColumnScope.() -> Unit,
) {
    val forma = if (lato == Lato.SINISTRO) {
        RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 2.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp)
    }
    Column(
        modifier
            .shadow(8.dp, forma)
            .clip(forma)
            .background(Colori.Pagina)
            .drawBehind {
                val ombra = 24.dp.toPx()
                if (lato == Lato.SINISTRO) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0x33000000)),
                            startX = size.width - ombra,
                            endX = size.width,
                        ),
                        topLeft = Offset(size.width - ombra, 0f),
                        size = Size(ombra, size.height),
                    )
                } else {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0x33000000), Color.Transparent),
                            startX = 0f,
                            endX = ombra,
                        ),
                        topLeft = Offset.Zero,
                        size = Size(ombra, size.height),
                    )
                }
            }
            .padding(
                start = if (lato == Lato.SINISTRO) 18.dp else 24.dp,
                end = if (lato == Lato.SINISTRO) 24.dp else 18.dp,
                top = 16.dp,
                bottom = 12.dp,
            ),
        content = contenuto,
    )
}

/** Il nastrino segnalibro rosso. */
@Composable
fun Nastro(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 18.dp, height = 72.dp)) {
        val tacca = size.width * 0.7f
        val percorso = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width / 2f, size.height - tacca)
            lineTo(0f, size.height)
            close()
        }
        drawPath(percorso, Colori.Nastro)
        drawRect(
            color = Color(0x22000000),
            topLeft = Offset(size.width * 0.72f, 0f),
            size = Size(size.width * 0.28f, size.height - tacca),
        )
    }
}

@Composable
fun Pallino(colore: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(coloreDi(colore)))
}

@Composable
fun Etichetta(testo: String, modifier: Modifier = Modifier) {
    Text(
        testo.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.SemiBold,
        color = Colori.InchiostroTenue,
    )
}

/** Un impegno scritto sulla pagina, con la striscia del colore del calendario. */
@Composable
fun RigaImpegno(
    impegno: Impegno,
    giorno: LocalDate,
    compatta: Boolean = false,
    onClick: () -> Unit,
) {
    val zona = remember { ZoneId.systemDefault() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (compatta) 2.dp else 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(4.dp)
                .height(if (compatta) 14.dp else 34.dp)
                .background(coloreDi(impegno.colore), RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        if (compatta) {
            val ora = if (impegno.iniziaIl(giorno, zona)) impegno.oraInizio(zona).testo() + "  " else ""
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Colori.InchiostroTenue)) { append(ora) }
                    append(impegno.titolo)
                },
                fontSize = 13.sp,
                color = Colori.Inchiostro,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Column {
                Text(
                    impegno.titolo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Colori.Inchiostro,
                )
                // Sotto l'etichetta «Tutto il giorno» l'orario sarebbe una ripetizione.
                val orario = if (impegno.tuttoIlGiorno) "" else impegno.descriviOrario(giorno, zona)
                val dettaglio = listOf(orario, impegno.luogo)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (dettaglio.isNotEmpty()) {
                    Text(dettaglio, fontSize = 13.sp, color = Colori.InchiostroTenue)
                }
            }
        }
    }
}

/** Avviso su fondo carta più scura. */
@Composable
fun Avviso(testo: String, modifier: Modifier = Modifier) {
    Text(
        testo,
        modifier = modifier
            .fillMaxWidth()
            .background(Colori.PaginaScura, RoundedCornerShape(8.dp))
            .padding(12.dp),
        fontSize = 14.sp,
        color = Colori.Inchiostro,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperiore(
    titolo: String,
    onIndietro: (() -> Unit)?,
    azioni: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(titolo, fontFamily = Grazie, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onIndietro != null) {
                IconButton(onClick = onIndietro) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                }
            }
        },
        actions = azioni,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Colori.Copertina,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}
