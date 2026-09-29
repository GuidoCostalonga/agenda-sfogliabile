package it.guidocostalonga.agendasfogliabile.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

/** Numero di pagine disponibili: circa cinquant'anni avanti e indietro. */
const val PAGINE = 40_000

/** La pagina di oggi (o della settimana corrente). */
const val CENTRO = 20_000

private fun scarto(stato: PagerState, pagina: Int): Float =
    (stato.currentPage - pagina) + stato.currentPageOffsetFraction

/**
 * Sfoglia le pagine come in un'agenda vera: la pagina che se ne va ruota
 * attorno al dorso a sinistra e scopre quella successiva, che resta ferma sotto.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Sfogliatore(
    stato: PagerState,
    modifier: Modifier = Modifier,
    contenuto: @Composable (Int) -> Unit,
) {
    HorizontalPager(
        state = stato,
        modifier = modifier,
        beyondViewportPageCount = 1,
    ) { pagina ->
        Box(
            Modifier
                .fillMaxSize()
                .zIndex(if (scarto(stato, pagina) >= 0f) 1f else 0f)
                .graphicsLayer {
                    val s = scarto(stato, pagina)
                    // Annulla lo scorrimento laterale: le pagine restano impilate.
                    translationX = s * size.width
                    cameraDistance = 18f * density
                    if (s > 0f) {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = -90f * s.coerceAtMost(1f)
                        alpha = if (s >= 0.98f) 0f else 1f
                    }
                },
        ) {
            contenuto(pagina)
        }
    }
}
