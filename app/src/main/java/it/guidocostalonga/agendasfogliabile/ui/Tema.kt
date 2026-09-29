package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Tavolozza dell'agenda di carta. */
object Colori {
    val Copertina = Color(0xFF1E3553)
    val Pagina = Color(0xFFFBF5E6)
    val PaginaScura = Color(0xFFF3EAD3)
    val Riga = Color(0xFFDCCFB0)
    val Bordo = Color(0xFFB8AA8A)
    val Margine = Color(0xFFD9A08F)
    val Inchiostro = Color(0xFF2B2A28)
    val InchiostroTenue = Color(0xFF6E665A)
    val Nastro = Color(0xFF9E2A2B)
    val Oro = Color(0xFFC9A55C)
}

/** Carattere con grazie per date e intestazioni, come in un'agenda stampata. */
val Grazie: FontFamily = FontFamily.Serif

fun coloreDi(argb: Int): Color = Color(argb or 0xFF000000.toInt())

@Composable
fun TemaAgenda(contenuto: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Colori.Copertina,
            onPrimary = Color.White,
            primaryContainer = Colori.PaginaScura,
            onPrimaryContainer = Colori.Inchiostro,
            secondary = Colori.Nastro,
            onSecondary = Color.White,
            background = Colori.Pagina,
            onBackground = Colori.Inchiostro,
            surface = Colori.Pagina,
            onSurface = Colori.Inchiostro,
            surfaceVariant = Colori.PaginaScura,
            onSurfaceVariant = Colori.InchiostroTenue,
            surfaceContainerLowest = Colori.Pagina,
            surfaceContainerLow = Colori.Pagina,
            surfaceContainer = Colori.Pagina,
            surfaceContainerHigh = Colori.Pagina,
            surfaceContainerHighest = Colori.PaginaScura,
            outline = Colori.Bordo,
            outlineVariant = Colori.Riga,
            error = Colori.Nastro,
        ),
        content = contenuto,
    )
}
